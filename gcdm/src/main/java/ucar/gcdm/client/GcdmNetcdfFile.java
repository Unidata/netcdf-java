/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
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
import ucar.array.Arrays;
import ucar.array.StructureDataArray;
import ucar.cdmr.CdmRemoteGrpc;
import ucar.cdmr.CdmrNetcdfProto.DataRequest;
import ucar.cdmr.CdmrNetcdfProto.DataResponse;
import ucar.cdmr.CdmrNetcdfProto.Header;
import ucar.cdmr.CdmrNetcdfProto.HeaderRequest;
import ucar.cdmr.CdmrNetcdfProto.HeaderResponse;
import ucar.cdmr.CdmrConverter;
import ucar.ma2.Section;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedArraySectionSpec;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** A remote CDM NetcdfFile, using gprc protocol to communicate. */
public class GcdmNetcdfFile extends NetcdfFile {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GcdmNetcdfFile.class);
  private static final int MAX_DATA_WAIT_SECONDS = 30;
  private static final int MAX_MESSAGE = 51 * 1000 * 1000; // 51 Mb
  private static boolean showRequest = true;

  public static final String PROTOCOL = "cdmr";
  public static final String SCHEME = PROTOCOL + ":";

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    showRequest = debugFlag.isSet("CdmRemote/showRequest");
  }

  @Override
  protected ucar.ma2.Array readData(Variable v, Section sectionWanted) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected StructureDataIterator getStructureIterator(Structure s, int bufferSize) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<ucar.array.StructureData> getStructureDataArrayIterator(Sequence s, int bufferSize)
      throws IOException {
    ucar.array.Array<?> data = readArrayData(s, s.getSection());
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(data instanceof StructureDataArray);
    StructureDataArray sdata = (StructureDataArray) data;
    return sdata.iterator();
  }

  @Nullable
  protected ucar.array.Array<?> readArrayData(Variable v, ucar.array.Section sectionWanted) throws IOException {
    String spec = ParsedArraySectionSpec.makeSectionSpecString(v, sectionWanted);
    if (showRequest)
      System.out.printf("GcdmNetcdfFile data request forspec=(%s)%n url='%s'%n path='%s'%n", spec, this.remoteURI,
          this.path);
    final Stopwatch stopwatch = Stopwatch.createStarted();

    List<ucar.array.Array<?>> results = new ArrayList<>();
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
        // Section sectionReturned = CdmrConverter.decodeSection(response.getSection());
        ucar.array.Array<?> result = CdmrConverter.decodeData(response.getData());
        results.add(result);
        size += result.length();
      }

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
      return Arrays.factoryCopy(v.getArrayType(), sectionWanted.getShape(), (List) results); // TODO generics
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
      log.warn("GcdmNetcdfFile shutdown interrupted");
      // fall through
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final String remoteURI;
  private final String path;
  private final ManagedChannel channel;
  private final CdmRemoteGrpc.CdmRemoteBlockingStub blockingStub;

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
    private CdmRemoteGrpc.CdmRemoteBlockingStub blockingStub;
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
        this.blockingStub = CdmRemoteGrpc.newBlockingStub(channel);
        readHeader(path);

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
        throw new RuntimeException("Cant open CdmRemote url " + this.remoteURI, e);
      }
    }

    private void readHeader(String location) {
      log.info("GcdmNetcdfFile request header for " + location);
      HeaderRequest request = HeaderRequest.newBuilder().setLocation(location).build();
      HeaderResponse response = blockingStub.getNetcdfHeader(request);
      if (response.hasError()) {
        throw new RuntimeException(response.getError().getMessage());
      } else {
        Header header = response.getHeader();
        setId(header.getId());
        setTitle(header.getTitle());
        setLocation(SCHEME + header.getLocation());

        this.rootGroup = Group.builder().setName("");
        CdmrConverter.decodeGroup(header.getRoot(), this.rootGroup);
      }
    }

  }

}
