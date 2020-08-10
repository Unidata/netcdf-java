/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

/** Test {@link ucar.nc2.NetcdfFiles} */
public class TestNetcdfFiles {

  @Test
  public void testOpenWithClassName()
      throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "longOffset.nc",
        "ucar.nc2.internal.iosp.netcdf3.N3iospNew", -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.printf("%s%n", ncfile);;
    }
  }

  @Test
  public void testOpenInMemory()
      throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
    try (NetcdfFile ncfile = NetcdfFiles.openInMemory(TestDir.cdmLocalTestDataDir + "longOffset.nc")) {
      System.out.printf("%s%n", ncfile);;
    }
  }

  @Test
  public void testCompressionZip() throws IOException {
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


}
