package ucar.nc2.iosp.zarr;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.iosp.LayoutBB;
import ucar.nc2.iosp.LayoutBBTiled;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;

public class ZarrLayoutBB implements LayoutBB {

  private LayoutBBTiled delegate;

  private RandomAccessFile raf;
  private ByteOrder byteOrder;
  private final long varOffset;

  // TODO filters and inflateBufferSize

  private final Section want;

  private int[] chunkSize; // number of elements per chunks
  private int elemSize;
  private int nChunks[]; // number of chunks per dimension
  private int nBytes; // number of bytes per chunk
  private int totalNChunks; // total number of chunks
  private int totalChunkSize; // total number of elements per chunk

  // bytes representing compressing
  private static final int ZARR_COMPRESSOR_OFFSET = 16;
  private int data_bytes_offset; // 0 or 16 depending on whether a compressor is found

  public ZarrLayoutBB(Variable v2, Section wantSection, RandomAccessFile raf) throws InvalidRangeException {
    this.raf = raf;

    ZarrHeader.VInfo vinfo = (ZarrHeader.VInfo) v2.getSPobject();

    this.byteOrder = vinfo.getByteOrder();
    this.varOffset = vinfo.getOffset();
    this.data_bytes_offset = vinfo.getCompressor() == null ? 0 : ZARR_COMPRESSOR_OFFSET;

    // get chunk info
    this.chunkSize = vinfo.getChunks();
    int ndims = this.chunkSize.length;

    // transpose Section and chunk if F order
    if (vinfo.getOrder() == ZArray.Order.F) {
      List<Range> ranges = wantSection.getRanges();
      List<Range> transpose = new ArrayList<>();
      int[] temp = new int[ndims];
      for (int i = 0; i < ndims; i++) {
        transpose.add(ranges.get(ndims-i-1));
        temp[i] = this.chunkSize[ndims-i-1];
      }
      this.want = new Section(transpose);
      this.chunkSize = temp;
    } else {
      this.want = wantSection;
    }

    this.nChunks = new int[ndims];
    this.totalNChunks = 1;
    this.totalChunkSize = 1;
    for (int i = 0; i < ndims; i++) {
      Dimension dim = v2.getDimension(i);
      this.nChunks[i] = (int) Math.ceil(dim.getLength() / this.chunkSize[i]);
      this.totalNChunks *= nChunks[i];
      this.totalChunkSize *= chunkSize[i];
    }

    this.elemSize = v2.getDataType().getSize();
    this.nBytes = totalChunkSize * elemSize;

    // create delegate and chunk iterator
    ZarrLayoutBB.DataChunkIterator iter = new ZarrLayoutBB.DataChunkIterator();
    delegate = new LayoutBBTiled(iter, chunkSize, elemSize, this.want);
  }

  @Override
  public long getTotalNelems() {
    return delegate.getTotalNelems();
  }

  @Override
  public int getElemSize() {
    return delegate.getElemSize();
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public LayoutBB.Chunk next() {
    return delegate.next();
  }

  private class DataChunkIterator implements LayoutBBTiled.DataChunkIterator {

    private int[] currChunk; // current chunk in array coords
    private int chunkNum; // current chunk as flat index

    DataChunkIterator() {
      this.currChunk = new int[chunkSize.length];
      this.chunkNum = 0;
    }

    public boolean hasNext() {
      return this.chunkNum < totalNChunks;
    }

    public LayoutBBTiled.DataChunk next() {
      // TODO: handle uninitialized chunks
      DataChunk chunk = new ZarrLayoutBB.DataChunk(this.currChunk, totalChunkSize, this.chunkNum);
      incrementChunk();
      return chunk;
    }

    private void incrementChunk() {
      this.chunkNum++;
      int i = this.currChunk.length - 1;
      while (this.currChunk[i] + 1 >= nChunks[i] && i > 0) {
        this.currChunk[i] = 0;
        i--;
      }
      this.currChunk[i]++;
    }
  }

  private class DataChunk implements LayoutBBTiled.DataChunk {

    private int[] offset; // start indices of chunk in elements
    private long rafOffset; // start position of chunk in bytes

    DataChunk(int[] index, int totalChunkSize, int chunkNum) {
      this.offset = new int[index.length];
      this.rafOffset = varOffset + (chunkNum * (nBytes + data_bytes_offset)) + data_bytes_offset;
      for (int i = 0; i < index.length; i++) {
        this.offset[i] = index[i] * chunkSize[i];
      }
    }

    public int[] getOffset() {
      return this.offset;
    }

    public ByteBuffer getByteBuffer() throws IOException {
      try {
        // read the data
        byte[] data = new byte[nBytes];
        raf.seek(this.rafOffset);
        raf.readFully(data);

        // TODO: apply filters in reverse order

        ByteBuffer result = ByteBuffer.wrap(data);
        result.order(byteOrder);
        return result;
      } catch (OutOfMemoryError e) {
        Error oom = new OutOfMemoryError("Ran out of memory trying to read zarr filtered chunk. Either increase the "
            + "JVM's heap size (use the -Xmx switch) or reduce the size of the dataset's chunks.");
        oom.initCause(e); // OutOfMemoryError lacks a constructor with a cause parameter.
        throw oom;
      }
    }
  }

}
