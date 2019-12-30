/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.time.Duration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient.Builder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * @author James McClain, based on work by John Caron and Donald Denbof
 */

public class S3RandomAccessFile extends ucar.unidata.io.RandomAccessFile {

  // default is 512 Kilobytes
  static private final int defaultS3BufferSize =
      Integer.parseInt(System.getProperty("ucar.unidata.io.s3.defaultS3BufferSize", "524288"));
  // default is 32 Megabytes
  static private final int defaultMaxCacheSize =
      Integer.parseInt(System.getProperty("ucar.unidata.io.s3.defaultMaxCacheSize", "33554432"));
  private int cacheBlockSize;
  private int maxCacheBlocks;

  private AmazonS3URI uri;
  private S3Client client;
  private String bucket;
  private String key;
  private HeadObjectResponse objectHeadResponse;

  private java.util.Map<Long, byte[]> cache = new java.util.HashMap<>();
  private java.util.LinkedList<Long> index = new java.util.LinkedList<>();

  /**
   * The maximum number of connections allowed in the S3 http connection pool. Each built S3 HTTP client has it's own
   * private
   * connection pool.
   */
  private static int maxConnections =
      Integer.parseInt(System.getProperty("ucar.unidata.io.s3.httpMaxConnections", "128"));
  /**
   * The amount of time to wait when initially establishing a connection before giving up and timing out. A duration of
   * 0 means infinity, and is not recommended.
   */
  private static int connectionTimeout =
      Integer.parseInt(System.getProperty("ucar.unidata.io.s3.connectionTimeout", "100000"));
  /**
   * The amount of time to wait for data to be transferred over an established, open connection before the connection is
   * timed out. A duration of 0 means infinity, and is not recommended.
   */
  private static int socketTimeout = Integer.parseInt(System.getProperty("ucar.unidata.io.s3.socketTimeout", "100000"));

  public S3RandomAccessFile(String url) throws IOException {
    this(url, defaultS3BufferSize, defaultMaxCacheSize);
  }

  public S3RandomAccessFile(String url, int bufferSize, int maxCacheSize) throws IOException {
    super(bufferSize);
    file = null;
    location = url;

    // Only enable cache if given size is at least twice the buffer size
    if (maxCacheSize >= 2 * bufferSize) {
      this.cacheBlockSize = 2 * bufferSize;
      this.maxCacheBlocks = maxCacheSize / this.cacheBlockSize;
    } else {
      this.cacheBlockSize = this.maxCacheBlocks = -1;
    }

    uri = new AmazonS3URI(url);
    bucket = uri.getBucket();
    key = uri.getKey();

    Builder httpConfig = ApacheHttpClient.builder().maxConnections(maxConnections)
        .connectionTimeout(Duration.ofMillis(connectionTimeout)).socketTimeout(Duration.ofMillis(socketTimeout));
    S3ClientBuilder s3ClientBuilder = S3Client.builder().httpClientBuilder(httpConfig);

    // use the Default Credential Provider Chain:
    // https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html
    // but, as a last resort, try the AnonymousCredentialsProvider.
    AwsCredentialsProviderChain defaultThenAnonymous = AwsCredentialsProviderChain.builder()
        .credentialsProviders(DefaultCredentialsProvider.create(), AnonymousCredentialsProvider.create()).build();

    // Uses the default Default Region Provider Chain:
    // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html
    client = s3ClientBuilder.credentialsProvider(defaultThenAnonymous).build();
    // request HEAD for the object
    objectHeadResponse = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
  }

  public void close() throws IOException {
    // close the client
    if (client != null) {
      client.close();
    }

    uri = null;
    client = null;
    bucket = null;
    key = null;
    objectHeadResponse = null;
  }

  /**
   * After execution of this function, the given block is guaranteed to be in the cache.
   */
  private void ensure(Long key) throws IOException {
    if (!cache.containsKey(key)) {
      long position = key.longValue() * cacheBlockSize;
      int toEOF = (int) (length() - position);
      // if size to EOF less than cacheBlockSize, just read to EOF
      int bytes = toEOF < cacheBlockSize ? toEOF : cacheBlockSize;

      byte[] buffer = new byte[bytes];

      read__(position, buffer, 0, bytes);
      cache.put(key, buffer);
      index.add(key);
      assert (cache.size() == index.size());
      while (cache.size() > maxCacheBlocks) {
        cache.remove(index.pop());
      }

      return;
    }
  }

  /**
   * Read directly from S3 [1], without going through the buffer. All reading goes through here or readToByteChannel;
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
  protected int read_(long pos, byte[] buff, int offset, int len) throws IOException {
    if (!(cacheBlockSize > 0) || !(maxCacheBlocks > 0)) {
      return read__(pos, buff, offset, len);
    }

    long start = pos / cacheBlockSize;
    long end = (pos + len - 1) / cacheBlockSize;

    if (pos >= length()) { // Do not read past end of the file
      return 0;
    } else if (end - start > 1) { // If the request touches more than two cache blocks, punt (should never happen)
      return read__(pos, buff, offset, len);
    } else if (end - start == 1) { // If the request touches two cache blocks, split it
      int length1 = (int) ((end * cacheBlockSize) - pos);
      int length2 = (int) ((pos + len) - (end * cacheBlockSize));
      return read_(pos, buff, offset, length1) + read_(pos + length1, buff, offset + length1, length2);
    }

    // Service a request that touches only one cache block
    Long key = new Long(start);
    ensure(key);

    byte[] src = (byte[]) cache.get(key);
    int srcPos = (int) (pos - (key.longValue() * cacheBlockSize));
    int toEOB = src.length - srcPos;
    int length = toEOB < len ? toEOB : len;
    System.arraycopy(src, srcPos, buff, offset, length);

    return len;
  }

  private int read__(long pos, byte[] buff, int offset, int len) throws IOException {

    String range = String.format("bytes=%d-%d", pos, pos + len);
    GetObjectRequest rangeObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key).range(range).build();

    ResponseBytes<GetObjectResponse> objectPortion = client.getObjectAsBytes(rangeObjectRequest);

    int bytes = 0;
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

  @Override
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    int n = (int) nbytes;
    byte[] buff = new byte[n];
    int done = read_(offset, buff, 0, n);
    dest.write(ByteBuffer.wrap(buff));
    return done;
  }

  @Override
  public long length() throws IOException {
    return objectHeadResponse.contentLength();
  }

  @Override
  public long getLastModified() {
    return objectHeadResponse.lastModified().toEpochMilli();
  }
}
