/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import ucar.array.ArrayDouble.StorageD;
import ucar.array.ArrayFloat.StorageF;
import ucar.array.StructureMembers.Member;
import ucar.ma2.DataType;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** Superclass for implementations of Array of StructureData. */
public class StructureDataArray2 extends Array<StructureData> {

  public static int[] makeMemberOffsets(Structure structure) {
    int pos = 0;
    int count = 0;
    int[] offset = new int[structure.getNumberOfMemberVariables()];
    for (Variable v2 : structure.getVariables()) {
      offset[count++] = pos;
      pos += v2.getSize();
    }
    return offset;
  }

  ////////////////////////////////////////////////////////////////////////////
  private final StorageBB storageSD;
  private final StructureMembers members;
  private ArrayList<Object> heap = new ArrayList<>();

  /** Create an Array of type StructureData and the given shape and storage with ByteBuffer. */
  public StructureDataArray2(StructureMembers members, int[] shape, ByteBuffer bytes) {
    super(DataType.STRUCTURE, shape);
    this.members = members;
    this.storageSD = new StorageBB(bytes, members, (int) length());;
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
    return indexFn.length() * members.getStructureSize();
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    // TODO
  }

  @Override
  Array<StructureData> createView(IndexFn index) {
    return null;
  }

  @Override
  public Iterator<StructureData> fastIterator() {
    return storageSD.iterator();
  }

  @Override
  public Iterator<StructureData> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  public StructureData sum() {
    return null;
  }

  @Override
  public StructureData get(int... index) {
    Preconditions.checkArgument(this.rank == index.length);
    return storageSD.get(indexFn.get(index));
  }

  @Override
  public StructureData get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  Storage<StructureData> storage() {
    return storageSD;
  }

  /** Get the size of each StructureData object in bytes. */
  public int getStructureSize() {
    return members.getStructureSize();
  }

  public int addObjectToHeap(Object s) {
    heap.add(s);
    return heap.size() - 1;
  }

  public ByteBuffer getByteBuffer() {
    return storageSD.bbuffer;
  }

  /////////////////////////////////////////////////////////////////

  private class CanonicalIterator implements Iterator<StructureData> {
    // used when the data is not in canonical order
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public StructureData next() {
      return storageSD.get(iter.next());
    }
  }

  ////////////////////////////////////////////////////////////

  static class StorageBB implements Storage<StructureData> {
    private final ByteBuffer bbuffer;
    private final int nelems;
    private final StructureMembers members;

    StorageBB(ByteBuffer bbuffer, StructureMembers members, int nelems) {
      this.bbuffer = bbuffer;
      this.members = members;
      this.nelems = nelems;
    }

    @Override
    public long getLength() {
      return bbuffer.array().length;
    }

    @Override
    public StructureData get(long elem) {
      return new StructureDataBB((int) elem);
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
        return count < nelems;
      }

      @Override
      public final StructureData next() {
        return new StructureDataBB(count++);
      }
    }

    class StructureDataBB extends StructureData {
      private final int recno;

      private StructureDataBB(int recno) {
        super(StorageBB.this.members);
        this.recno = recno;
      }

      @Override
      public Array<?> getMemberData(Member m) {
        DataType dataType = m.getDataType();
        bbuffer.order(m.getByteOrder());
        int length = m.length();
        int pos = recno * members.getStructureSize() + m.getOffset();

        switch (dataType) {
          case DOUBLE:
            double[] darray = new double[length];
            for (int count = 0; count < length; count++) {
              darray[count] = bbuffer.getDouble(pos + 8 * count);
            }
            return new ArrayDouble(m.getShape(), new StorageD(darray));

          case FLOAT:
            float[] farray = new float[length];
            for (int count = 0; count < length; count++) {
              farray[count] = bbuffer.getFloat(pos + 4 * count);
            }
            return new ArrayFloat(m.getShape(), new StorageF(farray));

          case STRUCTURE:
            StructureMembers nestedMembers = Preconditions.checkNotNull(m.getStructureMembers());
            int totalSize = length * nestedMembers.getStructureSize();
            ByteBuffer nestedBB = ByteBuffer.wrap(bbuffer.array(), pos, totalSize);
            // public StructureDataArray(StructureMembers members, int[] shape, ByteBuffer bytes, int[] offsets) {
            // look if members had offsets, could construct offset[]. Or wouldnt need it.
            return new StructureDataArray2(nestedMembers, m.getShape(), nestedBB);

          default:
            return null;
          // throw new RuntimeException("unknown dataType " + dataType);
        }
      }
    }
  }

}
