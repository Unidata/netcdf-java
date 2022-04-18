package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

public class TestParsedSectionSpec {

  @Test
  public void testVariableSection() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalFromTestDataDir + "testWrite.nc");
    Variable v = ncfile.findVariable("temperature");
    assert v != null;

    ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "temperature");
    System.out.printf("%s%n", spec);
    assert spec.section.equals(v.getShapeAsSection());

    spec = ParsedSectionSpec.parseVariableSection(ncfile, "temperature(1,0:127:2)");
    System.out.printf("%s%n", spec);
    Section s = new Section("1,0:127:2");
    assert spec.section.equals(s) : spec.section + " != " + s;

    spec = ParsedSectionSpec.parseVariableSection(ncfile, "temperature(:,0:127:2)");
    System.out.printf("%s%n", spec);
    s = new Section("0:63,0:127:2");
    assert spec.section.equals(s) : spec.section + " != " + s;

    ncfile.close();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGroupAndMembers() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "formats/netcdf4/compound/simple_nc4.nc4")) {
      Variable v = ncfile.findVariable("grp1/data");
      assert v != null;

      ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "grp1/data");
      System.out.printf("%s%n", spec);
      assert spec.section.equals(v.getShapeAsSection());

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "grp2/data.i1");
      System.out.printf("%s%n", spec);

      Variable s = ncfile.findVariable("grp2/data");
      assert spec.section.equals(s.getShapeAsSection());

      v = ncfile.findVariable("grp2/data.i1");
      assert spec.child.section.equals(v.getShapeAsSection());
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
      assert spec.section.equals(v2.getShapeAsSection());

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\.name/var\\.name(1,0:0)");
      System.out.printf("%s%n", spec);
      Section s = new Section("1,0");
      assert spec.section.equals(s);
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
      assert spec.section.equals(v2.getShapeAsSection());

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\(name/var\\(name(1,0:0)");
      System.out.printf("%s%n", spec);
      Section s = new Section("1,0");
      assert spec.section.equals(s);
    }
  }

  @Test
  public void testVlen() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = NetcdfDatasets
        .openDataset(TestDir.cdmLocalFromTestDataDir + "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc")) {
      Variable v2 = ncfile.findVariable("catchments_x");
      assert v2 != null;
      ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "catchments_x");
      System.out.printf("%s%n", spec);
      assertThat(spec.section).isEqualTo(v2.getShapeAsSection());

      Section section = spec.section;
      assertThat(section.getRank()).isEqualTo(2);
      assertThat(section.getRange(0)).isEqualTo(new Range(0, 2));
      assertThat(section.getRange(1)).isEqualTo(Range.VLEN);

      Section v2section = v2.getShapeAsSection();
      assertThat(v2section.getRank()).isEqualTo(2);
      assertThat(v2section.getRange(1)).isEqualTo(Range.VLEN);

      spec = ParsedSectionSpec.parseVariableSection(ncfile, "catchments_x(1,:)");
      System.out.printf("%s%n", spec);
      section = spec.section;
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
