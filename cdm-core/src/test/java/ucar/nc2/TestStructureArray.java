/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Section;
import ucar.unidata.util.test.TestDir;

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
    for (Variable v : ncfile.getAllVariables()) {
      System.out.println(" " + v.getShortName() + " =" + v.getFullName());
    }

    Structure record = (Structure) ncfile.findVariable("record");
    assertThat(record).isNotNull();

    for (Variable v : record.getVariables()) {
      assertThat("record." + v.getShortName()).isEqualTo(v.getFullName());
    }
  }

  @Test
  public void testReadTop() throws Exception {
    Variable v = ncfile.findVariable("record");
    assertThat(v).isNotNull();

    assertThat(v.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(v instanceof Structure);
    assertThat(v.getRank()).isEqualTo(1);
    assertThat(v.getSize()).isEqualTo(1000);

    Array<?> data = v.readArray(new Section(new int[] {4}, new int[] {3}));
    assertThat(data.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(data.getSize()).isEqualTo(3);
    assertThat(data.getRank()).isEqualTo(1);
  }
}
