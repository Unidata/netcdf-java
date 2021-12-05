/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Test;
import ucar.array.ArrayType;
import ucar.array.StructureData;
import ucar.array.StructureMembers;
import ucar.array.Array;
import ucar.array.Index;
import ucar.nc2.*;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;

import static com.google.common.truth.Truth.assertThat;

public class TestScaleOffsetMissingForStructure {

  @Test
  public void testNetcdfFile() throws Exception {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(TestDir.cdmLocalTestDataDir + "testScaleRecord.nc");
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(durl, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      Variable v = ncfile.findVariable("testScale");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.SHORT);

      Array<Short> data = (Array<Short>) v.readArray();
      Index ima = data.getIndex();
      short val = data.get(ima);
      assertThat(val).isEqualTo(-999);

      Structure s = (Structure) ncfile.findVariable("record");
      assertThat(s).isNotNull();

      Variable v2 = s.findVariable("testScale");
      assertThat(v2).isNotNull();
      Attribute att = v2.findAttribute("units");
      assertThat(att.getStringValue()).isEqualTo("m");
      assertThat(v2.getUnitsString()).isEqualTo("m");

      StructureData sd = s.readRecord(0);
      StructureMembers.Member m = sd.getStructureMembers().findMember("testScale");
      assertThat(m).isNotNull();
      assertThat(m.getUnitsString()).isEqualTo("m");

      Array<Number> marr = (Array<Number>) sd.getMemberData(m);
      double dval = marr.getScalar().doubleValue();
      assertThat(dval).isEqualTo(-999.0);

      int count = 0;
      Array<StructureData> sdarr = (Array<StructureData>) s.readArray();
      for (StructureData sdata : sdarr) {
        m = sdata.getStructureMembers().findMember("testScale");
        assertThat(m).isNotNull();
        assertThat(m.getUnitsString()).isEqualTo("m");
        marr = (Array<Number>) sdata.getMemberData(m);
        dval = marr.getScalar().doubleValue();
        double expect = (count == 0) ? -999.0 : 13.0;
        assertThat(dval).isEqualTo(expect);
        count++;
      }
    }
  }

  @Test
  public void testNetcdfDataset() throws Exception {
    try (NetcdfDataset ncfile = NetcdfDatasets.openDataset(TestDir.cdmLocalTestDataDir + "testScaleRecord.nc", true,
        null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.printf("Open %s%n", ncfile.getLocation());
      VariableDS v = (VariableDS) ncfile.findVariable("testScale");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.FLOAT);

      Array<Float> data = (Array<Float>) v.readArray();
      Index ima = data.getIndex();
      float val = data.get(ima);
      assertThat(val).isNaN();

      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      Structure s = (Structure) ncfile.findVariable("record");
      assertThat(s).isNotNull();

      VariableDS vm = (VariableDS) s.findVariable("testScale");
      Array<Float> vmData = (Array<Float>) vm.readArray();
      float vmval = vmData.getScalar();
      assertThat(vm.isMissing(vmval)).isTrue();
      assertThat(vmval).isNaN();

      StructureData sd = s.readRecord(0);
      StructureMembers.Member m = sd.getStructureMembers().findMember("testScale");
      assertThat(m).isNotNull();
      assertThat(m.getUnitsString()).isEqualTo("m");

      Array<Number> marr = (Array<Number>) sd.getMemberData(m);
      double dval = marr.getScalar().doubleValue();
      assertThat(dval).isNaN();
    }
  }

  @Test
  public void testNetcdfDatasetAttributes() throws Exception {
    try (NetcdfDataset ncfile = NetcdfDatasets.openDataset(TestDir.cdmLocalTestDataDir + "testScaleRecord.nc", true,
        null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.printf("Open %s%n", ncfile.getLocation());
      VariableDS v = (VariableDS) ncfile.findVariable("testScale");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.FLOAT);

      assertThat(v.getUnitsString()).isEqualTo("m");
      assertThat(v.attributes().findAttributeString("units", "")).isEqualTo("m");

      Structure s = (Structure) ncfile.findVariable("record");
      assertThat(s).isNotNull();

      Variable v2 = s.findVariable("testScale");
      assertThat(v2.getUnitsString()).isEqualTo("m");
      assertThat(v2.getArrayType()).isEqualTo(ArrayType.FLOAT);

      double scale = v2.attributes().findAttributeDouble("scale_factor", 0.0);
      double offset = v2.attributes().findAttributeDouble("add_offset", 0.0);
      StructureData sd = s.readRecord(0);
      StructureMembers.Member m = sd.getStructureMembers().findMember("testScale");
      assertThat(m).isNotNull();
      assertThat(m.getUnitsString()).isEqualTo("m");
      assertThat(m.getArrayType()).isEqualTo(ArrayType.FLOAT);

      int count = 0;
      Array<StructureData> sdarr = (Array<StructureData>) s.readArray();
      for (StructureData sdata : sdarr) {
        m = sdata.getStructureMembers().findMember("testScale");
        assertThat(m).isNotNull();
        assertThat(m.getUnitsString()).isEqualTo("m");
        Array<Number> marr = (Array<Number>) sdata.getMemberData(m);
        double dval = marr.getScalar().doubleValue();
        if (count == 0) {
          assertThat(dval).isNaN();
        } else {
          double expect = 13.0 * scale + offset;
          assertThat(Misc.nearlyEquals(dval, expect, 1.0e-6)).isTrue();
        }
        count++;
      }

    }
  }
}
