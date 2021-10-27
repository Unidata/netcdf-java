/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/** Test AggExisting with Unsigned Byte */

public class TestAggUnsignedByte {

  private final String AGG_FILENAME = "aggUbyte.ncml";
  private final String UBYTE_VAR_NAME = "ir_anvil_detection";

  private NetcdfFile ncfile;
  private Variable v;

  @Before
  public void prepAggDataset() {
    String filename = "file:./" + TestNcmlRead.topDir + AGG_FILENAME;
    try {
      ncfile = NcmlReader.readNcml(filename, null, null).build();
      v = ncfile.findVariable(UBYTE_VAR_NAME);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Test reading entire unsigned variable
   *
   * @throws IOException
   */
  @Test
  public void testIsUnsignedRead() throws IOException {
    // this worked as of 4.6.7, so no bug here...
    // assert v.isUnsigned();

    Array data = v.readArray();
    // this is the failure for https://github.com/Unidata/thredds/issues/695
    assert data.getArrayType().isUnsigned();
  }

  /**
   * Test reading a section of data from an unsigned variable
   * 
   * @throws IOException
   * @throws InvalidRangeException
   */
  @Test
  public void testIsUnsignedReadSection() throws IOException, InvalidRangeException {
    // this worked as of 4.6.7, so no bug here...
    // assert v.isUnsigned();

    int[] shape = new int[] {1, 10, 20};
    Section section = new Section(shape);
    Array data = v.readArray(section);
    // this is the failure for https://github.com/Unidata/thredds/issues/695
    assert data.getArrayType().isUnsigned();
  }

  /**
   * close out agg dataset when tests are finished
   */
  @After
  public void closeAggDataset() {
    try {
      ncfile.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
