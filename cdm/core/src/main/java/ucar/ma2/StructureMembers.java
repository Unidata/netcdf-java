/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import ucar.nc2.util.Indent;

/**
 * A Collection of members contained in a StructureData.
 * 
 * @deprecated use ucar.array.StructureMembers.
 */
@Deprecated
public final class StructureMembers {

  /** @deprecated use Builder */
  @Deprecated
  public StructureMembers(String name) {
    this.name = name;
    members = new ArrayList<>();
  }

  /** @deprecated use toBuilder().build(false) to make a copy with no data */
  @Deprecated
  public StructureMembers(StructureMembers from) {
    this.name = from.name;
    members = new ArrayList<>(from.getMembers().size());
    for (Member m : from.members) {
      Member nm = new Member(m); // make copy - without the data info
      addMember(nm);
      if (m.members != null) { // recurse
        nm.members = m.members.toBuilder(false).build();
      }
    }
  }

  /** Get the StructureMembers' name. */
  public String getName() {
    return name;
  }

  /**
   * Add a member.
   * 
   * @deprecated use Builder
   */
  @Deprecated
  public void addMember(Member m) {
    members.add(m);
  }

  /**
   * Add a member at the given position.
   * 
   * @deprecated use Builder
   */
  @Deprecated
  public void addMember(int pos, Member m) {
    members.add(pos, m);
  }

  /** @deprecated use Builder */
  @Deprecated
  public Member addMember(String name, String desc, String units, DataType dtype, int[] shape) {
    Member m = new Member(name, desc, units, dtype, shape);
    addMember(m);
    return m;
  }

  /**
   * Remove the given member
   * 
   * @param m member
   * @return position that it used to occupy, or -1 if not found
   * @deprecated use Builder
   */
  @Deprecated
  public int hideMember(Member m) {
    if (m == null)
      return -1;
    int index = members.indexOf(m);
    members.remove(m);
    return index;
  }

  /**
   * Get the total size of the Structure in bytes.
   *
   * @return the total size of the Structure in bytes.
   */
  public int getStructureSize() {
    if (structureSize < 0) {
      structureSize = calcStructureSize();
    }
    return structureSize;
  }

  private int calcStructureSize() {
    int sum = 0;
    for (Member member : members) {
      sum += member.getSizeBytes();
    }
    return sum;
  }

  /**
   * Set the total size of the Structure in bytes.
   * 
   * @deprecated use Builder
   */
  @Deprecated
  public void setStructureSize(int structureSize) {
    this.structureSize = structureSize;
  }

  /** Get the list of Member objects. */
  public ImmutableList<Member> getMembers() {
    return ImmutableList.copyOf(members);
  }

  /** Get the names of the members. */
  public ImmutableList<String> getMemberNames() {
    return members.stream().map(m -> m.getName()).collect(ImmutableList.toImmutableList());
  }

  /**
   * Get the index-th member
   *
   * @param index of member
   * @return Member
   */
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

  /** A member of a StructureData. */
  public final static class Member {
    // TODO make these final and immutable in 6.
    private String name, desc, units;
    private DataType dtype;
    private int size = 1;
    private int[] shape;
    private StructureMembers members; // only if member is type Structure
    private boolean isVariableLength;

    // optional, use depends on ArrayStructure subclass
    private Array dataArray;
    private Object dataObject;
    private int dataParam;

    private Member(MemberBuilder builder) {
      this.name = Preconditions.checkNotNull(builder.name);
      this.desc = builder.desc;
      this.units = builder.units;
      this.dtype = Preconditions.checkNotNull(builder.dtype);
      this.shape = builder.shape;
      this.members = builder.members;
      this.dataArray = builder.dataArray;
      this.dataObject = builder.dataObject;
      this.dataParam = builder.dataParam;

      this.size = (int) Index.computeSize(shape);
      this.isVariableLength = (shape.length > 0 && shape[shape.length - 1] < 0);
    }

    /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
    public MemberBuilder toBuilder(boolean wantsData) {
      MemberBuilder b = new MemberBuilder().setName(this.name).setDesc(this.desc).setUnits(this.units)
          .setDataType(this.dtype).setShape(this.shape);
      if (wantsData) {
        b.setDataArray(this.dataArray).setDataObject(this.dataObject).setDataParam(this.dataParam)
            .setStructureMembers(this.members);
      } else if (this.members != null) {
        b.setStructureMembers(this.members.toBuilder(wantsData).build());
      }
      return b;
    }

