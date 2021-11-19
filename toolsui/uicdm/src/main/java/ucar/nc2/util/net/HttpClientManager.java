/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.net;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import ucar.nc2.internal.http.HttpService;
import ucar.nc2.util.IO;
import ucar.unidata.util.Urlencoded;
import java.io.*;

public class HttpClientManager {

  @Urlencoded
  public static String getContentAsString(String urlencoded) throws IOException {
    HttpRequest request = HttpService.standardGetRequestBuilder(urlencoded).build();
    HttpResponse<String> response = HttpService.standardRequestForString(request);
    return response.body();
  }

  public static void copyUrlContentsToFile(String urlencoded, File file) throws IOException {
    HttpRequest request = HttpService.standardGetRequestBuilder(urlencoded).build();
    HttpResponse<InputStream> response = HttpService.standardRequest(request);
    IO.writeToFile(response.body(), file.getPath());
  }

  public static long appendUrlContentsToFile(String urlencoded, File file, long start, long end) throws IOException {

    HttpRequest request = HttpService.standardGetRequestBuilder(urlencoded)
        .header("Range", String.format("bytes=%d-%d", start, end)).build();
    HttpResponse<InputStream> response = HttpService.standardRequest(request);
    return IO.appendToFile(response.body(), file.getPath());
  }

}
