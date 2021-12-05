/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Section;
import ucar.array.StructureDataArray;
import ucar.array.StructureMembers;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test StructureIterator works when opened with IOSP_MESSAGE_ADD_RECORD_STRUCTURE. */
@Category(NeedsCdmUnitTest.class)
public class TestStructureSubset {
  private NetcdfFile ncfile;

  @Before
  public void setUp() throws Exception {
    ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "ft/station/Surface_METAR_20080205_0000.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
  }

  @After
  public void tearDown() throws Exception {
    ncfile.close();
  }

  @Test
  public void testReadStructureSubset() throws IOException {
    Structure record = (Structure) ncfile.findVariable("record");
    assertThat(record).isNotNull();

    List<String> vars = new ArrayList<>();
    vars.add("wind_speed");
    vars.add("wind_gust");
    vars.add("report");
    Structure subset = record.select(vars);

    // read entire subset
    StructureDataArray dataAll = (StructureDataArray) subset.readArray();

    StructureMembers sm = dataAll.getStructureMembers();
    for (StructureMembers.Member m : sm.getMembers()) {
      Variable v = subset.findVariable(m.getName());
      assertThat(v).isNotNull();
      Array<?> mdata = dataAll.extractMemberArray(m);
      assertThat(mdata.getShape()[0]).isEqualTo(dataAll.getShape()[0]);
      assertThat(mdata.getArrayType()).isEqualTo(m.getArrayType());
      System.out.println(m.getName() + " shape=" + new Section(mdata.getShape()));
    }
    System.out.println("*** TestStructureSubset ok");
  }

  @Test
  public void testReadStructureSection() throws Exception {
    Structure record = (Structure) ncfile.findVariable("record");
    assertThat(record).isNotNull();
    Structure subset = (Structure) record.section(new Section("0:10"));
    assertThat(subset).isNotNull();
    assertThat(subset.getRank()).isEqualTo(1);
    assertThat(subset.getSize()).isEqualTo(11);

    // read entire subset
    StructureDataArray dataAll = (StructureDataArray) subset.readArray(new Section("0:10"));
    assertThat(dataAll.getSize()).isEqualTo(11);

    StructureMembers sm = dataAll.getStructureMembers();
    for (StructureMembers.Member m : sm.getMembers()) {
      Variable v = subset.findVariable(m.getName());
      assertThat(v).isNotNull();
      Array<?> mdata = dataAll.extractMemberArray(m);
      assertThat(mdata.getShape()[0]).isEqualTo(dataAll.getShape()[0]);
      assertThat(mdata.getArrayType()).isEqualTo(m.getArrayType());
      System.out.println(m.getName() + " shape=" + new Section(mdata.getShape()));
    }
    System.out.println("*** TestStructureSubset ok");
  }


}
