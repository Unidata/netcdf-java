/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestParsedSectionSpec {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "formats/netcdf4/compound/simple_nc4.nc4");
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

    ncfile.close();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEscaping() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "ncml/escapeNames.ncml");
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

    ncfile.close();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEscaping2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "ncml/escapeNames.ncml");
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

    ncfile.close();
  }

}
