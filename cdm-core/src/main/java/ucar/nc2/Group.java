/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static ucar.nc2.NetcdfFiles.reservedFullName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.array.ArrayType;
import ucar.nc2.internal.util.EscapeStrings;
import ucar.nc2.util.Indent;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * A logical collection of Variables, Attributes, and Dimensions.
 * The Groups in a Dataset form a hierarchical tree, like directories on a disk.
 * A Group has a name and optionally a set of Attributes.
 * There is always at least one Group in a dataset, the root Group, whose name is the empty string.
 */
@Immutable
public class Group {

  /** The attributes contained by this Group. */
  public AttributeContainer attributes() {
    return attributes;
  }

  /**
   * Get the common parent of this and the other group.
   * Cant fail, since the root group is always a parent of any 2 groups.
   *
   * @param other the other group
   * @return common parent of this and the other group
   */
  public Group commonParent(Group other) {
    if (isParent(other)) {
      return this;
    }
    if (other.isParent(this)) {
      return other;
    }
    while (other != null && !other.isParent(this)) {
      other = other.getParentGroup();
    }
    return other;
  }

  /** Find the attribute by name, or null if not exist */
  @Nullable
  public Attribute findAttribute(String name) {
    return attributes.findAttribute(name);
  }

  /**
   * Find a String-valued Attribute by name (ignore case), return the String value of the Attribute.
   *
   * @return the attribute value, or defaultValue if not found
   */
  public String findAttributeString(String attName, String defaultValue) {
    return attributes.findAttributeString(attName, defaultValue);
  }

  /** Find a Dimension in this or a parent Group, matching on short name */
  public Optional<Dimension> findDimension(String name) {
    if (name == null)
      return Optional.empty();
    Dimension d = findDimensionLocal(name);
    if (d != null)
      return Optional.of(d);
    Group parent = getParentGroup();
    if (parent != null)
      return parent.findDimension(name);

    return Optional.empty();
  }

  /** Find a Dimension in this or a parent Group, using equals, or null if not found */
  @Nullable
  public Dimension findDimension(Dimension dim) {
    if (dim == null) {
      return null;
    }
    for (Dimension d : dimensions) {
      if (d.equals(dim)) {
        return d;
      }
    }
    Group parent = getParentGroup();
    if (parent != null) {
      return parent.findDimension(dim);
    }
    return null;
  }

  /** Find a Dimension using its (short) name, in this group only, or null if not found */
  @Nullable
  public Dimension findDimensionLocal(String shortName) {
    if (shortName == null)
      return null;
    for (Dimension d : dimensions) {
      if (shortName.equals(d.getShortName()))
        return d;
    }

    return null;
  }

  /** Find a Enumeration in this or a parent Group, using its short name. */
  @Nullable
  public EnumTypedef findEnumeration(String name) {
    if (name == null) {
      return null;
    }
    for (EnumTypedef d : enumTypedefs) {
      if (name.equals(d.getShortName())) {
        return d;
      }
    }
    Group parent = getParentGroup();
    if (parent != null) {
      return parent.findEnumeration(name);
    }

    return null;
  }

  /**
   * Retrieve the local Group with the specified (short) name. Must be contained in this Group.
   *
   * @param groupShortName short name of the local group you are looking for.
   * @return the Group, or null if not found
   */
  @Nullable
  public Group findGroupLocal(String groupShortName) {
    if (groupShortName == null) {
      return null;
    }

    for (Group group : groups) {
      if (groupShortName.equals(group.getShortName())) {
        return group;
      }
    }

    return null;
  }

  /**
   * Retrieve the nested Group with the specified (short) name. May be any level of nesting.
   *
   * @param groupShortName short name of the nested group you are looking for.
   */
  public Optional<Group> findGroupNested(String groupShortName) {
    if (groupShortName == null) {
      return Optional.empty();
    }

    Group local = this.findGroupLocal(groupShortName);
    if (local != null) {
      return Optional.of(local);
    }

    for (Group nested : groups) {
      Optional<Group> result = nested.findGroupNested(groupShortName);
      if (result.isPresent()) {
        return result;
      }
    }

    return Optional.empty();
  }

