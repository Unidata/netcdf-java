/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;
import ucar.nc2.filter.Filter;
import ucar.nc2.filter.Filters;
import ucar.nc2.filter.UnknownFilterException;
import ucar.nc2.iosp.LayoutBB;
import ucar.nc2.iosp.LayoutBBTiled;
import ucar.nc2.iosp.hdf5.DataBTree;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;

/**
 * Iterator to read/write subsets of an array.
 * This calculates byte offsets for HD5 chunked datasets.
 * Assumes that the data is stored in chunks, indexed by a Btree.
 * Used for filtered data
 * Had to split from old H5tiledLayoutBB because need to use H5headerNew.Vinfo.
 * 
 * @author caron
 */
public class H5tiledLayoutBB implements LayoutBB {

  static final int DEFAULTZIPBUFFERSIZE = 512;
  // System property name for -D flag
  static final String INFLATEBUFFERSIZE = "unidata.h5iosp.inflate.buffersize";

  public static boolean debugFilter;

  private LayoutBBTiled delegate;

  private RandomAccessFile raf;
  private Filter[] filters;
  private ByteOrder byteOrder;

  private Section want;
  private int[] chunkSize; // from the StorageLayout message (exclude the elemSize)
  private int elemSize; // last dimension of the StorageLayout message
  private int nChunkDims;

  private boolean debug;

  private int inflatebuffersize = DEFAULTZIPBUFFERSIZE;

  /**
   * Constructor.
   * This is for HDF5 chunked data storage. The data is read by chunk, for efficiency.
   *
   * @param v2 Variable to index over; assumes that vinfo is the data object
   * @param wantSection the wanted section of data, contains a List of Range objects. must be complete
   * @param raf the RandomAccessFile
   * @param filterProps set of filter properties from which filter object will be created
   * @throws InvalidRangeException if section invalid for this variable
   * @throws IOException on io error
   */
  public H5tiledLayoutBB(Variable v2, Section wantSection, RandomAccessFile raf, H5objects.Filter[] filterProps,
      ByteOrder byteOrder) throws InvalidRangeException, IOException {
    wantSection = Section.fill(wantSection, v2.getShape());

    H5headerNew.Vinfo vinfo = (H5headerNew.Vinfo) v2.getSPobject();
    assert vinfo.isChunked;
    assert vinfo.btree != null;

    this.raf = raf;
    this.filters = new Filter[filterProps.length];
    for (int i = 0; i < filterProps.length; i++) {
      // add var info to filter props
      Map<String, Object> props = filterProps[i].getProperties();
      props.put(Filters.Keys.ELEM_SIZE, v2.getElementSize());
      // try to get filter by name or id, throw if not recognized filter
      try {
        filters[i] = Filters.getFilter(props);
      } catch (UnknownFilterException ex) {
        throw new IOException(ex);
      }
    }
    this.byteOrder = byteOrder;

    // we have to translate the want section into the same rank as the storageSize, in order to be able to call
    // Section.intersect(). It appears that storageSize (actually msl.chunkSize) may have an extra dimension, relative
    // to the Variable.
    DataType dtype = v2.getDataType();
    if ((dtype == DataType.CHAR) && (wantSection.getRank() < vinfo.storageSize.length)) {
      this.want = Section.builder().appendRanges(wantSection.getRanges()).appendRange(1).build();
    } else {
      this.want = wantSection;
    }

    // one less chunk dimension, except in the case of char
    nChunkDims = (dtype == DataType.CHAR) ? vinfo.storageSize.length : vinfo.storageSize.length - 1;
    this.chunkSize = new int[nChunkDims];
    System.arraycopy(vinfo.storageSize, 0, chunkSize, 0, nChunkDims);
    this.elemSize = vinfo.storageSize[vinfo.storageSize.length - 1]; // last one is always the elements size

    // create the data chunk iterator
    DataBTree.DataChunkIterator iter = vinfo.btree.getDataChunkIteratorFilter(this.want);
    DataChunkIterator dcIter = new DataChunkIterator(iter);
    delegate = new LayoutBBTiled(dcIter, chunkSize, elemSize, this.want);

    if (System.getProperty(INFLATEBUFFERSIZE) != null) {
      try {
        int size = Integer.parseInt(System.getProperty(INFLATEBUFFERSIZE));
        if (size <= 0)
          H5iospNew.log.warn(String.format("-D%s must be > 0", INFLATEBUFFERSIZE));
        else
          this.inflatebuffersize = size;
      } catch (NumberFormatException nfe) {
        H5iospNew.log.warn(String.format("-D%s is not an integer", INFLATEBUFFERSIZE));
      }
    }
    if (debugFilter)
      System.out.printf("inflate buffer size -D%s = %d%n", INFLATEBUFFERSIZE, this.inflatebuffersize);

    if (debug)
      System.out.println(" H5tiledLayout: " + this);
  }

