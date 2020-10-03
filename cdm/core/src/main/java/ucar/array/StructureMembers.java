/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.array.StructureMembers.Member;
import ucar.ma2.DataType;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** A Collection of members contained in StructureData. */
@Immutable
public final class StructureMembers implements Iterable<Member> {

  public static StructureMembers.Builder makeStructureMembers(Structure structure) {
    Builder builder = builder().setName(structure.getShortName());
    for (Variable v2 : structure.getVariables()) {
      MemberBuilder m = builder.addMember(v2.getShortName(), v2.getDescription(), v2.getUnitsString(), v2.getDataType(),
          v2.getShape());
      if (v2 instanceof Structure) {
        m.setStructureMembers(makeStructureMembers((Structure) v2));
      }
    }
    return builder;
  }

  ////////////////////////////////////////////////////////////////////////

  /** Get the StructureMembers' name. */
  public String getName() {
    return name;
  }

  /** Get the number of members */
  public int numberOfMembers() {
    return members.size();
  }

  /** Get the list of Member objects. */
  public ImmutableList<Member> getMembers() {
    return ImmutableList.copyOf(members);
  }

  /** Get the names of the members. */
  public ImmutableList<String> getMemberNames() {
    return members.stream().map(Member::getName).collect(ImmutableList.toImmutableList());
  }

  /** Get the index-th member */
  public Member getMember(int index) {
    return members.get(index);
  }

  /** Find the member by its name. */
  @Nullable
  public Member findMember(String memberName) {
    if (memberName == null)
      return null;

    return members.stream().filter(m -> m.name.equals(memberName)).findFirst().orElse(null);
  }

  /** Get the total size of one Structure in bytes. */
  public int getStorageSizeBytes() {
    return structureSize;
  }

