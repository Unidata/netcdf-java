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
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataComposite;
import ucar.ma2.StructureDataFromMember;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Structure;
import ucar.nc2.VariableSimpleBuilder;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.TrajectoryProfileFeature;
import ucar.nc2.time.CalendarDateUnit;

/**
 * Write a CF "Discrete Sample" trajectory profile (section) collection file.
 * Contiguous ragged array representation of trajectory profile, H.6.3
 */
class WriterCFTrajectoryProfileCollection extends WriterCFPointAbstract {
  private static final String trajectoryIndexName = "trajectoryIndex";

  private int ntraj;
  private int traj_strlen;
  private Structure trajStructure; // used for netcdf4 extended
  private HashMap<String, Integer> trajIndexMap;

  private HashSet<String> trajVarMap = new HashSet<>();

  ///////////////////////////////////////////////////
  private Structure profileStruct; // used for netcdf4 extended
  private HashSet<String> profileVarMap = new HashSet<>();
  private boolean headerDone;

  WriterCFTrajectoryProfileCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    rootGroup.addAttribute(new Attribute(CF.FEATURE_TYPE, CF.FeatureType.trajectoryProfile.name()));
    rootGroup.addAttribute(
        new Attribute(CF.DSG_REPRESENTATION, "Contiguous ragged array representation of trajectory profile, H.6.3"));
  }

  void setFeatureAuxInfo2(int ntraj, int traj_strlen) {
    this.ntraj = ntraj;
    this.traj_strlen = traj_strlen;
    trajIndexMap = new HashMap<>(2 * ntraj);
  }

  int writeProfile(TrajectoryProfileFeature section, ProfileFeature profile) throws IOException {
    int count = 0;
    for (PointFeature pf : profile) {
      if (!headerDone) {
        if (id_strlen == 0)
          id_strlen = profile.getName().length() * 2;
        writeHeader(section, profile, pf);
        headerDone = true;
      }
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

  private void writeHeader(TrajectoryProfileFeature section, ProfileFeature profile, PointFeature obs)
      throws IOException {

    StructureData sectionData = section.getFeatureData();
    StructureData profileData = profile.getFeatureData();
    StructureData obsData = obs.getFeatureData();

    Formatter coordNames = new Formatter().format("%s %s %s", profileTimeName, latName, lonName);
    List<VariableSimpleIF> obsCoords = new ArrayList<>();
    if (useAlt) {
      obsCoords.add(VariableSimpleBuilder.makeScalar(altitudeCoordinateName, "obs altitude", altUnits, DataType.DOUBLE)
          .addAttribute(CF.STANDARD_NAME, "altitude")
          .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(altitudeCoordinateName, altUnits)).build());
      coordNames.format(" %s", altitudeCoordinateName);
    }

    super.writeHeader(obsCoords, sectionData, profileData, obsData, coordNames.toString());
  }

  @Override
  void makeFeatureVariables(StructureData trajData, boolean isExtended) {
    // add the dimensions : extended model can use an unlimited dimension
    Dimension trajDim = new Dimension(trajDimName, ntraj);
    rootGroup.addDimension(trajDim);

    List<VariableSimpleIF> trajVars = new ArrayList<>();

    trajVars.add(VariableSimpleBuilder.makeString(trajIdName, "trajectory identifier", null, traj_strlen)
        .addAttribute(CF.CF_ROLE, CF.TRAJECTORY_ID).build());

    for (StructureMembers.Member m : trajData.getMembers()) {
      if (findDataVar(m.getName()) != null)
        trajVars.add(VariableSimpleBuilder.fromMember(m).build());
    }

    if (isExtended) {
      Structure.Builder structb =
          Structure.builder().setName(trajStructName).setParentGroupBuilder(rootGroup).setDimensionsByName(trajDimName);
      rootGroup.addVariable(structb);
      addCoordinatesExtended(structb, trajVars);
    } else {
      addCoordinatesClassic(trajDim, trajVars, trajVarMap);
    }

  }

  @Override
  void finishBuilding() throws IOException {
    super.finishBuilding();
    profileStruct = findStructure(profileStructName);
    trajStructure = findStructure(trajStructName);
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
  protected void makeMiddleVariables(StructureData profileData, boolean isExtended) {
    Dimension profileDim = new Dimension(profileDimName, nfeatures);
    rootGroup.addDimension(profileDim);

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

    for (StructureMembers.Member m : profileData.getMembers()) {
      VariableSimpleIF dv = findDataVar(m.getName());
      if (dv != null)
        profileVars.add(dv);
    }

    if (isExtended) {
      Structure.Builder structb = Structure.builder().setName(profileStructName).setParentGroupBuilder(rootGroup)
          .setDimensionsByName(profileDimName);
      rootGroup.addVariable(structb);
      addCoordinatesExtended(structb, profileVars);
    } else {
      addCoordinatesClassic(profileDim, profileVars, profileVarMap);
    }
  }

  private int profileRecno;

  private void writeProfileData(int sectionIndex, ProfileFeature profile, int nobs) throws IOException {
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
    if (useAlt)
      smb.addMemberScalar(altitudeCoordinateName, null, null, DataType.DOUBLE, pf.getLocation().getAltitude());
    StructureData coords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(coords, pf.getFeatureData()));
    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
