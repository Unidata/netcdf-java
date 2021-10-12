/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.bufr;

import java.io.IOException;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.jdom2.Element;
import ucar.ma2.Array;
import ucar.ma2.ArraySequence;
import ucar.ma2.ArrayStructure;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

/** IOSP for BUFR data - using the preprocessor. */
public class BufrIosp extends AbstractIOServiceProvider {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrIosp.class);

  public static final String obsRecordName = "obs";
  public static final String fxyAttName = "BUFR:TableB_descriptor";
  public static final String centerId = "BUFR:centerId";

  // debugging
  private static boolean debugIter;

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugIter = debugFlag.isSet("Bufr/iter");
  }

  Sequence obsStructure;
  Message protoMessage; // prototypical message: all messages in the file must be the same.
  MessageScanner scanner;
  HashSet<Integer> messHash;
  boolean isSingle;
  BufrConfig config;
  Element iospParam;

  @Override
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return MessageScanner.isValidFile(raf);
  }

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    super.open(raf, rootGroup.getNcfile(), cancelTask);

    scanner = new MessageScanner(raf, 0, false);
    // TODO We have a problem - we havent finished building but we need to read the first message to use as the
    // protoMessage.
    // TODO Possible only trouble when theres an EmbeddedTable?
    protoMessage = scanner.getFirstDataMessage();
    if (protoMessage == null)
      throw new IOException("No data messages in the file= " + raf.getLocation());
    if (!protoMessage.isTablesComplete())
      throw new IllegalStateException("BUFR file has incomplete tables");

    // just get the fields
    config = BufrConfig.openFromMessage(raf, protoMessage, iospParam);

    // this fills the netcdf object
    new BufrIospBuilder(protoMessage, config, rootGroup, raf.getLocation());
    isSingle = false;
  }

  @Override
  public void buildFinish(NetcdfFile ncfile) {
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

  // for BufrMessageViewer
  public NetcdfFile open(RandomAccessFile raf, Message single) throws IOException {
    this.raf = raf;

    protoMessage = single;
    protoMessage.getRootDataDescriptor(); // construct the data descriptors, check for complete tables
    if (!protoMessage.isTablesComplete())
      throw new IllegalStateException("BUFR file has incomplete tables");

    BufrConfig config = BufrConfig.openFromMessage(raf, protoMessage, null);

    // this fills the netcdf object
    ConstructNetcdf construct = new ConstructNetcdf(protoMessage, config, raf.getLocation());
    this.ncfile = construct.getNetcdfFile();
    this.obsStructure = construct.getObsStructure();
    this.isSingle = true;
    return this.ncfile;
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

  public BufrConfig getConfig() {
    return config;
  }

  public Element getElem() {
    return iospParam;
  }

  int nelems = -1;

  @Override
  public Array readData(Variable v2, Section section) {
    findRootSequence();
    return new ArraySequence(obsStructure.makeStructureMembers(), new SeqIter(), nelems);
  }

  @Override
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) {
    findRootSequence();
    return isSingle ? new SeqIterSingle() : new SeqIter();
  }

  private void findRootSequence() {
    this.obsStructure = (Sequence) this.ncfile.findVariable(BufrIosp.obsRecordName);
  }

  private class SeqIter implements StructureDataIterator {
    StructureDataIterator currIter;
    int recnum;

    SeqIter() {
      reset();
    }

    @Override
    public StructureDataIterator reset() {
      recnum = 0;
      currIter = null;
      scanner.reset();
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (currIter == null) {
        currIter = readNextMessage();
        if (currIter == null) {
          nelems = recnum;
          return false;
        }
      }

      if (!currIter.hasNext()) {
        currIter = readNextMessage();
        return hasNext();
      }

      return true;
    }

    @Override
    public StructureData next() throws IOException {
      recnum++;
      return currIter.next();
    }

    private StructureDataIterator readNextMessage() throws IOException {
      while (true) { // dont use recursion to skip messages
        if (!scanner.hasNext())
          return null;
        Message m = scanner.next();
        if (m == null) {
          log.warn("BUFR scanner hasNext() true but next() null!");
          return null;
        }
        if (m.containsBufrTable()) // data messages only
          continue;

        // mixed messages
        if (!protoMessage.equals(m)) {
          if (messHash == null)
            messHash = new HashSet<>(20);
          if (!messHash.contains(m.hashCode())) {
            log.warn("File " + raf.getLocation() + " has different BUFR message types hash=" + protoMessage.hashCode()
                + "; skipping");
            messHash.add(m.hashCode());
          }
          continue;
        }

        ArrayStructure as = readMessage(m);
        return as.getStructureDataIterator();
      }
    }

    private ArrayStructure readMessage(Message m) throws IOException {
      ArrayStructure as;
      if (m.dds.isCompressed()) {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
      } else {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
      }
      return as;
    }

    @Override
    public int getCurrentRecno() {
      return recnum - 1;
    }

    @Override
    public void close() {
      if (currIter != null)
        currIter.close();
      currIter = null;
      if (debugIter)
        System.out.printf("BUFR read recnum %d%n", recnum);
    }
  }

  private class SeqIterSingle implements StructureDataIterator {
    StructureDataIterator currIter;
    int recnum;

    SeqIterSingle() {
      reset();
    }

    @Override
    public StructureDataIterator reset() {
      recnum = 0;
      currIter = null;
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (currIter == null) {
        currIter = readProtoMessage();
        if (currIter == null) {
          nelems = recnum;
          return false;
        }
      }

      return currIter.hasNext();
    }

    @Override
    public StructureData next() throws IOException {
      recnum++;
      return currIter.next();
    }

    private StructureDataIterator readProtoMessage() throws IOException {
      Message m = protoMessage;
      ArrayStructure as;
      if (m.dds.isCompressed()) {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
      } else {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
      }

      return as.getStructureDataIterator();
    }

    @Override
    public int getCurrentRecno() {
      return recnum - 1;
    }

    @Override
    public void close() {
      if (currIter != null)
        currIter.close();
      currIter = null;
    }
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
