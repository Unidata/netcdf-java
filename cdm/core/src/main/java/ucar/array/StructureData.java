/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import java.util.List;

/** A container for a Structure's data. */
public abstract class StructureData {
  protected StructureMembers members;

  protected StructureData(StructureMembers members) {
    this.members = members;
  }

  /** @return name of Structure */
  public String getName() {
    return members.getName();
  }

  /** @return StructureMembers */
  public StructureMembers getStructureMembers() {
    return members;
  }

  /** @return List of StructureMembers.Member */
  public List<StructureMembers.Member> getMembers() {
    return members.getMembers();
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
    if (m == null)
      throw new IllegalArgumentException("illegal member name =" + memberName);
    return getMemberData(m);
  }

}
