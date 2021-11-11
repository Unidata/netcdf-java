/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.bufr;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import ucar.array.StructureDataArray;
import ucar.array.StructureDataStorageBB;
import ucar.array.StructureMembers;
import ucar.array.StructureMembers.Member;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.iosp.BitReader;
import ucar.unidata.io.RandomAccessFile;

/**
 * Read data for uncompressed messages.
 *
 * Within one message there are n obs (datasets) and s fields in each dataset.
 * For uncompressed datasets, storage order is data(obs, fld) (fld varying fastest) :
 * 
 * R11, R12, R13, . . . R1s
 * R21, R22, R23, . . . R2s
 * ....
 * Rn1, Rn2, Rn3, . . . Rns
 * 
 * where Rij is the value of the jth field of the ith dataset.
 * the datasets each occupy an identical number of bits, unless delayed replication is used,
 * and are not necessarily aligned on octet boundaries.
 * 
 * A replicated field (structure) takes a group of fields and replicates them:
 * 
 * Ri1, (Ri2, Ri3)*r, . . . Ris
 * 
 * where r is set in the data descriptor, and is the same for all datasets.
 * 
 * A delayed replicated field (sequence) takes a group of fields and replicates them, and adds the number of
 * replications
 * in the data :
 * 
 * Ri1, dri, (Ri2, Ri3)*dri, . . . Ris
 * 
 * where the width (nbits) of dr is set in the data descriptor. This dr can be different for each dataset in the
 * message.
 * It can be 0. When it has a bit width of 1, it indicates an optional set of fields.
 * 
 * --------------------------
 * 
 * We use an StructureDataStorageBB to hold the data, and fill it sequentially as we scan the message.
 * Fixed length nested Structures are kept in the ArrayStructureBB. LOOK
 * Variable length objects (Sequences) are added to the heap.
 */
class MessageArrayUncompressedReader {
  private static final boolean structuresOnHeap = false;
  private final Message message; // LOOK gets modified I think
  private final RandomAccessFile raf;
  private final Formatter f;

  // top level sequence
  private final int nelems;
  private final ByteBuffer bbuffer;
  private final StructureMembers members;
  private final StructureDataStorageBB storageBB;
  private final boolean addTime = false;

  // map dkey to Member recursively
  private final HashMap<DataDescriptor, Member> topmap = new HashMap<>(100);

  /**
   * Uncompressed message reader, results to ucar.array.Array
   *
   * @param s outer variables
   * @param proto prototype message, has been processed
   * @param m read this message
   * @param raf from this file
   * @param f output bit count debugging info (may be null)
   */
  MessageArrayUncompressedReader(Structure s, Message proto, Message m, RandomAccessFile raf, @Nullable Formatter f) {
    // transfer info from proto message LOOK is message modified here?
    DataDescriptor.transferInfo(proto.getRootDataDescriptor().getSubKeys(), m.getRootDataDescriptor().getSubKeys());
    this.message = m;
    this.raf = raf;
    this.f = f;

    // allocate ArrayStructureBB for outer structure
    StructureMembers.Builder membersb = s.makeStructureMembersBuilder().setStandardOffsets();
    this.members = membersb.build();

    this.nelems = m.getNumberDatasets();
    this.bbuffer = ByteBuffer.allocate(nelems * members.getStorageSizeBytes()).order(ByteOrder.BIG_ENDIAN);

    storageBB = new StructureDataStorageBB(members, this.bbuffer, this.nelems);

    // map dkey to Member recursively
    MessageArrayReaderUtils.associateMessage2Members(this.members, message.getRootDataDescriptor(), topmap);
  }

