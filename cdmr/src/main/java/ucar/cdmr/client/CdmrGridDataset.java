package ucar.cdmr.client;

import com.google.common.collect.ImmutableList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ucar.cdmr.*;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.grid.*;
import ucar.nc2.internal.grid.GridCS;

import java.net.URI;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** A remote CDM GridDataset, using gprc protocol to communicate. */
public class CdmrGridDataset implements GridDataset {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CdmrGridDataset.class);
  private static final int MAX_MESSAGE = 51 * 1000 * 1000; // 51 Mb
  public static final String PROTOCOL = "cdmr";
  public static final String SCHEME = PROTOCOL + ":";

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
    return CdmrConverter.decodeAttributes(getName(), proto.getAttributesList());
  }

  @Override
  public FeatureType getFeatureType() {
    return CdmrGridConverter.convertFeatureType(proto.getFeatureType());
  }

  @Override
  public ImmutableList<GridCoordinateSystem> getGridCoordinateSystems() {
    return coordsys;
  }

  @Override
  public ImmutableList<GridAxis> getGridAxes() {
    return axes;
  }

  @Override
  public ImmutableList<Grid> getGrids() {
    return grids;
  }

  @Override
  public Optional<Grid> findGrid(String name) {
    for (Grid grid : getGrids()) {
      if (name.equals(grid.getName())) {
        return Optional.of(grid);
      }
    }
    return Optional.empty();
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
      log.warn("CdmrGridDataset shutdown interrupted");
      // fall through
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final String remoteURI;
  private final String path;
  private final ManagedChannel channel;
  private final CdmRemoteGrpc.CdmRemoteBlockingStub blockingStub;

  private final CdmrGridProto.GridDataset proto;
  private final ImmutableList<GridAxis> axes;
  private final ImmutableList<GridCoordinateSystem> coordsys;
  private final ImmutableList<Grid> grids;

  private CdmrGridDataset(Builder builder) {
    this.remoteURI = builder.remoteURI;
    this.path = builder.path;
    this.channel = builder.channel;
    this.blockingStub = builder.blockingStub;
    this.proto = builder.proto;

    // Have to set runtime axis into GridAxisOffsetTimeRegular
    ImmutableList.Builder<GridAxis> allAxesb = ImmutableList.builder();
    List<GridAxis1DTime> runtimeAxes = new ArrayList<>();
    for (GridAxis.Builder<?> axisb : builder.axes) {
      if (axisb.axisType == AxisType.RunTime) {
        GridAxis1DTime runtimeAxis = (GridAxis1DTime) axisb.build();
        allAxesb.add(runtimeAxis);
        runtimeAxes.add(runtimeAxis);
      }
    }
    for (GridAxis.Builder<?> axisb : builder.axes) {
      if (axisb.axisType != AxisType.RunTime) {
        if (axisb instanceof GridAxisOffsetTimeRegular.Builder<?>) {
          ((GridAxisOffsetTimeRegular.Builder<?>) axisb).setRuntimeAxis(runtimeAxes);
        }
        allAxesb.add(axisb.build());
      }
    }
    this.axes = allAxesb.build();

    ImmutableList.Builder<GridCoordinateSystem> coordsysb = ImmutableList.builder();
    for (GridCS.Builder<?> sys : builder.coordsys) {
      coordsysb.add(sys.build(this.axes));
    }
    this.coordsys = coordsysb.build();

    ImmutableList.Builder<Grid> gridsb = ImmutableList.builder();
    for (CdmrGrid.Builder b : builder.grids) {
      gridsb.add(b.build(this.coordsys));
    }
    this.grids = gridsb.build();
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
    private CdmRemoteGrpc.CdmRemoteBlockingStub blockingStub;
    private String path;
    private CdmrGridProto.GridDataset proto;
    private ArrayList<GridAxis.Builder<?>> axes = new ArrayList<>();
    private ArrayList<GridCS.Builder<?>> coordsys = new ArrayList<>();
    private ArrayList<CdmrGrid.Builder> grids = new ArrayList<>();

    private boolean built;

    public Builder setRemoteURI(String remoteURI) {
      this.remoteURI = remoteURI;
      return this;
    }

    public Builder addGridAxis(GridAxis.Builder<?> axis) {
      axes.add(axis);
      return this;
    }

    public Builder addCoordSys(GridCS.Builder<?> sys) {
      coordsys.add(sys);
      return this;
    }

    public Builder addGrid(CdmrGrid.Builder grid) {
      grids.add(grid);
      return this;
    }

    public CdmrGridDataset build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      openChannel();
      return new CdmrGridDataset(this);
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
        this.blockingStub = CdmRemoteGrpc.newBlockingStub(channel);
        readDataset(path, errlog);

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
        throw new RuntimeException("Cant open CdmRemote url " + this.remoteURI, e);
      }
    }

    private void readDataset(String location, Formatter errlog) {
      log.info("CdmrGridDataset request header for " + location);
      CdmrGridProto.GridDatasetRequest request =
          CdmrGridProto.GridDatasetRequest.newBuilder().setLocation(location).build();
      CdmrGridProto.GridDatasetResponse response = blockingStub.getGridDataset(request);
      if (response.hasError()) {
        throw new RuntimeException(response.getError().getMessage());
      } else {
        this.proto = response.getDataset();
        CdmrGridConverter.decodeDataset(this.proto, this, errlog);
      }
    }

  }
}
