package ucar.cdmr.client;

import com.google.common.base.Stopwatch;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.cdmr.CdmRemoteGrpc;
import ucar.cdmr.CdmrNetcdfProto.DataRequest;
import ucar.cdmr.CdmrNetcdfProto.DataResponse;
import ucar.cdmr.CdmrNetcdfProto.Header;
import ucar.cdmr.CdmrNetcdfProto.HeaderRequest;
import ucar.cdmr.CdmrNetcdfProto.HeaderResponse;
import ucar.cdmr.CdmrNetcdfProto.Variable;
import ucar.cdmr.CdmrConverter;
import ucar.array.Array;
import ucar.ma2.Section;

/** A simple client that makes a Netcdf request from CdmrServer. Used for testing. */
public class CdmrClient {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CdmrClient.class);
  private static final int MAX_MESSAGE = 51 * 1000 * 1000; // 51 Mb

  private final CdmRemoteGrpc.CdmRemoteBlockingStub blockingStub;
  private static final String cdmUnitTestDir = "D:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/";
  private static final String localFilename =
      cdmUnitTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";

  /** Construct client for accessing HelloWorld server using the existing channel. */
  public CdmrClient(Channel channel) {
    blockingStub = CdmRemoteGrpc.newBlockingStub(channel);
  }

  private Header getHeader(String location) {
    System.out.printf("Header request %s%n", location);
    HeaderRequest request = HeaderRequest.newBuilder().setLocation(location).build();
    HeaderResponse response;
    try {
      response = blockingStub.getNetcdfHeader(request);
      // System.out.printf("Header response %s%n", response);
      return response.getHeader();
    } catch (StatusRuntimeException e) {
      logger.warn("getHeader failed: " + location, e);
      e.printStackTrace();
    }
    return null;
  }

  private <T> Array<T> getData(String location, Variable v) {
    ArrayType dataType = CdmrConverter.convertDataType(v.getDataType());
    Section section = CdmrConverter.decodeSection(v);
    System.out.printf("Data request %s %s (%s)%n", v.getDataType(), v.getName(), section);
    if (dataType != ArrayType.DOUBLE && dataType != ArrayType.FLOAT) {
      System.out.printf("***skip%n");
      return null;
    }
    DataRequest request = DataRequest.newBuilder().setLocation(location).setVariableSpec(v.getName()).build();
    Iterator<DataResponse> responses;
    try {
      responses = blockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).getNetcdfData(request);
      List<Array<T>> results = new ArrayList<>();
      while (responses.hasNext()) {
        DataResponse response = responses.next();
        results.add(CdmrConverter.decodeData(response.getData()));
      }
      return Arrays.factoryCopy(dataType, section.getShape(), results);
    } catch (Throwable e) {
      logger.warn("getData failed: " + location, e);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting. The second argument is the target server.
   */
  public static void main(String[] args) throws Exception {
    String location = localFilename;
    String target = "localhost:16111";
    if (args.length > 0) {
      if ("--help".equals(args[0])) {
        System.err.printf("Usage: [name [target]]%n%n");
        System.err.printf("  location Get Header for this location. Defaults to %s%n", location);
        System.err.printf("  target  The server to connect to. Defaults to %s%n", target);
        System.exit(1);
      }
      location = args[0];
    }
    if (args.length > 1) {
      target = args[1];
    }

    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().enableFullStreamDecompression()
        .maxInboundMessageSize(MAX_MESSAGE).usePlaintext().build();
    try {
      Stopwatch stopwatchAll = Stopwatch.createStarted();
      CdmrClient client = new CdmrClient(channel);
      Header header = client.getHeader(location);
      long total = 0;
      for (Variable v : header.getRoot().getVarsList()) {
        Stopwatch s2 = Stopwatch.createStarted();
        Array<?> array = client.getData(location, v);
        s2.stop();
        if (array != null) {
          long size = array.length();
          double rate = ((double) size) / s2.elapsed(TimeUnit.MICROSECONDS);
          System.out.printf("    size = %d, time = %s rate = %10.4f MB/sec%n", size, s2, rate);
          total += size;
        }
      }
      stopwatchAll.stop();
      double rate = ((double) total) / stopwatchAll.elapsed(TimeUnit.MICROSECONDS);
      System.out.printf("*** %d bytes took %s = %10.4f MB/sec%n", total, stopwatchAll, rate);
    } finally {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
