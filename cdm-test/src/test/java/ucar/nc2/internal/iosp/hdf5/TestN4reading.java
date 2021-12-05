/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.ArrayType;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.array.Array;
import ucar.array.StructureData;
import ucar.array.StructureDataArray;
import ucar.array.StructureMembers;
import ucar.nc2.*;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.nc2.util.Misc;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * Test netcdf-4 reading of misc files
 */
@Category(NeedsCdmUnitTest.class)
public class TestN4reading {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static String testDir = TestDir.cdmUnitTestDir + "formats/netcdf4/";

  @Test
  public void testGodivaFindsDataHole() throws Exception {
    // this pattern of reads from godiva is finding a data hole - missing data where there shouldn't be any
    Section[] sections = {new Section("14:14,0:0,13:170,0:20"), new Section("14:14,0:0,170:194,21:167"),
        new Section("14:14,0:0,170:194,168:294"), new Section("14:14,0:0,13:170,21:167"),
        new Section("14:14,0:0,170:194,0:20"), new Section("14:14,0:0,0:0,0:0"),
        new Section("14:14,0:0,13:170,168:294"), new Section("14:14,0:0,0:0,0:0"), new Section("14:14,0:0,0:0,0:0"),
        new Section("14:14,0:0,0:12,0:20"), new Section("14:14,0:0,0:12,21:167"),
        new Section("14:14,0:0,0:12,168:294"),};

    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = testDir + "hiig_forec_20140208.nc";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable v = ncfile.findVariable("temp");
      for (Section sect : sections) {
        Array data = v.readArray(sect);
        if (0 < countMissing(data)) {
          Array data2 = v.readArray(sect);
          countMissing(data2);
          assert false;
        }
      }
      logger.debug("**** testGodivaFindsDataHole read ok on {}", ncfile.getLocation());
    }
  }

  private int countMissing(Array<Number> data) {
    int count = 0;
    for (Number val : data) {
      float fval = val.floatValue();
      if (fval == NetcdfFormatUtils.NC_FILL_FLOAT) {
        count++;
      }
    }
    return count;
  }


  @Test
  public void testMultiDimscale() throws IOException {
    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = testDir + "multiDimscale.nc4";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable v = ncfile.findVariable("siglev");
      v.readArray();
      v = ncfile.findVariable("siglay");
      v.readArray();
      logger.debug("**** testMultiDimScale read ok\n{}", ncfile);
    }
  }

  @Test
  public void testGlobalHeapOverun() throws IOException {
    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = testDir + "globalHeapOverrun.nc4";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      logger.debug("**** testGlobalHeapOverun done\n{}", ncfile);
      List<Variable> vars = ncfile.getAllVariables().stream().sorted().collect(Collectors.toList());
      for (Variable v : vars)
        logger.debug("  {}", v.getFullName());
      logger.debug("nvars = {}", ncfile.getAllVariables().size());
    }
  }

  @Test
  public void testEnums() throws IOException {
    // H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    String filename = testDir + "tst/tst_enums.nc";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      logger.debug("**** testReadNetcdf4 done\n{}", ncfile);
      List<Variable> vars = ncfile.getAllVariables().stream().sorted().collect(Collectors.toList());
      for (Variable v : vars)
        logger.debug("  {}", v.getFullName());
      logger.debug("nvars = {}", ncfile.getAllVariables().size());
    }
  }


  @Test
  public void testVlenStrings() throws IOException {
    // H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    String filename = testDir + "tst/tst_strings.nc";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      logger.debug("**** testReadNetcdf4 done\n{}", ncfile);
      Variable v = ncfile.findVariable("measure_for_measure_var");
      Array data = v.readArray();
      logger.debug(NcdumpArray.printArray(data, "measure_for_measure_var", null));
    }
  }

  @Test
  public void testVlen() throws Exception {
    String filename = testDir + "vlen/fpcs_1dwave_2.nc";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      logger.debug("**** testVlen open\n{}", ncfile);
      Variable v = ncfile.findVariable("levels");
      Array data = v.readArray();
      System.out.printf("%s%n", NcdumpArray.printArray(data, "read()", null));

      int count = 0;
      for (Object o : data) {
        count++;
      }
      assertThat(count).isEqualTo(10);

      // try subset
      Array data2 = v.readArray(new Section("0:9:2, :"));
      assertThat(data2.getShape()).isEqualTo(new int[] {5});
      assertThat(data2.getSize()).isEqualTo(5);

      // from bruno
      Array<Short> data3 = (Array<Short>) v.readArray(new Section("5:5,:"));
      assertThat(data3.getSize()).isEqualTo(10);
      short[] expected = new short[] {6, 6, 6, 7, 8, 9, 10, 11, 12, 13};
      int i = 0;
      for (Short val : data3) {
        assertThat(val).isEqualTo(expected[i++]);
      }
    }
  }

  @Test
  public void testVlen2() throws Exception {
    String filename = testDir + "vlen/tst_vlen_data.nc4";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      logger.debug("**** testVlen2 open\n{}", ncfile);
      Variable v = ncfile.findVariable("ragged_array");
      Array data = v.readArray();

      for (Object o : data) {
        Array vdata = (Array) o;
        assertThat(vdata.getArrayType()).isEqualTo(ArrayType.FLOAT);
      }

      // try subset
      data = v.readArray(new Section("0:4:2,:"));
      for (Object o : data) {
        Array vdata = (Array) o;
        assertThat(vdata.getArrayType()).isEqualTo(ArrayType.FLOAT);
      }

      assertThrows(InvalidRangeException.class, () -> v.readArray(new Section(":,0")));
    }

  }

  /*
   * netcdf Q:/cdmUnitTest/formats/netcdf4/testNestedStructure.nc {
   * variables:
   * Structure {
   * Structure {
   * int x;
   * int y;
   * } field1;
   * Structure {
   * int x;
   * int y;
   * } field2;
   * } x;
   * }
   */
  @Test
  public void testNestedStructure() throws Exception {
    String filename = testDir + "testNestedStructure.nc";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {

      Variable dset = ncfile.findVariable("x");
      assert (null != ncfile.findVariable("x"));
      assert (dset.getArrayType() == ArrayType.STRUCTURE);
      assert (dset.getRank() == 0);
      assert (dset.getSize() == 1);

      StructureDataArray data = (StructureDataArray) dset.readArray();
      StructureMembers.Member m = data.getStructureMembers().findMember("field2");
      assert m != null;
      assert (m.getArrayType() == ArrayType.STRUCTURE);

      logger.debug("{}", NcdumpArray.printArray(data, "", null));

    }
    logger.debug("*** testNestedStructure ok");
  }

  @Test
  public void testStrings() throws IOException {
    // H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    String filename = testDir + "files/nc_test_netcdf4.nc4";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      logger.debug("**** testReadNetcdf4 done\n{}", ncfile);
      Variable v = ncfile.findVariable("d");
      String attValue = v.findAttributeString("c", null);
      logger.debug(" d:c = ({}) = {}", attValue, Arrays.toString(attValue.getBytes(StandardCharsets.UTF_8)));
    }
  }

  @Test
  public void testAttStruct() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestN4reading.testDir + "attributeStruct.nc")) {
      Variable v = ncfile.findVariable("observations");
      assert v != null;
      assert v instanceof Structure;

      Structure s = (Structure) v;
      Variable v2 = s.findVariable("tempMin");
      assert v2 != null;
      assert v2.getArrayType() == ArrayType.FLOAT;

      assert null != v2.findAttribute("units");
      assert null != v2.findAttribute("coordinates");

      Attribute att = v2.findAttribute("units");
      assert att.getStringValue().equals("degF");
    }
  }

  @Test
  public void testAttStruct2() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestN4reading.testDir + "compound-attribute-test.nc")) {
      Variable v = ncfile.findVariable("compound_test");
      assert v != null;
      assert v instanceof Structure;

      Structure s = (Structure) v;
      Variable v2 = s.findVariable("field0");
      assert v2 != null;
      assert v2.getArrayType() == ArrayType.FLOAT;

      Attribute att = v2.findAttribute("att_primitive_test");
      assert !att.isString();
      assert att.getNumericValue().floatValue() == 1.0;

      att = v2.findAttribute("att_string_test");
      assert att.getStringValue().equals("string for field 0");

      att = v2.findAttribute("att_char_array_test");
      String result = att.getStringValue();
      assertThat(att.getStringValue()).isEqualTo("a");
    }
  }

  @Test
  public void testEmptyAtts() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestN4reading.testDir + "testEmptyAtts.nc")) {
      logger.debug("{}", ncfile);
    }
  }

  @Test
  public void testCompoundVlens() throws IOException {
    // H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    String filename = testDir + "vlen/cdm_sea_soundings.nc4";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      logger.debug("**** testReadNetcdf4 done\n{}", ncfile);
      Variable v = ncfile.findVariable("fun_soundings");
      Array data = v.readArray();
      System.out.printf("testCompoundVlens %s%n", NcdumpArray.printArray(data, "fun_soundings", null));

      assertThat(data).isInstanceOf(StructureDataArray.class);
      assertThat(data.getSize()).isEqualTo(3);

      StructureDataArray as = (StructureDataArray) data;
      int index = 2;
      String memberName = "temp_vl";
      StructureData sdata = as.get(2);
      Array<Float> vdata = (Array<Float>) sdata.getMemberData(memberName);
      assertThat(vdata.getSize()).isEqualTo(3);
      Index ii = vdata.getIndex();
      assertThat(Misc.nearlyEquals(vdata.get(ii.set(2)), 21.5f)).isTrue();

      int count = 0;
      Structure s = (Structure) v;
      Array<StructureData> siter = (Array<StructureData>) s.readArray();
      for (StructureData sdata2 : siter) {
        Array vdata2 = sdata2.getMemberData(memberName);
        logger.debug("iter {} has {} elements for vlen member {}", count++, vdata2.getSize(), memberName);
      }
    }
  }

  /*
   * Structure {
   * int shutterPositionA;
   * int shutterPositionD;
   * int shutterPositionB;
   * int shutterPositionC;
   * int dspGainMode;
   * int coneActiveStateA;
   * int coneActiveStateD;
   * int coneActiveStateB;
   * int coneActiveStateC;
   * int loopDataA(1, *, *);
   * int loopDataB(1, *, *);
   * long sampleVtcw;
   * } tim_records(time=29);
   * 
   */
  @Test
  public void testCompoundVlens2() throws IOException {
    String filename = testDir + "vlen/IntTimSciSamp.nc";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      System.out.printf("**** testCompoundVlens2 %s%n", filename);
      Variable v = ncfile.findVariable("tim_records");
      Preconditions.checkNotNull(v);
      int[] vshape = v.getShape();
      Array<?> data = v.readArray();

      assertThat(data).isInstanceOf(StructureDataArray.class);
      StructureDataArray as = (StructureDataArray) data;
      assertThat(as).hasSize(vshape[0]); // int loopDataA(1, *);

      StructureData sdata = as.get(0);
      Array<Integer> a1 = (Array<Integer>) sdata.getMemberData("loopDataA");
      assertThat(a1.getArrayType()).isEqualTo(ArrayType.INT);
      assertThat(a1).hasSize(2);
      Index ii = a1.getIndex();
      assertThat(a1.get(ii.set(1))).isEqualTo(50334);

      int len = 0;
      for (StructureData sdata2 : as) {
        Array<Integer> a2 = (Array<Integer>) sdata2.getMemberData("loopDataA");
        assertThat(a2.getArrayType()).isEqualTo(ArrayType.INT);
        System.out.printf("%d ", a2.getSize());
        len += a2.getSize();
      }
      System.out.printf(" = %d%n", len);
      assertThat(len).isEqualTo(34);
    }
  }
}
