/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.StructureDataArray;
import ucar.array.StructureMembers;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;
import java.util.*;
import java.io.IOException;

/**
 * A logical container for data, with a dataType, a set of Dimensions that define its array shape,
 * and a set of Attributes.
 * <p/>
 * The data is a multidimensional array of primitive types, Strings, or Structures.
 * Data access is done through the readXXX() methods, which return a memory resident Array.
 * <p>
 * The cache isnt immutable, but changes to it should not be visible.
 */
@Immutable
public class Variable implements ProxyReader, Comparable<Variable> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Variable.class);
  private static final boolean showSize = false;

  /**
   * Globally permit or prohibit caching. For use during testing and debugging.
   * <p>
   * A {@code true} value for this field does not indicate whether a Variable
   * {@link #isCaching() is caching}, only that it's <i>permitted</i> to cache.
   */
  public static boolean permitCaching = true;
  private static final int defaultSizeToCache = 4000; // bytes; cache any variable whose size() < defaultSizeToCache
  private static final int defaultCoordsSizeToCache = 40 * 1000; // bytes; cache coordinates whose size() < this

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

  /** The location of the dataset this belongs to. Labeling purposes only. */
  public String getDatasetLocation() {
    if (ncfile != null)
      return ncfile.getLocation();
    return "N/A";
  }

  /** Get the data type of the Variable. */
  public ArrayType getArrayType() {
    return dataType;
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
   * Get the list of dimensions used by this variable.
   * The most slowly varying (leftmost for Java and C programmers) dimension is first.
   * For scalar variables, the list is empty.
   */
  public ImmutableList<Dimension> getDimensions() {
    return dimensions;
  }

  /** Get the variable's Dimensions as a Set. */
  public ImmutableSet<Dimension> getDimensionSet() {
    return ImmutableSet.copyOf(dimensions);
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

  /** Get the list of Dimension names, space delineated. */
  public String getDimensionsString() {
    return Dimensions.makeDimensionsString(dimensions);
  }

  /**
   * Get the number of bytes for one element of this Variable.
   * For Variables of primitive type, this is equal to getArrayType().getSize().
   * Structure and subclasses return the size in bytes of one Structure.
   * This is the canonical size, not the actual size stored on disk.
   */
  public int getElementSize() {
    return getArrayType().getSize();
  }

  /** Get the EnumTypedef, only use if getDataType.isEnum() */
  @Nullable
  public EnumTypedef getEnumTypedef() {
    return enumTypedef;
  }

  /**
   * Get the file type id for the underlying data source.
   *
   * @return registered id of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  @Nullable
  public String getFileTypeId() {
    return ncfile == null ? null : ncfile.getFileTypeId();
  }

  /** Get the full name of this Variable. see {@link NetcdfFiles#makeFullName(Variable)} */
  public String getFullName() {
    return NetcdfFiles.makeFullName(this);
  }

  /** Get the NetcdfFile that this variable is contained in. May be null when the Variable is self contained. */
  @Nullable
  public NetcdfFile getNetcdfFile() {
    return ncfile;
  }

  /** Get its containing Group, never null */
  public Group getParentGroup() {
    return this.parentGroup;
  }

  /** Get its parent structure, or null if not in structure */
  @Nullable
  public Structure getParentStructure() {
    return this.parentStructure;
  }

  /** Get the number of dimensions of the Variable, aka the rank. */
  public int getRank() {
    return shapeAsSection.getRank();
  }

  /** Get the shape: length of Variable in each dimension. A scalar (rank 0) will have an int[0] shape. */
  public int[] getShape() {
    return shapeAsSection.getShape();
  }

  /** Get the size of the ith dimension */
  public int getShape(int index) {
    return shapeAsSection.getShape(index);
  }

  /**
   * Get shape as a Section object.
   *
   * @return Section describing this Variable's shape.
   */
  public Section getSection() {
    return this.shapeAsSection;
  }

  /** The short name of the variable */
  public String getShortName() {
    return this.shortName;
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
    return Arrays.computeSize(getShape());
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
   * Calculate if this is a classic coordinate variable: has same name as its first dimension.
   * If type char, must be 2D, else must be 1D.
   *
   * @return true if a coordinate variable.
   */
  public boolean isCoordinateVariable() {
    if ((getArrayType() == ArrayType.STRUCTURE) || isMemberOfStructure()) // Structures and StructureMembers cant be
                                                                          // coordinate
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
          (getArrayType() == ArrayType.CHAR);
    }

    return false;
  }

  /** Is this a Structure Member? */
  public boolean isMemberOfStructure() {
    return this.parentStructure != null;
  }

  /** Is this variable metadata?. True if its values need to be included explicitly in NcML output. */
  public boolean isMetadata() {
    return cache != null && cache.srcData != null;
  }

  /** Whether this is a scalar Variable (rank == 0). */
  public boolean isScalar() {
    return getRank() == 0;
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
   * Does this variable have a variable length dimension?
   * If so, it has as one of its dimensions Dimension.VLEN.
   *
   * @return true if Variable has a variable length dimension?
   */
  public boolean isVariableLength() {
    return isVariableLength;
  }

  /**
   * Lookup the enum string for this value.
   * Can only be called on enum types, where dataType.isEnum() is true.
   *
   * @param val the integer value of this enum
   * @return the String value, or null if not exist.
   */
  @Nullable
  public String lookupEnumString(int val) {
    Preconditions.checkArgument(dataType.isEnum(), "Can only call Variable.lookupEnumString() on enum types");
    Preconditions.checkNotNull(enumTypedef, "enum Variable does not have enumTypedef");
    return enumTypedef.lookupEnumString(val);
  }

  /**
   * Create a new Variable that is a logical subsection of this Variable.
   * No data is read until a read method is called on it.
   *
   * @param subsection Section of this variable.
   *        Each Range in the section corresponds to a Dimension, and specifies the section of data to read in that
   *        Dimension. A Range object may be null, which means use the entire dimension.
   * @return a new Variable which is a logical section of this Variable.
   * @throws InvalidRangeException if section not compatible with shape
   */
  public Variable section(Section subsection) throws InvalidRangeException {
    subsection = Section.fill(subsection, getShape());

    // create a copy of this variable with a proxy reader
    Variable.Builder<?> sectionV = this.toBuilder(); // subclasses override toBuilder()
    sectionV.setProxyReader(new SectionReader(this, subsection));
    sectionV.resetCache(); // dont cache
    sectionV.setIsCaching(false); // dont cache

    // replace dimensions if needed, note not shared
    int[] shape = subsection.getShape();
    ArrayList<Dimension> dimensions = new ArrayList<>();
    for (int i = 0; i < getRank(); i++) {
      Dimension oldD = getDimension(i);
      Dimension newD = (oldD.getLength() == shape[i]) ? oldD
          : Dimension.builder().setName(oldD.getShortName()).setIsUnlimited(oldD.isUnlimited()).setIsShared(false)
              .setLength(shape[i]).build();
      dimensions.add(newD);
    }
    sectionV.dimensions = dimensions;
    return sectionV.build(getParentGroup());
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
    if ((dim < 0) || (dim >= getRank()))
      throw new InvalidRangeException("Slice dim invalid= " + dim);

    // ok to make slice of record dimension with length 0
    boolean recordSliceOk = false;
    if ((dim == 0) && (value == 0)) {
      Dimension d = getDimension(0);
      recordSliceOk = d.isUnlimited();
    }

    // otherwise check slice in range
    if (!recordSliceOk) {
      if ((value < 0) || (value >= getShape(dim)))
        throw new InvalidRangeException("Slice value invalid= " + value + " for dimension " + dim);
    }

    // create a copy of this variable with a proxy reader
    Variable.Builder<?> sliceV = this.toBuilder(); // subclasses override toBuilder()
    Section.Builder slice = Section.builder().appendRanges(getShape());
    slice.replaceRange(dim, new Range(value, value));
    sliceV.setProxyReader(new SliceReader(this, dim, slice.build()));
    sliceV.resetCache(); // dont share the cache
    sliceV.setIsCaching(false); // dont cache

    // remove that dimension - reduce rank
    sliceV.dimensions.remove(dim);
    return sliceV.build(getParentGroup());
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
      Preconditions.checkArgument(dimensions.contains(d));
      Preconditions.checkArgument(d.getLength() == 1);
      dimIdx.add(dimensions.indexOf(d));
    }

    // create a copy of this variable with a proxy reader
    Variable.Builder<?> sliceV = this.toBuilder(); // subclasses override toBuilder()
    sliceV.setProxyReader(new ReduceReader(this, dimIdx));
    sliceV.resetCache(); // dont share the cache
    sliceV.setIsCaching(false); // dont cache

    // remove dimension(s) - reduce rank
    for (Dimension d : dims) {
      sliceV.dimensions.remove(d);
    }
    return sliceV.build(getParentGroup());
  }

  //////////////////////////////////////////////////////////////////////////////
  ///// scalar reading

  /**
   * Get the first value as a byte.
   *
   * @throws IOException if theres an IO Error
   * @throws RuntimeException if data type not convertible to byte
   */
  public byte readScalarByte() throws IOException {
    return ((Number) readArray().getScalar()).byteValue();
  }

  /**
   * Get the first value as a short.
   *
   * @throws IOException if theres an IO Error
   * @throws RuntimeException if data type not convertible to short
   */
  public short readScalarShort() throws IOException {
    return ((Number) readArray().getScalar()).shortValue();
  }

  /**
   * Get the first value as an int.
   *
   * @throws IOException if theres an IO Error
   * @throws RuntimeException if data type not convertible to int
   */
  public int readScalarInt() throws IOException {
    return ((Number) readArray().getScalar()).intValue();
  }

  /**
   * Get the first value as a long.
   *
   * @throws IOException if theres an IO Error
   * @throws RuntimeException if data type not convertible to long
   */
  public long readScalarLong() throws IOException {
    return ((Number) readArray().getScalar()).longValue();
  }

  /**
   * Get the first value as a float.
   *
   * @throws IOException if theres an IO Error
   * @throws RuntimeException if data type not convertible to float
   */
  public float readScalarFloat() throws IOException {
    return ((Number) readArray().getScalar()).floatValue();
  }

  /**
   * Get the first value as a double.
   *
   * @throws IOException if theres an IO Error
   * @throws RuntimeException if data type not convertible to double
   */
  public double readScalarDouble() throws IOException {
    return ((Number) readArray().getScalar()).doubleValue();
  }

  /**
   * Get the first value as a String for a STRING or CHAR Variable.
   *
   * @throws IOException if theres an IO Error
   * @throws IllegalArgumentException if data type not STRING or CHAR
   */
  public String readScalarString() throws IOException {
    if (getArrayType() == ArrayType.STRING) {
      return (String) readArray().getScalar();
    } else if (getArrayType() == ArrayType.CHAR) {
      return Arrays.makeStringFromChar((Array<Byte>) readArray());
    } else {
      throw new IllegalArgumentException("readScalarString not STRING or CHAR " + getFullName());
    }
  }

  ///////////////
  // internal reads: all other calls go through these.
  // subclasses must override, so that NetcdfDataset wrapping will work.

  // non-structure-member Variables.

  protected Array<?> _read() throws IOException {
    // caching overrides the proxyReader
    // check if already cached
    if (cache.getData() != null) {
      return cache.getData();
    }

    Array<?> data = proxyReader.proxyReadArray(this, null);

    // optionally cache it
    if (isCaching()) {
      cache.setCachedData(data);
    }
    return data;
  }

  /**
   * Read all the data for this Variable and return a memory resident Array.
   * The Array has the same element type and shape as the Variable.
   */
  public Array<?> readArray() throws IOException {
    if (cache.getData() != null) {
      return cache.getData();
    }

    Array<?> data = proxyReader.proxyReadArray(this, null);

    // optionally cache it
    if (isCaching()) {
      cache.setCachedData(data);
    }

    // ucar.array.Array is Immutable
    return data;
  }

  // section of non-structure-member Variable
  // assume filled, validated Section
  protected Array<?> _read(Section section) throws IOException, InvalidRangeException {
    // check if its really a full read
    if ((null == section) || section.computeSize() == getSize()) {
      return _read();
    }

    // full read was cached
    if (isCaching()) {
      Array<?> cacheData;
      if (cache.getData() == null) {
        cacheData = _read();
        cache.setCachedData(cacheData); // read and cache entire array
      } else {
        cacheData = cache.getData();
      }
      return Arrays.section(cacheData, new Section(section.getRanges())); // subset it
    }

    return proxyReader.proxyReadArray(this, section, null);
  }

  /**
   * Read a section of the data for this Variable from the netcdf file and return a memory resident Array.
   * The Array has the same element type as the Variable, and the requested shape.
   * Note that this does not do rank reduction, so the returned Array has the same rank
   * as the Variable. Use Array.reduce() for rank reduction.
   *
   * @param section The section of data to read.
   *        Must be null or same rank as variable.
   *        If list is null, assume all data.
   *        Each Range corresponds to a Dimension. If the Range object is null, it means use the entire dimension.
   */
  public Array<?> readArray(@Nullable Section section) throws java.io.IOException, InvalidRangeException {
    section = Section.fill(section, getShape());
    if (section.computeSize() == getSize()) {
      return readArray();
    }
    // full read was cached
    if (isCaching()) {
      if (cache.getData() == null) {
        cache.setCachedData(readArray()); // read and cache entire array
      }
      return Arrays.section(cache.getData(), section); // subset it
    }
    // not caching
    return proxyReader.proxyReadArray(this, section, null);
  }

  ////// ProxyReader

  /** public by accident, do not call directly. */
  @Override
  public Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException {
    if (isMemberOfStructure()) {
      Preconditions.checkNotNull(getParentStructure());
      // throw new UnsupportedOperationException("Cannot directly read Member Variable=" + getFullName());
      List<String> memList = new ArrayList<>();
      memList.add(this.getShortName());
      Structure s = getParentStructure().select(memList);
      Array<?> as = s.readArray();
      StructureDataArray sad = (StructureDataArray) as;
      StructureMembers.Member m = sad.getStructureMembers().findMember(this.getShortName());
      return sad.extractMemberArray(m);
    }

    Preconditions.checkNotNull(ncfile);
    try {
      return ncfile.readArrayData(this, getSection());
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage()); // cant happen haha
    }
  }

  /** public by accident, do not call directly. */
  @Override
  public Array<?> proxyReadArray(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    if (isMemberOfStructure()) {
      throw new UnsupportedOperationException("Cannot directly read section of Member Variable=" + getFullName());
    }
    Preconditions.checkNotNull(ncfile);
    return ncfile.readArrayData(this, section);
  }

  ////////////////////////////////////////////////////////////////////////

  /** Get the display name plus the dimensions, eg 'float name(dim1, dim2)' */
  public String getNameAndDimensions() {
    Formatter buf = new Formatter();
    getNameAndDimensions(buf, true, false);
    return buf.toString();
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
    if (strict) {
      name = NetcdfFiles.makeValidCDLName(getShortName());
    }
    buf.format("%s", name);

    if (getRank() > 0) {
      buf.format("(");
    }
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension myd = dimensions.get(i);
      if (i != 0) {
        buf.format(", ");
      }
      dimCDL(buf, myd, strict);
    }
    if (getRank() > 0) {
      buf.format(")");
    }
  }

  private void dimCDL(Formatter out, Dimension dim, boolean strict) {
    if (!dim.isShared()) {
      out.format("%d", dim.getLength());
    } else if (isVariableLength()) {
      out.format("*");
    } else {
      String dimName = dim.getShortName();
      if (!strict) {
        out.format("%s=%d", dimName, dim.getLength());
      } else {
        out.format("%s", dimName);
      }
    }
  }

  public String toString() {
    Formatter buf = new Formatter();
    writeCDL(buf, new Indent(2), false, false);
    return buf.toString();
  }

  protected void writeCDL(Formatter buf, Indent indent, boolean useFullName, boolean strict) {
    buf.format("%s", indent);
    if (getArrayType().isEnum()) {
      if (enumTypedef == null)
        buf.format("enum UNKNOWN");
      else
        buf.format("enum %s", NetcdfFiles.makeValidCDLName(enumTypedef.getShortName()));
    } else
      buf.format("%s", getArrayType().toCdl());

    buf.format(" ");
    getNameAndDimensions(buf, useFullName, strict);
    buf.format(";");
    if (!strict)
      buf.format(extraInfo());
    buf.format("%n");

    indent.incr();
    for (Attribute att : attributes()) {
      buf.format("%s", indent);
      att.writeCDL(buf, strict, getShortName());
      buf.format(";");
      if (!strict && (att.getArrayType() == ArrayType.STRING))
        buf.format(" // %s", att.getArrayType().toCdl());
      buf.format("%n");
    }
    indent.decr();
  }

  protected String extraInfo() {
    return showSize ? " // " + getElementSize() + " " + getSize() : "";
  }

  @Override
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
    if (getArrayType() != o.getArrayType())
      return false;
    if (!getParentGroup().equals(o.getParentGroup()))
      return false;
    if ((getParentStructure() != null) && !getParentStructure().equals(o.getParentStructure()))
      return false;
    if (isVariableLength() != o.isVariableLength())
      return false;
    if (dimensions.size() != o.getDimensions().size())
      return false;
    for (int i = 0; i < dimensions.size(); i++) {
      if (!getDimension(i).equals(o.getDimension(i)))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getShortName().hashCode();
      if (isScalar())
        result++;
      result = 37 * result + getArrayType().hashCode();
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

  // Joshua Bloch (Item 9, chapter 3, page 49)
  protected int hashCode;

  /** Sort by name */
  @Override
  public int compareTo(Variable o) {
    return getShortName().compareTo(o.getShortName());
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Attributes

  /** The attributes contained by this Variable. */
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

  /** Get immutable service provider opaque object. */
  public Object getSPobject() {
    return spiObject;
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // caching

  /**
   * If total data size is less than SizeToCache in bytes, then cache.
   *
   * @return size at which caching happens
   */
  public int getSizeToCache() {
    if (cache.sizeToCacheBytes != null) { // it was already set
      return cache.sizeToCacheBytes;
    }
    // default
    return isCoordinateVariable() ? defaultCoordsSizeToCache : defaultSizeToCache;
  }

  /**
   * Set whether to cache or not. Implies that the entire array will be stored, once read.
   * Normally this is set automatically based on size of data.
   * if false, cachedData is cleared.
   */
  public void setCaching(boolean caching) {
    this.cache.setIsCaching(caching);
    if (!caching) {
      this.cache.setCachedData(null);
    }
  }

  /**
   * Will this Variable be cached when read.
   * Set externally by setCaching, or calculated based on total size &lt; sizeToCache.
   * <p>
   * This will always return {@code false} if {@link #permitCaching caching isn't permitted}.
   *
   * @return true is caching
   */
  public boolean isCaching() {
    if (!permitCaching) {
      return false;
    }
    if (this.cache.isCaching != null) {
      return this.cache.isCaching;
    }
    if (this.cache.srcData != null || this.cache.cacheData != null) {
      return true;
    }

    return !(this instanceof Structure) && !isVariableLength && (getSize() * getElementSize() < getSizeToCache());
  }

  /** If this has cached data, or source data. */
  public boolean hasCachedData() {
    return (null != cache.cacheData) || (null != cache.srcData);
  }

  // this indirection allows us to share the cache among the variable's sections and copies
  // The cache isnt immutable, but changes to it should not be visible.
  protected static class Cache {
    // this is the only source of the data, do not erase, can only be set in Builder
    private Array<?> srcData;
    // this is temporary data, may be erased, can be set by setCachedData()
    private Array<?> cacheData;
    private Integer sizeToCacheBytes; // bytes
    private Boolean isCaching;

    private Cache() {}

    protected void reset() {
      this.cacheData = null;
    }

    protected Array<?> getData() {
      return (srcData != null) ? srcData : cacheData;
    }

    protected void setIsCaching(boolean isCaching) {
      this.isCaching = isCaching;
    }

    protected void setCachedData(Array<?> data) {
      this.cacheData = data;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////
  private final String shortName;
  private final Group parentGroup;
  @Nullable
  private final Structure parentStructure;
  private final ArrayType dataType;
  @Nullable
  private final EnumTypedef enumTypedef;

  @Nullable
  protected final NetcdfFile ncfile; // Physical container for this Variable where the I/O happens.
                                     // may be null if Variable is self contained (eg NcML).
  @Nullable
  protected final Object spiObject;
  protected final ImmutableList<Dimension> dimensions;
  protected final AttributeContainer attributes;
  protected final ProxyReader proxyReader;
  protected final Cache cache;

  private final Section shapeAsSection;
  protected final boolean isVariableLength;

  protected Variable(Builder<?> builder, Group parentGroup) {
    if (parentGroup == null) {
      throw new IllegalStateException(String.format("Parent Group must be set for Variable %s", builder.shortName));
    }
    if (builder.dataType == null) {
      throw new IllegalStateException(String.format("DataType must be set for Variable %s", builder.shortName));
    }
    if (Strings.isNullOrEmpty(builder.shortName)) {
      throw new IllegalStateException("Name must be set for Variable");
    }

    this.shortName = builder.shortName;
    this.parentGroup = parentGroup;
    this.ncfile = builder.ncfile;
    this.parentStructure = builder.parentStruct;
    this.dataType = builder.dataType;
    this.attributes = builder.attributes.toImmutable();
    this.spiObject = builder.spiObject;
    this.cache = builder.cache;

    ProxyReader useProxyReader = builder.proxyReader == null ? this : builder.proxyReader;

    if (this.dataType.isEnum()) {
      this.enumTypedef = this.parentGroup.findEnumeration(builder.enumTypeName);
      if (this.enumTypedef == null) {
        throw new IllegalStateException(
            String.format("EnumTypedef '%s' does not exist in a parent Group", builder.enumTypeName));
      }
    } else {
      this.enumTypedef = null;
    }

    // Convert dimension to shared dimensions that live in a parent group.
    List<Dimension> dims = new ArrayList<>();
    for (Dimension dim : builder.dimensions) {
      if (dim.isShared()) {
        Dimension sharedDim = this.parentGroup.findDimension(dim);
        if (sharedDim == null) {
          throw new IllegalStateException(String.format("Shared Dimension %s does not exist in a parent proup", dim));
        } else {
          dims.add(sharedDim);
        }
      } else {
        dims.add(dim);
      }
    }

    // possible slice of another variable
    if (builder.slicer != null) {
      int dim = builder.slicer.dim;
      int index = builder.slicer.index;
      Section slice =
          Dimensions.makeArraySectionFromDimensions(dims).replaceRange(dim, Range.make(index, index)).build();
      useProxyReader = new SliceReader(parentGroup, builder.slicer.orgName, dim, slice);
      builder.resetCache(); // no caching
      // remove that dimension in this variable
      dims.remove(dim);
    }
    this.proxyReader = useProxyReader;

    this.dimensions = ImmutableList.copyOf(dims);
    if (builder.autoGen != null) {
      this.cache.srcData = builder.autoGen.makeDataArray(getArrayType(), this.dimensions);
    }

    // calculated fields
    this.isVariableLength = this.dimensions.stream().anyMatch(Dimension::isVariableLength);
    try {
      List<Range> list = new ArrayList<>();
      for (Dimension d : dimensions) {
        int len = d.getLength();
        if (len > 0) {
          list.add(new Range(d.getShortName(), 0, len - 1));
        } else if (len == 0) {
          list.add(Range.EMPTY);
        } else {
          Preconditions.checkArgument(d.isVariableLength());
          list.add(Range.VLEN);
        }
      }
      this.shapeAsSection = new Section(list);

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
  // This makes an almost exact copy, including ncfile, parentStructure, and parentGroup
  // proxyReader is set only if its different from this
  // build() replaces parentGroup always.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
    builder.setName(this.shortName).setNcfile(this.ncfile).setParentStructure(this.getParentStructure())
        .setArrayType(this.dataType).setEnumTypeName(this.enumTypedef != null ? this.enumTypedef.getShortName() : null)
        .addDimensions(this.dimensions).addAttributes(this.attributes).setSPobject(this.spiObject);

    // Only set ProxyReader if its not this
    if (this.proxyReader != this) {
      builder.setProxyReader(this.proxyReader);
    }

    if (this.cache.srcData != null) {
      builder.setSourceData(this.cache.srcData);
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

  public static abstract class Builder<T extends Builder<T>> {
    public String shortName;
    public ArrayType dataType;

    public NetcdfFile ncfile; // set in Group build() if null
    private Structure parentStruct; // set in Structure.build(), no not use otherwise

    protected Group.Builder parentBuilder;
    protected Structure.Builder<?> parentStructureBuilder;
    private ArrayList<Dimension> dimensions = new ArrayList<>();
    public Object spiObject;
    public ProxyReader proxyReader;
    Cache cache = new Cache(); // cache cannot be null

    private String enumTypeName;
    private AutoGen autoGen;
    private Slicer slicer;
    private final AttributeContainerMutable attributes = new AttributeContainerMutable("");

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

    public T removeAttribute(String attName) {
      attributes.removeAttribute(attName);
      return self();
    }

    public AttributeContainerMutable getAttributeContainer() {
      return attributes;
    }

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

    /** Replace the ith dimension. */
    public void replaceDimension(int idx, Dimension dim) {
      dimensions.set(idx, dim);
    }

    /** Set dimensions by name. If not empty, the parent group builder must be set. */
    public T setDimensionsByName(String dimString) {
      if (dimString == null || dimString.isEmpty()) {
        return self();
      }
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
        return dimensions.stream().map(Dimension::getShortName).filter(Objects::nonNull).collect(Collectors.toList());
      }
      return ImmutableList.of();
    }

    public String makeDimensionsString() {
      return Dimensions.makeDimensionsString(this.dimensions);
    }

    /**
     * Set the dimensions using all anonymous (unshared) dimensions
     *
     * @param shape defines the dimension lengths. must be &gt; 0, or -1 for VLEN
     * @throws RuntimeException if any shape &lt; 1 and not -1.
     */
    public T setDimensionsAnonymous(int[] shape) {
      this.dimensions = new ArrayList<>(Dimensions.makeDimensionsAnon(shape));
      return self();
    }

    public ImmutableList<Dimension> getDimensions() {
      return ImmutableList.copyOf(this.dimensions);
    }

    public Dimension getDimension(int idx) {
      return this.dimensions.get(idx);
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

    public boolean isVariableLength() {
      return this.dimensions.stream().anyMatch(Dimension::isVariableLength);
    }

    public T setIsScalar() {
      this.dimensions = new ArrayList<>();
      return self();
    }

    // Convenience routines
    public int getRank() {
      return dimensions.size();
    }

    public int[] getShape() {
      return Dimensions.makeShape(dimensions);
    }

    public long getSize() {
      return Dimensions.getSize(dimensions);
    }

    public T setArrayType(ArrayType dataType) {
      this.dataType = dataType;
      return self();
    }

    public String getEnumTypeName() {
      return this.enumTypeName;
    }

    public int getElementSize() {
      return dataType.getSize();
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

    public T setSourceData(Array<?> srcData) {
      this.cache.srcData = srcData;
      return self();
    }

    public T setAutoGen(double start, double incr) {
      this.autoGen = new AutoGen(start, incr);
      return self();
    }

    public T resetAutoGen() {
      this.autoGen = null;
      return self();
    }

    public T resetCache() {
      this.cache = new Cache();
      return self();
    }

    public T setIsCaching(boolean caching) {
      this.cache.isCaching = caching;
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

    /** Copy metadata from orgVar. */
    public T copyFrom(Variable orgVar) {
      setName(orgVar.getShortName());
      setArrayType(orgVar.getArrayType());
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
      setArrayType(builder.dataType);
      addDimensions(builder.dimensions);
      setEnumTypeName(builder.getEnumTypeName());
      setNcfile(builder.ncfile);
      this.parentBuilder = builder.parentBuilder;
      setParentStructure(builder.parentStruct);
      setParentStructureBuilder(builder.parentStructureBuilder);
      setProxyReader(builder.proxyReader); // ??
      setName(builder.shortName);
      setSPobject(builder.spiObject);
      return self();
    }

    @Override
    public String toString() {
      return dataType.toCdl() + " " + shortName;
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

    private Array<?> makeDataArray(ArrayType dtype, List<Dimension> dimensions) {
      int[] shape = Dimensions.makeArraySectionFromDimensions(dimensions).build().getShape();
      Array<?> result = Arrays.makeArray(dtype, (int) Arrays.computeSize(shape), start, incr);
      return Arrays.reshape(result, shape);
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

}
