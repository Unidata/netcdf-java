package ucar.nc2.jni.netcdf;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.MAMath;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

public class TestNc4IospMisc {

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  /*
   * Demonstrates bug from https://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/2017/msg00012.html
   * Prior to fix, this test would fail for 'u_short', 'u_int', and 'u_long' variables with
   * "Unknown userType == 8", "Unknown userType == 9", and "Unknown userType == 11" errors respectively.
   */
  @Test
  public void shouldReadDataSectionOfVariables() throws Exception {
    String[] varNames = new String[] {"u_byte", "u_short", "u_int", "u_long"};
    List<Array> expectedData = new ArrayList<>();
    // Will overflow to [-128, -127, -126]
    expectedData
        .add(Array.makeFromJavaArray(new byte[] {(byte) (1 << 7), (byte) ((1 << 7) + 1), (byte) ((1 << 7) + 2)}));
    expectedData.add(
        Array.makeFromJavaArray(new short[] {(short) (1 << 15), (short) ((1 << 15) + 1), (short) ((1 << 15) + 2)}));
    expectedData.add(Array.makeFromJavaArray(new int[] {(1 << 31), (1 << 31) + 1, (1 << 31) + 2}));
    expectedData.add(Array.makeFromJavaArray(new long[] {(1L << 63), (1L << 63) + 1, (1L << 63) + 2}));

    File file = new File(getClass().getResource("unsigned.nc4").toURI());
    assertThat(file.exists()).isTrue();

    try (
        NetcdfFile ncFile = NetcdfFile.open(file.getAbsolutePath(), Nc4Iosp.class.getCanonicalName(), -1, null, null)) {
      Nc4Iosp nc4Iosp = (Nc4Iosp) ncFile.getIosp();

      for (int i = 0; i < varNames.length; i++) {
        Variable var = ncFile.findVariable(varNames[i]);
        Nc4Iosp.Vinfo vinfo = (Nc4Iosp.Vinfo) var.getSPobject();
        Array array = nc4Iosp.readDataSection(vinfo.g4.grpid, vinfo.varid, vinfo.typeid, var.getShapeAsSection());
        MAMath.nearlyEquals(array, expectedData.get(i));
      }
    }
  }

  /*
   * Demonstrates bug from
   * https://andy.unidata.ucar.edu/esupport/staff/index.php?_m=tickets&_a=viewticket&ticketid=28098
   * Prior to fix, primary2Dim and primary3Dim were not being identified as unlimited.
   */
  @Test
  public void shouldContainMultipleGroupsWithUnlimitedDimensions() throws Exception {
    File file = new File(this.getClass().getResource("DBP-690959.nc4").toURI());
    assertThat(file.exists()).isTrue();

    try (
        NetcdfFile ncFile = NetcdfFile.open(file.getAbsolutePath(), Nc4Iosp.class.getCanonicalName(), -1, null, null)) {
      Dimension primary1Dim = ncFile.findDimension("/group1/primary");
      Dimension primary2Dim = ncFile.findDimension("/group2/primary");
      Dimension primary3Dim = ncFile.findDimension("/group3/primary");

      assertThat(primary1Dim.isUnlimited()).isTrue();
      assertThat(primary2Dim.isUnlimited()).isTrue();
      assertThat(primary3Dim.isUnlimited()).isTrue();
    }
  }

  @Test
  public void shouldWriteUnlimitedDimension() throws Exception {
    File tempFile = new File(tempFolder.getRoot(), "Nc4IospMiscSpec.nc4");

    try (NetcdfFileWriter ncWriter =
        NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, tempFile.getAbsolutePath())) {
      Dimension dimBefore = ncWriter.addDimension(null, "dim", 3, true, false);
      ncWriter.create();
      ncWriter.close();

      try (NetcdfFile ncFile = NetcdfFile.open(tempFile.getAbsolutePath())) {
        Dimension dimAfter = ncFile.findDimension(dimBefore.getFullName());
        // Failed prior to fix, because dimAfter was not unlimited.
        assertThat(dimBefore).isEqualTo(dimAfter);
      }
    }
  }

  @Test
  public void shouldCreateFileWithNullValuedAttributes() throws Exception {
    File tempFile = new File(tempFolder.getRoot(), "Nc4IospMiscSpec.nc4");

    try (NetcdfFileWriter ncWriter =
        NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, tempFile.getAbsolutePath())) {
      Attribute attrNum = new Attribute("nullvalnum", DataType.INT);
      Attribute attrNumBefore = ncWriter.addGlobalAttribute(attrNum);

      Attribute attrStr = new Attribute("nullvalstr", DataType.STRING);
      Attribute attrStrBefore = ncWriter.addGlobalAttribute(attrStr);

      Attribute attrChar = new Attribute("nullvalchar", DataType.CHAR);
      Attribute attrCharBefore = ncWriter.addGlobalAttribute(attrChar);

      // add a character valued attribute with a specific null char value"
      Attribute attrNullChar = new Attribute("nullcharvalchar", DataType.CHAR);
      Array attrNullCharValue = ArrayChar.makeFromString("\0", 1);
      attrNullChar.setValues(attrNullCharValue);
      Attribute attrNullCharBefore = ncWriter.addGlobalAttribute(attrNullChar);

      ncWriter.create();
      ncWriter.close();

      try (NetcdfFile ncFile = NetcdfFile.open(tempFile.getAbsolutePath())) {
        Attribute attrNumAfter = ncFile.findGlobalAttribute(attrNumBefore.getFullName());
        assertThat(attrNumBefore.getValues()).isEqualTo(attrNumAfter.getValues());
        assertThat(attrNumBefore.getValues()).isNull();

        Attribute attrStrAfter = ncFile.findGlobalAttribute(attrStrBefore.getFullName());
        assertThat(attrStrAfter.getValues()).isEqualTo(attrStrBefore.getValues());
        assertThat(attrStrAfter.getValues()).isNull();

        Attribute attrCharAfter = ncFile.findGlobalAttribute(attrCharBefore.getFullName());
        assertThat(attrCharAfter.getValues()).isEqualTo(attrCharBefore.getValues());
        assertThat(attrCharAfter.getValues()).isNull();

        Attribute attrNullCharAfter = ncFile.findGlobalAttribute(attrNullCharBefore.getFullName());
        assertThat(attrNullCharAfter.getValues().getSize()).isEqualTo(attrNullCharBefore.getValues().getSize());
        assertThat(attrNullCharAfter.getValues().getSize()).isEqualTo(1);
        assertThat(attrNullCharAfter.getValue(0)).isEqualTo(attrNullCharBefore.getValue(0));
        assertThat(attrNullCharAfter).isEqualTo(attrNullCharBefore);
      }
    }
  }
}