  /**
   * Look in this Group and in its nested Groups for a Variable with a String valued Attribute with the given name
   * and value.
   *
   * @param attName look for an Attribuite with this name.
   * @param attValue look for an Attribuite with this value.
   * @return the first Variable that matches, or null if none match.
   */
  @Nullable
  public Variable findVariableByAttribute(String attName, String attValue) {
    Preconditions.checkNotNull(attName);
    for (Variable v : getVariables()) {
      for (Attribute att : v.attributes())
        if (attName.equals(att.getShortName()) && attValue.equals(att.getStringValue()))
          return v;
    }
    for (Group nested : getGroups()) {
      Variable v = nested.findVariableByAttribute(attName, attValue);
      if (v != null)
        return v;
    }
    return null;
  }

  /** Find the Variable with the specified (short) name in this group, or null if not found */
  @Nullable
  public Variable findVariableLocal(String varShortName) {
    if (varShortName == null)
      return null;
    for (Variable v : variables) {
      if (varShortName.equals(v.getShortName()))
        return v;
    }
    return null;
  }

  /** Find the Variable with the specified (short) name in this group or a parent group, or null if not found */
  @Nullable
  public Variable findVariableOrInParent(String varShortName) {
    if (varShortName == null)
      return null;

    Variable v = findVariableLocal(varShortName);
    Group parent = getParentGroup();
    if ((v == null) && (parent != null))
      v = parent.findVariableOrInParent(varShortName);
    return v;
  }

  /** Get the shared Dimensions contained directly in this group. */
  public ImmutableList<Dimension> getDimensions() {
    return dimensions;
  }

  /** Get the enumerations contained directly in this group. */
  public ImmutableList<EnumTypedef> getEnumTypedefs() {
    return ImmutableList.copyOf(enumTypedefs);
  }

  /**
   * Get the full name of this Group.
   * Certain characters are backslash escaped (see NetcdfFiles.makeFullName(Group))
   *
   * @return full name with backslash escapes
   */
  public String getFullName() {
    return NetcdfFiles.makeFullName(this);
  }

  /** Get the Groups contained directly in this Group. */
  public ImmutableList<Group> getGroups() {
    return ImmutableList.copyOf(groups);
  }

  /** Get the NetcdfFile that owns this Group. */
  public NetcdfFile getNetcdfFile() {
    return ncfile;
  }

  /** Get the parent Group, or null if its the root group. */
  @Nullable
  public Group getParentGroup() {
    return this.parentGroup;
  }

  /** Get the short name of the Group. */
  public String getShortName() {
    return shortName;
  }

  /** Get the Variables contained directly in this group. */
  public ImmutableList<Variable> getVariables() {
    return variables;
  }

  /**
   * Is this a parent of the other Group?
   *
   * @param other another Group
   * @return true is it is equal or a parent
   */
  public boolean isParent(Group other) {
    while ((other != this) && (other.getParentGroup() != null))
      other = other.getParentGroup();
    return (other == this);
  }

  /** Is this the root group? */
  public boolean isRoot() {
    return getParentGroup() == null;
  }

  /**
   * Create a dimension list using dimension names. The dimension is searched for recursively in the parent groups.
   *
   * @param dimString : whitespace separated list of dimension names, or '*' for Dimension.UNKNOWN, or number for anon
   *        dimension. null or empty String is a scalar.
   * @return list of dimensions, will return ImmutableList<> in version 6
   * @throws IllegalArgumentException if cant find dimension or parse error.
   */
  public ImmutableList<Dimension> makeDimensionsList(String dimString) throws IllegalArgumentException {
    return Dimensions.makeDimensionsList(this::findDimension, dimString);
  }

  //////////////////////////////////////////////////////////////////////////////////////


