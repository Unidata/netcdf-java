/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/** Test {@link ucar.nc2.ParsedArraySectionSpec} */
public class TestParsedArraySectionSpec {
  @Test
  public void testVariableSection() throws Exception {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testWrite.nc")) {
      Variable v = ncfile.findVariable("temperature");
      assertThat(v).isNotNull();

      ParsedArraySectionSpec spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "temperature");
      System.out.printf("%s%n", spec);
      assertThat(spec.getSection()).isEqualTo(v.getSection());

      spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "temperature(1,0:127:2)");
      System.out.printf("%s%n", spec);
      Section sect = new Section("1,0:127:2");
      assertThat(spec.getSection()).isEqualTo(sect);

      spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "temperature(:,0:127:2)");
      System.out.printf("%s%n", spec);
      sect = new Section("0:63,0:127:2");
      assertThat(spec.getSection()).isEqualTo(sect);

      String s = ParsedArraySectionSpec.makeSectionSpecString(v,
          new Section(ImmutableList.of(new Range(1, 1), new Range(0, 127, 2))));
      assertThat(s).isEqualTo("temperature(1:1, 0:126:2)");
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGroupAndMembers() throws Exception {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "formats/netcdf4/compound/simple_nc4.nc4")) {
      Variable v = ncfile.findVariable("grp1/data");
      assertThat(v).isNotNull();

      ParsedArraySectionSpec spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "grp1/data");
      System.out.printf("%s%n", spec);
      assertThat(spec.getSection()).isEqualTo(v.getSection());

      spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "grp2/data.i1");
      System.out.printf("%s%n", spec);

      Variable s = ncfile.findVariable("grp2/data");
      assertThat(spec.getSection()).isEqualTo(s.getSection());

      v = ncfile.findVariable("grp2/data.i1");
      assertThat(spec.getChild().getSection()).isEqualTo(v.getSection());
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEscaping() throws Exception {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "ncml/escapeNames.ncml")) {
      Group g = ncfile.findGroup("group.name");
      assertThat(g).isNotNull();

      Variable v = g.findVariableLocal("var.name");
      assertThat(v).isNotNull();
      Variable v2 = ncfile.findVariable("group.name/var.name");
      assertThat(v2).isNull();

      v2 = ncfile.findVariable("group\\.name/var\\.name");
      assertThat(v2).isNotNull();
      assertThat(v2).isEqualTo(v);

      ParsedArraySectionSpec spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "group\\.name/var\\.name");
      System.out.printf("%s%n", spec);
      assertThat(spec.getSection()).isEqualTo(v2.getSection());

      spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "group\\.name/var\\.name(1,0:0)");
      System.out.printf("%s%n", spec);
      Section s = new Section("1,0");
      assertThat(spec.getSection()).isEqualTo(s);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEscaping2() throws Exception {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "ncml/escapeNames.ncml")) {
      Group g = ncfile.findGroup("group(name");
      assertThat(g).isNotNull();

      Variable v = g.findVariableLocal("var(name");
      assertThat(v).isNotNull();

      Variable v2 = ncfile.findVariable("group(name/var(name");
      assertThat(v2).isNotNull();
      assertThat(v2).isEqualTo(v);

      v2 = ncfile.findVariable("group\\(name/var\\(name");
      assertThat(v2).isNotNull();
      assertThat(v2).isEqualTo(v);

      ParsedArraySectionSpec spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "group\\(name/var\\(name");
      System.out.printf("%s%n", spec);
      assertThat(spec.getSection()).isEqualTo(v2.getSection());

      spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "group\\(name/var\\(name(1,0:0)");
      System.out.printf("%s%n", spec);
      Section s = new Section("1,0");
      assertThat(spec.getSection()).isEqualTo(s);
    }
  }

  @Test
  public void testVlen() throws Exception {
    try (NetcdfFile ncfile = NetcdfDatasets
        .openDataset(TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc")) {
      Variable v2 = ncfile.findVariable("catchments_x");
      assertThat(v2).isNotNull();
      ParsedArraySectionSpec spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "catchments_x");
      System.out.printf("%s%n", spec);
      assertThat(spec.getSection()).isEqualTo(v2.getSection());

      Section section = spec.getSection();
      assertThat(section.getRank()).isEqualTo(2);
      assertThat(section.getRange(0)).isEqualTo(new Range(0, 2));
      assertThat(section.getRange(1)).isEqualTo(Range.VLEN);

      Section v2section = v2.getSection();
      assertThat(v2section.getRank()).isEqualTo(2);
      assertThat(v2section.getRange(1)).isEqualTo(Range.VLEN);

      spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "catchments_x(1,:)");
      System.out.printf("%s%n", spec);
      section = spec.getSection();
      assertThat(section.getRank()).isEqualTo(2);
      assertThat(section.getRange(0)).isEqualTo(new Range(1, 1));
      assertThat(section.getRange(1)).isEqualTo(Range.VLEN);

      try {
        ParsedArraySectionSpec.parseVariableSection(ncfile, "catchments_x(1:1,0:2)");
        fail();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(InvalidRangeException.class);
      }
    }
  }

}
