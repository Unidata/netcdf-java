/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.netcdf3;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import ucar.array.ArrayType;
import ucar.array.ArraysConvert;
import ucar.array.StructureDataArray;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayStructure;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.iosp.IospFileWriter;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutRegular;
import ucar.nc2.iosp.LayoutRegularSegmented;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

/** IOServiceProviderWriter for Netcdf3 files. */
public class N3iospWriter extends N3iosp implements IospFileWriter {
  private boolean fill = true;
  private final IOServiceProvider iosp;
  private N3headerWriter headerw;

  public N3iospWriter() {
    this.iosp = null;
  }

  // open existing only.
  public N3iospWriter(IOServiceProvider iosp) {
    this.iosp = iosp;
  }

  @Override
  public NetcdfFile create(String filename, Group.Builder rootGroup, int extra, long preallocateSize, boolean largeFile)
      throws IOException {

    raf = new ucar.unidata.io.RandomAccessFile(filename, "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);

    if (preallocateSize > 0) {
      java.io.RandomAccessFile myRaf = raf.getRandomAccessFile();
      myRaf.setLength(preallocateSize);
    }

    this.headerw = new N3headerWriter(this, raf);
    // The rootGroup is modified to be specific to the output file.
    headerw.create(rootGroup, extra, largeFile);
    this.header = headerw;

    NetcdfFile.Builder<?> ncfileb = NetcdfFile.builder().setRootGroup(rootGroup).setLocation(filename);
    this.ncfile = ncfileb.build();
    headerw.setRootGroup(this.ncfile.getRootGroup());

    if (fill) {
      fillNonRecordVariables();
    }
    // else
    // raf.setMinLength(recStart); // make sure file length is long enough, even if not written to.

    return this.ncfile;
  }

  // LOOK
  @Override
  public void openForWriting(String location, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {

    this.location = location;
    this.raf = new ucar.unidata.io.RandomAccessFile(location, "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);

    if (location != null && !location.startsWith("http:")) {
      File file = new File(location);
      if (file.exists())
        lastModified = file.lastModified();
    }

    this.headerw = new N3headerWriter(this, raf);
    headerw.initFromExisting((N3iosp) this.iosp, rootGroup); // hack-a-whack
    this.header = headerw;

    NetcdfFile.Builder<?> ncfileb = NetcdfFile.builder().setRootGroup(rootGroup).setLocation(location);
    this.ncfile = ncfileb.build();
    headerw.setRootGroup(this.ncfile.getRootGroup());
  }

  @Override
  public NetcdfFile getOutputFile() {
    return this.ncfile;
  }

  @Override
  public void setFill(boolean fill) {
    this.fill = fill;
  }

  public boolean rewriteHeader(boolean largeFile) throws IOException {
    return ((N3headerWriter) header).rewriteHeader(largeFile);
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // write

  public void writeData(Variable v2, Section section, Array values) throws java.io.IOException, InvalidRangeException {
    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    ArrayType dataType = v2.getArrayType();

    int[] varShape = v2.getShape();
    if (v2.isUnlimited()) {
      Range firstRange = section.getRange(0);
      int n = setNumrecs(firstRange.last() + 1);
      varShape[0] = n;
    }

    if (v2 instanceof Structure) {
      if (!(values instanceof ArrayStructure))
        throw new IllegalArgumentException("writeData for Structure: data must be ArrayStructure");

      if (v2.getRank() == 0)
        throw new IllegalArgumentException("writeData for Structure: must have rank > 0");

      Dimension d = v2.getDimension(0);
      if (!d.isUnlimited())
        throw new IllegalArgumentException("writeData for Structure: must have unlimited dimension");

      writeRecordData((Structure) v2, section, (ArrayStructure) values);

    } else {
      Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), varShape, section)
          : new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, varShape, section);
      writeData(values, layout, dataType);
    }
  }

  private void writeRecordData(ucar.nc2.Structure s, Section section, ArrayStructure structureArray)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {
    int countSrcRecnum = 0;
    Range recordRange = section.getRange(0);
    for (int recnum : recordRange) {
      StructureData sdata = structureArray.getStructureData(countSrcRecnum);
      writeRecordData(s, recnum, sdata);
      countSrcRecnum++;
    }
  }

  private void writeRecordData(ucar.nc2.Structure s, int recnum, StructureData sdata)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    StructureMembers members = sdata.getStructureMembers();

    // loop over members
    for (Variable vm : s.getVariables()) {
      StructureMembers.Member m = members.findMember(vm.getShortName());
      if (null == m)
        continue; // this means that the data is missing from the ArrayStructure

      // convert String member data into CHAR data
      Array data = sdata.getArray(m);
      if (data instanceof ArrayObject && vm.getArrayType() == ArrayType.CHAR && vm.getRank() > 0) {
        int strlen = vm.getShape(vm.getRank() - 1);
        data = ArrayChar.makeFromStringArray((ArrayObject) data, strlen); // turn it into an ArrayChar
      }

      // layout of the destination
      N3header.Vinfo vinfo = (N3header.Vinfo) vm.getSPobject();
      long begin = vinfo.begin + recnum * header.recsize; // this assumes unlimited dimension
      Section memberSection = vm.getShapeAsSection();
      Layout layout = new LayoutRegular(begin, vm.getElementSize(), vm.getShape(), memberSection);

      try {
        writeData(data, layout, vm.getArrayType());
      } catch (Exception e) {
        log.error("Error writing member=" + vm.getShortName() + " in struct=" + s.getFullName(), e);
        throw new IOException(e);
      }
    }
  }