    /** @deprecated use MemberBuilder */
    @Deprecated
    public Member(String name, String desc, String units, DataType dtype, int[] shape) {
      this.name = Preconditions.checkNotNull(name);
      this.desc = desc;
      this.units = units;
      this.dtype = Preconditions.checkNotNull(dtype);
      setShape(shape);
    }

    /** @deprecated use MemberBuilder */
    @Deprecated
    public Member(Member from) {
      this.name = from.name;
      this.desc = from.desc;
      this.units = from.units;
      this.dtype = from.dtype;
      setStructureMembers(from.members);
      setShape(from.shape);
    }

    /**
     * If member is type Structure, you must set its constituent members.
     *
     * @param members set to this value
     * @throws IllegalArgumentException if {@code members} is this Member's enclosing class instance.
     * @deprecated use MemberBuilder
     */
    @Deprecated
    public void setStructureMembers(StructureMembers members) {
      this.members = members;
    }

    public StructureMembers getStructureMembers() {
      return members;
    }

    /** @deprecated use MemberBuilder */
    @Deprecated
    public void setShape(int[] shape) {
      this.shape = Preconditions.checkNotNull(shape);
      this.size = (int) Index.computeSize(shape);
      this.isVariableLength = (shape.length > 0 && shape[shape.length - 1] < 0);
    }

    /** Get the StructureMembers name. */
    public String getName() {
      return name;
    }

    public String getFullNameEscaped() {
      return name;
    }

    public String getFullName() {
      return name;
    }

    /**
     * Get the units string, if any.
     *
     * @return the units string, or null if none.
     */
    public String getUnitsString() {
      return units;
    }

    /**
     * Get the description, if any.
     *
     * @return the description, or null if none.
     */
    public String getDescription() {
      return desc;
    }

    /**
     * Get the DataType.
     *
     * @return the DataType.
     */
    public DataType getDataType() {
      return dtype;
    }

    /**
     * Get the array shape. This does not have to match the VariableSimpleIF.
     *
     * @return the array shape.
     */
    public int[] getShape() {
      return shape;
    }

    /**
     * Get the total number of elements.
     * This does not have to match the VariableSimpleIF.
     *
     * @return the total number of elements.
     */
    public int getSize() {
      return size;
    }

    public boolean isVariableLength() {
      return isVariableLength;
    }

    /**
     * Get the total size in bytes. This does not have to match the VariableSimpleIF.
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

    /**
     * Is this a scalar (size == 1).
     * This does not have to match the VariableSimpleIF.
     *
     * @return if this is a scalar
     */
    public boolean isScalar() {
      return size == 1;
    }

    ////////////////////////////////////////////////
    // these should not really be public

    /**
     * Get the data parameter value, for use behind the scenes.
     *
     * @return data parameter value
     */
    public int getDataParam() {
      return dataParam;
    }

    /**
     * Set the data parameter value, for use behind the scenes.
     *
     * @param dataParam set to this value
     * @deprecated use MemberBuilder
     */
    @Deprecated
    public void setDataParam(int dataParam) {
      this.dataParam = dataParam;
    }

    /**
     * Get the data array, if any. Used for implementation, DO NOT USE DIRECTLY!
     * 
     * @return data object, may be null
     */
    public Array getDataArray() {
      return dataArray;
    }

    /**
     * Set the data array. Used for implementation, DO NOT USE DIRECTLY!
     * 
     * @param data set to this Array. must not be a logical view
     * @deprecated use MemberBuilder
     */
    @Deprecated
    public void setDataArray(Array data) {
      this.dataArray = data;
    }

    /**
     * Get an opaque data object, for use behind the scenes. May be null
     * 
     * @return data object, may be null
     */
    public Object getDataObject() {
      return dataObject;
    }

    /**
     * Set an opaque data object, for use behind the scenes.
     * 
     * @param o set to this value
     * @deprecated use MemberBuilder
     */
    @Deprecated
    public void setDataObject(Object o) {
      this.dataObject = o;
    }

