/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.http;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.internal.http.InMemoryRafHttpProvider;
import ucar.nc2.util.IO;
import ucar.unidata.io.InMemoryRandomAccessFile;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.ReadFromUrl;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link ReadFromUrl} */
public class TestReadFromUrl {
  // Some random file on the TDS
  private final String baseHttpLocation =
      "thredds.ucar.edu/thredds/fileServer/casestudies/irma/text/upper_air/upper_air_20170911_2300.txt";
  private final String httpsLocation = "https://" + baseHttpLocation;

  @Test
  @Category(NeedsExternalResource.class)
  public void testReadURLcontents() throws IOException {
    System.out.printf("testReadURLcontents Open %s%n", httpsLocation);

    String fromRaf;
    InMemoryRafHttpProvider provider = new InMemoryRafHttpProvider();
    try (RandomAccessFile rafh = provider.open(httpsLocation)) {
      assertThat(rafh).isInstanceOf(InMemoryRandomAccessFile.class);
      assertThat(rafh.getLocation()).isEqualTo(httpsLocation);
      assertThat(rafh.getLastModified()).isEqualTo(0);
      assertThat(rafh.length()).isEqualTo(18351);

      // read a couple of random bytes
      byte[] buff = new byte[(int) rafh.length()];
      rafh.seek(0L);
      assertThat(rafh.read(buff)).isEqualTo(rafh.length());
      fromRaf = new String(buff, StandardCharsets.UTF_8);
    }

    String contents = ReadFromUrl.readURLcontents(httpsLocation);
    assertThat(contents).isEqualTo(fromRaf);
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testGetgetInputStreamFromUrl() throws IOException {
    System.out.printf("testReadURLcontents Open %s%n", httpsLocation);

    String fromRaf;
    InMemoryRafHttpProvider provider = new InMemoryRafHttpProvider();
    try (RandomAccessFile rafh = provider.open(httpsLocation)) {
      assertThat(rafh).isInstanceOf(InMemoryRandomAccessFile.class);
      assertThat(rafh.getLocation()).isEqualTo(httpsLocation);
      assertThat(rafh.getLastModified()).isEqualTo(0);
      assertThat(rafh.length()).isEqualTo(18351);

      // read a couple of random bytes
      byte[] buff = new byte[(int) rafh.length()];
      rafh.seek(0L);
      assertThat(rafh.read(buff)).isEqualTo(rafh.length());
      fromRaf = new String(buff, StandardCharsets.UTF_8);
    }

    try (InputStream is = ReadFromUrl.getInputStreamFromUrl(httpsLocation)) {
      assertThat(IO.readContents(is)).isEqualTo(fromRaf);
    }
  }
}
