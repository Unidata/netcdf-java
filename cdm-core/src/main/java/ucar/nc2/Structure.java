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

import ucar.array.ArrayType;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayStructure;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
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
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Structure.class);
  private static final int defaultBufferSize = 500 * 1000; // 500K bytes


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

  /**
   * Create a StructureMembers object that describes this Structure.
   * CAUTION: Do not use for iterating over a StructureData or ArrayStructure - get the StructureMembers object
   * directly from the StructureData or ArrayStructure.
   *
   * @return a StructureMembers object that describes this Structure.
   * @deprecated use makeStructureMembersBuilder()
   */
  @Deprecated
  public StructureMembers makeStructureMembers() {
    StructureMembers.Builder builder = StructureMembers.builder().setName(getShortName());
    for (Variable v2 : this.getVariables()) {
      StructureMembers.MemberBuilder m = builder.addMember(v2.getShortName(), v2.getDescription(), v2.getUnitsString(),
          v2.getDataType(), v2.getShape());
      if (v2 instanceof Structure) {
        m.setStructureMembers(((Structure) v2).makeStructureMembers());
      }
    }
    return builder.build();
  }

  public ucar.array.StructureMembers.Builder makeStructureMembersBuilder() {
    ucar.array.StructureMembers.Builder builder = ucar.array.StructureMembers.builder().setName(this.getShortName());
    for (Variable v2 : this.getVariables()) {
      ucar.array.StructureMembers.MemberBuilder m = builder.addMember(v2.getShortName(), v2.getDescription(),
          v2.getUnitsString(), v2.getArrayType(), v2.getShape());
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

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  /** Calculation of size of one element of this structure - equals the sum of sizes of its members. */
  private int calcElementSize() {
    return makeStructureMembersBuilder().getStorageSizeBytes(false);
  }

  //////////////////////////////////////////////////////////////////////////////////////

  /**
   * Use this when this is a scalar or one dimensional array of Structures, or you are doing the index calculation
   * yourself for a multidimensional array. This will read only the ith structure, and return a StructureData.
   * 
   * @param index linear index into the array
   * @return ith StructureData
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if index out of range
   * @deprecated use readArray(Section)
   */
  @Deprecated
  public StructureData readStructure(int index) throws IOException, ucar.ma2.InvalidRangeException {
    Section.Builder sb = Section.builder();

    if (getRank() == 1) {
      sb.appendRange(index, index);

    } else if (getRank() > 1) {
      Index ii = Index.factory(shape); // convert to nD index
      ii.setCurrentCounter(index);
      int[] origin = ii.getCurrentCounter();
      for (int anOrigin : origin) {
        sb.appendRange(anOrigin, anOrigin);
      }
    }

    Array dataArray = read(sb.build());
    if (dataArray instanceof ArrayStructure) {
      ArrayStructure data = (ArrayStructure) dataArray;
      return data.getStructureData(0);
    } else if (dataArray instanceof ArrayObject) {
      return (StructureData) dataArray.getObject(0);
    }
    throw new RuntimeException("Unknown structure array class " + dataArray.getClass().getName());
  }

  /**
   * For rank 0 or 1 array of Structures, read count Structures and return the data as an ArrayStructure.
   * Use only when this is a one dimensional array of Structures.
   * 
   * @param start start at this index
   * @param count return this many StructureData
   * @return the StructureData recordsfrom start to start+count-1
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if start, count out of range
   * @deprecated use readArray(Section)
   */
  @Deprecated
  public ArrayStructure readStructure(int start, int count) throws IOException, ucar.ma2.InvalidRangeException {
    Preconditions.checkArgument(getRank() <= 1, "not a vector structure");
    int[] origin = {start};
    int[] shape = {count};
    return (ArrayStructure) read(origin, shape);
  }

  /**
   * Iterator over all the data in a Structure.
   * 
   * <pre>
   * StructureDataIterator ii = structVariable.getStructureIterator();
   * while (ii.hasNext()) {
   *   StructureData sdata = ii.next();
   * }
   * </pre>
   * 
   * @return StructureDataIterator over type StructureData
   * @throws java.io.IOException on read error
   * @see #getStructureIterator(int bufferSize)
   * @deprecated use readArray().iterator()
   */
  @Deprecated
  public StructureDataIterator getStructureIterator() throws java.io.IOException {
    return getStructureIterator(defaultBufferSize);
  }

  /**
   * Get an efficient iterator over all the data in the Structure.
   *
   * This is the efficient way to get all the data, it can be much faster than reading one record at a time,
   * and is optimized for large datasets.
   * This is accomplished by buffering bufferSize amount of data at once.
   *
   * <pre>
   * Example:
   *
   *  StructureDataIterator ii = structVariable.getStructureIterator(100 * 1000);
   *  while (ii.hasNext()) {
   *    StructureData sdata = ii.next();
   *  }
   * </pre>
   * 
   * @param bufferSize size in bytes to buffer, set < 0 to use default size
   * @return StructureDataIterator over type StructureData
   * @throws java.io.IOException on read error
   * @deprecated use readArray().iterator()
   */
  @Deprecated
  public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    return (getRank() < 2) ? new Structure.IteratorRank1(bufferSize) : new IteratorRankAny();
  }

  /** Iterator over type StructureData, rank 1 (common) case */
  private class IteratorRank1 implements StructureDataIterator {
    private int count;
    private final int recnum = (int) getSize();
    private int readStart;
    private int readCount;
    private int readAtaTime;
    private ArrayStructure as;

    IteratorRank1(int bufferSize) {
      setBufferSize(bufferSize);
    }

    @Override
    public boolean hasNext() {
      return count < recnum;
    }

    @Override
    public StructureDataIterator reset() {
      count = 0;
      readStart = 0;
      readCount = 0;
      return this;
    }

    @Override
    public StructureData next() throws IOException {
      if (count >= readStart) {
        readNext();
      }

      count++;
      return as.getStructureData(readCount++);
    }

    @Override
    public int getCurrentRecno() {
      return count - 1;
    }

    private void readNext() throws IOException {
      int left = Math.min(recnum, readStart + readAtaTime); // dont go over recnum
      int need = left - readStart; // how many to read this time
      try {
        as = readStructure(readStart, need);
        if (NetcdfFile.debugStructureIterator)
          System.out.println("readNext " + count + " " + readStart);

      } catch (InvalidRangeException e) {
        log.error("Structure.IteratorRank1.readNext() ", e);
        throw new IllegalStateException("Structure.Iterator.readNext() ", e);
      } // cant happen
      readStart += need;
      readCount = 0;
    }

    @Override
    public void setBufferSize(int bytes) {
      if (count > 0)
        return; // too late
      int structureSize = getElementSize();
      if (structureSize <= 0)
        structureSize = 1; // no members in the psuedo-structure LOOK is this ok?
      if (bytes <= 0)
        bytes = defaultBufferSize;
      readAtaTime = Math.max(10, bytes / structureSize);
      if (NetcdfFile.debugStructureIterator)
        System.out.println("Iterator structureSize= " + structureSize + " readAtaTime= " + readAtaTime);
    }

  }

  /** Iterator over type StructureData, general case */
  private class IteratorRankAny implements StructureDataIterator {
    private int count; // done so far
    private int total = (int) getSize();
    private int readStart; // current buffer starts at
    private int readCount; // count within the current buffer [0,readAtaTime)
    private int outerCount; // over the outer Dimension
    private ArrayStructure as;

    @Override
    public boolean hasNext() {
      return count < total;
    }

    @Override
    public StructureDataIterator reset() {
      count = 0;
      total = (int) getSize();
      readStart = 0;
      readCount = 0;
      outerCount = 0;
      return this;
    }

    @Override
    public int getCurrentRecno() {
      return count - 1;
    }

    @Override
    public StructureData next() throws IOException {
      if (count >= readStart) {
        readNextGeneralRank();
      }

      count++;
      return as.getStructureData(readCount++);
    }

    private void readNextGeneralRank() throws IOException {

      try {
        Section.Builder sb = Section.builder().appendRanges(shape);
        sb.setRange(0, new Range(outerCount, outerCount));

        as = (ArrayStructure) read(sb.build());

        if (NetcdfFile.debugStructureIterator) {
          System.out.println("readNext inner=" + outerCount + " total=" + outerCount);
        }

        outerCount++;

      } catch (InvalidRangeException e) {
        log.error("Structure.Iterator.readNext() ", e);
        throw new IllegalStateException("Structure.Iterator.readNext() ", e);
      } // cant happen

      readStart += as.getSize();
      readCount = 0;
    }

  }

  ////////////////////////////////////////////

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

    /** @deprecated use addMemberVariable(String, ArrayType, String) */
    @Deprecated
    public T addMemberVariable(String shortName, DataType dataType, String dimString) {
      Variable.Builder<?> vb = Variable.builder().setName(shortName).setDataType(dataType)
          .setParentGroupBuilder(this.parentBuilder).setDimensionsByName(dimString);
      addMemberVariable(vb);
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
