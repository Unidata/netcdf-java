/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.gcdm.server;

import com.google.common.base.Stopwatch;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import ucar.array.*;
import ucar.gcdm.GcdmGrpc.GcdmImplBase;
import ucar.gcdm.GcdmGridConverter;
import ucar.gcdm.GcdmGridProto;
import ucar.gcdm.GcdmNetcdfProto;
import ucar.gcdm.GcdmNetcdfProto.DataRequest;
import ucar.gcdm.GcdmNetcdfProto.DataResponse;
import ucar.gcdm.GcdmNetcdfProto.Header;
import ucar.gcdm.GcdmNetcdfProto.HeaderRequest;
import ucar.gcdm.GcdmNetcdfProto.HeaderResponse;
import ucar.gcdm.GcdmConverter;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedArraySectionSpec;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.geoloc.vertical.VerticalTransform;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.util.Misc;
import ucar.nc2.write.ChunkingIndex;

/**
 * Server that manages startup/shutdown of a gCDM Server.
 * Note that NetcdfDatast / GridDataset is opened/closed on each request.
 * LOOK test caching performance.
 */
public class GcdmServer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GcdmServer.class);
  private static final int MAX_MESSAGE = 50 * 1000 * 1000; // 50 Mb LOOK could be tuned
  private static final int SEQUENCE_CHUNK = 1000;

  private Server server;

  private void start() throws IOException {
    /* The port on which the server should run */
    int port = 16111;
    server = ServerBuilder.forPort(port) //
        .addService(new GcdmImpl()) //
        // .intercept(new MyServerInterceptor())
        .build().start();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          GcdmServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });

    logger.info("Server started, listening on " + port);
    System.out.println("---> Server started, listening on " + port);
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
    Misc.showClassPath();
    final GcdmServer server = new GcdmServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class MyServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata requestHeaders,
        ServerCallHandler<ReqT, RespT> next) {
      System.out.printf("***ServerCall %s%n", call);
      System.out.printf("   Attributes %s%n", call.getAttributes());
      System.out.printf("   MethodDesc %s%n", call.getMethodDescriptor());
      System.out.printf("   Authority %s%n", call.getAuthority());
      System.out.printf("   Metadata %s%n", requestHeaders);
      return next.startCall(call, requestHeaders);
    }
  }

  static class GcdmImpl extends GcdmImplBase {

    @Override
    public void getNetcdfHeader(HeaderRequest req, StreamObserver<HeaderResponse> responseObserver) {
      System.out.printf("GcdmServer getHeader open %s%n", req.getLocation());
      HeaderResponse.Builder response = HeaderResponse.newBuilder();
      try (NetcdfFile ncfile = NetcdfDatasets.openFile(req.getLocation(), null)) {
        Header.Builder header = Header.newBuilder().setLocation(req.getLocation())
            .setRoot(GcdmConverter.encodeGroup(ncfile.getRootGroup(), 100).build());
        response.setHeader(header);
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        logger.info("GcdmServer getHeader " + req.getLocation());
      } catch (Throwable t) {
        logger.warn("GcdmServer getHeader failed ", t);
        t.printStackTrace();
        response.setError(GcdmNetcdfProto.Error.newBuilder().setMessage(t.getMessage()).build());
      }
    }

    @Override
    public void getNetcdfData(DataRequest req, StreamObserver<DataResponse> responseObserver) {
      System.out.printf("GcdmServer getData %s %s%n", req.getLocation(), req.getVariableSpec());
      final Stopwatch stopwatch = Stopwatch.createStarted();
      long size = -1;

      try (NetcdfFile ncfile = NetcdfDatasets.openFile(req.getLocation(), null)) { // LOOK cache ncfile?
        ParsedArraySectionSpec varSection = ParsedArraySectionSpec.parseVariableSection(ncfile, req.getVariableSpec());
        Variable var = varSection.getVariable();
        if (var instanceof Sequence) {
          size = getSequenceData(ncfile, varSection, responseObserver);
        } else {
          Section wantSection = varSection.getSection();
          size = var.getElementSize() * wantSection.computeSize();
          getNetcdfData(ncfile, varSection, responseObserver);
        }
        responseObserver.onCompleted();
        logger.info("GcdmServer getData " + req.getLocation());

      } catch (Throwable t) {
        logger.warn("GcdmServer getData failed ", t);
        t.printStackTrace();
        DataResponse.Builder response =
            DataResponse.newBuilder().setLocation(req.getLocation()).setVariableSpec(req.getVariableSpec());
        response.setError(
            GcdmNetcdfProto.Error.newBuilder().setMessage(t.getMessage() == null ? "N/A" : t.getMessage()).build());
        responseObserver.onNext(response.build());
      }

      System.out.printf(" ** size=%d took=%s%n", size, stopwatch.stop());
    }

    private void getNetcdfData(NetcdfFile ncfile, ParsedArraySectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws IOException, InvalidRangeException {
      Variable var = varSection.getVariable();
      Section wantSection = varSection.getSection();
      long size = var.getElementSize() * wantSection.computeSize();
      if (size > MAX_MESSAGE) {
        getDataInChunks(ncfile, varSection, responseObserver);
      } else {
        getOneChunk(ncfile, varSection, responseObserver);
      }
    }

    private void getDataInChunks(NetcdfFile ncfile, ParsedArraySectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws IOException, InvalidRangeException {

      Variable var = varSection.getVariable();
      long maxChunkElems = MAX_MESSAGE / var.getElementSize();
      // LOOK wrong this assume starts at 0, should start at varSection
      ChunkingIndex index = new ChunkingIndex(var.getShape());
      while (index.currentElement() < index.size()) {
        int[] chunkOrigin = index.currentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);
        Section section = new Section(chunkOrigin, chunkShape);
        ParsedArraySectionSpec spec = new ParsedArraySectionSpec(var, section);
        getOneChunk(ncfile, spec, responseObserver);
        index.setCurrentCounter(index.currentElement() + (int) Arrays.computeSize(chunkShape));
      }
    }

    private void getOneChunk(NetcdfFile ncfile, ParsedArraySectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws IOException, InvalidRangeException {

      String spec = varSection.makeSectionSpecString();
      Variable var = varSection.getVariable();
      Section wantSection = varSection.getSection();

      DataResponse.Builder response = DataResponse.newBuilder().setLocation(ncfile.getLocation()).setVariableSpec(spec)
          .setVarFullName(var.getFullName()).setSection(GcdmConverter.encodeSection(wantSection));

      Array<?> data = var.readArray(wantSection);
      response.setData(GcdmConverter.encodeData(data.getArrayType(), data));

      responseObserver.onNext(response.build());
      System.out.printf(" Send one chunk %s size=%d bytes%n", spec,
          data.length() * varSection.getVariable().getElementSize());
    }


    private long getSequenceData(NetcdfFile ncfile, ParsedArraySectionSpec varSection,
        StreamObserver<DataResponse> responseObserver) throws InvalidRangeException {

      String spec = varSection.makeSectionSpecString();
      Sequence seq = (Sequence) varSection.getVariable();
      StructureMembers.Builder membersb = seq.makeStructureMembersBuilder();
      membersb.setStandardOffsets(false);
      StructureMembers members = membersb.build();

      StructureData[] sdata = new StructureData[SEQUENCE_CHUNK];
      int start = 0;
      int count = 0;
      Iterator<StructureData> it = seq.iterator();
      while (it.hasNext()) {
        sdata[count++] = it.next();

        if (count >= SEQUENCE_CHUNK || !it.hasNext()) {
          StructureDataArray sdataArray = new StructureDataArray(members, new int[] {count}, sdata);
          Section section = Section.builder().appendRange(start, start + count).build();
          DataResponse.Builder response = DataResponse.newBuilder().setLocation(ncfile.getLocation())
              .setVariableSpec(spec).setVarFullName(seq.getFullName()).setSection(GcdmConverter.encodeSection(section));
          response.setData(GcdmConverter.encodeData(ArrayType.SEQUENCE, sdataArray));
          responseObserver.onNext(response.build());
          start = count;
          count = 0;
        }
      }
      return (start + count) * members.getStorageSizeBytes();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // GridDataset

    @Override
    public void getGridDataset(GcdmGridProto.GridDatasetRequest request,
        io.grpc.stub.StreamObserver<GcdmGridProto.GridDatasetResponse> responseObserver) {
      System.out.printf("GcdmServer getGridDataset open %s%n", request.getLocation());
      GcdmGridProto.GridDatasetResponse.Builder response = GcdmGridProto.GridDatasetResponse.newBuilder();
      Formatter errlog = new Formatter();
      try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(request.getLocation(), errlog)) {
        if (gridDataset == null) {
          response.setError(GcdmNetcdfProto.Error.newBuilder()
              .setMessage(String.format("Dataset '%s' not found or not a GridDataset", request.getLocation())).build());
        } else {
          response.setDataset(GcdmGridConverter.encodeGridDataset(gridDataset));
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        logger.info("GcdmServer getGridDataset " + request.getLocation());
      } catch (Throwable t) {
        System.out.printf("GcdmServer getGridDataset failed %s %n%s%n", t.getMessage(), errlog);
        logger.warn("GcdmServer getGridDataset failed ", t);
        t.printStackTrace();
        response.setError(GcdmNetcdfProto.Error.newBuilder().setMessage(t.getMessage()).build());
      }
    }

    @Override
    public void getGridData(GcdmGridProto.GridDataRequest request,
        io.grpc.stub.StreamObserver<ucar.gcdm.GcdmGridProto.GridDataResponse> responseObserver) {

      System.out.printf("GcdmServer getData %s %s%n", request.getLocation(), request.getSubsetMap());
      GcdmGridProto.GridDataResponse.Builder response = GcdmGridProto.GridDataResponse.newBuilder();
      response.setLocation(request.getLocation()).putAllSubset(request.getSubsetMap());
      final Stopwatch stopwatch = Stopwatch.createStarted();

      GridSubset gridSubset = GridSubset.fromStringMap(request.getSubsetMap());
      if (gridSubset.getGridName() == null) {
        makeError(response, "GridName is not set");
        responseObserver.onNext(response.build());
        return;
      }

      Formatter errlog = new Formatter();
      try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(request.getLocation(), errlog)) {
        if (gridDataset == null) {
          makeError(response, String.format("GridDataset '%s' not found", request.getLocation()));
        } else {
          String wantGridName = gridSubset.getGridName();
          Grid wantGrid = gridDataset.findGrid(wantGridName).orElse(null);
          if (wantGrid == null) {
            makeError(response,
                String.format("GridDataset '%s' does not have Grid '%s", request.getLocation(), wantGridName));
          } else {
            GridReferencedArray geoReferencedArray = wantGrid.readData(gridSubset);
            response.setData(GcdmGridConverter.encodeGridReferencedArray(geoReferencedArray));
            System.out.printf(" ** size=%d shape=%s%n", geoReferencedArray.data().length(),
                java.util.Arrays.toString(geoReferencedArray.data().getShape()));

          }
        }

      } catch (Throwable t) {
        logger.warn("GcdmServer getGridData failed ", t);
        t.printStackTrace();
        errlog.format("%n%s", t.getMessage() == null ? "" : t.getMessage());
        makeError(response, errlog.toString());
      }
      responseObserver.onNext(response.build());
      System.out.printf(" ** took=%s%n", stopwatch.stop());
    }

    void makeError(GcdmGridProto.GridDataResponse.Builder response, String message) {
      response.setError(GcdmNetcdfProto.Error.newBuilder().setMessage(message).build());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // VerticalTransform

    @Override
    public void getVerticalTransform(GcdmGridProto.VerticalTransformRequest request,
        io.grpc.stub.StreamObserver<GcdmGridProto.VerticalTransformResponse> responseObserver) {
      System.out.printf("GcdmServer getVerticalTransform open %s%n", request.getLocation());
      GcdmGridProto.VerticalTransformResponse.Builder response = GcdmGridProto.VerticalTransformResponse.newBuilder();
      Formatter errlog = new Formatter();
      try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(request.getLocation(), errlog)) {
        response.setLocation(request.getLocation());
        response.setVerticalTransform(request.getVerticalTransform());
        response.setTimeIndex(request.getTimeIndex());

        if (gridDataset == null) {
          response.setError(
              GcdmNetcdfProto.Error.newBuilder().setMessage("Dataset not found or not a GridDataset").build());
        } else {
          Optional<VerticalTransform> cto = gridDataset.findVerticalTransformByHash(request.getId());
          if (cto.isPresent()) {
            Array<?> data = cto.get().getCoordinateArray3D(request.getTimeIndex());
            response.setData3D(GcdmConverter.encodeData(data.getArrayType(), data));
          } else {
            response.setError(GcdmNetcdfProto.Error.newBuilder().setMessage("VerticalTransform not found").build());
          }
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        logger.info("GcdmServer getVerticalTransform " + request.getLocation());
      } catch (Throwable t) {
        System.out.printf("GcdmServer getVerticalTransform failed %s %n%s%n", t.getMessage(), errlog);
        logger.warn("GcdmServer getVerticalTransform failed ", t);
        t.printStackTrace();
        response.setError(GcdmNetcdfProto.Error.newBuilder().setMessage(t.getMessage()).build());
      }
    }


  } // GcdmImpl
}
