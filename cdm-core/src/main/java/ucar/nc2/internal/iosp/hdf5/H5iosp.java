/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import static ucar.nc2.NetcdfFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Optional;

import com.google.common.base.Preconditions;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Array;
import ucar.array.ArrayVlen;
import ucar.array.StructureData;
import ucar.array.StructureDataArray;
import ucar.array.StructureDataStorageBB;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.array.StructureMembers;
import ucar.nc2.Group;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.internal.iosp.hdf4.HdfEos;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.IospArrayHelper;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutBB;
import ucar.nc2.iosp.LayoutRegular;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import javax.annotation.Nullable;

/** HDF5 I/O */
public class H5iosp extends AbstractIOServiceProvider {
  public static final String IOSP_MESSAGE_INCLUDE_ORIGINAL_ATTRIBUTES = "IncludeOrgAttributes";

  static final int VLEN_T_SIZE = 16; // Appears to be no way to compute on the fly.

  static boolean debug;
  static boolean debugPos;
  static boolean debugHeap;
  static boolean debugHeapStrings;
  static boolean debugFilter;
  static boolean debugRead;
  static boolean debugFilterIndexer;
  static boolean debugChunkIndexer;
  static boolean debugVlen;
  static boolean debugStructure;
  static boolean useHdfEos = true;

