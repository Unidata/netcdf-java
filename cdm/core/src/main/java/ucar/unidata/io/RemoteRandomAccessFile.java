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

    // Only enable cache if its maximum size is at least 2x the buffer size, both of which are configurable
    // at runtime
    int minimumCacheActivationSize = 2 * bufferSize;
    if (maxRemoteCacheSize >= minimumCacheActivationSize) {
      // have each cache block hold a 1 buffer sized chunk
      this.readCacheBlockSize = bufferSize;
      // user set max cache size in bytes
      // guava cache set as number of objects
      // The question here is, how many readCacheBlockSize objects would there be in maxRemoteCacheSize bytes?
      // total max cache size in bytes / size of one cache block, rounded up.
      long numberOfCacheBlocks = (maxRemoteCacheSize / readCacheBlockSize) + 1;
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

  /**
   * Fill byte array with remote data.
   *
   * @param pos position of remote file or object to start reading
   * @param buff put data into this buffer
   * @param offset buffer offset
   * @param len number of bytes to read
   * @return actual number of bytes read
   * @throws IOException error reading remote data
   */
  private int readFromCache(long pos, byte[] buff, int offset, int len) throws IOException {
    // We basically treat the entire remote file or object as a series of non-overlapping blocks of size
    // readCacheBlockSize. Each of these blocks are assigned a number (0 - N) starting from position 0 in the
    // remote file or object, and that number is used as the key of the cache.
    // Here, we compute the first and last cache block that we need to read from based on the desired position in the
    // file and the length of read.
    long firstCacheBlockNumber = pos / readCacheBlockSize;
    long lastCacheBlockNumber = (pos + len) / readCacheBlockSize;
    int totalBytesRead = 0;
    int currentOffsetIntoBuffer = offset;

    // Read cacheBlock containing pos, and fill the buffer from the effective location of pos in the cache block
    // up to the smaller of these three lengths: 1. bytes remaining in the cache block, 2. bytes remaining in the file,
    // or 3. bytes remaining in the destination array.
    totalBytesRead += readCacheBlockPartial(pos, buff, currentOffsetIntoBuffer, true);
    currentOffsetIntoBuffer += totalBytesRead;
    // If we have read everything we have been asked to read, skip the rest of the read logic.
    // Otherwise, keep going.
    if ((totalBytesRead < len) && (firstCacheBlockNumber != lastCacheBlockNumber)) {
      // Now fill the buffer using whole cache blocks, up until the last cache block (as reading from the last cache
      // block might be a partial read).
      // LOOK: might benefit from concurrency? Need to see how often we end up inside this while statement.
      // Initial testing shows this only happens for small readCacheBlockSize (which is equal to the read buffer size)
      // when reading netCDF-4 files, but I think might depend on the specific IOSP in use.
      long currentCacheBlockNumber = firstCacheBlockNumber + 1;
      while (currentCacheBlockNumber < lastCacheBlockNumber) {
        totalBytesRead += readCacheBlockFull(currentCacheBlockNumber, currentOffsetIntoBuffer, buff);
        currentOffsetIntoBuffer += readCacheBlockSize;
        currentCacheBlockNumber += 1;
      }
      logger.debug("Number of full cache block reads: {}", currentCacheBlockNumber - firstCacheBlockNumber - 1);

      // If there are still bytes to read, read last cacheBlock from the start of the cache block to the
      // smaller of these two lengths: 1. bytes remaining in the file, or 2. bytes remaining in the destination array.
      if (totalBytesRead < len) {
        totalBytesRead += readCacheBlockPartial(pos + totalBytesRead, buff, currentOffsetIntoBuffer, false);
      }
    }

    return totalBytesRead;
  }

  private int readCacheBlockPartial(long pos, byte[] buff, int positionInBuffer, boolean fillForward)
      throws IOException {

    long cacheBlockNumber = pos / readCacheBlockSize;

    // read in the cache block
    byte[] src;
    try {
      src = readCache.get(cacheBlockNumber);
    } catch (ExecutionException ee) {
      throw new IOException("Error obtaining data from the remote data read cache.", ee);
    }

    // Careful - we are doing a partial read from the cache block. The pos in the file is some offset into the cache
    // block, so let's start by calculating the pos of the first block
    long posCacheBlockStart = cacheBlockNumber * readCacheBlockSize;

    // Now, there are two possible cases. We either need to read from this position in the cache block to the end of
    // the cache block (i.e. "fill buffer forward from the offset into the cache block"), or read from the beginning
    // of the cache block up to the offset in the cache block (useful if reading last cache block in a series of cache
    // blocks). Basically,the question is where do we start reading in the cache block, and what size read do we use?
    int offsetIntoCacheBlock;
    int sizeToCopy;
    if (fillForward) {
      // .....size to copy (assuming we will want to read until the end of the cache block)
      // .....<-------->
      // X ---|------- X
      // ^____^
      // ..|
      // Offset Into Cache Block
      offsetIntoCacheBlock = Math.toIntExact(pos - posCacheBlockStart);
      sizeToCopy = readCacheBlockSize - offsetIntoCacheBlock;
    } else {
      // size to copy
      // <---->
      // X ---|------- X
      // ^
      // Offset Into Cache Block
      offsetIntoCacheBlock = 0;
      sizeToCopy = Math.toIntExact(pos - posCacheBlockStart);
    }

    // We don't want to read past the end of the file, so let's check sizeToCopy against how much of the file
    // is left to read
    long toEof = length() - pos;
    sizeToCopy = Math.toIntExact(Math.min(sizeToCopy, toEof));

    // Finally, check to make sure we are not reading more than buff will hold
    sizeToCopy = Math.toIntExact(Math.min(sizeToCopy, buff.length - positionInBuffer));

    // Copy byte array fulfilling the request as obtained from the cache into the destination buffer
    logger.debug("Requested {} bytes from the cache block (cache block size upper limit: {} bytes.)", sizeToCopy,
        readCacheBlockSize);
    logger.debug("Actual size of the cache block: {} bytes.", src.length);
    logger.debug("Offset into cache block to begin copy: {} bytes.", offsetIntoCacheBlock);
    logger.debug("Total size of the destination buffer: {} bytes.", buff.length);
    logger.debug("Position in buffer to place the copy from the cache: {} bytes.", positionInBuffer);
    logger.debug("Trying to fit {} bytes from the cache into {} bytes of the destination.",
        src.length - offsetIntoCacheBlock, buff.length - positionInBuffer);
    System.arraycopy(src, offsetIntoCacheBlock, buff, positionInBuffer, sizeToCopy);
    return sizeToCopy;
  }

  private int readCacheBlockFull(long cacheBlockNumber, int positionInBuffer, byte[] buff) throws IOException {
    byte[] src;
    try {
      src = readCache.get(cacheBlockNumber);
    } catch (ExecutionException ee) {
      throw new IOException("Error obtaining data from the remote data read cache.", ee);
    }
    System.arraycopy(src, 0, buff, positionInBuffer, readCacheBlockSize);
    return readCacheBlockSize;
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
    // since the byte buffer is a fixed length, reading past EOF will write additional zeroes to dest
    n = Math.min((int) nbytes, (int) this.length() - (int) offset);
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
