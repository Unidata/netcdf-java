/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm.client;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import ucar.gcdm.GcdmConverter;
import ucar.gcdm.GcdmGrpc;
import ucar.gcdm.GcdmNetcdfProto.DataRequest;
import ucar.gcdm.GcdmNetcdfProto.DataResponse;
import ucar.gcdm.GcdmNetcdfProto.Header;
import ucar.gcdm.GcdmNetcdfProto.HeaderRequest;
import ucar.gcdm.GcdmNetcdfProto.HeaderResponse;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.Section;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** A remote CDM NetcdfFile, using gprc protocol to communicate. */
public class GcdmNetcdfFile extends NetcdfFile {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GcdmNetcdfFile.class);
  private static final int MAX_DATA_WAIT_SECONDS = 30;
  private static final int MAX_MESSAGE = 101 * 1000 * 1000; // 101 Mb

  public static final String PROTOCOL = "gcdm";

  @Override
  protected StructureDataIterator getStructureIterator(Structure structure, int bufferSize) throws IOException {
    Array data = readData(structure, structure.getShapeAsSection());
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(data instanceof ArrayStructure);
    ArrayStructure arrayStructure = (ArrayStructure) data;
    return arrayStructure.getStructureDataIterator();
  }

  @Nullable
  protected Array readData(Variable variable, Section sectionWanted) throws IOException {
    String spec = ParsedSectionSpec.makeSectionSpecString(variable, sectionWanted.getRanges());
    if (logger.isDebugEnabled()) {
      long expected = sectionWanted.computeSize() * variable.getElementSize();
      logger.debug("GcdmNetcdfFile data request for spec=({})\n url='{}'\n path='{}' request bytes = {}\n", spec,
          this.remoteURI, this.path, expected);
    }
    final Stopwatch stopwatch = Stopwatch.createStarted();

    List<Array> results = new ArrayList<>();
    long size = 0;
    DataRequest request = DataRequest.newBuilder().setLocation(this.path).setVariableSpec(spec).build();
    try {
      Iterator<DataResponse> responses =
          blockingStub.withDeadlineAfter(MAX_DATA_WAIT_SECONDS, TimeUnit.SECONDS).getNetcdfData(request);
      while (responses.hasNext()) {
        DataResponse response = responses.next();
        if (response.hasError()) {
          throw new IOException(response.getError().getMessage());
        }
        Array result = GcdmConverter.decodeData(response.getData());
        results.add(result);
        size += result.getSize() * variable.getElementSize();
        if (logger.isDebugEnabled()) {
          logger.debug("  readArrayData bytes received = {}", result.getSize() * variable.getElementSize());
        }
      }

    } catch (StatusRuntimeException e) {
      logger.warn("readData failed: ", e);
      throw new IOException(e);

    } catch (Throwable t) {
      logger.debug(" ** failed after {}", stopwatch);
      logger.warn("readData failed: ", t);
      throw new IOException(t);
    }
    logger.debug(" ** received={} took={} rate={} MB/sec", size, stopwatch.stop(),
        ((double) size) / stopwatch.elapsed(TimeUnit.MICROSECONDS));

    if (results.size() == 1) {
      return results.get(0);
    } else {
      return Array.factoryCopy(variable.getDataType(), sectionWanted.getShape(), results);
    }
  }

  @Override
  public String getFileTypeId() {
    return PROTOCOL;
  }

  @Override
  public String getFileTypeDescription() {
    return PROTOCOL;
  }

  @Override
  public synchronized void close() {
    try {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException interruptedException) {
      logger.warn("GcdmNetcdfFile shutdown interrupted");
      // fall through
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final String remoteURI;
  private final String path;
  private final ManagedChannel channel;
  private final GcdmGrpc.GcdmBlockingStub blockingStub;

  private GcdmNetcdfFile(Builder<?> builder) {
    super(builder);
    this.remoteURI = builder.remoteURI;
    this.path = builder.path;
    this.channel = builder.channel;
    this.blockingStub = builder.blockingStub;
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  private Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setRemoteURI(this.remoteURI);
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends NetcdfFile.Builder<T> {
    private String remoteURI;
    private ManagedChannel channel;
    private GcdmGrpc.GcdmBlockingStub blockingStub;
    private String path;
    private boolean built;

    protected abstract T self();

    public T setRemoteURI(String remoteURI) {
      this.remoteURI = remoteURI;
      return self();
    }

    public GcdmNetcdfFile build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      openChannel();
      return new GcdmNetcdfFile(this);
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
      try {
        this.blockingStub = GcdmGrpc.newBlockingStub(channel);
        readHeader(path);

      } catch (Exception e) {
        // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
        // resources the channel should be shut down when it will no longer be used. If it may be used
        // again leave it running.
        try {
          channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
          logger.warn("Shutdown interrupted ", e);
          // fall through
        }
        e.printStackTrace();
        throw new RuntimeException("Cant open Gcdm url " + this.remoteURI, e);
      }
    }

    private void readHeader(String location) {
      logger.info("GcdmNetcdfFile request header for " + location);
      HeaderRequest request = HeaderRequest.newBuilder().setLocation(location).build();
      HeaderResponse response = blockingStub.getNetcdfHeader(request);
      if (response.hasError()) {
        throw new RuntimeException(response.getError().getMessage());
      } else {
        Header header = response.getHeader();
        setId(header.getId());
        setTitle(header.getTitle());
        setLocation(remoteURI);

        this.rootGroup = Group.builder().setName("");
        GcdmConverter.decodeGroup(header.getRoot(), this.rootGroup);
      }
    }
  }
}
