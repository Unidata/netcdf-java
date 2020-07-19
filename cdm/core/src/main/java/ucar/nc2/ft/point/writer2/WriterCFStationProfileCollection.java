/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.Station;

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
  private boolean useAlt;
  private boolean useWmoId;

  private int desc_strlen = 1, wmo_strlen = 1;
  private HashSet<String> stationVarMap = new HashSet<>();

  ///////////////////////////////////////////////////
  // private Formatter coordNames = new Formatter();
  private Structure profileStruct; // used for netcdf4 extended
  private HashSet<String> profileVarMap = new HashSet<>();
  private boolean headerDone;

  WriterCFStationProfileCollection(String fileOut, AttributeContainer globalAtts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    writerb.addAttribute(new Attribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeriesProfile.name()));
    writerb.addAttribute(
        new Attribute(CF.DSG_REPRESENTATION, "Ragged array representation of time series profiless, H.5.3"));
  }

  void setStations(List<StationFeature> stns) {
    this.stnList = stns;

    // see if there's altitude, wmoId for any stations
    for (StationFeature stationFeature : stnList) {
      Station stn = stationFeature.getStation();
      if (!Double.isNaN(stn.getAltitude()))
        useAlt = true;
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

    // llbb = CFPointWriterUtils.getBoundingBox(stnList); // gets written in super.finish();
  }

  int writeProfile(StationProfileFeature spf, ProfileFeature profile) throws IOException {
    int count = 0;
    for (PointFeature pf : profile) {
      if (!headerDone) {
        if (id_strlen == 0)
          id_strlen = profile.getName().length() * 2;
        writeHeader(spf, profile, pf);
        headerDone = true;
      }
      writeObsData(pf);
      count++;
    }

    Integer stnIndex = stationIndexMap.get(spf.getName());
    if (stnIndex == null) {
      log.warn("BAD station {}", spf.getName());
    } else {
      writeProfileData(stnIndex, profile, count);
    }

    return count;
  }

  private void writeHeader(StationProfileFeature stn, ProfileFeature profile, PointFeature obs) throws IOException {
    StructureData stnData = stn.getFeatureData();
    StructureData profileData = profile.getFeatureData();
    StructureData obsData = obs.getFeatureData();

    List<VariableSimpleIF> obsCoords = new ArrayList<>();
    Formatter coordNames = new Formatter().format("%s %s %s", profileTimeName, latName, lonName);
    obsCoords.add(VariableSimpleBuilder.makeScalar(altitudeCoordinateName, "obs altitude", altUnits, DataType.DOUBLE)
        .addAttribute(CF.STANDARD_NAME, "altitude")
        .addAttribute(CF.POSITIVE, getZisPositive(altitudeCoordinateName, altUnits)).build());
    coordNames.format(" %s", altitudeCoordinateName);

    super.writeHeader(obsCoords, stnData, profileData, obsData, coordNames.toString());

    // write the stations
    int count = 0;
    stationIndexMap = new HashMap<>(2 * stnList.size());
    for (StationFeature sf : stnList) {
      writeStationData(sf);
      stationIndexMap.put(sf.getStation().getName(), count);
      count++;
    }

  }

  void makeFeatureVariables(StructureData stnData, boolean isExtended) {
    // add the dimensions : extended model can use an unlimited dimension
    Dimension stationDim = writerb.addDimension(stationDimName, stnList.size());

    List<VariableSimpleIF> stnVars = new ArrayList<>();
    stnVars.add(VariableSimpleBuilder.makeScalar(latName, "station latitude", CDM.LAT_UNITS, DataType.DOUBLE).build());
    stnVars.add(VariableSimpleBuilder.makeScalar(lonName, "station longitude", CDM.LON_UNITS, DataType.DOUBLE).build());

    if (useAlt) {
      stnVars.add(VariableSimpleBuilder.makeScalar(stationAltName, "station altitude", altUnits, DataType.DOUBLE)
          .addAttribute(CF.STANDARD_NAME, CF.SURFACE_ALTITUDE)
          .addAttribute(CF.POSITIVE, getZisPositive(altName, altUnits)).build());
    }

    stnVars.add(VariableSimpleBuilder.makeString(stationIdName, "station identifier", null, id_strlen)
        .addAttribute(CF.CF_ROLE, CF.TIMESERIES_ID).build()); // station_id:cf_role = "timeseries_id";

    if (useDesc)
      stnVars.add(VariableSimpleBuilder.makeString(descName, "station description", null, desc_strlen)
          .addAttribute(CF.STANDARD_NAME, CF.PLATFORM_NAME).build());

    if (useWmoId)
      stnVars.add(VariableSimpleBuilder.makeString(wmoName, "station WMO id", null, wmo_strlen)
          .addAttribute(CF.STANDARD_NAME, CF.PLATFORM_ID).build());

    for (StructureMembers.Member m : stnData.getMembers()) {
      if (findDataVar(m.getName()) != null)
        stnVars.add(VariableSimpleBuilder.fromMember(m).build());
    }

    if (isExtended) {
      Structure.Builder structb = writerb.addStructure(stationStructName, stationDimName);
      addCoordinatesExtended(structb, stnVars);
    } else {
      addCoordinatesClassic(stationDim, stnVars, stationVarMap);
    }

  }

  private int stnRecno;

  private void writeStationData(StationFeature sf) throws IOException {
    Station stn = sf.getStation();
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
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(stnCoords, sf.getFeatureData()));
    stnRecno = super.writeStructureData(stnRecno, stationStruct, sdall, stationVarMap);
  }

  @Override
  void makeMiddleVariables(StructureData profileData, boolean isExtended) {
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

    for (StructureMembers.Member m : profileData.getMembers()) {
      VariableSimpleIF dv = findDataVar(m.getName());
      if (dv != null)
        profileVars.add(dv);
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

  private void writeObsData(PointFeature pf) throws IOException {
    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(altitudeCoordinateName, null, null, DataType.DOUBLE, pf.getLocation().getAltitude());
    StructureData coords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(coords, pf.getFeatureData()));
    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
