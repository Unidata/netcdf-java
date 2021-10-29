/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

/** A container for a Structure's data. */
public abstract class StructureData {
  protected StructureMembers members;

  protected StructureData(StructureMembers members) {
    this.members = members;
  }

  /** The name of Structure */
  public String getName() {
    return members.getName();
  }

  /** The StructureMembers */
  public StructureMembers getStructureMembers() {
    return members;
  }

  /**
   * Get member data array of any type as an Array.
   * 
   * @param m get data from this StructureMembers.Member.
   * @return Array values.
   */
  public abstract Array<?> getMemberData(StructureMembers.Member m);

  /**
   * Get member data array of any type as an Array.
   *
   * @param memberName name of member Variable.
   * @return member data array of any type as an Array.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public Array<?> getMemberData(String memberName) {
    StructureMembers.Member m = members.findMember(memberName);
    if (m == null) {
      throw new IllegalArgumentException("illegal member name =" + memberName);
    }
    return getMemberData(m);
  }

}
