/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.http;

import ucar.nc2.util.IO;
import ucar.unidata.io.InMemoryRandomAccessFile;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.spi.RandomAccessFileProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Reads an entire file into memory, over HTTP. Uses "slurp:" prefix.
 */
public class InMemoryRafHttpProvider implements RandomAccessFileProvider {

  @Override
  public boolean isOwnerOf(String location) {
    // TODO undocumented prefix
    return location.startsWith("slurp:");
  }

  @Override
  public RandomAccessFile open(String location) throws IOException {
    String scheme = location.split(":")[0];
    location = location.replace(scheme, "https");

    HttpRequest request = HttpService.standardGetRequestBuilder(location).build();
    HttpResponse<InputStream> response = HttpService.standardRequest(request);
    byte[] contents = IO.readContentsToByteArray(response.body());

    return new InMemoryRandomAccessFile(location, contents);
  }

}
