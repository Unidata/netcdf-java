/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.netcdf3;

import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ucar.array.ArrayType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.UnlimitedDimension;
import ucar.unidata.io.RandomAccessFile;

/** Class to write a netcdf3 header. */
class N3headerWriter extends N3header {
  private static final long MAX_UNSIGNED_INT = 0x00000000ffffffffL;

  private Group rootGroup;
  private long globalAttsPos; // global attributes start here - used for update
  private UnlimitedDimension unlimitedDim; // the unlimited dimension

  /**
   * Constructor.
   * 
   * @param n3iospNew the iosp
   * @param raf write to this file
   */
  N3headerWriter(N3iosp n3iospNew, RandomAccessFile raf) {
    super(n3iospNew);
    this.raf = raf;
  }

  void initFromExisting(N3iosp existingIosp, Group.Builder rootb) {
    N3header existingHeader = existingIosp.header;
    this.dataStart = existingHeader.dataStart;
    this.nonRecordDataSize = existingHeader.nonRecordDataSize;
    this.numrecs = existingHeader.numrecs;
    this.recsize = existingHeader.recsize;
    this.recStart = existingHeader.recStart;
    this.useLongOffset = existingHeader.useLongOffset;

    if (existingHeader.udim != null) {
      this.unlimitedDim = new UnlimitedDimension(existingHeader.udim.getShortName(), existingHeader.udim.getLength());
      this.udim = this.unlimitedDim;
      // replace Group's dimensions with unlimitedDim
      if (!rootb.replaceDimension(this.unlimitedDim)) {
        throw new IllegalStateException();
      }
      // replace Variable's dimensions with unlimitedDim
      for (Variable.Builder<?> vb : rootb.vbuilders) {
        if (vb.isUnlimited()) {
          if (!vb.replaceDimensionByName(this.unlimitedDim)) {
            throw new IllegalStateException();
          }
        }
      }
    }
  }

  /**
   * Write the header out, based on ncfile structures.
   *
   * @param extra if > 0, pad header with extra bytes
   * @param largeFile if large file format
   * @throws IOException on write error
   */
  void create(Group.Builder rootGroup, int extra, boolean largeFile) throws IOException {
    writeHeader(rootGroup, extra, largeFile, false);
  }

  void setRootGroup(Group rootGroup) {
    this.rootGroup = rootGroup;
  }

  /**
   * Sneaky way to make the header bigger, if there is room for it
   *
   * @param largeFile is large file format
   * @return true if it worked
   */
  boolean rewriteHeader(boolean largeFile) {
    int want = sizeHeader(largeFile);
    if (want > dataStart)
      return false;

    // writeHeader(0, largeFile, true, fout);
    return true;
  }

  void writeHeader(Group.Builder rootGroup, int extra, boolean largeFile, boolean keepDataStart) throws IOException {
    this.useLongOffset = largeFile;
    this.nonRecordDataSize = 0; // length of non-record data
    recsize = 0; // length of single record
    recStart = Long.MAX_VALUE; // where the record data starts

    // magic number
    raf.seek(0);
    raf.write(largeFile ? N3header.MAGIC_LONG : N3header.MAGIC);

    // numrecs
    raf.writeInt(0);

    // dims
    Iterable<Dimension> dims = rootGroup.getDimensions();
    int numdims = Iterables.size(dims);
    if (numdims == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(N3header.MAGIC_DIM);
      raf.writeInt(numdims);
    }
    for (Dimension dim : dims) {
      writeString(dim.getShortName());
      raf.writeInt(dim.isUnlimited() ? 0 : dim.getLength());
      if (dim.isUnlimited()) {
        udim = dim; // needed?
        unlimitedDim = dim instanceof UnlimitedDimension ? (UnlimitedDimension) dim
            : new UnlimitedDimension(dim.getShortName(), dim.getLength());
      }
    }

    // global attributes
    globalAttsPos = raf.getFilePointer(); // position where global attributes start
    writeAtts(rootGroup.getAttributeContainer());

    //// variables

    // Track record variables.
    ArrayList<Variable.Builder<?>> uvarb = new ArrayList<>();
    for (Variable.Builder<?> curVar : rootGroup.vbuilders) {
      if (curVar.isUnlimited()) {
        uvarb.add(curVar);
      }
    }
    writeVars(rootGroup, uvarb, largeFile);

    // now calculate where things go
    if (!keepDataStart) {
      dataStart = raf.getFilePointer();
      if (extra > 0)
        dataStart += extra;
    }
    long pos = dataStart;

    // non-record variable starting positions
    for (Variable.Builder<?> var : rootGroup.vbuilders) {
      N3header.Vinfo vinfo = (N3header.Vinfo) var.spiObject;
      if (!vinfo.isRecord) {
        raf.seek(vinfo.begin);

        if (largeFile) {
          raf.writeLong(pos);
        } else {
          if (pos > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Variable starting pos=" + pos + " may not exceed " + Integer.MAX_VALUE);
          raf.writeInt((int) pos);
        }

        vinfo.begin = pos;
        pos += vinfo.vsize;

        // track how big each record is
        nonRecordDataSize = Math.max(nonRecordDataSize, vinfo.begin + vinfo.vsize);
      }
    }

    recStart = pos; // record variables start here

    // record variable starting positions
    for (Variable.Builder<?> var : rootGroup.vbuilders) {
      N3header.Vinfo vinfo = (N3header.Vinfo) var.spiObject;
      if (vinfo.isRecord) {
        raf.seek(vinfo.begin);

        if (largeFile)
          raf.writeLong(pos);
        else
          raf.writeInt((int) pos);

        vinfo.begin = pos;
        pos += vinfo.vsize;

        // track how big each record is
        recsize += vinfo.vsize;
        recStart = Math.min(recStart, vinfo.begin);
      }
    }

    if (nonRecordDataSize > 0) // if there are non-record variables
      nonRecordDataSize -= dataStart;
    if (uvarb.isEmpty()) // if there are no record variables
      recStart = 0;
  }

  // calculate the size writing a header would take
  int sizeHeader(boolean largeFile) {
    int size = 4; // magic number
    size += 4; // numrecs

    // dims
    size += 8; // magic, ndims
    for (Dimension dim : rootGroup.getDimensions()) {
      size += sizeString(dim.getShortName()) + 4; // name, len
    }

    // global attributes
    size += sizeAtts(rootGroup.attributes());

    // variables
    size += 8; // magic, nvars
    for (Variable var : rootGroup.getVariables()) {
      size += sizeString(var.getShortName());

      // dimensions
      size += 4; // ndims
      size += 4 * var.getDimensions().size(); // dim id

      // variable attributes
      size += sizeAtts(var.attributes());

      size += 8; // data type, variable size
      size += (largeFile) ? 8 : 4;
    }

    return size;
  }

  private void writeAtts(Iterable<Attribute> atts) throws IOException {

    int n = Iterables.size(atts);
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_ATT);
      raf.writeInt(n);
    }

