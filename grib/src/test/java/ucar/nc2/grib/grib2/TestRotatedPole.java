/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.projection.RotatedPole;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test reading GRIB2 files with {@link RotatedPole} projections.
 * 
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
@RunWith(JUnit4.class)
public class TestRotatedPole {

  /**
   * Tolerance for floating point comparisons.
   */
  public static final double DELTA = 1e-6;

  /**
   * Test reading an RAP native GRIB2 file with a GDS template 32769
   * {@link RotatedPole} projection.
   */
  @Test
  public void testRapNative() throws Exception {
    try (NetcdfFile nc = NetcdfFiles.open("../grib/src/test/data/rap-native.grib2")) {
      assertThat(nc).isNotNull();
      // check dimensions
      Dimension rlonDim = nc.findDimension("rlon");
      assertThat(rlonDim).isNotNull();
      assertThat(7).isEqualTo(rlonDim.getLength());
      Dimension rlatDim = nc.findDimension("rlat");
      assertThat(rlatDim).isNotNull();
      assertThat(5).isEqualTo(rlatDim.getLength());
      Dimension timeDim = nc.findDimension("time");
      assertThat(timeDim).isNotNull();
      assertThat(1).isEqualTo(timeDim.getLength());

      // check coordinate variables
      Variable rlonVar = nc.findVariable("rlon");
      assertThat(rlonVar).isNotNull();
      assertThat(1).isEqualTo(rlonVar.getDimensions().size());
      assertThat(rlonDim).isEqualTo(rlonVar.getDimensions().get(0));
      assertThat("grid_longitude").isEqualTo(rlonVar.findAttribute("standard_name").getStringValue());
      assertThat("degrees").isEqualTo(rlonVar.findAttribute("units").getStringValue());
      Array<Float> expecteds =
          Arrays.factory(ArrayType.FLOAT, new int[] {7}, new float[] {-30, -20, -10, 0, 10, 20, 30});
      Array<Float> actuals = (Array<Float>) rlonVar.readArray();
      assertThat(Arrays.equalFloats(expecteds, actuals)).isTrue();

      Variable rlatVar = nc.findVariable("rlat");
      assertThat(rlatVar).isNotNull();
      assertThat(1).isEqualTo(rlatVar.getDimensions().size());
      assertThat(rlatDim).isEqualTo(rlatVar.getDimensions().get(0));
      assertThat("grid_latitude").isEqualTo(rlatVar.findAttribute("standard_name").getStringValue());
      assertThat("degrees").isEqualTo(rlatVar.findAttribute("units").getStringValue());
      expecteds = Arrays.factory(ArrayType.FLOAT, new int[] {5}, new float[] {-20, -10, 0, 10, 20});
      actuals = (Array<Float>) rlatVar.readArray();
      assertThat(Arrays.equalFloats(expecteds, actuals)).isTrue();

      Variable timeVar = nc.findVariable("time");
      assertThat(timeVar).isNotNull();
      assertThat(1).isEqualTo(timeVar.getDimensions().size());
      assertThat(timeDim).isEqualTo(timeVar.getDimensions().get(0));
      assertThat("time").isEqualTo(timeVar.findAttribute("standard_name").getStringValue());
      assertThat("hours since 2016-04-25T22:00Z").isEqualTo(timeVar.findAttribute("units").getStringValue());
      Array<Double> expectedd = Arrays.factory(ArrayType.DOUBLE, new int[] {1}, new double[] {0});
      Array<Double> actuald = (Array<Double>) timeVar.readArray();
      assertThat(Arrays.equalDoubles(expectedd, actuald)).isTrue();

      // check projection variable
      Variable projVar = nc.findVariable("RotatedLatLon32769_Projection");
      assertThat(projVar).isNotNull();
      assertThat("rotated_latitude_longitude").isEqualTo(projVar.findAttribute("grid_mapping_name").getStringValue());
      assertThat(projVar.findAttribute("grid_north_pole_longitude").getNumericValue().doubleValue()).isWithin(DELTA)
          .of(74.0);
      assertThat(projVar.findAttribute("grid_north_pole_latitude").getNumericValue().doubleValue()).isWithin(DELTA)
          .of(36.0);
      // check data variable
      Variable dataVar = nc.findVariable("TMP_P0_L100_GLC0_surface");
      assertThat(dataVar).isNotNull();
      assertThat("RotatedLatLon32769_Projection").isEqualTo(dataVar.findAttribute("grid_mapping").getStringValue());
      assertThat("K").isEqualTo(dataVar.findAttribute("units").getStringValue());
      assertThat(3).isEqualTo(dataVar.getDimensions().size());
      assertThat(timeDim).isEqualTo(dataVar.getDimensions().get(0));
      assertThat(rlatDim).isEqualTo(dataVar.getDimensions().get(1));
      assertThat(rlonDim).isEqualTo(dataVar.getDimensions().get(2));
      float[] expected = new float[] {300, 299, 298, 297, 296, 295, 294, 299, 300, 299, 298, 297, 296, 295, 298, 299,
          300, 299, 298, 297, 296, 297, 298, 299, 300, 299, 298, 297, 296, 297, 298, 299, 300, 299, 298};
      expecteds = Arrays.factory(ArrayType.FLOAT, new int[] {expected.length}, expected);
      actuals = (Array<Float>) dataVar.readArray();
      assertThat(Arrays.equalFloats(expecteds, actuals)).isTrue();
    }
  }

