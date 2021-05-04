/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.*;
import ucar.array.StructureMembers.Member;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.StructureEnhanced;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;

/** Enhance StructureData, for both StructureDS and SequenceDS. TODO move to internal. */
public class StructureDataArrayEnhancer {
  private static final Logger logger = LoggerFactory.getLogger(StructureDataArrayEnhancer.class);
  private final StructureEnhanced topStructure;
  private final StructureDataArray org;
  private final StructureMembers members;
  private final ByteBuffer bbuffer;
  private final StructureDataStorageBB storage;

  public StructureDataArrayEnhancer(StructureEnhanced topStructure, StructureDataArray org) {
    this.topStructure = topStructure;
    this.org = org;
    this.members = ((Structure) topStructure).makeStructureMembersBuilder().setStandardOffsets(false).build();
    this.bbuffer = ByteBuffer.allocate((int) (org.length() * members.getStorageSizeBytes()));
    this.storage = new StructureDataStorageBB(members, bbuffer, (int) org.length());
  }

  public StructureDataArray enhance() {
    StructureMembers orgMembers = org.getStructureMembers();
    for (int row = 0; row < org.length(); row++) {
      StructureData orgData = org.get(row);
      for (StructureMembers.Member member : members) {
        StructureMembers.Member orgMember = orgMembers.findMember(member.getName());
        int pos = row * members.getStorageSizeBytes();
        Array<?> data = orgData.getMemberData(orgMember);
        convertNestedData(topStructure, pos, member, storage, bbuffer, data);
      }
    }
    return new StructureDataArray(members, org.getShape(), storage);
  }

  static void convertNestedData(StructureEnhanced structure, int offset, Member member, StructureDataStorageBB storage,
      ByteBuffer bbuffer, Array<?> orgMemberData) {
    VariableEnhanced ve = (VariableEnhanced) structure.findVariable(member.getName());
    if ((ve == null) && (structure.getOriginalVariable() != null)) { // these are from orgVar - may have been renamed
      ve = findVariableFromOrgName(structure, member.getName());
    }
    Preconditions.checkNotNull(ve);

    if (member.getArrayType() == ArrayType.OPAQUE) {
      int index = storage.putOnHeap(orgMemberData); // no conversion possible
      int pos = offset + member.getOffset();
      bbuffer.position(pos);
      bbuffer.putInt(index);

    } else if (member.getArrayType() == ArrayType.SEQUENCE) {
      Preconditions.checkArgument(ve instanceof StructureEnhanced);
      Preconditions.checkArgument(orgMemberData instanceof StructureDataArray);
      StructureDataArrayEnhancer nested =
          new StructureDataArrayEnhancer((StructureEnhanced) ve, (StructureDataArray) orgMemberData);
      StructureDataArray seqData = nested.enhance();
      int index = storage.putOnHeap(seqData);
      int pos = offset + member.getOffset();
      bbuffer.position(pos);
      bbuffer.putInt(index);

    } else if (member.isVlen()) {
      Preconditions.checkArgument(orgMemberData instanceof ArrayVlen);
      Preconditions.checkArgument(ve instanceof VariableDS);
      Array<?> converted = convertVlen((VariableDS) ve, (ArrayVlen<?>) orgMemberData);
      int index = storage.putOnHeap(converted);
      int pos = offset + member.getOffset();
      bbuffer.position(pos);
      bbuffer.putInt(index);

    } else if (member.getArrayType() == ArrayType.STRUCTURE) {
      StructureEnhanced nestedStucture = (StructureEnhanced) structure.findVariable(member.getName());
      Preconditions.checkNotNull(nestedStucture);
      Preconditions.checkArgument(member.getStructureMembers() != null);
      StructureMembers nestedMembers = member.getStructureMembers();
      StructureDataArray orgArray = (StructureDataArray) orgMemberData;
      StructureMembers orgMembers = orgArray.getStructureMembers();

      int length = (int) orgArray.length();
      for (int nrow = 0; nrow < length; nrow++) {
        StructureData orgSdata = orgArray.get(nrow);
        for (StructureMembers.Member nmember : nestedMembers) {
          StructureMembers.Member orgMember = orgMembers.findMember(nmember.getName());
          Preconditions.checkNotNull(orgMember, "Cant find " + orgMember.getName());
          int nestedPos = offset + nestedMembers.getStorageSizeBytes() * nrow + member.getOffset();
          convertNestedData(nestedStucture, nestedPos, nmember, storage, bbuffer, orgSdata.getMemberData(orgMember));
        }
      }
    } else {
      VariableDS vds = (VariableDS) ve;
      Array<?> converted = vds.convertNeeded() ? vds.convertArray(orgMemberData) : orgMemberData;
      storage.setMemberData(offset, member, converted);
    }

  }

  private static ArrayVlen<?> convertVlen(VariableDS vds, ArrayVlen<?> orgVlenData) {
    if (!vds.convertNeeded()) {
      return orgVlenData;
    }
    ArrayVlen<?> result = ArrayVlen.factory(vds.getArrayType(), orgVlenData.getShape());
    int index = 0;
    for (Array<?> data : orgVlenData) {
      result.set(index++, vds.convertArray(data));
    }
    return result;
  }

  // look for the top variable that has an orgVar with the wanted orgName
  private static VariableEnhanced findVariableFromOrgName(StructureEnhanced structureEn, String orgName) {
    for (Variable vTop : structureEn.getVariables()) {
      Variable v = vTop;
      while (v instanceof VariableEnhanced) {
        VariableEnhanced ve = (VariableEnhanced) v;
        if ((ve.getOriginalName() != null) && (ve.getOriginalName().equals(orgName))) {
          return (VariableEnhanced) vTop;
        }
        v = ve.getOriginalVariable();
      }
    }
    return null;
  }

}
