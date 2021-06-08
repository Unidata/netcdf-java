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

  /*
   * LOOK not done
   * Put content to a url, using HTTP PUT. Handles one level of 302 redirection.
   *
   * @param urlencoded url as a String
   * 
   * @param content PUT this content at the given url.
   * 
   * @return the HTTP status return code
   * 
   * @throws java.io.IOException on error
   *
   * public static int putContent(String urlencoded, String content) throws IOException {
   * 
   * HttpRequest.BodyPublisher bodyPublisher = null;
   * 
   * HttpRequest request = HttpRequest.newBuilder()
   * .uri(URI.create(urlencoded))
   * .timeout(Duration.ofSeconds(10))
   * .PUT(bodyPublisher)
   * .build();
   * 
   * try (HTTPMethod m = HTTPFactory.Put(urlencoded)) {
   * m.setRequestContent(new StringEntity(content, "application/text", CDM.UTF8));
   * m.execute();
   * 
   * int resultCode = m.getStatusCode();
   * 
   * // followRedirect wont work for PUT
   * if (resultCode == 302) {
   * String redirectLocation;
   * Optional<String> locationOpt = m.getResponseHeaderValue("location");
   * if (locationOpt.isPresent()) {
   * redirectLocation = locationOpt.get();
   * resultCode = putContent(redirectLocation, content);
   * }
   * }
   * return resultCode;
   * }
   * }
   */

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
