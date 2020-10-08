/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.bufr;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import ucar.array.Arrays;
import ucar.array.StructureDataArray;
import ucar.array.StructureDataStorageBB;
import ucar.array.StructureMembers;
import ucar.array.StructureMembers.Member;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.iosp.BitReader;
import ucar.unidata.io.RandomAccessFile;

/**
 * Read data for uncompressed messages.
 * Can count bits / transfer all or some data to an Array.
 *
 * Within one message there are n obs (datasets) and s fields in each dataset.
 * For compressed datasets, storage order is data(fld, obs) (obs varying fastest) :
 * 
 * Ro1, NBINC1, I11, I12, . . . I1n
 * Ro2, NBINC2, I21, I22, . . . I2n
 * ...
 * Ros, NBINCs, Is1, Is2, . . . Isn
 * 
 * where Ro1, Ro2, . . . Ros are local reference values (number of bits as Table B) for field i.
 * NBINC1 . . . NBINCs contain, as 6-bit quantities, the number of bits occupied by the increments that follow.
 * If NBINC1 = 0, all values of element I are equal to Ro1; in such cases, the increments shall be omitted.
 * For character data, NBINC shall contain the number of octets occupied by the character element.
 * However, if the character data in all subsets are identical NBINC=0.
 * Iij is the increment for the ith field and the jth obs.
 * 
 * A replicated field (structure) takes a group of fields and replicates them.
 * Let C be the entire compressed block for the ith field, as above.
 * 
 * Ci = Roi, NBINCi, Ii1, Ii2, . . . Iin
 * 
 * data:
 * 
 * C1, (C2, C3)*r, ... Cs
 * 
 * where r is set in the data descriptor, and is the same for all datasets.
 * 
 * A delayed replicated field (sequence) takes a group of fields and replicates them, with the number of replications
 * in the data :
 * 
 * C1, dr, 6bits, (C2, C3)*dr, ... Cs
 * 
 * where the width (nbits) of dr is set in the data descriptor. This dr must be the same for each dataset in the
 * message.
 *
 * --------------------------
 * LOOK For some reason there is an extra 6 bits after the dr. My guess its a programming mistake that is now needed.
 * There is no description of this case in the spec or the guide.
 *
 * For Sequences, inner.length is the same for all datasets in the message. However, it may vary across messages.
 * However, we only iterate over the inner sequence, never across all messages. So the implementation can be specific to
 * the
 * message.
 */
public class MessageArrayCompressedReader {
  private static final boolean structuresOnHeap = false;
  private final Message message; // LOOK gets modified I think
  private final RandomAccessFile raf;
  private final Formatter f;
  private final HashMap<DataDescriptor, Member> topmap = new HashMap<>(100); // map dkey to Member recursively

  // top level sequence
  private final int ndatasets;
  private final Request topreq;

  /**
   * Read all datasets from a single message
   * 
   * @param s outer variables
   * @param proto prototype message, has been processed
   * @param message read this message
   * @param raf from this file
   * @param f output bit count debugging info (may be null)
   */
  public MessageArrayCompressedReader(Structure s, Message proto, Message message, RandomAccessFile raf, Formatter f) {
    this.message = message;
    this.raf = raf;
    this.f = f;

    // transfer info (refersTo, name) from the proto message
    DataDescriptor.transferInfo(proto.getRootDataDescriptor().getSubKeys(),
        message.getRootDataDescriptor().getSubKeys());

    // allocate ArrayStructureBB for outer structure
    StructureMembers.Builder membersb = StructureMembers.makeStructureMembers(s);
    membersb.setStandardOffsets(structuresOnHeap); // LOOK ??
    StructureMembers members = membersb.build();

    this.ndatasets = message.getNumberDatasets();
    ByteBuffer bbuffer = ByteBuffer.allocate(this.ndatasets * members.getStorageSizeBytes());
    bbuffer.order(ByteOrder.BIG_ENDIAN);

    StructureDataStorageBB storageBB = new StructureDataStorageBB(members, bbuffer, this.ndatasets);

    // map dkey to Member recursively
    MessageArrayReaderUtils.associateMessage2Members(members, message.getRootDataDescriptor(), topmap);
    this.topreq = new Request(storageBB, bbuffer, members, members.getStorageSizeBytes(), topmap);
  }