  /**
   * Get String with name and attributes. Used in short descriptions like tooltips.
   *
   * @return name and attributes String.
   */
  public String getNameAndAttributes() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("Group ");
    sbuff.append(getShortName());
    sbuff.append("\n");
    for (Attribute att : attributes) {
      sbuff.append("  ").append(getShortName()).append(":");
      sbuff.append(att);
      sbuff.append(";");
      sbuff.append("\n");
    }
    return sbuff.toString();
  }

  void writeCDL(Formatter out, Indent indent, boolean strict) {
    boolean hasE = (!enumTypedefs.isEmpty());
    boolean hasD = (!dimensions.isEmpty());
    boolean hasV = (!variables.isEmpty());
    // boolean hasG = (groups.size() > 0);
    boolean hasA = (!Iterables.isEmpty(attributes));

    if (hasE) {
      out.format("%stypes:%n", indent);
      indent.incr();
      for (EnumTypedef e : enumTypedefs) {
        e.writeCDL(out, indent, strict);
        out.format("%n");
      }
      indent.decr();
      out.format("%n");
    }

    if (hasD) {
      out.format("%sdimensions:%n", indent);
      indent.incr();
      for (Dimension myd : dimensions) {
        myd.writeCDL(out, indent, strict);
        out.format("%n");
      }
      indent.decr();
    }

    if (hasV) {
      out.format("%svariables:%n", indent);
      indent.incr();
      for (Variable v : variables) {
        v.writeCDL(out, indent, false, strict);
        out.format("%n");
      }
      indent.decr();
    }

    for (Group g : groups) {
      String gname = strict ? NetcdfFiles.makeValidCDLName(g.getShortName()) : g.getShortName();
      out.format("%sgroup: %s {%n", indent, gname);
      indent.incr();
      g.writeCDL(out, indent, strict);
      indent.decr();
      out.format("%s}%n%n", indent);
    }

    // if (hasA && (hasE || hasD || hasV || hasG))
    // out.format("%n");

    if (hasA) {
      if (isRoot())
        out.format("%s// global attributes:%n", indent);
      else
        out.format("%s// group attributes:%n", indent);

      for (Attribute att : attributes) {
        // String name = strict ? NetcdfFile.escapeNameCDL(getShortName()) : getShortName();
        out.format("%s", indent);
        att.writeCDL(out, strict, null);
        out.format(";");
        if (!strict && (att.getArrayType() != ArrayType.STRING))
          out.format(" // %s", att.getArrayType().toCdl());
        out.format("%n");
      }
    }
  }

  @Override
  public String toString() {
    Formatter buf = new Formatter();
    writeCDL(buf, new Indent(2), false);
    return buf.toString();
  }

  // LOOK using just the name seems wrong.
  @Override
  public boolean equals(Object oo) {
    if (this == oo)
      return true;
    if (!(oo instanceof Group))
      return false;
    Group og = (Group) oo;
    if (!getShortName().equals(og.getShortName()))
      return false;
    return !((getParentGroup() != null) && !getParentGroup().equals(og.getParentGroup()));
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 37 * result + getShortName().hashCode();
    if (getParentGroup() != null)
      result = 37 * result + getParentGroup().hashCode();
    return result;
  }

  ////////////////////////////////////////////////////////////////
  private final NetcdfFile ncfile;
  private final ImmutableList<Variable> variables;
  private final ImmutableList<Dimension> dimensions;
  private final ImmutableList<Group> groups;
  private final AttributeContainer attributes;
  private final ImmutableList<EnumTypedef> enumTypedefs;
  private final String shortName;
  private final Group parentGroup;

  private Group(Builder builder, @Nullable Group parent) {
    this.shortName = builder.shortName;
    this.parentGroup = parent;
    this.ncfile = builder.ncfile;

    this.dimensions = ImmutableList.copyOf(builder.dimensions);
    this.enumTypedefs = ImmutableList.copyOf(builder.enumTypedefs);

    // only the root group build() should be called, the rest get called recursively
    this.groups = builder.gbuilders.stream().map(g -> g.setNcfile(this.ncfile).build(this))
        .collect(ImmutableList.toImmutableList());

    builder.vbuilders.forEach(vb -> {
      // dont override ncfile if its been set.
      if (vb.ncfile == null) {
        vb.setNcfile(this.ncfile);
      }
    });
    ImmutableList.Builder<Variable> vlistb = ImmutableList.builder();
    for (Variable.Builder<?> vb : builder.vbuilders) {
      Variable var = vb.build(this);
      vlistb.add(var);
    }
    this.variables = vlistb.build();

    this.attributes = builder.attributes.toImmutable();
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder toBuilder() {
    Builder builder = builder().setName(this.shortName).setNcfile(this.ncfile).addAttributes(this.attributes)
        .addDimensions(this.dimensions).addEnumTypedefs(this.enumTypedefs);

    this.groups.forEach(g -> builder.addGroup(g.toBuilder()));
    this.variables.forEach(v -> builder.addVariable(v.toBuilder()));

    return builder;
  }

  public static Builder builder() {
    return new Builder();
  }

  // TODO implement Tree interface for common methods between Group and Group.Builder
  public static class Builder {
    private @Nullable Group.Builder parentGroup; // null for root group; ignored during build()
    public List<Group.Builder> gbuilders = new ArrayList<>();
    public List<Variable.Builder<?>> vbuilders = new ArrayList<>();
    public String shortName = "";
    private NetcdfFile ncfile; // set by NetcdfFile.build()
    private final AttributeContainerMutable attributes = new AttributeContainerMutable("");
    private final List<Dimension> dimensions = new ArrayList<>();
    public final List<EnumTypedef> enumTypedefs = new ArrayList<>();
    private boolean built;

    public Builder setParentGroup(@Nullable Group.Builder parentGroup) {
      this.parentGroup = parentGroup;
      return this;
    }

    public @Nullable Group.Builder getParentGroup() {
      return this.parentGroup;
    }

    public Builder addAttribute(Attribute att) {
      Preconditions.checkNotNull(att);
      attributes.addAttribute(att);
      return this;
    }

    public Builder addAttributes(Iterable<Attribute> atts) {
      Preconditions.checkNotNull(atts);
      attributes.addAll(atts);
      return this;
    }

    public AttributeContainerMutable getAttributeContainer() {
      return attributes;
    }

    /** Add Dimension with error if it already exists */
    public Builder addDimension(Dimension dim) {
      Preconditions.checkNotNull(dim);
      findDimensionLocal(dim.getShortName()).ifPresent(d -> {
        throw new IllegalArgumentException("Dimension '" + d.getShortName() + "' already exists");
      });
      dimensions.add(dim);
      return this;
    }

    /**
     * Add Dimension if one with same name doesnt already exist.
     * 
     * @return true if it did not exist and was added.
     */
    public boolean addDimensionIfNotExists(Dimension dim) {
      Preconditions.checkNotNull(dim);
      if (!findDimensionLocal(dim.getShortName()).isPresent()) {
        dimensions.add(dim);
        return true;
      }
      return false;
    }

    /** Add Dimensions with error if any already exist */
    public Builder addDimensions(Collection<Dimension> dims) {
      Preconditions.checkNotNull(dims);
      dims.forEach(this::addDimension);
      return this;
    }

    /**
     * Replace dimension if it exists, else just add it.
     *
     * @return true if there was an existing dimension of that name
     */
    public boolean replaceDimension(Dimension dim) {
      Optional<Dimension> want = findDimensionLocal(dim.getShortName());
      want.ifPresent(dimensions::remove);
      addDimension(dim);
      return want.isPresent();
    }

    /**
     * Remove dimension, if it exists.
     *
     * @return true if there was an existing dimension of that name
     */
    public boolean removeDimension(String name) {
      Optional<Dimension> want = findDimensionLocal(name);
      want.ifPresent(dimensions::remove);
      return want.isPresent();
    }

    /** Find Dimension local to this Group */
    public Optional<Dimension> findDimensionLocal(String name) {
      return dimensions.stream().filter(d -> d.getShortName().equals(name)).findFirst();
    }

    /** Find Dimension in this Group or a parent Group */
    public boolean contains(Dimension want) {
      Dimension have = dimensions.stream().filter(d -> d.equals(want)).findFirst().orElse(null);
      if (have != null) {
        return true;
      }
      if (this.parentGroup != null) {
        return this.parentGroup.contains(want);
      }
      return false;
    }

    /** Find Dimension in this Group or a parent Group */
    public Optional<Dimension> findDimension(String name) {
      if (name == null) {
        return Optional.empty();
      }
      Optional<Dimension> dopt = findDimensionLocal(name);
      if (dopt.isPresent()) {
        return dopt;
      }
      if (this.parentGroup != null)
        return this.parentGroup.findDimension(name);

      return Optional.empty();
    }

    // Unmodifiable iterator
    public Iterable<Dimension> getDimensions() {
      return ImmutableList.copyOf(dimensions);
    }

    /** Add a nested Group. */
    public Builder addGroup(Group.Builder nested) {
      Preconditions.checkNotNull(nested);
      this.findGroupLocal(nested.shortName).ifPresent(g -> {
        throw new IllegalStateException("Nested group already exists " + nested.shortName);
      });
      this.gbuilders.add(nested);
      nested.setParentGroup(this);
      return this;
    }

    public Builder addGroups(Collection<Group.Builder> groups) {
      Preconditions.checkNotNull(groups);
      this.gbuilders.addAll(groups);
      return this;
    }

    /**
     * Remove group, if it exists.
     *
     * @return true if there was an existing group of that name
     */
    public boolean removeGroup(String name) {
      Optional<Group.Builder> want = findGroupLocal(name);
      want.ifPresent(v -> gbuilders.remove(v));
      return want.isPresent();
    }

    public Optional<Group.Builder> findGroupLocal(String shortName) {
      return this.gbuilders.stream().filter(g -> g.shortName.equals(shortName)).findFirst();
    }

    /**
     * Find a subgroup of this Group, with the specified reletive name.
     * An embedded "/" separates group names.
     * Can have a leading "/" only if this is the root group.
     *
     * @param reletiveName eg "group/subgroup/wantGroup".
     * @return Group or empty if not found.
     */
    public Optional<Group.Builder> findGroupNested(String reletiveName) {
      if (reletiveName == null || reletiveName.isEmpty()) {
        return (this.getParentGroup() == null) ? Optional.of(this) : Optional.empty();
      }

      Group.Builder g = this;
      StringTokenizer stoke = new StringTokenizer(reletiveName, "/");
      while (stoke.hasMoreTokens()) {
        String groupName = NetcdfFiles.makeNameUnescaped(stoke.nextToken());
        Optional<Group.Builder> sub = g.findGroupLocal(groupName);
        if (!sub.isPresent()) {
          return Optional.empty();
        }
        g = sub.get();
      }
      return Optional.of(g);
    }

    /** Is this group a parent of the other group ? */
    public boolean isParent(Group.Builder other) {
      while ((other != this) && (other.parentGroup != null))
        other = other.parentGroup;
      return (other == this);
    }

    /** Find the common parent with the other group ? */
    public Group.Builder commonParent(Group.Builder other) {
      Preconditions.checkNotNull(other);
      if (isParent(other))
        return this;
      if (other.isParent(this))
        return other;
      while (!other.isParent(this))
        other = other.parentGroup;
      return other;
    }

    public Builder addEnumTypedef(EnumTypedef typedef) {
      Preconditions.checkNotNull(typedef);
      enumTypedefs.add(typedef);
      return this;
    }

    public Builder addEnumTypedefs(Collection<EnumTypedef> typedefs) {
      Preconditions.checkNotNull(typedefs);
      enumTypedefs.addAll(typedefs);
      return this;
    }

    /**
     * Add a EnumTypedef if it does not already exist.
     * Return new or existing.
     */
    public EnumTypedef findOrAddEnumTypedef(String name, Map<Integer, String> map) {
      Optional<EnumTypedef> opt = findEnumeration(name);
      if (opt.isPresent()) {
        return opt.get();
      } else {
        EnumTypedef enumTypedef = new EnumTypedef(name, map);
        addEnumTypedef(enumTypedef);
        return enumTypedef;
      }
    }

    public Optional<EnumTypedef> findEnumeration(String name) {
      return this.enumTypedefs.stream().filter(e -> e.getShortName().equals(name)).findFirst();
    }

    /** Add a Variable, throw error if one of the same name if it exists. */
    public Builder addVariable(Variable.Builder<?> variable) {
      Preconditions.checkNotNull(variable);
      findVariableLocal(variable.shortName).ifPresent(v -> {
        throw new IllegalArgumentException("Variable '" + v.shortName + "' already exists");
      });
      vbuilders.add(variable);
      variable.setParentGroupBuilder(this);
      return this;
    }

    /** Add Variables, throw error if one of the same name if it exists. */
    public Builder addVariables(Collection<Variable.Builder<?>> vars) {
      vars.forEach(this::addVariable);
      return this;
    }

    /**
     * Replace variable of same name, if it exists, else just add it.
     * 
     * @return true if there was an existing variable of that name
     */
    public boolean replaceVariable(Variable.Builder<?> vb) {
      Optional<Variable.Builder<?>> want = findVariableLocal(vb.shortName);
      want.ifPresent(v -> vbuilders.remove(v));
      addVariable(vb);
      return want.isPresent();
    }

    /**
     * Remove variable, if it exists.
     *
     * @return true if there was an existing variable of that name
     */
    public boolean removeVariable(String name) {
      Optional<Variable.Builder<?>> want = findVariableLocal(name);
      want.ifPresent(v -> vbuilders.remove(v));
      return want.isPresent();
    }

    public Optional<Variable.Builder<?>> findVariableLocal(String name) {
      return vbuilders.stream().filter(v -> v.shortName.equals(name)).findFirst();
    }

    /**
     * Find a Variable, with the specified reletive name. No structure members.
     * 
     * @param reletiveName eg "group/subgroup/varname".
     */
    public Optional<Variable.Builder<?>> findVariableNested(String reletiveName) {
      if (reletiveName == null || reletiveName.isEmpty()) {
        return Optional.empty();
      }

      // break into groupNames and varName
      Group.Builder group = this;
      String varName = reletiveName;
      int pos = reletiveName.lastIndexOf('/');
      if (pos >= 0) {
        String groupNames = reletiveName.substring(0, pos);
        varName = reletiveName.substring(pos + 1);
        group = findGroupNested(groupNames).orElse(null);
      }

      return group == null ? Optional.empty() : group.findVariableLocal(varName);
    }

    /**
     * Find a Variable, with the specified (escaped full) name.
     * It may possibly be nested in multiple groups and/or structures.
     * An embedded "." is interpreted as structure.member.
     * An embedded "/" is interpreted as group/variable.
     * If the name actually has a ".", you must escape it (call NetcdfFiles.makeValidPathName(varname))
     * Any other chars may also be escaped, as they are removed before testing.
     *
     * @param fullNameEscaped eg "/group/subgroup/name1.name2.name".
     * @return Optional<Variable.Builder>
     *         {@link NetcdfFile#findVariable(String fullNameEscaped)}
     */
    public Optional<Variable.Builder<?>> findVariable(String fullNameEscaped) {
      if (fullNameEscaped == null || fullNameEscaped.isEmpty()) {
        return Optional.empty();
      }

      Group.Builder group = this;
      String vars = fullNameEscaped;

      // break into group/group and var.var
      int pos = fullNameEscaped.lastIndexOf('/');
      if (pos >= 0) {
        String groupNames = fullNameEscaped.substring(0, pos);
        vars = fullNameEscaped.substring(pos + 1);
        group = findGroupNested(groupNames).orElse(null);
      }
      if (group == null) {
        return Optional.empty();
      }

      // heres var.var - tokenize respecting the possible escaped '.'
      List<String> snames = EscapeStrings.tokenizeEscapedName(vars);
      if (snames.isEmpty()) {
        return Optional.empty();
      }

      String varShortName = NetcdfFiles.makeNameUnescaped(snames.get(0));
      Variable.Builder<?> v = group.findVariableLocal(varShortName).orElse(null);
      if (v == null) {
        return Optional.empty();
      }

      int memberCount = 1;
      while (memberCount < snames.size()) {
        if (!(v instanceof Structure.Builder<?>)) {
          return Optional.empty();
        }
        Structure.Builder<?> sb = (Structure.Builder<?>) v;
        String name = NetcdfFiles.makeNameUnescaped(snames.get(memberCount));
        v = sb.findMemberVariable(name).orElse(null);
        if (v == null) {
          return Optional.empty();
        }
        memberCount++;
      }

      return Optional.of(v);
    }

    /**
     * Find the Variable with the specified (short) name in this group or a parent group.
     *
     * @param varShortName short name of Variable.
     * @return the Variable or empty.
     */
    public Optional<Variable.Builder<?>> findVariableOrInParent(String varShortName) {
      if (varShortName == null)
        return Optional.empty();

      Optional<Variable.Builder<?>> vopt = findVariableLocal(varShortName);

      Group.Builder parent = getParentGroup();
      if (!vopt.isPresent() && (parent != null)) {
        vopt = parent.findVariableOrInParent(varShortName);
      }
      return vopt;
    }

    // Generally ncfile is set in NetcdfFile.build()
    public Builder setNcfile(NetcdfFile ncfile) {
      this.ncfile = ncfile;
      return this;
    }

    public Builder setName(String shortName) {
      this.shortName = NetcdfFiles.makeValidCdmObjectName(shortName);
      return this;
    }

    @Deprecated
    public NetcdfFile getNcfile() {
      return this.ncfile;
    }

    /** Make list of dimensions by looking in this Group or parent groups */
    public ImmutableList<Dimension> makeDimensionsList(String dimString) throws IllegalArgumentException {
      return Dimensions.makeDimensionsList(this::findDimension, dimString);
    }

    /**
     * Make the full name of the this group.
     * TODO In light of CF groups, we may have to start full names with '/'
     */
    public String makeFullName() {
      if (parentGroup == null) {
        return "";
      }
      StringBuilder sbuff = new StringBuilder();
      appendGroupName(sbuff, this);
      return sbuff.toString();
    }

    private void appendGroupName(StringBuilder sbuff, Group.Builder g) {
      if (g == null || g.getParentGroup() == null) {
        return;
      }
      appendGroupName(sbuff, g.getParentGroup());
      sbuff.append(EscapeStrings.backslashEscape(g.shortName, reservedFullName));
      sbuff.append("/");
    }

    /** Remove the given dimension from this group and any subgroups */
    public void removeDimensionFromAllGroups(Group.Builder group, Dimension remove) {
      group.dimensions.removeIf(dim -> dim.equals(remove));
      group.gbuilders.forEach(g -> removeDimensionFromAllGroups(g, remove));
    }

    /** Build the root group, with parent = null. */
    public Group build() {
      return build(null);
    }

    /** Normally this is called by NetcdfFile.build() */
    Group build(@Nullable Group parent) {
      if (built)
        throw new IllegalStateException("Group was already built " + this.shortName);
      built = true;
      return new Group(this, parent);
    }
  }
}
