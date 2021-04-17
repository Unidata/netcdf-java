/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.base.Preconditions;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.ArraysConvert;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.internal.iosp.IospFileWriter;
import ucar.nc2.internal.iosp.hdf5.H5iosp;
import ucar.nc2.internal.iosp.netcdf3.N3iosp;
import ucar.nc2.internal.iosp.netcdf3.N3iospWriter;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static ucar.nc2.NetcdfFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

/**
 * Writes new Netcdf 3 or 4 formatted files to disk.
 *
 * <pre>
 * NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
 * writerb.addDimension(Dimension.builder().setName("vdim").setIsUnlimited(true).build());
 * writerb.addVariable("v", DataType.BYTE, "vdim");
 * try (NetcdfFormatWriter writer = writerb.build()) {
 *   writer.write("v", dataArray);
 * }
 * </pre>
 * 
 * LOOK consider making netcdf3 and netcdf4 seperate, so they can have different functionality.
 */
public class NetcdfFormatWriter implements Closeable {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFormatWriter.class);

  /**
   * Create a new Netcdf3 file.
   *
   * @param location name of new file to open; if it exists, will overwrite it.
   */
  public static Builder<?> createNewNetcdf3(String location) {
    return builder().setFormat(NetcdfFileFormat.NETCDF3).setLocation(location);
  }

  /**
   * Create a new NetcdfFileFormat.NETCDF4 file, with default chunker.
   *
   * @param location name of new file to open; if it exists, will overwrite it.
   */
  public static Builder<?> createNewNetcdf4(String location) {
    return builder().setFormat(NetcdfFileFormat.NETCDF4).setLocation(location).setChunker(new Nc4ChunkingDefault());
  }

  /**
   * Create a new Netcdf4 file.
   *
   * @param format One of the netcdf-4 NetcdfFileFormat.
   * @param location name of new file to open; if it exists, will overwrite it.
   * @param chunker used only for netcdf4, or null for default chunking algorithm
   */
  public static Builder<?> createNewNetcdf4(NetcdfFileFormat format, String location, @Nullable Nc4Chunking chunker) {
    return builder().setFormat(format).setLocation(location).setChunker(chunker);
  }

  /**
   * Open an existing Netcdf format file for writing data.
   * Cannot add new objects, you can only read/write data to existing Variables.
   * TODO: allow changes to Netcdf-4 format.
   *
   * @param location name of existing NetCDF file to open.
   * @return existing file that can be written to
   */
  public static Builder<?> openExisting(String location) throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(location)) {
      IOServiceProvider iosp = (IOServiceProvider) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_GET_IOSP);
      Preconditions.checkArgument(
          iosp instanceof N3iosp || iosp instanceof H5iosp || iosp.getClass().getName().endsWith("Nc4reader"),
          "Can only modify Netcdf-3 or Netcdf-4 files");
      Group.Builder root = ncfile.getRootGroup().toBuilder();
      NetcdfFileFormat format = (NetcdfFileFormat) iosp.sendIospMessage(IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT);
      if (format == null && !format.isNetdf3format() && !format.isNetdf4format()) {
        throw new IllegalArgumentException(String.format("%s is not a netcdf-3 or netcdf-4 file", format));
      }
      return builder().setRootGroup(root).setLocation(location).setIosp(iosp).setFormat(format).setIsExisting();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** The output file root group. */
  public Group getRootGroup() {
    return ncout.getRootGroup();
  }

  public NetcdfFileFormat getFormat() {
    return this.format;
  }

  @Nullable
  public Variable findVariable(String fullNameEscaped) {
    return ncout.findVariable(fullNameEscaped);
  }

  @Nullable
  public Dimension findDimension(String dimName) {
    return ncout.findDimension(dimName);
  }

  @Nullable
  public Attribute findGlobalAttribute(String attName) {
    return getRootGroup().findAttribute(attName);
  }

  public long calcSize() {
    return calcSize(getRootGroup());
  }

  // Note that we have enough info to try to estimate effects of compression, if its a Netcdf4 file.
  private long calcSize(Group group) {
    long totalSizeOfVars = 0;
    for (Variable var : group.getVariables()) {
      totalSizeOfVars += Dimensions.getSize(var.getDimensions()) * var.getElementSize();
    }
    for (Group nested : group.getGroups()) {
      totalSizeOfVars += calcSize(nested);
    }
    return totalSizeOfVars;
  }

  ////////////////////////////////////////////
  // use these calls to write data to the file

  /**
   * Write to Variable with an ucar.array.Array.
   * 
   * @param v variable to write to
   * @param origin offset within the variable to start writing.
   * @param values write this array; must be same type and rank as Variable
   */
  public void write(Variable v, int[] origin, ucar.array.Array<?> values)
      throws IOException, ucar.array.InvalidRangeException {
    ucar.ma2.Array oldArray = ArraysConvert.convertFromArray(values);
    try {
      write(v, origin, oldArray);
    } catch (InvalidRangeException e) {
      throw new ucar.array.InvalidRangeException(e);
    }
  }

  /**
   * Write to Variable with ucar.array.Array, origin assumed 0.
   * 
   * @param v write to this variable
   * @param values write this array; must be same type and rank as Variable
   */
  public void write(Variable v, ucar.array.Array<?> values) throws IOException, ucar.array.InvalidRangeException {
    write(v, new int[values.getRank()], values);
  }

  /**
   * Write to Variable with a primitive java array
   * 
   * @param v write to this variable
   * @param origin offset within the variable to start writing.
   * @param primArray must be java primitive array of type compatible with v.
   * @param shape the data shape, same rank as v, with product equal to primArray. If rank = 0, use v.getShape().
   */
  public void write(Variable v, int[] origin, Object primArray, int... shape)
      throws IOException, ucar.array.InvalidRangeException {
    if (shape.length == 0) {
      shape = v.getShape();
    }
    ucar.array.Array<?> values = Arrays.factory(v.getArrayType(), shape, primArray);
    write(v, origin, values);
  }

  /**
   * Write to Variable with a primitive java array, origin assumed 0.
   * 
   * @param v write to this variable
   * @param primArray must be java primitive array of type compatible with v.
   * @param shape the data shape, same rank as v, with product equal to primArray. If rank = 0, use v.getShape().
   */
  public void write(Variable v, Object primArray, int... shape) throws IOException, ucar.array.InvalidRangeException {
    write(v, new int[v.getRank()], primArray, shape);
  }

  /**
   * Write String values to a CHAR or STRING Variable.
   * 
   * @param v write to this variable, must be of type CHAR or STRING.
   * @param origin offset within the variable to start writing.
   * @param data The String or String[] to write.
   */
  public void writeStringData(Variable v, int[] origin, Object data)
      throws IOException, ucar.array.InvalidRangeException {
    if (v.getArrayType() == ArrayType.STRING) {
      writeStringDataToString(v, origin, data);
    } else if (v.getArrayType() == ArrayType.CHAR) {
      writeStringDataToChar(v, origin, data);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void writeStringData(Variable v, Object data) throws IOException, ucar.array.InvalidRangeException {
    writeStringData(v, new int[v.getRank()], data);
  }

  private void writeStringDataToChar(Variable v, int[] origin, Object data)
      throws IOException, ucar.array.InvalidRangeException {
    String[] sarray;
    if (data instanceof String) {
      sarray = new String[] {(String) data};
    } else if (data instanceof String[]) {
      sarray = (String[]) data;
    } else {
      throw new IllegalArgumentException();
    }

    ucar.ma2.Array cvalues = ucar.ma2.ArrayChar.makeFromStringArray(sarray, v.getShape());
    try {
      write(v, origin, cvalues);
    } catch (InvalidRangeException e) {
      throw new ucar.array.InvalidRangeException(e);
    }
  }

  private void writeStringDataToString(Variable v, int[] origin, Object data)
      throws IOException, ucar.array.InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  public void writeStringData(Variable v, int[] origin, ucar.array.Array<?> values)
      throws IOException, ucar.array.InvalidRangeException {
    ucar.ma2.Array oldArray = ArraysConvert.convertFromArray(values);
    try {
      writeStringDataToChar(v, origin, oldArray);
    } catch (InvalidRangeException e) {
      throw new ucar.array.InvalidRangeException(e);
    }
  }

  public int appendStructureData(Structure s, ucar.array.StructureData sdata)
      throws IOException, ucar.array.InvalidRangeException {
    ucar.ma2.StructureMembers membersMa2 = s.makeStructureMembers(); // ??
    ucar.ma2.StructureData oldStructure = ArraysConvert.convertStructureData(membersMa2, sdata);
    try {
      return appendStructureData(s, oldStructure);
    } catch (InvalidRangeException e) {
      throw new ucar.array.InvalidRangeException(e);
    }
  }


  ////////////////////////////////////////////
  //// deprecated

  /**
   * Write data to the given variable, origin assumed to be 0.
   *
   * @param v variable to write to
   * @param values write this array; must be same type and rank as Variable
   * @throws IOException if I/O error
   * @throws InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#write(Variable, ucar.array.Array)}
   */
  @Deprecated
  public void write(Variable v, ucar.ma2.Array values) throws IOException, InvalidRangeException {
    write(v, new int[values.getRank()], values);
  }

  /**
   * Write data to the named variable, origin assumed to be 0.
   * 
   * @deprecated use {@link NetcdfFormatWriter#write(Variable, ucar.array.Array)}
   */
  @Deprecated
  public void write(String varName, ucar.ma2.Array values) throws IOException, InvalidRangeException {
    Variable v = findVariable(varName);
    Preconditions.checkNotNull(v);
    write(v, values);
  }

  /**
   * Write data to the given variable.
   *
   * @param v variable to write to
   * @param origin offset within the variable to start writing.
   * @param values write this array; must be same type and rank as Variable
   * @throws IOException if I/O error
   * @throws InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#write(Variable, int[], ucar.array.Array)}
   */
  @Deprecated
  public void write(Variable v, int[] origin, ucar.ma2.Array values) throws IOException, InvalidRangeException {
    spiw.writeData(v, new ucar.ma2.Section(origin, values.getShape()), values);
  }

  /**
   * Write data to the named variable.
   *
   * @param varName name of variable to write to
   * @param origin offset within the variable to start writing.
   * @param values write this array; must be same type and rank as Variable
   * @throws IOException if I/O error
   * @throws InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#write(Variable, int[], ucar.array.Array)}
   */
  @Deprecated
  public void write(String varName, int[] origin, ucar.ma2.Array values) throws IOException, InvalidRangeException {
    Variable v = findVariable(varName);
    Preconditions.checkNotNull(v);
    write(v, origin, values);
  }

  /**
   * Write String data to a CHAR variable, origin assumed to be 0.
   *
   * @param v variable to write to
   * @param values write this array; must be ArrayObject of String
   * @throws IOException if I/O error
   * @throws InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#writeStringData(Variable, int[], ucar.array.Array)}
   */
  @Deprecated
  public void writeStringDataToChar(Variable v, ucar.ma2.Array values) throws IOException, InvalidRangeException {
    writeStringDataToChar(v, new int[values.getRank()], values);
  }

  /**
   * Write String data to a CHAR variable.
   *
   * @param v variable to write to
   * @param origin offset to start writing, ignore the strlen dimension.
   * @param values write this array; must be ArrayObject of String
   * @throws IOException if I/O error
   * @throws InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#writeStringData(Variable, int[], ucar.array.Array)}
   */
  @Deprecated
  public void writeStringDataToChar(Variable v, int[] origin, ucar.ma2.Array values)
      throws IOException, InvalidRangeException {
    if (values.getElementType() != String.class)
      throw new IllegalArgumentException("values must be an ArrayObject of String ");

    if (v.getArrayType() != ArrayType.CHAR)
      throw new IllegalArgumentException("variable " + v.getFullName() + " is not type CHAR");

    int rank = v.getRank();
    int strlen = v.getShape(rank - 1);

    // turn it into an ArrayChar
    ucar.ma2.ArrayChar cvalues = ucar.ma2.ArrayChar.makeFromStringArray((ucar.ma2.ArrayObject) values, strlen);

    int[] corigin = new int[rank];
    System.arraycopy(origin, 0, corigin, 0, rank - 1);

    write(v, corigin, cvalues);
  }

  /**
   * @deprecated use {@link NetcdfFormatWriter#appendStructureData(Structure, ucar.array.StructureData)}
   */
  @Deprecated
  public int appendStructureData(Structure s, ucar.ma2.StructureData sdata) throws IOException, InvalidRangeException {
    return spiw.appendStructureData(s, sdata);
  }

  /**
   * Update the value of an existing attribute. Attribute is found by name, which must match exactly.
   * You cannot make an attribute longer, or change the number of values.
   * For strings: truncate if longer, zero fill if shorter. Strings are padded to 4 byte boundaries, ok to use padding
   * if it exists.
   * For numerics: must have same number of values.
   * <p/>
   * This is really a netcdf-3 writing only, in particular supporting point feature writing.
   * netcdf-4 attributes can be changed without rewriting.
   *
   * @param v2 variable, or null for global attribute
   * @param att replace with this value
   * @throws IOException if I/O error
   */
  public void updateAttribute(Variable v2, Attribute att) throws IOException {
    spiw.updateAttribute(v2, att);
  }

  /** close the file. */
  @Override
  public synchronized void close() throws IOException {
    if (!isClosed) {
      spiw.close();
      isClosed = true;
    }
  }

  /** Abort writing to this file. The file is closed. */
  public void abort() throws IOException {
    if (!isClosed) {
      spiw.close();
      isClosed = true;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  final String location;
  final boolean fill;
  final int extraHeaderBytes;
  final long preallocateSize;
  final Nc4Chunking chunker;
  final boolean isExisting;

  final NetcdfFileFormat format;
  final boolean useJna;
  final NetcdfFile ncout;
  final IospFileWriter spiw;

  private boolean isClosed = false;

  NetcdfFormatWriter(Builder<?> builder) throws IOException {
    this.location = builder.location;
    this.fill = builder.fill;
    this.extraHeaderBytes = builder.extraHeaderBytes;
    this.preallocateSize = builder.preallocateSize;
    this.chunker = builder.chunker;
    this.isExisting = builder.isExisting;

    if (isExisting) {
      // read existing file to get the format
      try (RandomAccessFile existingRaf = new ucar.unidata.io.RandomAccessFile(location, "r")) {
        this.format = NetcdfFileFormat.findNetcdfFormatType(existingRaf);
      }

      this.useJna = builder.useJna || format.isNetdf4format();
      IospFileWriter spi = useJna ? openJna("ucar.nc2.jni.netcdf.Nc4updater") : new N3iospWriter(builder.iosp);
      try {
        // builder.iosp has the metadata of the file to be created.
        // NC3 doesnt allow additions, NC4 should. So this is messed up here.
        // NC3 can only write to existing variables and extend along the record dimension.
        spi.openForWriting(location, builder.rootGroup, null);
        spi.setFill(fill);
      } catch (Throwable t) {
        spi.close();
        throw t;
      }
      this.ncout = spi.getOutputFile();
      this.spiw = spi;

    } else {
      this.format = builder.format;

      this.useJna = builder.useJna || format.isNetdf4format();
      IospFileWriter spi = useJna ? openJna("ucar.nc2.jni.netcdf.Nc4writer") : new N3iospWriter();
      try {
        // builder.rootGroup has the metadata of the file to be created.
        this.ncout =
            spi.create(location, builder.rootGroup, extraHeaderBytes, preallocateSize, this.format.isLargeFile());
        spi.setFill(fill);
      } catch (Throwable t) {
        spi.close();
        throw t;
      }
      this.spiw = spi;
    }

  }

  private IospFileWriter openJna(String className) {
    IospFileWriter spi;
    try {
      Class<?> iospClass = this.getClass().getClassLoader().loadClass(className);
      Constructor<?> ctor = iospClass.getConstructor(format.getClass());
      spi = (IospFileWriter) ctor.newInstance(format);

      Method method = iospClass.getMethod("setChunker", Nc4Chunking.class);
      method.invoke(spi, chunker);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalArgumentException(className + " cannot use JNI/C library err= " + e.getMessage());
    }
    return spi;
  }

  /////////////////////////////////////////////////////////////////////////////
  /** Obtain a Builder to set custom options */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> {
    String location;
    boolean fill = true;
    int extraHeaderBytes;
    long preallocateSize;
    Nc4Chunking chunker;
    boolean useJna;
    boolean isExisting;

    NetcdfFileFormat format = NetcdfFileFormat.NETCDF3;
    IOServiceProvider iosp; // existing only
    Group.Builder rootGroup = Group.builder().setName("");

    protected abstract T self();

    public Builder<?> setIosp(IOServiceProvider iosp) {
      this.iosp = iosp;
      return this;
    }

    /** The file locatipn */
    public T setLocation(String location) {
      this.location = location;
      return self();
    }

    public T setIsExisting() {
      this.isExisting = true;
      return self();
    }

    /**
     * Set the fill flag. Only used by netcdf-3.
     * If true, the data is first written with fill values.
     * Default is fill = true, to follow the C library.
     * Set false if you expect to write all data values, which makes writing faster.
     * Set true if you want to be sure that unwritten data values are set to the fill value.
     */
    public T setFill(boolean fill) {
      this.fill = fill;
      return self();
    }

    /** Set the format version. Only needed when its a new file. Default is NetcdfFileFormat.NETCDF3 */
    public T setFormat(NetcdfFileFormat format) {
      this.format = format;
      return self();
    }

    /**
     * Set extra bytes to reserve in the header. Only used by netcdf-3.
     * This can prevent rewriting the entire file on redefine.
     *
     * @param extraHeaderBytes # bytes extra for the header
     */
    public T setExtraHeader(int extraHeaderBytes) {
      this.extraHeaderBytes = extraHeaderBytes;
      return self();
    }

    /** Preallocate the file size, for efficiency. Only used by netcdf-3. */
    public T setPreallocateSize(long preallocateSize) {
      this.preallocateSize = preallocateSize;
      return self();
    }

    /** Nc4Chunking, used only for netcdf4 */
    public T setChunker(Nc4Chunking chunker) {
      this.chunker = chunker;
      return self();
    }

    /**
     * Set if you want to use JNA / netcdf c library to do the writing. Default is false.
     * JNA must be used for Netcdf-4. This is used to write to Netcdf-3 format with jna.
     */
    public T setUseJna(boolean useJna) {
      this.useJna = useJna;
      return self();
    }

    /** Add a global attribute */
    public T addAttribute(Attribute att) {
      rootGroup.addAttribute(att);
      return self();
    }

    /** Add a dimension to the root group. */
    public Dimension addDimension(String dimName, int length) {
      return addDimension(new Dimension(dimName, length));
    }

    /** Add a dimension to the root group. */
    public Dimension addDimension(Dimension dim) {
      Dimension useDim = dim;
      if (dim.isUnlimited() && !(dim instanceof UnlimitedDimension)) {
        useDim = new UnlimitedDimension(dim.getShortName(), dim.getLength());
      }
      rootGroup.addDimension(useDim);
      return useDim;
    }

    /** Add an unlimited dimension to the root group. */
    public Dimension addUnlimitedDimension(String dimName) {
      return addDimension(new UnlimitedDimension(dimName, 0));
    }

    /** Get the root group */
    public Group.Builder getRootGroup() {
      return rootGroup;
    }

    /** Set the root group. This allows metadata to be modified externally. */
    public T setRootGroup(Group.Builder rootGroup) {
      this.rootGroup = rootGroup;
      return self();
    }

    /** @deprecated use addVariable(String shortName, ArrayType dataType, String dimString) */
    @Deprecated
    public Variable.Builder<?> addVariable(String shortName, ucar.ma2.DataType dataType, String dimString) {
      return addVariable(shortName, dataType.getArrayType(), dimString);
    }

    /** Add a Variable to the root group. */
    public Variable.Builder<?> addVariable(String shortName, ArrayType dataType, String dimString) {
      Variable.Builder<?> vb = Variable.builder().setName(shortName).setArrayType(dataType)
          .setParentGroupBuilder(rootGroup).setDimensionsByName(dimString);
      rootGroup.addVariable(vb);
      return vb;
    }

    /** @deprecated use addVariable(String shortName, ArrayType dataType, List<Dimension> dims) */
    @Deprecated
    public Variable.Builder<?> addVariable(String shortName, ucar.ma2.DataType dataType, List<Dimension> dims) {
      return addVariable(shortName, dataType.getArrayType(), dims);
    }

    /** Add a Variable to the root group. */
    public Variable.Builder<?> addVariable(String shortName, ArrayType dataType, List<Dimension> dims) {
      Variable.Builder<?> vb = Variable.builder().setName(shortName).setArrayType(dataType)
          .setParentGroupBuilder(rootGroup).setDimensions(dims);
      rootGroup.addVariable(vb);
      return vb;
    }

    /** Add a Structure to the root group. */
    public Structure.Builder<?> addStructure(String shortName, String dimString) {
      Structure.Builder<?> vb =
          Structure.builder().setName(shortName).setParentGroupBuilder(rootGroup).setDimensionsByName(dimString);
      rootGroup.addVariable(vb);
      return vb;
    }

    // TODO doesnt work yet
    public Optional<Variable.Builder<?>> renameVariable(String oldName, String newName) {
      Optional<Variable.Builder<?>> vbOpt = getRootGroup().findVariableLocal(oldName);
      vbOpt.ifPresent(vb -> {
        rootGroup.removeVariable(oldName);
        vb.setName(newName);
        rootGroup.addVariable(vb);
      });
      return vbOpt;
    }

    /** Once this is called, do not use the Builder again. */
    public NetcdfFormatWriter build() throws IOException {
      return new NetcdfFormatWriter(this);
    }
  }
}
