/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset;

import static ucar.nc2.internal.dataset.StructureDataArrayEnhancer.convertNestedData;

import java.nio.ByteBuffer;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.StructureData;
import ucar.array.StructureDataStorageBB;
import ucar.array.StructureMembers;
import ucar.nc2.dataset.SequenceDS;

/**
 * Enhance StructureData, for both StructureDS and SequenceDS.
 *
 * @deprecated use StructureDataArrayEnhancer
 */
@Deprecated
public class SequenceArrayEnhancer implements Iterator<StructureData> {
  private static final Logger logger = LoggerFactory.getLogger(SequenceArrayEnhancer.class);
  private final SequenceDS topStructure;
  private final Iterator<ucar.array.StructureData> orgIterator;
  private final StructureMembers members;

  public SequenceArrayEnhancer(SequenceDS topStructure, Iterator<StructureData> orgIterator) {
    this.topStructure = topStructure;
    this.orgIterator = orgIterator;
    this.members = topStructure.makeStructureMembersBuilder().setStandardOffsets(false).build();
  }

  @Override
  public boolean hasNext() {
    return this.orgIterator.hasNext();
  }

  @Override
  public StructureData next() {
    StructureData sdata = this.orgIterator.next();
    return enhance(sdata);
  }

  private StructureData enhance(StructureData orgData) {
    StructureMembers orgMembers = orgData.getStructureMembers();
    ByteBuffer bbuffer = ByteBuffer.allocate(members.getStorageSizeBytes());
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bbuffer, 1);

    for (StructureMembers.Member member : members) {
      StructureMembers.Member orgMember = orgMembers.findMember(member.getName());
      Array<?> data = orgData.getMemberData(orgMember);
      convertNestedData(topStructure, 0, member, storage, bbuffer, data);
    }
    return storage.get(0);
  }

}
