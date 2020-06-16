/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.write.NetcdfFormatWriter;

public class TestRedefine3 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testRedefine3() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder writerb =
        NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false).setExtraHeader(64 * 1000);
    writerb.addDimension("time", 100);

    double[] jackData = new double[100];
    for (int i = 0; i < 100; i++)
      jackData[i] = i;
    double[] jillData = new double[100];
    for (int i = 0; i < 100; i++)
      jillData[i] = 2 * i;

    writerb.addVariable("jack", DataType.DOUBLE, "time").addAttribute(new Attribute("where", "up the hill"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      int[] start = new int[] {0};
      int[] count = new int[] {100};
      writer.write("jack", start, Array.factory(DataType.DOUBLE, count, jackData));

      writerb.addVariable("jill", DataType.DOUBLE, "time");
      Array jillArray = Array.factory(DataType.DOUBLE, count, jillData);
      try {
        writer.write("jill", start, jillArray);
        fail();
      } catch (Exception e) {
        assert e instanceof NullPointerException;
      }
    }

    try (NetcdfFile nc = NetcdfFiles.open(filename, null)) {
      Variable v = nc.findVariable("jill");
      assert v == null;
    }
  }
}
