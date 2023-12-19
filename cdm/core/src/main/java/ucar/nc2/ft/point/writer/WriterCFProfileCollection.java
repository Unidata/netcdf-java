/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.*;
import ucar.ma2.*;
import ucar.nc2.*;
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
public class WriterCFProfileCollection extends CFPointWriter {

  ///////////////////////////////////////////////////
  // private Formatter coordNames = new Formatter();
  private Structure profileStruct; // used for netcdf4 extended
  private Map<String, Variable> featureVarMap = new HashMap<>();

  public WriterCFProfileCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    writer.addGroupAttribute(null,
        new Attribute(CF.DSG_REPRESENTATION, "Contiguous ragged array representation of profiles, H.3.4"));
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.profile.name()));
  }

  public int writeProfile(ProfileFeature profile) throws IOException {
    if (id_strlen == 0)
      id_strlen = profile.getName().length() * 2;
    int count = 0;
    for (PointFeature pf : profile) {
      writeObsData(pf);
      count++;
    }

    writeProfileData(profile, count);
    return count;
  }

  protected void writeHeader(List<ProfileFeature> profiles) throws IOException {
    List<VariableSimpleIF> coords = new ArrayList<>();
    List<StructureData> profileData = new ArrayList<>();

    for (ProfileFeature profile : profiles) {
      profileData.add(profile.getFeatureData());
      coords.add(VariableSimpleBuilder
          .makeScalar(profile.getTimeName(), "time of measurement", profile.getTimeUnit().getUdUnit(), DataType.DOUBLE)
          .addAttribute(CF.CALENDAR, profile.getTimeUnit().getCalendar().toString()).build());

      if (useAlt) {
        altitudeCoordinateName = profile.getAltName();
        coords.add(VariableSimpleBuilder.makeScalar(altitudeCoordinateName, "obs altitude", altUnits, DataType.DOUBLE)
            .addAttribute(CF.STANDARD_NAME, "altitude")
            .addAttribute(CF.POSITIVE, CF1Convention.getZisPositive(altitudeCoordinateName, altUnits)).build());
      }
    }

    super.writeHeader(coords, profiles, profileData, null);
  }

  protected void makeFeatureVariables(List<StructureData> featureDataStructs, boolean isExtended) {

    // LOOK why not unlimited here ?
    Dimension profileDim = writer.addDimension(null, profileDimName, nfeatures);
    // Dimension profileDim = isExtendedModel ? writer.addUnlimitedDimension(profileDimName) : writer.addDimension(null,
    // profileDimName, nprofiles);

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


    for (StructureData featureData : featureDataStructs) {
      for (StructureMembers.Member m : featureData.getMembers()) {
        VariableSimpleIF dv = getDataVar(m.getName());
        if (dv != null)
          profileVars.add(dv);
      }
    }

    if (isExtended) {
      profileStruct = (Structure) writer.addVariable(null, profileStructName, DataType.STRUCTURE, profileDimName);
      addCoordinatesExtended(profileStruct, profileVars);
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
    smb.addMemberScalar(pf.getFeatureCollection().getTimeName(), null, null, DataType.DOUBLE, pf.getObservationTime());
    if (useAlt)
      smb.addMemberScalar(pf.getFeatureCollection().getAltName(), null, null, DataType.DOUBLE,
          pf.getLocation().getAltitude());
    StructureData coords = new StructureDataFromMember(smb.build());

    // coords first so it takes precedence
    StructureDataComposite sdall = StructureDataComposite.create(ImmutableList.of(coords, pf.getFeatureData()));
    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
