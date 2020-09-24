/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.bufr;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import ucar.array.StructureData;
import ucar.array.StructureDataArray;
import ucar.nc2.Sequence;

/** IOSP for BUFR data - using the preprocessor. */
public class BufrIospArrays extends BufrIosp {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrIospArrays.class);

  @Override
  public Iterator<ucar.array.StructureData> getStructureDataArrayIterator(Sequence s, int bufferSize)
      throws IOException {
    findRootSequence();
    return isSingle ? new SeqIterSingle() : new SeqIter();
  }

  private void findRootSequence() {
    this.obsStructure = (Sequence) this.ncfile.findVariable(BufrIospArrays.obsRecordName);
  }

  private class SeqIter implements Iterator<ucar.array.StructureData> {
    Iterator<StructureData> currIter;
    int recnum;

    @Override
    public boolean hasNext() {
      if (currIter == null) {
        try {
          currIter = readNextMessage();
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (currIter == null) {
          nelems = recnum;
          return false;
        }
      }

      if (!currIter.hasNext()) {
        try {
          currIter = readNextMessage();
        } catch (IOException e) {
          e.printStackTrace();
        }
        return hasNext();
      }

      return true;
    }

    @Override
    public StructureData next() {
      recnum++;
      return currIter.next();
    }

    private Iterator<StructureData> readNextMessage() throws IOException {
      if (!scanner.hasNext())
        return null;
      Message m = scanner.next();
      if (m == null) {
        log.warn("BUFR scanner hasNext() true but next() null!");
        return null;
      }
      if (m.containsBufrTable()) // data messages only
        return readNextMessage();

      // mixed messages
      if (!protoMessage.equals(m)) {
        if (messHash == null)
          messHash = new HashSet<>(20);
        if (!messHash.contains(m.hashCode())) {
          log.warn("File " + raf.getLocation() + " has different BUFR message types hash=" + protoMessage.hashCode()
              + "; skipping");
          messHash.add(m.hashCode());
        }
        return readNextMessage();
      }

      StructureDataArray as = readMessage(m);
      return as.iterator();
    }

    private StructureDataArray readMessage(Message m) throws IOException {
      /*
       * StructureDataArray as;
       * if (m.dds.isCompressed()) {
       * MessageCompressedDataReader comp = new MessageCompressedDataReader();
       * as = comp.readEntireMessage(obsStructure, protoMessage, m, raf, null);
       * } else {
       * MessageUncompressedDataReader uncomp = new MessageUncompressedDataReader();
       * as = uncomp.readEntireMessage(obsStructure, protoMessage, m, raf, null);
       * }
       * return as;
       */
      return null;
    }
  }

  private class SeqIterSingle implements Iterator<ucar.array.StructureData> {
    Iterator<StructureData> currIter;
    int recnum;

    @Override
    public boolean hasNext() {
      if (currIter == null) {
        try {
          currIter = readProtoMessage();
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (currIter == null) {
          nelems = recnum;
          return false;
        }
      }

      return currIter.hasNext();
    }

    @Override
    public StructureData next() {
      recnum++;
      return currIter.next();
    }

    private Iterator<StructureData> readProtoMessage() throws IOException {
      /*
       * Message m = protoMessage;
       * StructureDataArray as;
       * if (m.dds.isCompressed()) {
       * MessageCompressedDataReader reader = new MessageCompressedDataReader();
       * as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
       * } else {
       * MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
       * as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
       * }
       * 
       * return as.iterator();
       */
      return null;
    }
  }
}
