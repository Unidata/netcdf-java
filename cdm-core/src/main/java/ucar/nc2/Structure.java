/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.array.StructureData;
import ucar.array.StructureMembers;
import ucar.nc2.util.Indent;

/**
 * A Structure is a type of Variable that contains other Variables, like a struct in C.
 * A Structure can be scalar or multidimensional.
 * <p>
 * A call to structure.read() will read all of the data in a Structure,
 * including nested structures, and returns an Array of StructureData, with all of the data in memory.
 * If there is a nested sequence, the sequence data may be read into memory all at once, or it may be
 * read in increments as the iteration proceeds.
 * <p>
 * Generally, the programmer can assume that the data in one Structure are stored together,
 * so that it is efficient to read an entire Structure, and then access the Variable data through the
 * Arrays in the StructureData.
 *
 * @author caron
 */
@Immutable
public class Structure extends Variable {

  /** Find the Variable member with the specified (short) name, or null if not found. */
  @Nullable
  public Variable findVariable(String shortName) {
    if (shortName == null)
      return null;
    return memberHash.get(shortName);
  }

  /** Get the number of variables contained directly in this Structure. */
  public int getNumberOfMemberVariables() {
    return members.size();
  }

  /** Get the size in bytes of one Structure. */
  @Override
  public int getElementSize() {
    return elementSize;
  }

  /** Get the variables contained directly in this Structure. */
  public ImmutableList<Variable> getVariables() {
    return members;
  }

  /** Get the (short) names of the variables contained directly in this Structure. */
  public ImmutableList<String> getVariableNames() {
    return members.stream().map(Variable::getShortName).collect(ImmutableList.toImmutableList());
  }

  /** Find if this was created from a subset() method. */
  public boolean isSubset() {
    return isSubset;
  }

  public StructureMembers.Builder makeStructureMembersBuilder() {
    StructureMembers.Builder builder = StructureMembers.builder().setName(this.getShortName());
    for (Variable v2 : this.getVariables()) {
      StructureMembers.MemberBuilder m = builder.addMember(v2.getShortName(), v2.getDescription(), v2.getUnitsString(),
          v2.getArrayType(), v2.getShape());
      if (v2 instanceof Structure) {
        Structure s2 = (Structure) v2;
        m.setStructureMembers(s2.makeStructureMembersBuilder());
      }
    }
    return builder;
  }

  /**
   * Create a subset of the Structure consisting only of the given member variables
   * 
   * @param memberNames list of Variable names, already a member
   * @return Structure containing just those members
   */
  public Structure select(List<String> memberNames) {
    Structure.Builder<?> result = this.toBuilder();
    // Will read the entire Structure, then transfer selected members to StructureData
    result.setElementSize(this.elementSize);

    List<Variable.Builder<?>> selected = new ArrayList<>();
    for (String name : memberNames) {
      result.findMemberVariable(name).ifPresent(selected::add);
    }
    result.vbuilders = selected;
    result.isSubset = true;
    return result.build(getParentGroup());
  }

  /**
   * Create a subset of the Structure consisting only of the one member variable
   * 
   * @param varName name of member Variable
   * @return containing just that member
   */
  public Structure select(String varName) {
    Preconditions.checkArgument(findVariable(varName) != null);
    return select(ImmutableList.of(varName));
  }

  /** Calculation of size of one element of this structure - equals the sum of sizes of its members. */
  private int calcElementSize() {
    return makeStructureMembersBuilder().getStorageSizeBytes();
  }

  /** Get String with name and attributes. Used in short descriptions like tooltips. */
  public String getNameAndAttributes() {
    Formatter sbuff = new Formatter();
    sbuff.format("Structure ");
    getNameAndDimensions(sbuff, false, true);
    sbuff.format("%n");
    for (Attribute att : attributes) {
      sbuff.format("  %s:%s;%n", getShortName(), att.toString());
    }
    return sbuff.toString();
  }