  /**
   * Read all datasets from a single message
   * 
   * @return StructureDataArray with all the data from the message in it.
   * @throws IOException on read error
   */
  StructureDataArray readEntireMessage() throws IOException {
    BitReader reader = new BitReader(raf, message.dataSection.getDataPos() + 4);
    DataDescriptor root = message.getRootDataDescriptor();
    if (!root.isBad) {
      Request req = new Request(this.storageBB, this.bbuffer, topmap);
      // LOOK why are we changing fields in the message. Could this be in req??
      // Or is this the way we send the info back?
      message.counterDatasets = new BitCounterUncompressed[this.nelems]; // one for each dataset
      message.msg_nbits = 0;// loop over the rows
      for (int row = 0; row < nelems; row++) {
        if (f != null) {
          f.format("Count bits in observation %d%n", row);
        }
        // the top table always has exactly one "row", since we are working with a single obs
        message.counterDatasets[row] = new BitCounterUncompressed(root, 1, 0);
        DebugOut out = (f == null) ? null : new DebugOut(f);
        req.setRow(row);

        if (addTime) {
          req.bb.putInt(0); // placeholder for time assumes an int
        }
        readData(reader, message.counterDatasets[row], root.subKeys, row, req, out);
        message.msg_nbits += message.counterDatasets[row].countBits(message.msg_nbits);
      }
    } else {
      throw new RuntimeException("Bad root descriptor");
    }
    return new ucar.array.StructureDataArray(members, new int[] {this.nelems}, storageBB);
  }

  // manage recursive reads
  private static class Request {
    final StructureDataStorageBB storageBB;
    final ByteBuffer bb;
    final HashMap<DataDescriptor, Member> memberMap;
    int row = 0;

    Request(StructureDataStorageBB storageBB, ByteBuffer bb, HashMap<DataDescriptor, Member> memberMap) {
      this.storageBB = storageBB;
      this.bb = bb;
      this.memberMap = memberMap;
    }

    Request setRow(int row) {
      this.row = row;
      return this;
    }
  }

  /**
   * Count/read the bits in one row of a "nested table", defined by List<DataDescriptor> dkeys.
   * Recursive.
   *
   * @param reader read data with this
   * @param dkeys the fields of the table
   * @param table put the results here
   * @param row which row of the table
   * @param req read data into here, cant be null
   * @param out optional debug output, may be null
   */
  private void readData(BitReader reader, BitCounterUncompressed table, List<DataDescriptor> dkeys, int row,
      Request req, @Nullable DebugOut out) throws IOException {

    for (DataDescriptor dkey : dkeys) {
      if (!dkey.isOkForVariable()) {// misc skip
        if (out != null)
          out.f.format("%s %d %s (%s) %n", out.indent(), out.fldno++, dkey.name, dkey.getFxyName());
        continue;
      }
      Member member = req.memberMap.get(dkey);

      // sequence
      if (dkey.replication == 0) {
        // find out how many objects in the sequence
        int count = (int) reader.bits2UInt(dkey.replicationCountSize);
        if (out != null)
          out.f.format("%4d delayed replication count=%d %n", out.fldno++, count);
        if ((out != null) && (count > 0)) {
          out.f.format("%4d %s read sequence %s count= %d bitSize=%d start at=0x%x %n", out.fldno, out.indent(),
              dkey.getFxyName(), count, dkey.replicationCountSize, reader.getPos());
        }

        // read the data
        BitCounterUncompressed bitCounterNested = table.makeNested(dkey, count, 0, dkey.replicationCountSize);
        StructureDataArray seq = makeNestedSequence(reader, bitCounterNested, dkey, out);

        int expected = member.getOffset() + row * req.storageBB.getStructureSize();
        int pos = req.bb.position();
        int index = req.storageBB.putOnHeap(seq);
        req.bb.putInt(index); // an index into the Heap
        continue;
      }

      // compound
      if (dkey.type == 3) {
        BitCounterUncompressed nested = table.makeNested(dkey, dkey.replication, 0, 0);
        if (out != null) {
          out.f.format("%4d %s read structure %s count= %d%n", out.fldno, out.indent(), dkey.getFxyName(),
              dkey.replication);
        }

        for (int nestedRow = 0; nestedRow < dkey.replication; nestedRow++) {
          if (out != null) {
            out.f.format("%s read row %d (struct %s) %n", out.indent(), nestedRow, dkey.getFxyName());
            out.indent.incr();
            readData(reader, nested, dkey.subKeys, nestedRow, req, out);
            out.indent.decr();
          } else {
            readData(reader, nested, dkey.subKeys, nestedRow, req, null);
          }
        }
        continue;
      }

      // char data
      if (dkey.type == 1) {
        byte[] vals = readCharData(dkey, reader, req);
        if (out != null) {
          String s = new String(vals, StandardCharsets.UTF_8);
          out.f.format("%4d %s read char %s (%s) width=%d end at= 0x%x val=<%s>%n", out.fldno++, out.indent(),
              dkey.getFxyName(), dkey.getName(), dkey.bitWidth, reader.getPos(), s);
        }
        continue;
      }

      // otherwise read a number
      long value = reader.bits2UInt(dkey.bitWidth);

      int expected = member.getOffset(); // + row * req.storageBB.getStructureSize();
      int pos = req.bb.position();
      // LOOK not setting position, relying on correct sequence of writing bytes....
      // req.bb.position(member.getOffset() + nestedRow * member.getStorageSizeBytes());
      MessageArrayReaderUtils.putNumericData(dkey, req.bb, value);

      if (out != null) {
        out.f.format(
            "%4d %s read %s (%s %s) bitWidth=%d end at= 0x%x raw=%d convert=%f row=%d position=%d expected=%d%n",
            out.fldno++, out.indent(), dkey.getFxyName(), dkey.getName(), dkey.getUnits(), dkey.bitWidth,
            reader.getPos(), value, dkey.convert(value), row, pos, expected);
      }
    }
  }

