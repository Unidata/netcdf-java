/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
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
import ucar.nc2.time.CalendarDateUnit;

/**
 * Write a CF "Discrete Sample" profile collection file.
 * Example H.3.5. Contiguous ragged array representation of profiles, H.3.4
 *
 * <p/>
 * 
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8372832"
 * @author caron
 * @since April, 2012
 */
class WriterCFProfileCollection extends WriterCFPointAbstract {

  ///////////////////////////////////////////////////
  private Structure profileStruct; // used for netcdf4 extended
  private HashSet<String> featureVarMap = new HashSet<>();
  private boolean headerDone;

  WriterCFProfileCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    rootGroup.addAttribute(
        new Attribute(CF.DSG_REPRESENTATION, "Contiguous ragged array representation of profiles, H.3.4"));
    rootGroup.addAttribute(new Attribute(CF.FEATURE_TYPE, CF.FeatureType.profile.name()));
  }

  @Override
  void finishBuilding() throws IOException {
    super.finishBuilding();
    profileStruct = findStructure(profileStructName);
  }

  int writeProfile(ProfileFeature profile) throws IOException {
    int count = 0;
    for (PointFeature pf : profile) {
      if (!headerDone) {
        if (id_strlen == 0)
          id_strlen = profile.getName().length() * 2;
        writeHeader(profile, pf);
        headerDone = true;
      }
      writeObsData(pf);
      count++;
    }

    writeProfileData(profile, count);
    return count;
  }

  private void writeHeader(ProfileFeature profile, PointFeature obs) throws IOException {

    Formatter coordNames = new Formatter().format("%s %s %s", profileTimeName, latName, lonName);
    List<VariableSimpleIF> coords = new ArrayList<>();
    if (useAlt) {
      coords.add(VariableSimpleBuilder.makeScalar(altitudeCoordinateName, "obs altitude", altUnits, DataType.DOUBLE)
          .addAttribute(CF.STANDARD_NAME, "altitude")
          .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(altitudeCoordinateName, altUnits)).build());
      coordNames.format(" %s", altitudeCoordinateName);
    }

    super.writeHeader(coords, profile.getFeatureData(), null, obs.getFeatureData(), coordNames.toString());
  }

  @Override
  protected void makeFeatureVariables(StructureData featureData, boolean isExtended) {

    // LOOK why not unlimited here ?
    Dimension profileDim = new Dimension(profileDimName, nfeatures);
    rootGroup.addDimension(profileDim);

    // add the profile Variables using the profile dimension
    List<VariableSimpleIF> profileVars = new ArrayList<>();
    profileVars
        .add(VariableSimpleBuilder.makeScalar(latName, "profile latitude", CDM.LAT_UNITS, DataType.DOUBLE).build());
    profileVars
        .add(VariableSimpleBuilder.makeScalar(lonName, "profile longitude", CDM.LON_UNITS, DataType.DOUBLE).build());
    profileVars.add(VariableSimpleBuilder.makeString(profileIdName, "profile identifier", null, id_strlen)
        .addAttribute(CF.CF_ROLE, CF.PROFILE_ID).build()); // profileId:cf_role = "profile_id";

    profileVars
        .add(VariableSimpleBuilder.makeScalar(numberOfObsName, "number of obs for this profile", null, DataType.INT)
            .addAttribute(CF.SAMPLE_DIMENSION, recordDimName).build()); // rowSize:sample_dimension = "obs"

    profileVars.add(VariableSimpleBuilder
        .makeScalar(profileTimeName, "nominal time of profile", timeUnit.getUdUnit(), DataType.DOUBLE)
        .addAttribute(CF.CALENDAR, timeUnit.getCalendar().toString()).build());


    for (StructureMembers.Member m : featureData.getMembers()) {
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
      addCoordinatesClassic(profileDim, profileVars, featureVarMap);
    }
  }

  private int profileRecno;

  private void writeProfileData(ProfileFeature profile, int nobs) throws IOException {
    trackBB(profile.getLatLon(), profile.getTime());

    StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
    smb.addMemberScalar(latName, null, null, DataType.DOUBLE, profile.getLatLon().getLatitude());
    smb.addMemberScalar(lonName, null, null, DataType.DOUBLE, profile.getLatLon().getLongitude());
    if (profile.getTime() != null) {// LOOK time not always part of profile
      smb.addMemberScalar(profileTimeName, null, null, DataType.DOUBLE,
          timeUnit.makeOffsetFromRefDate(profile.getTime()));
    }
    smb.addMemberString(profileIdName, null, null, profile.getName().trim(), id_strlen);
    smb.addMemberScalar(numberOfObsName, null, null, DataType.INT, nobs);
    StructureData profileCoords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall =
        StructureDataComposite.create(ImmutableList.of(profileCoords, profile.getFeatureData()));
    profileRecno = super.writeStructureData(profileRecno, profileStruct, sdall, featureVarMap);
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
