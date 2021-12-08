/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

/** Test that redefine no longer supported by NetcdfFormatWriter. */
public class TestRedefine3 {

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testRedefine3() throws Exception {
    String filename = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder<?> writerb =
        NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false).setExtraHeader(64 * 1000);
    writerb.addDimension("time", 100);

    double[] jackData = new double[100];
    for (int i = 0; i < 100; i++)
      jackData[i] = i;
    double[] jillData = new double[100];
    for (int i = 0; i < 100; i++)
      jillData[i] = 2 * i;

    writerb.addVariable("jack", ArrayType.DOUBLE, "time").addAttribute(new Attribute("where", "up the hill"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      int[] count = new int[] {100};
      writer.write("jack", Index.ofRank(1), Arrays.factory(ArrayType.DOUBLE, count, jackData));

      writerb.addVariable("jill", ArrayType.DOUBLE, "time");
      Array<?> jillArray = Arrays.factory(ArrayType.DOUBLE, count, jillData);
      try {
        writer.write("jill", Index.ofRank(1), jillArray);
        fail();
      } catch (Exception e) {
        assert e instanceof NullPointerException;
      }
    }

    try (NetcdfFile nc = NetcdfFiles.open(filename, null)) {
      Variable v = nc.findVariable("jill");
      assertThat(v).isNull();
    }
  }
}
