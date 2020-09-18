/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import java.util.List;
import ucar.ma2.DataType;

/**
 * Superclass for implementations of Array of StructureData.
 */
public class StructureDataArray extends ucar.array.Array<StructureData> {
  Storage<StructureData> storageSD;
  StructureMembers members;

  /** Create an empty Array of type StructureData and the given shape. */
  public StructureDataArray(StructureMembers members, int[] shape) {
    super(DataType.STRUCTURE, shape);
    this.members = members;
    storageSD = new StorageSD(new StructureData[(int) indexCalc.getSize()]);
  }

  /** Create an Array of type StructureData and the given shape and storage. */
  public StructureDataArray(StructureMembers members, int[] shape, Storage<StructureData> storageSD) {
    super(DataType.STRUCTURE, shape);
    Preconditions.checkArgument(indexCalc.getSize() == storageSD.getLength());
    this.members = members;
    this.storageSD = storageSD;
  }

  /** Create an Array of type StructureData and the given shape and storage. */
  private StructureDataArray(StructureMembers members, Strides shape, Storage<StructureData> storageSD) {
    super(DataType.STRUCTURE, shape);
    this.members = members;
    this.storageSD = storageSD;
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
    return indexCalc.getSize() * members.getStructureSize();
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    // TODO
  }

  @Override
  Array<StructureData> createView(Strides index) {
    return null;
  }

  @Override
  public Iterator<StructureData> fastIterator() {
    return storageSD.iterator();
  }

  @Override
  public Iterator<StructureData> iterator() {
    return indexCalc.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  public StructureData sum() {
    return null;
  }

  @Override
  public StructureData get(int... index) {
    Preconditions.checkArgument(this.rank == index.length);
    return storageSD.get(indexCalc.get(index));
  }

  @Override
  public StructureData get(ucar.array.Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  Storage<StructureData> storage() {
    return storageSD;
  }

  public void setStructureData(int index, StructureData sdata) {
    // TODO kludge
    ((StorageSD) storageSD).storage[index] = sdata;
  }

  /** Get the size of each StructureData object in bytes. */
  public int getStructureSize() {
    return members.getStructureSize();
  }

  private class CanonicalIterator implements Iterator<StructureData> {
    // used when the data is not in canonical order
    private final Iterator<Integer> iter = indexCalc.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public StructureData next() {
      return storageSD.get(iter.next());
    }
  }

  static class StorageSD implements Storage<StructureData> {
    final StructureData[] storage;

    StorageSD(StructureData[] storage) {
      this.storage = storage;
    }

    @Override
    public long getLength() {
      return storage.length;
    }

    @Override
    public StructureData get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      // TODO
    }

    @Override
    public Iterator<StructureData> iterator() {
      return new Iter();
    }

    private final class Iter implements Iterator<StructureData> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final StructureData next() {
        return storage[count++];
      }
    }
  }

}
