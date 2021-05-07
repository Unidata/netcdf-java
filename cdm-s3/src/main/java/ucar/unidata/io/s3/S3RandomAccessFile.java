/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import static com.google.common.base.Preconditions.checkState;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.ReadableRemoteFile;
import ucar.unidata.io.RemoteRandomAccessFile;
import ucar.unidata.io.spi.RandomAccessFileProvider;

/**
 * Manage random access file level access to objects stored on AWS S3 compatible Object Stores.
 *
 * Extensions to {@link ucar.unidata.io.RandomAccessFile} and {@link ucar.unidata.io.RemoteRandomAccessFile} for
 * objects stored on AWS S3 compatible Object Stores.
 *
 * @author James McClain, based on work by John Caron and Donald Denbof
 * @since 5.3.2
 */

public final class S3RandomAccessFile extends RemoteRandomAccessFile implements ReadableRemoteFile, Closeable {

  private static final int s3BufferSize = Integer
      .parseInt(System.getProperty("ucar.unidata.io.s3.bufferSize", String.valueOf(defaultRemoteFileBufferSize)));

  private static final long s3MaxReadCacheSize = Long
      .parseLong(System.getProperty("ucar.unidata.io.s3.maxReadCacheSize", String.valueOf(defaultMaxReadCacheSize)));

  private final CdmS3Uri uri;
  private S3Client client;

  private HeadObjectResponse objectHeadResponse;

  private S3RandomAccessFile(String url) throws IOException {
    this(url, s3BufferSize);
  }

  private S3RandomAccessFile(String url, int bufferSize) throws IOException {

    super(url, bufferSize, s3MaxReadCacheSize);

    try {
      uri = new CdmS3Uri(url);
    } catch (URISyntaxException urie) {
      // If we are given a string that is not a valid CdmS3Uri
      // throw an IOException
      throw new IOException(urie.getCause());
    }

    // must be trying to open an object, so the key of the cdms3uri must be present
    checkState(uri.getKey().isPresent(), "The cdmS3Uri must reference an object - object key is missing.");

    // create client that will make S3 API requests
    client = CdmS3Client.acquire(uri);

    // request HEAD for the object
    HeadObjectRequest headdObjectRequest =
        HeadObjectRequest.builder().bucket(uri.getBucket()).key(uri.getKey().get()).build();

    objectHeadResponse = client.headObject(headdObjectRequest);
  }

  public void closeRemote() {
    // client managed by CdmS3Client cache
    client = null;
    objectHeadResponse = null;
  }

  /**
   * Read directly from the remote service All reading goes through here or readToByteChannel;
   *
   * 1. https://docs.aws.amazon.com/AmazonS3/latest/dev/RetrievingObjectUsingJava.html
   *
   * @param pos start here in the file
   * @param buff put data into this buffer
   * @param offset buffer offset
   * @param len this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  @Override
  public int readRemote(long pos, byte[] buff, int offset, int len) throws IOException {

    String range = String.format("bytes=%d-%d", pos, pos + len);
    GetObjectRequest rangeObjectRequest =
        GetObjectRequest.builder().bucket(uri.getBucket()).key(uri.getKey().get()).range(range).build();

    ResponseBytes<GetObjectResponse> objectPortion = client.getObjectAsBytes(rangeObjectRequest);

    int bytes;
    int totalBytes = 0;
    // read response into buff
    try (InputStream objectData = objectPortion.asInputStream()) {
      bytes = objectData.read(buff, offset + totalBytes, len - totalBytes);
      while ((bytes > 0) && ((len - totalBytes) > 0)) {
        totalBytes += bytes;
        bytes = objectData.read(buff, offset + totalBytes, len - totalBytes);
      }
    }
    return totalBytes;
  }

  static int getDefaultRemoteFileTimeout() {
    return defaultRemoteFileTimeout;
  }

  @Override
  public long length() {
    return objectHeadResponse.contentLength();
  }

  @Override
  public long getLastModified() {
    return objectHeadResponse.lastModified().toEpochMilli();
  }

  @Override
  public String getLocation() {
    return uri.toString();
  }

  public String getObjectName() {
    return uri.getKey().get();
  }

  /**
   * Hook into service provider interface to RandomAccessFileProvider. Register in
   * META-INF.services.ucar.unidata.io.spi.RandomAccessFileProvider
   */
  public static class Provider implements RandomAccessFileProvider {

    @Override
    public boolean isOwnerOf(String location) {
      return location.startsWith("cdms3:") || location.startsWith("s3:");
    }

    /**
     * Open a location that this Provider is the owner of.
     */
    @Override
    public RandomAccessFile open(String location) throws IOException {
      return new S3RandomAccessFile(location);
    }

    @Override
    public RandomAccessFile open(String location, int bufferSize) throws IOException {
      return new S3RandomAccessFile(location, bufferSize);
    }
  }
}