    int count = 0;
    for (Attribute att : atts) {
      writeString(att.getShortName());
      int type = getType(att.getArrayType());
      raf.writeInt(type);

      if (type == 2) {
        writeStringValues(att);
      } else {
        int nelems = att.getLength();
        raf.writeInt(nelems);
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += writeAttributeValue(att.getNumericValue(j));
        pad(nbytes, (byte) 0);
      }
    }
  }

  private int sizeAtts(Iterable<Attribute> atts) {
    int size = 8; // magic, natts

    for (Attribute att : atts) {
      size += sizeString(att.getShortName());
      size += 4; // type

      int type = getType(att.getArrayType());
      if (type == 2) {
        size += sizeStringValues(att);
      } else {
        size += 4; // nelems
        int nelems = att.getLength();
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += sizeAttributeValue(att.getNumericValue(j));
        size += nbytes;
        size += padding(nbytes);
      }
    }
    return size;
  }

  private void writeStringValues(Attribute att) throws IOException {
    int n = att.getLength();
    if (n == 1) {
      writeString(att.getStringValue());
    } else {
      StringBuilder values = new StringBuilder();
      for (int i = 0; i < n; i++)
        values.append(att.getStringValue(i));
      writeString(values.toString());
    }
  }

  private int sizeStringValues(Attribute att) {
    int size = 0;
    int n = att.getLength();
    if (n == 1)
      size += sizeString(att.getStringValue());
    else {
      StringBuilder values = new StringBuilder();
      for (int i = 0; i < n; i++)
        values.append(att.getStringValue(i));
      size += sizeString(values.toString());
    }

    return size;
  }

  private int writeAttributeValue(Number numValue) throws IOException {
    if (numValue instanceof Byte) {
      raf.write(numValue.byteValue());
      return 1;

    } else if (numValue instanceof Short) {
      raf.writeShort(numValue.shortValue());
      return 2;

    } else if (numValue instanceof Integer) {
      raf.writeInt(numValue.intValue());
      return 4;

    } else if (numValue instanceof Float) {
      raf.writeFloat(numValue.floatValue());
      return 4;

    } else if (numValue instanceof Double) {
      raf.writeDouble(numValue.doubleValue());
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == " + numValue.getClass().getName());
  }

  private int sizeAttributeValue(Number numValue) {

    if (numValue instanceof Byte) {
      return 1;

    } else if (numValue instanceof Short) {
      return 2;

    } else if (numValue instanceof Integer) {
      return 4;

    } else if (numValue instanceof Float) {
      return 4;

    } else if (numValue instanceof Double) {
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == " + numValue.getClass().getName());
  }

  private void writeVars(Group.Builder rootGroup, ArrayList<Variable.Builder<?>> uvarb, boolean largeFile)
      throws IOException {
    int n = rootGroup.vbuilders.size();
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_VAR);
      raf.writeInt(n);
    }

    for (Variable.Builder<?> var : rootGroup.vbuilders) {
      writeString(var.shortName);

      // dimensions
      long vsize = var.dataType.getSize(); // works for all netcdf-3 data types
      List<Dimension> dims = var.getDimensions();
      raf.writeInt(dims.size());
      for (Dimension dim : dims) {
        int dimIndex = findDimensionIndex(rootGroup, dim);
        raf.writeInt(dimIndex);

        if (!dim.isUnlimited())
          vsize *= dim.getLength();
      }
      long unpaddedVsize = vsize;
      vsize += padding(vsize);

      // variable attributes
      long varAttsPos = raf.getFilePointer();
      writeAtts(var.getAttributeContainer());

      // data type, variable size, beginning file position
      int type = getType(var.dataType);
      raf.writeInt(type);

      int vsizeWrite = (vsize < MAX_UNSIGNED_INT) ? (int) vsize : -1;
      raf.writeInt(vsizeWrite);
      long pos = raf.getFilePointer();
      if (largeFile) {
        raf.writeLong(0); // come back to this later
      } else {
        raf.writeInt(0); // come back to this later
      }

      // From nc3 file format specification
      // (https://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#NetCDF-Classic-Format):
      // Note on padding: In the special case of only a single record variable of character,
      // byte, or short type, no padding is used between data values.
      // 2/15/2011: we will continue to write the (incorrect) padded vsize into the header, but we will use the unpadded
      // size to read/write
      if (uvarb.size() == 1 && uvarb.get(0) == var) {
        if ((var.dataType == ArrayType.CHAR) || (var.dataType == ArrayType.BYTE) || (var.dataType == ArrayType.SHORT)) {
          vsize = unpaddedVsize;
        }
      }
      var.setSPobject(new N3header.Vinfo(var.shortName, vsize, pos, var.isUnlimited(), varAttsPos));
    }
  }

  // write a string then pad to 4 byte boundary
  private void writeString(String s) throws IOException {
    byte[] b = s.getBytes(StandardCharsets.UTF_8); // all strings are encoded in UTF-8 Unicode.
    raf.writeInt(b.length);
    raf.write(b);
    pad(b.length, (byte) 0);
  }

  private int sizeString(String s) {
    int size = s.length() + 4;
    return size + padding(s.length());
  }

  private int findDimensionIndex(Group.Builder rootGroup, Dimension wantDim) {
    int count = 0;
    for (Dimension dim : rootGroup.getDimensions()) {
      if (dim.equals(wantDim))
        return count;
      count++;
    }
    throw new IllegalStateException("unknown Dimension == " + wantDim);
  }

  // pad to a 4 byte boundary
  private void pad(int nbytes, byte fill) throws IOException {
    int pad = padding(nbytes);
    for (int i = 0; i < pad; i++)
      raf.write(fill);
  }

  void writeNumrecs() throws IOException {
    // set number of records in the header
    raf.seek(4);
    raf.writeInt(numrecs);
  }

  void setNumrecs(int n) {
    this.numrecs = n;
    this.unlimitedDim.setLength(n);
  }

  ///////////////////////////////////////////////////////////////////////////////
  // tricky - perhaps deprecate ?

  void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException {
    long pos;
    if (v2 == null)
      pos = findAttPos(globalAttsPos, att.getShortName());
    else {
      N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
      pos = findAttPos(vinfo.attsPos, att.getShortName());
    }

    raf.seek(pos);
    int type = raf.readInt();
    ArrayType have = getDataType(type);
    ArrayType want = att.getArrayType();
    if (want == ArrayType.STRING) {
      want = ArrayType.CHAR;
    }
    if (want != have) {
      throw new IllegalArgumentException(
          "Update Attribute must have same type or original = " + have + " att = " + att);
    }

    if (type == 2) { // String
      String s = att.getStringValue();
      int org = raf.readInt();
      int size = org + padding(org); // ok to use the padding
      int max = Math.min(size, s.length()); // cant make any longer than size
      if (max > org) { // adjust if its using the padding, but not if its shorter
        raf.seek(pos + 4);
        raf.writeInt(max);
      }

      byte[] b = new byte[size];
      for (int i = 0; i < max; i++)
        b[i] = (byte) s.charAt(i);
      raf.write(b);

    } else {
      int nelems = raf.readInt();
      int max = Math.min(nelems, att.getLength()); // cant make any longer
      for (int j = 0; j < max; j++)
        writeAttributeValue(att.getNumericValue(j));
    }
  }

  // just skipping forward to find the named attribute's file position (after the name)
  private long findAttPos(long start_pos, String want) throws IOException {
    raf.seek(start_pos + 4);

    int natts = raf.readInt();
    for (int i = 0; i < natts; i++) {
      String name = readString();
      if (name.equals(want)) {
        return raf.getFilePointer();
      }

      int type = raf.readInt();
      int nelems = raf.readInt();
      ArrayType dtype = getDataType(type);
      int nbytes = nelems * dtype.getSize();
      raf.skipBytes(nbytes);
      skip(nbytes); // 4 byte boundary
    }

    throw new IllegalArgumentException("no such attribute " + want);
  }

}
