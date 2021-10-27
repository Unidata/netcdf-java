/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

/** Test {@link ucar.nc2.NetcdfFiles} */
public class TestNetcdfFiles {

  @Test
  public void testOpenWithClassName()
      throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "longOffset.nc",
        "ucar.nc2.internal.iosp.netcdf3.N3iosp", -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.printf("%s%n", ncfile);
    }
  }

  @Test
  public void testOpenInMemory() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.openInMemory(TestDir.cdmLocalTestDataDir + "longOffset.nc")) {
      System.out.printf("%s%n", ncfile);
    }
  }

  @Test
  public void testCanOpen() {
    assertThat(NetcdfFiles.canOpen(TestDir.cdmLocalTestDataDir + "longOffset.nc")).isTrue();
    assertThat(NetcdfFiles.canOpen(TestDir.cdmLocalTestDataDir + "sunya.nc")).isFalse();
    assertThat(NetcdfFiles.canOpen(TestDir.cdmLocalTestDataDir + "testUnsignedFillValueNew.dump")).isFalse();
  }

  @Test
  public void testCompressionZ() throws IOException {
    File uncompressedFile = new File(TestDir.cdmLocalTestDataDir + "compress/testCompress.nc");
    uncompressedFile.delete();

    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "compress/testCompress.nc.Z")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }

    // repeat, to read from cache
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "compress/testCompress.nc.Z")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }
  }

  @Test
  public void testCompressionZip() throws IOException {
    File uncompressedFile = new File(TestDir.cdmLocalTestDataDir + "compress/testZip.nc");
    uncompressedFile.delete();

    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "compress/testZip.nc.zip")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }

    // repeat, to read from cache
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "compress/testZip.nc.zip")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }
  }

  @Test
  public void testCompressionGzip() throws IOException {
    File uncompressedFile = new File(TestDir.cdmLocalTestDataDir + "compress/testGzip.nc");
    uncompressedFile.delete();

    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "compress/testGzip.nc.gz")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }

    // repeat, to read from cache
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "compress/testGzip.nc.zip")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }
  }

  @Test
  public void testCompressionBzip() throws IOException {
    File uncompressedFile = new File(TestDir.cdmLocalTestDataDir + "compress/testBzip.nc");
    uncompressedFile.delete();

    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "compress/testBzip.nc.bz2")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }

    // repeat, to read from cache
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "compress/testBzip.nc.bz2")) {
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");
    }
  }

  @Test
  public void testMakeFullNameGroup() {
    // root
    // parent
    // child
    // grandchild
    Group.Builder parent = Group.builder().setName("parent");
    Group.Builder child = Group.builder().setName("child");
    parent.addGroup(child);
    Group.Builder grandchild = Group.builder().setName("grandchild");
    child.addGroup(grandchild);

    Group root = Group.builder().addGroup(parent).build();

    assertThat(NetcdfFiles.makeFullName(root)).isEqualTo("");

    assertThat(root.getGroups()).hasSize(1);
    Group parentGroup = root.getGroups().get(0);
    assertThat(NetcdfFiles.makeFullName(parentGroup)).isEqualTo("parent");

    assertThat(parentGroup.getGroups()).hasSize(1);
    Group childGroup = parentGroup.getGroups().get(0);
    assertThat(NetcdfFiles.makeFullName(childGroup)).isEqualTo("parent/child");

    assertThat(childGroup.getGroups()).hasSize(1);
    Group grandchildGroup = childGroup.getGroups().get(0);
    assertThat(NetcdfFiles.makeFullName(grandchildGroup)).isEqualTo("parent/child/grandchild");

  }
}
