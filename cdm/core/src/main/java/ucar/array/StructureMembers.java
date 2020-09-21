/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/** A Collection of members contained in StructureData. */
@Immutable
public final class StructureMembers {

  /** Get the StructureMembers' name. */
  public String getName() {
    return name;
  }

  /** Get the total size of the Structure in bytes. */
  public int getStructureSize() {
    return structureSize;
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
    return structureSize == that.structureSize && Objects.equal(name, that.name)
        && Objects.equal(members, that.members);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, members, structureSize);
  }

  /** A member of a StructureData. */
  @Immutable
  public final static class Member {
    private final String name, desc, units;
    private final DataType dtype;
    private final int index;
    private final int size;
    private final int[] shape;
    private final StructureMembers members; // only if member is type Structure
    private final boolean isVariableLength;

    private Member(MemberBuilder builder, int index) {
      this.name = Preconditions.checkNotNull(builder.name);
      this.desc = builder.desc;
      this.units = builder.units;
      this.dtype = Preconditions.checkNotNull(builder.dtype);
      this.index = index;
      this.shape = builder.shape != null ? builder.shape : new int[0];
      this.members = builder.members;

      this.size = (int) ucar.ma2.Index.computeSize(shape);
      this.isVariableLength = (shape.length > 0 && shape[shape.length - 1] < 0);
    }

    /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
    public MemberBuilder toBuilder() {
      MemberBuilder b = new MemberBuilder().setName(this.name).setDesc(this.desc).setUnits(this.units)
          .setDataType(this.dtype).setShape(this.shape);
      if (this.members != null) {
        b.setStructureMembers(this.members.toBuilder().build());
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
    public int getSize() {
      return size;
    }

    public boolean isVariableLength() {
      return isVariableLength;
    }

    /**
     * Get the total size in bytes.
     *
     * Note that this will not be correct when containing a member of type Sequence, or String, since those
     * are variable length. In that case
     *
     * @return total size in bytes
     */
    public int getSizeBytes() {
      if (getDataType() == DataType.SEQUENCE)
        return getDataType().getSize();
      else if (getDataType() == DataType.STRING)
        return getDataType().getSize();
      else if (getDataType() == DataType.STRUCTURE)
        return size * members.getStructureSize();
      // else if (this.isVariableLength())
      // return 0; // do not know
      else
        return size * getDataType().getSize();
    }

    /** Is this a scalar (size == 1). */
    public boolean isScalar() {
      return size == 1;
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
      return index == member.index && size == member.size && isVariableLength == member.isVariableLength
          && Objects.equal(name, member.name) && Objects.equal(desc, member.desc) && Objects.equal(units, member.units)
          && dtype == member.dtype && Objects.equal(shape, member.shape) && Objects.equal(members, member.members);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, desc, units, dtype, index, size, shape, members, isVariableLength);
    }
  }

  public static class MemberBuilder {
    private String name, desc, units;
    private DataType dtype;
    private int[] shape;
    private StructureMembers members; // only if member is type Structure
    private boolean built;

    private MemberBuilder() {}

    public MemberBuilder setName(String name) {
      Preconditions.checkNotNull(name);
      this.name = name;
      return this;
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
      this.dtype = dtype;
      return this;
    }

    public MemberBuilder setStructureMembers(StructureMembers members) {
      this.members = members;
      return this;
    }

    public MemberBuilder setShape(int[] shape) {
      this.shape = Preconditions.checkNotNull(shape);
      return this;
    }

    public Member build(int index) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new Member(this, index);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  private final String name;
  private final ImmutableList<Member> members;
  private final int structureSize;

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
      list.add(mbuilder.build(count++));
    }
    this.members = list.build();
    this.structureSize = builder.structureSize < 0 ? calcStructureSize() : builder.structureSize;
  }

  private int calcStructureSize() {
    int sum = 0;
    for (Member member : members) {
      sum += member.getSizeBytes();
    }
    return sum;
  }

  /** Turn into a mutable Builder. Can use toBuilder().build(wantsData) to copy. */
  public Builder toBuilder() {
    Builder b = builder().setName(this.name).setStructureSize(this.structureSize);
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

    public Builder setStructureSize(int structureSize) {
      this.structureSize = structureSize;
      return this;
    }

    public StructureMembers build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new StructureMembers(this);
    }
  }

}
