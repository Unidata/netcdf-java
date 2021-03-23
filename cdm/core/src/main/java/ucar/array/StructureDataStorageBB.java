/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.StructureMembers.Member;
import ucar.nc2.iosp.IospHelper;

/**
 * Storage for StructureDataArray with all data in a single ByteBuffer, using member's offsets and ByteOrder,
 * and a heap for vlen data such as Strings, Vlens, and Sequences.
 * The StructureData are manufactured on the fly, referencing the ByteBuffer and heap for data.
 */
public final class StructureDataStorageBB implements Storage<StructureData> {
  private static final Logger log = LoggerFactory.getLogger(StructureDataStorageBB.class);

  private static final boolean debug = false;
  private final StructureMembers members;
  private final ByteBuffer bbuffer;
  private final int nelems;
  private final int offset;
  private final ArrayList<Object> heap = new ArrayList<>();

  private boolean structuresOnHeap = false;

  public StructureDataStorageBB(StructureMembers members, ByteBuffer bbuffer, int nelems) {
    this.members = members;
    this.bbuffer = bbuffer;
    this.nelems = nelems;
    this.offset = 0;
  }

  StructureDataStorageBB(StructureMembers members, ByteBuffer bbuffer, int nelems, ArrayList<Object> heap, int offset) {
    this.members = members;
    this.bbuffer = bbuffer;
    this.nelems = nelems;
    this.offset = offset;
    this.heap.addAll(heap);
  }

  /** If Structures are stored on the heap. Do not use until better tested. */
  public StructureDataStorageBB setStructuresOnHeap(boolean structuresOnHeap) {
    this.structuresOnHeap = structuresOnHeap;
    return this;
  }

  /** Put the object on the heap, return heap index. */
  public int putOnHeap(Object s) {
    heap.add(s);
    return heap.size() - 1;
  }

