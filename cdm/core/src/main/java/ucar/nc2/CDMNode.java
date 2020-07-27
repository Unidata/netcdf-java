/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.nc2.dataset.StructureDS;
import ucar.nc2.dataset.VariableDS;
import java.util.HashMap;
import java.util.Map;

/**
 * Define a superclass for all the CDM node classes: Group, Dimension, etc.
 * Define the sort of the node {@link CDMSort} so that we can
 * 1. do true switching on node type
 * 2. avoid use of instanceof
 * 3. Use container classes that have more than one kind of node
 * <p>
 * Also move various common fields and methods to here.
 *
 * @author Heimbigner
 * @deprecated Will move to Dap4 module in version 6.
 */
@Deprecated
public abstract class CDMNode {

  protected CDMSort sort;
  protected Group group;
  protected Structure parentstruct;
  protected boolean immutable;
  protected String shortName;

  protected Map<Object, Object> annotations;

  // String fullName = null; // uses backslash escaping

  // HACK: sadly, because of the existing class structure,
  // we need to track the original name of a
  // Variable/Dimension/Attribute object.
  // This is primarily used in DAP processing
  // because the names in the DAP DDS and/or DAS
  // may contain group information separated
  // by forward slash.

  protected String dodsname;

  //////////////////////////////////////////////////
  // Constructors

  protected CDMNode() {
    // Use Instanceof to figure out the sort
    if (this instanceof Attribute)
      setSort(CDMSort.ATTRIBUTE);
    else if (this instanceof EnumTypedef)
      setSort(CDMSort.ENUMERATION);
    else if (this instanceof Sequence)
      setSort(CDMSort.SEQUENCE);
    else if (this instanceof Structure)
      setSort(CDMSort.STRUCTURE);
    else if (this instanceof Group)
      setSort(CDMSort.GROUP);
    else if (this instanceof Variable) // Only case left is atomic var
      setSort(CDMSort.VARIABLE);
  }

  public CDMNode(String name) {
    this();
    setShortName(name);
  }

  /** @deprecated do not use */
  @Deprecated
  public CDMSort getSort() {
    return this.sort;
  }

  /** @deprecated do not use */
  @Deprecated
  public void setSort(CDMSort sort) {
    if (!immutable)
      this.sort = sort;
  }

  /**
   * Get the short name of this Variable. The name is unique within its parent group.
   */
  public String getShortName() {
    return this.shortName;
  }

  /**
   * Set the short name of this Variable. The name is unique within its parent group.
   *
   * @param name new short name
   * @deprecated Do not use.
   */
  @Deprecated
  public void setShortName(String name) {
    if (!immutable)
      this.shortName = NetcdfFiles.makeValidCdmObjectName(name);
  }

  /**
   * Get its parent Group, or null if its the root group.
   *
   * @return parent Group
   * @deprecated Do not use for Dimension or Attribute
   */
  @Deprecated
  public Group getParentGroup() {
    return this.group;
  }

  /**
   * Alias for getParentGroup
   *
   * @return parent Group
   * @deprecated Do not use.
   */
  @Deprecated
  public Group getGroup() {
    return getParentGroup();
  }

  /**
   * Set the parent Group
   *
   * @param parent The new parent group
   * @deprecated Do not use.
   */
  @Deprecated
  public void setParentGroup(Group parent) {
    if (!immutable)
      this.group = parent;
  }

  /**
   * Get its parent structure, or null if not in structure
   *
   * @return parent structure
   * @deprecated Do not use.
   */
  @Deprecated
  public Structure getParentStructure() {
    return this.parentstruct;
  }

  /**
   * Set the parent Structure
   *
   * @param parent The new parent structure
   * @deprecated Do not use.
   */
  @Deprecated
  public void setParentStructure(Structure parent) {
    if (!immutable)
      this.parentstruct = parent;
  }

  /**
   * Test for presence of parent Structure
   *
   * @return true iff struct != null
   * @deprecated Do not use.
   */
  @Deprecated
  public boolean isMemberOfStructure() {
    return this.parentstruct != null;
  }