  public boolean isStructuresOnHeap() {
    return structuresOnHeap;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).add("members", members)
        .add("structureSize", structureSize).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StructureMembers that = (StructureMembers) o;
    return structureSize == that.structureSize && structuresOnHeap == that.structuresOnHeap
        && Objects.equal(name, that.name) && Objects.equal(members, that.members);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, structureSize, structuresOnHeap, members);
  }

  @Override
  public Iterator<Member> iterator() {
    return members.iterator();
  }

  /** A member of a StructureData. */
  @Immutable
  public final static class Member {
    private final String name, desc, units;
    private final DataType dtype;
    private final int index;
    private final int length;
    private final int[] shape;
    private final StructureMembers members; // only if member is type Structure
    private final ByteOrder byteOrder; // needed by StructureDataArray
    private final int offset; // needed by StructureDataArray
    private final int storageSizeInBytes; // needed by StructureDataArray
    private final boolean isVariableLength;

    private Member(MemberBuilder builder, int index, boolean structuresOnHeap) {
      this.index = index;

      this.name = Preconditions.checkNotNull(builder.name);
      this.desc = builder.desc;
      this.units = builder.units;
      this.dtype = Preconditions.checkNotNull(builder.dataType);
      this.shape = builder.shape != null ? builder.shape : new int[0];
      this.members = builder.members != null ? builder.members.build() : null;
      this.byteOrder = builder.byteOrder;
      this.offset = builder.offset;

      // calculated
      this.length = (int) ucar.ma2.Index.computeSize(shape);
      this.isVariableLength = (shape.length > 0 && shape[shape.length - 1] < 0);
      this.storageSizeInBytes = builder.getStorageSizeBytes(structuresOnHeap);
    }

    /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
    public MemberBuilder toBuilder() {
      MemberBuilder b = new MemberBuilder().setName(this.name).setDesc(this.desc).setUnits(this.units)
          .setDataType(this.dtype).setShape(this.shape).setByteOrder(this.getByteOrder()).setOffset(this.getOffset());
      if (this.members != null) {
        b.setStructureMembers(this.members.toBuilder());
      }
      return b;
    }

    @Nullable
    public StructureMembers getStructureMembers() {
      return members;
    }

    /** Get the StructureMember's name. */
    public String getName() {
      return name;
    }

    /** Get the units string, if any. */
    @Nullable
    public String getUnitsString() {
      return units;
    }

    /** Get the description, if any. */
    @Nullable
    public String getDescription() {
      return desc;
    }

    /** Get the DataType. */
    public DataType getDataType() {
      return dtype;
    }

    /** Get the index in the Members list. */
    public int getIndex() {
      return index;
    }

    /** Get the array shape. */
    public int[] getShape() {
      return shape;
    }

    /** Get the total number of elements. */
    public int length() {
      return length;
    }

    public boolean isVariableLength() {
      return isVariableLength;
    }

    public ByteOrder getByteOrder() {
      return byteOrder;
    }

    public int getOffset() {
      return offset;
    }

    /**
     * Get the total size in bytes needed for storing the data in this Member.
     * A Sequence, String and Vlen are always stored on the heap, so this returns 4 bytes used for the heap index.
     * A Structure may be stored on the heap, depending on StructureMembers.isStructuresOnHeap().
     * If true, then takes 4 bytes. If false, then this will be the sum of the member's sizes (including nested
     * Structures) times the number of Strutures.
     *
     * If the Member is a Structure, then the size of one Structure will be getStructureMembers.getStorageSizeBytes().
     *
     * @return total size in bytes
     */
    public int getStorageSizeBytes() {
      return storageSizeInBytes;
    }

    /** Convenience method for members.getStorageSizeBytes(). Throws exception if not a Structure. */
    public int getStructureSize() {
      Preconditions.checkNotNull(this.members);
      return members.getStorageSizeBytes();
    }

    /** Is this a scalar (size == 1). */
    public boolean isScalar() {
      return length == 1;
    }

    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Member member = (Member) o;
      return index == member.index && length == member.length && offset == member.offset
          && storageSizeInBytes == member.storageSizeInBytes && isVariableLength == member.isVariableLength
          && Objects.equal(name, member.name) && Objects.equal(desc, member.desc) && Objects.equal(units, member.units)
          && dtype == member.dtype && Objects.equal(shape, member.shape) && Objects.equal(members, member.members)
          && Objects.equal(byteOrder, member.byteOrder);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, desc, units, dtype, index, length, shape, members, byteOrder, offset,
          storageSizeInBytes, isVariableLength);
    }
  }

  public static class MemberBuilder {
    private String name, desc, units;
    private DataType dataType;
    private int[] shape;
    private StructureMembers.Builder members; // only if member is type Structure
    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN; // needed by StructureDataArray
    private int offset = -1; // needed by StructureDataArray
    private boolean built;

    private MemberBuilder() {}

    public MemberBuilder setName(String name) {
      Preconditions.checkNotNull(name);
      this.name = name;
      return this;
    }

    public String getName() {
      return this.name;
    }

    public MemberBuilder setDesc(String desc) {
      this.desc = desc;
      return this;
    }

    public MemberBuilder setUnits(String units) {
      this.units = units;
      return this;
    }

    public MemberBuilder setDataType(DataType dtype) {
      Preconditions.checkNotNull(dtype);
      this.dataType = dtype;
      return this;
    }

    public MemberBuilder setStructureMembers(StructureMembers.Builder members) {
      this.members = members;
      return this;
    }

    public StructureMembers.Builder getStructureMembers() {
      return this.members;
    }

    public MemberBuilder setShape(int[] shape) {
      this.shape = Preconditions.checkNotNull(shape);
      return this;
    }

    public MemberBuilder setByteOrder(ByteOrder byteOrder) {
      this.byteOrder = byteOrder;
      return this;
    }

    /** The offset of the member in the ByteBuffer. If a StructureArray, then for the first record. */
    public MemberBuilder setOffset(int offset) {
      this.offset = offset;
      return this;
    }

    private int getStorageSizeBytes(boolean structuresOnHeap) {
      boolean isVariableLength = (shape != null && shape.length > 0 && shape[shape.length - 1] < 0);
      int length = (shape == null) ? 1 : (int) Arrays.computeSize(shape);

      if (isVariableLength) {
        return 4;
      } else if (dataType == DataType.SEQUENCE) {
        return 4;
      } else if (dataType == DataType.STRING) {
        return 4;
      } else if (dataType == DataType.STRUCTURE) {
        return structuresOnHeap ? 4 : length * members.getStorageSizeBytes(structuresOnHeap);
      } else {
        return length * dataType.getSize();
      }
    }

    @Override
    public String toString() {
      return name;
    }

    public Member build(int index, boolean structuresOnHeap) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new Member(this, index, structuresOnHeap);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  private final String name;
  private final int structureSize;
  private final boolean structuresOnHeap;
  private final ImmutableList<Member> members;

  private StructureMembers(Builder builder) {
    this.name = builder.name == null ? "" : builder.name;
    ImmutableList.Builder<Member> list = ImmutableList.builder();
    HashSet<String> nameSet = new HashSet<>();
    int count = 0;
    for (MemberBuilder mbuilder : builder.members) {
      if (nameSet.contains(mbuilder.name)) {
        throw new IllegalStateException("Duplicate member name in " + name);
      } else {
        nameSet.add(mbuilder.name);
      }
      list.add(mbuilder.build(count++, builder.structuresOnHeap));
    }
    this.members = list.build();
    this.structuresOnHeap = builder.structuresOnHeap;
    this.structureSize =
        builder.structureSize > 0 ? builder.structureSize : builder.getStorageSizeBytes(builder.structuresOnHeap);
  }

  /** Turn into a mutable Builder. Can use toBuilder().build(wantsData) to copy. */
  public Builder toBuilder() {
    Builder b =
        builder().setName(this.name).setStructureSize(this.structureSize).setStructuresOnHeap(this.structuresOnHeap);
    this.members.forEach(m -> b.addMember(m.toBuilder()));
    return b;
  }

  /** Create an StructureMembers builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Create an StructureMembers builder. */
  public static MemberBuilder memberBuilder() {
    return new MemberBuilder();
  }

  /** A builder for StructureMembers */
  public static class Builder {
    private String name;
    private final ArrayList<MemberBuilder> members = new ArrayList<>();
    private int structureSize = -1;
    private boolean structuresOnHeap = false;
    private boolean built;

    private Builder() {}

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder addMember(MemberBuilder m) {
      members.add(m);
      return this;
    }

    public Builder addMember(int pos, MemberBuilder m) {
      members.add(pos, m);
      return this;
    }

    public MemberBuilder addMember(String name, String desc, String units, DataType dtype, int[] shape) {
      Preconditions.checkNotNull(name);
      Preconditions.checkNotNull(dtype);
      MemberBuilder m =
          new MemberBuilder().setName(name).setDesc(desc).setUnits(units).setDataType(dtype).setShape(shape);
      addMember(m);
      return m;
    }

    public boolean hasMember(String memberName) {
      Preconditions.checkNotNull(memberName);
      for (MemberBuilder mb : members) {
        if (memberName.equals(mb.name)) {
          return true;
        }
      }
      return false;
    }

    public List<MemberBuilder> getStructureMembers() {
      return members;
    }

    /** Set structureSize and offsets yourself, or call setStandardOffsets. */
    public Builder setStructureSize(int structureSize) {
      this.structureSize = structureSize;
      return this;
    }

    Builder setStructuresOnHeap(boolean structuresOnHeap) {
      this.structuresOnHeap = structuresOnHeap;
      return this;
    }

    /** Set structureSize and offsets yourself, or call setStandardOffsets. */
    public void setStandardOffsets(boolean structuresOnHeap) {
      this.structuresOnHeap = structuresOnHeap;
      int offset = 0;
      for (MemberBuilder m : members) {
        m.setOffset(offset);
        if (m.dataType == DataType.SEQUENCE || m.dataType == DataType.STRUCTURE) {
          m.getStructureMembers().setStandardOffsets(structuresOnHeap);
        }
        offset += m.getStorageSizeBytes(structuresOnHeap);
      }
      setStructureSize(offset);
    }

    /** Get the total size of the Structure in bytes. */
    private int getStorageSizeBytes(boolean structuresOnHeap) {
      return members.stream().mapToInt(m -> m.getStorageSizeBytes(structuresOnHeap)).sum();
    }

    public StructureMembers build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new StructureMembers(this);
    }
  }

}
