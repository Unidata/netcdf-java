/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm.server;

import com.google.common.base.Stopwatch;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import ucar.gcdm.GcdmGrpc.GcdmImplBase;
import ucar.gcdm.GcdmNetcdfProto;
import ucar.gcdm.GcdmNetcdfProto.DataRequest;
import ucar.gcdm.GcdmNetcdfProto.DataResponse;
import ucar.gcdm.GcdmNetcdfProto.Header;
import ucar.gcdm.GcdmNetcdfProto.HeaderRequest;
import ucar.gcdm.GcdmNetcdfProto.HeaderResponse;
import ucar.gcdm.GcdmConverter;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.write.ChunkingIndex;

/** Server that manages startup/shutdown of a gCDM Server. */
public class GcdmServer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GcdmServer.class);

  // public for testing
  public static final int MAX_MESSAGE = 50 * 1000 * 1000; // 50 Mb LOOK could be tuned
  private static final int SEQUENCE_CHUNK = 1000;
  private static final int PORT = 16111;

  private Server server;

  private void start() throws IOException {
    server = ServerBuilder.forPort(PORT).addService(new GcdmImpl()).build().start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // Use stderr here since the logger may have been reset by its JVM shutdown hook.
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      try {
        GcdmServer.this.stop();
      } catch (InterruptedException e) {
        e.printStackTrace(System.err);
      }
      System.err.println("*** server shut down");
    }));

    logger.info("Server started, listening on " + PORT);
    System.out.println("---> Server started, listening on " + PORT); // Used for gradle startDaemon
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
    final GcdmServer server = new GcdmServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class GcdmImpl extends GcdmImplBase {
    @Override
    public void getNetcdfHeader(HeaderRequest req, StreamObserver<HeaderResponse> responseObserver) {
      logger.info("GcdmServer getHeader " + req.getLocation());
      final HeaderResponse.Builder response = HeaderResponse.newBuilder();
      try (NetcdfFile ncfile = NetcdfDatasets.openFile(req.getLocation(), null)) {
        final Header.Builder header = Header.newBuilder().setLocation(req.getLocation())
            .setRoot(GcdmConverter.encodeGroup(ncfile.getRootGroup(), 100).build());
        response.setHeader(header);
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
      } catch (Throwable t) {
        logger.warn("GcdmServer getHeader failed ", t);
        t.printStackTrace();
        response.setError(GcdmNetcdfProto.Error.newBuilder().setMessage(t.getMessage()).build());
      }
    }

    @Override
    public void getNetcdfData(DataRequest req, StreamObserver<DataResponse> responseObserver) {
      logger.info("GcdmServer getData {} {}", req.getLocation(), req.getVariableSpec());
      final Stopwatch stopwatch = Stopwatch.createStarted();
      long size = -1;

      try (NetcdfFile ncfile = NetcdfDatasets.openFile(req.getLocation(), null)) { // LOOK cache ncfile?
        final ParsedSectionSpec varSection = ParsedSectionSpec.parseVariableSection(ncfile, req.getVariableSpec());
        final Variable var = varSection.getVariable();
        if (var instanceof Sequence) {
          size = getSequenceData(ncfile, varSection, responseObserver);
        } else {
          final Section wantSection = varSection.getArraySection();
          size = var.getElementSize() * wantSection.computeSize();
          getNetcdfData(ncfile, varSection, responseObserver);
        }
        responseObserver.onCompleted();
      } catch (Throwable t) {
        logger.warn("GcdmServer getData failed ", t);
        t.printStackTrace();
        final DataResponse.Builder response =
            DataResponse.newBuilder().setLocation(req.getLocation()).setVariableSpec(req.getVariableSpec());
        response.setError(
            GcdmNetcdfProto.Error.newBuilder().setMessage(t.getMessage() == null ? "N/A" : t.getMessage()).build());
        responseObserver.onNext(response.build());
      }

      logger.debug(" ** size={} took={}", size, stopwatch.stop());
    }

    private void getNetcdfData(NetcdfFile ncfile, ParsedSectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws IOException, InvalidRangeException {
      final Variable var = varSection.getVariable();
      final Section wantSection = varSection.getArraySection();
      long size = var.getElementSize() * wantSection.computeSize();
      if (size > MAX_MESSAGE) {
        getDataInChunks(ncfile, varSection, responseObserver);
      } else {
        getOneChunk(ncfile, varSection, responseObserver);
      }
    }

    // TODO this returns structure member data in one chunk no matter the size, see testDataChunkingForStructures
    private void getDataInChunks(NetcdfFile ncfile, ParsedSectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws IOException, InvalidRangeException {

      final Variable var = varSection.getVariable();
      final Section section = varSection.getArraySection();
      long maxChunkElems = MAX_MESSAGE / var.getElementSize();
      final ChunkingIndex index = new ChunkingIndex(section.getShape());
      while (index.currentElement() < index.getSize()) {
        final int[] chunkOrigin = index.getCurrentCounter();
        final int[] chunkShape = index.computeChunkShape(maxChunkElems);
        final Section chunkSection = new Section(chunkOrigin, chunkShape);
        final ParsedSectionSpec spec = new ParsedSectionSpec(var, chunkSection);
        getOneChunk(ncfile, spec, responseObserver);
        index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));
      }
    }

    private void getOneChunk(NetcdfFile ncfile, ParsedSectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws IOException, InvalidRangeException {

      final String spec = varSection.makeSectionSpecString();
      final Variable var = varSection.getVariable();
      final Section wantSection = varSection.getArraySection();

      final DataResponse.Builder response = DataResponse.newBuilder().setLocation(ncfile.getLocation())
          .setVariableSpec(spec).setVarFullName(var.getFullName());

      final Array data = var.read(wantSection);
      response.setData(GcdmConverter.encodeData(data.getDataType(), data));

      responseObserver.onNext(response.build());
      logger.debug("Send one chunk {} size={} bytes", spec, data.getSize() * varSection.getVariable().getElementSize());
    }

    // TODO count >= SEQUENCE_CHUNK is not covered in tests
    private long getSequenceData(NetcdfFile ncfile, ParsedSectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws IOException {

      final String spec = varSection.makeSectionSpecString();
      final Sequence seq = (Sequence) varSection.getVariable();
      final StructureMembers members = seq.makeStructureMembers();

      StructureData[] structureData = new StructureData[SEQUENCE_CHUNK];
      int start = 0;
      int count = 0;
      StructureDataIterator it = seq.getStructureIterator();
      while (it.hasNext()) {
        structureData[count++] = it.next();

        if (count >= SEQUENCE_CHUNK || !it.hasNext()) {
          final StructureData[] correctSizeArray = Arrays.copyOf(structureData, count);
          final ArrayStructureW arrayStructure = new ArrayStructureW(members, new int[] {count}, correctSizeArray);
          final DataResponse.Builder response = DataResponse.newBuilder().setLocation(ncfile.getLocation())
              .setVariableSpec(spec).setVarFullName(seq.getFullName());
          response.setData(GcdmConverter.encodeData(DataType.SEQUENCE, arrayStructure));
          responseObserver.onNext(response.build());
          start = count;
          count = 0;
        }
      }
      return (long) (start + count) * members.getStructureSize();
    }
  } // GcdmImpl
}
