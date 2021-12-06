/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.DebugFlags;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author caron
 * @since Jul 17, 2007
 */
public class TestH5OddTypes {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  static public String testDir = TestH5.testDir;

  @AfterClass
  static public void after() {
    H5header.setDebugFlags(DebugFlags.create("")); // make sure debug flags are off
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testOpaque() throws Exception {
    H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("samples/opaque.h5")) {
      System.out.println("\n" + ncfile);
      Variable v2 = ncfile.findVariable("Opaque");
      assertThat(v2).isNotNull();

      Array<?> data = v2.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.OPAQUE);
      System.out.println("data size= " + new Section(data.getShape()));

      Array<?> odata = v2.readArray(new Section("1:20"));
      assertThat(data.getArrayType()).isEqualTo(ArrayType.OPAQUE);
      assertThat(odata.getSize()).isEqualTo(20);
    }
    H5header.setDebugFlags(DebugFlags.create(""));
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEnum() throws Exception {
    try (NetcdfFile ncfile = TestH5.openH5("support/enum.h5")) {
      assertThat(ncfile).isNotNull();
      Variable v2 = ncfile.findVariable("enum");
      assertThat(v2).isNotNull();

      Array<?> data = v2.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.ENUM4);
    }

    try (NetcdfDataset ncd = TestH5.openH5dataset("support/enum.h5")) {
      assertThat(ncd).isNotNull();
      Variable v2 = ncd.findVariable("enum");
      assertThat(v2).isNotNull();

      Array<?> data = v2.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.STRING);
    }
  }

  /*
   * public void testTime() throws IOException {
   * H5header.setDebugFlags(DebugFlags.create("H5header/header"));
   * NetcdfFile ncfile = TestH5.openH5("support/time.h5");
   * 
   * Variable v = ncfile.findVariable("dset");
   * Array data = v.read();
   * NCdump.printArray(data, "dset", System.out, null);
   * System.out.println( "\n**** testReadNetcdf4 done\n\n"+ncfile);
   * ncfile.close();
   * H5header.setDebugFlags( DebugFlags.create(""));
   * }
   */

  // not supporting bitfield, poor documentation
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testBitfield() throws IOException {
    H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("samples/bitfield.h5")) {

    }
    H5header.setDebugFlags(DebugFlags.create(""));
  }

  // attribute vlen String
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testVlenStrings() throws IOException {
    H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("support/vlstra.h5")) {
      System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    }
    H5header.setDebugFlags(DebugFlags.create(""));
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testAttString() throws IOException {
    // H5header.setDebugFlags( DebugFlags.create("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("support/attstr.h5")) {
    }
  }

  // FIXME: This is a crappy test; it doesn't fail when it can't read the file.
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCompoundString() throws IOException {
    H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    TestDir.readAll(TestH5.testDir + "support/cstr.h5");
    H5header.setDebugFlags(DebugFlags.create(""));
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCompoundEnum() throws IOException {
    H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("support/cenum.h5")) {
      Variable v = ncfile.findVariable("enum");
      Array<?> data = v.readArray();
      System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    }
    H5header.setDebugFlags(DebugFlags.create(""));
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void misc() throws IOException {
    H5header.setDebugFlags(DebugFlags.create("H5header/header"));

    // bitfields, opaque
    try (NetcdfFile ncfile = TestH5.openH5("support/bitop.h5")) {
      System.out.println("\n" + ncfile);
    }
    H5header.setDebugFlags(DebugFlags.create(""));
  }

  @Test
  public void testMisc() {
    byte[] heapId = new byte[] {0, 22, 32, 0, 0, 0, -19, 5};
    int offset = makeIntFromBytes(heapId, 1, 5);
    System.out.printf("%d%n", offset);
  }

  private int makeIntFromBytes(byte[] bb, int start, int n) {
    int result = 0;
    for (int i = start + n - 1; i >= start; i--) {
      result <<= 8;
      byte b = bb[i];
      result += (b < 0) ? b + 256 : b;
    }
    return result;
  }

}