  private byte[] readCharData(DataDescriptor dkey, BitReader reader, Request req) throws IOException {
    int nchars = dkey.getByteWidthCDM();
    byte[] b = new byte[nchars];
    for (int i = 0; i < nchars; i++)
      b[i] = (byte) reader.bits2UInt(8);

    for (int i = 0; i < nchars; i++) {
      req.bb.put(b[i]);
    }
    return b;
  }

  private StructureDataArray makeNestedSequence(BitReader reader, BitCounterUncompressed bitCounterNested,
      DataDescriptor seqdd, DebugOut out) throws IOException {

    int nestedNrows = bitCounterNested.getNumberRows(); // the actual number of rows in this sequence
    Sequence seq = seqdd.refersTo;
    Preconditions.checkNotNull(seq);

    // Create nested StructureDataArray
    StructureMembers.Builder membersb = seq.makeStructureMembersBuilder().setStandardOffsets();
    StructureMembers nestedMembers = membersb.build();

    ByteBuffer nestedBB =
        ByteBuffer.allocate(nestedNrows * nestedMembers.getStorageSizeBytes()).order(ByteOrder.BIG_ENDIAN);
    StructureDataStorageBB nestedStorage = new StructureDataStorageBB(nestedMembers, nestedBB, nestedNrows);

    HashMap<DataDescriptor, Member> nestedMap = new HashMap<>();
    MessageArrayReaderUtils.associateMessage2Members(nestedMembers, seqdd, nestedMap);
    Request reqNested = new Request(nestedStorage, nestedBB, nestedMap);

    // loop through nested obs
    for (int i = 0; i < nestedNrows; i++) {
      if (out != null) {
        out.f.format("%s read row %d (seq %s) %n", out.indent(), i, seqdd.getFxyName());
        out.indent.incr();
        readData(reader, bitCounterNested, seqdd.getSubKeys(), i, reqNested, out);
        out.indent.decr();

      } else {
        readData(reader, bitCounterNested, seqdd.getSubKeys(), i, reqNested, null);
      }
    }

    int[] shape = {nestedNrows};
    return new ucar.array.StructureDataArray(nestedMembers, shape, nestedStorage);
  }
}
