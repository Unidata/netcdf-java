/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import ucar.ma2.*;
import ucar.nc2.*;
import opendap.dap.*;
import java.util.*;

/**
 * A read-only DODS-netCDF Variable. Same as a ucar.nc2.Variable except that
 * it might have type boolean or long. Note that DODS DUInt32 widened to long and
 * DODS DUInt16 widened to int.
 */
class DodsVariable extends ucar.nc2.Variable {

  // use when a dods variable is a scalar
  static DodsVariable.Builder<?> builder(String dodsShortName, DodsV dodsV) {
    DodsVariable.Builder<?> builder = builder().setName(DODSNetcdfFile.makeShortName(dodsShortName))
        .setDataType(dodsV.getDataType()).setSPobject(dodsV);

    // check for netcdf char array
    Dimension strlenDim;
    if ((builder.dataType == DataType.STRING) && (null != (strlenDim = dodsfile.getNetcdfStrlenDim(this)))) {
      List<Dimension> dims = new ArrayList<Dimension>();
      if (strlenDim.getLength() != 0)
        dims.add(dodsfile.getSharedDimension(parentGroup, strlenDim));
      builder.setDimensions(dims);
      builder.setDataType(DataType.CHAR);
    }

    return builder;
  }

  // use when a dods variable is an Array, rank > 0
  static DodsVariable.Builder<?> builder(String dodsShortName, DArray dodsArray, DodsV dodsV) {
    DodsVariable.Builder<?> builder = builder().setName(DODSNetcdfFile.makeShortName(dodsShortName))
        .setDataType(dodsV.getDataType()).setSPobject(dodsV);

    List<Dimension> dims = dodsfile.constructDimensions(parentGroup, dodsArray);

    // check for netcdf char array
    Dimension strlenDim;
    if ((builder.dataType == DataType.STRING) && (null != (strlenDim = dodsfile.getNetcdfStrlenDim(this)))) {

      if (strlenDim.getLength() != 0)
        dims.add(dodsfile.getSharedDimension(parentGroup, strlenDim));
      builder.setDataType(DataType.CHAR);
    }

    builder.setDimensions(dims);
    return builder;
  }

  public String getDodsName() {
    return dodsName;
  }

  protected boolean hasCE() {
    return CE != null;
  }

  protected String nameWithCE() {
    return hasCE() ? getShortName() + CE : getShortName();
  }

  /**
   * Instances which have same content are equal.
   */
  @Override
  public boolean equals(Object oo) {
    if (this == oo)
      return true;
    if (!(oo instanceof DodsVariable))
      return false;
    DodsVariable o = (DodsVariable) oo;
    if (this.CE == null ^ o.CE == null)
      return false;
    return super.equals(oo);
  }

  public int hashCode() {
    int supercode = super.hashCode();
    if (CE != null)
      supercode += (37 * CE.hashCode());
    return supercode;
  }


  ////////////////////////////////////////////////////////
  private final String CE; // projection is allowed
  private final String dodsName;
  private final DODSNetcdfFile dodsfile; // so we dont have to cast everywhere

  protected DodsVariable(DodsVariable.Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.CE = builder.CE;
    this.dodsName = builder.dodsName != null ? builder.dodsName : builder.shortName;
    this.dodsfile = (DODSNetcdfFile) parentGroup.getNetcdfFile();
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  @Override
  public DodsVariable.Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected DodsVariable.Builder<?> addLocalFieldsToBuilder(DodsVariable.Builder<? extends DodsVariable.Builder<?>> b) {
    return (DodsVariable.Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  public static DodsVariable.Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends DodsVariable.Builder<DodsVariable.Builder2> {
    @Override
    protected DodsVariable.Builder2 self() {
      return this;
    }
  }

  /** A builder of DodsVariable. */
  public static abstract class Builder<T extends DodsVariable.Builder<T>> extends Variable.Builder<T> {
    private boolean built;
    private String CE; // projection is allowed
    private String dodsName;

    public T setCE(String CE) {
      this.CE = CE;
      return self();
    }

    public void setDodsName(String name) {
      this.dodsName = name;
    }

    /** Normally this is only called by Group.build() */
    public DodsVariable build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new DodsVariable(this, parentGroup);
    }
  }

}
