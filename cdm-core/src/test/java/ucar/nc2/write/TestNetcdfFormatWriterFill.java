/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.iosp.NetcdfFormatUtils;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test writing with fill values
 */
public class TestNetcdfFormatWriterFill {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testCreateWithFill() throws IOException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(Dimension.builder().setName("time").setIsUnlimited(true).build());
    Dimension latDim = writerb.addDimension("lat", 6);
    Dimension lonDim = writerb.addDimension("lon", 12);

    // define Variables
    writerb.addVariable("temperature", DataType.DOUBLE, "lat lon").addAttribute(new Attribute("units", "K"))
        .addAttribute(new Attribute("_FillValue", -999.9));

    writerb.addVariable("lat", DataType.DOUBLE, "lat");
    writerb.addVariable("lon", DataType.FLOAT, "lon");
    writerb.addVariable("shorty", DataType.SHORT, "lat");

    writerb.addVariable("rtemperature", DataType.INT, "time lat lon").addAttribute(new Attribute("units", "K"))
        .addAttribute(new Attribute("_FillValue", -9999));

    writerb.addVariable("rdefault", DataType.INT, "time lat lon");

    // add string-valued variables
    writerb.addVariable("svar", DataType.CHAR, "lat lon");
    writerb.addVariable("svar2", DataType.CHAR, "lat lon");

    // string array
    writerb.addDimension("names", 3);
    writerb.addDimension("svar_len", 80);
    writerb.addVariable("names", DataType.CHAR, "names svar_len");
    writerb.addVariable("names2", DataType.CHAR, "names svar_len");

    try (NetcdfFormatWriter writer = writerb.build()) {
      // write some data
      ArrayDouble A = new ArrayDouble.D3(1, latDim.getLength(), lonDim.getLength() / 2);
      int i, j;
      Index ima = A.getIndex();
      // write
      for (i = 0; i < latDim.getLength(); i++) {
        for (j = 0; j < lonDim.getLength() / 2; j++) {
          A.setDouble(ima.set(0, i, j), (i * 1000000 + j * 1000));
        }
      }

      int[] origin = new int[3];
      try {
        writer.write("rtemperature", origin, A);
      } catch (IOException e) {
        System.err.println("ERROR writing file");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }
    }

    //////////////////////////////////////////////////////////////////////
    // test reading, checking for fill values
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {

      Variable temp = ncfile.findVariable("temperature");
      assert (null != temp);

      Array tA = temp.read();
      assert (tA.getRank() == 2);

      Index ima = tA.getIndex();
      int[] shape = tA.getShape();

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          assert (tA.getDouble(ima.set(i, j)) == -999.9);
        }
      }

      Variable rtemp = ncfile.findVariable("rtemperature");
      assert (null != rtemp);

      Array rA = rtemp.read();
      assert (rA.getRank() == 3);

      ima = rA.getIndex();
      int[] rshape = rA.getShape();
      for (int i = 0; i < rshape[1]; i++) {
        for (int j = rshape[2] / 2 + 1; j < rshape[2]; j++) {
          assert (rA.getDouble(ima.set(0, i, j)) == -9999.0) : rA.getDouble(ima);
        }
      }

      Variable v = ncfile.findVariable("lat");
      assert (null != v);
      assert v.getDataType() == DataType.DOUBLE;

      Array data = v.read();
      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext()) {
        assert ii.getDoubleNext() == NetcdfFormatUtils.NC_FILL_DOUBLE;
      }

      v = ncfile.findVariable("lon");
      assert (null != v);
      data = v.read();
      ii = data.getIndexIterator();
      while (ii.hasNext()) {
        assert ii.getFloatNext() == NetcdfFormatUtils.NC_FILL_FLOAT;
      }

      v = ncfile.findVariable("shorty");
      assert (null != v);
      data = v.read();
      ii = data.getIndexIterator();
      while (ii.hasNext()) {
        assert ii.getShortNext() == NetcdfFormatUtils.NC_FILL_SHORT;
      }

      v = ncfile.findVariable("rdefault");
      assert (null != v);
      data = v.read();
      ii = data.getIndexIterator();
      while (ii.hasNext()) {
        assert ii.getIntNext() == NetcdfFormatUtils.NC_FILL_INT;
      }
    }
  }
}