  // manage the request
  private static class Request {
    final StructureDataStorageBB storageBB;
    final ByteBuffer bb;
    final StructureMembers members;
    final int datasetSize;
    final HashMap<DataDescriptor, Member> memberMap;

    @Nullable
    DpiTracker dpiTracker; // may be null
    int dpiRow; // dont understand this, but used for dpi

    Request(StructureDataStorageBB storageBB, ByteBuffer bb, StructureMembers members, int datasetSize,
        HashMap<DataDescriptor, Member> memberMap) {
      this.storageBB = storageBB;
      this.bb = bb;
      this.members = members;
      this.datasetSize = datasetSize; // number of bytes one dataset is.
      this.memberMap = memberMap;
    }

    int getDatasetSize() {
      return datasetSize;
    }
  }

  // read / count the bits in a compressed message
  public StructureDataArray readEntireMessage() throws IOException {
    BitReader reader = new BitReader(raf, message.dataSection.getDataPos() + 4);
    DataDescriptor root = message.getRootDataDescriptor();
    if (!root.isBad) {
      DebugOut out = (f == null) ? null : new DebugOut(f);
      // one for each field LOOK why not m.counterFlds ?
      BitCounterCompressed[] counterFlds = new BitCounterCompressed[root.subKeys.size()];
      readData(reader, root, 0, 0, this.topreq, counterFlds, out);

      message.msg_nbits = 0;
      for (BitCounterCompressed counter : counterFlds) {
        if (counter != null) {
          message.msg_nbits += counter.getTotalBits();
        }
      }
    } else {
      throw new RuntimeException("Bad root descriptor");
    }

    return new ucar.array.StructureDataArray(this.topreq.members, new int[] {this.ndatasets}, this.topreq.storageBB);
  }

