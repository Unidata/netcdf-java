/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import ucar.array.ArrayVlen;
import ucar.array.Arrays;
import ucar.array.StructureMembers;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Group;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.internal.iosp.hdf4.HdfEos;
import ucar.nc2.internal.iosp.hdf5.H5objects.GlobalHeap;
import ucar.nc2.internal.iosp.hdf5.H5objects.HeapIdentifier;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutBB;
import ucar.nc2.iosp.LayoutRegular;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

/** HDF5 I/O with ucar.array.Array */
public class H5iospArrays extends H5iosp {

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    super.open(raf, rootGroup.getNcfile(), cancelTask);

    raf.order(RandomAccessFile.BIG_ENDIAN);
    header = new H5header(raf, rootGroup, this);
    header.read(null);

    // check if its an HDF5-EOS file
    if (useHdfEos) {
      rootGroup.findGroupLocal(HdfEos.HDF5_GROUP).ifPresent(eosGroup -> {
        try {
          isEos = HdfEos.amendFromODL(raf.getLocation(), header, eosGroup);
        } catch (IOException e) {
          log.warn(" HdfEos.amendFromODL failed");
        }
      });
    }
  }

  @Override
  public ucar.array.Array<?> readArrayData(Variable v2, Section section)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {
    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    Preconditions.checkNotNull(vinfo);
    if (debugRead) {
      System.out.printf("%s read %s%n", v2.getFullName(), section);
    }
    return readArrayData(v2, vinfo.dataPos, section);
  }

  // all the work is here, so it can be called recursively
  private ucar.array.Array<?> readArrayData(Variable v2, long dataPos, Section wantSection)
      throws IOException, InvalidRangeException {
    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();
    Object data;
    Layout layout;

    if (vinfo.useFillValue) { // fill value only
      Object pa = IospHelper.makePrimitiveArray((int) wantSection.computeSize(), dataType, vinfo.getFillValue());
      if (dataType == DataType.CHAR) {
        pa = IospHelper.convertByteToChar((byte[]) pa);
      }
      return Arrays.factory(dataType, wantSection.getShape(), pa);
    }

    if (vinfo.mfp != null) { // filtered
      if (debugFilter)
        System.out.println("read variable filtered " + v2.getFullName() + " vinfo = " + vinfo);
      assert vinfo.isChunked;
      ByteOrder bo = (vinfo.typeInfo.endian == 0) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
      layout = new H5tiledLayoutBB(v2, wantSection, raf, vinfo.mfp.getFilters(), bo);
      if (vinfo.typeInfo.isVString) {
        data = readFilteredStringData((LayoutBB) layout);
      } else {
        data = IospHelper.readDataFill((LayoutBB) layout, v2.getDataType(), vinfo.getFillValue());
      }

    } else { // normal case
      if (debug)
        System.out.println("read variable " + v2.getFullName() + " vinfo = " + vinfo);

      DataType readDtype = v2.getDataType();
      int elemSize = v2.getElementSize();
      Object fillValue = vinfo.getFillValue();
      int endian = vinfo.typeInfo.endian;

      // fill in the wantSection
      wantSection = Section.fill(wantSection, v2.getShape());

      if (vinfo.typeInfo.hdfType == 2) { // time
        readDtype = vinfo.mdt.timeType;
        elemSize = readDtype.getSize();
        fillValue = NetcdfFormatUtils.getFillValueDefault(readDtype);

      } else if (vinfo.typeInfo.hdfType == 8) { // enum
        H5header.TypeInfo baseInfo = vinfo.typeInfo.base;
        readDtype = baseInfo.dataType;
        elemSize = readDtype.getSize();
        fillValue = NetcdfFormatUtils.getFillValueDefault(readDtype);
        endian = baseInfo.endian;

      } else if (vinfo.typeInfo.hdfType == 9) { // vlen
        elemSize = vinfo.typeInfo.byteSize;
        endian = vinfo.typeInfo.endian;
        // wantSection = wantSection.removeVlen(); // remove vlen dimension
      }

      if (vinfo.isChunked) {
        layout = new H5tiledLayout((H5header.Vinfo) v2.getSPobject(), readDtype, wantSection);
      } else {
        layout = new LayoutRegular(dataPos, elemSize, v2.getShape(), wantSection);
      }
      data = readArrayOrPrimitive(vinfo, v2, layout, readDtype, wantSection.getShape(), fillValue, endian);
    }

    if (data instanceof ucar.array.Array)
      return (ucar.array.Array<?>) data;
    else if (dataType == DataType.STRUCTURE) // LOOK does this ever happen?
      return makeStructureDataArray((Structure) v2, layout, wantSection.getShape(), (byte[]) data); // LOOK
    else
      return Arrays.factory(dataType, wantSection.getShape(), data);
  }

  private String[] readFilteredStringData(LayoutBB layout) throws IOException {
    int size = (int) layout.getTotalNelems();
    String[] sa = new String[size];
    while (layout.hasNext()) {
      LayoutBB.Chunk chunk = layout.next();
      ByteBuffer bb = chunk.getByteBuffer();
      // bb.position(chunk.getSrcElem());
      if (debugHeapStrings)
        System.out.printf("readFilteredStringData chunk=%s%n", chunk);
      int destPos = (int) chunk.getDestElem();
      for (int i = 0; i < chunk.getNelems(); i++) { // 16 byte "heap ids"
        // LOOK does this handle section correctly ??
        sa[destPos++] = header.readHeapString(bb, (chunk.getSrcElem() + i) * 16);
      }
    }
    return sa;
  }

  /**
   * Read data subset from file for a variable, return Array or java primitive array.
   *
   * @param v the variable to read.
   * @param layout handles skipping around in the file.
   * @param dataType dataType of the data to read
   * @param shape the shape of the output
   * @param fillValue fill value as a wrapped primitive
   * @return primitive array or Array with data read in
   * @throws IOException if read error
   * @throws InvalidRangeException if invalid section
   */
  private Object readArrayOrPrimitive(H5header.Vinfo vinfo, Variable v, Layout layout, DataType dataType, int[] shape,
      Object fillValue, int endian) throws IOException, InvalidRangeException {

    H5header.TypeInfo typeInfo = vinfo.typeInfo;

    // special processing
    if (typeInfo.hdfType == 2) { // time
      Object data = IospHelper.readDataFill(raf, layout, dataType, fillValue, endian, true);
      ucar.array.Array<Long> timeArray = Arrays.factory(dataType, shape, data);

      // now transform into an ISO Date String
      String[] stringData = new String[(int) timeArray.length()];
      int count = 0;
      for (long time : timeArray) {
        stringData[count++] = CalendarDate.of(time).toString();
      }
      return stringData;
    }

    if (typeInfo.hdfType == 8) { // enum
      return IospHelper.readDataFill(raf, layout, dataType, fillValue, endian);
    }

    if (typeInfo.isVlen) { // vlen (not string)
      return readVlen(dataType, shape, typeInfo, layout, endian);
    }

    if (dataType == DataType.STRUCTURE) { // LOOK what about subsetting ?
      return readStructureData((Structure) v, shape, layout);
    }

    // normal case
    return readDataArrayPrimitive(layout, dataType, shape, fillValue, endian, true);
  }

  /**
   * Read data subset from file for a variable, create primitive array.
   *
   * @param layout handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param shape the shape of the output
   * @param fillValue fill value as a wrapped primitive
   * @param endian byte order
   * @return primitive array with data read in
   */
  private Object readDataArrayPrimitive(Layout layout, DataType dataType, int[] shape, Object fillValue, int endian,
      boolean convertChar) throws IOException {

    if (dataType == DataType.STRING) {
      int size = (int) layout.getTotalNelems();
      String[] sa = new String[size];
      int count = 0;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null)
          continue;
        for (int i = 0; i < chunk.getNelems(); i++) { // 16 byte "heap ids"
          sa[count++] = header.readHeapString(chunk.getSrcPos() + layout.getElemSize() * i);
        }
      }
      return sa;
    }

    if (dataType == DataType.OPAQUE) {
      ArrayVlen<?> result = ArrayVlen.factory(DataType.OPAQUE, shape);
      Preconditions.checkArgument(Index.computeSize(shape) == layout.getTotalNelems());

      int count = 0;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null)
          continue;
        int recsize = layout.getElemSize();
        for (int i = 0; i < chunk.getNelems(); i++) {
          byte[] pa = new byte[recsize];
          raf.seek(chunk.getSrcPos() + i * recsize);
          raf.readFully(pa, 0, recsize);
          result.set(count++, pa);
        }
      }
      return result;
    }

    // normal case
    return IospHelper.readDataFill(raf, layout, dataType, fillValue, endian, convertChar);
  }

  ///////////////////////////////////////////////
  // Vlen

  private ucar.array.Array<?> readVlen(DataType dataType, int[] shape, H5header.TypeInfo typeInfo, Layout layout,
      int endian) throws IOException {
    DataType readType = dataType;
    if (typeInfo.base.hdfType == 7) { // reference
      readType = DataType.LONG;
    }

    ArrayVlen<?> result = ArrayVlen.factory(dataType, shape);
    int count = 0;
    while (layout.hasNext()) {
      Layout.Chunk chunk = layout.next();
      if (chunk == null)
        continue;
      for (int i = 0; i < chunk.getNelems(); i++) {
        long address = chunk.getSrcPos() + layout.getElemSize() * i;
        Object refArray = readHeapPrimitiveArray(address, readType, endian);
        result.set(count, (typeInfo.base.hdfType == 7) ? convertReferenceArray((long[]) refArray) : refArray);
        count++;
      }
    }
    return result;
  }

  private String[] convertReferenceArray(long[] refArray) throws IOException {
    int nelems = refArray.length;
    String[] result = new String[nelems];
    int count = 0;
    for (long reference : refArray) {
      String name = header.getDataObjectName(reference);
      result[count++] = name != null ? name : Long.toString(reference);
    }
    return result;
  }

  /**
   * Fetch a Vlen data array.
   *
   * @param globalHeapIdAddress address of the heapId, used to get the String out of the heap
   * @param dataType type of data
   * @param endian byteOrder of the data (0 = BE, 1 = LE)
   * @return the primitice array read from the heap
   */
  private Object readHeapPrimitiveArray(long globalHeapIdAddress, DataType dataType, int endian) throws IOException {
    HeapIdentifier heapId = header.h5objects.readHeapIdentifier(globalHeapIdAddress);
    if (debugHeap) {
      log.debug(" heapId= {}", heapId);
    }

    GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (ho == null) {
      throw new IllegalStateException("Illegal Heap address, HeapObject = " + heapId);
    }

    if (debugHeap) {
      log.debug(" HeapObject= {}", ho);
    }
    if (endian >= 0) {
      raf.order(endian);
    }

    if (DataType.FLOAT == dataType) {
      float[] pa = new float[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readFloat(pa, 0, pa.length);
      return pa;

    } else if (DataType.DOUBLE == dataType) {
      double[] pa = new double[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readDouble(pa, 0, pa.length);
      return pa;

    } else if (dataType.getPrimitiveClassType() == byte.class) {
      byte[] pa = new byte[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readFully(pa, 0, pa.length);
      return pa;

    } else if (dataType.getPrimitiveClassType() == short.class) {
      short[] pa = new short[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readShort(pa, 0, pa.length);
      return pa;

    } else if (dataType.getPrimitiveClassType() == int.class) {
      int[] pa = new int[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readInt(pa, 0, pa.length);
      return pa;

    } else if (dataType.getPrimitiveClassType() == long.class) {
      long[] pa = new long[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readLong(pa, 0, pa.length);
      return pa;
    }
    throw new UnsupportedOperationException("getHeapPrimitiveArray dataType=" + dataType);
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // StructureData

  private ucar.array.Array<?> readStructureData(Structure v, int[] shape, Layout layout) throws IOException {
    int recsize = layout.getElemSize();
    long size = recsize * layout.getTotalNelems();
    byte[] byteArray = new byte[(int) size];
    while (layout.hasNext()) {
      Layout.Chunk chunk = layout.next();
      if (chunk == null)
        continue;
      if (debugStructure) {
        System.out.println(
            " readStructure " + v.getFullName() + " chunk= " + chunk + " index.getElemSize= " + layout.getElemSize());
      }
      // copy bytes directly into the underlying byte[] LOOK : assumes contiguous layout ??
      raf.seek(chunk.getSrcPos());
      raf.readFully(byteArray, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
    }

    // place data into a StructureArray
    return makeStructureDataArray(v, layout, shape, byteArray); // LOOK
  }

  private ucar.array.Array<ucar.array.StructureData> makeStructureDataArray(Structure s, Layout layout, int[] shape,
      byte[] byteArray) throws IOException {

    // create the StructureMembers
    ucar.array.StructureMembers.Builder mb = ucar.array.StructureMembers.makeStructureMembers(s);

    // set offsets and byteOrders
    boolean hasHeap = augmentStructureMembers(s, mb);

    // LOOK Problem is Strings and Vlens, and nested Structures
    // LOOK Opaque is ok, because in hdf5, its not variable length!
    // This is wrong, need to use H5header.Vinfo.dataPos

    int recSize = layout.getElemSize();
    mb.setStructureSize(recSize); // needed ?
    ucar.array.StructureMembers sm = mb.build();

    if (recSize != sm.getStructureSize()) {
      log.error("calcSize = {} actualSize = {}%n", sm.getStructureSize(), recSize);
      throw new IOException("H5iosp illegal structure size " + s.getFullName());
    }

    ucar.array.StructureDataArray2 structArray =
        new ucar.array.StructureDataArray2(sm, shape, ByteBuffer.wrap(byteArray));

    // strings and vlens are stored on the heap, and must be read separately
    if (hasHeap) {
      int destPos = 0;
      for (int i = 0; i < layout.getTotalNelems(); i++) { // loop over each structure
        convertArrayHeap(structArray, destPos, sm);
        destPos += layout.getElemSize();
      }
    }

    return structArray;
  }

  // recursive
  private boolean augmentStructureMembers(Structure s, StructureMembers.Builder sm) {
    boolean hasHeap = false;
    for (StructureMembers.MemberBuilder mb : sm.getStructureMembers()) {
      Variable v2 = s.findVariable(mb.getName());
      assert v2 != null;
      H5header.Vinfo vm = (H5header.Vinfo) v2.getSPobject();

      // apparently each member may have seperate byte order (!!!??)
      // perhaps better to flip as needed?
      if (vm.typeInfo.endian >= 0) {
        mb.setByteOrder(
            vm.typeInfo.endian == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
      }

      // vm.dataPos : offset since start of Structure
      mb.setOffset((int) vm.dataPos);

      // track if there is a heap
      if (v2.getDataType() == DataType.STRING || v2.isVariableLength()) {
        hasHeap = true;
      }

      // recurse
      if (v2 instanceof Structure) {
        Structure nested = (Structure) v2;
        StructureMembers.Builder nestSm = mb.getStructureMembers();
        hasHeap |= augmentStructureMembers(nested, nestSm);
      }
    }
    return hasHeap;
  }

  private void convertArrayHeap(ucar.array.StructureDataArray2 asbb, int pos, StructureMembers sm) throws IOException {
    ByteBuffer bb = asbb.getByteBuffer();
    for (StructureMembers.Member m : sm.getMembers()) {
      if (m.getDataType() == DataType.STRING) {
        int size = m.length();
        int destPos = pos + m.getOffset();
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
          result[i] = header.readHeapString(bb, destPos + i * 16); // 16 byte "heap ids" are in the ByteBuffer
        }

        int index = asbb.addObjectToHeap(result);
        bb.order(m.getByteOrder()); // write the string index in whatever that member's byte order is.
        bb.putInt(destPos, index); // overwrite with the index into the StringHeap

      } else if (m.isVariableLength()) {
        int startPos = pos + m.getOffset();
        bb.order(ByteOrder.LITTLE_ENDIAN);

        ByteOrder bo = (ByteOrder) m.getByteOrder();
        int endian = bo.equals(ByteOrder.LITTLE_ENDIAN) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;
        // Compute rank and size upto the first (and ideally last) VLEN
        int[] fieldshape = m.getShape();
        int prefixrank = 0;
        int size = 1;
        for (; prefixrank < fieldshape.length; prefixrank++) {
          if (fieldshape[prefixrank] < 0)
            break;
          size *= fieldshape[prefixrank];
        }
        assert size == m.length() : "Internal error: field size mismatch";
        Array[] fieldarray = new Array[size]; // hold all the vlen instance data
        // destPos will point to each vlen instance in turn
        // assuming we have 'size' such instances in a row.
        int destPos = startPos;
        for (int i = 0; i < size; i++) {
          // vlenarray extracts the i'th vlen contents (struct not supported).
          Array vlenArray = header.readHeapVlen(bb, destPos, m.getDataType(), endian);
          fieldarray[i] = vlenArray;
          destPos += VLEN_T_SIZE; // Apparentlly no way to compute VLEN_T_SIZE on the fly
        }
        Array result;
        if (prefixrank == 0) // if scalar, return just the singleton vlen array
          result = fieldarray[0];
        else {
          int[] newshape = new int[prefixrank];
          System.arraycopy(fieldshape, 0, newshape, 0, prefixrank);
          // result = Array.makeObjectArray(m.getDataType(), fieldarray[0].getClass(), newshape, fieldarray);
          result = Array.makeVlenArray(newshape, fieldarray);
        }
        int index = asbb.addObjectToHeap(result);
        bb.order(ByteOrder.nativeOrder());
        bb.putInt(startPos, index); // overwrite with the index into the Heap
      }
    }
  }

}
