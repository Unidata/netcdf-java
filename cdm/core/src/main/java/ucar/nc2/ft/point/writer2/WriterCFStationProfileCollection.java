/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataComposite;
import ucar.ma2.StructureDataFromMember;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.Structure;
import ucar.nc2.VariableSimpleBuilder;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.time.CalendarDateUnit;

/**
 * Write a CF "Discrete Sample" station profile collection file.
 * Ragged array representation of time series profiles, H.5.3
 * This uses the contiguous ragged array representation for each profile (9.5.43.3), and the indexed ragged array
 * representation to organise the profiles into time series (9.3.54).
 *
 * @author caron
 * @since 7/14/2014
 */
class WriterCFStationProfileCollection extends WriterCFPointAbstract {
  private static Logger log = LoggerFactory.getLogger(WriterCFStationProfileCollection.class);

  private List<StationFeature> stnList;
  private Structure stationStruct; // used for netcdf4 extended
  private HashMap<String, Integer> stationIndexMap;

  private boolean useDesc;
  private boolean useWmoId;

  private int desc_strlen = 1, wmo_strlen = 1;
  private HashSet<String> stationVarMap = new HashSet<>();

  ///////////////////////////////////////////////////
  // private Formatter coordNames = new Formatter();
  private Structure profileStruct; // used for netcdf4 extended
  private HashSet<String> profileVarMap = new HashSet<>();

