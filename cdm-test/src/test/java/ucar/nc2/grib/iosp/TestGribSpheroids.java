/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.iosp;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by rmay on 5/22/15.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribSpheroids {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  String dir = TestDir.cdmUnitTestDir + "formats/grib2/";

  @Test
  public void code0_assume_spherical() throws IOException {
    String filename = dir + "grid174_scanmode_64_example.grb2";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.findVariable("LatLon_Projection");
      Attribute axis = v.findAttribute("earth_radius");
      assertThat(axis).isNotNull();
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6367470.);
    }
  }

  @Test
  public void code1_spherical_specified() throws IOException {
    String filename = dir + "LDUE18.grib2";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.findVariable("LambertConformal_Projection");
      Attribute axis = v.findAttribute("earth_radius");
      assertThat(axis).isNotNull();
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6371200.);
    }
  }

  // Exercises code path that corrects bad values of earth radius
  @Test
  public void code1_spherical_specified_bad() throws IOException {
    String filename = dir + "sfc_d01_20080430_1200_f00000.grb2";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.findVariable("LambertConformal_Projection");
      Attribute axis = v.findAttribute("earth_radius");
      assertThat(axis).isNotNull();
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6371200.);
    }
  }

  @Test
  public void code2_assume_oblate_iau() throws IOException {
    String filename = dir + "MESH_20070326-162126.grib";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.findVariable("LatLon_Projection");
      Attribute axis = v.findAttribute("semi_major_axis");
      assertThat(axis).isNotNull();
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6378160.);
      axis = v.findAttribute("semi_minor_axis");
      assertThat(axis).isNotNull();
      // We use the inverse flattening, not the specified minor
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6356684.7);
    }
  }

  @Test
  public void code3_oblate_specified_km() throws IOException {
    String filename = dir + "Eumetsat.VerticalPerspective.grb";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.findVariable("SpaceViewPerspective_Projection");
      Attribute axis = v.findAttribute("semi_major_axis");
      assertThat(axis).isNotNull();
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6378140.);
      axis = v.findAttribute("semi_minor_axis");
      assertThat(axis).isNotNull();
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6356755.);
    }
  }

  @Test
  public void code5_assume_WGS84() throws IOException {
    String filename = dir + "Albers_viirs_s.grb2";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.findVariable("AlbersEqualArea_Projection");
      Attribute axis = v.findAttribute("semi_major_axis");
      assertThat(axis).isNotNull();
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6378137.);
    }
  }

  @Test
  public void code6_assume_spherical() throws IOException {
    String filename = dir + "berkes.grb2";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Group grp = ncfile.getRootGroup().getGroups().get(0);
      Variable v = grp.findVariableLocal("LatLon_Projection");
      Attribute axis = v.findAttribute("earth_radius");
      assertThat(axis).isNotNull();
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6371229.);
    }
  }

  @Test
  public void code7_oblate_specified_m() throws IOException {
    String filename = dir + "TT_FC_INCA.grb2";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.findVariable("LambertConformal_Projection");
      Attribute axis = v.findAttribute("semi_major_axis");
      assertThat(axis).isNotNull();
      assertThat(axis.getNumericValue().doubleValue()).isWithin(0.1).of(6377397.);
    }
  }
}
