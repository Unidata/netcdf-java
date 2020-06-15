/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.internal.iosp.netcdf3.N3iospWriter;
import ucar.nc2.iosp.IOServiceProviderWriter;

/**
 * Writes Netcdf 3 or 4 formatted files to disk.
 */
public class NetcdfFormatWriter implements Closeable {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFormatWriter.class);
  private static Set<DataType> validN3types =
      EnumSet.of(DataType.BYTE, DataType.CHAR, DataType.SHORT, DataType.INT, DataType.DOUBLE, DataType.FLOAT);

  /**
   * Open an existing Netcdf file for writing data.
   * Cannot add new objects, you can only read/write data to existing Variables.
   *
   * @param location name of existing file to open.
   * @param fill fill mode
   * @return existing file that can be written to
   * @throws IOException on I/O error
   */
  public static NetcdfFormatWriter.Builder openExisting(String location, boolean fill) {
    return builder().setLocation(location).setFill(fill);
  }

  /**
   * Create a new Netcdf file, using the default fill mode.
   *
   * @param location name of new file to open; if it exists, will overwrite it.
   * @return new NetcdfFormatWriter
   * @throws IOException on I/O error
   */
  public static NetcdfFormatWriter.Builder createNewNetcdf3(String location) {
    return builder().setNewFile(true).setFormat(NetcdfFileFormat.NETCDF3).setLocation(location);
  }

  /**
   * Create a new Netcdf file.
   *
   * @param format One of the netcdf-4 NetcdfFileFormat.
   * @param location name of new file to open; if it exists, will overwrite it.
   * @param chunker used only for netcdf4, or null for default chunking algorithm
   * @return new NetcdfFormatWriter
   * @throws IOException on I/O error
   */
  public static NetcdfFormatWriter.Builder createNewVersion4(NetcdfFileFormat format, String location,
      Nc4Chunking chunker) {
    return builder().setNewFile(true).setFormat(format).setLocation(location).setChunker(chunker);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String location;
    private NetcdfFileFormat format = NetcdfFileFormat.NETCDF3;
    private boolean isNewFile;
    private boolean fill = true;
    private int extraHeaderBytes;
    private long preallocateSize;
    private Nc4Chunking chunker;
    private boolean useJna;

    private Group.Builder rootGroup = Group.builder().setName("");

    /** The file locatipn */
    public Builder setLocation(String location) {
      this.location = location;
      return this;
    }

    /** Set the format version. Only needed when its a new file. Default is NetcdfFileFormat.CLASSIC */
    public Builder setFormat(NetcdfFileFormat format) {
      this.format = format;
      return this;
    }

    public NetcdfFileFormat getFormat() {
      return this.format;
    }

    /** True if its a new file, false if its an existing file. Default false. */
    public Builder setNewFile(boolean newFile) {
      isNewFile = newFile;
      return this;
    }

    /**
     * Set the fill flag. Only used by netcdf-3.
     * If true, the data is first written with fill values.
     * Default is fill = true, to follow the C library.
     * Leave false if you expect to write all data values, set to true if you want to be
     * sure that unwritten data values have the fill value in it.
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

    /** Set if you want to use JNA / netcdf c library to do the writing. Default is false. */
    public Builder setUseJna(boolean useJna) {
      this.useJna = useJna;
      return this;
    }

    public Builder addAttribute(Attribute att) {
      rootGroup.addAttribute(att);
      return this;
    }

    public Dimension addDimension(String dimName, int length) {
      Dimension dim = new Dimension(dimName, length);
      rootGroup.addDimension(dim);
      return dim;
    }

    public Dimension addDimension(Dimension dim) {
      rootGroup.addDimension(dim);
      return dim;
    }

    public Group.Builder getRootGroup() {
      return rootGroup;
    }

    /** Set the root group. This allows it to be built externally. */
    public void setRootGroup(Group.Builder rootGroup) {
      this.rootGroup = rootGroup;
    }

    /** Add a Variable to the root group. */
    public Variable.Builder addVariable(String shortName, DataType dataType, String dimString) {
      Variable.Builder vb = Variable.builder().setName(shortName).setDataType(dataType).setParentGroupBuilder(rootGroup)
          .setDimensionsByName(dimString);
      rootGroup.addVariable(vb);
      return vb;
    }

    /** Add a Variable to the root group. */
    public Variable.Builder addVariable(String shortName, DataType dataType, List<Dimension> dims) {
      Variable.Builder vb = Variable.builder().setName(shortName).setDataType(dataType).setParentGroupBuilder(rootGroup)
          .setDimensions(dims);
      rootGroup.addVariable(vb);
      return vb;
    }

    /** Add a Structure to the root group. */
    public Structure.Builder addStructure(String shortName, String dimString) {
      Structure.Builder vb =
          Structure.builder().setName(shortName).setParentGroupBuilder(rootGroup).setDimensionsByName(dimString);
      rootGroup.addVariable(vb);
      return vb;
    }

    public NetcdfFormatWriter build() throws IOException {
      return new NetcdfFormatWriter(this);
    }
  }

  /** Value class is the result of calling create() */
  public static class Result {
    private final long sizeToBeWritten;
    private final boolean wasWritten;
    @Nullable
    private final String errorMessage;

    private Result(long sizeToBeWritten, boolean wasWritten, @Nullable String errorMessage) {
      this.sizeToBeWritten = sizeToBeWritten;
      this.wasWritten = wasWritten;
      this.errorMessage = errorMessage;
    }

    /**
     * Estimated number of bytes the file will take. This is NOT the same as the size of the the whole output file, but
     * it's close.
     */
    public long sizeToBeWritten() {
      return sizeToBeWritten;
    }

    /** Whether the file was created or not. */
    public boolean wasWritten() {
      return wasWritten;
    }

    @Nullable
    public String getErrorMessage() {
      return errorMessage;
    }

    public static Result create(long sizeToBeWritten, boolean wasWritten, @Nullable String errorMessage) {
      return new Result(sizeToBeWritten, wasWritten, errorMessage);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  private final String location;
  private final NetcdfFileFormat format;
  private final boolean isNewFile;
  private final boolean fill;
  private final int extraHeaderBytes;
  private final long preallocateSize;
  private final Nc4Chunking chunker;
  private final boolean useJna;

  private final Group rootGroup;
  private final NetcdfFile ncout;
  private final IOServiceProviderWriter spiw;
  private final ucar.unidata.io.RandomAccessFile existingRaf;

  private boolean isClosed = false;

  private NetcdfFormatWriter(Builder builder) throws IOException {
    this.location = builder.location;
    this.format = builder.format;
    this.isNewFile = builder.isNewFile;
    this.fill = builder.fill;
    this.extraHeaderBytes = builder.extraHeaderBytes;
    this.preallocateSize = builder.preallocateSize;
    this.chunker = builder.chunker;
    this.useJna = builder.useJna || format.isNetdf4format();

    this.ncout = NetcdfFile.builder().setRootGroup(builder.rootGroup).build();
    this.rootGroup = this.ncout.getRootGroup();

    if (!isNewFile) {
      existingRaf = new ucar.unidata.io.RandomAccessFile(location, "rw");
      NetcdfFileFormat existingVersion = NetcdfFileFormat.findNetcdfFormatType(existingRaf);
      if (format != null && format != existingVersion) {
        existingRaf.close();
        throw new IllegalArgumentException("Existing file at location" + location + " (" + existingVersion
            + ") does not match requested version " + format);
      }
    } else {
      existingRaf = null;
    }

    if (useJna) {
      String className = "ucar.nc2.jni.netcdf.Nc4Iosp";
      IOServiceProviderWriter spi;
      try {
        Class iospClass = this.getClass().getClassLoader().loadClass(className);
        NetcdfFileWriter.Version version = convertToNetcdfFileWriterVersion(format);
        Constructor<IOServiceProviderWriter> ctor = iospClass.getConstructor(version.getClass());
        spi = ctor.newInstance(version);

        Method method = iospClass.getMethod("setChunker", Nc4Chunking.class);
        method.invoke(spi, chunker);
      } catch (Throwable e) {
        throw new IllegalArgumentException(className + " cannot use JNI/C library err= " + e.getMessage());
      }
      spiw = spi;
    } else {
      spiw = new N3iospWriter();
    }

    if (!isNewFile) {
      spiw.openForWriting(existingRaf, this.ncout, null);
    } else {
      spiw.create(location, this.ncout, extraHeaderBytes, preallocateSize, testIfLargeFile());
    }
    spiw.setFill(fill);
  }

  // Temporary bridge to NetcdfFileWriter.Version
  public static NetcdfFileWriter.Version convertToNetcdfFileWriterVersion(NetcdfFileFormat format) {
    switch (format) {
      case NETCDF3:
        return NetcdfFileWriter.Version.netcdf3;
      case NETCDF4:
        return NetcdfFileWriter.Version.netcdf4;
      case NETCDF4_CLASSIC:
        return NetcdfFileWriter.Version.netcdf4_classic;
      case NETCDF3_64BIT_OFFSET:
        return NetcdfFileWriter.Version.netcdf3c64;
      default:
        throw new IllegalStateException("Unsupported format: " + format);
    }
  }

  public static NetcdfFileFormat convertToNetcdfFileFormat(NetcdfFileWriter.Version version) {
    switch (version) {
      case netcdf3:
        return NetcdfFileFormat.NETCDF3;
      case netcdf4:
        return NetcdfFileFormat.NETCDF4;
      case netcdf4_classic:
        return NetcdfFileFormat.NETCDF4_CLASSIC;
      case netcdf3c64:
        return NetcdfFileFormat.NETCDF3_64BIT_OFFSET;
      default:
        throw new IllegalStateException("Unsupported version: " + version);
    }
  }


  public NetcdfFile getOutputFile() {
    return this.ncout;
  }

  public NetcdfFileFormat getFormat() {
    return format;
  }

  public Variable findVariable(String fullNameEscaped) {
    return this.ncout.findVariable(fullNameEscaped);
  }

  public Dimension findDimension(String dimName) {
    return this.ncout.findDimension(dimName);
  }

  public Attribute findGlobalAttribute(String attName) {
    return this.ncout.getRootGroup().findAttribute(attName);
  }

  private String makeValidObjectName(String name) {
    if (!isValidObjectName(name)) {
      String nname = createValidObjectName(name);
      log.warn("illegal object name= " + name + " change to " + name);
      return nname;
    }
    return name;
  }

  private boolean isValidObjectName(String name) {
    return NetcdfFileFormat.isValidNetcdfObjectName(name);
  }

  private boolean isValidDataType(DataType dt) {
    return format.isExtendedModel() || validN3types.contains(dt);
  }

  private String createValidObjectName(String name) {
    return NetcdfFileFormat.makeValidNetcdfObjectName(name);
  }

  /*
   * LOOK how to check if file is too large and convert ??
   * After you have added all of the Dimensions, Variables, and Attributes,
   * call create() to actually create the new file, or open the existing file.
   *
   * @param netcdfOut Contains the metadata of the file to be written. Caller must write data with write().
   * 
   * @param maxBytes if > 0, only create the file if sizeToBeWritten < maxBytes.
   * 
   * @return return Result indicating sizeToBeWritten and if it was created.
   *
   * public Result create(NetcdfFile netcdfOut, long maxBytes) throws IOException {
   * Preconditions.checkArgument(isNewFile, "can only call create on a new file");
   * 
   * long sizeToBeWritten = calcSize(netcdfOut.getRootGroup());
   * if (maxBytes > 0 && sizeToBeWritten > maxBytes) {
   * return Result.create(sizeToBeWritten, false, String.format("Too large, max size = %d", maxBytes));
   * }
   * //this.ncout = netcdfOut;
   * spiw.setFill(fill);
   * 
   * if (!isNewFile) {
   * spiw.openForWriting(existingRaf, this.ncout, null);
   * } else {
   * spiw.create(location, this.ncout, extraHeaderBytes, preallocateSize, testIfLargeFile());
   * }
   * 
   * return Result.create(sizeToBeWritten, true, null);
   * }
   */

  private boolean testIfLargeFile() {
    if (format == NetcdfFileFormat.NETCDF3_64BIT_OFFSET) {
      return true;
    }

    if (format == NetcdfFileFormat.NETCDF3) {
      long totalSizeOfVars = calcSize(ncout.getRootGroup());
      long maxSize = Integer.MAX_VALUE;
      if (totalSizeOfVars > maxSize) {
        log.debug("Request size = {} Mbytes", totalSizeOfVars / 1000 / 1000);
        return true;
      }
    }
    return false;
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
   * @throws ucar.ma2.InvalidRangeException if values Array has illegal shape
   */
  public void write(Variable v, Array values) throws java.io.IOException, InvalidRangeException {
    if (this.ncout != v.getNetcdfFile())
      throw new IllegalArgumentException("Variable is not owned by this writer.");

    write(v, new int[values.getRank()], values);
  }

  /** Write data to the named variable, origin assumed to be 0. */
  public void write(String varName, Array values) throws IOException, InvalidRangeException {
    write(findVariable(varName), values);
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
    write(findVariable(varName), origin, values);
  }

  /**
   * Write String data to a CHAR variable, origin assumed to be 0.
   *
   * @param v variable to write to
   * @param values write this array; must be ArrayObject of String
   * @throws IOException if I/O error
   * @throws InvalidRangeException if values Array has illegal shape
   */
  public void writeStringData(Variable v, Array values) throws IOException, InvalidRangeException {
    writeStringData(v, new int[values.getRank()], values);
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
  public void writeStringData(Variable v, int[] origin, Array values) throws IOException, InvalidRangeException {

    if (values.getElementType() != String.class)
      throw new IllegalArgumentException("Must be ArrayObject of String ");

    if (v.getDataType() != DataType.CHAR)
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
  public void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException {
    spiw.updateAttribute(v2, att);
  }

  /** Flush anything written to disk. */
  public void flush() throws IOException {
    spiw.flush();
  }

  /** close the file. */
  @Override
  public synchronized void close() throws IOException {
    if (!isClosed) {
      flush();
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
