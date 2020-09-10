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
import ucar.cdmr.CdmRemoteGrpc;
import ucar.cdmr.CdmRemoteProto.DataRequest;
import ucar.cdmr.CdmRemoteProto.DataResponse;
import ucar.cdmr.CdmRemoteProto.Header;
import ucar.cdmr.CdmRemoteProto.HeaderRequest;
import ucar.cdmr.CdmRemoteProto.HeaderResponse;
import ucar.cdmr.CdmRemoteProto.Variable;
import ucar.cdmr.CdmrDataToMa;
import ucar.array.Array;
import ucar.ma2.DataType;
import ucar.ma2.Section;

/** A simple client that makes a request from CdmrServer. */
public class CdmrClient {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CdmrClient.class);
  private static final int MAX_MESSAGE = 51 * 1000 * 1000; // 51 Mb

  private final CdmRemoteGrpc.CdmRemoteBlockingStub blockingStub;

  /** Construct client for accessing HelloWorld server using the existing channel. */
  public CdmrClient(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
    blockingStub = CdmRemoteGrpc.newBlockingStub(channel);
  }

  public Header getHeader(String location) {
    System.out.printf("Header request %s%n", location);
    HeaderRequest request = HeaderRequest.newBuilder().setLocation(location).build();
    HeaderResponse response;
    try {
      response = blockingStub.getHeader(request);
      // System.out.printf("Header response %s%n", response);
      return response.getHeader();
    } catch (StatusRuntimeException e) {
      logger.warn("getHeader failed: " + location, e);
      e.printStackTrace();
    }
    return null;
  }

  public Array<?> getData(String location, Variable v) {
    DataType dataType = CdmrDataToMa.convertDataType(v.getDataType());
    Section section = CdmrDataToMa.decodeSection(v);
    System.out.printf("Data request %s %s (%s)%n", v.getDataType(), v.getName(), section);
    if (dataType != DataType.DOUBLE && dataType != DataType.FLOAT) {
      System.out.printf("***skip%n");
      return null;
    }
    DataRequest request = DataRequest.newBuilder().setLocation(location).setVariableSpec(v.getName()).build();
    Iterator<DataResponse> responses;
    try {
      responses = blockingStub.getData(request);
      List<Array> results = new ArrayList<>();
      while (responses.hasNext()) {
        DataResponse response = responses.next();
        results.add(CdmrDataToMa.decodeData(response.getData(), response.getSection()));
      }
      return Array.factoryCopy(dataType, section.getShape(), results);
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
    String location2 = "C:/dev/github/netcdf-java/cdm/core/src/test/data/testWrite.nc";
    String location =
        "D:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";
    // Access a service running on the local machine on port 50051
    String target = "localhost:16111";
    // Allow passing in the user and target strings as command line arguments
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

    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable. It is common to create channels at the beginning of your application and reuse
    // them until the application shuts down.
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().enableFullStreamDecompression()
        .maxInboundMessageSize(MAX_MESSAGE).usePlaintext().build();
    try {
      Stopwatch stopwatch = Stopwatch.createStarted();
      CdmrClient client = new CdmrClient(channel);
      Header header = client.getHeader(location);
      for (Variable v : header.getRoot().getVarsList()) {
        Array<?> data = client.getData(location, v);
        if (data != null) {
          Stopwatch s2 = Stopwatch.createStarted();
          System.out.printf(" sum=%s took=%s%n", data.sum(), s2.stop());
        }
      }
      System.out.printf("That took %s%n", stopwatch.stop());
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