  static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5iosp.class);

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debug = debugFlag.isSet("H5iosp/read");
    debugPos = debugFlag.isSet("H5iosp/filePos");
    debugHeap = debugFlag.isSet("H5iosp/Heap");
    debugFilter = debugFlag.isSet("H5iosp/filter");
    debugFilterIndexer = debugFlag.isSet("H5iosp/filterIndexer");
    debugChunkIndexer = debugFlag.isSet("H5iosp/chunkIndexer");
    debugVlen = debugFlag.isSet("H5iosp/vlen");

    H5header.setDebugFlags(debugFlag);
    if (debugFilter)
      H5tiledLayoutBB.debugFilter = debugFilter;
  }

  //////////////////////////////////////////////////////////////////////////////////

  H5header header;
  boolean isEos;
  boolean includeOriginalAttributes;
  private Charset valueCharset;

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    setRaf(raf);

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
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    return H5header.isValidFile(raf);
  }

  @Override
  public String getFileTypeId() {
    if (isEos) {
      return "HDF5-EOS";
    }
    if (header.isNetcdf4()) {
      return DataFormatType.NETCDF4.getDescription();
    }
    return DataFormatType.HDF5.getDescription();
  }

  @Override
  public String getFileTypeDescription() {
    return "Hierarchical Data Format 5";
  }

  public static void useHdfEos(boolean val) {
    useHdfEos = val;
  }

  @Override
  public String getFileTypeVersion() {
    // TODO this only works for files writtten by netcdf4 c library. what about plain hdf5?
    return ncfile.getRootGroup().findAttributeString(CDM.NCPROPERTIES, "N/A");
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (message instanceof Charset) {
      setValueCharset((Charset) message);
    }
    if (message.equals(IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT)) {
      if (!header.isNetcdf4()) {
        return null;
      }
      return header.isClassic() ? NetcdfFileFormat.NETCDF4_CLASSIC : NetcdfFileFormat.NETCDF4;
    }
    return super.sendIospMessage(message);
  }

  @Override
  public void close() throws IOException {
    super.close();
    header.close();
  }

  @Override
  public void reacquire() throws IOException {
    super.reacquire();
    // TODO headerParser.raf = this.raf;
  }

  @Override
  public String toStringDebug(Object o) {
    if (o instanceof Variable) {
      Variable v = (Variable) o;
      H5header.Vinfo vinfo = (H5header.Vinfo) v.getSPobject();
      return vinfo.toString();
    }
    return null;
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Return {@link Charset value charset} if it was defined. Definition of charset
   * occurs by sending a charset as a message using the {@link #sendIospMessage}
   * method.
   * 
   * @return {@link Charset value charset} if it was defined.
   */
  Optional<Charset> getValueCharset() {
    return Optional.ofNullable(valueCharset);
  }

  /**
   * Define {@link Charset value charset}.
   * 
   * @param charset may be null.
   */
  private void setValueCharset(@Nullable Charset charset) {
    this.valueCharset = charset;
  }

  public H5header getHeader() {
    return header;
  }

  Array<String> convertReferenceArray(Array<Long> refArray) throws IOException {
    int nelems = (int) refArray.getSize();
    String[] result = new String[nelems];
    for (int i = 0; i < nelems; i++) {
      long reference = refArray.get(i);
      String name = header.getDataObjectName(reference);
      result[i] = name != null ? name : Long.toString(reference);
      if (debugVlen)
        System.out.printf(" convertReference 0x%x to %s %n", reference, result[i]);
    }
    return Arrays.factory(ArrayType.STRING, new int[] {nelems}, result);
  }

  // read from the hdf5 heap, insert into StructureDataStorageBB heap
  void convertHeapArray(ByteBuffer hdf5heap, StructureDataStorageBB storage, int pos, ucar.array.StructureMembers sm)
      throws IOException {
    for (ucar.array.StructureMembers.Member m : sm.getMembers()) {
      if (m.getArrayType() == ArrayType.STRING) {
        int size = m.length();
        int destPos = pos + m.getOffset();
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
          result[i] = header.readHeapString(hdf5heap, destPos + i * 16); // 16 byte "heap ids" are in the ByteBuffer
        }

        hdf5heap.order(m.getByteOrder());
        int index = storage.putOnHeap(result);
        hdf5heap.putInt(destPos, index); // overwrite with the index into the StringHeap
        // System.out.printf(" put %s on heap at offset %d value %d bo %s%n", m.getName(), destPos, index, bb.order());

      } else {
        int startPos = pos + m.getOffset();
        ByteOrder endian = m.getByteOrder();
        // Compute rank and size up to the first (and ideally last) VLEN
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
          Array<?> vlenArray = header.readHeapVlen(hdf5heap, destPos, m.getArrayType(), endian);
          fieldarray[i] = vlenArray;
          destPos += VLEN_T_SIZE; // Apparentlly no way to compute VLEN_T_SIZE on the fly
        }
        Array<?> result;
        if (prefixrank == 0) // if scalar, return just the singleton vlen array
          result = fieldarray[0];
        else {
          int[] newshape = new int[prefixrank];
          System.arraycopy(fieldshape, 0, newshape, 0, prefixrank);
          // result = Array.makeObjectArray(m.getArrayType(), fieldarray[0].getClass(), newshape, fieldarray);
          result = null; // Array.makeVlenArray(newshape, fieldarray);
        }
        hdf5heap.order(m.getByteOrder());
        int index = storage.putOnHeap(result);
        hdf5heap.putInt(startPos, index); // overwrite with the index into the Heap
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Array<?> readArrayData(Variable v2, Section section) throws java.io.IOException, InvalidRangeException {
    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    Preconditions.checkNotNull(vinfo);
    if (debugRead) {
      System.out.printf("%s read %s%n", v2.getFullName(), section);
    }
    return readArrayData(v2, vinfo.dataPos, section);
  }

  // all the work is here, so it can be called recursively
  private Array<?> readArrayData(Variable v2, long dataPos, Section wantSection)
      throws IOException, InvalidRangeException {
    H5header.Vinfo vinfo = (H5header.Vinfo) v2.getSPobject();
    ArrayType dataType = v2.getArrayType();
    Object data;
    Layout layout;

    if (vinfo.useFillValue) { // fill value only
      Object pa = IospArrayHelper.makePrimitiveArray((int) wantSection.computeSize(), dataType, vinfo.getFillValue());
      if (dataType == ArrayType.CHAR) {
        pa = IospArrayHelper.convertByteToChar((byte[]) pa);
      }
      return Arrays.factory(dataType, wantSection.getShape(), pa);
    }

    ByteOrder endian = vinfo.typeInfo.endian;
    if (vinfo.mfp != null) { // filtered
      if (debugFilter)
        System.out.println("read variable filtered " + v2.getFullName() + " vinfo = " + vinfo);
      assert vinfo.isChunked;
      layout = new H5tiledLayoutBB(v2, wantSection, raf, vinfo.mfp.getFilters(), endian);
      if (vinfo.typeInfo.isVString) {
        data = readFilteredStringData((LayoutBB) layout);
      } else {
        data = IospArrayHelper.readDataFill((LayoutBB) layout, v2.getArrayType(), vinfo.getFillValue());
      }

    } else { // normal case
      if (debug)
        System.out.println("read variable " + v2.getFullName() + " vinfo = " + vinfo);

      ArrayType readDtype = v2.getArrayType();
      int elemSize = vinfo.elementSize;
      Object fillValue = vinfo.getFillValue();

      // fill in the wantSection
      wantSection = Section.fill(wantSection, v2.getShape());

      if (vinfo.typeInfo.hdfType == 2) { // time
        readDtype = vinfo.mdt.timeType;
        elemSize = readDtype.getSize();
        fillValue = NetcdfFormatUtils.getFillValueDefault(readDtype);

      } else if (vinfo.typeInfo.hdfType == 8) { // enum
        Hdf5Type baseInfo = vinfo.typeInfo.base;
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
      data = readArrayOrPrimitive(vinfo, v2, layout, readDtype, wantSection.getShape(), fillValue, endian, true);
    }

    if (data instanceof Array) {
      return (Array<?>) data;
    } else if (dataType == ArrayType.STRUCTURE) { // TODO does this ever happen?
      return makeStructureDataArray((Structure) v2, layout, wantSection.getShape(), (byte[]) data);
    } else {
      return Arrays.factory(dataType, wantSection.getShape(), data);
    }
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
        // TODO does this handle section correctly ??
        sa[destPos++] = header.readHeapString(bb, (chunk.getSrcElem() + i) * 16);
      }
    }
    return sa;
  }

  /**
   * Read data subset from file for a variable, return Array or java primitive array.
   *
   * @param v the variable to read. Only used for Structure.
   * @param layout handles skipping around in the file.
   * @param dataType dataType of the data to read
   * @param shape the shape of the output
   * @param fillValue fill value as a wrapped primitive
   * @return primitive array or Array with data read in
   * @throws IOException if read error
   */
  Object readArrayOrPrimitive(H5header.Vinfo vinfo, @Nullable Variable v, Layout layout, ArrayType dataType,
      int[] shape, Object fillValue, ByteOrder endian, boolean convertChar) throws IOException {

    Hdf5Type typeInfo = vinfo.typeInfo;

    // special processing
    if (typeInfo.hdfType == 2) { // time
      Object data = IospArrayHelper.readDataFill(raf, layout, dataType, fillValue, endian, true);
      Array<Long> timeArray = Arrays.factory(dataType, shape, data);

      // now transform into an ISO Date String
      String[] stringData = new String[(int) timeArray.length()];
      int count = 0;
      for (long time : timeArray) {
        stringData[count++] = CalendarDate.of(time).toString();
      }
      return stringData;
    }

    if (typeInfo.hdfType == 8) { // enum
      return IospArrayHelper.readDataFill(raf, layout, dataType, fillValue, endian);
    }

    if (typeInfo.isVlen) { // vlen (not string)
      return readVlen(dataType, shape, typeInfo, layout, endian);
    }

    if (dataType == ArrayType.STRUCTURE) { // TODO what about subsetting ?
      if (v == null) {
        throw new IllegalStateException("Cant read in an attribute of type Structure");
      } else {
        return readStructureData((Structure) v, shape, layout);
      }
    }

    if (dataType == ArrayType.STRING) {
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

    if (dataType == ArrayType.OPAQUE) {
      ArrayVlen<?> result = ArrayVlen.factory(ArrayType.OPAQUE, shape);
      Preconditions.checkArgument(Arrays.computeSize(shape) == layout.getTotalNelems());

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
    return IospArrayHelper.readDataFill(raf, layout, dataType, fillValue, endian, convertChar);
  }

  ///////////////////////////////////////////////
  // Vlen

  private Array<?> readVlen(ArrayType dataType, int[] shape, Hdf5Type typeInfo, Layout layout, ByteOrder endian)
      throws IOException {
    ArrayType readType = dataType;
    if (typeInfo.base.hdfType == 7) { // reference
      readType = ArrayType.LONG;
    }

    ArrayVlen<?> vlenArray = ArrayVlen.factory(dataType, shape);
    int count = 0;
    while (layout.hasNext()) {
      Layout.Chunk chunk = layout.next();
      if (chunk == null)
        continue;
      for (int i = 0; i < chunk.getNelems(); i++) {
        long address = chunk.getSrcPos() + layout.getElemSize() * i;
        Object refArray = readHeapPrimitiveArray(address, readType, endian);
        vlenArray.set(count, (typeInfo.base.hdfType == 7) ? convertReferenceArray((long[]) refArray) : refArray);
        count++;
      }
    }
    if (vlenArray.length() == 1) {
      return vlenArray.get(0);
    }
    return vlenArray;
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
  private Object readHeapPrimitiveArray(long globalHeapIdAddress, ArrayType dataType, ByteOrder endian)
      throws IOException {
    H5objects.HeapIdentifier heapId = header.h5objects.readHeapIdentifier(globalHeapIdAddress);
    if (debugHeap) {
      log.debug(" heapId= {}", heapId);
    }

    H5objects.GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (ho == null) {
      throw new IllegalStateException("Illegal Heap address, HeapObject = " + heapId);
    }

    if (debugHeap) {
      log.debug(" HeapObject= {}", ho);
    }
    if (endian != null) {
      raf.order(endian);
    }

    if (ArrayType.FLOAT == dataType) {
      float[] pa = new float[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readFloat(pa, 0, pa.length);
      return pa;

    } else if (ArrayType.DOUBLE == dataType) {
      double[] pa = new double[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readDouble(pa, 0, pa.length);
      return pa;

    } else if (dataType.getPrimitiveClass() == Byte.class) {
      byte[] pa = new byte[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readFully(pa, 0, pa.length);
      return pa;

    } else if (dataType.getPrimitiveClass() == Short.class) {
      short[] pa = new short[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readShort(pa, 0, pa.length);
      return pa;

    } else if (dataType.getPrimitiveClass() == Integer.class) {
      int[] pa = new int[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readInt(pa, 0, pa.length);
      return pa;

    } else if (dataType.getPrimitiveClass() == Long.class) {
      long[] pa = new long[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readLong(pa, 0, pa.length);
      return pa;
    }
    throw new UnsupportedOperationException("getHeapPrimitiveArray dataType=" + dataType);
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // StructureData

  private Array<?> readStructureData(Structure v, int[] shape, Layout layout) throws IOException {
    int recsize = layout.getElemSize();
    long size = recsize * layout.getTotalNelems();
    byte[] byteArray = new byte[(int) size];
    while (layout.hasNext()) {
      Layout.Chunk chunk = layout.next();
      if (chunk == null)
        continue;
      // copy bytes directly into the underlying byte[] TODO : assumes contiguous layout ??
      raf.seek(chunk.getSrcPos());
      raf.readFully(byteArray, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
    }

    // place data into a StructureArray
    return makeStructureDataArray(v, layout, shape, byteArray);
  }

  // already read the data into the byte buffer.
  private Array<StructureData> makeStructureDataArray(Structure s, Layout layout, int[] shape, byte[] byteArray)
      throws IOException {

    // create the StructureMembers
    ucar.array.StructureMembers.Builder mb = s.makeStructureMembersBuilder();

    // set offsets and byteOrders
    boolean hasHeap = augmentStructureMembers(s, mb);

    int recSize = layout.getElemSize();
    mb.setStructureSize(recSize); // needed ?
    ucar.array.StructureMembers sm = mb.build();

    if (recSize != sm.getStorageSizeBytes()) {
      log.error("calcSize = {} actualSize = {}%n", sm.getStorageSizeBytes(), recSize);
      throw new IOException("H5iosp illegal structure size " + s.getFullName());
    }

    // We are going to directly use the bytes on the hdf5 heap (!) for the StructureDataStorageBB
    ByteBuffer hdf5bb = ByteBuffer.wrap(byteArray);
    StructureDataStorageBB storage = new StructureDataStorageBB(sm, hdf5bb, (int) Arrays.computeSize(shape));

    // strings and vlens are stored on the heap, and must be read separately
    if (hasHeap) {
      int destPos = 0;
      for (int i = 0; i < layout.getTotalNelems(); i++) { // loop over each structure
        readHeapData(hdf5bb, storage, destPos, sm);
        destPos += layout.getElemSize();
      }
    }

    return new StructureDataArray(sm, shape, storage);
  }

  // recursive
  private boolean augmentStructureMembers(Structure s, StructureMembers.Builder sm) {
    boolean hasHeap = false;
    for (StructureMembers.MemberBuilder mb : sm.getStructureMembers()) {
      Variable v2 = s.findVariable(mb.getName());
      Preconditions.checkNotNull(v2);
      H5header.Vinfo vm = (H5header.Vinfo) v2.getSPobject();

      // apparently each member may have different byte order (!!!??)
      // perhaps better to flip as needed?
      if (vm.typeInfo.endian != null) {
        mb.setByteOrder(vm.typeInfo.endian);
      }

      // vm.dataPos : offset since start of Structure
      mb.setOffset((int) vm.dataPos);

      // track if there is a heap
      if (v2.getArrayType() == ArrayType.STRING || v2.isVariableLength()) {
        hasHeap = true;
      }

      // recurse : nested structure are inside of outer structure in the byte array
      if (v2 instanceof Structure) {
        Structure nested = (Structure) v2;
        StructureMembers.Builder nestSm = mb.getStructureMembers();
        hasHeap |= augmentStructureMembers(nested, nestSm);
      }
    }
    return hasHeap;
  }

  // Reads the Strings and Vlens from the hdf5 heap into a structure data storage
  private void readHeapData(ByteBuffer storageBB, StructureDataStorageBB storage, int pos, StructureMembers sm)
      throws IOException {

    for (StructureMembers.Member m : sm.getMembers()) {
      ByteOrder endian = m.getByteOrder();
      storageBB.order(endian);
      if (m.getArrayType() == ArrayType.STRING) {
        int size = m.length();
        int destPos = pos + m.getOffset();
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
          result[i] = header.readHeapString(storageBB, destPos + i * 16); // 16 byte "heap ids" are in the ByteBuffer
        }

        int index = storage.putOnHeap(result);
        storageBB.order(endian); // write the string index in whatever that member's byte order is.
        storageBB.putInt(destPos, index); // overwrite with the index into the StringHeap

      } else if (m.isVlen()) { // TODO needs testing
        int startPos = pos + m.getOffset();
        // hdf5heap.order(ByteOrder.LITTLE_ENDIAN); // why ?

        ArrayVlen<?> vlenArray = ArrayVlen.factory(m.getArrayType(), m.getShape());
        int size = (int) Arrays.computeSize(vlenArray.getShape());
        Preconditions.checkArgument(size == m.length(), "Internal error: field size mismatch");

        int readPos = startPos;
        for (int i = 0; i < size; i++) {
          // header.readHeapVlen reads the vlen at destPos from H5 heap, into an Array of T.
          // Structs not supported.
          Array<?> vlen = header.readHeapVlen(storageBB, readPos, m.getArrayType(), endian);
          vlenArray.set(i, vlen);
          readPos += VLEN_T_SIZE;
        }
        // put resulting ArrayVlen into the storage heap.
        Array heapArray = vlenArray.length() == 1 ? vlenArray.get(0) : vlenArray;
        int index = storage.putOnHeap(heapArray);
        storageBB.order(endian);
        storageBB.putInt(startPos, index); // overwrite with the index into the Heap
      }
    }
  }
}
