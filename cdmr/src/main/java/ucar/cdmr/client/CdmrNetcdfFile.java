/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.cdmr.client;

import com.google.common.base.Preconditions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import ucar.cdmr.CdmRemoteGrpc;
import ucar.cdmr.CdmRemoteProto.DataRequest;
import ucar.cdmr.CdmRemoteProto.DataResponse;
import ucar.cdmr.CdmRemoteProto.Header;
import ucar.cdmr.CdmRemoteProto.HeaderRequest;
import ucar.cdmr.CdmToProtobuf;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** A remote CDM dataset, using cdmremote protocol to communicate. */
public class CdmrNetcdfFile extends NetcdfFile {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CdmrNetcdfFile.class);

  public static final String PROTOCOL = "cdmr";
  public static final String SCHEME = PROTOCOL + ":";

  private static boolean showRequest = false;

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    showRequest = debugFlag.isSet("CdmRemote/showRequest");
  }

  /**
   * Create the canonical form of the URL.
   * If the urlName starts with "http:", change it to start with "cdmremote:", otherwise
   * leave it alone.
   *
   * @param urlName the url string
   * @return canonical form
   */
  public static String canonicalURL(String urlName) {
    if (urlName.startsWith("http:")) {
      return SCHEME + urlName.substring(5);
    } else if (urlName.startsWith("https:")) {
      return SCHEME + urlName.substring(6);
    }
    return urlName;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Array readSection(String variableSection) throws IOException {
    if (showRequest)
      System.out.printf("CdmrNetcdfFile data request forspec=(%s)%n url='%s'%n path='%s'%n", variableSection,
          this.remoteURI, this.path);

    DataRequest request = DataRequest.newBuilder().setLocation(getLocation()).setVariableSpec(variableSection).build();
    try {
      DataResponse response = blockingStub.getData(request);
      if (response.hasError()) {
        throw new IOException(response.getError().getMessage());
      }
      Section sectionReturned = CdmToProtobuf.decodeSection(response.getSection());
      return CdmToProtobuf.decodeData(response.getData(), sectionReturned.getShape());

    } catch (StatusRuntimeException e) {
      log.warn("readSection requestData failed failed: " + e.getStatus());
      throw new IOException(e);
    }
  }

  @Override
  protected Array readData(Variable v, Section sectionWanted) throws IOException {
    String spec = ParsedSectionSpec.makeSectionSpecString(v, sectionWanted.getRanges());

    if (showRequest)
      System.out.printf("CdmrNetcdfFile data request for variable: '%s' spec=(%s)%n url='%s'%n path='%s'%n",
          v.getFullName(), spec, this.remoteURI, this.path);

    DataRequest request = DataRequest.newBuilder().setLocation(this.path).setVariableSpec(spec).build();
    try {
      DataResponse response = blockingStub.getData(request);
      if (response.hasError()) {
        throw new IOException(response.getError().getMessage());
      }
      Section sectionReturned = CdmToProtobuf.decodeSection(response.getSection());
      Preconditions.checkArgument(sectionReturned.equals(sectionWanted));
      return CdmToProtobuf.decodeData(response.getData(), sectionReturned.getShape());

    } catch (StatusRuntimeException e) {
      log.warn("readSection readData failed failed: " + e.getStatus());
      throw new IOException(e);
    }
  }

  protected StructureDataIterator getStructureIterator(Structure s, int bufferSize) {
    throw new UnsupportedOperationException();
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
      log.warn("CdmrNetcdfFile shutdown interrupted");
      // fall through
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final String remoteURI;
  private final String path;
  private final ManagedChannel channel;
  private final CdmRemoteGrpc.CdmRemoteBlockingStub blockingStub;

  private CdmrNetcdfFile(Builder<?> builder) {
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

  /**
   * Get Builder for this class that allows subclassing.
   *
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
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

    public CdmrNetcdfFile build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      openChannel();
      return new CdmrNetcdfFile(this);
    }

    private void openChannel() {
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
          // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
          // needing certificates.
          .usePlaintext().build();
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
      log.info("CdmrNetcdfFile request header for " + location);
      HeaderRequest request = HeaderRequest.newBuilder().setLocation(location).build();
      Header response = blockingStub.getHeader(request);
      setId(response.getId());
      setTitle(response.getTitle());
      setLocation(SCHEME + response.getLocation());

      this.rootGroup = Group.builder().setName("");
      CdmToProtobuf.decodeGroup(response.getRoot(), this.rootGroup);
    }

  }

}