  private void writeData(Array values, Layout index, ArrayType dataType) throws java.io.IOException {
    if ((dataType == ArrayType.BYTE) || (dataType == ArrayType.CHAR)) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++)
          raf.write(ii.getByteNext());
      }
      return;

    } else if (dataType == ArrayType.STRING) { // LOOK not legal
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++) {
          String val = (String) ii.getObjectNext();
          if (val != null)
            raf.write(val.getBytes(StandardCharsets.UTF_8)); // LOOK ??
        }
      }
      return;

    } else if (dataType == ArrayType.SHORT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++)
          raf.writeShort(ii.getShortNext());
      }
      return;

    } else if (dataType == ArrayType.INT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++)
          raf.writeInt(ii.getIntNext());
      }
      return;

    } else if (dataType == ArrayType.FLOAT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++)
          raf.writeFloat(ii.getFloatNext());
      }
      return;

    } else if (dataType == ArrayType.DOUBLE) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++)
          raf.writeDouble(ii.getDoubleNext());
      }
      return;
    }

    throw new IllegalStateException("dataType= " + dataType);
  }

  /////////////////////////////////////////////////////////////

  @Override
  public void writeData(Variable v2, ucar.array.Section section, ucar.array.Array<?> values)
      throws IOException, ucar.array.InvalidRangeException {
    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    ArrayType dataType = v2.getArrayType();

    try {
      int[] varShape = v2.getShape();
      if (v2.isUnlimited()) {
        ucar.array.Range firstRange = section.getRange(0);
        int n = setNumrecs(firstRange.last() + 1);
        varShape[0] = n;
      }

      if (v2 instanceof Structure) {
        if (!(values instanceof StructureDataArray))
          throw new IllegalArgumentException("writeData for Structure: data must be ArrayStructure");

        if (v2.getRank() == 0)
          throw new IllegalArgumentException("writeData for Structure: must have rank > 0");

        Dimension d = v2.getDimension(0);
        if (!d.isUnlimited())
          throw new IllegalArgumentException("writeData for Structure: must have unlimited dimension");

        writeRecordArrayData((Structure) v2, section, (StructureDataArray) values);

      } else {
        ucar.ma2.Section oldSection = ArraysConvert.convertSection(section);

        Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), varShape, oldSection)
            : new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, varShape, oldSection);
        writeArrayData(values, layout, dataType);
      }
    } catch (InvalidRangeException range) {
      throw new ucar.array.InvalidRangeException(range);
    }
  }

  private void writeRecordArrayData(ucar.nc2.Structure s, ucar.array.Section section, StructureDataArray structureArray)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {
    int countSrcRecnum = 0;
    ucar.array.Range recordRange = section.getRange(0);
    for (int recnum : recordRange) {
      ucar.array.StructureData sdata = structureArray.get(countSrcRecnum);
      writeRecordArrayData(s, recnum, sdata);
      countSrcRecnum++;
    }
  }

  private void writeRecordArrayData(ucar.nc2.Structure s, int recnum, ucar.array.StructureData sdata)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    ucar.array.StructureMembers members = sdata.getStructureMembers();

    // loop over members
    for (Variable vm : s.getVariables()) {
      ucar.array.StructureMembers.Member m = members.findMember(vm.getShortName());
      if (null == m)
        continue; // this means that the data is missing from the ArrayStructure

      ucar.array.Array data = sdata.getMemberData(m);

      // layout of the destination
      N3header.Vinfo vinfo = (N3header.Vinfo) vm.getSPobject();
      long begin = vinfo.begin + recnum * header.recsize; // this assumes unlimited dimension
      Section memberSection = vm.getShapeAsSection();
      Layout layout = new LayoutRegular(begin, vm.getElementSize(), vm.getShape(), memberSection);

      try {
        writeArrayData(data, layout, vm.getArrayType());
      } catch (Exception e) {
        log.error("Error writing member=" + vm.getShortName() + " in struct=" + s.getFullName(), e);
        throw new IOException(e);
      }
    }
  }

  private void writeArrayData(ucar.array.Array<?> values, Layout index, ArrayType dataType) throws java.io.IOException {
    switch (dataType) {
      case BYTE:
      case CHAR: {
        ucar.array.Array<Byte> bvalues = (ucar.array.Array<Byte>) values;
        Iterator<Byte> ii = bvalues.iterator();
        while (index.hasNext()) {
          Layout.Chunk chunk = index.next();
          raf.seek(chunk.getSrcPos());
          for (int k = 0; k < chunk.getNelems(); k++) {
            raf.write(ii.next());
          }
        }
        return;
      }

      case SHORT: {
        ucar.array.Array<Short> bvalues = (ucar.array.Array<Short>) values;
        Iterator<Short> ii = bvalues.iterator();
        while (index.hasNext()) {
          Layout.Chunk chunk = index.next();
          raf.seek(chunk.getSrcPos());
          for (int k = 0; k < chunk.getNelems(); k++) {
            raf.writeShort(ii.next());
          }
        }
        return;
      }

      case INT: {
        ucar.array.Array<Integer> bvalues = (ucar.array.Array<Integer>) values;
        Iterator<Integer> ii = bvalues.iterator();
        while (index.hasNext()) {
          Layout.Chunk chunk = index.next();
          raf.seek(chunk.getSrcPos());
          for (int k = 0; k < chunk.getNelems(); k++) {
            raf.writeInt(ii.next());
          }
        }
        return;
      }

      case FLOAT: {
        ucar.array.Array<Float> bvalues = (ucar.array.Array<Float>) values;
        Iterator<Float> ii = bvalues.iterator();
        while (index.hasNext()) {
          Layout.Chunk chunk = index.next();
          raf.seek(chunk.getSrcPos());
          for (int k = 0; k < chunk.getNelems(); k++) {
            raf.writeFloat(ii.next());
          }
        }
        return;
      }

      case DOUBLE: {
        ucar.array.Array<Double> bvalues = (ucar.array.Array<Double>) values;
        Iterator<Double> ii = bvalues.iterator();
        while (index.hasNext()) {
          Layout.Chunk chunk = index.next();
          raf.seek(chunk.getSrcPos());
          for (int k = 0; k < chunk.getNelems(); k++) {
            raf.writeDouble(ii.next());
          }
        }
        return;
      }
    }

    throw new IllegalStateException("dataType= " + dataType);
  }

  @Override
  public int appendStructureData(Structure s, ucar.array.StructureData sdata)
      throws IOException, ucar.array.InvalidRangeException {
    return 0;
  }

  /////////////////////////////////////////////////////////////

  private int setNumrecs(int n) throws IOException, InvalidRangeException {
    if (n <= header.numrecs) {
      return header.numrecs;
    }
    int startRec = header.numrecs;

    ((N3headerWriter) header).setNumrecs(n);

    // need to let all unlimited variables know of new shape
    for (Variable v : ncfile.getVariables()) {
      if (v.isUnlimited()) {
        v.resetShape(); // LOOK this doesnt work.
        v.invalidateCache();
      }
    }

    // extend file, handle filling
    if (fill) {
      fillRecordVariables(startRec, n);
    } else {
      raf.setMinLength(header.calcFileSize());
    }

    return n;
  }

  /**
   * Update the value of an existing attribute on disk, not in memory. Attribute is found by name, which must match
   * exactly.
   * You cannot make an attribute longer, or change the number of values.
   * For strings: truncate if longer, zero fill if shorter. Strings are padded to 4 byte boundaries, ok to use padding
   * if it exists.
   * For numerics: must have same number of values.
   *
   * @param v2 variable, or null for global attribute
   * @param att replace with this value
   */
  @Override
  public void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException {
    ((N3headerWriter) header).updateAttribute(v2, att);
  }

  @Override
  public void updateAttribute(Group g, Attribute att) throws IOException {
    // LOOK
  }

  public void flush() throws java.io.IOException {
    if (raf != null) {
      raf.flush();
      if (header != null) {
        ((N3headerWriter) header).writeNumrecs();
        raf.flush();
      }
    }
  }

  @Override
  public void close() throws java.io.IOException {
    flush();
    super.close();
  }


  // fill buffer with fill value

  private void fillNonRecordVariables() throws IOException {
    // run through each variable
    for (Variable v : ncfile.getVariables()) {
      if (v.isUnlimited())
        continue;
      try {
        writeData(v, v.getShapeAsSection(), makeConstantArray(v));
      } catch (InvalidRangeException e) {
        e.printStackTrace(); // shouldnt happen
      }
    }
  }

  private void fillRecordVariables(int recStart, int recEnd) throws IOException, InvalidRangeException {
    // do each record completely, should be a bit more efficient

    for (int i = recStart; i < recEnd; i++) { // do one record at a time
      Range r = new Range(i, i);

      // run through each variable
      for (Variable v : ncfile.getVariables()) {
        if (!v.isUnlimited() || (v instanceof Structure))
          continue;
        Section.Builder recordSection = Section.builder().appendRanges(v.getRanges());
        recordSection.setRange(0, r);
        writeData(v, recordSection.build(), makeConstantArray(v));
      }
    }
  }

  private Array makeConstantArray(Variable v) {
    Class<?> classType = v.getDataType().getPrimitiveClassType();
    // int [] shape = v.getShape();
    Attribute att = v.findAttribute(CDM.FILL_VALUE);

    Object storage = null;
    if (classType == double.class) {
      double[] storageP = new double[1];
      storageP[0] = (att == null) ? NetcdfFormatUtils.NC_FILL_DOUBLE : att.getNumericValue().doubleValue();
      storage = storageP;

    } else if (classType == float.class) {
      float[] storageP = new float[1];
      storageP[0] = (att == null) ? NetcdfFormatUtils.NC_FILL_FLOAT : att.getNumericValue().floatValue();
      storage = storageP;

    } else if (classType == int.class) {
      int[] storageP = new int[1];
      storageP[0] = (att == null) ? NetcdfFormatUtils.NC_FILL_INT : att.getNumericValue().intValue();
      storage = storageP;

    } else if (classType == short.class) {
      short[] storageP = new short[1];
      storageP[0] = (att == null) ? NetcdfFormatUtils.NC_FILL_SHORT : att.getNumericValue().shortValue();
      storage = storageP;

    } else if (classType == byte.class) {
      byte[] storageP = new byte[1];
      storageP[0] = (att == null) ? NetcdfFormatUtils.NC_FILL_BYTE : att.getNumericValue().byteValue();
      storage = storageP;

    } else if (classType == char.class) {
      char[] storageP = new char[1];
      storageP[0] = (att != null) && (!att.getStringValue().isEmpty()) ? att.getStringValue().charAt(0)
          : NetcdfFormatUtils.NC_FILL_CHAR;
      storage = storageP;
    }

    return Array.factoryConstant(v.getDataType(), v.getShape(), storage);
  }

}
