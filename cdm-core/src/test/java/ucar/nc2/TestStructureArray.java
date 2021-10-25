/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.unidata.util.test.TestDir;
import java.io.*;

/** Test reading record data */
public class TestStructureArray {

  private NetcdfFile ncfile;

  @Before
  public void setUp() throws Exception {
    // testStructures is 1 dimensional (nc2 record dimension)
    ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testStructures.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
  }

  @After
  public void tearDown() throws Exception {
    ncfile.close();
  }

  @Test
  public void testNames() {
    for (Variable v : ncfile.getVariables()) {
      System.out.println(" " + v.getShortName() + " == " + v.getFullName());
    }

    Structure record = (Structure) ncfile.findVariable("record");
    assertThat(record).isNotNull();

    for (Variable v : record.getVariables()) {
      assertThat("record." + v.getShortName()).isEqualTo(v.getFullName());
    }
  }

  @Test
  public void testReadTop() throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable("record");
    assert v != null;

    assert (v.getArrayType() == ArrayType.STRUCTURE);
    assert (v instanceof Structure);
    assert (v.getRank() == 1);
    assert (v.getSize() == 1000);

    Array<?> data = v.readArray(new Section(new int[] {4}, new int[] {3}));
    assert (data.getArrayType() == ArrayType.STRUCTURE);
    assert (data.getSize() == 3) : data.getSize();
    assert (data.getRank() == 1);
  }
}