    /** @deprecated use MemberBuilder */
    @Deprecated
    public void setVariableInfo(String vname, String desc, String unitString, DataType dtype) {
      name = vname;

      if (dtype != null)
        this.dtype = dtype;
      if (unitString != null)
        this.units = unitString;
      if (desc != null)
        this.desc = desc;
    }

    void showInternal(Formatter f, Indent indent) {
      f.format("%sname='%s' desc='%s' units='%s' dtype=%s size=%d dataObject=%s dataParam=%d", indent, name, desc,
          units, dtype, size, dataObject, dataParam);
      if (members != null) {
        indent.incr();
        f.format("%n%sNested members %s%n", indent, members.getName());
        for (StructureMembers.Member m : members.getMembers())
          m.showInternal(f, indent);
        indent.decr();
      }
      f.format("%n");
    }

    public String toString() {
      return name;
    }
  }

  public static class MemberBuilder {
    private String name, desc, units;
    private DataType dtype;
    private int[] shape;
    private StructureMembers members; // only if member is type Structure

    // optional, use depends on ArrayStructure subclass
    private Array dataArray;
    private Object dataObject;
    private int dataParam;
    private boolean built;

    private MemberBuilder() {}

    public MemberBuilder setName(String name) {
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
      this.dtype = dtype;
      return this;
    }

    public MemberBuilder setDataArray(Array data) {
      this.dataArray = data;
      return this;
    }

    public MemberBuilder setDataObject(Object o) {
      this.dataObject = o;
      return this;
    }

    public MemberBuilder setDataParam(int dataParam) {
      this.dataParam = dataParam;
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

    public Member build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new Member(this);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  // TODO make these final and immutable in 6.
  private final String name;
  private final List<Member> members;
  private int structureSize = -1;

  private StructureMembers(Builder builder) {
    this.name = builder.name == null ? "" : builder.name;
    this.members = builder.members.stream().map(mb -> mb.build()).collect(Collectors.toList());
    this.structureSize = builder.structureSize < 0 ? calcStructureSize() : builder.structureSize;
  }

  /** Turn into a mutable Builder. Can use toBuilder().build(wantsData) to copy. */
  public Builder toBuilder(boolean wantsData) {
    Builder b = builder().setName(this.name).setStructureSize(this.structureSize);
    this.members.forEach(m -> b.addMember(m.toBuilder(wantsData)));
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
      MemberBuilder m =
          new MemberBuilder().setName(name).setDesc(desc).setUnits(units).setDataType(dtype).setShape(shape);
      addMember(m);
      return m;
    }

    /** Add a Member, creating a scalar DataArray from the val. Use with StructureDataFromMember. */
    public MemberBuilder addMemberScalar(String name, String desc, String units, DataType dtype, Number val) {
      MemberBuilder mb = addMember(name, desc, units, dtype, new int[0]);
      Array data = null;
      switch (dtype) {
        case UBYTE:
        case BYTE:
        case ENUM1:
          data = new ArrayByte.D0(dtype.isUnsigned());
          data.setByte(0, val.byteValue());
          break;
        case SHORT:
        case USHORT:
        case ENUM2:
          data = new ArrayShort.D0(dtype.isUnsigned());
          data.setShort(0, val.shortValue());
          break;
        case INT:
        case UINT:
        case ENUM4:
          data = new ArrayInt.D0(dtype.isUnsigned());
          data.setInt(0, val.intValue());
          break;
        case LONG:
        case ULONG:
          data = new ArrayLong.D0(dtype.isUnsigned());
          data.setDouble(0, val.longValue());
          break;
        case FLOAT:
          data = new ArrayFloat.D0();
          data.setFloat(0, val.floatValue());
          break;
        case DOUBLE:
          data = new ArrayDouble.D0();
          data.setDouble(0, val.doubleValue());
          break;
      }
      mb.setDataArray(data);
      return mb;
    }

    /** Add a Member, creating a CHAR DataArray from the String val. Use with StructureDataFromMember. */
    public MemberBuilder addMemberString(String name, String desc, String units, String val, int max_len) {
      MemberBuilder mb = addMember(name, desc, units, DataType.CHAR, new int[] {max_len});
      Array data = ArrayChar.makeFromString(val, max_len);
      mb.setDataArray(data);
      return mb;
    }

    public boolean hasMember(String memberName) {
      return members.stream().anyMatch(m -> m.name.equals(memberName));
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