  public long getTotalNelems() {
    return delegate.getTotalNelems();
  }

  public int getElemSize() {
    return delegate.getElemSize();
  }

  public boolean hasNext() {
    return delegate.hasNext();
  }

  public Chunk next() {
    return delegate.next();
  }

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("want=").append(want).append("; ");
    sbuff.append("chunkSize=[");
    for (int i = 0; i < chunkSize.length; i++) {
      if (i > 0)
        sbuff.append(",");
      sbuff.append(chunkSize[i]);
    }
    sbuff.append("] totalNelems=").append(getTotalNelems());
    sbuff.append(" elemSize=").append(elemSize);
    return sbuff.toString();
  }

  private class DataChunkIterator implements LayoutBBTiled.DataChunkIterator {
    DataBTree.DataChunkIterator delegate;

    DataChunkIterator(DataBTree.DataChunkIterator delegate) {
      this.delegate = delegate;
    }

    public boolean hasNext() {
      return delegate.hasNext();
    }

    public LayoutBBTiled.DataChunk next() throws IOException {
      return new DataChunk(delegate.next());
    }
  }

  private class DataChunk implements LayoutBBTiled.DataChunk {
    // Copied from ArrayList.
    private static final int MAX_ARRAY_LEN = Integer.MAX_VALUE - 8;

    DataBTree.DataChunk delegate;

    DataChunk(DataBTree.DataChunk delegate) {
      this.delegate = delegate;

      // Check that the chunk length (delegate.size) isn't greater than the maximum array length that we can
      // allocate (MAX_ARRAY_LEN). This condition manifests in two ways.
      // 1) According to the HDF docs (https://www.hdfgroup.org/HDF5/doc/Advanced/Chunking/, "Chunk Maximum Limits"),
      // max chunk length is 4GB (i.e. representable in an unsigned int). Java, however, only has signed ints.
      // So, if we try to store a large unsigned int in a singed int, it'll overflow, and the signed int will come
      // out negative. We're trusting here that the chunk size read from the HDF file is never negative.
      // 2) In most JVM implementations MAX_ARRAY_LEN is actually less than Integer.MAX_VALUE (see note in ArrayList).
      // So, we could have: "MAX_ARRAY_LEN < chunkSize <= Integer.MAX_VALUE".
      if (delegate.size < 0 || delegate.size > MAX_ARRAY_LEN) {
        // We want to report the size of the chunk, but we may be in an arithmetic overflow situation. So to get the
        // correct value, we're going to reinterpet the integer's bytes as long bytes.
        byte[] intBytes = Ints.toByteArray(delegate.size);
        byte[] longBytes = new byte[8];
        System.arraycopy(intBytes, 0, longBytes, 4, 4); // Copy int bytes to the lowest 4 positions.
        long chunkSize = Longs.fromByteArray(longBytes); // Method requires an array of length 8.

        throw new IllegalArgumentException(String.format("Filtered data chunk is %s bytes and we must load it all "
            + "into memory. However the maximum length of a byte array in Java is %s.", chunkSize, MAX_ARRAY_LEN));
      }
    }

    public int[] getOffset() {
      int[] offset = delegate.offset;
      if (offset.length > nChunkDims) { // may have to eliminate last offset
        offset = new int[nChunkDims];
        System.arraycopy(delegate.offset, 0, offset, 0, nChunkDims);
      }
      return offset;
    }

    public ByteBuffer getByteBuffer() throws IOException {
      try {
        // read the data
        byte[] data = new byte[delegate.size];
        raf.seek(delegate.filePos);
        raf.readFully(data);

        // apply filters backwards
        for (int i = filters.length - 1; i >= 0; i--) {
          Filter f = filters[i];
          if (isBitSet(delegate.filterMask, i)) {
            if (debug) {
              System.out.println("skip for chunk " + delegate);
            }
            continue;
          }
          data = f.decode(data);
        }

        ByteBuffer result = ByteBuffer.wrap(data);
        result.order(byteOrder);
        return result;
      } catch (OutOfMemoryError e) {
        Error oom = new OutOfMemoryError("Ran out of memory trying to read HDF5 filtered chunk. Either increase the "
            + "JVM's heap size (use the -Xmx switch) or reduce the size of the dataset's chunks (use nccopy -c).");
        oom.initCause(e); // OutOfMemoryError lacks a constructor with a cause parameter.
        throw oom;
      }
    }

    boolean isBitSet(int val, int bitno) {
      return ((val >>> bitno) & 1) != 0;
    }
  }
}