  /**
   *
   * @param reader raf wrapper for bit reading
   * @param parent parent.subkeys() holds the fields
   * @param bitOffset bit offset from beginning of message's data
   * @param posOffset bytebuffer position offset, for nested structs
   * @param req for writing into the StructureDataArray;
   * @param fldCounters bitCounters, one for each field
   * @param out debug info; may be null
   */
  private int readData(BitReader reader, DataDescriptor parent, int bitOffset, int posOffset, Request req,
      BitCounterCompressed[] fldCounters, @Nullable DebugOut out) throws IOException {

    List<DataDescriptor> flds = parent.getSubKeys();
    for (int fldidx = 0; fldidx < flds.size(); fldidx++) {
      DataDescriptor dkey = flds.get(fldidx);
      if (!dkey.isOkForVariable()) { // dds with no data to read
        // the dpi nightmare
        if ((dkey.f == 2) && (dkey.x == 36)) {
          req.dpiTracker = new DpiTracker(dkey.dpi, dkey.dpi.getNfields());
        }
        if (out != null) {
          out.f.format("%s %d %s (%s) %n", out.indent(), out.fldno++, dkey.name, dkey.getFxyName());
        }
        continue;
      }
      Member member = req.memberMap.get(dkey);
      if (member == null && dkey.subKeys != null) {
        member = req.memberMap.get(dkey.subKeys.get(0)); // use first field of the
      }
      Preconditions.checkNotNull(member);

      BitCounterCompressed counter = new BitCounterCompressed(dkey, this.ndatasets, bitOffset);
      fldCounters[fldidx] = counter;

      // replicated with unknown length (sequence)
      if (dkey.replication == 0) {
        reader.setBitOffset(bitOffset);
        int nestedNrows = (int) reader.bits2UInt(dkey.replicationCountSize);
        bitOffset += dkey.replicationCountSize;

        reader.bits2UInt(6);
        if (null != out) {
          out.f.format("%s--sequence %s bitOffset=%d replication=%s %n", out.indent(), dkey.getFxyName(), bitOffset,
              nestedNrows);
        }
        bitOffset += 6; // LOOK seems to be an extra 6 bits.

        counter.addNestedCounters(nestedNrows);

        // make an ArrayObject of ArraySequence, place it into the data array
        bitOffset = makeNestedSequence(reader, member, dkey, bitOffset, nestedNrows, req, counter, out);
        continue;
      }

      // replicated with known length (structure or non-scalar field)
      if (dkey.type == 3) {
        if (null != out) {
          out.f.format("%s--structure %s bitOffset=%d replication=%s %n", out.indent(), dkey.getFxyName(), bitOffset,
              dkey.replication);
        }

        // p 11 of "standard", doesnt describe the case of compression with nested replication.
        counter.addNestedCounters(dkey.replication);
        for (int nrow = 0; nrow < dkey.replication; nrow++) {
          BitCounterCompressed[] bitCounters = counter.getNestedCounters(nrow);
          req.dpiRow = nrow;
          int nestedOffset;
          if (member.getStructureMembers() == null) {
            // this is the case where its a singleton replica, so not a compound, just a non-scalar field.
            nestedOffset = posOffset + nrow * member.getDataType().getSize();
          } else {
            nestedOffset = posOffset + nrow * member.getStructureSize() + member.getOffset();
          }
          if (null != out) {
            out.f.format("%n");
            out.indent.incr();
            bitOffset = readData(reader, dkey, bitOffset, nestedOffset, req, bitCounters, out);
            out.indent.decr();
          } else {
            bitOffset = readData(reader, dkey, bitOffset, nestedOffset, req, bitCounters, null);
          }
        }
        continue;
      }

      //// all other fields

      // IndexIterator iter = (IndexIterator) member.getDataObject();
      // LOOK setDataArray never called
      // StructureDataArray dataDpi = (iter == null) ? (StructureDataArray) member.getDataArray() : null;

      reader.setBitOffset(bitOffset); // ?? needed ??

      // char data special case
      if (dkey.type == 1) {
        int nc = dkey.bitWidth / 8;
        byte[] minValue = new byte[nc];
        for (int i = 0; i < nc; i++) {
          minValue[i] = (byte) reader.bits2UInt(8);
        }
        int dataWidth = (int) reader.bits2UInt(6); // incremental data width in bytes
        counter.setDataWidth(8 * dataWidth);
        int totalWidth = dkey.bitWidth + 6 + 8 * dataWidth * this.ndatasets; // total width in bits for this compressed
                                                                             // set
        // of values
        bitOffset += totalWidth; // bitOffset now points to the next field

        if (null != out) {
          out.f.format("%s read %d %s (%s) bitWidth=%d defValue=%s dataWidth=%d n=%d bitOffset=%d %n", out.indent(),
              out.fldno++, dkey.name, dkey.getFxyName(), dkey.bitWidth, new String(minValue, StandardCharsets.UTF_8),
              dataWidth, this.ndatasets, bitOffset);
        }

        for (int dataset = 0; dataset < this.ndatasets; dataset++) {
          int pos = posOffset + req.getDatasetSize() * dataset + member.getOffset();
          req.bb.position(pos);

          if (dataWidth == 0) { // use the min value
            req.bb.put(minValue);

          } else { // read the incremental value
            int nt = Math.min(nc, dataWidth);
            byte[] incValue = new byte[nc];
            for (int i = 0; i < nt; i++) {
              incValue[i] = (byte) reader.bits2UInt(8);
            }
            for (int i = nt; i < nc; i++) { // can dataWidth < n ?
              incValue[i] = 0;
            }

            for (int i = 0; i < nc; i++) {
              int cval = incValue[i];
              if (incValue[i] < 32 || incValue[i] > 126)
                cval = 0; // printable ascii KLUDGE!
              incValue[i] = (byte) cval;
            }
            req.bb.put(incValue);
            if (out != null) {
              out.f.format(" %s,", new String(incValue, StandardCharsets.UTF_8));
            }
          }
        }
        if (out != null) {
          out.f.format("%n");
        }
        continue;
      }

      // numeric fields
      int useBitWidth = dkey.bitWidth;

      // a dpi Field needs to be substituted
      boolean isDpi = ((dkey.f == 0) && (dkey.x == 31) && (dkey.y == 31));
      boolean isDpiField = false;
      if ((dkey.f == 2) && (dkey.x == 24) && (dkey.y == 255)) {
        isDpiField = true;
        DataDescriptor dpiDD = req.dpiTracker.getDpiDD(req.dpiRow);
        useBitWidth = dpiDD.bitWidth;
      }

      long dataMin = reader.bits2UInt(useBitWidth);
      int dataWidth = (int) reader.bits2UInt(6); // increment data width - always in 6 bits, so max is 2^6 = 64
      if (dataWidth > useBitWidth && (null != out)) {
        out.f.format(" BAD WIDTH ");
      }
      if (dkey.type == 1) {
        dataWidth *= 8; // char data count is in bytes
      }
      counter.setDataWidth(dataWidth);

      int totalWidth = useBitWidth + 6 + dataWidth * this.ndatasets; // total width in bits for this compressed set of
                                                                     // values
      bitOffset += totalWidth; // bitOffset now points to the next field

      if (null != out) {
        out.f.format("%s read %d, %s (%s) bitWidth=%d dataMin=%d (%f) dataWidth=%d n=%d bitOffset=%d %n", out.indent(),
            out.fldno++, dkey.name, dkey.getFxyName(), useBitWidth, dataMin, dkey.convert(dataMin), dataWidth,
            this.ndatasets, bitOffset);
      }

      //// numeric fields

      // if dataWidth == 0, just use min value, otherwise read the compressed value here
      for (int dataset = 0; dataset < this.ndatasets; dataset++) {
        long value = dataMin; // using an "unsigned long" presumably as widest needed?

        if (dataWidth > 0) {
          long cv = reader.bits2UInt(dataWidth);
          if (BufrNumbers.isMissing(cv, dataWidth)) {
            value = BufrNumbers.missingValue(useBitWidth); // set to missing value
          } else { // add to minimum
            value += cv;
          }
        }

        // workaround for malformed messages
        if (dataWidth > useBitWidth) {
          long missingVal = BufrNumbers.missingValue(useBitWidth);
          if ((value & missingVal) != value) { // overflow
            value = missingVal; // replace with missing value
          }
        }

        int pos = posOffset + req.getDatasetSize() * dataset + member.getOffset();
        req.bb.position(pos);
        MessageArrayReaderUtils.putNumericData(dkey, req.bb, value);

        /*
         * WAS:
         * if (isDpiField) {
         * if (dataDpi != null) {
         * DataDescriptor dpiDD = req.dpiTracker.getDpiDD(req.outerRow);
         * StructureMembers sms = dataDpi.getStructureMembers();
         * Member m0 = sms.getMember(0);
         * IndexIterator iter2 = (IndexIterator) m0.getDataObject();
         * iter2.setObjectNext(dpiDD.getName());
         * 
         * Member m1 = sms.getMember(1);
         * iter2 = (IndexIterator) m1.getDataObject();
         * iter2.setFloatNext(dpiDD.convert(value));
         * }
         * } else if (iter != null) {
         * iter.setLongNext(value);
         * }
         */

        // since dpi must be the same for all datasets, just keep the first one
        if (isDpi && (dataset == 0)) {
          // keep track of dpi values in the tracker - perhaps not expose
          req.dpiTracker.setDpiValue(req.dpiRow, value);
        }

        if ((out != null) && (dataWidth > 0)) {
          out.f.format(" %d (%f)", value, dkey.convert(value));
        }
      }
      if (out != null) {
        out.f.format("%n");
      }
    }

    return bitOffset;
  }

