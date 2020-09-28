/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/**
 * Array<StructureData>.
 * Specialization is in Storage<StructureData></StructureData>
 * Not really immutable, since Storage<StructureData> may not be, but thats hidden to the consumer.
 */
@Immutable
public class StructureDataArray extends Array<StructureData> {
  private final Storage<StructureData> storage;
  private final StructureMembers members;

  /** Create an Array of type StructureData and the given shape and storage. */
  public StructureDataArray(StructureMembers members, int[] shape, StructureData[] parray) {
    super(DataType.STRUCTURE, shape);
    this.members = members;
    storage = new StorageSD(parray);
  }

  /** Create an Array of type StructureData and the given shape and storage. */
  public StructureDataArray(StructureMembers members, int[] shape, Storage<StructureData> storage) {
    super(DataType.STRUCTURE, shape);
    Preconditions.checkArgument(indexFn.length() == storage.getLength());
    this.members = members;
    this.storage = storage;
  }

  /** Create an Array of type StructureData and the given indexFn and storage. */
  public StructureDataArray(StructureMembers members, IndexFn indexFn, Storage<StructureData> storage) {
    super(DataType.STRUCTURE, indexFn);
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

  @Override
  public long getSizeBytes() {
    return indexFn.length() * members.getStorageSizeBytes();
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    // TODO
  }

  @Override
  Array<StructureData> createView(IndexFn index) {
    return new StructureDataArray(this.members, indexFn, this.storage);
  }

  @Override
  public Iterator<StructureData> fastIterator() {
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

  /** Get the size of each StructureData object in bytes. */
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

  static class StorageSD implements StorageMutable<StructureData> { // LOOK mutable ??
    final StructureData[] parray;

    StorageSD(StructureData[] parray) {
      this.parray = parray;
    }

    @Override
    public long getLength() {
      return parray.length;
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
    public void set(int index, StructureData value) {
      parray[index] = value;
    }

    @Override
    public Iterator<StructureData> iterator() {
      return new Iter();
    }

    private final class Iter implements Iterator<StructureData> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < parray.length;
      }

      @Override
      public final StructureData next() {
        return parray[count++];
      }
    }
  }

}
