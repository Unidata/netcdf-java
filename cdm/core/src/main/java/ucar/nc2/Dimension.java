/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.nc2.util.Indent;
import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A Dimension is used to define the array shape of a Variable.
 * It may be shared among Variables, which provides a simple yet powerful way of associating Variables.
 * When a Dimension is shared, it has a unique name within its Group.
 * It may have a coordinate Variable, which gives each index a coordinate value.
 * A private Dimension cannot have a coordinate Variable, so use shared dimensions with coordinates when possible.
 * The Dimension length must be > 0, except for an unlimited dimension which may have length = 0, and a vlen
 * Dimension which has length = -1.
 * <p/>
 * <p>
 * Immutable if setImmutable() was called, except for an Unlimited Dimension, whose size can change.
 * <p>
 * Note: this class has a natural ordering that is inconsistent with equals.
 *
 * @author caron
 */

public class Dimension extends CDMNode implements Comparable<Dimension> {
  /** A variable-length dimension: the length is not known until the data is read. */
  public static Dimension VLEN = Dimension.builder().setName("*").setIsVariableLength(true).build().setImmutable();

  /**
   * Make a space-delineated String from a list of Dimension names.
   * Inverse of makeDimensionsList().
   *
   * @return space-delineated String of Dimension names.
   */
  public static String makeDimensionsString(List<Dimension> dimensions) {
    if (dimensions == null)
      return "";

    Formatter buf = new Formatter();
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension myd = dimensions.get(i);
      String dimName = myd.getShortName();

      if (i != 0)
        buf.format(" ");

      if (myd.isVariableLength()) {
        buf.format("*");
      } else if (myd.isShared()) {
        buf.format("%s", dimName);
      } else {
        // if (dimName != null) // LOOK losing anon dim name
        // buf.format("%s=", dimName);
        buf.format("%d", myd.getLength());
      }
    }
    return buf.toString();
  }

  /**
   * Create a dimension list using the dimensions names. The dimension is searched for recursively in the parent groups,
   * so it must already have been added.
   * Inverse of makeDimensionsString().
   *
   * @param parentGroup containing group, may not be null
   * @param dimString : whitespace separated list of dimension names, or '*' for Dimension.UNKNOWN, or number for anon
   *        dimension. null or empty String is a scalar.
   * @return list of dimensions
   * @throws IllegalArgumentException if cant find dimension or parse error.
   */
  public static List<Dimension> makeDimensionsList(Group parentGroup, String dimString)
      throws IllegalArgumentException {
    List<Dimension> newDimensions = new ArrayList<>();
    if (dimString == null) // scalar
      return newDimensions; // empty list
    dimString = dimString.trim();
    if (dimString.isEmpty()) // scalar
      return newDimensions; // empty list

    StringTokenizer stoke = new StringTokenizer(dimString);
    while (stoke.hasMoreTokens()) {
      String dimName = stoke.nextToken();
      Dimension d;
      if (dimName.equals("*"))
        d = Dimension.VLEN;
      else
        d = parentGroup.findDimension(dimName);
      if (d == null) {
        // if numeric - then its anonymous dimension
        try {
          int len = Integer.parseInt(dimName);
          d = new Dimension(null, len, false, false, false);
        } catch (Exception e) {
          throw new IllegalArgumentException("Dimension " + dimName + " does not exist");
        }
      }
      newDimensions.add(d);
    }

    return newDimensions;
  }

  @Deprecated
  public static List<Dimension> makeDimensionsAnon(int[] shape) {
    List<Dimension> newDimensions = new ArrayList<>();

    if ((shape == null) || (shape.length == 0)) { // scalar
      return newDimensions; // empty list
    }

    for (int s : shape)
      newDimensions.add(new Dimension(null, s, false, false, false));

    return newDimensions;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // TODO make these final in 6.
  private boolean isUnlimited;
  private boolean isVariableLength;
  private boolean isShared; // shared means its in a group dimension list.
  private int length;

  Dimension(Builder builder) {
    super(builder.shortName);
    setParentGroup(builder.parent);
    this.isShared = builder.isShared;
    this.isVariableLength = builder.isVariableLength;
    this.isUnlimited = builder.isUnlimited;
    this.length = builder.length;
  }

  public Builder toBuilder() {
    return builder()
        .setName(this.shortName)
        .setGroup(this.getGroup())
        .setIsUnlimited(this.isUnlimited)
        .setIsVariableLength(this.isVariableLength)
        .setIsShared(this.isShared)
        .setLength(this.length);
  }

  /**
   * Get the length of the Dimension.
   *
   * @return length of Dimension
   */
  public int getLength() {
    return length;
  }

  /**
   * If unlimited, then the length can increase; otherwise it is immutable.
   *
   * @return if its an "unlimited" Dimension
   */
  public boolean isUnlimited() {
    return isUnlimited;
  }

  /**
   * If variable length, then the length is unknown until the data is read.
   *
   * @return if its a "variable length" Dimension.
   */
  public boolean isVariableLength() {
    return isVariableLength;
  }

  /**
   * If this Dimension is shared, or is private to a Variable.
   * All Dimensions in NetcdfFile.getDimensions() or Group.getDimensions() are shared.
   * Dimensions in the Variable.getDimensions() may be shared or private.
   *
   * @return if its a "shared" Dimension.
   */
  public boolean isShared() {
    return isShared;
  }

  /**
   * Get the Group that owns this Dimension.
   *
   * @return owning group or null if !isShared
   * @deprecated Do not use.
   */
  public Group getGroup() {
    return getParentGroup();
  }

  public String makeFullName() {
    return super.getFullName();
  }

  /**
   * Instances which have same contents are equal.
   * Careful!! this is not object identity !!
   */
  @Override
  public boolean equals(Object oo) {
    if (this == oo)
      return true;
    if (!(oo instanceof Dimension))
      return false;
    Dimension other = (Dimension) oo;
    Group g = getGroup();
    if ((g != null) && !g.equals(other.getGroup()))
      return false;
    if ((getShortName() == null) && (other.getShortName() != null))
      return false;
    if ((getShortName() != null) && !getShortName().equals(other.getShortName()))
      return false;
    return (getLength() == other.getLength()) && (isUnlimited() == other.isUnlimited())
        && (isVariableLength() == other.isVariableLength()) && (isShared() == other.isShared());
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  @Override
  public int hashCode() {
    int result = 17;
    Group g = getGroup();
    if (g != null)
      result += 37 * result + g.hashCode();
    if (null != getShortName())
      result += 37 * result + getShortName().hashCode();
    result += 37 * result + getLength();
    result += 37 * result + (isUnlimited() ? 0 : 1);
    result += 37 * result + (isVariableLength() ? 0 : 1);
    result += 37 * result + (isShared() ? 0 : 1);
    return result;
  }

  /**
   * CDL representation, not strict.
   */
  @Override
  public String toString() {
    return writeCDL(false);
  }

  /**
   * Dimensions with the same name are equal. This method is inconsistent with equals()!
   *
   * @param odim compare to this Dimension
   * @return 0, 1, or -1
   */
  public int compareTo(Dimension odim) {
    String name = getShortName();
    return name.compareTo(odim.getShortName());
  }

  /**
   * CDL representation.
   *
   * @param strict if true, write in strict adherence to CDL definition.
   * @return CDL representation.
   */
  public String writeCDL(boolean strict) {
    Formatter f = new Formatter();
    writeCDL(f, new Indent(2), strict);
    return f.toString();
  }

  protected void writeCDL(Formatter out, Indent indent, boolean strict) {
    String name = strict ? NetcdfFile.makeValidCDLName(getShortName()) : getShortName();
    out.format("%s%s", indent, name);
    if (isUnlimited())
      out.format(" = UNLIMITED;   // (%d currently)", getLength());
    else if (isVariableLength())
      out.format(" = UNKNOWN;");
    else
      out.format(" = %d;", getLength());
  }

  /**
   * Constructor
   *
   * @param name name must be unique within group
   * @param length length of Dimension
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public Dimension(String name, int length) {
    this(name, length, true, false, false);
  }

  /**
   * Constructor
   *
   * @param name name must be unique within group
   * @param length length, or UNLIMITED.length or UNKNOWN.length
   * @param isShared whether its shared or local to Variable.
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public Dimension(String name, int length, boolean isShared) {
    this(name, length, isShared, false, false);
  }

  /**
   * Constructor
   *
   * @param name name must be unique within group. Can be null only if not shared.
   * @param length length, or UNLIMITED.length or UNKNOWN.length
   * @param isShared whether its shared or local to Variable.
   * @param isUnlimited whether the length can grow.
   * @param isVariableLength whether the length is unknown until the data is read.
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public Dimension(String name, int length, boolean isShared, boolean isUnlimited, boolean isVariableLength) {
    super(name);
    this.isShared = isShared;
    this.isUnlimited = isUnlimited;
    this.isVariableLength = isVariableLength;
    if (isVariableLength && (isUnlimited || isShared))
      throw new IllegalArgumentException("variable length dimension cannot be shared or unlimited");
    setLength(length);
    assert (name != null) || !this.isShared;
  }

  /**
   * Copy Constructor. used to make global dimensions
   *
   * @param name name must be unique within group. Can be null only if not shared.
   * @param from copy all other fields from here.
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public Dimension(String name, Dimension from) {
    super(name);
    this.length = from.length;
    this.isUnlimited = from.isUnlimited;
    this.isVariableLength = from.isVariableLength;
    this.isShared = from.isShared;
  }

  ///////////////////////////////////////////////////////////
  // the following make this mutable

  /**
   * Set whether this is unlimited, meaning length can increase.
   *
   * @param b true if unlimited
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public void setUnlimited(boolean b) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    this.isUnlimited = b;
    setLength(this.length); // check legal
  }

  /**
   * Set whether the length is variable.
   *
   * @param b true if variable length
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public void setVariableLength(boolean b) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    this.isVariableLength = b;
    if (b) {
      this.isShared = false;
      this.isUnlimited = false;
    }
    setLength(this.length); // check legal
  }

  /**
   * Set whether this is shared.
   *
   * @param b true if shared
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public void setShared(boolean b) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    this.isShared = b;
  }

  /**
   * Set the Dimension length.
   *
   * @param n length of Dimension
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public void setLength(int n) {
    if (immutable && !isUnlimited)
      throw new IllegalStateException("Cant modify");
    if (isVariableLength) {
      if (n != -1)
        throw new IllegalArgumentException("VariableLength Dimension length =" + n + " must be -1");
    } else if (isUnlimited) {
      if (n < 0)
        throw new IllegalArgumentException("Unlimited Dimension length =" + n + " must >= 0");
    } else {
      if (n < 1)
        throw new IllegalArgumentException("Dimension length =" + n + " must be > 0");
    }
    this.length = n;
  }


  /**
   * Set the name, converting to valid CDM object name if needed.
   *
   * @param name set to this value
   * @return valid CDM object name
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public String setName(String name) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (name.isEmpty())
      name = null;
    setShortName(name);
    return getShortName();
  }

  /**
   * Set the group
   *
   * @param g parent group
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public void setGroup(Group g) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    setParentGroup(g);
  }


  /**
   * Make this immutable.
   *
   * @return this
   * @deprecated Use Dimension.builder()
   */
  @Deprecated
  public Dimension setImmutable() {
    super.setImmutable();
    return this;
  }

  //////////////////////////////////////////////////////////////

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(String name, int length) {
    return new Builder(name, length);
  }

  public static class Builder {
    private Group parent;
    private String shortName;
    private boolean isUnlimited;
    private boolean isVariableLength;
    private boolean isShared = true; // shared means its in a group dimension list.
    private int length;
    private boolean built;

    private Builder() {}

    private Builder(String name, int length) {
      this.shortName = name;
      this.length = length;
    }

    /** Set is unlimited.
     *  Used by netcdf3 for variables whose outer dimension can change
     */
    public Builder setIsUnlimited(boolean isUnlimited) {
      this.isUnlimited = isUnlimited;
      return this;
    }

    /** Set variable length is true.
     *  Implies that its not shared, nor unlimited.
     *  Used by sequences.
     */
    public Builder setIsVariableLength(boolean isVariableLength) {
      this.isVariableLength = isVariableLength;
      if (isVariableLength) {
        this.isShared = false;
        this.isUnlimited = false;
        this.length = -1;
      }
      return this;
    }

    /**
     * Set whether this is shared.
     * Default value is true.
     * Implies it is owned by a Group.
     */
    public Builder setIsShared(boolean isShared) {
      this.isShared = isShared;
      return this;
    }

    /**
     * Set the Dimension length.
     *
     * @param n length of Dimension
     */
    public Builder setLength(int n) {
      if (isVariableLength) {
        if (n != -1)
          throw new IllegalArgumentException("VariableLength Dimension length =" + n + " must be -1");
      } else if (isUnlimited) {
        if (n < 0)
          throw new IllegalArgumentException("Unlimited Dimension length =" + n + " must >= 0");
      } else {
        if (n < 1)
          throw new IllegalArgumentException("Dimension length =" + n + " must be > 0");
      }
      this.length = n;
      return this;
    }

    public Builder setName(String shortName) {
      this.shortName = shortName;
      return this;
    }

    /**
     * Set the parent group.
     * @deprecated Will not use in 6.0
     */
    @Deprecated
    public Builder setGroup(Group parent) {
      this.parent = parent;
      return this;
    }

    public Dimension build() {
      if (built) throw new IllegalStateException("already built");
      built = true;
      return new Dimension(this);
    }

  }

}
