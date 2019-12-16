package ucar.nc2.iosp.hdf5;

import java.io.IOException;
import ucar.unidata.io.RandomAccessFile;

/**
 * Temporary workaround to share code between H5header and internal/iosp/H5headerNew.
 * Should go away in version 6.
 */
public interface H5headerIF {

  RandomAccessFile getRandomAccessFile();

  long getFileOffset(long address);

  long readOffset() throws IOException;

  long readLength() throws IOException;

  long readVariableSizeUnsigned(int i) throws IOException;

  byte getSizeOffsets();

  long readAddress() throws IOException;

  byte getSizeLengths();

  int getNumBytesFromMax(long l);

  int makeIntFromBytes(byte[] heapId, int i, int n);

  boolean isOffsetLong();
}
