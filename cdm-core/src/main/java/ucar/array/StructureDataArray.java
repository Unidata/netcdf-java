/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * Concrete implementation of Array specialized for StructureData.
 * Not really immutable, since Storage&lt;StructureData&gt; may not be, but thats hidden to the consumer.
 */
@Immutable
public final class StructureDataArray extends Array<StructureData> {
  private final Storage<StructureData> storage;
  private final StructureMembers members;

  /** Create an Array of type StructureData and the given shape and storage. Ok if parray is bigger than shape. */
  public StructureDataArray(StructureMembers members, int[] shape, StructureData[] parray) {
    super(ArrayType.STRUCTURE, shape);
    this.members = members;
    storage = new StorageSD(parray, (int) indexFn.length());
  }

  /** Create an Array of type StructureData and the given shape and storage. */
  public StructureDataArray(StructureMembers members, int[] shape, Storage<StructureData> storage) {
    super(ArrayType.STRUCTURE, shape);
    if (indexFn.length() != storage.length()) {
      throw new IllegalArgumentException(
          String.format("indexFn.length %d != %d storage.length", indexFn.length(), storage.length()));
    }
    this.members = members;
    this.storage = storage;
  }

  /** Create an Array of type StructureData and the given indexFn and storage. */
  StructureDataArray(StructureMembers members, IndexFn indexFn, Storage<StructureData> storage) {
    super(ArrayType.STRUCTURE, indexFn);
    this.members = members;
    this.storage = storage;
  }

  /** Get the StructureMembers. */
  public StructureMembers getStructureMembers() {
    return members;
  }

  /** Get a list of structure member names. */
  public List<String> getStructureMemberNames() {
    return members.getMemberNames();
  }

  /**
   * Copies the internal data to dest. The parameters are different from the normal case.
   *
   * @param srcPos the starting byte offset into dest.
   * @param dest must be a StructureDataStorageBB (LOOK or StorageSD?)
   * @param destPos the starting byte offset into dest.
   * @param length number of bytes to copy.
   */
  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    storage.arraycopy(srcPos, dest, destPos, length);
  }

  @Override
  Array<StructureData> createView(IndexFn view) {
    return new StructureDataArray(this.members, view, this.storage);
  }

  @Override
  Iterator<StructureData> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<StructureData> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public StructureData get(int... index) {
    Preconditions.checkArgument(this.rank == index.length);
    return storage.get(indexFn.get(index));
  }

  @Override
  public StructureData get(Index index) {
    return get(index.getCurrentIndex());
  }

  /** Get the size of one StructureData in bytes. */
  public int getStructureSize() {
    return members.getStorageSizeBytes();
  }

  @Override
  Storage<StructureData> storage() {
    return storage;
  }

  private class CanonicalIterator implements Iterator<StructureData> {
    // used when the data is not in canonical order
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public StructureData next() {
      return storage.get(iter.next());
    }
  }

  static final class StorageSD implements StorageMutable<StructureData> { // LOOK mutable ??
    final StructureData[] parray;
    final int length;

    StorageSD(StructureData[] parray, int length) {
      Preconditions.checkArgument(parray.length >= length);
      this.parray = parray;
      this.length = length;
    }

    @Override
    public long length() {
      return length;
    }

    @Override
    public StructureData get(long elem) {
      return parray[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      // TODO
    }

    @Override
    public void setPrimitiveArray(int index, Object value) {
      parray[index] = (StructureData) value;
    }

    @Override
    public Object getPrimitiveArray(int elem) {
      return parray[elem];
    }

    @Override
    public Iterator<StructureData> iterator() {
      return new Iter();
    }

    private final class Iter implements Iterator<StructureData> {
      private int count = 0;

      @Override
      public boolean hasNext() {
        return count < length;
      }

      @Override
      public StructureData next() {
        return parray[count++];
      }
    }
  }


  //////////////////////////////////////////////////////////////////////////

  /**
   * Extract data for one member, over all structures.
   * The resulting shape is the structure shape appended to the member's shape.
   * LOOK problem with this for sequences
   *
   * @param m get all data for this StructureMembers.Member.
   */
  public Array<?> extractMemberArray(StructureMembers.Member m) {
    Preconditions.checkArgument(members.getMembers().contains(m));
    ArrayType dataType = m.getArrayType();

    List<Array<?>> memberData = new ArrayList<>();
    for (StructureData sdata : this) {
      memberData.add(sdata.getMemberData(m));
    }

    // combine the shapes
    int[] mshape = m.getShape();
    int rrank = rank + mshape.length;
    int[] rshape = new int[rrank];
    System.arraycopy(getShape(), 0, rshape, 0, rank);
    System.arraycopy(mshape, 0, rshape, rank, mshape.length);

    return Arrays.combine(dataType, rshape, memberData);
  }

  /**
   * Extract data for one member, over all nested structures.
   * The resulting shape is the structure shape appended to the member's shape.
   * LOOK problem with this for sequences
   *
   * @param nestedStruct th parent Stucture of m.
   * @param m get all data for this StructureMembers.Member.
   */
  public Array<?> extractNestedMemberArray(StructureMembers.Member nestedStruct, StructureMembers.Member m) {
    Preconditions.checkArgument(members.getMembers().contains(nestedStruct));
    Preconditions.checkArgument(nestedStruct.getArrayType().isStruct());

    StructureMembers nested = nestedStruct.getStructureMembers();
    Preconditions.checkNotNull(nested);
    Preconditions.checkArgument(nested.getMembers().contains(m));

    List<Array<?>> memberData = new ArrayList<>();
    for (StructureData sdata : this) {
      Array<StructureData> nsdata = (Array<StructureData>) sdata.getMemberData(nestedStruct);
      for (StructureData ndata : nsdata) {
        memberData.add(ndata.getMemberData(m));
      }
    }

    // combine the shapes
    int nrank = nestedStruct.getShape().length;
    int mrank = m.getShape().length;
    int[] rshape = new int[rank + nrank + mrank];
    System.arraycopy(getShape(), 0, rshape, 0, rank);
    System.arraycopy(nestedStruct.getShape(), 0, rshape, rank, nrank);
    System.arraycopy(m.getShape(), 0, rshape, rank + nrank, mrank);

    return Arrays.combine(m.getArrayType(), rshape, memberData);
  }

}