  // TODO: Not sure this handles seq inside of seq inside of obs, or if compressed message allows that (?)
  private int makeNestedSequence(BitReader reader, Member member, DataDescriptor seqdd, int bitOffset, int nestedNrows,
      Request req, BitCounterCompressed bitCounterNested, DebugOut out) throws IOException {

    Sequence seq = seqdd.refersTo;
    StructureMembers.Builder membersb = StructureMembers.makeStructureMembers(seq);
    membersb.setStandardOffsets(structuresOnHeap);
    StructureMembers nestedMembers = membersb.build();

    // same number of rows in each dataset
    int nestedElements = this.ndatasets * nestedNrows;
    ByteBuffer nestedBB = ByteBuffer.allocate(nestedElements * nestedMembers.getStorageSizeBytes());
    nestedBB.order(ByteOrder.BIG_ENDIAN);

    StructureDataStorageBB nestedStorage = new StructureDataStorageBB(nestedMembers, nestedBB, nestedElements);
    nestedStorage.setStructuresOnHeap(structuresOnHeap);

    HashMap<DataDescriptor, Member> nestedMap = new HashMap<>();
    MessageArrayReaderUtils.associateMessage2Members(nestedMembers, seqdd, nestedMap);
    Request nreq =
        new Request(nestedStorage, nestedBB, nestedMembers, member.getStructureSize() * nestedNrows, nestedMap);

    if (out != null) {
      out.indent.incr();
    }

    // iterate over the number of replications, reading ndataset compressed values at each iteration
    for (int nrow = 0; nrow < nestedNrows; nrow++) {
      BitCounterCompressed[] bitCounters = bitCounterNested.getNestedCounters(nrow);
      nreq.dpiRow = nrow;
      int nestedOffset = nrow * member.getStructureSize();
      bitOffset = readData(reader, seqdd, bitOffset, nestedOffset, nreq, bitCounters, out);
    }

    if (out != null) {
      out.indent.decr();
    }

    int[] shape = {nestedElements};
    StructureDataArray nested = new StructureDataArray(nestedMembers, shape, nestedStorage);

    // We made ndatasets * nestedNrows structs, now distribute to each dataset
    int count = 0;
    for (int dataset = 0; dataset < this.ndatasets; dataset++) {
      try {
        List<Range> ranges = ImmutableList.of(new Range(count, count + nestedNrows - 1));
        StructureDataArray nestedRow = (StructureDataArray) Arrays.section(nested, ranges);
        int index = req.storageBB.putOnHeap(nestedRow);
        int pos = dataset * req.getDatasetSize() + member.getOffset();
        req.bb.position(pos);
        req.bb.putInt(index);
        count += nestedNrows;
      } catch (InvalidRangeException e) {
        throw new RuntimeException(e);
      }
    }

    return bitOffset;
  }

  private static class DpiTracker {
    DataDescriptorTreeConstructor.DataPresentIndicator dpi;
    boolean[] isPresent;
    List<DataDescriptor> dpiDD;

    DpiTracker(DataDescriptorTreeConstructor.DataPresentIndicator dpi, int nPresentFlags) {
      this.dpi = dpi;
      isPresent = new boolean[nPresentFlags];
    }

    void setDpiValue(int fldidx, long value) {
      isPresent[fldidx] = (value == 0); // present if the value is zero
    }

    DataDescriptor getDpiDD(int fldPresentIndex) {
      if (dpiDD == null) {
        dpiDD = new ArrayList<>();
        for (int i = 0; i < isPresent.length; i++) {
          if (isPresent[i])
            dpiDD.add(dpi.linear.get(i));
        }
      }
      return dpiDD.get(fldPresentIndex);
    }

    boolean isDpiDDs(DataDescriptor dkey) {
      return (dkey.f == 2) && (dkey.x == 24) && (dkey.y == 255);
    }

    boolean isDpiField(DataDescriptor dkey) {
      return (dkey.f == 2) && (dkey.x == 24) && (dkey.y == 255);
    }

  }
}
