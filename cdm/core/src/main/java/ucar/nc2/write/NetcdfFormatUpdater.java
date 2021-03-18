/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import static ucar.nc2.NetcdfFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import ucar.array.ArrayType;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayObject;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.internal.iosp.IospFileUpdater;
import ucar.nc2.internal.iosp.hdf5.H5iosp;
import ucar.nc2.internal.iosp.netcdf3.N3iosp;
import ucar.nc2.internal.iosp.netcdf3.N3iospWriter;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.unidata.io.RandomAccessFile;

/**
 * Writes Netcdf 3 or 4 formatted files to disk.
 * Note that there is no redefine mode. Once you call build(), you cannot add new metadata, you can only write data.
 *
 * <pre>
 * NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
 * writerb.addDimension(Dimension.builder().setName("vdim").setIsUnlimited(true).build());
 * writerb.addVariable("v", DataType.BYTE, "vdim");
 * try (NetcdfFormatWriter writer = writerb.build()) {
 *   writer.write("v", dataArray);
 * }
 * </pre>
 */
public class NetcdfFormatUpdater implements Closeable {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFormatUpdater.class);

  /**
   * Open an existing Netcdf format file for writing data.
   * Cannot add new objects, you can only read/write data to existing Variables.
   * TODO: allow changes to Netcdf-4 format.
   *
   * @param location name of existing NetCDF file to open.
   * @return existing file that can be written to
   */
  public static NetcdfFormatUpdater.Builder openExisting(String location) throws IOException {
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
      return builder().setRootGroup(root).setLocation(location).setIosp(iosp).setFormat(format);
    }
  }

  /** Obtain a Builder to set custom options */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String location;
    private NetcdfFileFormat format = NetcdfFileFormat.NETCDF3;
    private boolean fill = true;
    private int extraHeaderBytes;
    private long preallocateSize;
    private Nc4Chunking chunker;
    private boolean useJna;
    private IOServiceProvider iosp; // existing only
    private Group.Builder rootGroup = Group.builder().setName("");

    /** The file locatipn */
    public Builder setLocation(String location) {
      this.location = location;
      return this;
    }

    public Builder setIosp(IOServiceProvider iosp) {
      this.iosp = iosp;
      return this;
    }

    public IOServiceProvider getIosp() {
      return iosp;
    }

    /** Set the format version. Only needed when its a new file. Default is NetcdfFileFormat.NETCDF3 */
    public Builder setFormat(NetcdfFileFormat format) {
      this.format = format;
      return this;
    }

    public NetcdfFileFormat getFormat() {
      return this.format;
    }

    /**
     * Set the fill flag. Only used by netcdf-3.
     * If true, the data is first written with fill values.
     * Default is fill = true, to follow the C library.
     * Set false if you expect to write all data values, which makes writing faster.
     * Set true if you want to be sure that unwritten data values are set to the fill value.
     */
    public Builder setFill(boolean fill) {
      this.fill = fill;
      return this;
    }

    /**
     * Set extra bytes to reserve in the header. Only used by netcdf-3.
     * This can prevent rewriting the entire file on redefine.
     *
     * @param extraHeaderBytes # bytes extra for the header
     */
    public Builder setExtraHeader(int extraHeaderBytes) {
      this.extraHeaderBytes = extraHeaderBytes;
      return this;
    }

    /** Preallocate the file size, for efficiency. Only used by netcdf-3. */
    public Builder setPreallocateSize(long preallocateSize) {
      this.preallocateSize = preallocateSize;
      return this;
    }

    /** Nc4Chunking, used only for netcdf4 */
    public Builder setChunker(Nc4Chunking chunker) {
      this.chunker = chunker;
      return this;
    }

    /**
     * Set if you want to use JNA / netcdf c library to do the writing. Default is false.
     * JNA must be used for Netcdf-4. This is used to write to Netcdf-3 format.
     */
    public Builder setUseJna(boolean useJna) {
      this.useJna = useJna;
      return this;
    }

    /** Add a global attribute */
    public Builder addAttribute(Attribute att) {
      if (!useJna) {
        throw new UnsupportedOperationException("Cant add attribute to existing netcdf-3 files");
      }
      rootGroup.addAttribute(att);
      return this;
    }

    /** Add a dimension to the root group. */
    public Dimension addDimension(String dimName, int length) {
      return addDimension(new Dimension(dimName, length));
    }

    /** Add a dimension to the root group. */
    public Dimension addDimension(Dimension dim) {
      if (!useJna) {
        throw new UnsupportedOperationException("Cant add dimension to existing netcdf-3 files");
      }
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

    /**
     * Set the root group. This allows metadata to be modified externally.
     * Also this is the rootGroup from an openExisting() file.
     */
    public Builder setRootGroup(Group.Builder rootGroup) {
      this.rootGroup = rootGroup;
      return this;
    }

    /** @deprecated use addVariable(String shortName, ArrayType dataType, String dimString) */
    @Deprecated
    public Variable.Builder<?> addVariable(String shortName, ucar.ma2.DataType dataType, String dimString) {
      return addVariable(shortName, dataType.getArrayType(), dimString);
    }

    /** Add a Variable to the root group. */
    public Variable.Builder<?> addVariable(String shortName, ArrayType dataType, String dimString) {
      if (!useJna) {
        throw new UnsupportedOperationException("Cant add variable to existing netcdf-3 files");
      }
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
      if (!useJna) {
        throw new UnsupportedOperationException("Cant add variable to existing netcdf-3 files");
      }
      Variable.Builder<?> vb = Variable.builder().setName(shortName).setArrayType(dataType)
          .setParentGroupBuilder(rootGroup).setDimensions(dims);
      rootGroup.addVariable(vb);
      return vb;
    }

    /** Add a Structure to the root group. */
    public Structure.Builder<?> addStructure(String shortName, String dimString) {
      if (!useJna) {
        throw new UnsupportedOperationException("Cant add structure to existing netcdf-3 files");
      }
      Structure.Builder<?> vb =
          Structure.builder().setName(shortName).setParentGroupBuilder(rootGroup).setDimensionsByName(dimString);
      rootGroup.addVariable(vb);
      return vb;
    }

    // TODO doesnt work yet
    public Optional<Variable.Builder<?>> renameVariable(String oldName, String newName) {
      Optional<Variable.Builder<?>> vbOpt = rootGroup.findVariableLocal(oldName);
      vbOpt.ifPresent(vb -> {
        rootGroup.removeVariable(oldName);
        vb.setName(newName);
        rootGroup.addVariable(vb);
      });
      return vbOpt;
    }

    /** Once this is called, do not use the Builder again. */
    public NetcdfFormatUpdater build() throws IOException {
      return new NetcdfFormatUpdater(this);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  private final String location;
  private final NetcdfFileFormat format;
  private final boolean fill;
  private final int extraHeaderBytes;
  private final long preallocateSize;
  private final Nc4Chunking chunker;
  private final boolean useJna;

  private final Group rootGroup;
  private final NetcdfFile ncout;
  private final IospFileUpdater spiw;

  private boolean isClosed = false;

  private NetcdfFormatUpdater(Builder builder) throws IOException {
    this.location = builder.location;
    this.fill = builder.fill;
    this.extraHeaderBytes = builder.extraHeaderBytes;
    this.preallocateSize = builder.preallocateSize;
    this.chunker = builder.chunker;

    // This is what we want the file to look like. Once we call build(), it is complete.
    // NetcdfFile.Builder<?> ncfileb =
    // NetcdfFile.builder().setRootGroup(builder.rootGroup).setLocation(builder.location);

    // read existing file to get the format
    try (RandomAccessFile existingRaf = new ucar.unidata.io.RandomAccessFile(location, "r")) {
      this.format = NetcdfFileFormat.findNetcdfFormatType(existingRaf);
    }

    this.useJna = builder.useJna || format.isNetdf4format();
    if (useJna) {
      String className = "ucar.nc2.jni.netcdf.Nc4updater";
      IospFileUpdater spi;
      try {
        Class iospClass = this.getClass().getClassLoader().loadClass(className);
        Constructor<IospFileUpdater> ctor = iospClass.getConstructor(format.getClass());
        spi = ctor.newInstance(format);

        Method method = iospClass.getMethod("setChunker", Nc4Chunking.class);
        method.invoke(spi, chunker);
      } catch (Throwable e) {
        e.printStackTrace();
        throw new IllegalArgumentException(className + " cannot use JNI/C library err= " + e.getMessage());
      }
      spiw = spi;
    } else {
      spiw = new N3iospWriter(builder.getIosp());
    }

    try {
      // ncfileb has the metadata of the file to be written to. NC3 doesnt allow additions, NC4 should. So this is
      // messed up here.
      // NC3 can only write to existing variables and extend along the record dimension.
      spiw.openForWriting(location, builder.rootGroup, null);
      spiw.setFill(fill);
    } catch (Throwable t) {
      spiw.close();
      throw t;
    }

    this.ncout = spiw.getOutputFile();
    this.rootGroup = this.ncout.getRootGroup();
  }

  // TODO should not be used to read data, close and reopen
  public NetcdfFile getOutputFile() {
    return this.ncout;
  }

  public NetcdfFileFormat getFormat() {
    return format;
  }

  @Nullable
  public Variable findVariable(String fullNameEscaped) {
    return this.ncout.findVariable(fullNameEscaped);
  }

  @Nullable
  public Dimension findDimension(String dimName) {
    return this.ncout.findDimension(dimName);
  }

  @Nullable
  public Attribute findGlobalAttribute(String attName) {
    return this.ncout.getRootGroup().findAttribute(attName);
  }

  public long calcSize() {
    return calcSize(ncout.getRootGroup());
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
  //// use these calls to write data to the file

  /**
   * Write data to the given variable, origin assumed to be 0.
   *
   * @param v variable to write to
   * @param values write this array; must be same type and rank as Variable
   * @throws IOException if I/O error
   * @throws InvalidRangeException if values Array has illegal shape
   */
  public void write(Variable v, Array values) throws IOException, InvalidRangeException {
    write(v, new int[values.getRank()], values);
  }

  /** Write data to the named variable, origin assumed to be 0. */
  public void write(String varName, Array values) throws IOException, InvalidRangeException {
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
   */
  public void write(Variable v, int[] origin, Array values) throws IOException, InvalidRangeException {
    spiw.writeData(v, new Section(origin, values.getShape()), values);
  }

  /**
   * Write data to the named variable.
   *
   * @param varName name of variable to write to
   * @param origin offset within the variable to start writing.
   * @param values write this array; must be same type and rank as Variable
   * @throws IOException if I/O error
   * @throws InvalidRangeException if values Array has illegal shape
   */
  public void write(String varName, int[] origin, Array values) throws IOException, InvalidRangeException {
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
   */
  public void writeStringDataToChar(Variable v, Array values) throws IOException, InvalidRangeException {
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
   */
  public void writeStringDataToChar(Variable v, int[] origin, Array values) throws IOException, InvalidRangeException {
    if (values.getElementType() != String.class)
      throw new IllegalArgumentException("values must be an ArrayObject of String ");

    if (v.getArrayType() != ArrayType.CHAR)
      throw new IllegalArgumentException("variable " + v.getFullName() + " is not type CHAR");

    int rank = v.getRank();
    int strlen = v.getShape(rank - 1);

    // turn it into an ArrayChar
    ArrayChar cvalues = ArrayChar.makeFromStringArray((ArrayObject) values, strlen);

    int[] corigin = new int[rank];
    System.arraycopy(origin, 0, corigin, 0, rank - 1);

    write(v, corigin, cvalues);
  }

  public int appendStructureData(Structure s, StructureData sdata) throws IOException, InvalidRangeException {
    return spiw.appendStructureData(s, sdata);
  }

  /**
   * Update the value of an existing attribute. Attribute is found by name, which must match exactly.
   * You cannot make an attribute longer, or change the number of values.
   * For strings: truncate if longer, zero fill if shorter. Strings are padded to 4 byte boundaries, ok to use padding
   * if it exists.
   * For numerics: must have same number of values.
   * This is really a netcdf-3 writing only. netcdf-4 attributes can be changed without rewriting.
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
}
