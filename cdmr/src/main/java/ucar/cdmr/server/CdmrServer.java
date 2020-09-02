package ucar.cdmr.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import ucar.cdmr.CdmRemoteGrpc.CdmRemoteImplBase;
import ucar.cdmr.CdmRemoteProto;
import ucar.cdmr.CdmRemoteProto.DataRequest;
import ucar.cdmr.CdmRemoteProto.DataResponse;
import ucar.cdmr.CdmRemoteProto.Header;
import ucar.cdmr.CdmRemoteProto.HeaderRequest;
import ucar.cdmr.CdmToProto;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.dataset.NetcdfDatasets;

/** Server that manages startup/shutdown of a Cdm Remote server. */
public class CdmrServer {
  private static final Logger logger = Logger.getLogger(CdmrServer.class.getName());

  private Server server;

  private void start() throws IOException {
    /* The port on which the server should run */
    int port = 16111;
    server = ServerBuilder.forPort(port).addService(new CdmRemoteImpl()).build().start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          CdmrServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final CdmrServer server = new CdmrServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class CdmRemoteImpl extends CdmRemoteImplBase {
    @Override
    public void getHeader(HeaderRequest req, StreamObserver<Header> responseObserver) {
      try (NetcdfFile ncfile = NetcdfDatasets.openFile(req.getLocation(), null)) {
        Header.Builder reply = Header.newBuilder().setLocation(req.getLocation())
            .setRoot(CdmToProto.encodeGroup(ncfile.getRootGroup(), 100).build());
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
        logger.info("CdmrServer getHeader " + req.getLocation());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void getData(DataRequest req, StreamObserver<DataResponse> responseObserver) {
      // LOOK cache ncfile
      try (NetcdfFile ncfile = NetcdfDatasets.openFile(req.getLocation(), null)) {
        DataResponse.Builder reply =
            DataResponse.newBuilder().setLocation(req.getLocation()).setVariableSpec(req.getVariableSpec());
        try {
          ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ncfile, req.getVariableSpec());
          Array data = ncfile.readSection(req.getVariableSpec());
          reply.setSection(CdmToProto.encodeSection(cer.getSection()));
          reply.setData(CdmToProto.encodeData(data.getDataType(), data));
        } catch (Throwable t) {
          reply.setError(CdmRemoteProto.Error.newBuilder().setMessage(t.getMessage()).build());
        }
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
        logger.info("CdmrServer getData " + req.getLocation());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
