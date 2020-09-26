/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import ucar.array.StructureMembers.Member;
import ucar.ma2.DataType;
import ucar.nc2.iosp.IospHelper;

/**
 * Storage for Array<StructureData> with all data in a single ByteBuffer, member offsets and ByteOrder,
 * and a heap for vlen data such as Strings, Vlens, and Sequences. Mimics ArrayStructureBB.
 * The StructureData are manufactured on the fly, referencing the ByteBuffer and heap for data.
 */
public class StructureDataStorageBB implements Storage<StructureData> {
  private final ByteBuffer bbuffer;
  private final int nelems;
  private final StructureMembers members;
  private final ArrayList<Object> heap = new ArrayList<>();
  private boolean charIsOneByte = true;
  private boolean structuresOnHeap = false;

  public StructureDataStorageBB(StructureMembers members, ByteBuffer bbuffer, int nelems) {
    this.members = members;
    this.bbuffer = bbuffer;
    this.nelems = nelems;
  }

  public StructureDataStorageBB setCharIsOneByte(boolean charIsOneByte) {
    this.charIsOneByte = charIsOneByte;
    return this;
  }

  public StructureDataStorageBB setStructuresOnHeap(boolean structuresOnHeap) {
    this.structuresOnHeap = structuresOnHeap;
    return this;
  }

  public int addObjectToHeap(Object s) {
    heap.add(s);
    return heap.size() - 1;
  }

  @Override
  public long getLength() {
    return nelems;
  }

  @Override
  public StructureData get(long elem) {
    return new StructureDataBB((int) elem);
  }

  @Override
  public void arraycopy(int srcPos, Object dest, int destPos, long length) {
    // TODO
  }

  public int getStructureSize() {
    return members.getStorageSizeBytes();
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
      super(StructureDataStorageBB.this.members);
      this.recno = recno;
    }

    @Override
    public Array<?> getMemberData(Member m) {
      DataType dataType = m.getDataType();
      bbuffer.order(m.getByteOrder());
      int length = m.length();
      int pos = recno * members.getStorageSizeBytes() + m.getOffset();

      switch (dataType) {
        case BOOLEAN:
        case UBYTE:
        case ENUM1:
        case BYTE: {
          byte[] array = new byte[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.get(pos + count);
          }
          return new ArrayByte(dataType, m.getShape(), new ucar.array.ArrayByte.StorageS(array));
        }

        case CHAR: {
          if (charIsOneByte) {
            byte[] array = new byte[length];
            for (int count = 0; count < length; count++) {
              array[count] = bbuffer.get(pos + count);
            }
            return new ArrayChar(m.getShape(), new ucar.array.ArrayChar.StorageS(IospHelper.convertByteToChar(array)));
          } else {
            char[] array = new char[length];
            for (int count = 0; count < length; count++) {
              array[count] = bbuffer.getChar(pos + 2 * count);
            }
            return new ArrayChar(m.getShape(), new ucar.array.ArrayChar.StorageS(array));
          }
        }

        case DOUBLE: {
          double[] darray = new double[length];
          for (int count = 0; count < length; count++) {
            darray[count] = bbuffer.getDouble(pos + 8 * count);
          }
          return new ArrayDouble(m.getShape(), new ucar.array.ArrayDouble.StorageD(darray));
        }

        case FLOAT: {
          float[] farray = new float[length];
          for (int count = 0; count < length; count++) {
            farray[count] = bbuffer.getFloat(pos + 4 * count);
          }
          return new ArrayFloat(m.getShape(), new ucar.array.ArrayFloat.StorageF(farray));
        }

        case UINT:
        case ENUM4:
        case INT: {
          int[] array = new int[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.getInt(pos + 4 * count);
          }
          return new ArrayInteger(dataType, m.getShape(), new ucar.array.ArrayInteger.StorageS(array));
        }

        case ULONG:
        case LONG: {
          long[] array = new long[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.getLong(pos + 8 * count);
          }
          return new ArrayLong(dataType, m.getShape(), new ucar.array.ArrayLong.StorageS(array));
        }

        case USHORT:
        case ENUM2:
        case SHORT: {
          short[] array = new short[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.getShort(pos + 2 * count);
          }
          return new ArrayShort(dataType, m.getShape(), new ucar.array.ArrayShort.StorageS(array));
        }

        case STRING: {
          int heapIdx = bbuffer.getInt(pos);
          String[] array = (String[]) heap.get(heapIdx);
          return new ArrayString(m.getShape(), new ucar.array.ArrayString.StorageS(array));
        }

        case SEQUENCE: {
          int heapIdx = bbuffer.getInt(pos);
          StructureDataArray structArray = (StructureDataArray) heap.get(heapIdx);
          return structArray;
        }

        case STRUCTURE:
          if (structuresOnHeap) {
            int heapIdx = bbuffer.getInt(pos);
            StructureDataArray structArray = (StructureDataArray) heap.get(heapIdx);
            return structArray;
          } else {
            StructureMembers nestedMembers = Preconditions.checkNotNull(m.getStructureMembers());
            int totalSize = length * nestedMembers.getStorageSizeBytes();
            ByteBuffer nestedBB = ByteBuffer.wrap(bbuffer.array(), pos, totalSize);
            // LOOK probably wrong
            Storage<StructureData> nestedStorage =
                new ucar.array.StructureDataStorageBB(nestedMembers, nestedBB, length).setCharIsOneByte(charIsOneByte);
            return new StructureDataArray(nestedMembers, m.getShape(), nestedStorage);
          }

        default:
          throw new RuntimeException("unknown dataType " + dataType);
      }
    }
  }
}
