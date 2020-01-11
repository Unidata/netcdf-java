/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.ma2.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Sequence is a one-dimensional Structure with indeterminate length.
 * The only data access is through getStructureIterator().
 * However, read() will read in the entire data and return an ArraySequence.
 * 
 * @author caron
 * @since Feb 23, 2008
 */
public class Sequence extends Structure {

  /**
   * Sequence Constructor
   * 
   * @param ncfile the containing NetcdfFile.
   * @param group the containing group; if null, use rootGroup
   * @param parent parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   * @deprecated use Builder.
   */
  @Deprecated
  public Sequence(NetcdfFile ncfile, Group group, Structure parent, String shortName) {
    super(ncfile, group, parent, shortName);

    List<Dimension> dims = new ArrayList<>();
    dims.add(Dimension.VLEN);
    setDimensions(dims);
    setDataType(DataType.SEQUENCE);
  }

  public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    return ncfile.getStructureIterator(this, bufferSize);
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public Array read(int[] origin, int[] shape) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public Array read(String sectionSpec) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public Array read(List<Range> ranges) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Array read(ucar.ma2.Section section) throws java.io.IOException {
    return read();
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public StructureData readStructure() {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public StructureData readStructure(int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public ArrayStructure readStructure(int start, int count) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public Variable slice(int dim, int value) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public Variable section(Section subsection) {
    throw new UnsupportedOperationException();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  protected Sequence(Builder<?> builder) {
    super(builder);
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
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

  public static abstract class Builder<T extends Builder<T>> extends Structure.Builder<T> {
    private boolean built;

    protected abstract T self();

    public Sequence build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setDataType(DataType.SEQUENCE);
      return new Sequence(this);
    }
  }

}
