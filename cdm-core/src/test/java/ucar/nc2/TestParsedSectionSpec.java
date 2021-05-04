/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

/** Test {@link ucar.nc2.ParsedSectionSpec} */
public class TestParsedSectionSpec {
  @Test
  public void testVariableSection() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testWrite.nc")) {
      Variable v = ncfile.findVariable("temperature");
      assertThat(v).isNotNull();

      ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "temperature");
      System.out.printf("%s%n", spec);
      assertThat(spec.getSection()).isEqualTo(v.getShapeAsSection());

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "temperature(1,0:127:2)");
      System.out.printf("%s%n", spec);
      Section sect = new Section("1,0:127:2");
      assertThat(spec.getSection()).isEqualTo(sect);

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "temperature(:,0:127:2)");
      System.out.printf("%s%n", spec);
      sect = new Section("0:63,0:127:2");
      assertThat(spec.getSection()).isEqualTo(sect);

      String s = ParsedSectionSpec.makeSectionSpecString(v, ImmutableList.of(new Range(1, 1), new Range(0, 127, 2)));
      assertThat(s).isEqualTo("temperature(1:1, 0:126:2)");
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGroupAndMembers() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "formats/netcdf4/compound/simple_nc4.nc4")) {
      Variable v = ncfile.findVariable("grp1/data");
      assert v != null;

      ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "grp1/data");
      System.out.printf("%s%n", spec);
      assert spec.getSection().equals(v.getShapeAsSection());

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "grp2/data.i1");
      System.out.printf("%s%n", spec);

      Variable s = ncfile.findVariable("grp2/data");
      assert spec.getSection().equals(s.getShapeAsSection());

      v = ncfile.findVariable("grp2/data.i1");
      assert spec.getChild().getSection().equals(v.getShapeAsSection());
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEscaping() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "ncml/escapeNames.ncml")) {
      Group g = ncfile.findGroup("group.name");
      assert g != null;

      Variable v = g.findVariableLocal("var.name");
      assert v != null;

      Variable v2 = ncfile.findVariable("group.name/var.name");
      assert v2 == null;

      v2 = ncfile.findVariable("group\\.name/var\\.name");
      assert v2 != null;
      assert v2.equals(v);

      ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\.name/var\\.name");
      System.out.printf("%s%n", spec);
      assert spec.getSection().equals(v2.getShapeAsSection());

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\.name/var\\.name(1,0:0)");
      System.out.printf("%s%n", spec);
      Section s = new Section("1,0");
      assert spec.getSection().equals(s);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEscaping2() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "ncml/escapeNames.ncml")) {
      Group g = ncfile.findGroup("group(name");
      assert g != null;

      Variable v = g.findVariableLocal("var(name");
      assert v != null;

      Variable v2 = ncfile.findVariable("group(name/var(name");
      assert v2 != null;
      assert v2.equals(v);

      v2 = ncfile.findVariable("group\\(name/var\\(name");
      assert v2 != null;
      assert v2.equals(v);

      ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\(name/var\\(name");
      System.out.printf("%s%n", spec);
      assert spec.getSection().equals(v2.getShapeAsSection());

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\(name/var\\(name(1,0:0)");
      System.out.printf("%s%n", spec);
      Section s = new Section("1,0");
      assert spec.getSection().equals(s);
    }
  }

  @Test
  public void testVlen() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = NetcdfDatasets
        .openDataset(TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc")) {
      Variable v2 = ncfile.findVariable("catchments_x");
      assertThat(v2).isNotNull();
      ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "catchments_x");
      System.out.printf("%s%n", spec);
      assertThat(spec.getSection()).isEqualTo(v2.getShapeAsSection());

      Section section = spec.getSection();
      assertThat(section.getRank()).isEqualTo(2);
      assertThat(section.getRange(0)).isEqualTo(new Range(0, 2));
      assertThat(section.getRange(1)).isEqualTo(Range.VLEN);

      Section v2section = v2.getShapeAsSection();
      assertThat(v2section.getRank()).isEqualTo(2);
      assertThat(v2section.getRange(1)).isEqualTo(Range.VLEN);

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "catchments_x(1,:)");
      System.out.printf("%s%n", spec);
      section = spec.getSection();
      assertThat(section.getRank()).isEqualTo(2);
      assertThat(section.getRange(0)).isEqualTo(new Range(1, 1));
      assertThat(section.getRange(1)).isEqualTo(Range.VLEN);

      try {
        ParsedSectionSpec.parseVariableSection(ncfile, "catchments_x(1:1,0:2)");
        fail();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(InvalidRangeException.class);
      }
    }
  }

}
