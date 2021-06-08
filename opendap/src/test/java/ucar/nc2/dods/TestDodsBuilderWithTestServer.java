/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dods;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.Variable;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestDodsBuilderWithTestServer {
  private static final String testUrl =
      "https://thredds-test.unidata.ucar.edu/thredds/dodsC/grib/NCEP/GFS/CONUS_80km/Best";
  private static final String testCE = "time[0:42],reftime[0:1:14]";


  @Test
  public void testSubset() throws IOException {
    String datasetUrl = testUrl + "?" + testCE;
    try (DodsNetcdfFile dodsfile = DodsNetcdfFile.builder().build(datasetUrl, null)) {
      // DodsNetcdfFile dodsfile = TestDODSRead.open("test.02?i32[1:10],f64[2:2:10]");

      // int32
      Variable v = dodsfile.findVariable("time");
      assertThat(v).isNotNull();

      assertThat(v.getFullName()).isEqualTo("time");
      assertThat(v.getRank()).isEqualTo(1);
      assertThat(v.getSize()).isEqualTo(43);
      assertThat(v.getArrayType()).isEqualTo(ArrayType.DOUBLE);

      Array<?> a = v.readArray();
      assertThat(a.getRank()).isEqualTo(1);
      assertThat(Arrays.computeSize(a.getShape())).isEqualTo(163); // LOOK!
      assertThat(a.getArrayType()).isEqualTo(ArrayType.DOUBLE);

      int count = 1;
      Array<Double> ai = (Array<Double>) a;
      for (double val : ai) {
        assertThat(val).isEqualTo(count * 6.0);
        count++;
      }

      /*
       * // uint16
       * assert null == (v = dodsfile.findVariable("ui16"));
       * assert null == (v = dodsfile.findVariable("ui32"));
       * 
       * 
       * // double
       * assert (null != (v = dodsfile.findVariable("f64")));
       * assert v.getFullName().equals("f64");
       * assert v.getRank() == 1;
       * assert v.getSize() == 5;
       * assert v.getDataType() == DataType.DOUBLE : v.getDataType();
       * a = v.read();
       * assert a.getRank() == 1;
       * assert a.getSize() == 25;
       * assert a.getElementType() == double.class;
       * assert a instanceof ArrayDouble.D1;
       * ArrayDouble.D1 ad = (ArrayDouble.D1) a;
       * double[] tFloat64 =
       * new double[] {1.0, 0.9999500004166653, 0.9998000066665778, 0.9995500337489875, 0.9992001066609779,
       * 0.9987502603949663, 0.9982005399352042, 0.9975510002532796, 0.9968017063026194, 0.9959527330119943,
       * 0.9950041652780257, 0.9939560979566968, 0.9928086358538663, 0.9915618937147881, 0.9902159962126371,
       * 0.9887710779360422, 0.9872272833756269, 0.9855847669095608, 0.9838436927881214, 0.9820042351172703,
       * 0.9800665778412416, 0.9780309147241483, 0.9758974493306055, 0.9736663950053749, 0.9713379748520297};
       * 
       * for (int i = 0; i < 5; i++) {
       * double val = ad.get(i);
       * Assert2.assertNearlyEquals(val, tFloat64[i], 1.0e-9);
       * }
       */
    }
  }

}
