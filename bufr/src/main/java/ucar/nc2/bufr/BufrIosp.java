/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.bufr;

import com.google.common.collect.AbstractIterator;
import org.jdom2.Element;
import ucar.array.Array;
import ucar.array.StructureData;
import ucar.array.StructureDataArray;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** IOSP for BUFR data - using ucar.array. Registered by reflection. */
public class BufrIosp extends AbstractIOServiceProvider {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrIosp.class);

  public static final String obsRecordName = "obs";
  public static final String centerId = "BUFR:centerId";
  public static final String fxyAttName = "BUFR:TableB_descriptor";

  // debugging
  private static boolean debugIter;

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugIter = debugFlag.isSet("Bufr/iter");
  }

  Sequence obsStructure;
  Message protoMessage; // prototypical message: all messages in the file must be the same.
  HashSet<Integer> messHash;
  boolean isSingle;
  BufrConfig config;
  Element iospParam;

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    return MessageScanner.isValidFile(raf);
  }

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    setRaf(raf);

    MessageScanner scanner = new MessageScanner(raf);
    // TODO We have a problem - we havent finished building but we need to read the first as protoMessage
    // TODO Possible only trouble when theres an EmbeddedTable?
    protoMessage = scanner.getFirstDataMessage();
    if (protoMessage == null)
      throw new IOException("No data messages in the file= " + raf.getLocation());
    if (!protoMessage.isTablesComplete())
      throw new IllegalStateException("BUFR file has incomplete tables");

    // just get the fields
    config = BufrConfig.openFromMessage(raf, protoMessage, iospParam);

    // this fills the rootGroup object
    new BufrIospBuilder(protoMessage, config, rootGroup, raf.getLocation());
    isSingle = false;
  }

  @Override
  public void buildFinish(NetcdfFile ncfile) {
    super.buildFinish(ncfile);
    obsStructure = (Sequence) ncfile.findVariable(obsRecordName);
    // The proto DataDescriptor must have a link to the Sequence object to read nested Sequences.
    connectSequences(obsStructure.getVariables(), protoMessage.getRootDataDescriptor().getSubKeys());
  }

  static void connectSequences(List<Variable> variables, List<DataDescriptor> dataDescriptors) {
    for (Variable v : variables) {
      if (v instanceof Sequence) {
        findDataDescriptor(dataDescriptors, v.getShortName()).ifPresent(dds -> {
          dds.refersTo = (Sequence) v;
          // System.out.printf("connectSequences %s with %s%n", dds, v);
        });
      }
      if (v instanceof Structure) { // recurse
        findDataDescriptor(dataDescriptors, v.getShortName())
            .ifPresent(dds -> connectSequences(((Structure) v).getVariables(), dds.getSubKeys()));
      }
    }
  }

  private static Optional<DataDescriptor> findDataDescriptor(List<DataDescriptor> dataDescriptors, String name) {
    Optional<DataDescriptor> ddsOpt = dataDescriptors.stream().filter(d -> name.equals(d.name)).findFirst();
    if (ddsOpt.isPresent()) {
      return ddsOpt;
    } else {
      throw new IllegalStateException("DataDescriptor does not contain " + name);
    }
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (message instanceof Element) {
      iospParam = (Element) message;
      iospParam.detach();
      return true;
    }

    return super.sendIospMessage(message);
  }

  public Sequence getTopSequence() {
    return obsStructure;
  }

  public Message getProtoMessage() {
    return protoMessage;
  }

  public BufrConfig getConfig() {
    return config;
  }

  public Element getElem() {
    return iospParam;
  }

  @Override
  public ucar.array.Array<?> readArrayData(Variable v2, ucar.array.Section section)
      throws java.io.IOException, ucar.array.InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<ucar.array.StructureData> getSequenceIterator(Sequence s, int bufferSize) {
    findRootSequence();
    try {
      return new SeqIter();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void findRootSequence() {
    this.obsStructure = (Sequence) this.ncfile.findVariable(BufrIosp.obsRecordName);
  }

  private class SeqIter extends AbstractIterator<StructureData> {
    MessageScanner scannerIter;
    Iterator<StructureData> currIter;

    SeqIter() throws IOException {
      scannerIter = new MessageScanner(raf);
      currIter = readNextMessage();
    }

    @Override
    protected StructureData computeNext() {
      if (currIter.hasNext()) {
        return currIter.next();
      }
      currIter = readNextMessage();
      if (currIter == null) {
        return endOfData();
      }
      return computeNext();
    }

    @Nullable
    private Iterator<StructureData> readNextMessage() {
      while (true) { // dont use recursion to skip messages
        try {
          if (!scannerIter.hasNext()) {
            return null;
          }
          Message m = scannerIter.next();
          if (m == null) {
            log.warn("BUFR scanner hasNext() true but next() is null!");
            return null;
          }
          if (m.containsBufrTable()) { // skip table messages
            continue;
          }
          // mixed messages
          if (!protoMessage.equals(m)) {
            if (messHash == null)
              messHash = new HashSet<>(20);
            if (!messHash.contains(m.hashCode())) {
              log.warn(String.format("File %s has different BUFR message type proto= %s message= %s; skipping message",
                  raf.getLocation(), Integer.toHexString(protoMessage.hashCode()), Integer.toHexString(m.hashCode())));
              messHash.add(m.hashCode());
            }
            continue; // skip mixed messages
          }
          // ok we got a good one
          Array<StructureData> as = readMessage(m);
          return as.iterator();

        } catch (IOException ioe) {
          log.warn(String.format("IOException reading BUFR messages on %s", raf.getLocation(), ioe));
          return null;
        }
      }
    }
  }

  public StructureDataArray readMessage(Message m) throws IOException {
    StructureDataArray as;
    Formatter f = new Formatter();
    try {
      if (m.dds.isCompressed()) {
        MessageArrayCompressedReader comp = new MessageArrayCompressedReader(obsStructure, protoMessage, m, raf, f);
        as = comp.readEntireMessage();
      } else {
        MessageArrayUncompressedReader uncomp =
            new MessageArrayUncompressedReader(obsStructure, protoMessage, m, raf, f);
        as = uncomp.readEntireMessage();
      }
    } catch (Throwable t) {
      log.warn(String.format("BufrIosp readMessage FAIL= %s%n", f), t);
      throw t;
    }
    return as;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public String getDetailInfo() {
    Formatter ff = new Formatter();
    ff.format("%s", super.getDetailInfo());
    protoMessage.dump(ff);
    ff.format("%n");
    config.show(ff);
    return ff.toString();
  }

  @Override
  public String getFileTypeId() {
    return DataFormatType.BUFR.getDescription();
  }

  @Override
  public String getFileTypeDescription() {
    return "WMO Binary Universal Form";
  }

}
