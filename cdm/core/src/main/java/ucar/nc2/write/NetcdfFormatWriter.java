/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
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
  public static NetcdfFormatWriter openExisting(String location, boolean fill) throws IOException {
    return builder().setLocation(location).setFill(fill).build();
  }

  /**
   * Create a new Netcdf file, using the default fill mode.
   *
   * @param format One of the netcdf-3 NetcdfFileFormat.
   * @param location name of new file to open; if it exists, will overwrite it.
   * @return new NetcdfFormatWriter
   * @throws IOException on I/O error
   */
  public static NetcdfFormatWriter createNewVersion3(NetcdfFileFormat format, String location) throws IOException {
    return builder().setNewFile(true).setFormat(format).setLocation(location).build();
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
  public static NetcdfFormatWriter createNewVersion4(NetcdfFileFormat format, String location, Nc4Chunking chunker)
      throws IOException {
    return builder().setNewFile(true).setFormat(format).setLocation(location).setChunker(chunker).build();
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

    public NetcdfFormatWriter build() throws IOException {
      return new NetcdfFormatWriter(this);
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

  private ucar.unidata.io.RandomAccessFile outputRaf;
  private IOServiceProviderWriter spiw;
  private NetcdfFile ncout;

  private NetcdfFormatWriter(Builder builder) throws IOException {
    this.location = builder.location;
    this.format = builder.format;
    this.isNewFile = builder.isNewFile;
    this.fill = builder.fill;
    this.extraHeaderBytes = builder.extraHeaderBytes;
    this.preallocateSize = builder.preallocateSize;
    this.chunker = builder.chunker;
    this.useJna = builder.useJna;

    if (!isNewFile) {
      outputRaf = new ucar.unidata.io.RandomAccessFile(location, "rw");
      NetcdfFileFormat existingVersion = NetcdfFileFormat.findNetcdfFormatType(outputRaf);
      if (format != null && format != existingVersion) {
        outputRaf.close();
        throw new IllegalArgumentException("Existing file at location" + location + " (" + existingVersion
            + ") does not match requested version " + format);
      }
    }

    if (useJna) {
      String className = "ucar.nc2.jni.netcdf.Nc4Iosp";
      IOServiceProviderWriter spi;
      try {
        Class iospClass = this.getClass().getClassLoader().loadClass(className);
        Constructor<IOServiceProviderWriter> ctor =
            iospClass.getConstructor(convertToNetcdfFileWriterVersion(format).getClass());
        spi = ctor.newInstance(format);

        Method method = iospClass.getMethod("setChunker", Nc4Chunking.class);
        method.invoke(spi, chunker);
      } catch (Throwable e) {
        throw new IllegalArgumentException(className + " is not loaded, cannot use JNI/C library");
      }
      spiw = spi;
    } else {
      spiw = new N3iospWriter();
    }
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

  /**
   * After you have added all of the Dimensions, Variables, and Attributes,
   * call create() to actually create the file.
   *
   * @throws IOException if I/O error
   */
  public void create(NetcdfFile netcdfOut) throws IOException {
    if (!isNewFile)
      throw new UnsupportedOperationException("can only call create on a new file");

    this.ncout = netcdfOut;
    if (!isNewFile)
      spiw.openForWriting(outputRaf, this.ncout, null);

    this.ncout.finish(); // ??
    spiw.setFill(fill); // ??
    spiw.create(location, this.ncout, extraHeaderBytes, preallocateSize,
        format == NetcdfFileFormat.NETCDF3_64BIT_OFFSET);
  }

  /*
   * rewrite entire file
   * private void rewrite() throws IOException {
   * // close existing file, rename and open as read-only
   * spiw.flush();
   * spiw.close();
   * 
   * File prevFile = new File(location);
   * if (!prevFile.exists()) {
   * return;
   * }
   * 
   * File tmpFile = new File(location + ".tmp");
   * if (tmpFile.exists()) {
   * boolean ok = tmpFile.delete();
   * if (!ok)
   * log.warn("rewrite unable to delete {}", tmpFile.getPath());
   * }
   * if (!prevFile.renameTo(tmpFile)) {
   * throw new RuntimeException("Cant rename " + prevFile.getAbsolutePath() + " to " + tmpFile.getAbsolutePath());
   * }
   * 
   * NetcdfFile oldFile = NetcdfFiles.open(tmpFile.getPath());
   * 
   * // create new file with current set of objects
   * spiw.create(location, ncfile, extraHeaderBytes, preallocateSize, isLargeFile);
   * spiw.setFill(fill);
   * // isClosed = false;
   * 
   * FileCopier fileWriter2 = FileCopier.create(null, this);
   * for (Variable v : ncfile.getVariables()) {
   * String oldVarName = v.getFullName();
   * Variable oldVar = oldFile.findVariable(oldVarName);
   * if (oldVar != null) {
   * fileWriter2.copyAll(oldVar, v);
   * } else if (varRenameMap.containsKey(oldVarName)) {
   * // var name has changed in ncfile - use the varRenameMap to find
   * // the correct variable name to request from oldFile
   * String realOldVarName = varRenameMap.get(oldVarName);
   * oldVar = oldFile.findVariable(realOldVarName);
   * if (oldVar != null) {
   * fileWriter2.copyAll(oldVar, v);
   * }
   * } else {
   * String message = "Cannot find variable " + oldVarName + " to copy to new file.";
   * log.warn(message);
   * }
   * }
   * 
   * // delete old
   * oldFile.close();
   * if (!tmpFile.delete()) {
   * throw new RuntimeException("Cant delete " + tmpFile.getAbsolutePath());
   * }
   * }
   */

  ////////////////////////////////////////////
  //// use these calls to write data to the file

  /**
   * Write data to the named variable, origin assumed to be 0.
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

  public void write(String varName, int[] origin, Array values) throws IOException, InvalidRangeException {
    write(findVariable(varName), origin, values);
  }

  /**
   * Write data to the named variable.
   *
   * @param v variable to write to
   * @param origin offset within the variable to start writing.
   * @param values write this array; must be same type and rank as Variable
   * @throws IOException if I/O error
   * @throws InvalidRangeException if values Array has illegal shape
   */
  public void write(Variable v, int[] origin, Array values) throws IOException, InvalidRangeException {
    spiw.writeData(v, new Section(origin, values.getShape()), values);
    v.invalidateCache();
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

  /** Flush anything written to disk. */
  public void flush() throws IOException {
    spiw.flush();
  }

  /** close the file. */
  @Override
  public synchronized void close() throws IOException {
    if (spiw != null) {
      flush();
      spiw.close();
      spiw = null;
    }
  }

  /** Abort writing to this file. The file is closed. */
  public void abort() throws IOException {
    if (spiw != null) {
      spiw.close();
      spiw = null;
    }
  }
}
