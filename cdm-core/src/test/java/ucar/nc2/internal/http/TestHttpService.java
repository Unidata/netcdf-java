/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.http;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;

public class TestHttpService {

  // Some random file on the TDS
  private final String baseHttpLocation =
      "thredds.ucar.edu/thredds/fileServer/casestudies/irma/text/upper_air/upper_air_20170911_2300.txt";
  private final String httpsLocation = "https://" + baseHttpLocation;

  @Test
  @Category(NeedsExternalResource.class)
  public void testInMemoryHttpProvider() throws IOException {
    HttpRequest request = HttpService.standardGetRequestBuilder(httpsLocation).build();
    HttpResponse<InputStream> response = HttpService.standardRequest(request);
    byte[] contents = IO.readContentsToByteArray(response.body());

    HttpResponse<String> responseAsString = HttpService.standardRequestForString(request);
    assertThat(responseAsString.body()).isEqualTo(new String(contents, StandardCharsets.UTF_8));
  }
}
