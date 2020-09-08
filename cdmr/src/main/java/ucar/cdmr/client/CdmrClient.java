package ucar.cdmr.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import ucar.cdmr.CdmRemoteGrpc;
import ucar.cdmr.CdmRemoteProto.DataRequest;
import ucar.cdmr.CdmRemoteProto.DataResponse;
import ucar.cdmr.CdmRemoteProto.HeaderRequest;
import ucar.cdmr.CdmRemoteProto.HeaderResponse;

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

  public void getHeader(String location) {
    System.out.printf("Header request %s%n", location);
    HeaderRequest request = HeaderRequest.newBuilder().setLocation(location).build();
    HeaderResponse response;
    try {
      response = blockingStub.getHeader(request);
      System.out.printf("Header response %s%n", response);
    } catch (StatusRuntimeException e) {
      logger.warn("getHeader failed: " + location, e);
      e.printStackTrace();
    }
  }

  public void getData(String location, String varSpec) {
    System.out.printf("Data request %s%n", varSpec);
    DataRequest request = DataRequest.newBuilder().setLocation(location).setVariableSpec(varSpec).build();
    Iterator<DataResponse> responses;
    try {
      responses = blockingStub.getData(request);
      while (responses.hasNext()) {
        DataResponse response = responses.next();
        System.out.printf("Data reponse %s%n", response.getSection());
      }
    } catch (StatusRuntimeException e) {
      logger.warn("getData failed: " + location, e);
      e.printStackTrace();
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
      CdmrClient client = new CdmrClient(channel);
      client.getHeader(location);
      client.getData(location, "DELP(0:0, 0:71, 0:720, 0:1151)");
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
