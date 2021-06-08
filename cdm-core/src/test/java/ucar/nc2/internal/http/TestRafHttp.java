/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.http;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link RafHttp} */
public class TestRafHttp {
  // Some random file on the TDS
  private final String baseHttpLocation =
      "thredds.ucar.edu/thredds/fileServer/casestudies/irma/text/upper_air/upper_air_20170911_2300.txt";
  private final String httpsLocation = "https://" + baseHttpLocation;

  @Test
  @Category(NeedsExternalResource.class)
  public void testHttpRaf() throws IOException {
    System.out.printf("testHttpRaf Open %s%n", httpsLocation);
    try (RafHttp rafh = new RafHttp(httpsLocation)) {
      assertThat(rafh.getLocation()).isEqualTo(httpsLocation);
      assertThat(rafh.getLastModified()).isEqualTo(0);
      assertThat(rafh.length()).isEqualTo(18351);

      // read a couple of random bytes
      byte[] buff = new byte[2];
      assertThat(rafh.readRemote(42L, buff, 0, 2)).isEqualTo(2);
      assertThat(buff[0]).isEqualTo(32);
      assertThat(buff[1]).isEqualTo(55);
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testRedirectedHttpRaf() throws IOException {
    String httpLocation = "http://" + baseHttpLocation;
    // TDS does a 307 internal redirect to https (examining network traffic in Chrome Inspect tool
    System.out.printf("testRedirectedHttpRaf Open %s%n", httpLocation);
    try (RafHttp rafh = new RafHttp(httpLocation)) {
      assertThat(rafh.getLocation()).isEqualTo(httpLocation);
      assertThat(rafh.getLastModified()).isEqualTo(0);
      assertThat(rafh.length()).isEqualTo(18351);

      // read a couple of random bytes
      byte[] buff = new byte[2];
      assertThat(rafh.readRemote(42L, buff, 0, 2)).isEqualTo(2);
      assertThat(buff[0]).isEqualTo(32);
      assertThat(buff[1]).isEqualTo(55);
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testHttpRafProvider() throws IOException {
    System.out.printf("testHttpRafProvider Open %s%n", httpsLocation);
    RafHttp.Provider provider = new RafHttp.Provider();
    try (RandomAccessFile rafh = provider.open(httpsLocation)) {
      assertThat(rafh).isInstanceOf(RafHttp.class);
      assertThat(rafh.getLocation()).isEqualTo(httpsLocation);
      assertThat(rafh.getLastModified()).isEqualTo(0);
      assertThat(rafh.length()).isEqualTo(18351);

      // read a couple of random bytes
      byte[] buff = new byte[2];
      rafh.seek(42L);
      assertThat(rafh.read(buff)).isEqualTo(2);
      assertThat(buff[0]).isEqualTo(32);
      assertThat(buff[1]).isEqualTo(55);
    }
  }
}
