/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.grib2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.array.Array;
import ucar.array.IndexFn;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class TestDataTemplate {
  // Tests reading data using template 5.0
  @Test
  public void testDrs0() throws IOException {
    final String testfile = "../grib/src/test/data/Eumetsat.VerticalPerspective.grib2";
    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      Variable var = nc.findVariable("Pixel_scene_type");
      assertThat(var).isNotNull();
      Array<Float> data = (Array<Float>) var.readArray();

      assertThat(data.getScalar()).isNaN();
      assertThat(data.get(0, 584, 632)).isWithin(1e-6f).of(101.0f);
    }
  }

  // Tests reading data using template 5.2
  @Test
  public void testDrs2() throws IOException {
    final String testfile = "../grib/src/test/data/ds.snow.grib2";
    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      Variable var = nc.findVariable("Total_snowfall_surface_6_Hour_Accumulation");
      assertThat(var).isNotNull();
      Array<Float> data = (Array<Float>) var.readArray();

      assertThat(data.getScalar()).isNaN();
      IndexFn indexfn = IndexFn.builder(data.getShape()).build();
      assertThat(data.get(indexfn.odometer(1234))).isNaN();
    }
  }

  // Tests reading data using template 5.3
  @Test
  public void testDrs3() throws IOException {
    final String testfile = "../grib/src/test/data/ds.sky.grib2";
    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      Variable var = nc.findVariable("Total_cloud_cover_surface");
      assertThat(var).isNotNull();
      Array<Float> data = (Array<Float>) var.readArray();

      assertThat(data.getScalar()).isNaN();
      assertThat(data.get(0, 37, 114)).isWithin(1e-6f).of(23.0f);
    }
  }

  // Tests reading data using template 5.40
  @Test
  public void testDrs40() throws IOException {
    final String testfile = "../grib/src/test/data/pdsScale.pds1.grib2";
    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      Variable var = nc.findVariable("Temperature_isobaric_ens");
      assertThat(var).isNotNull();

      Array<Float> data = (Array<Float>) var.readArray();
      assertThat(data.getScalar()).isWithin(1e-6f).of(263.57705688f);
      IndexFn indexfn = IndexFn.builder(data.getShape()).build();
      assertThat(data.get(indexfn.odometer(1234))).isWithin(1e-6f).of(263.70205688f);
    }
  }

  // Tests reading data using template 5.41
  @Test
  public void testPng() throws IOException {
    final String testfile = "../grib/src/test/data/MRMS_LowLevelCompositeReflectivity_00.50_20141207-072038.grib2";
    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      Variable var = nc.findVariable("LowLevelCompositeReflectivity_altitude_above_msl");
      assertThat(var).isNotNull();

      Array<Float> data = (Array<Float>) var.readArray();
      assertThat(data.get(0, 0, 0, 15)).isWithin(1e-6f).of(-99f);
      IndexFn indexfn = IndexFn.builder(data.getShape()).build();
      assertThat(data.get(indexfn.odometer(5602228))).isWithin(1e-6f).of(18.5f);
    }
  }

  // Tests reading data using template 5.41 with a bitmap
  @Test
  public void testPngBitmap() throws IOException {
    final String testfile = "../grib/src/test/data/HLYA10.grib2";
    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      Variable var = nc.findVariable("VAR0-19-223_FROM_7-212--1_isobaric");
      assertThat(var).isNotNull();

      Array<Float> data = (Array<Float>) var.readArray();
      assertThat(data.get(0, 0, 0, 13)).isWithin(1e-6f).of(0.36976f);
      assertThat(data.get(0, 0, 0, 15)).isNaN();
    }
  }
}