  @Override
  protected void writeCDL(Formatter buf, Indent indent, boolean useFullName, boolean strict) {
    buf.format("%n%s%s {%n", indent, dataType);

    indent.incr();
    for (Variable v : members)
      v.writeCDL(buf, indent, useFullName, strict);
    indent.decr();

    buf.format("%s} ", indent);
    getNameAndDimensions(buf, useFullName, strict);
    buf.format(";%s%n", extraInfo());

    for (Attribute att : attributes()) {
      buf.format("%s", indent);
      att.writeCDL(buf, strict, getShortName());
      buf.format(";");
      if (!strict && (att.getArrayType() != ArrayType.STRING))
        buf.format(" // %s", att.getArrayType());
      buf.format("%n");
    }
    buf.format("%n");
  }

  /**
   * Read one StructureData at index recno.
   * For rank 0 or 1 Structure, eg netcdf3 record variables.
   * 
   * @param recno start at this index
   */
  public StructureData readRecord(int recno) throws IOException, InvalidRangeException {
    Array<?> arr = readArray(Section.builder().appendRange(new Range(recno, recno)).build());
    return (StructureData) arr.get(0);
  }

  ////////////////////////////////////////////////////////
  protected final ImmutableList<Variable> members;
  private final HashMap<String, Variable> memberHash;
  protected final boolean isSubset;

  protected Structure(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    builder.vbuilders.forEach(v -> v.setParentStructure(this).setNcfile(builder.ncfile));
    this.members = builder.vbuilders.stream().map(vb -> vb.build(parentGroup)).collect(ImmutableList.toImmutableList());
    memberHash = new HashMap<>();
    this.members.forEach(m -> memberHash.put(m.getShortName(), m));
    if (builder.elementSize <= 0) {
      this.elementSize = calcElementSize();
    }
    this.isSubset = builder.isSubset;
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  @Override
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    this.members.forEach(m -> b.addMemberVariable(m.toBuilder()));
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /**
   * Get Builder for this class that allows subclassing.
   * 
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static Builder<?> builder() {
    return new Builder2().setArrayType(ArrayType.STRUCTURE);
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends Variable.Builder<T> {
    public List<Variable.Builder<?>> vbuilders = new ArrayList<>();
    private boolean isSubset;
    private boolean built;

    public T addMemberVariable(Variable.Builder<?> v) {
      vbuilders.add(v);
      v.setParentStructureBuilder(this);
      return self();
    }

    public T addMemberVariables(List<Variable.Builder<?>> vars) {
      vbuilders.addAll(vars);
      return self();
    }

    /** Add a Variable to the root group. */
    public T addMemberVariable(String shortName, ArrayType dataType, String dimString) {
      Variable.Builder<?> vb = Variable.builder().setName(shortName).setArrayType(dataType)
          .setParentGroupBuilder(this.parentBuilder).setDimensionsByName(dimString);
      addMemberVariable(vb);
      return self();
    }

    /** Remove memeber variable, if present. Return whether it was present */
    public boolean removeMemberVariable(String memberName) {
      Optional<Variable.Builder<?>> want = vbuilders.stream().filter(v -> v.shortName.equals(memberName)).findFirst();
      want.ifPresent(v -> vbuilders.remove(v));
      return want.isPresent();
    }

    /** Add a member variable, replacing one of same name if there is one. Return whether it was present */
    public boolean replaceMemberVariable(Variable.Builder<?> replacement) {
      boolean wasPresent = removeMemberVariable(replacement.shortName);
      addMemberVariable(replacement);
      return wasPresent;
    }

    public long calcElementSize() {
      int total = 0;
      for (Variable.Builder<?> v : vbuilders) {
        total += v.getElementSize() * v.getSize();
      }
      return total;
    }

    public Optional<Variable.Builder<?>> findMemberVariable(String name) {
      return vbuilders.stream().filter(d -> d.shortName.equals(name)).findFirst();
    }

    /** Normally this is only called by Group.build() */
    public Structure build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setArrayType(ArrayType.STRUCTURE);
      return new Structure(this, parentGroup);
    }
  }

}
