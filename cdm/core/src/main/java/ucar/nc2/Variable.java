/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;
import ucar.nc2.util.rc.RC;
import java.io.OutputStream;
import java.util.*;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * A Variable is a logical container for data. It has a dataType, a set of Dimensions that define its array shape,
 * and optionally a set of Attributes.
 * <p/>
 * The data is a multidimensional array of primitive types, Strings, or Structures.
 * Data access is done through the read() methods, which return a memory resident Array.
 * <p>
 * Immutable if setImmutable() was called.
 * TODO Variable will be immutable in 6.
 *
 * @author caron
 * @see ucar.ma2.Array
 * @see ucar.ma2.DataType
 */
public class Variable implements VariableSimpleIF, ProxyReader {
  /**
   * Globally permit or prohibit caching. For use during testing and debugging.
   * <p>
   * A {@code true} value for this field does not indicate whether a Variable
   * {@link #isCaching() is caching}, only that it's <i>permitted</i> to cache.
   */
  public static boolean permitCaching = true;

  public static int defaultSizeToCache = 4000; // bytes cache any variable whose size() < defaultSizeToCache
  public static int defaultCoordsSizeToCache = 40 * 1000; // bytes cache coordinate variable whose size() <
                                                          // defaultSizeToCache
  protected static boolean debugCaching;
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Variable.class);

  /**
   * Get the data type of the Variable.
   */
  public DataType getDataType() {
    return dataType;
  }

  /**
   * Get the shape: length of Variable in each dimension.
   * A scalar (rank 0) will have an int[0] shape.
   *
   * @return int array whose length is the rank of this Variable
   *         and whose values equal the length of that Dimension.
   */
  public int[] getShape() {
    int[] result = new int[shape.length]; // optimization over clone()
    System.arraycopy(shape, 0, result, 0, shape.length);
    return result;
  }

  /**
   * Get the size of the ith dimension
   *
   * @param index which dimension
   * @return size of the ith dimension
   */
  public int getShape(int index) {
    return shape[index];
  }

  /**
   * Get the total number of elements in the Variable.
   * If this is an unlimited Variable, will use the current number of elements.
   * If this is a Sequence, will return 1.
   * If variable length, will skip vlen dimensions
   *
   * @return total number of elements in the Variable.
   */
  public long getSize() {
    long size = 1;
    for (int aShape : shape) {
      if (aShape >= 0)
        size *= aShape;
    }
    return size;
  }

  /**
   * Get the number of bytes for one element of this Variable.
   * For Variables of primitive type, this is equal to getDataType().getSize().
   * Variables of String type dont know their size, so what they return is undefined.
   * Variables of Structure type return the total number of bytes for all the members of
   * one Structure, plus possibly some extra padding, depending on the underlying format.
   * Variables of Sequence type return the number of bytes of one element.
   *
   * @return total number of bytes for the Variable
   */
  public int getElementSize() {
    return elementSize;
  }

  /** Get the number of dimensions of the Variable, aka the rank. */
  public int getRank() {
    return shape.length;
  }

  /**
   * Get the parent group, or if null, the root group.
   * 
   * @deprecated Will go away in ver6, shouldnt be needed when builders are used.
   */
  @Deprecated
  public Group getParentGroupOrRoot() {
    Group g = this.getParentGroup();
    if (g == null) {
      g = ncfile.getRootGroup();
      // super.setParentGroup(g); // TODO: WTF?
    }
    return g;
  }

  /**
   * Is this variable metadata?. True if its values need to be included explicitly in NcML output.
   *
   * @return true if Variable values need to be included in NcML
   */
  public boolean isMetadata() {
    return cache != null && cache.isMetadata;
  }

  /**
   * Whether this is a scalar Variable (rank == 0).
   *
   * @return true if Variable has rank 0
   */
  public boolean isScalar() {
    return getRank() == 0;
  }

  /**
   * Does this variable have a variable length dimension?
   * If so, it has as one of its dimensions Dimension.VLEN.
   *
   * @return true if Variable has a variable length dimension?
   */
  public boolean isVariableLength() {
    return isVariableLength;
  }

  /**
   * Can this variable's size grow?.
   * This is equivalent to saying at least one of its dimensions is unlimited.
   *
   * @return boolean true iff this variable can grow
   */
  public boolean isUnlimited() {
    for (Dimension d : dimensions) {
      if (d.isUnlimited())
        return true;
    }
    return false;
  }

  /**
   * Get the list of dimensions used by this variable.
   * The most slowly varying (leftmost for Java and C programmers) dimension is first.
   * For scalar variables, the list is empty.
   *
   * @return List<Dimension>, will be ImmutableList in ver 6.
   */
  public ImmutableList<Dimension> getDimensions() {
    return ImmutableList.copyOf(dimensions);
  }

  /**
   * Get the ith dimension.
   *
   * @param i index of the dimension.
   * @return requested Dimension, or null if i is out of bounds.
   */
  public Dimension getDimension(int i) {
    if ((i < 0) || (i >= getRank()))
      return null;
    return dimensions.get(i);
  }

  /**
   * Get the list of Dimension names, space delineated.
   *
   * @return Dimension names, space delineated
   */
  public String getDimensionsString() {
    return Dimensions.makeDimensionsString(dimensions);
  }

  /**
   * Find the index of the named Dimension in this Variable.
   *
   * @param name the name of the dimension
   * @return the index of the named Dimension, or -1 if not found.
   */
  public int findDimensionIndex(String name) {
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension d = dimensions.get(i);
      if (name.equals(d.getShortName()))
        return i;
    }
    return -1;
  }

  /**
   * Get the description of the Variable.
   * Default is to use CDM.LONG_NAME attribute value. If not exist, look for "description", "title", or
   * "standard_name" attribute value (in that order).
   *
   * @return description, or null if not found.
   */
  public String getDescription() {
    String desc = null;
    Attribute att = attributes.findAttributeIgnoreCase(CDM.LONG_NAME);
    if ((att != null) && att.isString())
      desc = att.getStringValue();

    if (desc == null) {
      att = attributes.findAttributeIgnoreCase("description");
      if ((att != null) && att.isString())
        desc = att.getStringValue();
    }

    if (desc == null) {
      att = attributes.findAttributeIgnoreCase(CDM.TITLE);
      if ((att != null) && att.isString())
        desc = att.getStringValue();
    }

    if (desc == null) {
      att = attributes.findAttributeIgnoreCase(CF.STANDARD_NAME);
      if ((att != null) && att.isString())
        desc = att.getStringValue();
    }

    return desc;
  }

  /**
   * Get the Unit String for the Variable.
   * Looks for the CDM.UNITS attribute value
   *
   * @return unit string, or null if not found.
   */
  @Nullable
  public String getUnitsString() {
    String units = null;
    Attribute att = attributes().findAttributeIgnoreCase(CDM.UNITS);
    if ((att != null) && att.isString()) {
      units = att.getStringValue();
      if (units != null)
        units = units.trim();
    }
    return units;
  }

  /**
   * Get shape as an List of Range objects.
   * The List is immutable.
   *
   * @return List of Ranges, one for each Dimension.
   */
  public ImmutableList<Range> getRanges() {
    // Ok to use Immutable as there are no nulls.
    return ImmutableList.copyOf(getShapeAsSection().getRanges());
  }

  /**
   * Get shape as a Section object.
   *
   * @return Section containing List<Range>, one for each Dimension.
   */
  public Section getShapeAsSection() {
    if (shapeAsSection == null) {
      shapeAsSection = Dimensions.makeSectionFromDimensions(this.dimensions).build();
    }
    return shapeAsSection;
  }

  /** @deprecated Use Variable.builder() */
  @Deprecated
  public ProxyReader getProxyReader() {
    return proxyReader;
  }

  /** @deprecated Use Variable.builder() */
  @Deprecated
  public void setProxyReader(ProxyReader proxyReader) {
    this.proxyReader = proxyReader;
  }

  /**
   * Create a new Variable that is a logical subsection of this Variable.
   * No data is read until a read method is called on it.
   *
   * @param ranges List of type ucar.ma2.Range, with size equal to getRank().
   *        Each Range corresponds to a Dimension, and specifies the section of data to read in that Dimension.
   *        A Range object may be null, which means use the entire dimension.
   * @return a new Variable which is a logical section of this Variable.
   * @throws InvalidRangeException if shape and range list dont match
   */
  public Variable section(List<Range> ranges) throws InvalidRangeException {
    return section(new Section(ranges, shape).makeImmutable());
  }

  /**
   * Create a new Variable that is a logical subsection of this Variable.
   * No data is read until a read method is called on it.
   *
   * @param subsection Section of this variable.
   *        Each Range in the section corresponds to a Dimension, and specifies the section of data to read in that
   *        Dimension.
   *        A Range object may be null, which means use the entire dimension.
   * @return a new Variable which is a logical section of this Variable.
   * @throws InvalidRangeException if section not compatible with shape
   */
  public Variable section(Section subsection) throws InvalidRangeException {

    subsection = Section.fill(subsection, shape);

    // create a copy of this variable with a proxy reader
    Variable sectionV = copy(); // subclasses must override
    sectionV.setProxyReader(new SectionReader(this, subsection));
    sectionV.shape = subsection.getShape();
    sectionV.createNewCache(); // dont share the cache
    sectionV.setCaching(false); // dont cache

    // replace dimensions if needed !! LOOK not shared
    int[] shape = subsection.getShape();
    List<Dimension> dimensions = new ArrayList<>();
    for (int i = 0; i < getRank(); i++) {
      Dimension oldD = getDimension(i);
      Dimension newD = (oldD.getLength() == shape[i]) ? oldD
          : Dimension.builder().setName(oldD.getShortName()).setIsUnlimited(oldD.isUnlimited()).setIsShared(false)
              .setLength(shape[i]).build();
      dimensions.add(newD);
    }
    sectionV.dimensions = dimensions;
    return sectionV;
  }

  /**
   * Create a new Variable that is a logical slice of this Variable, by
   * fixing the specified dimension at the specified index value. This reduces rank by 1.
   * No data is read until a read method is called on it.
   *
   * @param dim which dimension to fix
   * @param value at what index value
   * @return a new Variable which is a logical slice of this Variable.
   * @throws InvalidRangeException if dimension or value is illegal
   */
  public Variable slice(int dim, int value) throws InvalidRangeException {
    if ((dim < 0) || (dim >= shape.length))
      throw new InvalidRangeException("Slice dim invalid= " + dim);

    // ok to make slice of record dimension with length 0
    boolean recordSliceOk = false;
    if ((dim == 0) && (value == 0)) {
      Dimension d = getDimension(0);
      recordSliceOk = d.isUnlimited();
    }

    // otherwise check slice in range
    if (!recordSliceOk) {
      if ((value < 0) || (value >= shape[dim]))
        throw new InvalidRangeException("Slice value invalid= " + value + " for dimension " + dim);
    }

    // create a copy of this variable with a proxy reader
    Variable.Builder sliceV = this.toBuilder(); // subclasses override toBuilder()
    Section.Builder slice = Section.builder().appendRanges(getShape());
    slice.replaceRange(dim, new Range(value, value));
    sliceV.setProxyReader(new SliceReader(this, dim, slice.build()));
    sliceV.resetCache(); // dont share the cache
    sliceV.setCaching(false); // dont cache

    // remove that dimension - reduce rank
    sliceV.dimensions.remove(dim);
    return sliceV.build(getParentGroupOrRoot());
  }

  /**
   * Create a new Variable that is a logical view of this Variable, by
   * eliminating the specified dimension(s) of length 1.
   * No data is read until a read method is called on it.
   *
   * @param dims list of dimensions of length 1 to reduce
   * @return a new Variable which is a logical slice of this Variable.
   */
  public Variable reduce(List<Dimension> dims) {
    List<Integer> dimIdx = new ArrayList<>(dims.size());
    for (Dimension d : dims) {
      assert dimensions.contains(d);
      assert d.getLength() == 1;
      dimIdx.add(dimensions.indexOf(d));
    }

    // create a copy of this variable with a proxy reader
    Variable.Builder sliceV = this.toBuilder(); // subclasses override toBuilder()
    sliceV.setProxyReader(new ReduceReader(this, dimIdx));
    sliceV.resetCache(); // dont share the cache
    sliceV.setCaching(false); // dont cache

    // remove dimension(s) - reduce rank
    for (Dimension d : dims)
      sliceV.dimensions.remove(d);
    return sliceV.build(getParentGroupOrRoot());
  }

  /** @deprecated Use Variable.toBuilder() */
  @Deprecated
  protected Variable copy() {
    return new Variable(this);
  }

  /** Get the NetcdfFile that this variable is contained in. May be null. */
  @Nullable
  public NetcdfFile getNetcdfFile() {
    return ncfile;
  }

  @Nullable
  public String getFileTypeId() {
    return ncfile == null ? null : ncfile.getFileTypeId();
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Lookup the enum string for this value.
   * Can only be called on enum types, where dataType.isEnum() is true.
   *
   * @param val the integer value of this enum
   * @return the String value
   */
  @Nullable
  public String lookupEnumString(int val) {
    if (!dataType.isEnum())
      throw new UnsupportedOperationException("Can only call Variable.lookupEnumVal() on enum types");
    return enumTypedef.lookupEnumString(val);
  }

  /**
   * Public by accident.
   *
   * @param enumTypedef set the EnumTypedef, only use if getDataType.isEnum()
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setEnumTypedef(EnumTypedef enumTypedef) {
    if (!dataType.isEnum())
      throw new UnsupportedOperationException("Can only call Variable.setEnumTypedef() on enum types");
    this.enumTypedef = enumTypedef;
  }

  /**
   * Get the EnumTypedef, only use if getDataType.isEnum()
   *
   * @return enumTypedef or null if !getDataType.isEnum()
   */
  public EnumTypedef getEnumTypedef() {
    return enumTypedef;
  }

  //////////////////////////////////////////////////////////////////////////////
  // IO
  // implementation notes to subclassers
  // all other calls use them, so override only these:
  // _read()
  // _read(Section section)
  // _readNestedData(Section section, boolean flatten)

  /**
   * Read a section of the data for this Variable and return a memory resident Array.
   * The Array has the same element type as the Variable, and the requested shape.
   * Note that this does not do rank reduction, so the returned Array has the same rank
   * as the Variable. Use Array.reduce() for rank reduction.
   * <p/>
   * <code>assert(origin[ii] + shape[ii]*stride[ii] <= Variable.shape[ii]); </code>
   * <p/>
   *
   * @param origin int array specifying the starting index. If null, assume all zeroes.
   * @param shape int array specifying the extents in each dimension.
   *        This becomes the shape of the returned Array.
   * @return the requested data in a memory-resident Array
   */
  public Array read(int[] origin, int[] shape) throws IOException, InvalidRangeException {
    if ((origin == null) && (shape == null))
      return read();

    if (origin == null)
      return read(new Section(shape));

    if (shape == null)
      return read(new Section(origin, this.shape));

    return read(new Section(origin, shape));
  }

  /**
   * Read data section specified by a "section selector", and return a memory resident Array. Uses
   * Fortran 90 array section syntax.
   *
   * @param sectionSpec specification string, eg "1:2,10,:,1:100:10". May optionally have ().
   * @return the requested data in a memory-resident Array
   * @see ucar.ma2.Section for sectionSpec syntax
   */
  public Array read(String sectionSpec) throws IOException, InvalidRangeException {
    return read(new Section(sectionSpec));
  }

  /**
   * Read a section of the data for this Variable from the netcdf file and return a memory resident Array.
   *
   * @param ranges list of Range specifying the section of data to read.
   * @return the requested data in a memory-resident Array
   * @throws IOException if error
   * @throws InvalidRangeException if ranges is invalid
   * @see #read(Section)
   */
  public Array read(List<Range> ranges) throws IOException, InvalidRangeException {
    if (null == ranges)
      return _read();

    return read(new Section(ranges));
  }

  /**
   * Read a section of the data for this Variable from the netcdf file and return a memory resident Array.
   * The Array has the same element type as the Variable, and the requested shape.
   * Note that this does not do rank reduction, so the returned Array has the same rank
   * as the Variable. Use Array.reduce() for rank reduction.
   * <p/>
   * If the Variable is a member of an array of Structures, this returns only the variable's data
   * in the first Structure, so that the Array shape is the same as the Variable.
   * To read the data in all structures, use ncfile.readSectionSpec().
   * <p/>
   * Note this only allows you to specify a subset of this variable.
   * If the variable is nested in a array of structures and you want to subset that, use
   * NetcdfFile.read(String sectionSpec, boolean flatten);
   *
   * @param section list of Range specifying the section of data to read.
   *        Must be null or same rank as variable.
   *        If list is null, assume all data.
   *        Each Range corresponds to a Dimension. If the Range object is null, it means use the entire dimension.
   * @return the requested data in a memory-resident Array
   * @throws IOException if error
   * @throws InvalidRangeException if section is invalid
   */
  public Array read(ucar.ma2.Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException {
    return (section == null) ? _read() : _read(Section.fill(section, shape));
  }

  /**
   * Read all the data for this Variable and return a memory resident Array.
   * The Array has the same element type and shape as the Variable.
   * <p/>
   * If the Variable is a member of an array of Structures, this returns only the variable's data
   * in the first Structure, so that the Array shape is the same as the Variable.
   * To read the data in all structures, use ncfile.readSection().
   *
   * @return the requested data in a memory-resident Array.
   */
  public Array read() throws IOException {
    return _read();
  }

  ///// scalar reading

  /**
   * Get the value as a byte for a scalar Variable. May also be one-dimensional of length 1.
   *
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to byte
   */
  public byte readScalarByte() throws IOException {
    Array data = _readScalarData();
    return data.getByte(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a short for a scalar Variable. May also be one-dimensional of length 1.
   *
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to short
   */
  public short readScalarShort() throws IOException {
    Array data = _readScalarData();
    return data.getShort(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a int for a scalar Variable. May also be one-dimensional of length 1.
   *
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to int
   */
  public int readScalarInt() throws IOException {
    Array data = _readScalarData();
    return data.getInt(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a long for a scalar Variable. May also be one-dimensional of length 1.
   *
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable
   * @throws ForbiddenConversionException if data type not convertible to long
   */
  public long readScalarLong() throws IOException {
    Array data = _readScalarData();
    return data.getLong(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a float for a scalar Variable. May also be one-dimensional of length 1.
   *
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to float
   */
  public float readScalarFloat() throws IOException {
    Array data = _readScalarData();
    return data.getFloat(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a double for a scalar Variable. May also be one-dimensional of length 1.
   *
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar Variable or one-dimensional of length 1.
   * @throws ForbiddenConversionException if data type not convertible to double
   */
  public double readScalarDouble() throws IOException {
    Array data = _readScalarData();
    return data.getDouble(Index.scalarIndexImmutable);
  }

  /**
   * Get the value as a String for a scalar Variable. May also be one-dimensional of length 1.
   * May also be one-dimensional of type CHAR, which wil be turned into a scalar String.
   *
   * @throws IOException if theres an IO Error
   * @throws UnsupportedOperationException if not a scalar or one-dimensional.
   * @throws ClassCastException if data type not DataType.STRING or DataType.CHAR.
   */
  public String readScalarString() throws IOException {
    Array data = _readScalarData();
    if (dataType == DataType.STRING)
      return (String) data.getObject(Index.scalarIndexImmutable);
    else if (dataType == DataType.CHAR) {
      ArrayChar dataC = (ArrayChar) data;
      return dataC.getString();
    } else
      throw new IllegalArgumentException("readScalarString not STRING or CHAR " + getFullName());
  }

  /** @deprecated use readScalarXXXX */
  @Deprecated
  protected Array getScalarData() throws IOException {
    Array scalarData = (cache.data != null) ? cache.data : read();
    scalarData = scalarData.reduce();

    if ((scalarData.getRank() == 0) || ((scalarData.getRank() == 1) && dataType == DataType.CHAR))
      return scalarData;
    throw new java.lang.UnsupportedOperationException("not a scalar variable =" + this);
  }

  ///////////////
  // internal reads: all other calls go through these.
  // subclasses must override, so that NetcdfDataset wrapping will work.

  // non-structure-member Variables.

  protected Array _read() throws IOException {
    // caching overrides the proxyReader
    // check if already cached
    if (cache.data != null) {
      if (debugCaching)
        System.out.println("got data from cache " + getFullName());
      return cache.data.copy();
    }

    Array data = proxyReader.reallyRead(this, null);

    // optionally cache it
    if (isCaching()) {
      setCachedData(data);
      if (debugCaching)
        System.out.println("cache " + getFullName());
      return cache.data.copy(); // dont let users get their nasty hands on cached data
    } else {
      return data;
    }
  }

  // section of non-structure-member Variable
  // assume filled, validated Section
  protected Array _read(Section section) throws IOException, InvalidRangeException {
    // check if its really a full read
    if ((null == section) || section.computeSize() == getSize())
      return _read();

    // full read was cached
    if (isCaching()) {
      if (cache.data == null) {
        setCachedData(_read()); // read and cache entire array
        if (debugCaching)
          System.out.println("cache " + getFullName());
      }
      if (debugCaching)
        System.out.println("got data from cache " + getFullName());
      return cache.data.sectionNoReduce(section.getRanges()).copy(); // subset it, return copy
    }

    return proxyReader.reallyRead(this, section, null);
  }

  protected Array _readScalarData() throws IOException {
    Array scalarData = read();
    scalarData = scalarData.reduce();

    if ((scalarData.getRank() == 0) || ((scalarData.getRank() == 1) && dataType == DataType.CHAR))
      return scalarData;
    throw new java.lang.UnsupportedOperationException("not a scalar variable =" + this);
  }

  /**
   * public by accident, do not call directly.
   *
   * @return Array
   * @throws IOException on error
   */
  @Override
  public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    if (isMemberOfStructure()) { // LOOK should be UnsupportedOperationException ??
      List<String> memList = new ArrayList<>();
      memList.add(this.getShortName());
      Structure s = getParentStructure().select(memList);
      ArrayStructure as = (ArrayStructure) s.read();
      return as.extractMemberArray(as.findMember(getShortName()));
    }

    try {
      return ncfile.readData(this, getShapeAsSection());
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage()); // cant happen haha
    }
  }

  /**
   * public by accident, do not call directly.
   *
   * @return Array
   * @throws IOException on error
   */
  @Override
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    if (isMemberOfStructure()) {
      throw new UnsupportedOperationException("Cannot directly read section of Member Variable=" + getFullName());
    }
    // read just this section
    return ncfile.readData(this, section);
  }

  /** Read variable data to a stream. Support for NcStreamWriter. */
  public long readToStream(Section section, OutputStream out) throws IOException, InvalidRangeException {
    if ((ncfile == null) || hasCachedData())
      return IospHelper.copyToOutputStream(read(section), out);

    return ncfile.readToOutputStream(this, section, out);
  }

  /**
   * Get its containing Group.
   * LOOK if you relied on Group being set during construction, use getParentGroupOrRoot().
   */
  @SuppressWarnings("deprecated")
  public Group getParentGroup() {
    return this.parentGroup;
  }

  /**
   * Get its parent structure, or null if not in structure
   *
   * @return parent structure
   */
  @Nullable
  public Structure getParentStructure() {
    return this.parentStructure;
  }

  /**
   * Test for presence of parent Structure.
   */
  public boolean isMemberOfStructure() {
    return this.parentStructure != null;
  }

  /**
   * Get the full name of this Variable.
   * Certain characters are backslash escaped (see NetcdfFiles.getFullName(Variable))
   *
   * @return full name with backslash escapes
   */
  public String getFullName() {
    return NetcdfFiles.makeFullName(this);
  }

  public String getShortName() {
    return this.shortName;
  }

  /**
   * Get the display name plus the dimensions, eg 'float name(dim1, dim2)'
   *
   * @return display name plus the dimensions
   */
  public String getNameAndDimensions() {
    Formatter buf = new Formatter();
    getNameAndDimensions(buf, true, false);
    return buf.toString();
  }

  /**
   * Get the display name plus the dimensions, eg 'float name(dim1, dim2)'
   *
   * @param strict strictly comply with ncgen syntax, with name escaping. otherwise, get extra info, no escaping
   * @return display name plus the dimensions
   */
  public String getNameAndDimensions(boolean strict) {
    Formatter buf = new Formatter();
    getNameAndDimensions(buf, false, strict);
    return buf.toString();
  }

  /**
   * Get the display name plus the dimensions, eg 'name(dim1, dim2)'
   *
   * @param buf add info to this StringBuilder
   * @deprecated use CDLWriter
   */
  @Deprecated
  public void getNameAndDimensions(StringBuilder buf) {
    getNameAndDimensions(buf, true, false);
  }

  /**
   * Get the display name plus the dimensions, eg 'name(dim1, dim2)'
   *
   * @param buf add info to this StringBuffer
   * @deprecated use getNameAndDimensions(StringBuilder buf)
   */
  @Deprecated
  public void getNameAndDimensions(StringBuffer buf) {
    Formatter proxy = new Formatter();
    getNameAndDimensions(proxy, true, false);
    buf.append(proxy);
  }

  /**
   * Add display name plus the dimensions to the StringBuffer
   *
   * @param buf add info to this
   * @param useFullName use full name else short name. strict = true implies short name
   * @param strict strictly comply with ncgen syntax, with name escaping. otherwise, get extra info, no escaping
   * @deprecated use CDLWriter
   */
  @Deprecated
  public void getNameAndDimensions(StringBuilder buf, boolean useFullName, boolean strict) {
    Formatter proxy = new Formatter();
    getNameAndDimensions(proxy, useFullName, strict);
    buf.append(proxy);
  }

  /**
   * Add display name plus the dimensions to the Formatter
   *
   * @param buf add info to this
   * @param useFullName use full name else short name. strict = true implies short name
   * @param strict strictly comply with ncgen syntax, with name escaping. otherwise, get extra info, no escaping
   */
  public void getNameAndDimensions(Formatter buf, boolean useFullName, boolean strict) {
    useFullName = useFullName && !strict;
    String name = useFullName ? getFullName() : getShortName();
    if (strict)
      name = NetcdfFiles.makeValidCDLName(getShortName());
    buf.format("%s", name);

    if (shape != null) {
      if (getRank() > 0)
        buf.format("(");
      for (int i = 0; i < dimensions.size(); i++) {
        Dimension myd = dimensions.get(i);
        String dimName = myd.getShortName();
        if ((dimName != null) && strict)
          dimName = NetcdfFiles.makeValidCDLName(dimName);
        if (i != 0)
          buf.format(", ");
        if (myd.isVariableLength()) {
          buf.format("*");
        } else if (myd.isShared()) {
          if (!strict)
            buf.format("%s=%d", dimName, myd.getLength());
          else
            buf.format("%s", dimName);
        } else {
          if (dimName != null) {
            buf.format("%s=", dimName);
          }
          buf.format("%d", myd.getLength());
        }
      }
      if (getRank() > 0)
        buf.format(")");
    }
  }

  public String toString() {
    return writeCDL(false, false);
  }

  /**
   * CDL representation of a Variable.
   *
   * @param useFullName use full name, else use short name
   * @param strict strictly comply with ncgen syntax
   * @return CDL representation of the Variable.
   * @deprecated use CDLWriter
   */
  @Deprecated
  public String writeCDL(boolean useFullName, boolean strict) {
    Formatter buf = new Formatter();
    writeCDL(buf, new Indent(2), useFullName, strict);
    return buf.toString();
  }

  /** @deprecated use CDLWriter */
  @Deprecated
  protected void writeCDL(Formatter buf, Indent indent, boolean useFullName, boolean strict) {
    buf.format("%s", indent);
    if (dataType == null)
      buf.format("Unknown");
    else if (dataType.isEnum()) {
      if (enumTypedef == null)
        buf.format("enum UNKNOWN");
      else
        buf.format("enum %s", NetcdfFiles.makeValidCDLName(enumTypedef.getShortName()));
    } else
      buf.format("%s", dataType.toString());

    // if (isVariableLength) buf.append("(*)"); // LOOK
    buf.format(" ");
    getNameAndDimensions(buf, useFullName, strict);
    buf.format(";");
    if (!strict)
      buf.format(extraInfo());
    buf.format("%n");

    indent.incr();
    for (Attribute att : attributes()) {
      if (Attribute.isspecial(att))
        continue;
      buf.format("%s", indent);
      att.writeCDL(buf, strict, getShortName());
      buf.format(";");
      if (!strict && (att.getDataType() != DataType.STRING))
        buf.format(" // %s", att.getDataType());
      buf.format("%n");
    }
    indent.decr();
  }

  /**
   * String representation of Variable and its attributes.
   */
  public String toStringDebug() {
    Formatter f = new Formatter();
    f.format("Variable %s", getFullName());
    if (ncfile != null) {
      f.format(" in file %s", getDatasetLocation());
      String extra = ncfile.toStringDebug(this);
      if (extra != null)
        f.format(" %s", extra);
    }
    return f.toString();
  }

  private static boolean showSize = false;

  protected String extraInfo() {
    return showSize ? " // " + getElementSize() + " " + getSize() : "";
  }

  /** The location of the dataset this belongs to. Labeling purposes only. */
  public String getDatasetLocation() {
    if (ncfile != null)
      return ncfile.getLocation();
    return "N/A";
  }

  /**
   * Instances which have same content are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo)
      return true;
    if (!(oo instanceof Variable))
      return false;
    Variable o = (Variable) oo;

    if (!getShortName().equals(o.getShortName()))
      return false;
    if (isScalar() != o.isScalar())
      return false;
    if (getDataType() != o.getDataType())
      return false;
    if (!getParentGroup().equals(o.getParentGroup()))
      return false;
    if ((getParentStructure() != null) && !getParentStructure().equals(o.getParentStructure()))
      return false;
    if (isVariableLength() != o.isVariableLength())
      return false;
    if (dimensions.size() != o.getDimensions().size())
      return false;
    for (int i = 0; i < dimensions.size(); i++)
      if (!getDimension(i).equals(o.getDimension(i)))
        return false;

    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getShortName().hashCode();
      if (isScalar())
        result++;
      result = 37 * result + getDataType().hashCode();
      result = 37 * result + getParentGroup().hashCode();
      if (getParentStructure() != null)
        result = 37 * result + getParentStructure().hashCode();
      if (isVariableLength)
        result++;
      result = 37 * result + dimensions.hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  protected int hashCode;

  /**
   * Sort by name
   */
  public int compareTo(VariableSimpleIF o) {
    return getShortName().compareTo(o.getShortName());
  }

  /////////////////////////////////////////////////////////////////////////////

  /** @deprecated Use Variable.builder() */
  @Deprecated
  protected Variable() {}

  /**
   * Create a Variable. Also must call setDataType() and setDimensions()
   *
   * @param ncfile the containing NetcdfFile.
   * @param group the containing group; if null, use rootGroup
   * @param parent parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public Variable(NetcdfFile ncfile, Group group, Structure parent, String shortName) {
    this.shortName = shortName;
    this.ncfile = ncfile;
    if (parent == null) {
      setParentGroup((group == null) ? ncfile.getRootGroup() : group);
    } else {
      this.parentStructure = parent;
    }
    attributes = new AttributeContainerMutable(shortName);
  }

  /**
   * Create a Variable. Also must call setDataType() and setDimensions()
   *
   * @param ncfile the containing NetcdfFile.
   * @param group the containing group; if null, use rootGroup
   * @param parent parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   * @param dtype the Variable's DataType
   * @param dims space delimited list of dimension names. may be null or "" for scalars.
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public Variable(NetcdfFile ncfile, Group group, Structure parent, String shortName, DataType dtype, String dims) {
    this(ncfile, group, parent, shortName, dtype, (List<Dimension>) null);
    if (group == null)
      group = ncfile.getRootGroup();
    setDimensions(Dimensions.makeDimensionsList(group::findDimension, dims));
  }

  /**
   * Create a Variable. Also must call setDataType() and setDimensions()
   *
   * @param ncfile the containing NetcdfFile.
   * @param group the containing group; if null, use rootGroup
   * @param parent parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   * @param dtype the Variable's DataType
   * @param dims dimension names.
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public Variable(NetcdfFile ncfile, Group group, Structure parent, String shortName, DataType dtype,
      List<Dimension> dims) {
    this(ncfile, group, parent, shortName);
    setDataType(dtype);
    setDimensions(dims);
  }


  /**
   * Copy constructor.
   * The returned Variable is mutable.
   * It shares the cache object and the iosp Object, attributes and dimensions with the original.
   * Does not share the proxyReader.
   * Use for section, slice, "logical views" of original variable.
   *
   * @param from copy from this Variable.
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public Variable(Variable from) {
    this.shortName = from.getShortName();
    this.attributes = new AttributeContainerMutable(from.getShortName(), from.attributes());
    this.cache = from.cache; // caller should do createNewCache() if dont want to share
    setDataType(from.getDataType());
    this.dimensions = new ArrayList<>(from.dimensions); // dimensions are shared
    this.elementSize = from.getElementSize();
    this.enumTypedef = from.enumTypedef;
    setParentGroup(from.parentGroup);
    this.parentStructure = from.getParentStructure();
    this.isVariableLength = from.isVariableLength;
    this.ncfile = from.ncfile;
    this.shape = from.getShape();
    this.sizeToCache = from.sizeToCache;
    this.spiObject = from.spiObject;
  }

  /**
   * Set the data type
   *
   * @param dataType set to this value
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setDataType(DataType dataType) {
    this.dataType = dataType;
    this.elementSize = getDataType().getSize();
  }

  /**
   * Set the short name, converting to valid CDM object name if needed.
   *
   * @param shortName set to this value
   * @return valid CDM object name
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public String setName(String shortName) {
    this.shortName = shortName;
    return getShortName();
  }

  /**
   * Set the parent group.
   *
   * @param group set to this value
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setParentGroup(Group group) {
    this.parentGroup = group;
  }

  @Deprecated
  public void setParentStructure(Structure struct) {
    this.parentStructure = struct;
  }

  /**
   * Set the element size. Usually elementSize is determined by the dataType,
   * use this only for exceptional cases.
   *
   * @param elementSize set to this value
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setElementSize(int elementSize) {
    this.elementSize = elementSize;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Attributes

  /** The attributes contained by this Variable. */
  @Override
  public AttributeContainer attributes() {
    return attributes;
  }

  /** Find the attribute by name, return null if not exist */
  @Nullable
  public Attribute findAttribute(String name) {
    return attributes.findAttribute(name);
  }

  /**
   * Find a String-valued Attribute by name (ignore case), return the String value of the Attribute.
   *
   * @return the attribute value, or defaultValue if not found
   */
  public String findAttributeString(String attName, String defaultValue) {
    return attributes.findAttributeString(attName, defaultValue);
  }

  /** @deprecated Use Variable.builder() */
  @Deprecated
  public Attribute addAttribute(Attribute att) {
    return attributes.addAttribute(att);
  }

  ////////////////////////////////////////////////////////////////////////

  /**
   * Set the shape with a list of Dimensions. The Dimensions may be shared or not.
   * Dimensions are in order, slowest varying first. Send a null for a scalar.
   * Technically you can use Dimensions from any group; pragmatically you should only use
   * Dimensions contained in the Variable's parent groups.
   *
   * @param dims list of type ucar.nc2.Dimension
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setDimensions(List<Dimension> dims) {
    this.dimensions = (dims == null) ? new ArrayList<>() : new ArrayList<>(dims);
    resetShape();
  }


  /**
   * Use when dimensions have changed, to recalculate the shape.
   * 
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void resetShape() {
    // if (immutable) throw new IllegalStateException("Cant modify"); LOOK allow this for unlimited dimension updating
    this.shape = new int[dimensions.size()];
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension dim = dimensions.get(i);
      shape[i] = dim.getLength();
      // shape[i] = Math.max(dim.getLength(), 0); // LOOK
      // if (dim.isUnlimited() && (i != 0)) // LOOK only true for Netcdf-3
      // throw new IllegalArgumentException("Unlimited dimension must be outermost");
      if (dim.isVariableLength()) {
        // if (dimensions.size() != 1)
        // throw new IllegalArgumentException("Unknown dimension can only be used in 1 dim array");
        // else
        isVariableLength = true;
      }
    }
    this.shapeAsSection = null; // recalc next time its asked for
  }

  /**
   * Set the dimensions using the dimensions names. The dimension is searched for recursively in the parent groups.
   *
   * @param dimString : whitespace separated list of dimension names, or '*' for Dimension.UNKNOWN, or number for anon
   *        dimension. null or empty String is a scalar.
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setDimensions(String dimString) {
    try {
      setDimensions(Dimensions.makeDimensionsList(getParentGroupOrRoot()::findDimension, dimString));
      resetShape();
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException("Variable " + getFullName() + " setDimensions = '" + dimString + "' FAILED: "
          + e.getMessage() + " file = " + getDatasetLocation());
    }
  }

  /**
   * Reset the dimension array. Anonymous dimensions are left alone.
   * Shared dimensions are searched for recursively in the parent groups.
   * 
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void resetDimensions() {
    ArrayList<Dimension> newDimensions = new ArrayList<>();

    for (Dimension dim : dimensions) {
      if (dim.isShared()) {
        Dimension newD = getParentGroupOrRoot().findDimension(dim.getShortName());
        if (newD == null)
          throw new IllegalArgumentException(
              "Variable " + getFullName() + " resetDimensions  FAILED, dim doesnt exist in parent group=" + dim);
        newDimensions.add(newD);
      } else {
        newDimensions.add(dim);
      }
    }
    this.dimensions = newDimensions;
    resetShape();
  }

  /**
   * Set the dimensions using all anonymous (unshared) dimensions
   *
   * @param shape defines the dimension lengths. must be > 0, or -1 for VLEN
   * @throws ucar.ma2.InvalidRangeException if any shape < 1
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setDimensionsAnonymous(int[] shape) throws InvalidRangeException {
    this.dimensions = new ArrayList<>();
    for (int i = 0; i < shape.length; i++) {
      if ((shape[i] < 1) && (shape[i] != -1))
        throw new InvalidRangeException("shape[" + i + "]=" + shape[i] + " must be > 0");
      Dimension anon;
      if (shape[i] == -1) {
        anon = Dimension.VLEN;
        isVariableLength = true;
      } else {
        anon = new Dimension(null, shape[i], false, false, false);
      }

      dimensions.add(anon);
    }
    resetShape();
  }

  /**
   * Set this Variable to be a scalar
   * 
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setIsScalar() {
    this.dimensions = new ArrayList<>();
    resetShape();
  }

  /**
   * Replace a dimension with an equivalent one.
   *
   * @param idx index into dimension array
   * @param dim to set
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setDimension(int idx, Dimension dim) {
    dimensions.set(idx, dim);
    resetShape();
  }

  /** Get immutable service provider opaque object. */
  public Object getSPobject() {
    return spiObject;
  }

  /** @deprecated Do not use. */
  @Deprecated
  public void setSPobject(Object spiObject) {
    this.spiObject = spiObject;
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // caching

  /**
   * If total data size is less than SizeToCache in bytes, then cache.
   *
   * @return size at which caching happens
   */
  public int getSizeToCache() {
    if (sizeToCache >= 0)
      return sizeToCache; // it was set
    return isCoordinateVariable() ? defaultCoordsSizeToCache : defaultSizeToCache;
  }

  /**
   * Set the sizeToCache. If not set, use defaults
   *
   * @param sizeToCache size at which caching happens. < 0 means use defaults
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setSizeToCache(int sizeToCache) {
    this.sizeToCache = sizeToCache;
  }

  /**
   * Set whether to cache or not. Implies that the entire array will be stored, once read.
   * Normally this is set automatically based on size of data.
   *
   * @param caching set if caching.
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setCaching(boolean caching) {
    this.cache.isCaching = caching;
    this.cache.cachingSet = true;
  }

  /**
   * Will this Variable be cached when read.
   * Set externally, or calculated based on total size < sizeToCache.
   * <p>
   * This will always return {@code false} if {@link #permitCaching caching isn't permitted}.
   *
   * @return true is caching
   */
  public boolean isCaching() {
    if (!permitCaching) {
      return false;
    }

    if (!this.cache.cachingSet) {
      cache.isCaching = !isVariableLength && (getSize() * getElementSize() < getSizeToCache());
      if (debugCaching)
        System.out.printf("  cache %s %s %d < %d%n", getFullName(), cache.isCaching, getSize() * getElementSize(),
            getSizeToCache());
      this.cache.cachingSet = true;
    }
    return cache.isCaching;
  }

  /**
   * Note that standalone Ncml caches data values set in the Ncml.
   * So one cannont invalidate those caches.
   * 
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void invalidateCache() {
    cache.data = null;
  }

  /** @deprecated Use Variable.builder() */
  @Deprecated
  public void setCachedData(Array cacheData) {
    setCachedData(cacheData, false);
  }

  /**
   * Set the data cache
   *
   * @param cacheData cache this Array
   * @param isMetadata : synthesized data, set true if must be saved in NcML output (ie data not actually in the file).
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setCachedData(Array cacheData, boolean isMetadata) {
    if ((cacheData != null) && (cacheData.getElementType() != getDataType().getPrimitiveClassType()))
      throw new IllegalArgumentException(
          "setCachedData type=" + cacheData.getElementType() + " incompatible with variable type=" + getDataType());

    this.cache.data = cacheData;
    this.cache.isMetadata = isMetadata;
    this.cache.cachingSet = true;
    this.cache.isCaching = true;
  }

  /**
   * Create a new data cache, use this when you dont want to share the cache.
   */
  public void createNewCache() {
    this.cache = new Cache();
  }

  /**
   * Has data been read and cached.
   * Use only on a Variable, not a subclass.
   *
   * @return true if data is read and cached
   */
  public boolean hasCachedData() {
    return (null != cache.data);
  }

  // this indirection allows us to share the cache among the variable's sections and copies
  protected static class Cache {
    private Array data;
    protected boolean isCaching;
    protected boolean cachingSet;
    private boolean isMetadata;

    private Cache() {}

    public void reset() {
      this.data = null;
      this.isCaching = false;
      this.cachingSet = false;
      this.isMetadata = false;
    }
  }

  ///////////////////////////////////////////////////////////////////////
  // setting variable data values

  /**
   * Generate the list of values from a starting value and an increment.
   * Will reshape to variable if needed.
   *
   * @param npts number of values, must = v.getSize()
   * @param start starting value
   * @param incr increment
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setValues(int npts, double start, double incr) {
    if (npts != getSize())
      throw new IllegalArgumentException("bad npts = " + npts + " should be " + getSize());
    Array data = Array.makeArray(getDataType(), npts, start, incr);
    if (getRank() != 1)
      data = data.reshape(getShape());
    setCachedData(data, true);
  }

  /**
   * Set the data values from a list of Strings.
   *
   * @param values list of Strings
   * @throws IllegalArgumentException if values array not correct size, or values wont parse to the correct type
   * @deprecated Use Variable.builder()
   */
  @Deprecated
  public void setValues(List<String> values) throws IllegalArgumentException {
    Array data = Array.makeArray(getDataType(), values);

    if (data.getSize() != getSize())
      throw new IllegalArgumentException("Incorrect number of values specified for the Variable " + getFullName()
          + " needed= " + getSize() + " given=" + data.getSize());

    if (getRank() != 1) // dont have to reshape for rank 1
      data = data.reshape(getShape());

    setCachedData(data, true);
  }

  ////////////////////////////////////////////////////////////////////////
  // StructureMember - could be a subclass, but that has problems

  /**
   * Get list of Dimensions, including parents if any.
   *
   * @return array of Dimension, rank of v plus all parents.
   * @deprecated use Dimensions.makeDimensionsAll(Variable);
   */
  @Deprecated
  public List<Dimension> getDimensionsAll() {
    List<Dimension> dimsAll = new ArrayList<>();
    addDimensionsAll(dimsAll, this);
    return dimsAll;
  }

  private void addDimensionsAll(List<Dimension> result, Variable v) {
    if (v.isMemberOfStructure())
      addDimensionsAll(result, v.getParentStructure());

    for (int i = 0; i < v.getRank(); i++)
      result.add(v.getDimension(i));
  }

  /** @deprecated use Dimensions.makeDimensionsAll(Variable); */
  @Deprecated
  public int[] getShapeAll() {
    if (getParentStructure() == null)
      return getShape();
    List<Dimension> dimAll = getDimensionsAll();
    int[] shapeAll = new int[dimAll.size()];
    for (int i = 0; i < dimAll.size(); i++)
      shapeAll[i] = dimAll.get(i).getLength();
    return shapeAll;
  }

  /**
   * Calculate if this is a classic coordinate variable: has same name as its first dimension.
   * If type char, must be 2D, else must be 1D.
   *
   * @return true if a coordinate variable.
   */
  public boolean isCoordinateVariable() {
    if ((dataType == DataType.STRUCTURE) || isMemberOfStructure()) // Structures and StructureMembers cant be coordinate
                                                                   // variables
      return false;

    int n = getRank();
    if (n == 1 && dimensions.size() == 1) {
      Dimension firstd = dimensions.get(0);
      if (getShortName().equals(firstd.getShortName())) { // : short names match
        return true;
      }
    }
    if (n == 2 && dimensions.size() == 2) { // two dimensional
      Dimension firstd = dimensions.get(0);
      // must be char valued (really a String)
      return shortName.equals(firstd.getShortName()) && // short names match
          (getDataType() == DataType.CHAR);
    }

    return false;
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // TODO make private final and Immutable in release 6.
  // Physical container for this Variable where the I/O happens. may be null if Variable is self contained.
  private String shortName;
  private Group parentGroup;
  protected Structure parentStructure;
  protected NetcdfFile ncfile;
  protected DataType dataType;
  private EnumTypedef enumTypedef;
  protected List<Dimension> dimensions = new ArrayList<>(5); // TODO immutable in ver 6
  protected AttributeContainerMutable attributes; // TODO immutable in ver 6
  protected ProxyReader proxyReader = this;
  protected Object spiObject;

  // computed
  private Section shapeAsSection; // derived from the shape, immutable; used for every read, deferred creation
  protected int[] shape = new int[0];
  protected boolean isVariableLength;
  protected int elementSize;

  // TODO do we need these? breaks immutability
  // TODO maybe caching read data shoul be seperate from "this is the source of the data".
  protected Cache cache = new Cache(); // cache cannot be null
  protected int sizeToCache = -1; // bytes

  protected Variable(Builder<?> builder, Group parentGroup) {
    this.shortName = builder.shortName;

    if (parentGroup == null) {
      throw new IllegalStateException(String.format("Parent Group must be set for Variable %s", builder.shortName));
    }

    if (builder.dataType == null) {
      throw new IllegalStateException(String.format("DataType must be set for Variable %s", builder.shortName));
    }

    if (builder.shortName == null || builder.shortName.isEmpty()) {
      throw new IllegalStateException("Name must be set for Variable");
    }

    this.parentGroup = parentGroup;
    this.ncfile = builder.ncfile;
    this.parentStructure = builder.parentStruct;
    this.dataType = builder.dataType;
    this.attributes = builder.attributes;
    this.proxyReader = builder.proxyReader == null ? this : builder.proxyReader;
    this.spiObject = builder.spiObject;
    this.cache = builder.cache;

    if (this.dataType.isEnum()) {
      this.enumTypedef = this.parentGroup.findEnumeration(builder.enumTypeName);
      if (this.enumTypedef == null) {
        throw new IllegalStateException(
            String.format("EnumTypedef '%s' does not exist in a parent Group", builder.enumTypeName));
      }
    }

    // Convert dimension to shared dimensions that live in a parent group.
    // TODO: In 6.0 remove group field in dimensions, just use equals() to match.
    List<Dimension> dims = new ArrayList<>();
    for (Dimension dim : builder.dimensions) {
      if (dim.isShared()) {
        Dimension sharedDim = this.parentGroup.findDimension(dim.getShortName());
        if (sharedDim == null) {
          throw new IllegalStateException(String.format("Shared Dimension %s does not exist in a parent proup", dim));
        } else {
          dims.add(sharedDim);
        }
      } else {
        dims.add(dim);
      }
    }

    // LOOK cant use findVariableLocal because variables not set built.
    // possible slice of another variable
    if (builder.slicer != null) {
      int dim = builder.slicer.dim;
      int index = builder.slicer.index;
      Section slice = Dimensions.makeSectionFromDimensions(dims).replaceRange(dim, Range.make(index, index)).build();
      setProxyReader(new SliceReader(parentGroup, builder.slicer.orgName, dim, slice));
      setCaching(false); // dont cache
      // remove that dimension in this variable
      dims.remove(dim);
    }

    this.dimensions = dims;
    if (builder.autoGen != null) {
      this.cache.data = builder.autoGen.makeDataArray(this.dataType, this.dimensions);
      this.cache.isMetadata = true; // So it gets copied
    }

    // calculated fields
    this.elementSize = builder.elementSize > 0 ? builder.elementSize : getDataType().getSize();
    this.isVariableLength = this.dimensions.stream().anyMatch(Dimension::isVariableLength);
    try {
      List<Range> list = new ArrayList<>();
      for (Dimension d : dimensions) {
        int len = d.getLength();
        if (len > 0)
          list.add(new Range(d.getShortName(), 0, len - 1));
        else if (len == 0)
          list.add(Range.EMPTY); // LOOK empty not named
        else {
          assert d.isVariableLength();
          list.add(Range.VLEN); // LOOK vlen not named
        }
      }
      this.shapeAsSection = new Section(list).makeImmutable();
      this.shape = shapeAsSection.getShape();

    } catch (InvalidRangeException e) {
      log.error("Bad shape in variable " + getFullName(), e);
      throw new IllegalStateException(e.getMessage());
    }
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  // This makes an exact copy, including ncfile and parent and proxyReader.
  // build() replaces parent but respects ncfile and proxyReader.
  // Normally on a copy you want to set proxyReader to null;
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
    builder.setName(this.shortName).setNcfile(this.ncfile).setParentStructure(this.getParentStructure())
        .setDataType(this.dataType).setEnumTypeName(this.enumTypedef != null ? this.enumTypedef.getShortName() : null)
        .addDimensions(this.dimensions).addAttributes(this.attributes).setProxyReader(this.proxyReader)
        .setSPobject(this.spiObject);

    if (this.cache.isMetadata) {
      builder.setCachedData(this.cache.data, true);
    }
    return builder;
  }

  /**
   * Get Builder for this class that allows subclassing.
   * 
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  /** A builder for Variables. */
  public static abstract class Builder<T extends Builder<T>> {
    public String shortName;
    public DataType dataType;
    private int elementSize;

    public NetcdfFile ncfile; // set in Group build() if null
    private Structure parentStruct; // set in Structure.build(), no not use otherwise

    protected Group.Builder parentBuilder;
    protected Structure.Builder<?> parentStructureBuilder;
    private ArrayList<Dimension> dimensions = new ArrayList<>(); // The dimension's group is ignored; replaced when
                                                                 // build()
    public Object spiObject;
    public ProxyReader proxyReader;
    public Cache cache = new Cache(); // cache cannot be null

    private String enumTypeName;
    private AutoGen autoGen;
    private Slicer slicer;
    private AttributeContainerMutable attributes = new AttributeContainerMutable("");

    private boolean built;

    protected abstract T self();

    // add / replace previous
    public T addAttribute(Attribute att) {
      attributes.addAttribute(att);
      return self();
    }

    public T addAttributes(Iterable<Attribute> atts) {
      attributes.addAll(atts);
      return self();
    }

    public AttributeContainerMutable getAttributeContainer() {
      return attributes;
    }

    /*
     * TODO Dimensions are tricky during the transition to 6, because they have a pointer to their Group in 5.x,
     * but with Builders, the Group isnt created until build(). The Group is part of equals() and hash().
     * The second issue is that we may not know their shape, so that may have to be made later.
     * Provisionally, we are going to try this strategy: during build, Dimensions are created without Groups, and
     * hash and equals are modified to allow that. During build, the Dimensions are recreated with the Group, and
     * Variables Dimensions are replaced with shared Dimensions.
     * For 6.0, Dimensions become value objects, without a reference to containing Group.
     *
     * A VariableBuilder does not know its Group.Builder, so searching for "parent dimensions", must be done with the
     * Group.Builder object, not the Variable.Builder.
     *
     * Havent dealt with structure yet, eg getDimensionsAll(), but should be ok because Variable.Builder does know its
     * parent structure.
     */

    public T addDimension(Dimension dim) {
      dimensions.add(dim);
      return self();
    }

    public T addDimensions(Collection<Dimension> dims) {
      dimensions.addAll(dims);
      return self();
    }

    public T setDimensions(List<Dimension> dims) {
      this.dimensions = new ArrayList<>(dims);
      return self();
    }

    /** Find the dimension with the same name as dim, and replace it with dim */
    public boolean replaceDimensionByName(Dimension dim) {
      int idx = -1;
      for (int i = 0; i < dimensions.size(); i++) {
        if (dimensions.get(i).getShortName().equals(dim.getShortName())) {
          idx = i;
        }
      }
      if (idx >= 0) {
        dimensions.set(idx, dim);
      }
      return (idx >= 0);
    }

    /** Set dimensions by name. The parent group builder must be set. */
    public T setDimensionsByName(String dimString) {
      Preconditions.checkNotNull(this.parentBuilder);
      this.dimensions = new ArrayList<>(this.parentBuilder.makeDimensionsList(dimString));
      return self();
    }

    @Nullable
    public String getFirstDimensionName() {
      return getDimensionName(0);
    }

    @Nullable
    public String getDimensionName(int index) {
      if (dimensions.size() > index) {
        return dimensions.get(index).getShortName();
      }
      return null;
    }

    public Iterable<String> getDimensionNames() {
      if (dimensions.size() > 0) {
        // TODO test if this always works
        return dimensions.stream().map(d -> d.getShortName()).filter(Objects::nonNull).collect(Collectors.toList());
      }
      return ImmutableList.of();
    }

    public String makeDimensionsString() {
      return Dimensions.makeDimensionsString(this.dimensions);
    }

    /**
     * Set the dimensions using all anonymous (unshared) dimensions
     *
     * @param shape defines the dimension lengths. must be > 0, or -1 for VLEN
     * @throws RuntimeException if any shape < 1 and not -1.
     */
    public T setDimensionsAnonymous(int[] shape) {
      this.dimensions = new ArrayList<>(Dimensions.makeDimensionsAnon(shape));
      return self();
    }

    public ImmutableList<Dimension> getDimensions() {
      return ImmutableList.copyOf(this.dimensions);
    }

    // Get all dimension names, including parent structure
    public ImmutableSet<String> getDimensionNamesAll() {
      ImmutableSet.Builder<String> dimsAll = new ImmutableSet.Builder<>();
      addDimensionNamesAll(dimsAll, this);
      return dimsAll.build();
    }

    private void addDimensionNamesAll(ImmutableSet.Builder<String> result, Variable.Builder<?> v) {
      if (v.parentStructureBuilder != null) {
        v.parentStructureBuilder.getDimensionNamesAll().forEach(result::add);
      }
      getDimensionNames().forEach(result::add);
    }

    public boolean isUnlimited() {
      for (Dimension d : dimensions) {
        if (d.isUnlimited())
          return true;
      }
      return false;
    }

    public T setIsScalar() {
      this.dimensions = new ArrayList<>();
      return self();
    }

    public int getRank() {
      return dimensions.size();
    }

    public T setDataType(DataType dataType) {
      this.dataType = dataType;
      return self();
    }

    public String getEnumTypeName() {
      return this.enumTypeName;
    }

    public int getElementSize() {
      return elementSize > 0 ? elementSize : dataType.getSize();
    }

    // In some case we need to override standard element size.
    public T setElementSize(int elementSize) {
      this.elementSize = elementSize;
      return self();
    }

    public T setEnumTypeName(String enumTypeName) {
      this.enumTypeName = enumTypeName;
      return self();
    }

    public T setNcfile(NetcdfFile ncfile) {
      this.ncfile = ncfile;
      return self();
    }

    public T setSPobject(Object spiObject) {
      this.spiObject = spiObject;
      return self();
    }

    public T setName(String shortName) {
      this.shortName = NetcdfFiles.makeValidCdmObjectName(shortName);
      this.attributes.setName(shortName);
      return self();
    }

    public String getFullName() {
      String full = "";
      Group.Builder group = parentStructureBuilder != null ? parentStructureBuilder.parentBuilder : parentBuilder;
      if (group != null) {
        full = group.makeFullName();
      }
      if (parentStructureBuilder != null) {
        full += parentStructureBuilder.shortName + ".";
      }
      return full + this.shortName;
    }

    public T setParentGroupBuilder(Group.Builder parent) {
      this.parentBuilder = parent;
      return self();
    }

    public Group.Builder getParentGroupBuilder() {
      return parentBuilder;
    }

    T setParentStructureBuilder(Structure.Builder<?> structureBuilder) {
      this.parentStructureBuilder = structureBuilder;
      return self();
    }

    public Structure.Builder<?> getParentStructureBuilder() {
      return parentStructureBuilder;
    }

    // Only the parent Structure should call this.
    T setParentStructure(Structure parent) {
      this.parentStruct = parent;
      return self();
    }

    public T setProxyReader(ProxyReader proxy) {
      this.proxyReader = proxy;
      return self();
    }

    public T setCachedData(Array cacheData, boolean isMetadata) {
      this.cache.data = cacheData;
      this.cache.isMetadata = isMetadata;
      this.cache.cachingSet = true;
      this.cache.isCaching = true;
      return self();
    }

    public T setAutoGen(double start, double incr) {
      this.autoGen = new AutoGen(start, incr);
      return self();
    }

    public T resetCache() {
      this.cache.data = null;
      return self();
    }

    public T setCaching(boolean caching) {
      this.cache.isCaching = caching;
      this.cache.cachingSet = true;
      return self();
    }

    /**
     * Create a new Variable.Builder that is a logical slice of this one, by
     * fixing the specified dimension at the specified index value.
     * 
     * @param dim which dimension to fix
     * @param index at what index value
     */
    public Variable.Builder<?> makeSliceBuilder(int dim, int index) {
      // create a copy of this builder with a slicer
      Variable.Builder<?> sliced = this.copy();
      sliced.slicer = new Slicer(dim, index, this.shortName);
      return sliced;
    }

    public Builder<?> copy() {
      return new Builder2().copyFrom(this);
    }

    /** TODO Copy metadata from orgVar. */
    public T copyFrom(Variable orgVar) {
      setName(orgVar.getShortName());
      setDataType(orgVar.getDataType());
      if (orgVar.getEnumTypedef() != null) {
        setEnumTypeName(orgVar.getEnumTypedef().getShortName());
      }
      setSPobject(orgVar.getSPobject());
      addDimensions(orgVar.getDimensions());
      addAttributes(orgVar.attributes()); // copy

      return self();
    }

    public T copyFrom(Variable.Builder<?> builder) {
      addAttributes(builder.attributes); // copy
      this.autoGen = builder.autoGen;
      this.cache = builder.cache;
      setDataType(builder.dataType);
      addDimensions(builder.dimensions);
      this.elementSize = builder.elementSize;
      setEnumTypeName(builder.getEnumTypeName());
      setNcfile(builder.ncfile);
      this.parentBuilder = builder.parentBuilder;
      setParentStructure(builder.parentStruct);
      setParentStructureBuilder(builder.parentStructureBuilder);
      setProxyReader(builder.proxyReader);
      setName(builder.shortName);
      setSPobject(builder.spiObject);
      return self();
    }

    @Override
    public String toString() {
      return dataType + " " + shortName;
    }

    /** Normally this is called by Group.build() */
    public Variable build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new Variable(this, parentGroup);
    }
  }

  @Immutable
  private static class AutoGen {
    final double start;
    final double incr;

    private AutoGen(double start, double incr) {
      this.start = start;
      this.incr = incr;
    }

    private Array makeDataArray(DataType dtype, List<Dimension> dimensions) {
      Section section = Dimensions.makeSectionFromDimensions(dimensions).build();
      return Array.makeArray(dtype, (int) section.getSize(), start, incr).reshape(section.getShape());
    }
  }

  @Immutable
  private static class Slicer {
    final int dim;
    final int index;
    final String orgName;

    Slicer(int dim, int index, String orgName) {
      this.dim = dim;
      this.index = index;
      this.orgName = orgName;
    }
  }

  ///////////////////////////////////////////////////////////////////////
  // deprecated

  /**
   * @return isVariableLength()
   * @deprecated use isVariableLength()
   */
  public boolean isUnknownLength() {
    return isVariableLength;
  }
}
