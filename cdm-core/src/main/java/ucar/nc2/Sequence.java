/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import ucar.array.ArrayType;
import ucar.array.Section;
import ucar.array.StructureData;
import ucar.array.StructureDataArray;
import ucar.array.StructureMembers;

/**
 * A one-dimensional Structure with indeterminate length, possibly 0.
 * The only data access is through getStructureIterator().
 */
@Immutable
public class Sequence extends Structure implements Iterable<StructureData> {

  /** An iterator over all the data in the sequence. */
  @Override
  public Iterator<ucar.array.StructureData> iterator() {
    if (cache.getData() != null) {
      ucar.array.Array<?> array = cache.getData();
      if (array instanceof ucar.array.StructureDataArray) {
        return ((StructureDataArray) array).iterator();
      }
    }
    try {
      Preconditions.checkNotNull(ncfile);
      return ncfile.getSequenceIterator(this, -1);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Read all data into memory. */
  @Override
  public StructureDataArray readArray() {
    List<StructureData> list = new ArrayList<>();
    for (StructureData sdata : this) {
      list.add(sdata);
    }
    StructureMembers members = list.size() > 0 ? list.get(0).getStructureMembers()
        : makeStructureMembersBuilder().setStandardOffsets().build();
    StructureData[] arr = list.toArray(new StructureData[0]);
    return new StructureDataArray(members, new int[] {list.size()}, arr);
  }

  /** @throws UnsupportedOperationException always */
  @Override
  public Variable section(Section subsection) {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  public Variable slice(int dim, int value) {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  public StructureData readRecord(int recno) {
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

  public static abstract class Builder<T extends Builder<T>> extends Structure.Builder<T> {
    private boolean built;

    protected abstract T self();

    public Sequence build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setArrayType(ArrayType.SEQUENCE);
      return new Sequence(this, parentGroup);
    }
  }

}
