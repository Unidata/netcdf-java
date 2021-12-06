/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import ucar.array.Index;
import ucar.array.Array;
import ucar.array.Section;
import ucar.array.StructureData;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestReadRecord {
  @Test
  // Normal reading of Nc3 record variables
  public void testNC3ReadRecordVariables() throws Exception {
    try (NetcdfFile nc = TestDir.openFileLocal("testWriteRecord.nc")) {

      /* Get the value of the global attribute named "title" */
      String title = nc.getRootGroup().findAttributeString("title", "N/A");

      /*
       * Read the latitudes into an array of double.
       * This works regardless of the external
       * type of the "lat" variable.
       */
      Variable lat = nc.findVariable("lat");
      assertThat(lat).isNotNull();
      assertThat(lat.getRank()).isEqualTo(1);
      int nlats = lat.getShape()[0]; // number of latitudes
      double[] lats = new double[nlats]; // where to put them

      Array<Number> values = (Array<Number>) lat.readArray(); // read all into memory
      Index ima = values.getIndex(); // index array to specify which value
      for (int ilat = 0; ilat < nlats; ilat++) {
        lats[ilat] = values.get(ima.set0(ilat)).doubleValue();
      }
      /* Read units attribute of lat variable */
      String latUnits = lat.findAttributeString("units", "N/A");
      assertThat(latUnits).isEqualTo("degrees_north");

      /* Read the longitudes. */
      Variable lon = nc.findVariable("lon");
      assertThat(lon).isNotNull();
      Array<Number> fa = (Array<Number>) lon.readArray();
      assertThat(Misc.nearlyEquals(fa.get(0).floatValue(), -109.0f)).isTrue();
      assertThat(Misc.nearlyEquals(fa.get(1).floatValue(), -107.0f)).isTrue();
      assertThat(Misc.nearlyEquals(fa.get(2).floatValue(), -105.0f)).isTrue();
      assertThat(Misc.nearlyEquals(fa.get(3).floatValue(), -103.0f)).isTrue();

      /*
       * Now we can just use the MultiArray to access values, or
       * we can copy the MultiArray elements to another array with
       * toArray(), or we can get access to the MultiArray storage
       * without copying. Each of these approaches to accessing
       * the data are illustrated below.
       */

      /* Read the times: unlimited dimension */
      Variable time = nc.findVariable("time");
      assertThat(time).isNotNull();
      Array<Integer> ta = (Array<Integer>) time.readArray();
      assertThat(ta.get(0)).isEqualTo(6);
      assertThat(ta.get(1)).isEqualTo(18);

      /* Read the relative humidity data */
      Variable rh = nc.findVariable("rh");
      assertThat(rh).isNotNull();
      Array<Integer> rha = (Array<Integer>) rh.readArray();
      int[] shape = rha.getShape();
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          for (int k = 0; k < shape[2]; k++) {
            int want = 20 * i + 4 * j + k + 1;
            int val = rha.get(i, j, k);
            assertThat(want).isEqualTo(val);
          }
        }
      }

      /* Read the temperature data */
      Variable t = nc.findVariable("T");
      assertThat(t).isNotNull();
      Array<Double> Ta = (Array<Double>) t.readArray();
      assertThat(Misc.nearlyEquals(Ta.get(0, 0, 0), 1.0f)).isTrue();
      assertThat(Misc.nearlyEquals(Ta.get(1, 1, 1), 10.0f)).isTrue();

      /* Read subset of the temperature data */
      Ta = (Array<Double>) t.readArray(new Section(new int[3], new int[] {2, 2, 2}));
      assertThat(Misc.nearlyEquals(Ta.get(0, 0, 0), 1.0f)).isTrue();
      assertThat(Misc.nearlyEquals(Ta.get(1, 1, 1), 10.0f)).isTrue();
    }
  }

  @Test
  // Reading of Nc3 record variables, having been made into a structure
  public void testNC3ReadRecordsAsStructuture() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      Variable record = ncfile.findVariable("record");
      assertThat(record).isNotNull();
      assertThat(record).isInstanceOf(Structure.class);
      Structure rs = (Structure) record;
      assertThat(rs.getRank()).isEqualTo(1);
      assertThat(rs.getDimension(0).getLength()).isEqualTo(2);

      /* Read the records */
      Array<?> rsValues = rs.readArray();
      assertThat(rsValues.getRank()).isEqualTo(1);
      assertThat(rsValues.getShape()[0]).isEqualTo(2);

      /* Read the times: unlimited dimension */
      Variable time = rs.findVariable("time");
      assertThat(time).isNotNull();
      Array<Integer> timeValues = (Array<Integer>) time.readArray();
      assertThat(timeValues.get(0)).isEqualTo(6);

      /* Read the relative humidity data */
      Variable rh = rs.findVariable("rh");
      assertThat(rh).isNotNull();
      Array<Integer> rha = (Array<Integer>) rh.readArray();
      int[] shape = rha.getShape();
      // for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[0]; j++) {
        for (int k = 0; k < shape[1]; k++) {
          int want = 4 * j + k + 1;
          int val = rha.get(j, k);
          assertThat(want).isEqualTo(val);
        }
      }
    }
  }


  @Test
  public void testNC3ReadRecordStrided() throws Exception {
    // record variable
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      Variable record = ncfile.findVariable("record");
      assertThat(record).isNotNull();
      assertThat(record).isInstanceOf(Structure.class);
      Structure rs = (Structure) record;
      assertThat(rs.getRank()).isEqualTo(1);
      assertThat(rs.getDimension(0).getLength()).isEqualTo(2);

      /* Read a record */
      Array<StructureData> rsValues = (Array<StructureData>) rs.readArray(new Section("1:1:2"));
      assertThat(rsValues.getRank()).isEqualTo(1);
      assertThat(rsValues.getShape()[0]).isEqualTo(1);

      StructureData sdata = rsValues.get(0);
      Array<Integer> tdata = (Array<Integer>) sdata.getMemberData("time");
      int t = tdata.get(0);
      assertThat(t).isEqualTo(18);

      Number t2 = (Number) sdata.getMemberData("time").getScalar();
      assertThat(t2.intValue()).isEqualTo(18);
    }
  }

  @Test
  public void testDatasetAddRecord() throws Exception {
    String location = TestDir.cdmLocalTestDataDir + "testWriteRecord.nc";
    DatasetUrl durl = DatasetUrl.create(null, location);
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(durl, NetcdfDataset.getDefaultEnhanceMode(), -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {

      // record variable
      Variable record = ncd.findVariable("record");
      assertThat(record).isNotNull();
      assertThat(record).isInstanceOf(StructureDS.class);
      StructureDS rs = (StructureDS) record;
      assertThat(rs.getRank()).isEqualTo(1);
      assertThat(rs.getDimension(0).getLength()).isEqualTo(2);

      /* Read a record */
      Array<StructureData> rsValues = (Array<StructureData>) rs.readArray(new Section("1:1:2"));
      assertThat(rsValues.getRank()).isEqualTo(1);
      assertThat(rsValues.getShape()[0]).isEqualTo(1);

      StructureData sdata = rsValues.get(0);
      Array<Integer> tdata = (Array<Integer>) sdata.getMemberData("time");
      int t = tdata.getScalar();
      assertThat(t).isEqualTo(18);

      /* Read the times: unlimited dimension */
      Variable time = rs.findVariable("time");
      assertThat(time).isNotNull();
      Array<Integer> timeValues = (Array<Integer>) time.readArray();
      int t2 = timeValues.getScalar();
      assertThat(t2).isEqualTo(6);
    }
  }

  // This only works on old iosp
  @Test
  public void testDatasetAddRecordAfter() throws Exception {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc", true, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      assertThat((Boolean) ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)).isTrue();

      // record variable
      Variable record = ncd.findVariable("record");
      assertThat(record).isNotNull();
      assertThat(record).isInstanceOf(StructureDS.class);
      StructureDS rs = (StructureDS) record;
      assertThat(rs.getRank()).isEqualTo(1);
      assertThat(rs.getDimension(0).getLength()).isEqualTo(2);

      /* Read a record */
      Array<?> rsValues = rs.readArray(new Section("1:1:2"));
      assertThat(rsValues.getRank()).isEqualTo(1);
      assertThat(rsValues.getShape()[0]).isEqualTo(1);

      StructureData sdata = (StructureData) rsValues.get(rsValues.getIndex());
      Array<Integer> tdata = (Array<Integer>) sdata.getMemberData("time");
      int t = tdata.getScalar();
      assertThat(t).isEqualTo(18);

      /* Read the times: unlimited dimension */
      Variable time = rs.findVariable("time");
      assertThat(time).isNotNull();
      Array<Integer> timeValues = (Array<Integer>) time.readArray();
      int t2 = timeValues.getScalar();
      assertThat(t2).isEqualTo(6);
    }
  }
}
