package ucar.nc2.iosp.zarr;

import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.filter.Filter;
import ucar.nc2.iosp.LayoutBB;
import ucar.nc2.iosp.LayoutBBTiled;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A tiled layout for Zarr formats that accommodates uncompressing and filtering data before returning
 */
public class ZarrLayoutBB implements LayoutBB {

  private LayoutBBTiled delegate;

  private RandomAccessFile raf;
  private ByteOrder byteOrder;
  private final long varOffset; // start of variable data in raf
  private final Section want;

  private int[] chunkSize; // number of elements per chunks
  private int elemSize; // size of elements in bytes
  private int nChunks[]; // number of chunks per dimension
  private int totalNChunks; // total number of chunks
  private boolean F_order = false; // F order storage?
  private Map<Integer, Long> initializedChunks; // set of chunks that exist as files and their compressed size
  private Filter compressor;
  private List<Filter> filters;

  public ZarrLayoutBB(Variable v2, Section wantSection, RandomAccessFile raf) {
    // var data info
    this.raf = raf;
    ZarrHeader.VInfo vinfo = (ZarrHeader.VInfo) v2.getSPobject();
    this.byteOrder = vinfo.getByteOrder();
    this.varOffset = vinfo.getOffset();
    this.compressor = vinfo.getCompressor();
    this.filters = vinfo.getFilters();


    // fill in chunk info
    this.chunkSize = vinfo.getChunks();
    int ndims = this.chunkSize.length;
    this.initializedChunks = vinfo.getInitializedChunks();
    this.nChunks = new int[ndims];
    this.totalNChunks = 1;
    for (int i = 0; i < ndims; i++) {
      Dimension dim = v2.getDimension(i);
      // round up nchunks if not evenly divisible by chunk size
      this.nChunks[i] = (int) Math.ceil(dim.getLength() / this.chunkSize[i]);
      this.totalNChunks *= nChunks[i];
    }

    // transpose wantsSection and chunk shape if F order
    if (vinfo.getOrder() == ZArray.Order.F) {
      this.F_order = true;
      List<Range> ranges = wantSection.getRanges();
      List<Range> transpose = new ArrayList<>();
      int[] temp = new int[ndims];
      for (int i = 0; i < ndims; i++) {
        transpose.add(ranges.get(ndims - i - 1));
        temp[i] = this.chunkSize[ndims - i - 1];
      }
      this.want = new Section(transpose);
      this.chunkSize = temp;
    } else {
      this.want = wantSection;
    }

    this.elemSize = v2.getDataType().getSize();

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

    private int[] currChunk; // current chunk in subscript coords
    private int chunkNum; // current chunk as flat index
    private long currOffset; // byte position of current chunk


    DataChunkIterator() {
      this.currChunk = new int[chunkSize.length];
      this.chunkNum = 0;
      this.currOffset = varOffset; // start at start of variable data
    }

    public boolean hasNext() {
      return this.chunkNum < totalNChunks;
    }

    public LayoutBBTiled.DataChunk next() {
      DataChunk chunk = new ZarrLayoutBB.DataChunk(this.currChunk, this.chunkNum, this.currOffset);
      incrementChunk();
      return chunk;
    }

    private void incrementChunk() {
      // increment index from inner dimension outward
      int i = this.currChunk.length - 1;
      while (this.currChunk[i] + 1 >= nChunks[i] && i > 0) {
        this.currChunk[i] = 0;
        i--;
      }
      this.currChunk[i]++;
      this.currOffset += initializedChunks.getOrDefault(this.chunkNum, (long)0);
      this.chunkNum = ZarrUtils.subscriptsToIndex(this.currChunk, nChunks);
    }
  }

  private class DataChunk implements LayoutBBTiled.DataChunk {

    private int[] offset; // start indices of chunk in elements
    private long rafOffset; // start position of chunk in bytes
    private int chunkNum;

    DataChunk(int[] index, int chunkNum, long rafOffset) {
      this.rafOffset = rafOffset;
      this.offset = new int[index.length];
      for (int i = 0; i < index.length; i++) {
        int j = F_order ? index.length - i - 1 : i;
        this.offset[i] = index[j] * chunkSize[i];
      }
      this.chunkNum = chunkNum;
    }

    public int[] getOffset() {
      return this.offset;
    }

    public ByteBuffer getByteBuffer() throws IOException {
      // read the data
      byte[] data;
      // if chunk does not exist as file, return empty buffer
      long dataLength = initializedChunks.getOrDefault(chunkNum, (long)0);
      if (dataLength == 0) {
        ByteBuffer result = ByteBuffer.wrap(new byte[0]);
        result.order(byteOrder);
        return result;
      }

      data = new byte[(int)dataLength];
      raf.seek(this.rafOffset);
//      raf.read(data, 0, (int)dataLength);
      raf.readFully(data);

      // apply compressor
      data = compressor.decode(data);
      // apply filters in reverse order
      for (int i = filters.size()-1; i >= 0; i--) {
        data = filters.get(i).decode(data);
      }

      ByteBuffer result = ByteBuffer.wrap(data);
      result.order(byteOrder);
      return result;
    }
  }

}
