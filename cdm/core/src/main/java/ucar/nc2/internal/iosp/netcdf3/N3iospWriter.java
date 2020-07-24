/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.netcdf3;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayStructure;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.IOServiceProviderWriter;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutRegular;
import ucar.nc2.iosp.LayoutRegularSegmented;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

/** IOServiceProviderWriter for Netcdf3 files. */
public class N3iospWriter extends N3iospNew implements IOServiceProviderWriter {

  private boolean fill = true;
  private IOServiceProvider iosp = null;

  public N3iospWriter(IOServiceProvider iosp) {
    this.iosp = iosp;
  }

  @Override
  public void openForWriting(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) {
    // Cant call superclass open, so some duplicate code here
    this.raf = raf;
    this.location = (raf != null) ? raf.getLocation() : null;
    this.ncfile = ncfile;

    if (location != null && !location.startsWith("http:")) {
      File file = new File(location);
      if (file.exists())
        lastModified = file.lastModified();
    }

    raf.order(RandomAccessFile.BIG_ENDIAN);
    N3headerWriter headerw = new N3headerWriter(this, raf, ncfile);
    headerw.initFromExisting((N3iospNew) this.iosp); // hack-a-whack
    this.header = headerw;
  }

  @Override
  public void setFill(boolean fill) {
    this.fill = fill;
  }

  @Override
  public void create(String filename, ucar.nc2.NetcdfFile ncfile, int extra, long preallocateSize, boolean largeFile)
      throws IOException {
    this.ncfile = ncfile;

    // finish any structures
    ncfile.finish();

    raf = new ucar.unidata.io.RandomAccessFile(filename, "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);

    if (preallocateSize > 0) {
      java.io.RandomAccessFile myRaf = raf.getRandomAccessFile();
      myRaf.setLength(preallocateSize);
    }

    N3headerWriter headerw = new N3headerWriter(this, raf, ncfile);
    headerw.create(extra, largeFile, null);
    this.header = headerw;

    if (fill)
      fillNonRecordVariables();
    // else
    // raf.setMinLength(recStart); // make sure file length is long enough, even if not written to.
  }

  @Override
  public boolean rewriteHeader(boolean largeFile) throws IOException {
    return ((N3headerWriter) header).rewriteHeader(largeFile, null);
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // write

  @Override
  public void writeData(Variable v2, Section section, Array values) throws java.io.IOException, InvalidRangeException {
    N3headerNew.Vinfo vinfo = (N3headerNew.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    if (v2.isUnlimited()) {
      Range firstRange = section.getRange(0);
      setNumrecs(firstRange.last() + 1);
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
      Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), v2.getShape(), section)
          : new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, v2.getShape(), section);
      writeData(values, layout, dataType);
    }
  }

  @Override
  public int appendStructureData(Structure s, StructureData sdata) throws IOException, InvalidRangeException {
    int recnum = header.numrecs;
    setNumrecs(recnum + 1);
    writeRecordData(s, recnum, sdata);
    return recnum;
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
      if (data instanceof ArrayObject && vm.getDataType() == DataType.CHAR && vm.getRank() > 0) {
        int strlen = vm.getShape(vm.getRank() - 1);
        data = ArrayChar.makeFromStringArray((ArrayObject) data, strlen); // turn it into an ArrayChar
      }

      // layout of the destination
      N3headerNew.Vinfo vinfo = (N3headerNew.Vinfo) vm.getSPobject();
      long begin = vinfo.begin + recnum * header.recsize; // this assumes unlimited dimension
      Section memberSection = vm.getShapeAsSection();
      Layout layout = new LayoutRegular(begin, vm.getElementSize(), vm.getShape(), memberSection);

      try {
        writeData(data, layout, vm.getDataType());
      } catch (Exception e) {
        log.error("Error writing member=" + vm.getShortName() + " in struct=" + s.getFullName(), e);
        throw new IOException(e);
      }
    }
  }

  /**
   * write data to a file for a variable.
   *
   * @param values write this data.
   * @param index handles skipping around in the file.
   * @param dataType dataType of the variable
   */
  private void writeData(Array values, Layout index, DataType dataType) throws java.io.IOException {
    if ((dataType == DataType.BYTE) || (dataType == DataType.CHAR)) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++)
          raf.write(ii.getByteNext());
      }
      return;

    } else if (dataType == DataType.STRING) { // LOOK not legal
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

    } else if (dataType == DataType.SHORT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++)
          raf.writeShort(ii.getShortNext());
      }
      return;

    } else if (dataType == DataType.INT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++)
          raf.writeInt(ii.getIntNext());
      }
      return;

    } else if (dataType == DataType.FLOAT) {
      IndexIterator ii = values.getIndexIterator();
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek(chunk.getSrcPos());
        for (int k = 0; k < chunk.getNelems(); k++)
          raf.writeFloat(ii.getFloatNext());
      }
      return;

    } else if (dataType == DataType.DOUBLE) {
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

  private void setNumrecs(int n) throws IOException, InvalidRangeException {
    if (n <= header.numrecs)
      return;
    int startRec = header.numrecs;

    // fileUsed = recStart + recsize * n;
    ((N3headerWriter) header).setNumrecs(n);
    // this.numrecs = n;

    // TODO udim.setLength : need UnlimitedDimension extends Dimension?
    // need to let unlimited dimension know of new shape
    for (Dimension dim : ncfile.getRootGroup().getDimensions()) {
      if (dim.isUnlimited())
        dim.setLength(n);
    }

    // need to let all unlimited variables know of new shape TODO immutable??
    for (Variable v : ncfile.getVariables()) {
      if (v.isUnlimited()) {
        v.resetShape();
        v.setCachedData(null, false);
      }
    }

    // extend file, handle filling
    if (fill)
      fillRecordVariables(startRec, n);
    else
      raf.setMinLength(header.calcFileSize());
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
  public void flush() throws java.io.IOException {
    if (raf != null) {
      raf.flush();
      ((N3headerWriter) header).writeNumrecs();
      raf.flush();
    }
  }

  /////////////////////////////////////////////////////////////

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
    Class classType = v.getDataType().getPrimitiveClassType();
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

  //////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean syncExtend() throws IOException {
    boolean result = ((N3headerWriter) header).synchNumrecs();
    if (result && log.isDebugEnabled())
      log.debug(" N3iosp syncExtend " + raf.getLocation() + " numrecs =" + header.numrecs);
    return result;
  }
}
