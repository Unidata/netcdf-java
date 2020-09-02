package ucar.cdmr.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.cdmr.CdmRemoteGrpc;
import ucar.cdmr.CdmRemoteProto.DataRequest;
import ucar.cdmr.CdmRemoteProto.DataResponse;
import ucar.cdmr.CdmRemoteProto.Header;
import ucar.cdmr.CdmRemoteProto.HeaderRequest;

/** A simple client that makes a request from CdmrServer. */
public class CdmrClient {
  private static final Logger logger = Logger.getLogger(CdmrClient.class.getName());

  private final CdmRemoteGrpc.CdmRemoteBlockingStub blockingStub;

  /** Construct client for accessing HelloWorld server using the existing channel. */
  public CdmrClient(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
    blockingStub = CdmRemoteGrpc.newBlockingStub(channel);
  }

  public void getHeader(String location) {
    logger.info("Header request " + location);
    HeaderRequest request = HeaderRequest.newBuilder().setLocation(location).build();
    Header response;
    try {
      response = blockingStub.getHeader(request);
      logger.info("getHeader response: " + response);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "getHeader failed: {0}", e.getStatus());
      e.printStackTrace();
    }
  }

  public void getData(String location, String varSpec) {
    logger.info("Data request " + varSpec);
    DataRequest request = DataRequest.newBuilder().setLocation(location).setVariableSpec(varSpec).build();
    DataResponse response;
    try {
      response = blockingStub.getData(request);
      logger.info("getData response: " + response);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "getData failed: {0}", e.getStatus());
      e.printStackTrace();
    }
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting. The second argument is the target server.
   */
  public static void main(String[] args) throws Exception {
    String location = "C:/dev/github/netcdf-java/cdm/core/src/test/data/testWrite.nc";
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

    String url = "cdmr://localhost:16111/C:/dev/github/netcdf-java/cdm/core/src/test/data/testWrite.nc";
    URI what = java.net.URI.create(url);
    System.out.printf("%s%n", what);

    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable. It is common to create channels at the beginning of your application and reuse
    // them until the application shuts down.
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext().build();
    try {
      CdmrClient client = new CdmrClient(channel);
      client.getHeader(location);
      client.getData(location, "bvar(0:4:2)");
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
