package ucar.cdmr.server;

import com.google.common.base.Stopwatch;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import ucar.cdmr.CdmRemoteGrpc.CdmRemoteImplBase;
import ucar.cdmr.CdmRemoteProto;
import ucar.cdmr.CdmRemoteProto.DataRequest;
import ucar.cdmr.CdmRemoteProto.DataResponse;
import ucar.cdmr.CdmRemoteProto.Header;
import ucar.cdmr.CdmRemoteProto.HeaderRequest;
import ucar.cdmr.CdmRemoteProto.HeaderResponse;
import ucar.cdmr.CdmrConverter;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayStructure;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.write.ChunkingIndex;

/** Server that manages startup/shutdown of a Cdm Remote server. */
public class CdmrServer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CdmrServer.class);
  private static final int MAX_MESSAGE = 50 * 1000 * 1000; // 50 Mb

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

  /** Await termination on the main thread since the grpc library uses daemon threads. */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /** Main launches the server from the command line. */
  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Working Directory = " + System.getProperty("user.dir"));
    final CdmrServer server = new CdmrServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class CdmRemoteImpl extends CdmRemoteImplBase {

    @Override
    public void getHeader(HeaderRequest req, StreamObserver<HeaderResponse> responseObserver) {
      System.out.printf("CdmrServer getHeader open %s%n", req.getLocation());
      HeaderResponse.Builder response = HeaderResponse.newBuilder();
      try (NetcdfFile ncfile = NetcdfDatasets.openFile(req.getLocation(), null)) {
        Header.Builder header = Header.newBuilder().setLocation(req.getLocation())
            .setRoot(CdmrConverter.encodeGroup(ncfile.getRootGroup(), 100).build());
        response.setHeader(header);
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        logger.info("CdmrServer getHeader " + req.getLocation());
      } catch (Throwable t) {
        logger.warn("CdmrServer getHeader failed ", t);
        t.printStackTrace();
        response.setError(CdmRemoteProto.Error.newBuilder().setMessage(t.getMessage()).build());
      }
    }

    @Override
    public void getData(DataRequest req, StreamObserver<DataResponse> responseObserver) {
      System.out.printf("CdmrServer getData %s %s%n", req.getLocation(), req.getVariableSpec());
      final Stopwatch stopwatch = Stopwatch.createStarted();
      long size = -1;

      try (NetcdfFile ncfile = NetcdfDatasets.openFile(req.getLocation(), null)) { // LOOK cache ncfile?
        ParsedSectionSpec varSection = ParsedSectionSpec.parseVariableSection(ncfile, req.getVariableSpec());
        Variable var = varSection.getVariable();
        Section wantSection = varSection.getSection();
        size = var.getElementSize() * wantSection.getSize();
        getData(ncfile, varSection, responseObserver);
        responseObserver.onCompleted();
        logger.info("CdmrServer getData " + req.getLocation());

      } catch (Throwable t) {
        logger.warn("CdmrServer getData failed ", t);
        t.printStackTrace();
        DataResponse.Builder response =
            DataResponse.newBuilder().setLocation(req.getLocation()).setVariableSpec(req.getVariableSpec());
        response.setError(CdmRemoteProto.Error.newBuilder().setMessage(t.getMessage()).build());
        responseObserver.onNext(response.build());
      }

      System.out.printf(" ** size=%d took=%s%n", size, stopwatch.stop());
    }

    private void getData(NetcdfFile ncfile, ParsedSectionSpec varSection, StreamObserver<DataResponse> responseObserver)
        throws IOException, InvalidRangeException {

      Variable var = varSection.getVariable();
      Section wantSection = varSection.getSection();
      long size = var.getElementSize() * wantSection.getSize();
      if (size > MAX_MESSAGE) {
        getDataInChunks(ncfile, varSection, responseObserver);
      } else {
        getOneChunk(ncfile, varSection, responseObserver);
      }
    }

    private void getDataInChunks(NetcdfFile ncfile, ParsedSectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws IOException, InvalidRangeException {

      Variable var = varSection.getVariable();
      long maxChunkElems = MAX_MESSAGE / var.getElementSize();
      ChunkingIndex index = new ChunkingIndex(var.getShape()); // LOOK wrong this assume starts at 0, should start at
                                                               // varSection
      while (index.currentElement() < index.getSize()) {
        int[] chunkOrigin = index.getCurrentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);
        Section section = new Section(chunkOrigin, chunkShape);
        getOneChunk(ncfile, new ParsedSectionSpec(var, section), responseObserver);
        index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));
      }
    }

    private void getOneChunk(NetcdfFile ncfile, ParsedSectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws IOException, InvalidRangeException {

      String spec = varSection.makeSectionSpecString();
      DataResponse.Builder response = DataResponse.newBuilder().setLocation(ncfile.getLocation()).setVariableSpec(spec);
      Array data = ncfile.readSection(spec);
      response.setSection(CdmrConverter.encodeSection(varSection.getSection()));

      if (varSection.getVariable().isVariableLength()) { // LOOK CANT CHUNK VLEN I THINK ??
        response.setData(CdmrConverter.encodeVlenData(data.getDataType(), (ArrayObject) data));
        response.setIsVariableLength(true);
      } else if (data instanceof ArrayStructure) {
        response.setData(CdmrConverter.encodeArrayStructureData((ArrayStructure) data));
      } else {
        response.setData(CdmrConverter.encodeData(data.getDataType(), data));
      }
      responseObserver.onNext(response.build());
      System.out.printf(" Send one chunk %s size=%d bytes%n", spec,
          data.getSize() * varSection.getVariable().getElementSize());
    }

  }
}