  /**
   * Get immutable flag
   * As a rule, subclasses will access directly
   *
   * @return Immutable flag
   * @deprecated Do not use.
   */
  @Deprecated
  public boolean getImmutable() {
    return this.immutable;
  }

  /**
   * Set the immutable flag to true.
   * Once set, cannot be unset
   * 
   * @deprecated Do not use.
   */
  @Deprecated
  public CDMNode setImmutable() {
    this.immutable = true;
    return this;
  }


  /**
   * Get the dodsname
   *
   * @return the original names from the DDS or DAS; if null,
   *         then return the short name
   * @deprecated Do not use.
   */
  @Deprecated
  public String getDODSName() {
    if (dodsname == null)
      return this.shortName;
    else
      return this.dodsname;
  }

  /**
   * Store the original dods name
   *
   * @param name The original name from the DDS/DAS
   * @deprecated Do not use.
   */
  @Deprecated
  public void setDODSName(String name) {
    this.dodsname = name;
  }

  /**
   * Get the Full name of this object. Certain characters are
   * backslash escaped (see NetcdfFile)
   *
   * @return full name with backslash escapes
   * @deprecated use NetcdfFiles.makeFullName(*)
   */
  @Deprecated
  public String getFullName() {
    if (this instanceof Group) {
      return NetcdfFiles.makeFullName((Group) this);
    } else if (this instanceof Variable) {
      return NetcdfFiles.makeFullName((Variable) this);
    } else {
      return this.getShortName();
    }
  }

  /**
   * Alias for getFullName
   *
   * @return full name with backslash escapes
   * @deprecated use getFullName() when it exists in ver6.
   */
  public String getFullNameEscaped() {
    return getFullName();
  }


  /**
   * getName is deprecated because, as the code below shows,
   * it has no consistent meaning. Sometimes it returns
   * the short name, sometimes it returns the full name.
   *
   * @deprecated Replaced by {@link #getShortName()} and {@link #getFullName()}
   */
  @Deprecated
  public String getName() {
    switch (sort) {
      case ATTRIBUTE:
      case DIMENSION:
      case ENUMERATION:
        // for these cases, getName is getShortName
        return getShortName();

      case VARIABLE: // Atomic
      case SEQUENCE:
      case STRUCTURE:
      case GROUP:
        // for these cases, getName is getFullName
        return getFullName();
      default:
        break;
    }
    return getShortName(); // default
  }

  // Override the node's hashCode for subclasses of CDMNode.

  /** @deprecated do not use */
  public int localhash() {
    return super.hashCode();
  }

  /**
   * NetcdfDataset can end up wrapping a variable
   * in multiple wrapping classes (e.g. VariableDS).
   * Goal of this procedure is to get down to the
   * lowest level Variable instance
   *
   * @param node possibly wrapped ode
   * @return the lowest level node instance
   * @deprecated Do not use.
   */
  @Deprecated
  public static CDMNode unwrap(CDMNode node) {
    if (!(node instanceof Variable))
      return node;
    Variable inner = (Variable) node;
    for (;;) {
      if (inner instanceof VariableDS) {
        VariableDS vds = (VariableDS) inner;
        inner = vds.getOriginalVariable();
        if (inner == null) {
          inner = vds;
          break;
        }
      } else if (inner instanceof StructureDS) {
        StructureDS sds = (StructureDS) inner;
        inner = sds.getOriginalVariable();
        if (inner == null) {
          inner = sds;
          break;
        }
      } else
        break; // base case we have straight Variable or Stucture
    }
    return inner;
  }

  /** @deprecated do not use */
  @Deprecated
  public Object annotation(Object id) {
    return (this.annotations == null ? null : this.annotations.get(id));
  }

  /** @deprecated do not use */
  public Object annotate(Object id, Object value) {
    if (annotations == null)
      annotations = new HashMap<>();
    Object old = annotations.get(id);
    annotations.put(id, value);
    return old;
  }

}
