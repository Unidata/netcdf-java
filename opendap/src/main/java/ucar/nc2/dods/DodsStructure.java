/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import ucar.ma2.*;
import ucar.nc2.*;
import opendap.dap.*;
import java.util.*;
import java.io.IOException;

/** A DODS Structure. */
class DodsStructure extends ucar.nc2.Structure {

  // constructor called from DODSNetcdfFile.makeVariable() for scalar Structure or Sequence
  static DodsStructure.Builder<?> builder(DodsBuilder<?> dodsBuilder, Group.Builder parentGroup, String dodsShortName,
      DodsV dodsV) throws IOException {
    DodsStructure.Builder<?> builder = builder().setName(DodsNetcdfFiles.makeShortName(dodsShortName))
        .setDataType(dodsV.getDataType()).setSPobject(dodsV);
    builder.setConstructor((DConstructor) dodsV.bt);

    if (builder.constructor instanceof DSequence) {
      builder.addDimension(Dimension.VLEN);
    }

    for (DodsV nested : dodsV.children) {
      dodsBuilder.addVariable(parentGroup, builder, nested);
    }

    return builder;
  }

  // constructor called from DODSNetcdfFile.makeVariable() for array of Structure
  static DodsStructure.Builder<?> builder(DodsBuilder<?> dodsBuilder, Group.Builder parentGroup, String dodsShortName,
      DArray dodsArray, DodsV dodsV) {
    DodsStructure.Builder<?> builder = builder().setName(DodsNetcdfFiles.makeShortName(dodsShortName))
        .setDataType(dodsV.getDataType()).setSPobject(dodsV);
    List<Dimension> dims = dodsBuilder.constructDimensions(parentGroup, dodsArray);
    builder.setDimensions(dims);
    return builder;
  }

  DConstructor getDConstructor() {
    return this.constructor;
  }

  protected String getDodsName() {
    return dodsName;
  }

  ///////////////////////////////////////////////////////////////////////
  /**
   * Return an iterator over the set of repeated structures. The iterator
   * will return an object of type Structure. When you call this method, the
   * Sequence will be read using the given constraint expression, and the data
   * returned sequentially.
   *
   * <br>
   * If the data has been cached by a read() to an enclosing container, you must
   * leave the CE null. Otherwise a new call will be made to the server.
   *
   * @param CE constraint expression, or null.
   * @return iterator over type DODSStructure.
   * @see DodsStructure
   * @throws java.io.IOException on io error
   */
  public StructureDataIterator getStructureIterator(String CE) throws java.io.IOException {
    return new SequenceIterator(CE);
  }

  private class SequenceIterator implements StructureDataIterator {
    private int nrows, row = 0;
    private ArrayStructure structArray;

    SequenceIterator(String CE) throws java.io.IOException {
      // nothin better to do for now !!
      structArray = (ArrayStructure) read();
      nrows = (int) structArray.getSize();
    }

    @Override
    public boolean hasNext() {
      return row < nrows;
    }

    @Override
    public StructureData next() {
      return structArray.getStructureData(row++);
    }

    @Override
    public StructureDataIterator reset() {
      row = 0;
      return this;
    }

    @Override
    public int getCurrentRecno() {
      return row - 1;
    }

  }

  ////////////////////////////////////////////////////////
  private final DConstructor constructor;
  private final DodsNetcdfFile dodsfile; // so we dont have to cast everywhere
  private final String dodsName;

  protected DodsStructure(DodsStructure.Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.constructor = builder.constructor;
    this.isVariableLength = builder.constructor instanceof DSequence;
    this.dodsName = builder.dodsName != null ? builder.dodsName : builder.shortName;
    this.dodsfile = (DodsNetcdfFile) parentGroup.getNetcdfFile();
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  @Override
  public DodsStructure.Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected DodsStructure.Builder<?> addLocalFieldsToBuilder(
      DodsStructure.Builder<? extends DodsStructure.Builder<?>> b) {
    this.members.forEach(m -> b.addMemberVariable(m.toBuilder()));
    return (DodsStructure.Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /**
   * Get Builder for this class that allows subclassing.
   *
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static DodsStructure.Builder<?> builder() {
    return new DodsStructure.Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  /** A builder of Structures. */
  public static abstract class Builder<T extends DodsStructure.Builder<T>> extends Structure.Builder<T>
      implements DodsVariableBuilder<T> {
    private boolean built;
    private String dodsName;
    private DConstructor constructor;
    private String CE; // projection is allowed

    public void setDodsName(String name) {
      this.dodsName = name;
    }

    public void setConstructor(DConstructor constructor) {
      this.constructor = constructor;
    }

    public T setCE(String CE) {
      this.CE = CE;
      return self();
    }

    /** Normally this is only called by Group.build() */
    public DodsStructure build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setDataType(DataType.STRUCTURE);
      return new DodsStructure(this, parentGroup);
    }
  }

}
