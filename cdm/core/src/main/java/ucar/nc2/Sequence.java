/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.IOException;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;
import ucar.array.ArraysConvert;
import ucar.ma2.*;
import java.util.List;

/**
 * Sequence is a one-dimensional Structure with indeterminate length, including 0.
 * The only data access is through getStructureIterator().
 * However, read() will read in the entire data and return an in-memory ArraySequence.
 */
@Immutable
public class Sequence extends Structure implements Iterable<ucar.array.StructureData> {

  /** @deprecated use iterator() */
  @Deprecated
  public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    if (cache.getData() != null) {
      ucar.array.Array<?> array = cache.getData();
      if (array instanceof ucar.array.StructureDataArray) {
        ucar.ma2.Array ma2 = ArraysConvert.convertFromArray(array);
        if (ma2 instanceof ArrayStructure) {
          return ((ArrayStructure) ma2).getStructureDataIterator();
        }
      }
    }
    if (ncfile != null) {
      return ncfile.getStructureIterator(this, bufferSize);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<ucar.array.StructureData> iterator() {
    if (cache.getData() != null) {
      ucar.array.Array<?> array = cache.getData();
      if (array instanceof ucar.array.StructureDataArray) {
        return (Iterator<ucar.array.StructureData>) array;
      }
    }
    try {
      return ncfile.getStructureDataArrayIterator(this, -1);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Same as read() */
  @Override
  @Deprecated
  public Array read(ucar.ma2.Section section) throws java.io.IOException {
    return read();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  @Deprecated
  public Array read(int[] origin, int[] shape) {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  @Deprecated
  public Array read(String sectionSpec) {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  @Deprecated
  public Array read(List<Range> ranges) {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  @Deprecated
  public StructureData readStructure(int index) {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  @Deprecated
  public ArrayStructure readStructure(int start, int count) {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  public Variable slice(int dim, int value) {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  public Variable section(Section subsection) {
    throw new UnsupportedOperationException();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  protected Sequence(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
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

  /** A builder of Sequences */
  public static abstract class Builder<T extends Builder<T>> extends Structure.Builder<T> {
    private boolean built;

    protected abstract T self();

    public Sequence build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setDataType(DataType.SEQUENCE);
      return new Sequence(this, parentGroup);
    }
  }

}
