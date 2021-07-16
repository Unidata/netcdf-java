/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.gcdm.client;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import ucar.gcdm.*;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisInterval;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridDataset;
import ucar.nc2.grid2.GridReferencedArray;
import ucar.nc2.grid2.GridSubset;
import ucar.nc2.internal.grid2.GridNetcdfCS;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** A remote CDM GridDataset, using gprc protocol to communicate. */
public class GcdmGridDataset implements GridDataset {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GcdmGridDataset.class);
  private static final int MAX_DATA_WAIT_SECONDS = 30;
  private static final int MAX_MESSAGE = 51 * 1000 * 1000; // 51 Mb

  @Override
  public String getName() {
    return proto.getName();
  }

  @Override
  public String getLocation() {
    return proto.getLocation(); // TODO or path?
  }

  @Override
  public AttributeContainer attributes() {
    return GcdmConverter.decodeAttributes(getName(), proto.getAttributesList());
  }

  @Override
  public FeatureType getFeatureType() {
    return GcdmGridConverter.convertFeatureType(proto.getFeatureType());
  }

  @Override
  public ImmutableList<GridCoordinateSystem> getGridCoordinateSystems() {
    return coordsys;
  }

  @Override
  public ImmutableList<GridAxis<?>> getGridAxes() {
    return axes;
  }

  @Override
  public ImmutableList<Grid> getGrids() {
    return grids;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    toString(f);
    return f.toString();
  }

  @Override
  public void close() {
    try {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException interruptedException) {
      log.warn("GcdmGridDataset shutdown interrupted");
      // fall through
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final String remoteURI;
  private final String path;
  private final ManagedChannel channel;
  private final GcdmGrpc.GcdmBlockingStub blockingStub;

  private final GcdmGridProto.GridDataset proto;
  private final ImmutableList<GridAxis<?>> axes;
  private final ImmutableList<GridCoordinateSystem> coordsys;
  private final ImmutableList<Grid> grids;

  private GcdmGridDataset(Builder builder) {
    this.remoteURI = builder.remoteURI;
    this.path = builder.path;
    this.channel = builder.channel;
    this.blockingStub = builder.blockingStub;
    this.proto = builder.proto;

    ImmutableList.Builder<GridAxis<?>> allAxesb = ImmutableList.builder();
    for (GridAxis.Builder<?> axisb : builder.axes) {
      if (axisb instanceof GridAxisPoint.Builder<?>) {
        GridAxisPoint.Builder<?> pointb = (GridAxisPoint.Builder<?>) axisb;
        allAxesb.add(pointb.build());
      } else {
        GridAxisInterval.Builder<?> intvb = (GridAxisInterval.Builder<?>) axisb;
        allAxesb.add(intvb.build());
      }
    }
    this.axes = allAxesb.build();

    ImmutableList.Builder<GridCoordinateSystem> coordsysb = ImmutableList.builder();
    for (GridNetcdfCS.Builder<?> sys : builder.coordsys) {
      coordsysb.add(sys.build(this.axes));
    }
    this.coordsys = coordsysb.build();

    ImmutableList.Builder<Grid> gridsb = ImmutableList.builder();
    for (GcdmGrid.Builder b : builder.grids) {
      gridsb.add(b.setDataset(this).build(this.coordsys));
    }
    this.grids = gridsb.build();
  }

  GridReferencedArray readData(GridSubset subset) throws IOException {
    log.info("GcdmGridDataset request data subset " + subset);
    GcdmGridProto.GridDataRequest.Builder requestb = GcdmGridProto.GridDataRequest.newBuilder().setLocation(path);
    for (Map.Entry<String, Object> entry : subset.getEntries()) {
      requestb.putSubset(entry.getKey(), entry.getValue().toString());
    }
    final Stopwatch stopwatch = Stopwatch.createStarted();
    long size = 0;
    List<GridReferencedArray> results = new ArrayList<>();

    try {
      Iterator<GcdmGridProto.GridDataResponse> responses =
          blockingStub.withDeadlineAfter(MAX_DATA_WAIT_SECONDS, TimeUnit.SECONDS).getGridData(requestb.build());

      // while (responses.hasNext()) {
      GcdmGridProto.GridDataResponse response = responses.next();
      if (response.hasError()) {
        throw new IOException(response.getError().getMessage());
      }
      Formatter errlog = new Formatter();
      GridReferencedArray result =
          GcdmGridConverter.decodeGridReferencedArray(response.getData(), getGridAxes(), errlog);
      results.add(result);
      size += result.data().length();
      // }

    } catch (StatusRuntimeException e) {
      log.warn("readSection requestData failed failed: ", e);
      throw new IOException(e);

    } catch (Throwable t) {
      System.out.printf(" ** failed after %s%n", stopwatch);
      log.warn("readSection requestData failed failed: ", t);
      throw new IOException(t);
    }
    System.out.printf(" ** size=%d took=%s%n", size, stopwatch.stop());

    if (results.size() == 1) {
      return results.get(0);
    } else {
      throw new UnsupportedOperationException("multiple responses not supported"); // TODO
    }
  }

  public Builder toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  private Builder addLocalFieldsToBuilder(Builder b) {
    b.setRemoteURI(this.remoteURI);
    return b;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String remoteURI;
    private ManagedChannel channel;
    private GcdmGrpc.GcdmBlockingStub blockingStub;
    private String path;
    private GcdmGridProto.GridDataset proto;
    private final ArrayList<GridAxis.Builder<?>> axes = new ArrayList<>();
    // LOOK could implement GridCoordinateSystem instead of using GridNetcdfCS
    private final ArrayList<GridNetcdfCS.Builder<?>> coordsys = new ArrayList<>();
    private final ArrayList<GcdmGrid.Builder> grids = new ArrayList<>();

    private boolean built;

    public Builder setRemoteURI(String remoteURI) {
      this.remoteURI = remoteURI;
      return this;
    }

    public Builder addGridAxis(GridAxis.Builder<?> axis) {
      axes.add(axis);
      return this;
    }

    public Builder addCoordSys(GridNetcdfCS.Builder<?> sys) {
      coordsys.add(sys);
      return this;
    }

    public Builder addGrid(GcdmGrid.Builder grid) {
      grids.add(grid);
      return this;
    }

    public GcdmGridDataset build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      openChannel();
      return new GcdmGridDataset(this);
    }

    private void openChannel() {
      // parse the URI
      URI uri = java.net.URI.create(this.remoteURI);
      String target = uri.getAuthority();
      this.path = uri.getPath();
      if (this.path.startsWith("/")) {
        this.path = this.path.substring(1);
      }

      // Create a communication channel to the server, known as a Channel. Channels are thread-safe
      // and reusable. It is common to create channels at the beginning of your application and reuse
      // them until the application shuts down.
      this.channel = ManagedChannelBuilder.forTarget(target)
          // Channels are secure by default (via SSL/TLS). For now, we disable TLS to avoid needing certificates.
          .usePlaintext() //
          .enableFullStreamDecompression() //
          .maxInboundMessageSize(MAX_MESSAGE) //
          .build();
      Formatter errlog = new Formatter();
      try {
        this.blockingStub = GcdmGrpc.newBlockingStub(channel);
        readDataset(errlog);

      } catch (Exception e) {
        // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
        // resources the channel should be shut down when it will no longer be used. If it may be used
        // again leave it running.
        try {
          channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
          log.warn("Shutdown interrupted ", e);
          // fall through
        }
        e.printStackTrace();
        System.out.printf("%nerrlog = %s%n", errlog);
        throw new RuntimeException("Cant open Gcdm url " + this.remoteURI, e);
      }
    }

    private void readDataset(Formatter errlog) {
      log.info("GcdmGridDataset request header for " + path);
      GcdmGridProto.GridDatasetRequest request =
          GcdmGridProto.GridDatasetRequest.newBuilder().setLocation(path).build();
      GcdmGridProto.GridDatasetResponse response = blockingStub.getGridDataset(request);
      if (response.hasError()) {
        throw new RuntimeException(response.getError().getMessage());
      } else {
        this.proto = response.getDataset();
        GcdmGridConverter.decodeGridDataset(this.proto, this, errlog);
      }
    }
  }
}
