/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import java.nio.charset.StandardCharsets;

import ucar.array.ArrayType;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.PositioningDataInputStream;
import ucar.ma2.*;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.stream.NcStream;
import java.io.OutputStream;
import java.nio.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Helper methods for IOSP's for reading data.
 * 
 * @deprecated use IospArrayHelper
 */
@Deprecated
public class IospHelper {
  private static final boolean showLayoutTypes = false;

  /**
   * Read data subset from RandomAccessFile, create primitive array of size Layout.getTotalNelems.
   * Reading is controlled by the Layout object.
   *
   * @param raf read from here.
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param fillValue must be Number if dataType.isNumeric(), or String for STRING, byte[] for Structure, or null for
   *        none
   * @param byteOrder if equal to RandomAccessFile.ORDER_XXXX, set the byte order just before reading
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  public static Object readDataFill(RandomAccessFile raf, Layout index, DataType dataType, Object fillValue,
      ByteOrder byteOrder) throws java.io.IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) index.getTotalNelems(), dataType)
        : makePrimitiveArray((int) index.getTotalNelems(), dataType, fillValue);
    return readData(raf, index, dataType, arr, byteOrder, true);
  }

  public static Object readDataFill(RandomAccessFile raf, Layout index, DataType dataType, Object fillValue,
      ByteOrder byteOrder, boolean convertChar) throws java.io.IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) index.getTotalNelems(), dataType)
        : makePrimitiveArray((int) index.getTotalNelems(), dataType, fillValue);
    return readData(raf, index, dataType, arr, byteOrder, convertChar);
  }

  /**
   * Read data subset from RandomAccessFile, place in given primitive array.
   * Reading is controlled by the Layout object.
   *
   * @param raf read from here.
   * @param layout handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param arr primitive array to read data into
   * @param byteOrder if equal to RandomAccessFile.ORDER_XXXX, set the byte order just before reading
   * @param convertChar true if bytes should be converted to char for dataType CHAR
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  public static Object readData(RandomAccessFile raf, Layout layout, DataType dataType, Object arr, ByteOrder byteOrder,
      boolean convertChar) throws java.io.IOException {
    if (showLayoutTypes)
      System.out.println("***RAF LayoutType=" + layout.getClass().getName());

    if (dataType.getPrimitiveClassType() == byte.class || dataType == DataType.CHAR) {
      byte[] pa = (byte[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readFully(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      if (convertChar && dataType == DataType.CHAR)
        return convertByteToChar(pa);
      else
        return pa; // javac ternary compile error

    } else if (dataType.getPrimitiveClassType() == short.class) {
      short[] pa = (short[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readShort(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType.getPrimitiveClassType() == int.class) {
      int[] pa = (int[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readInt(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readFloat(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readDouble(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType.getPrimitiveClassType() == long.class) {
      long[] pa = (long[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readLong(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.STRUCTURE) {
      byte[] pa = (byte[]) arr;
      int recsize = layout.getElemSize();
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readFully(pa, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }
      return pa;

    } else if (dataType == DataType.STRING) {
      int size = (int) layout.getTotalNelems();
      int elemSize = layout.getElemSize();
      StringBuilder sb = new StringBuilder(size);
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) {
          continue;
        }
        for (int i = 0; i < chunk.getNelems(); i++) {
          sb.append(raf.readString(elemSize));
        }
      }
      return sb.toString();
    }

    throw new IllegalStateException("unknown type= " + dataType);
  }

  /**
   * Read data subset from PositioningDataInputStream, create primitive array of size Layout.getTotalNelems.
   * Reading is controlled by the Layout object.
   *
   * @param is read from here.
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param fillValue must be Number if dataType.isNumeric(), or String for STRING, byte[] for Structure, or null for
   *        none
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  public static Object readDataFill(PositioningDataInputStream is, Layout index, DataType dataType, Object fillValue)
      throws java.io.IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) index.getTotalNelems(), dataType)
        : makePrimitiveArray((int) index.getTotalNelems(), dataType, fillValue);
    return readData(is, index, dataType, arr);
  }

  /**
   * Read data subset from PositioningDataInputStream, place in given primitive array.
   * Reading is controlled by the Layout object.
   *
   * @param raf read from here.
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param arr primitive array to read data into
   * @return primitive array with data read in
   * @throws java.io.IOException on read error
   */
  public static Object readData(PositioningDataInputStream raf, Layout index, DataType dataType, Object arr)
      throws java.io.IOException {
    if (showLayoutTypes)
      System.out.println("***PositioningDataInputStream LayoutType=" + index.getClass().getName());

    if (dataType.getPrimitiveClassType() == byte.class || dataType == DataType.CHAR) {
      byte[] pa = (byte[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.read(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      // return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;
      if (dataType == DataType.CHAR)
        return convertByteToChar(pa);
      else
        return pa;

    } else if (dataType.getPrimitiveClassType() == short.class) {
      short[] pa = (short[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readShort(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType.getPrimitiveClassType() == int.class) {
      int[] pa = (int[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readInt(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readFloat(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readDouble(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType.getPrimitiveClassType() == long.class) {
      long[] pa = (long[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readLong(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (dataType == DataType.STRUCTURE) {
      int recsize = index.getElemSize();
      byte[] pa = (byte[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.read(chunk.getSrcPos(), pa, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }
      return pa;
    }

    throw new IllegalStateException();
  } //

  /**
   * Read data subset from ByteBuffer, create primitive array of size Layout.getTotalNelems.
   * Reading is controlled by the Layout object.
   *
   * @param layout handles skipping around in the file, provide ByteBuffer to read from
   * @param dataType dataType of the variable
   * @param fillValue must be Number if dataType.isNumeric(), or String for STRING, byte[] for Structure, or null for
   *        none
   * @return primitive array with data read in
   */
  public static Object readDataFill(LayoutBB layout, DataType dataType, Object fillValue) {
    long size = layout.getTotalNelems();
    if (dataType == DataType.STRUCTURE) {
      size *= layout.getElemSize();
    }
    if (size >= Integer.MAX_VALUE) {
      throw new RuntimeException("Read request too large");
    }
    Object arr = (fillValue == null) ? makePrimitiveArray((int) size, dataType)
        : makePrimitiveArray((int) size, dataType, fillValue);
    return readData(layout, dataType, arr);
  }

  /**
   * Read data subset from ByteBuffer, place in given primitive array.
   * Reading is controlled by the LayoutBB object.
   *
   * @param layout handles skipping around in the file, privide ByteBuffer to read from
   * @param dataType dataType of the variable
   * @param arr primitive array to read data into
   * @return the primitive array with data read in
   */
  public static Object readData(LayoutBB layout, DataType dataType, Object arr) {
    if (showLayoutTypes)
      System.out.println("***BB LayoutType=" + layout.getClass().getName());

    if (dataType.getPrimitiveClassType() == byte.class || (dataType == DataType.CHAR)) {
      byte[] pa = (byte[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        ByteBuffer bb = chunk.getByteBuffer();
        bb.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = bb.get();
      }
      // return (dataType == DataType.CHAR) ? convertByteToChar(pa) : pa;
      if (dataType == DataType.CHAR)
        return convertByteToChar(pa);
      else
        return pa;

    } else if (dataType.getPrimitiveClassType() == short.class) {
      short[] pa = (short[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        ShortBuffer buff = chunk.getShortBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType.getPrimitiveClassType() == int.class) {
      int[] pa = (int[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        IntBuffer buff = chunk.getIntBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = (float[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        FloatBuffer buff = chunk.getFloatBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        DoubleBuffer buff = chunk.getDoubleBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType.getPrimitiveClassType() == long.class) {
      long[] pa = (long[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        LongBuffer buff = chunk.getLongBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (dataType == DataType.STRUCTURE) {
      byte[] pa = (byte[]) arr;
      int recsize = layout.getElemSize();
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        ByteBuffer bb = chunk.getByteBuffer();
        bb.position(chunk.getSrcElem() * recsize);
        int pos = (int) chunk.getDestElem() * recsize;
        for (int i = 0; i < chunk.getNelems() * recsize; i++)
          pa[pos++] = bb.get();
      }
      return pa;
    }

    throw new IllegalStateException();
  }

  /**
   * Copy data to a OutputStream. Used by ncstream. Not doing Structures correctly yet.
   *
   * @param data copy from here
   * @param out copy to here
   * @return number of bytes copied
   * @throws java.io.IOException on write error
   * @deprecated do not use.
   */
  public static long copyToOutputStream(Array data, OutputStream out) throws java.io.IOException {
    Class classType = data.getElementType();

    DataOutputStream dataOut;
    if (out instanceof DataOutputStream)
      dataOut = (DataOutputStream) out;
    else
      dataOut = new DataOutputStream(out);

    /*
     * if (data instanceof ArrayStructure) { // use NcStream encoding
     * return NcStream.encodeArrayStructure((ArrayStructure) data, null, dataOut);
     * }
     */

    IndexIterator iterA = data.getIndexIterator();

    if (classType == double.class) {
      while (iterA.hasNext())
        dataOut.writeDouble(iterA.getDoubleNext());

    } else if (classType == float.class) {
      while (iterA.hasNext())
        dataOut.writeFloat(iterA.getFloatNext());

    } else if (classType == long.class) {
      while (iterA.hasNext())
        dataOut.writeLong(iterA.getLongNext());

    } else if (classType == int.class) {
      while (iterA.hasNext())
        dataOut.writeInt(iterA.getIntNext());

    } else if (classType == short.class) {
      while (iterA.hasNext())
        dataOut.writeShort(iterA.getShortNext());

    } else if (classType == char.class) { // LOOK why are we using chars anyway ?
      byte[] pa = convertCharToByte((char[]) data.get1DJavaArray(DataType.CHAR));
      dataOut.write(pa, 0, pa.length);

    } else if (classType == byte.class) {
      while (iterA.hasNext())
        dataOut.writeByte(iterA.getByteNext());

    } else if (classType == boolean.class) {
      while (iterA.hasNext())
        dataOut.writeBoolean(iterA.getBooleanNext());

    } else if (classType == String.class) {
      long size = 0;
      while (iterA.hasNext()) {
        String s = (String) iterA.getObjectNext();
        size += NcStream.writeString(dataOut, s);
      }
      return size;

    } else if (classType == ByteBuffer.class) { // OPAQUE
      long size = 0;
      while (iterA.hasNext()) {
        ByteBuffer bb = (ByteBuffer) iterA.getObjectNext();
        size += NcStream.writeByteBuffer(dataOut, bb);
      }
      return size;

    } else if (data instanceof ArrayObject) { // vlen
      long size = 0;
      // size += NcStream.writeVInt(outStream, (int) data.getSize()); // nelems already written
      while (iterA.hasNext()) {
        Array row = (Array) iterA.getObjectNext();
        ByteBuffer bb = row.getDataAsByteBuffer();
        size += NcStream.writeByteBuffer(dataOut, bb);
      }
      return size;

    } else
      throw new UnsupportedOperationException("Class type = " + classType.getName());

    return data.getSizeBytes();
  }

  /**
   * Create 1D primitive array of the given size and type
   *
   * @param size the size of the array to create
   * @param dataType dataType of the variable
   * @return primitive array with data read in
   */
  public static Object makePrimitiveArray(int size, DataType dataType) {
    Object arr = null;

    if ((dataType.getPrimitiveClassType() == byte.class) || (dataType == DataType.CHAR) || (dataType == DataType.OPAQUE)
        || (dataType == DataType.STRUCTURE)) {
      arr = new byte[size];

    } else if (dataType.getPrimitiveClassType() == short.class) {
      arr = new short[size];

    } else if (dataType.getPrimitiveClassType() == int.class) {
      arr = new int[size];

    } else if (dataType.getPrimitiveClassType() == long.class) {
      arr = new long[size];

    } else if (dataType == DataType.FLOAT) {
      arr = new float[size];

    } else if (dataType == DataType.DOUBLE) {
      arr = new double[size];

    } else if (dataType == DataType.STRING) {
      arr = new String[size];
    }

    return arr;
  }


  /**
   * Create 1D primitive array of the given size and type, fill it with the given value
   *
   * @param size the size of the array to create
   * @param dataType dataType of the variable
   * @param fillValue must be Number if dataType.isNumeric(), or String for STRING, byte[] for Structure, or null for
   *        none
   * @return primitive array with data read in
   */
  public static Object makePrimitiveArray(int size, DataType dataType, Object fillValue) {

    if (dataType.getPrimitiveClassType() == byte.class || (dataType == DataType.CHAR)) {
      byte[] pa = new byte[size];
      byte val = ((Number) fillValue).byteValue();
      if (val != 0)
        for (int i = 0; i < size; i++)
          pa[i] = val;
      // if (dataType == DataType.CHAR) return convertByteToChar(pa);
      return pa;

    } else if (dataType == DataType.OPAQUE) {
      return new byte[size];

    } else if (dataType.getPrimitiveClassType() == short.class) {
      short[] pa = new short[size];
      short val = ((Number) fillValue).shortValue();
      if (val != 0)
        for (int i = 0; i < size; i++)
          pa[i] = val;
      return pa;

    } else if (dataType.getPrimitiveClassType() == int.class) {
      int[] pa = new int[size];
      int val = ((Number) fillValue).intValue();
      if (val != 0)
        for (int i = 0; i < size; i++)
          pa[i] = val;
      return pa;

    } else if (dataType.getPrimitiveClassType() == long.class) {
      long[] pa = new long[size];
      long val = ((Number) fillValue).longValue();
      if (val != 0)
        for (int i = 0; i < size; i++)
          pa[i] = val;
      return pa;

    } else if (dataType == DataType.FLOAT) {
      float[] pa = new float[size];
      float val = ((Number) fillValue).floatValue();
      if (val != 0.0)
        for (int i = 0; i < size; i++)
          pa[i] = val;
      return pa;

    } else if (dataType == DataType.DOUBLE) {
      double[] pa = new double[size];
      double val = ((Number) fillValue).doubleValue();
      if (val != 0.0)
        for (int i = 0; i < size; i++)
          pa[i] = val;
      return pa;

    } else if (dataType == DataType.STRING) {
      String[] pa = new String[size];
      for (int i = 0; i < size; i++)
        pa[i] = (String) fillValue;
      return pa;

    } else if (dataType == DataType.STRUCTURE) {
      byte[] pa = new byte[size];
      if (fillValue != null) {
        byte[] val = (byte[]) fillValue;
        int count = 0;
        while (count < size && count < val.length)
          for (byte aVal : val)
            pa[count++] = aVal;
      }
      return pa;
    }

    throw new IllegalStateException();
  }

  // convert byte array to char array, assuming UTF-8 encoding
  public static char[] convertByteToCharUTF(byte[] byteArray) {
    return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(byteArray)).array();
  }

  // convert char array to byte array, assuming UTF-8 encoding
  public static byte[] convertCharToByteUTF(char[] from) {
    return StandardCharsets.UTF_8.encode(CharBuffer.wrap(from)).array();
  }

  // convert byte array to char array
  public static char[] convertByteToChar(byte[] byteArray) {
    int size = byteArray.length;
    char[] cbuff = new char[size];
    for (int i = 0; i < size; i++) {
      cbuff[i] = (char) ArrayType.unsignedByteToShort(byteArray[i]); // NOTE: not Unicode !
    }
    return cbuff;
  }

  // convert char array to byte array
  public static byte[] convertCharToByte(char[] from) {
    byte[] to = null;
    if (from != null) {
      int size = from.length;
      to = new byte[size];
      for (int i = 0; i < size; i++)
        to[i] = (byte) from[i]; // LOOK wrong, convert back to unsigned byte ???
    }
    return to;
  }

  /** @deprecated do not use. */
  @Deprecated
  public static ucar.ma2.Array readSection(ParsedSectionSpec cer) throws IOException, InvalidRangeException {
    Variable inner = null;
    List<Range> totalRanges = new ArrayList<>();
    ParsedSectionSpec current = cer;
    while (current != null) {
      totalRanges.addAll(current.getSection().getRanges());
      inner = current.getVariable();
      current = current.getChild();
    }
    assert inner != null;

    Section total = new Section(totalRanges);
    Array result = Array.factory(inner.getDataType(), total.getShape());

    // must be a Structure
    Structure outer = (Structure) cer.getVariable();
    // allows IOSPs to optimize for this case
    Structure outerSubset = outer.select(cer.getChild().getVariable().getShortName());
    ArrayStructure outerData = (ArrayStructure) outerSubset.read(cer.getSection());
    extractSection(cer.getChild(), outerData, result.getIndexIterator());
    return result;
  }

  /** @deprecated do not use. */
  private static void extractSection(ParsedSectionSpec child, ArrayStructure outerData, IndexIterator to)
      throws IOException, InvalidRangeException {
    long wantNelems = child.getSection().computeSize();

    StructureMembers.Member m = outerData.findMember(child.getVariable().getShortName());
    for (int recno = 0; recno < outerData.getSize(); recno++) {
      Array innerData = outerData.getArray(recno, m);

      if (child.getChild() == null) { // inner variable
        if (wantNelems != innerData.getSize())
          innerData = innerData.section(child.getSection().getRanges());
        MAMath.copy(child.getVariable().getDataType(), innerData.getIndexIterator(), to);

      } else { // not an inner variable - must be an ArrayStructure

        if (innerData instanceof ArraySequence)
          extractSectionFromSequence(child.getChild(), (ArraySequence) innerData, to);
        else {
          if (wantNelems != innerData.getSize())
            innerData = sectionArrayStructure(child, (ArrayStructure) innerData, m);
          extractSection(child.getChild(), (ArrayStructure) innerData, to);
        }
      }
    }
  }

  /** @deprecated do not use. */
  private static void extractSectionFromSequence(ParsedSectionSpec child, ArraySequence outerData, IndexIterator to)
      throws IOException {
    try (StructureDataIterator sdataIter = outerData.getStructureDataIterator()) {
      while (sdataIter.hasNext()) {
        StructureData sdata = sdataIter.next();
        StructureMembers.Member m = outerData.findMember(child.getVariable().getShortName());
        Array innerData = sdata.getArray(child.getVariable().getShortName());
        MAMath.copy(m.getDataType(), innerData.getIndexIterator(), to);
      }
    }
  }

  /** @deprecated do not use. */
  private static ArrayStructure sectionArrayStructure(ParsedSectionSpec child, ArrayStructure innerData,
      StructureMembers.Member m) {
    StructureMembers membersw = m.getStructureMembers().toBuilder(false).build(); // no data arrays get propagated
    ArrayStructureW result = new ArrayStructureW(membersw, child.getSection().getShape());

    int count = 0;
    Section.Iterator iter = child.getSection().getIterator(child.getVariable().getShape());
    while (iter.hasNext()) {
      int recno = iter.next(null);
      StructureData sd = innerData.getStructureData(recno);
      result.setStructureData(sd, count++);
    }

    return result;
  }

}