  WriterCFStationProfileCollection(String fileOut, AttributeContainer globalAtts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    writerb.addAttribute(new Attribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeriesProfile.name()));
    writerb.addAttribute(
        new Attribute(CF.DSG_REPRESENTATION, "Ragged array representation of time series profiles, H.5.3"));
  }

  void setStations(List<StationFeature> stns) {
    this.stnList = stns.stream().distinct().collect(Collectors.toList());

    // see if there's altitude, wmoId for any stations
    for (StationFeature stn : stnList) {
      useAlt = !Double.isNaN(stn.getAltitude());
      if ((stn.getWmoId() != null) && (!stn.getWmoId().trim().isEmpty()))
        useWmoId = true;
      if ((stn.getDescription() != null) && (!stn.getDescription().trim().isEmpty()))
        useDesc = true;

      // find string lengths
      id_strlen = Math.max(id_strlen, stn.getName().length());
      if (stn.getDescription() != null)
        desc_strlen = Math.max(desc_strlen, stn.getDescription().length());
      if (stn.getWmoId() != null)
        wmo_strlen = Math.max(wmo_strlen, stn.getWmoId().length());
    }

    llbb = CFPointWriterUtils.getBoundingBox(stnList); // gets written in super.finish();
  }

  int writeProfile(ProfileFeature profile) throws IOException {
    if (id_strlen == 0)
      id_strlen = profile.getName().length() * 2;
    int count = 0;
    for (PointFeature pf : profile) {
      writeObsData(pf);
      count++;
    }
    return count;
  }

  protected void writeHeader(List<StationFeature> stations) throws IOException {
    List<PointFeatureCollection> coverageCollections = new ArrayList<>();
    List<StructureData> stationData = new ArrayList<>();
    List<StructureData> profileData = new ArrayList<>();
    List<VariableSimpleIF> obsCoords = new ArrayList<>();

    for (StationFeature station : stations) {
      stationData.add(station.getFeatureData());
      for (ProfileFeature profile : (StationProfileFeature) station) {
        profileData.add(profile.getFeatureData());
        coverageCollections.add(profile);

        obsCoords.add(VariableSimpleBuilder
            .makeScalar(profile.getTimeName(), "time of measurement", timeUnit.toString(), DataType.DOUBLE).build());

        altitudeCoordinateName = profile.getAltName();
        obsCoords
            .add(VariableSimpleBuilder.makeScalar(altitudeCoordinateName, "obs altitude", altUnits, DataType.DOUBLE)
                .addAttribute(CF.STANDARD_NAME, "altitude")
                .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(altitudeCoordinateName, altUnits)).build());
      }
    }

    super.writeHeader(obsCoords, coverageCollections, stationData, profileData);

    // write the stations
    int count = 0;
    stationIndexMap = new HashMap<>(2 * stnList.size());
    for (StationFeature sf : stnList) {
      writeStationData(sf);
      stationIndexMap.put(sf.getName(), count);
      for (ProfileFeature p : (StationProfileFeature) sf) {
        int countPoints = 0;
        if (p.size() >= 0) {
          countPoints += p.size();
        } else {
          countPoints += Iterables.size(p);
        }
        writeProfileData(count, p, countPoints);
      }
      count++;
    }
  }

  @Override
  void setFeatureAuxInfo(int nfeatures, int id_strlen) {
    int countProfiles = 0;
    int name_strlen = 0;
    for (StationFeature s : stnList) {
      name_strlen = Math.max(name_strlen, s.getName().length());
      if (((StationProfileFeature) s).size() >= 0)
        countProfiles += ((StationProfileFeature) s).size();
      else {
        countProfiles += Iterables.size(((StationProfileFeature) s));
      }
    }
    this.nfeatures = countProfiles;
    this.id_strlen = name_strlen;
  }

  void makeFeatureVariables(List<StructureData> stnDataStructs, boolean isExtended) {
    // add the dimensions : extended model can use an unlimited dimension
    Dimension stationDim = writerb.addDimension(stationDimName, stnList.size());

    List<VariableSimpleIF> stnVars = new ArrayList<>();
    stnVars.add(VariableSimpleBuilder.makeScalar(latName, "station latitude", CDM.LAT_UNITS, DataType.DOUBLE).build());
    stnVars.add(VariableSimpleBuilder.makeScalar(lonName, "station longitude", CDM.LON_UNITS, DataType.DOUBLE).build());

    if (useAlt) {
      stnVars.add(VariableSimpleBuilder.makeScalar(stationAltName, "station altitude", altUnits, DataType.DOUBLE)
          .addAttribute(CF.STANDARD_NAME, CF.STATION_ALTITUDE)
          .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(stationAltName, altUnits)).build());
    }

    stnVars.add(VariableSimpleBuilder.makeString(stationIdName, "station identifier", null, id_strlen)
        .addAttribute(CF.CF_ROLE, CF.TIMESERIES_ID).build()); // station_id:cf_role = "timeseries_id";

    if (useDesc)
      stnVars.add(VariableSimpleBuilder.makeString(descName, "station description", null, desc_strlen)
          .addAttribute(CF.STANDARD_NAME, CF.PLATFORM_NAME).build());

    if (useWmoId)
      stnVars.add(VariableSimpleBuilder.makeString(wmoName, "station WMO id", null, wmo_strlen)
          .addAttribute(CF.STANDARD_NAME, CF.PLATFORM_ID).build());

    for (StructureData stnData : stnDataStructs) {
      for (StructureMembers.Member m : stnData.getMembers()) {
        if (findDataVar(m.getName()) != null)
          stnVars.add(VariableSimpleBuilder.fromMember(m).build());
      }
    }

    if (isExtended) {
      Structure.Builder structb = writerb.addStructure(stationStructName, stationDimName);
      addCoordinatesExtended(structb, stnVars);
    } else {
      addCoordinatesClassic(stationDim, stnVars, stationVarMap);
    }

  }

  private int stnRecno;

  private void writeStationData(StationFeature stn) throws IOException {
    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(latName, null, null, DataType.DOUBLE, stn.getLatLon().getLatitude());
    smb.addMemberScalar(lonName, null, null, DataType.DOUBLE, stn.getLatLon().getLongitude());
    if (useAlt)
      smb.addMemberScalar(stationAltName, null, null, DataType.DOUBLE, stn.getAltitude());
    smb.addMemberString(stationIdName, null, null, stn.getName().trim(), id_strlen);
    if (useDesc)
      smb.addMemberString(descName, null, null, stn.getDescription().trim(), desc_strlen);
    if (useWmoId)
      smb.addMemberString(wmoName, null, null, stn.getWmoId().trim(), wmo_strlen);
    StructureData stnCoords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(stnCoords, stn.getFeatureData()));
    stnRecno = super.writeStructureData(stnRecno, stationStruct, sdall, stationVarMap);
  }

  void makeMiddleVariables(List<StructureData> profileDataStructs, boolean isExtended) {
    Dimension profileDim = writerb.addDimension(profileDimName, nfeatures);

    // add the profile Variables using the profile dimension
    List<VariableSimpleIF> profileVars = new ArrayList<>();
    profileVars.add(VariableSimpleBuilder.makeString(profileIdName, "profile identifier", null, id_strlen)
        .addAttribute(CF.CF_ROLE, CF.PROFILE_ID) // profileId:cf_role = "profile_id";
        .addAttribute(CDM.MISSING_VALUE, String.valueOf(idMissingValue)).build());

    profileVars
        .add(VariableSimpleBuilder.makeScalar(numberOfObsName, "number of obs for this profile", null, DataType.INT)
            .addAttribute(CF.SAMPLE_DIMENSION, recordDimName).build()); // rowSize:sample_dimension = "obs"

    profileVars.add(VariableSimpleBuilder
        .makeScalar(profileTimeName, "nominal time of profile", timeUnit.getUdUnit(), DataType.DOUBLE)
        .addAttribute(CF.CALENDAR, timeUnit.getCalendar().toString()).build());

    profileVars
        .add(VariableSimpleBuilder.makeScalar(stationIndexName, "station index for this profile", null, DataType.INT)
            .addAttribute(CF.INSTANCE_DIMENSION, stationDimName).build());

    for (StructureData profileData : profileDataStructs) {
      for (StructureMembers.Member m : profileData.getMembers()) {
        VariableSimpleIF dv = findDataVar(m.getName());
        if (dv != null)
          profileVars.add(dv);
      }
    }

    if (isExtended) {
      Structure.Builder structb = writerb.addStructure(profileStructName, profileDimName);
      addCoordinatesExtended(structb, profileVars);
    } else {
      addCoordinatesClassic(profileDim, profileVars, profileVarMap);
    }
  }

  @Override
  void finishBuilding() throws IOException {
    super.finishBuilding();
    stationStruct = findStructure(stationStructName);
    profileStruct = findStructure(profileStructName);
  }

  private int profileRecno;

  private void writeProfileData(int stnIndex, ProfileFeature profile, int nobs) throws IOException {
    trackBB(profile.getLatLon(), profile.getTime());

    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(latName, null, null, DataType.DOUBLE, profile.getLatLon().getLatitude());
    smb.addMemberScalar(lonName, null, null, DataType.DOUBLE, profile.getLatLon().getLongitude());
    // Date date = (profile.getTime() != null) ? (double) profile.getTime().getTime() : 0.0; // LOOK (profile.getTime()
    // != null) ???
    double timeInMyUnits = timeUnit.makeOffsetFromRefDate(profile.getTime());
    smb.addMemberScalar(profileTimeName, null, null, DataType.DOUBLE, timeInMyUnits); // LOOK time not always part
    // of profile
    smb.addMemberString(profileIdName, null, null, profile.getName().trim(), id_strlen);
    smb.addMemberScalar(numberOfObsName, null, null, DataType.INT, nobs);
    smb.addMemberScalar(stationIndexName, null, null, DataType.INT, stnIndex);
    StructureData profileCoords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall =
        StructureDataComposite.create(ImmutableList.of(profileCoords, profile.getFeatureData()));
    profileRecno = super.writeStructureData(profileRecno, profileStruct, sdall, profileVarMap);
  }

  private int obsRecno;

  protected void resetObsIndex() {
    obsRecno = 0;
  }

  private void writeObsData(PointFeature pf) throws IOException {
    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(pf.getFeatureCollection().getTimeName(), null, null, DataType.DOUBLE, pf.getObservationTime());
    smb.addMemberScalar(pf.getFeatureCollection().getAltName(), null, null, DataType.DOUBLE,
        pf.getLocation().getAltitude());
    StructureData coords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(coords, pf.getFeatureData()));
    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }


}