  /**
   * Test reading a COSMO EU GRIB2 file with a GDS template 1
   * {@link RotatedPole} projection.
   */
  @Test
  public void testCosmoEu() throws Exception {
    try (NetcdfFile nc = NetcdfFiles.open("../grib/src/test/data/cosmo-eu.grib2")) {
      assertThat(nc).isNotNull();
      // check dimensions
      Dimension rlonDim = nc.findDimension("rlon");
      assertThat(rlonDim).isNotNull();
      assertThat(5).isEqualTo(rlonDim.getLength());
      Dimension rlatDim = nc.findDimension("rlat");
      assertThat(rlatDim).isNotNull();
      assertThat(5).isEqualTo(rlatDim.getLength());
      Dimension timeDim = nc.findDimension("time");
      assertThat(timeDim).isNotNull();
      assertThat(1).isEqualTo(timeDim.getLength());
      // check coordinate variables
      Variable rlonVar = nc.findVariable("rlon");
      assertThat(rlonVar).isNotNull();
      assertThat(1).isEqualTo(rlonVar.getDimensions().size());
      assertThat(rlonDim).isEqualTo(rlonVar.getDimensions().get(0));
      assertThat("grid_longitude").isEqualTo(rlonVar.findAttribute("standard_name").getStringValue());
      assertThat("degrees").isEqualTo(rlonVar.findAttribute("units").getStringValue());
      Array<Float> expecteds = Arrays.factory(ArrayType.FLOAT, new int[] {5}, new float[] {-18, -8, 2, 12, 22});
      Array<Float> actuals = (Array<Float>) rlonVar.readArray();
      assertThat(Arrays.equalFloats(expecteds, actuals)).isTrue();

      Variable rlatVar = nc.findVariable("rlat");
      assertThat(rlatVar).isNotNull();
      assertThat(1).isEqualTo(rlatVar.getDimensions().size());
      assertThat(rlatDim).isEqualTo(rlatVar.getDimensions().get(0));
      assertThat("grid_latitude").isEqualTo(rlatVar.findAttribute("standard_name").getStringValue());
      assertThat("degrees").isEqualTo(rlatVar.findAttribute("units").getStringValue());
      expecteds = Arrays.factory(ArrayType.FLOAT, new int[] {5}, new float[] {-20, -10, 0, 10, 20});
      actuals = (Array<Float>) rlatVar.readArray();
      assertThat(Arrays.equalFloats(expecteds, actuals)).isTrue();

      Variable timeVar = nc.findVariable("time");
      assertThat(timeVar).isNotNull();
      assertThat(1).isEqualTo(timeVar.getDimensions().size());
      assertThat(timeDim).isEqualTo(timeVar.getDimensions().get(0));
      assertThat("time").isEqualTo(timeVar.findAttribute("standard_name").getStringValue());
      assertThat("hours since 2010-03-29T00:00Z").isEqualTo(timeVar.findAttribute("units").getStringValue());
      Array<Double> expectedd = Arrays.factory(ArrayType.DOUBLE, new int[] {1}, new double[] {0});
      Array<Double> actuald = (Array<Double>) timeVar.readArray();
      assertThat(Arrays.equalDoubles(expectedd, actuald)).isTrue();

      // check projection variable
      Variable projVar = nc.findVariable("RotatedLatLon_Projection");
      assertThat(projVar).isNotNull();
      assertThat("rotated_latitude_longitude").isEqualTo(projVar.findAttribute("grid_mapping_name").getStringValue());
      assertThat(projVar.findAttribute("grid_north_pole_longitude").getNumericValue().doubleValue()).isWithin(DELTA)
          .of(-170.0);
      assertThat(projVar.findAttribute("grid_north_pole_latitude").getNumericValue().doubleValue()).isWithin(DELTA)
          .of(40.0);
      // check data variable
      Variable dataVar = nc.findVariable("Snow_depth_water_equivalent_surface");
      assertThat(dataVar).isNotNull();
      assertThat("RotatedLatLon_Projection").isEqualTo(dataVar.findAttribute("grid_mapping").getStringValue());
      assertThat("kg.m-2").isEqualTo(dataVar.findAttribute("units").getStringValue());
      assertThat(3).isEqualTo(dataVar.getDimensions().size());
      assertThat(timeDim).isEqualTo(dataVar.getDimensions().get(0));
      assertThat(rlatDim).isEqualTo(dataVar.getDimensions().get(1));
      assertThat(rlonDim).isEqualTo(dataVar.getDimensions().get(2));
      float[] expected = new float[] {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115,
          116, 117, 118, 119, 120, 121, 122, 123, 124};
      expecteds = Arrays.factory(ArrayType.FLOAT, new int[] {expected.length}, expected);
      actuals = (Array<Float>) dataVar.readArray();
      assertThat(Arrays.equalFloats(expecteds, actuals)).isTrue();
    }
  }

}
