/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An abstract superclass for remote RandomAccessFile */
// not immutable because RandomAccessFile is not immutable.
public abstract class RemoteRandomAccessFile extends ucar.unidata.io.RandomAccessFile implements ReadableRemoteFile {
  private static final Logger logger = LoggerFactory.getLogger(RemoteRandomAccessFile.class);

  // 10 MiB default maximum loading cache size
  protected static final long defaultMaxReadCacheSize = 10485760;
  // 256 KiB default remote file buffer size
  protected static final int defaultRemoteFileBufferSize = 262144;
  // default connection timeout in milliseconds (10 seconds)
  protected static final int defaultRemoteFileTimeout = 10 * 1000;
  // default cache time to live in milliseconds
  private static final long defaultReadCacheTimeToLive = 30 * 1000;

  protected final String url;
  private final boolean readCacheEnabled;
  private final int readCacheBlockSize;
  private final LoadingCache<Long, byte[]> readCache;

  protected RemoteRandomAccessFile(String url, int bufferSize, long maxRemoteCacheSize) {
    super(bufferSize);

    this.url = url;
    file = null;
    location = url;

    // Only enable cache if given size is at least twice the buffer size
    if (maxRemoteCacheSize >= 2 * bufferSize) {
      this.readCacheBlockSize = 2 * bufferSize;
      // user set max cache size in bytes
      // guava cache set as number of objects
      // The question here is, how many readCacheBlockSize objects would there be in maxRemoteCacheSize bytes?
      // total max cache size in bytes / size of one cache block, rounded up.
      long numberOfCacheBlocks = (long) Math.ceil(maxRemoteCacheSize / readCacheBlockSize);
      this.readCache = initCache(numberOfCacheBlocks, Duration.ofMillis(defaultReadCacheTimeToLive));
      readCacheEnabled = true;
    } else {
      this.readCacheBlockSize = -1;
      readCacheEnabled = false;
      readCache = null;
    }
  }

  private LoadingCache<Long, byte[]> initCache(long maximumNumberOfCacheBlocks, java.time.Duration timeToLive) {
    CacheBuilder<Object, Object> cb =
        CacheBuilder.newBuilder().maximumSize(maximumNumberOfCacheBlocks).expireAfterWrite(timeToLive);
    if (debugAccess) {
      cb.recordStats();
    }
    return cb.build(new CacheLoader<Long, byte[]>() {
      public byte[] load(@Nonnull Long key) throws IOException {
        return readRemoteCacheSizedChunk(key);
      }
    });
  }

  /**
   *
   * Read data into the buffer, and return number of bytes read.
   *
   * If the read cache is enabled, the cache will be checked for data first.
   * If not in the cache, data will be read directly from the remote service and placed in the cache.
   * All reading goes through here or readToByteChannel;
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
    return readCacheEnabled ? readFromCache(pos, buff, offset, len) : readRemote(pos, buff, offset, len);
  }

  private int readFromCache(long pos, byte[] buff, int offset, int len) throws IOException {
    long startCacheBlock = pos / readCacheBlockSize;
    long endCacheBlock = (pos + len - 1) / readCacheBlockSize;

    if (pos >= length()) {
      // Do not read past end of the file
      return 0;
    } else if (endCacheBlock - startCacheBlock > 1) {
      // If the request touches more than two cache blocks, punt
      // This should never happen because cache block size is
      // twice the buffer size
      return readFromCache(pos, buff, offset, len);
    } else if (endCacheBlock - startCacheBlock == 1) {
      // If the request touches two cache blocks, split it
      int length1 = (int) ((endCacheBlock * readCacheBlockSize) - pos);
      int length2 = (int) ((pos + len) - (endCacheBlock * readCacheBlockSize));
      return readFromCache(pos, buff, offset, length1) + readFromCache(pos + length1, buff, offset + length1, length2);
    }

    // If we make it here, we are servicing a request that touches
    // only one cache block.
    byte[] src;
    Long key = startCacheBlock;
    try {
      src = readCache.get(key);
    } catch (ExecutionException ee) {
      throw new IOException(ee.getCause());
    }

    int srcPos = (int) (pos - (key * readCacheBlockSize));
    int toEOB = src.length - srcPos;
    int length = Math.min(toEOB, len);
    // copy byte array fulfilling the request as obtained from the cache or a fresh read
    // of the remote data into the destination buffer
    System.arraycopy(src, srcPos, buff, offset, length);

    return len;
  }

  /**
   * read a readCacheBlockSize chunk of the remote file
   */
  private byte[] readRemoteCacheSizedChunk(Long cacheBlockNumber) throws IOException {
    long position = cacheBlockNumber * readCacheBlockSize;
    long toEOF = length() - position;
    // if size to EOF less than readCacheBlockSize, just read to EOF
    long bytesToRead = toEOF < readCacheBlockSize ? toEOF : readCacheBlockSize;
    int bytes = Math.toIntExact(bytesToRead);
    byte[] buffer = new byte[bytes];

    readRemote(position, buffer, 0, bytes);
    return buffer;
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
  public void close() throws IOException {
    closeRemote();
    super.close();
    // clean out the cache when closing the Remote Random Access File
    if (readCache != null) {
      readCache.invalidateAll();
      if (debugAccess) {
        logger.info(readCache.stats().toString());
      }
    }
  }
}
