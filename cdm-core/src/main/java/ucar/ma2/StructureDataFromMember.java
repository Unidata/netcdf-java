/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * A StructureData implementation that has its data self-contained.
 * Created from the existing Data Array in each member.
 *
 * @author caron
 */
public class StructureDataFromMember extends StructureDataW {

  public StructureDataFromMember(StructureMembers sm) {
    super(sm);
    for (StructureMembers.Member member : sm.getMembers()) {
      setMemberData(member, member.getDataArray());
    }
  }

}

