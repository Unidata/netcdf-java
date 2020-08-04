/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.util.Indent;
import java.util.Formatter;

/**
 * A Dimension is used to define the array shape of a Variable.
 * A Variable can be thought of as a sampled function with Domain its Dimensions.
 * A Dimension may be shared among Variables, which provides a simple yet powerful way of associating Variables.
 * When a Dimension is shared, it has a unique name within its Group.
 * It may have a coordinate Variable, which gives each index a coordinate value.
 * A private Dimension cannot have a coordinate Variable, so use shared dimensions with coordinates when possible.
 * The Dimension length must be > 0, except for an unlimited dimension which may have length = 0, and a vlen
 * Dimension which has a length known only when the variable is read.
 * <p/>
 * <p>
 * Note: this class has a natural ordering (sort by name) that is inconsistent with equals.
 */
@Immutable
public class Dimension implements Comparable<Dimension> {
  /** A variable-length dimension: the length is not known until the data is read. */
  public static final Dimension VLEN = Dimension.builder().setName("*").setIsVariableLength(true).build();

  /**
   * Default Constructor, shared, not unlimited, not variable length.
   *
   * @param name name must be unique within group
   * @param length length of Dimension
   */
  public Dimension(String name, int length) {
    this(name, length, true, false, false);
  }

  /**
   * General Constructor.
   *
   * @param name name must be unique within group. Can be null/empty only if not shared.
   * @param length length, ignored if isVariableLength, >= 0 if unlimited, else > 0.
   * @param isShared whether its shared or local to Variable.
   * @param isUnlimited whether the length can grow.
   * @param isVariableLength whether the length is unknown until the data is read.
   */
  public Dimension(String name, int length, boolean isShared, boolean isUnlimited, boolean isVariableLength) {
    Preconditions.checkArgument((name != null && !name.trim().isEmpty()) || !isShared,
        "Dimension name can only be null/empty if not shared");
    Preconditions.checkArgument(!isVariableLength || !isUnlimited, "variable length dimension cannot be unlimited");
    Preconditions.checkArgument(!isVariableLength || !isShared, "variable length dimension cannot be shared");

    this.shortName = name == null ? null : name.trim();
    this.isShared = isShared;
    this.isUnlimited = isUnlimited;
    this.isVariableLength = isVariableLength;

    if (isVariableLength) {
      length = -1;
    } else if (isUnlimited) {
      if (length < 0)
        throw new IllegalArgumentException("Unlimited Dimension length =" + length + " must >= 0");
    } else {
      if (length < 1)
        throw new IllegalArgumentException("Dimension length =" + length + " must be > 0");
    }
    this.length = length;
  }

  /** Get the length of the Dimension. */
  public int getLength() {
    return length;
  }

  /** Get the name of the Dimension. */
  public String getShortName() {
    return this.shortName;
  }

  /**
   * If this is a NetCDF unlimited dimension. The length might increase between invocations,
   * but it remains fixed for the lifetime of the NetcdfFile.
   * If you modify the file in a seperate process, you must close and reopen the file.
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
   * Make the full name by searching for the containing group starting from the Variable's parent group.
   * 
   * @throws IllegalStateException if not found in one of the Variable's parent groups.
   */
  public String makeFullName(Variable v) {
    return makeFullName(v.getParentGroup());
  }

  /** Make the full name, starting with any group which contains the dimension in itself or a parent group. */
  public String makeFullName(Group containingGroup) {
    if (!isShared) {
      return String.format("%d", getLength()); // LOOK ??
    }
    Preconditions.checkNotNull(containingGroup);
    Group group = containingGroup;
    while (group != null) {
      if (group.findDimensionLocal(this.shortName) != null) {
        return NetcdfFiles.makeFullNameWithString(group, this.shortName);
      }
      group = group.getParentGroup();
    }
    throw new IllegalStateException(String.format("Dimension %s not found starting from group '%s'", this.shortName,
        containingGroup.getFullName()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Dimension dimension = (Dimension) o;
    return isUnlimited == dimension.isUnlimited && isVariableLength == dimension.isVariableLength
        && isShared == dimension.isShared && getLength() == dimension.getLength()
        && Objects.equal(shortName, dimension.shortName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(shortName, isUnlimited, isVariableLength, isShared, getLength());
  }

  /**
   * Dimensions are compared by name. This method is inconsistent with equals()!
   *
   * @param odim compare to this Dimension
   * @return 0, 1, or -1
   */
  public int compareTo(Dimension odim) {
    String name = getShortName();
    return name.compareTo(odim.getShortName());
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    writeCDL(f, new Indent(2), false);
    return f.toString();
  }

  void writeCDL(Formatter out, Indent indent, boolean strict) {
    String name = strict ? NetcdfFiles.makeValidCDLName(getShortName()) : getShortName();
    out.format("%s%s", indent, name);
    if (isUnlimited())
      out.format(" = UNLIMITED;   // (%d currently)", getLength());
    else if (isVariableLength())
      out.format(" = UNKNOWN;");
    else
      out.format(" = %d;", getLength());
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private final String shortName;
  private final boolean isUnlimited;
  private final boolean isVariableLength;
  private final boolean isShared; // shared means its in a group dimension list.
  private final int length;

  private Dimension(Builder builder) {
    this(builder.shortName, builder.length, builder.isShared, builder.isUnlimited, builder.isVariableLength);
  }

  /** Turn into a mutable Builder, use toBuilder().build() to make a copy. */
  public Builder toBuilder() {
    return builder().setName(this.shortName).setIsUnlimited(this.isUnlimited).setIsVariableLength(this.isVariableLength)
        .setIsShared(this.isShared).setLength(getLength());
  }

  //////////////////////////////////////////////////////////////

  public static Builder builder() {
    return new Builder();
  }

  /** A builder with the Dimension name and length set */
  public static Builder builder(String name, int length) {
    return new Builder().setName(name).setLength(length);
  }

  /** A builder of Dimension. */
  public static class Builder {
    private String shortName;
    private boolean isUnlimited;
    private boolean isVariableLength;
    private boolean isShared = true; // shared means its in a group dimension list.
    private int length;
    private boolean built;

    private Builder() {}

    /**
     * Set is unlimited.
     * Used by netcdf3 for variables whose outer dimension can change
     */
    public Builder setIsUnlimited(boolean isUnlimited) {
      this.isUnlimited = isUnlimited;
      return this;
    }

    /**
     * Set variable length is true.
     * Implies that its not shared, nor unlimited.
     * Used by sequences.
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
      this.shortName = NetcdfFiles.makeValidCdmObjectName(shortName);
      return this;
    }

    public Dimension build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new Dimension(this);
    }

  }

}
