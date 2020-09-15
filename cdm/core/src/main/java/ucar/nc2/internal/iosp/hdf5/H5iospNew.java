/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import static ucar.nc2.NetcdfFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Optional;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureBB;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureMembers;
import ucar.nc2.Group;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.internal.iosp.hdf4.HdfEos;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutBB;
import ucar.nc2.iosp.LayoutRegular;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import javax.annotation.Nullable;

/** HDF5 I/O */
public class H5iospNew extends AbstractIOServiceProvider {
  public static final String IOSP_MESSAGE_INCLUDE_ORIGINAL_ATTRIBUTES = "IncludeOrgAttributes";

  private static final int VLEN_T_SIZE = 16; // Appears to be no way to compute on the fly.

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

  static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5iospNew.class);

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debug = debugFlag.isSet("H5iosp/read");
    debugPos = debugFlag.isSet("H5iosp/filePos");
    debugHeap = debugFlag.isSet("H5iosp/Heap");
    debugFilter = debugFlag.isSet("H5iosp/filter");
    debugFilterIndexer = debugFlag.isSet("H5iosp/filterIndexer");
    debugChunkIndexer = debugFlag.isSet("H5iosp/chunkIndexer");
    debugVlen = debugFlag.isSet("H5iosp/vlen");

    H5headerNew.setDebugFlags(debugFlag);
    if (debugFilter)
      H5tiledLayoutBB.debugFilter = debugFilter;
  }

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    return H5headerNew.isValidFile(raf);
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

  //////////////////////////////////////////////////////////////////////////////////

  private H5headerNew header;
  private boolean isEos;
  boolean includeOriginalAttributes;
  private Charset valueCharset;

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    super.open(raf, rootGroup.getNcfile(), cancelTask);

    raf.order(RandomAccessFile.BIG_ENDIAN);
    header = new H5headerNew(raf, rootGroup, this);
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

  public H5headerNew getHeader() {
    return header;
  }

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    H5headerNew.Vinfo vinfo = (H5headerNew.Vinfo) v2.getSPobject();
    if (debugRead)
      System.out.printf("%s read %s%n", v2.getFullName(), section);
    return readData(v2, vinfo.dataPos, section);
  }

  // all the work is here, so can be called recursively
  private Array readData(Variable v2, long dataPos, Section wantSection) throws IOException, InvalidRangeException {
    H5headerNew.Vinfo vinfo = (H5headerNew.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();
    Object data;
    Layout layout;

    if (vinfo.useFillValue) { // fill value only
      Object pa = IospHelper.makePrimitiveArray((int) wantSection.computeSize(), dataType, vinfo.getFillValue());
      if (dataType == DataType.CHAR)
        pa = IospHelper.convertByteToChar((byte[]) pa);
      return Array.factory(dataType, wantSection.getShape(), pa);
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
        H5headerNew.TypeInfo baseInfo = vinfo.typeInfo.base;
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
        layout = new H5tiledLayout((H5headerNew.Vinfo) v2.getSPobject(), readDtype, wantSection);
      } else {
        layout = new LayoutRegular(dataPos, elemSize, v2.getShape(), wantSection);
      }
      data = readData(vinfo, v2, layout, readDtype, wantSection.getShape(), fillValue, endian);
    }

    if (data instanceof Array)
      return (Array) data;
    else if (dataType == DataType.STRUCTURE)
      return convertStructure((Structure) v2, layout, wantSection.getShape(), (byte[]) data); // LOOK
    else
      return Array.factory(dataType, wantSection.getShape(), data);
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
        sa[destPos++] = header.readHeapString(bb, (chunk.getSrcElem() + i) * 16); // LOOK does this handle section
                                                                                  // correctly ??
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
   * @throws java.io.IOException if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  private Object readData(H5headerNew.Vinfo vinfo, Variable v, Layout layout, DataType dataType, int[] shape,
      Object fillValue, int endian) throws IOException, InvalidRangeException {

    H5headerNew.TypeInfo typeInfo = vinfo.typeInfo;

    // special processing
    if (typeInfo.hdfType == 2) { // time
      Object data = IospHelper.readDataFill(raf, layout, dataType, fillValue, endian, true);
      Array timeArray = Array.factory(dataType, shape, data);

      // now transform into an ISO Date String
      String[] stringData = new String[(int) timeArray.getSize()];
      int count = 0;
      while (timeArray.hasNext()) {
        long time = timeArray.nextLong();
        stringData[count++] = CalendarDate.of(time).toString();
      }
      return Array.factory(DataType.STRING, shape, stringData);
    }

    if (typeInfo.hdfType == 8) { // enum
      Object data = IospHelper.readDataFill(raf, layout, dataType, fillValue, endian);
      return Array.factory(dataType, shape, data);
    }

    if (typeInfo.isVlen) { // vlen (not string)
      DataType readType = dataType;
      if (typeInfo.base.hdfType == 7) // reference
        readType = DataType.LONG;

      // general case is to read an array of vlen objects
      // each vlen generates an Array - so return ArrayObject of Array
      // boolean scalar = false; // layout.getTotalNelems() == 1; // if scalar, return just the len Array // remove
      // 12/25/10 jcaron
      Array[] data = new Array[(int) layout.getTotalNelems()];
      int count = 0;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null)
          continue;
        for (int i = 0; i < chunk.getNelems(); i++) {
          long address = chunk.getSrcPos() + layout.getElemSize() * i;
          Array vlenArray = header.getHeapDataArray(address, readType, endian);
          data[count++] = (typeInfo.base.hdfType == 7) ? convertReference(vlenArray) : vlenArray;
        }
      }
      int prefixrank = 0;
      for (int i = 0; i < shape.length; i++) { // find leftmost vlen
        if (shape[i] < 0) {
          prefixrank = i;
          break;
        }
      }
      Array result;
      if (prefixrank == 0) // if scalar, return just the singleton vlen array
        result = data[0];
      else {
        int[] newshape = new int[prefixrank];
        System.arraycopy(shape, 0, newshape, 0, prefixrank);
        result = Array.makeVlenArray(newshape, data);
      }
      return result;
    }

    if (dataType == DataType.STRUCTURE) { // LOOK what about subset ?
      int recsize = layout.getElemSize();
      long size = recsize * layout.getTotalNelems();
      byte[] byteArray = new byte[(int) size];
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null)
          continue;
        if (debugStructure)
          System.out.println(
              " readStructure " + v.getFullName() + " chunk= " + chunk + " index.getElemSize= " + layout.getElemSize());
        // copy bytes directly into the underlying byte[] LOOK : assumes contiguous layout ??
        raf.seek(chunk.getSrcPos());
        raf.readFully(byteArray, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }

      // place data into an ArrayStructureBB
      return convertStructure((Structure) v, layout, shape, byteArray); // LOOK
    }

    // normal case
    return readDataPrimitive(layout, dataType, shape, fillValue, endian, true);
  }

  Array convertReference(Array refArray) throws IOException {
    int nelems = (int) refArray.getSize();
    Index ima = refArray.getIndex();
    String[] result = new String[nelems];
    for (int i = 0; i < nelems; i++) {
      long reference = refArray.getLong(ima.set(i));
      String name = header.getDataObjectName(reference);
      result[i] = name != null ? name : Long.toString(reference);
      if (debugVlen)
        System.out.printf(" convertReference 0x%x to %s %n", reference, result[i]);
    }
    return Array.factory(DataType.STRING, new int[] {nelems}, result);
  }

  private ArrayStructure convertStructure(Structure s, Layout layout, int[] shape, byte[] byteArray)
      throws IOException, InvalidRangeException {
    // create StructureMembers - must set offsets
    StructureMembers sm = s.makeStructureMembers();
    int calcSize = ArrayStructureBB.setOffsets(sm); // standard

    // special offset setting
    boolean hasHeap = convertStructure(s, sm);

    int recSize = layout.getElemSize();
    if (recSize < calcSize) {
      log.error("calcSize = {} actualSize = {}%n", calcSize, recSize);
      throw new IOException("H5iosp illegal structure size " + s.getFullName());
    }
    sm.setStructureSize(recSize);

    // place data into an ArrayStructureBB
    ByteBuffer bb = ByteBuffer.wrap(byteArray);
    ArrayStructureBB asbb = new ArrayStructureBB(sm, shape, bb, 0);

    // strings and vlens are stored on the heap, and must be read separately
    if (hasHeap) {
      int destPos = 0;
      for (int i = 0; i < layout.getTotalNelems(); i++) { // loop over each structure
        convertHeap(asbb, destPos, sm);
        destPos += layout.getElemSize();
      }
    }
    return asbb;
  }

  // recursive
  private boolean convertStructure(Structure s, StructureMembers sm) {
    boolean hasHeap = false;
    for (StructureMembers.Member m : sm.getMembers()) {
      Variable v2 = s.findVariable(m.getName());
      assert v2 != null;
      H5headerNew.Vinfo vm = (H5headerNew.Vinfo) v2.getSPobject();

      // apparently each member may have seperate byte order (!!!??)
      if (vm.typeInfo.endian >= 0) {
        m.setDataObject(
            vm.typeInfo.endian == RandomAccessFile.LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
      }

      // vm.dataPos : offset since start of Structure
      m.setDataParam((int) vm.dataPos);

      // track if there is a heap
      if (v2.getDataType() == DataType.STRING || v2.isVariableLength())
        hasHeap = true;

      // recurse
      if (v2 instanceof Structure) {
        Structure nested = (Structure) v2;
        StructureMembers nestSm = nested.makeStructureMembers();
        m.setStructureMembers(nestSm);
        hasHeap |= convertStructure(nested, nestSm);
      }
    }
    return hasHeap;
  }

  void convertHeap(ArrayStructureBB asbb, int pos, StructureMembers sm) throws IOException, InvalidRangeException {
    ByteBuffer bb = asbb.getByteBuffer();
    for (StructureMembers.Member m : sm.getMembers()) {
      if (m.getDataType() == DataType.STRING) {
        m.setDataObject(ByteOrder.nativeOrder()); // the index is always written in "native order"
        int size = m.getSize();
        int destPos = pos + m.getDataParam();
        String[] result = new String[size];
        for (int i = 0; i < size; i++)
          result[i] = header.readHeapString(bb, destPos + i * 16); // 16 byte "heap ids" are in the ByteBuffer

        int index = asbb.addObjectToHeap(result);
        bb.order(ByteOrder.nativeOrder()); // the string index is always written in "native order"
        bb.putInt(destPos, index); // overwrite with the index into the StringHeap

      } else if (m.isVariableLength()) {
        int startPos = pos + m.getDataParam();
        bb.order(ByteOrder.LITTLE_ENDIAN);

        ByteOrder bo = (ByteOrder) m.getDataObject();
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
        assert size == m.getSize() : "Internal error: field size mismatch";
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

  /**
   * Read data subset from file for a variable, create primitive array.
   *
   * @param layout handles skipping around in the file.
   * @param dataType dataType of the variable
   * @param shape the shape of the output
   * @param fillValue fill value as a wrapped primitive
   * @param endian byte order
   * @return primitive array with data read in
   * @throws java.io.IOException if read error
   */
  Object readDataPrimitive(Layout layout, DataType dataType, int[] shape, Object fillValue, int endian,
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
      Array opArray = Array.factory(DataType.OPAQUE, shape);
      assert (new Section(shape).computeSize() == layout.getTotalNelems());

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
          opArray.setObject(count++, ByteBuffer.wrap(pa));
        }
      }
      return opArray;
    }

    // normal case
    return IospHelper.readDataFill(raf, layout, dataType, fillValue, endian, convertChar);
  }

  //////////////////////////////////////////////////////////////////////////
  // override base class

  @Override
  public void close() throws IOException {
    super.close();
    header.close();
  }

  @Override
  public void reacquire() throws IOException {
    super.reacquire();
    // LOOK headerParser.raf = this.raf;
  }

  @Override
  public String toStringDebug(Object o) {
    if (o instanceof Variable) {
      Variable v = (Variable) o;
      H5headerNew.Vinfo vinfo = (H5headerNew.Vinfo) v.getSPobject();
      return vinfo.toString();
    }
    return null;
  }

  /*
   * @Override
   * public String getDetailInfo() {
   * Formatter f = new Formatter();
   * ByteArrayOutputStream os = new ByteArrayOutputStream(100 * 1000);
   * PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
   * 
   * try {
   * NetcdfFile ncfile = new NetcdfFileSubclass();
   * H5headerNew detailParser = new H5headerNew(raf, ncfile, this);
   * detailParser.read(pw);
   * f.format("%s", super.getDetailInfo());
   * f.format("%s", os.toString(CDM.UTF8));
   * 
   * } catch (IOException e) {
   * e.printStackTrace();
   * }
   * 
   * return f.toString();
   * }
   * 
   * @Override
   * public Object sendIospMessage(Object message) {
   * if (message.toString().equals(IOSP_MESSAGE_INCLUDE_ORIGINAL_ATTRIBUTES)) {
   * includeOriginalAttributes = true;
   * return null;
   * }
   * 
   * if (message.toString().equals("header"))
   * return headerParser;
   * 
   * if (message.toString().equals("headerEmpty")) {
   * NetcdfFile ncfile = new NetcdfFileSubclass();
   * return new H5headerNew(raf, ncfile, this);
   * }
   * 
   * return super.sendIospMessage(message);
   * }
   */

}
