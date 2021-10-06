/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.ArraysConvert;
import ucar.array.InvalidRangeException;
import ucar.array.Index;
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

import static ucar.nc2.NetcdfFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

/**
 * Creates new Netcdf 3/4 format files. Writes data to new or existing files.
 * <p/>
 * LOOK in ver7, netcdf3 and netcdf4 will be seperate, so they can have different functionality.
 * <p/>
 * 
 * <pre>
 * NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
 * writerb.addDimension(Dimension.builder().setName("vdim").setIsUnlimited(true).build());
 * writerb.addVariable("v", DataType.BYTE, "vdim");
 * try (NetcdfFormatWriter writer = writerb.build()) {
 *   writer.config().forVariable("v").withArray(dataArray).write();
 * }
 * </pre>
 */
public class NetcdfFormatWriter implements Closeable {

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
      if (format != null && !format.isNetdf3format() && !format.isNetdf4format()) {
        throw new IllegalArgumentException(
            String.format("%s is not a netcdf-3 or netcdf-4 file (%s)", location, format));
      }
      return builder().setRootGroup(root).setLocation(location).setIosp(iosp).setFormat(format).setIsExisting();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** The output file's root group. */
  public Group getRootGroup() {
    return ncout.getRootGroup();
  }

  /** The output file's format. */
  public NetcdfFileFormat getFormat() {
    return this.format;
  }

  /** Find the named Variable in the output file. */
  @Nullable
  public Variable findVariable(String fullNameEscaped) {
    return ncout.findVariable(fullNameEscaped);
  }

  /** Find the named Dimension in the output file. */
  @Nullable
  public Dimension findDimension(String dimName) {
    return ncout.findDimension(dimName);
  }

  /** Find the named global attribute in the output file. */
  @Nullable
  public Attribute findGlobalAttribute(String attName) {
    return getRootGroup().findAttribute(attName);
  }

  /**
   * An estimate of the size of the file when written to disk. Ignores compression for netcdf4.
   * 
   * @return estimated file size in bytes.
   */
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
   * @param values write this array; must have compatible type and shape with Variable
   */
  public void write(Variable v, Index origin, ucar.array.Array<?> values) throws IOException, InvalidRangeException {
    Preconditions.checkArgument(v.getArrayType() == values.getArrayType()); // LOOK do something better?
    Preconditions.checkArgument(v.getRank() == values.getRank()); // LOOK do something better: contains?

    // we have to keep using old until all spis implement new?
    ucar.ma2.Array oldArray = ArraysConvert.convertFromArray(values);
    try {
      write(v, origin.getCurrentIndex(), oldArray);
    } catch (ucar.ma2.InvalidRangeException e) {
      throw new InvalidRangeException(e);
    }
  }

  /**
   * Write String value to a CHAR Variable.
   * Truncated or zero extended as needed to fit into last dimension of v.
   * Note that origin is not incremeted as in previous versions.
   * 
   * <pre>
   * Index index = Index.ofRank(v.getRank());
   * writer.writeStringData(v, index, "This is the first string.");
   * writer.writeStringData(v, index.incr(0), "Shorty");
   * writer.writeStringData(v, index.incr(0), "This is too long so it will get truncated");
   * </pre>
   * 
   * @param v write to this variable, must be of type CHAR.
   * @param origin offset within the variable to start writing.
   * @param data The String to write.
   */
  public void writeStringData(Variable v, Index origin, String data) throws IOException, InvalidRangeException {
    Preconditions.checkArgument(v.getArrayType() == ArrayType.CHAR);
    Preconditions.checkArgument(v.getRank() > 0);
    int[] shape = v.getShape();
    // all but the last shape is 1
    for (int i = 0; i < shape.length - 1; i++) {
      shape[i] = 1;
    }
    int last = shape[shape.length - 1];

    // previously we truncated chars to bytes.
    // here we are going to use UTF encoded bytes, just as if we were real programmers.
    byte[] bb = data.getBytes(Charsets.UTF_8);
    if (bb.length != last) {
      byte[] storage = new byte[last];
      System.arraycopy(bb, 0, storage, 0, Math.min(bb.length, last));
      bb = storage;
    }

    Array<?> barray = Arrays.factory(ArrayType.CHAR, shape, bb);
    write(v, origin, barray);
  }