  @Override
  public long length() {
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

  /** Get the total size of one Structure in bytes. */
  public int getStructureSize() {
    return members.getStorageSizeBytes();
  }

  // TODO go away in version 7 I hope
  ByteBuffer buffer() {
    return bbuffer;
  }

  /** Copy Array data into ByteBuffer at offset + member.getOffset(). */
  public void setMemberData(int offset, Member member, Array<?> data) {
    int pos = offset + member.getOffset();
    bbuffer.position(pos);
    bbuffer.order(member.getByteOrder());
    if (debug) {
      System.out.printf("setMemberData at = %d member = %s bo = %s%n", pos, member.getName(), member.getByteOrder());
    }
    if (member.isVlen()) {
      // LOOK not making a copy
      int index = this.putOnHeap(data);
      bbuffer.putInt(index);
      return;
    }

    ArrayType dataType = data.getArrayType();
    switch (dataType) {
      case ENUM1:
      case UBYTE:
      case BYTE: {
        Array<Byte> bdata = (Array<Byte>) data;
        for (byte val : bdata) {
          bbuffer.put(val);
        }
        return;
      }
      case OPAQUE: {
        int index = this.putOnHeap(data);
        bbuffer.putInt(index);
        return;
      }
      case CHAR: {
        Array<Character> cdata = (Array<Character>) data;
        for (char val : cdata) {
          bbuffer.put((byte) val);
        }
        return;
      }
      case ENUM2:
      case USHORT:
      case SHORT: {
        Array<Short> sdata = (Array<Short>) data;
        for (short val : sdata) {
          bbuffer.putShort(val);
        }
        return;
      }
      case ENUM4:
      case UINT:
      case INT: {
        Array<Integer> idata = (Array<Integer>) data;
        for (int val : idata) {
          bbuffer.putInt(val);
        }
        return;
      }
      case ULONG:
      case LONG: {
        Array<Long> ldata = (Array<Long>) data;
        for (long val : ldata) {
          bbuffer.putLong(val);
        }
        return;
      }
      case FLOAT: {
        Array<Float> fdata = (Array<Float>) data;
        for (float val : fdata) {
          bbuffer.putFloat(val);
        }
        return;
      }
      case DOUBLE: {
        Array<Double> ddata = (Array<Double>) data;
        for (double val : ddata) {
          bbuffer.putDouble(val);
        }
        return;
      }
      case STRING: {
        // LOOK could put Array<String> onto the heap
        String[] vals = new String[(int) data.length()];
        Array<String> sdata = (Array<String>) data;
        int idx = 0;
        for (String val : sdata) {
          vals[idx++] = val;
        }
        int index = this.putOnHeap(vals);
        bbuffer.putInt(index);
        return;
      }
      case STRUCTURE: {
        Preconditions.checkArgument(member.getStructureMembers() != null);
        StructureDataArray orgArray = (StructureDataArray) data;
        StructureMembers nestedMembers = orgArray.getStructureMembers();
        int length = (int) orgArray.length();
        for (int nrow = 0; nrow < length; nrow++) {
          StructureData orgData = orgArray.get(nrow);
          for (StructureMembers.Member nmember : nestedMembers) {
            int nestedPos = offset + nestedMembers.getStorageSizeBytes() * nrow;
            setMemberData(nestedPos, nmember, orgData.getMemberData(nmember));
          }
        }
        return;
      }
      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
  }

  /** Fast iterator over StructureData objects. */
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

  private final class StructureDataBB extends StructureData {
    private final int recno;

    private StructureDataBB(int recno) {
      super(StructureDataStorageBB.this.members);
      this.recno = recno;
    }

    @Override
    public Array<?> getMemberData(Member m) {
      ArrayType dataType = m.getArrayType();
      if (m.isVlen() || dataType == ArrayType.OPAQUE) {
        return getMemberVlenData(m);
      }

      bbuffer.order(m.getByteOrder());
      int length = m.length();
      int pos = offset + recno * members.getStorageSizeBytes() + m.getOffset();

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
          // char is stored in a single byte
          byte[] array = new byte[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.get(pos + count);
          }
          return new ArrayChar(m.getShape(), new ucar.array.ArrayChar.StorageS(IospHelper.convertByteToChar(array)));
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
          if (heapIdx < 0 || heapIdx >= heap.size()) {
            log.warn("getMemberData = {} heapIdx = {} member = {} bo = {}", pos, heapIdx, m.getName(),
                m.getByteOrder());
          }
          String[] array = (String[]) heap.get(heapIdx);
          return new ArrayString(m.getShape(), new ucar.array.ArrayString.StorageS(array));
        }

        case SEQUENCE: {
          int heapIdx = bbuffer.getInt(pos);
          // System.out.printf("getMemberData get seq %s at heap = %d pos = %d%n", m.getName(), heapIdx, pos);
          return (StructureDataArray) heap.get(heapIdx);
        }

        case STRUCTURE:
          if (structuresOnHeap) {
            int heapIdx = bbuffer.getInt(pos);
            StructureDataArray structArray = (StructureDataArray) heap.get(heapIdx);
            return structArray;
          } else {
            StructureMembers nestedMembers = Preconditions.checkNotNull(m.getStructureMembers());
            Storage<StructureData> nestedStorage = new ucar.array.StructureDataStorageBB(nestedMembers,
                StructureDataStorageBB.this.bbuffer, length, StructureDataStorageBB.this.heap, pos);
            return new StructureDataArray(nestedMembers, m.getShape(), nestedStorage);
          }

        default:
          throw new RuntimeException("unknown dataType " + dataType);
      }
    }

    private Array<?> getMemberVlenData(Member m) {
      bbuffer.order(m.getByteOrder());
      int pos = offset + recno * members.getStorageSizeBytes() + m.getOffset();
      int heapIdx = bbuffer.getInt(pos);
      return (ArrayVlen<?>) heap.get(heapIdx);
    }

  }
}
