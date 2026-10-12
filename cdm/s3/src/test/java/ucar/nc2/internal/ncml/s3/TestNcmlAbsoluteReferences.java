/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.ncml.NcmlReader;
import ucar.nc2.write.NetcdfFormatWriter;

/** Reads HTTP datasets referenced by NcML documents with a cdms3 base location. */
public class TestNcmlAbsoluteReferences {
  private static final String BASE = "cdms3://profile@localhost/bucket?site/wrapper.ncml#delimiter=/";
  private static final String NAMESPACE = "http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2";

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private HttpServer server;
  private String datasetUrl;

  /**
   * Serve a generated NetCDF file on the loopback interface without external services.
   *
   * @throws IOException if the file or HTTP server cannot be created
   * @throws InvalidRangeException if the fixture values do not match the time dimension
   */
  @Before
  public void setUp() throws IOException, InvalidRangeException {
    Path fixture = temporaryFolder.newFile("data.nc").toPath();
    NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(fixture.toString());
    builder.addDimension("time", 2);
    builder.addVariable("time", DataType.DOUBLE, "time");
    try (NetcdfFormatWriter writer = builder.build()) {
      writer.write("time", Array.factory(DataType.DOUBLE, new int[] {2}, new double[] {1.0, 2.0}));
    }
    byte[] contents = Files.readAllBytes(fixture);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/data.nc", exchange -> serveFixture(exchange, contents));
    server.start();
    datasetUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/data.nc";
  }

  /** Stop the HTTP server, including when setup or a dataset read fails. */
  @After
  public void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  /**
   * Read values through a wrapper containing an absolute HTTP reference.
   *
   * @throws IOException if the wrapper or referenced dataset cannot be read
   */
  @Test
  public void testWrapper() throws IOException {
    String ncml = "<netcdf xmlns=\"" + NAMESPACE + "\" location=\"" + datasetUrl + "\"/>";
    assertNcmlData(ncml, new double[] {1.0, 2.0});
  }

  /**
   * Read all values from explicit aggregation members with absolute HTTP references.
   *
   * @throws IOException if the aggregation or referenced datasets cannot be read
   */
  @Test
  public void testAggregation() throws IOException {
    String ncml = "<netcdf xmlns=\"" + NAMESPACE + "\">" + "<aggregation dimName=\"time\" type=\"joinExisting\">"
        + "<netcdf location=\"" + datasetUrl + "\" ncoords=\"2\"/>" + "<netcdf location=\"" + datasetUrl
        + "\" ncoords=\"2\"/>" + "</aggregation></netcdf>";
    assertNcmlData(ncml, new double[] {1.0, 2.0, 1.0, 2.0});
  }

  /**
   * Check the complete time array returned by an NcML document with a cdms3 base location.
   *
   * @param ncml document containing absolute HTTP references to the generated NetCDF file
   * @param expected complete time values expected from the document
   * @throws IOException if the document or referenced dataset cannot be read
   */
  private void assertNcmlData(String ncml, double[] expected) throws IOException {
    try (NetcdfDataset dataset = NcmlReader.readNcml(new StringReader(ncml), BASE, null).build()) {
      Variable time = dataset.findVariable("time");
      assertNotNull(time);
      Array values = time.read();
      assertThat(values.getSize()).isEqualTo(expected.length);
      for (int i = 0; i < expected.length; i++) {
        assertThat(values.getDouble(i)).isEqualTo(expected[i]);
      }
    }
  }

  /**
   * Serve HEAD and single byte-range requests for the generated NetCDF file.
   *
   * @param exchange request from the NetCDF HTTP reader
   * @param contents complete NetCDF file bytes
   * @throws IOException if the response cannot be written
   */
  private static void serveFixture(HttpExchange exchange, byte[] contents) throws IOException {
    try {
      exchange.getResponseHeaders().set("Content-Type", "application/x-netcdf");
      exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
      if (exchange.getRequestMethod().equals("HEAD")) {
        exchange.getResponseHeaders().set("Content-Length", Integer.toString(contents.length));
        exchange.sendResponseHeaders(200, -1);
        return;
      }
      String range = exchange.getRequestHeaders().getFirst("Range");
      int start = 0;
      int end = contents.length - 1;
      if (range != null) {
        String[] bounds = range.substring("bytes=".length()).split("-", -1);
        start = Integer.parseInt(bounds[0]);
        if (!bounds[1].isEmpty()) {
          end = Math.min(end, Integer.parseInt(bounds[1]));
        }
        if (start > end) {
          exchange.getResponseHeaders().set("Content-Range", "bytes */" + contents.length);
          exchange.sendResponseHeaders(416, -1);
          return;
        }
        exchange.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + contents.length);
      }
      exchange.sendResponseHeaders(range == null ? 200 : 206, end - start + 1);
      exchange.getResponseBody().write(contents, start, end - start + 1);
    } finally {
      exchange.close();
    }
  }
}
