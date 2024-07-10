/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import com.google.common.collect.ImmutableList;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDateUnit;
import java.io.IOException;
import java.util.*;

/**
 * Write a CF "Discrete Sample" trajectory profile (section) collection file.
 * Contiguous ragged array representation of trajectory profile, H.6.3
 * *
 * 
 * @author caron
 * @since 7/14/2014
 */
public class WriterCFTrajectoryProfileCollection extends CFPointWriter {
  public static final String trajectoryIndexName = "trajectoryIndex";

  private int ntraj;
  private int traj_strlen;
  private Structure trajStructure; // used for netcdf4 extended
  private HashMap<String, Integer> trajIndexMap;

  private Map<String, Variable> trajVarMap = new HashMap<>();

  ///////////////////////////////////////////////////
  private Structure profileStruct; // used for netcdf4 extended
  private Map<String, Variable> profileVarMap = new HashMap<>();

  public WriterCFTrajectoryProfileCollection(String fileOut, List<Attribute> globalAtts,
      List<VariableSimpleIF> dataVars, CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config)
      throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.trajectoryProfile.name()));
    writer.addGroupAttribute(null,
        new Attribute(CF.DSG_REPRESENTATION, "Contiguous ragged array representation of trajectory profile, H.6.3"));
  }

  public WriterCFTrajectoryProfileCollection(String fileOut, List<Attribute> globalAtts,
      List<VariableSimpleIF> dataVars, List<CoordinateAxis> coordVars, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, config, coordVars);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.trajectoryProfile.name()));
    writer.addGroupAttribute(null,
        new Attribute(CF.DSG_REPRESENTATION, "Contiguous ragged array representation of trajectory profile, H.6.3"));
  }


  public void setFeatureAuxInfo2(int ntraj, int traj_strlen) {
    this.ntraj = ntraj;
    this.traj_strlen = traj_strlen;
    trajIndexMap = new HashMap<>(2 * ntraj);
  }

  public int writeProfile(TrajectoryProfileFeature section, ProfileFeature profile) throws IOException {
    int count = 0;
    for (PointFeature pf : profile) {
      if (id_strlen == 0)
        id_strlen = profile.getName().length() * 2;
      writeObsData(pf);
      count++;
    }

    Integer sectionIndex = trajIndexMap.get(section.getName());
    if (sectionIndex == null) {
      sectionIndex = writeSectionData(section);
      trajIndexMap.put(section.getName(), sectionIndex);
    }
    writeProfileData(sectionIndex, profile, count);
    return count;
  }

  protected void writeHeader(List<TrajectoryProfileFeature> trajectoryProfiles) throws IOException {
    List<VariableSimpleIF> obsCoords = new ArrayList<>();
    List<ProfileFeature> profileFeatures = new ArrayList<>();
    List<StructureData> trajectoryData = new ArrayList<>();
    List<StructureData> profileData = new ArrayList<>();

    for (TrajectoryProfileFeature trajectoryProfile : trajectoryProfiles) {
      trajectoryData.add(trajectoryProfile.getFeatureData());
      for (ProfileFeature profile : trajectoryProfile) {
        profileData.add(profile.getFeatureData());
        profileFeatures.add(profile);
      }
      obsCoords.add(VariableSimpleBuilder
          .makeScalar(trajectoryProfile.getTimeName(), "time of measurement", timeUnit.toString(), DataType.DOUBLE)
          .build());

      if (altUnits != null) {
        altitudeCoordinateName = trajectoryProfile.getAltName();
        obsCoords
            .add(VariableSimpleBuilder.makeScalar(altitudeCoordinateName, "obs altitude", altUnits, DataType.DOUBLE)
                .addAttribute(CF.STANDARD_NAME, "altitude")
                .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(altitudeCoordinateName, altUnits)).build());
      }
    }
    super.writeHeader(obsCoords, profileFeatures, trajectoryData, profileData);
  }

  protected void makeFeatureVariables(List<StructureData> trajDataStructs, boolean isExtended) {

    // add the dimensions : extended model can use an unlimited dimension
    Dimension trajDim = writer.addDimension(null, trajDimName, ntraj);

    List<VariableSimpleIF> trajVars = new ArrayList<>();

    trajVars.add(VariableSimpleBuilder.makeString(trajIdName, "trajectory identifier", null, traj_strlen)
        .addAttribute(CF.CF_ROLE, CF.TRAJECTORY_ID).build());

    for (StructureData trajData : trajDataStructs) {
      for (StructureMembers.Member m : trajData.getMembers()) {
        if (getDataVar(m.getName()) != null)
          trajVars.add(VariableSimpleBuilder.fromMember(m).build());
      }
    }

    if (isExtended) {
      trajStructure = (Structure) writer.addVariable(null, trajStructName, DataType.STRUCTURE, trajDimName);
      addCoordinatesExtended(trajStructure, trajVars);
    } else {
      addCoordinatesClassic(trajDim, trajVars, trajVarMap);
    }

  }

  private int trajRecno;

  private int writeSectionData(TrajectoryProfileFeature section) throws IOException {

    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberString(trajIdName, null, null, section.getName().trim(), traj_strlen);
    StructureData coords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(coords, section.getFeatureData()));
    trajRecno = super.writeStructureData(trajRecno, trajStructure, sdall, trajVarMap);
    return trajRecno - 1;
  }

  @Override
  protected void makeMiddleVariables(List<StructureData> profileDataStructs, boolean isExtended) {

    Dimension profileDim = writer.addDimension(null, profileDimName, nfeatures);

    // add the profile Variables using the profile dimension
    List<VariableSimpleIF> profileVars = new ArrayList<>();
    profileVars.add(VariableSimpleBuilder.makeString(profileIdName, "profile identifier", null, id_strlen)
        .addAttribute(CF.CF_ROLE, CF.PROFILE_ID) // profileId:cf_role = "profile_id";
        .addAttribute(CDM.MISSING_VALUE, String.valueOf(idMissingValue)).build());

    profileVars
        .add(VariableSimpleBuilder.makeScalar(latName, "profile latitude", CDM.LAT_UNITS, DataType.DOUBLE).build());
    profileVars
        .add(VariableSimpleBuilder.makeScalar(lonName, "profile longitude", CDM.LON_UNITS, DataType.DOUBLE).build());
    profileVars.add(VariableSimpleBuilder
        .makeScalar(profileTimeName, "nominal time of profile", timeUnit.getUdUnit(), DataType.DOUBLE)
        .addAttribute(CF.CALENDAR, timeUnit.getCalendar().toString()).build());

    profileVars.add(
        VariableSimpleBuilder.makeScalar(trajectoryIndexName, "trajectory index for this profile", null, DataType.INT)
            .addAttribute(CF.INSTANCE_DIMENSION, trajDimName).build());

    profileVars
        .add(VariableSimpleBuilder.makeScalar(numberOfObsName, "number of obs for this profile", null, DataType.INT)
            .addAttribute(CF.SAMPLE_DIMENSION, recordDimName).build());

    for (StructureData profileData : profileDataStructs) {
      for (StructureMembers.Member m : profileData.getMembers()) {
        VariableSimpleIF dv = getDataVar(m.getName());
        if (dv != null)
          profileVars.add(dv);
      }
    }

    if (isExtended) {
      profileStruct = (Structure) writer.addVariable(null, profileStructName, DataType.STRUCTURE, profileDimName);
      addCoordinatesExtended(profileStruct, profileVars);
    } else {
      addCoordinatesClassic(profileDim, profileVars, profileVarMap);
    }
  }

  private int profileRecno;

  public void writeProfileData(int sectionIndex, ProfileFeature profile, int nobs) throws IOException {
    trackBB(profile.getLatLon(), profile.getTime());

    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(latName, null, null, DataType.DOUBLE, profile.getLatLon().getLatitude());
    smb.addMemberScalar(lonName, null, null, DataType.DOUBLE, profile.getLatLon().getLongitude());
    // double time = (profile.getTime() != null) ? (double) profile.getTime().getTime() : 0.0;
    double timeInMyUnits = timeUnit.makeOffsetFromRefDate(profile.getTime());
    smb.addMemberScalar(profileTimeName, null, null, DataType.DOUBLE, timeInMyUnits); // LOOK time always exist?
    smb.addMemberString(profileIdName, null, null, profile.getName().trim(), id_strlen);
    smb.addMemberScalar(numberOfObsName, null, null, DataType.INT, nobs);
    smb.addMemberScalar(trajectoryIndexName, null, null, DataType.INT, sectionIndex);
    StructureData profileCoords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall =
        StructureDataComposite.create(ImmutableList.of(profileCoords, profile.getFeatureData()));
    profileRecno = super.writeStructureData(profileRecno, profileStruct, sdall, profileVarMap);
  }

  private int obsRecno;

  private void writeObsData(PointFeature pf) throws IOException {
    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(pf.getFeatureCollection().getTimeName(), null, null, DataType.DOUBLE, pf.getObservationTime());
    if (altUnits != null)
      smb.addMemberScalar(pf.getFeatureCollection().getAltName(), null, null, DataType.DOUBLE,
          pf.getLocation().getAltitude());
    StructureData coords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(coords, pf.getFeatureData()));
    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