  /**
   * Write StructureData along the unlimited dimension.
   * 
   * @return the recnum where it was written.
   */
  // TODO does this work?
  public int appendStructureData(Structure s, ucar.array.StructureData sdata)
      throws IOException, InvalidRangeException {
    ucar.ma2.StructureMembers membersMa2 = s.makeStructureMembers(); // ??
    ucar.ma2.StructureData oldStructure = ArraysConvert.convertStructureData(membersMa2, sdata);
    try {
      return appendStructureData(s, oldStructure);
    } catch (ucar.ma2.InvalidRangeException e) {
      throw new InvalidRangeException(e);
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
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#write(Variable, ucar.array.Index, ucar.array.Array)}
   */
  @Deprecated
  public void write(Variable v, ucar.ma2.Array values) throws IOException, ucar.ma2.InvalidRangeException {
    write(v, new int[values.getRank()], values);
  }

  /**
   * Write data to the named variable, origin assumed to be 0.
   *
   * @deprecated use {@link NetcdfFormatWriter#write(Variable, ucar.array.Index, ucar.array.Array)}
   */
  @Deprecated
  public void write(String varName, ucar.ma2.Array values) throws IOException, ucar.ma2.InvalidRangeException {
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
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#write(Variable, Index, ucar.array.Array)}
   */
  @Deprecated
  public void write(Variable v, int[] origin, ucar.ma2.Array values)
      throws IOException, ucar.ma2.InvalidRangeException {
    spiw.writeData(v, new ucar.ma2.Section(origin, values.getShape()), values);
  }

  /**
   * Write data to the named variable.
   *
   * @param varName name of variable to write to
   * @param origin offset within the variable to start writing.
   * @param values write this array; must be same type and rank as Variable
   * @throws IOException if I/O error
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#write(Variable, ucar.array.Index, ucar.array.Array)}
   */
  @Deprecated
  public void write(String varName, int[] origin, ucar.ma2.Array values)
      throws IOException, ucar.ma2.InvalidRangeException {
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
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#writeStringData(Variable, Index, String)}
   */
  @Deprecated
  public void writeStringDataToChar(Variable v, ucar.ma2.Array values)
      throws IOException, ucar.ma2.InvalidRangeException {
    writeStringDataToChar(v, new int[values.getRank()], values);
  }

  /**
   * Write String data to a CHAR variable.
   *
   * @param v variable to write to
   * @param origin offset to start writing, ignore the strlen dimension.
   * @param values write this array; must be ArrayObject of String
   * @throws IOException if I/O error
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   * @deprecated use {@link NetcdfFormatWriter#writeStringData(Variable, Index, String)}
   */
  @Deprecated
  public void writeStringDataToChar(Variable v, int[] origin, ucar.ma2.Array values)
      throws IOException, ucar.ma2.InvalidRangeException {
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
  public int appendStructureData(Structure s, ucar.ma2.StructureData sdata)
      throws IOException, ucar.ma2.InvalidRangeException {
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

  /** Close the file. */
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

    } else { // create file
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
  /** Obtain a mutable Builder to create or modify the file metadata. */
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

    /*
     * TODO doesnt work yet
     * public Optional<Variable.Builder<?>> renameVariable(String oldName, String newName) {
     * Optional<Variable.Builder<?>> vbOpt = getRootGroup().findVariableLocal(oldName);
     * vbOpt.ifPresent(vb -> {
     * rootGroup.removeVariable(oldName);
     * vb.setName(newName);
     * rootGroup.addVariable(vb);
     * });
     * return vbOpt;
     * }
     */

    /** Once this is called, do not use the Builder again. */
    public NetcdfFormatWriter build() throws IOException {
      return new NetcdfFormatWriter(this);
    }
  }

  /** Obtain a WriteConfig to configure data writing. */
  public WriteConfig config() {
    return new WriteConfig();
  }

  /** Fluid API for writing. */
  public class WriteConfig {
    Variable v;
    String varName;
    Index origin;
    Object primArray;
    ucar.array.Array<?> values;
    int[] shape;
    String stringValue;

    /** Write to this Variable. Set Variable or Variable name. */
    public WriteConfig forVariable(Variable v) {
      this.v = v;
      return this;
    }

    /** Write to this named Variable. Set Variable or Variable name. */
    public WriteConfig forVariable(String varName) {
      this.varName = varName;
      return this;
    }

    /**
     * The starting element, ie write(int[] origin, int[] shape).
     * If not set, origin of 0 is assumed.
     */
    public WriteConfig withOrigin(Index origin) {
      this.origin = origin;
      return this;
    }

    /** The starting element as an int[]. */
    public WriteConfig withOrigin(int... origin) {
      this.origin = Index.of(origin);
      return this;
    }

    /** The values to write. ArrayType must match the Variable. */
    public WriteConfig withArray(ucar.array.Array<?> values) {
      this.values = values;
      this.shape = values.getShape();
      return this;
    }

    /**
     * The values to write as a 1D java primitive array, eg float[].
     * 
     * @see Arrays#factory(ArrayType type, int[] shape, Object primArray)
     */
    public WriteConfig withPrimitiveArray(Object primArray) {
      this.primArray = primArray;
      return this;
    }

    /**
     * Shape of primitive array to write, ie write(int[] origin, int[] shape).
     * Only needed if you use withPrimitiveArray, otherwise the Array values' shape is used.
     * Use v.getShape() if shape is not set, and Array values is not set.
     */
    public WriteConfig withShape(int... shape) {
      this.shape = shape;
      return this;
    }

    /**
     * For writing a String into a CHAR Variable.
     * 
     * @see NetcdfFormatWriter#writeStringData(Variable, Index, String)
     */
    public WriteConfig withString(String sval) {
      this.stringValue = sval;
      return this;
    }

    /**
     * Do the write to the file, agter constructing the WriteConfig.
     * 
     * @throws IllegalArgumentException when not configured correctly.
     * @see NetcdfFormatWriter#write(Variable, ucar.array.Index, ucar.array.Array)
     */
    public void write() throws IOException, InvalidRangeException, IllegalArgumentException {
      if (this.v == null && this.varName == null) {
        throw new IllegalArgumentException("Must set Variable");
      }
      if (v == null) {
        this.v = findVariable(this.varName);
        if (this.v == null) {
          throw new IllegalArgumentException("Unknown Variable " + this.varName);
        }
      }
      if (this.origin == null) {
        this.origin = Index.ofRank(v.getRank());
      }

      if (stringValue != null) {
        NetcdfFormatWriter.this.writeStringData(this.v, this.origin, stringValue);
        return;
      }

      if (this.values == null) {
        if (this.primArray == null) {
          throw new IllegalArgumentException("Must set Array or primitive array");
        }
        if (this.shape == null) {
          this.shape = v.getShape();
        }
        this.values = Arrays.factory(v.getArrayType(), this.shape, this.primArray);
      }
      NetcdfFormatWriter.this.write(this.v, this.origin, this.values);
    }
  }
}
