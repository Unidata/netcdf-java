/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.time.CalendarDateUnit;
import java.io.IOException;
import java.util.*;

/**
 * Write a CF "Discrete Sample" station profile collection file.
 * Ragged array representation of time series profiles, H.5.3
 * This uses the contiguous ragged array representation for each profile (9.5.43.3), and the indexed ragged array
 * representation to organise the profiles into time series (9.3.54).
 *
 * @author caron
 * @since 7/14/2014
 */
public class WriterCFStationProfileCollection extends CFPointWriter {
  private static Logger log = LoggerFactory.getLogger(WriterCFStationProfileCollection.class);

  private List<StationFeature> stnList;
  private Structure stationStruct; // used for netcdf4 extended
  private HashMap<String, Integer> stationIndexMap;

  private boolean useDesc;
  private boolean useAlt;
  private boolean useWmoId;

  private int desc_strlen = 1, wmo_strlen = 1;
  private Map<String, Variable> stationVarMap = new HashMap<>();

  ///////////////////////////////////////////////////
  // private Formatter coordNames = new Formatter();
  private Structure profileStruct; // used for netcdf4 extended
  private Map<String, Variable> profileVarMap = new HashMap<>();
  private boolean headerDone;

  public WriterCFStationProfileCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeriesProfile.name()));
    writer.addGroupAttribute(null,
        new Attribute(CF.DSG_REPRESENTATION, "Ragged array representation of time series profiless, H.5.3"));
  }

  public void setStations(List<StationFeature> stns) {
    this.stnList = stns;

    // see if there's altitude, wmoId for any stations
    for (StationFeature stn : stnList) {
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

    llbb = CFPointWriterUtils.getBoundingBox(stnList); // gets written in super.finish();
  }

  public int writeProfile(StationProfileFeature spf, ProfileFeature profile) throws IOException {
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
    // obsCoords.add(VariableSimpleBuilder.makeScalar(timeName, "time of measurement", timeUnit.getUnitsString(),
    // DataType.DOUBLE)); // LOOK ??
    Formatter coordNames = new Formatter().format("%s %s %s", profileTimeName, latName, lonName);
    // if (useAlt) {
    obsCoords.add(VariableSimpleBuilder.makeScalar(altitudeCoordinateName, "obs altitude", altUnits, DataType.DOUBLE)
        .addAttribute(CF.STANDARD_NAME, "altitude")
        .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(altitudeCoordinateName, altUnits)).build());
    coordNames.format(" %s", altitudeCoordinateName);
    // }

    super.writeHeader2(obsCoords, stnData, profileData, obsData, coordNames.toString());

    // write the stations
    int count = 0;
    stationIndexMap = new HashMap<>(2 * stnList.size());
    for (StationFeature sf : stnList) {
      writeStationData(sf);
      stationIndexMap.put(sf.getName(), count);
      count++;
    }

  }

  protected void makeFeatureVariables(StructureData stnData, boolean isExtended) {
    // add the dimensions : extended model can use an unlimited dimension
    // Dimension stationDim = isExtended ? writer.addDimension(null, stationDimName, 0, true, true, false) :
    // writer.addDimension(null, stationDimName, nstns);
    Dimension stationDim = writer.addDimension(null, stationDimName, stnList.size());

    List<VariableSimpleIF> stnVars = new ArrayList<>();
    stnVars.add(VariableSimpleBuilder.makeScalar(latName, "station latitude", CDM.LAT_UNITS, DataType.DOUBLE).build());
    stnVars.add(VariableSimpleBuilder.makeScalar(lonName, "station longitude", CDM.LON_UNITS, DataType.DOUBLE).build());

    if (useAlt) {
      stnVars.add(VariableSimpleBuilder.makeScalar(stationAltName, "station altitude", altUnits, DataType.DOUBLE)
          .addAttribute(CF.STANDARD_NAME, CF.SURFACE_ALTITUDE)
          .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(altName, altUnits)).build());
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
      if (getDataVar(m.getName()) != null)
        stnVars.add(VariableSimpleBuilder.fromMember(m).build());
    }

    if (isExtended) {
      stationStruct = (Structure) writer.addVariable(null, stationStructName, DataType.STRUCTURE, stationDimName);
      addCoordinatesExtended(stationStruct, stnVars);
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

  @Override
  protected void makeMiddleVariables(StructureData profileData, boolean isExtended) {
    Dimension profileDim = writer.addDimension(null, profileDimName, nfeatures);

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
      VariableSimpleIF dv = getDataVar(m.getName());
      if (dv != null)
        profileVars.add(dv);
    }

    if (isExtended) {
      profileStruct = (Structure) writer.addVariable(null, profileStructName, DataType.STRUCTURE, profileDimName);
      addCoordinatesExtended(profileStruct, profileVars);
    } else {
      addCoordinatesClassic(profileDim, profileVars, profileVarMap);
    }
  }

  private int profileRecno;

  public void writeProfileData(int stnIndex, ProfileFeature profile, int nobs) throws IOException {
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

  public void writeObsData(PointFeature pf) throws IOException {
    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(altitudeCoordinateName, null, null, DataType.DOUBLE, pf.getLocation().getAltitude());
    StructureData coords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(coords, pf.getFeatureData()));
    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
