package ucar.nc2.internal.iosp.hdf5;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.unidata.io.RandomAccessFile;

/** The low-level HDF5 data objects. */
public class H5objects {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5objects.class);

  // debugging
  private static boolean debugEnum;
  private static boolean debug1, debugDetail, debugPos, debugHeap;
  private static boolean debugGroupBtree, debugDataBtree, debugBtree2;
  private static boolean debugContinueMessage, debugTracker, debugSoftLink, debugHardLink, debugSymbolTable;
  private static boolean warnings = true, debugReference, debugRegionReference, debugCreationOrder;
  private static boolean debugDimensionScales;

  private final H5headerNew header;
  private final RandomAccessFile raf;

  private final PrintWriter debugOut;
  private final MemTracker memTracker;
  private final Map<Long, GlobalHeap> heapMap = new HashMap<>();
  private final Map<Long, H5Group> hashGroups = new HashMap<>();

  H5objects(H5headerNew header, PrintWriter debugOut, MemTracker memTracker) {
    this.header = header;
    this.raf = header.getRandomAccessFile();
    this.debugOut = debugOut;
    this.memTracker = memTracker;
  }

  H5Group readRootSymbolTable(long pos) throws IOException {
    // The root object's SymbolTableEntry
    SymbolTableEntry rootEntry = new SymbolTableEntry(pos);

    // extract the root group object, recursively read all objects
    long rootObjectAddress = rootEntry.getObjectAddress();
    DataObjectFacade f = new DataObjectFacade(null, "", rootObjectAddress);
    return new H5Group(f);
  }

  H5Group readRootObject(long rootObjectAddress) throws IOException {
    DataObjectFacade f = new DataObjectFacade(null, "", rootObjectAddress);
    return new H5Group(f);
  }

  /**
   * A DataObjectFacade can be:
   * 1) a DataObject with a specific group/name.
   * 2) a SymbolicLink to a DataObject.
   * DataObjects can be pointed to from multiple places.
   * A DataObjectFacade is in a specific group and has a name specific to that group.
   * A DataObject's name is one of its names.
   */
  class DataObjectFacade {
    H5Group parent;
    String name, displayName;
    DataObject dobj;

    boolean isGroup;
    boolean isVariable;
    boolean isTypedef;
    boolean is2DCoordinate;
    boolean hasNetcdfDimensions;

    // is a group
    H5Group group;

    // or a variable
    String dimList; // list of dimension names for this variable

    // or a link
    String linkName;

    DataObjectFacade(H5Group parent, String name, String linkName) {
      this.parent = parent;
      this.name = name;
      this.linkName = linkName;
    }

    DataObjectFacade(H5Group parent, String name, long address) throws IOException {
      this.parent = parent;
      this.name = name;
      displayName = (name.isEmpty()) ? "root" : name;
      dobj = header.getDataObject(address, displayName);

      // hash for soft link lookup
      header.addSymlinkMap(getName(), this); // LOOK does getName() match whats stored in soft link ??

      // if has a "group message", then its a group
      if ((dobj.groupMessage != null) || (dobj.groupNewMessage != null)) { // if has a "groupNewMessage", then its a
        // groupNew
        isGroup = true;

        // if it has a Datatype and a StorageLayout, then its a Variable
      } else if ((dobj.mdt != null) && (dobj.msl != null)) {
        isVariable = true;

        // if it has only a Datatype, its a Typedef
      } else if (dobj.mdt != null) {
        isTypedef = true;

      } else if (warnings) { // we dont know what it is
        log.warn("WARNING Unknown DataObjectFacade = {}", this);
        // return;
      }

    }

    String getName() {
      return (parent == null) ? name : parent.getName() + "/" + name;
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append(getName());
      if (dobj == null) {
        sbuff.append(" dobj is NULL! ");
      } else {
        sbuff.append(" id= ").append(dobj.address);
        sbuff.append(" messages= ");
        for (HeaderMessage message : dobj.messages)
          sbuff.append("\n  ").append(message);
      }

      return sbuff.toString();
    }

  }

  H5Group readH5Group(DataObjectFacade facade) throws IOException {
    return new H5Group(facade);
  }

  class H5Group {
    H5Group parent;
    String name, displayName;
    DataObjectFacade facade;
    List<DataObjectFacade> nestedObjects = new ArrayList<>(); // nested data objects
    Map<String, Dimension> dimMap = new HashMap<>();
    List<Dimension> dimList = new ArrayList<>(); // need to track dimension order

    // "Data Object Header" Level 2A
    // read a Data Object Header
    // no side effects, can be called multiple time for debugging
    private H5Group(DataObjectFacade facade) throws IOException {
      this.facade = facade;
      this.parent = facade.parent;
      this.name = facade.name;
      displayName = (name.isEmpty()) ? "root" : name;

      // if has a "group message", then its an "old group"
      if (facade.dobj.groupMessage != null) {
        // check for hard links
        if (debugHardLink) {
          log.debug("HO look for group address = {}", facade.dobj.groupMessage.btreeAddress);
        }
        if (null != (facade.group = hashGroups.get(facade.dobj.groupMessage.btreeAddress))) {
          if (debugHardLink) {
            log.debug("WARNING hard link to group = {}", facade.group.getName());
          }
          if (parent.isChildOf(facade.group)) {
            if (debugHardLink) {
              log.debug("ERROR hard link to group create a loop = {}", facade.group.getName());
            }
            log.debug("Remove hard link to group that creates a loop = {}", facade.group.getName());
            facade.group = null;
            return;
          }
        }

        // read the group, and its contained data objects.
        readGroupOld(this, facade.dobj.groupMessage.btreeAddress, facade.dobj.groupMessage.nameHeapAddress);

      } else if (facade.dobj.groupNewMessage != null) { // if has a "groupNewMessage", then its a groupNew
        // read the group, and its contained data objects.
        readGroupNew(this, facade.dobj.groupNewMessage, facade.dobj);

      } else { // we dont know what it is
        throw new IllegalStateException("H5Group needs group messages " + facade.getName());
      }

      facade.group = this;
    }

    String getName() {
      return (parent == null) ? name : parent.getName() + "/" + name;
    }

    // is this a child of that ?
    boolean isChildOf(H5Group that) {
      if (parent == null)
        return false;
      if (parent == that)
        return true;
      return parent.isChildOf(that);
    }

    @Override
    public String toString() {
      return displayName;
    }
  }

  //////////////////////////////////////////////////////////////
  // HDF5 primitive objects

  //////////////////////////////////////////////////////////////
  // Level 2A "data object header"

  DataObject readDataObject(long address, String who) throws IOException {
    return new DataObject(address, who);
  }

  public class DataObject implements Named {
    // debugging
    public long getAddress() {
      return address;
    }

    public String getName() {
      return who;
    }

    public List<HeaderMessage> getMessages() {
      List<HeaderMessage> result = new ArrayList<>(100);
      for (HeaderMessage m : messages)
        if (!(m.messData instanceof MessageAttribute))
          result.add(m);
      return result;
    }

    public List<MessageAttribute> getAttributes() {
      /*
       * List<MessageAttribute> result = new ArrayList<MessageAttribute>(100);
       * for (HeaderMessage m : messages)
       * if (m.messData instanceof MessageAttribute)
       * result.add((MessageAttribute)m.messData);
       * result.addAll(attributes);
       */
      return attributes;
    }

    long address; // aka object id : obviously unique
    String who; // may be null, may not be unique
    List<HeaderMessage> messages = new ArrayList<>();
    List<MessageAttribute> attributes = new ArrayList<>();

    // need to look for these
    MessageGroup groupMessage;
    MessageGroupNew groupNewMessage;
    MessageDatatype mdt;
    MessageDataspace mds;
    MessageLayout msl;
    MessageFilter mfp;

    byte version; // 1 or 2

    public void show(Formatter f) {
      if (mdt != null) {
        f.format("%s ", mdt.getType());
      }
      f.format("%s", getName());
      if (mds != null) {
        f.format("(");
        for (int len : mds.dimLength)
          f.format("%d,", len);
        f.format(");%n");
      }
      /*
       * for (MessageAttribute mess : getAttributes()) {
       * Attribute att = mess.getNcAttribute();
       * f.format("  :%s%n", att);
       * }
       */
      f.format("%n");
    }

    // "Data Object Header" Level 2A
    // read a Data Object Header
    // no side effects, can be called multiple time for debugging
    private DataObject(long address, String who) throws IOException {
      this.address = address;
      this.who = who;

      if (debug1) {
        log.debug("\n--> DataObject.read parsing <" + who + "> object ID/address=" + address);
      }
      if (debugPos) {
        log.debug("      DataObject.read now at position=" + raf.getFilePointer() + " for <" + who + "> reposition to "
            + header.getFileOffset(address));
      }
      // if (offset < 0) return null;
      raf.seek(header.getFileOffset(address));

      version = raf.readByte();
      if (version == 1) { // Level 2A1 (first part, before the messages)
        raf.readByte(); // skip byte
        short nmess = raf.readShort();
        if (debugDetail) {
          log.debug(" version=" + version + " nmess=" + nmess);
        }

        int referenceCount = raf.readInt();
        int headerSize = raf.readInt();
        if (debugDetail) {
          log.debug(" referenceCount=" + referenceCount + " headerSize=" + headerSize);
        }

        // if (referenceCount > 1)
        // log.debug("WARNING referenceCount="+referenceCount);
        raf.skipBytes(4); // header messages multiples of 8

        long posMess = raf.getFilePointer();
        int count = readMessagesVersion1(posMess, nmess, Integer.MAX_VALUE, this.who);
        if (debugContinueMessage) {
          log.debug(" nmessages read = {}", count);
        }
        if (debugPos) {
          log.debug("<--done reading messages for <" + who + ">; position=" + raf.getFilePointer());
        }
        if (debugTracker)
          memTracker.addByLen("Object " + who, header.getFileOffset(address), headerSize + 16);

      } else { // level 2A2 (first part, before the messages)
        // first byte was already read
        String magic = raf.readString(3);
        if (!magic.equals("HDR"))
          throw new IllegalStateException("DataObject doesnt start with OHDR");

        version = raf.readByte();
        byte flags = raf.readByte(); // data object header flags (version 2)
        if (debugDetail) {
          log.debug(" version=" + version + " flags=" + Integer.toBinaryString(flags));
        }

        // raf.skipBytes(2);
        if (((flags >> 5) & 1) == 1) {
          int accessTime = raf.readInt();
          int modTime = raf.readInt();
          int changeTime = raf.readInt();
          int birthTime = raf.readInt();
        }
        if (((flags >> 4) & 1) == 1) {
          short maxCompactAttributes = raf.readShort();
          short minDenseAttributes = raf.readShort();
        }

        long sizeOfChunk = readVariableSizeFactor(flags & 3);
        if (debugDetail) {
          log.debug(" sizeOfChunk=" + sizeOfChunk);
        }

        long posMess = raf.getFilePointer();
        int count = readMessagesVersion2(posMess, sizeOfChunk, (flags & 4) != 0, this.who);
        if (debugContinueMessage) {
          log.debug(" nmessages read = {}", count);
        }
        if (debugPos) {
          log.debug("<--done reading messages for <" + who + ">; position=" + raf.getFilePointer());
        }
      }

      // look for group or a datatype/dataspace/layout message
      for (HeaderMessage mess : messages) {
        if (debugTracker)
          memTracker.addByLen("Message (" + who + ") " + mess.mtype, mess.start, mess.size + 8);

        if (mess.mtype == MessageType.Group)
          groupMessage = (MessageGroup) mess.messData;
        else if (mess.mtype == MessageType.GroupNew)
          groupNewMessage = (MessageGroupNew) mess.messData;
        else if (mess.mtype == MessageType.SimpleDataspace)
          mds = (MessageDataspace) mess.messData;
        else if (mess.mtype == MessageType.Datatype)
          mdt = (MessageDatatype) mess.messData;
        else if (mess.mtype == MessageType.Layout)
          msl = (MessageLayout) mess.messData;
        else if (mess.mtype == MessageType.FilterPipeline)
          mfp = (MessageFilter) mess.messData;
        else if (mess.mtype == MessageType.Attribute)
          attributes.add((MessageAttribute) mess.messData);
        else if (mess.mtype == MessageType.AttributeInfo)
          processAttributeInfoMessage((MessageAttributeInfo) mess.messData, attributes);
      }

      if (debug1) {
        log.debug("<-- end DataObject {}", who);
      }
    }

    private void processAttributeInfoMessage(MessageAttributeInfo attInfo, List<MessageAttribute> list)
        throws IOException {
      long btreeAddress =
          (attInfo.v2BtreeAddressCreationOrder > 0) ? attInfo.v2BtreeAddressCreationOrder : attInfo.v2BtreeAddress;
      if ((btreeAddress < 0) || (attInfo.fractalHeapAddress < 0))
        return;

      BTree2 btree = new BTree2(header, who, btreeAddress);
      FractalHeap fractalHeap = new FractalHeap(header, who, attInfo.fractalHeapAddress, memTracker);

      for (BTree2.Entry2 e : btree.entryList) {
        byte[] heapId;
        switch (btree.btreeType) {
          case 8:
            heapId = ((BTree2.Record8) e.record).getHeapId();
            break;
          case 9:
            heapId = ((BTree2.Record9) e.record).getHeapId();
            break;
          default:
            continue;
        }

        // the heapId points to an Attribute Message in the fractal Heap
        FractalHeap.DHeapId fractalHeapId = fractalHeap.getFractalHeapId(heapId);
        long pos = fractalHeapId.getPos();
        if (pos > 0) {
          MessageAttribute attMessage = new MessageAttribute();
          if (attMessage.read(pos))
            list.add(attMessage);
          if (debugBtree2) {
            log.debug("    attMessage={}", attMessage);
          }
        }
      }
    }

    // read messages, starting at pos, until you hit maxMess read, or maxBytes read
    // if you hit a continuation message, call recursively
    // return number of messaages read
    private int readMessagesVersion1(long pos, int maxMess, int maxBytes, String objectName) throws IOException {
      if (debugContinueMessage) {
        log.debug(" readMessages start at =" + pos + " maxMess= " + maxMess + " maxBytes= " + maxBytes);
      }

      int count = 0;
      int bytesRead = 0;
      while ((count < maxMess) && (bytesRead < maxBytes)) {
        /*
         * LOOK: MessageContinue not correct ??
         * if (posMess >= actualSize)
         * break;
         */

        HeaderMessage mess = new HeaderMessage();
        // messages.add( mess);
        int n = mess.read(pos, 1, false, objectName);
        pos += n;
        bytesRead += n;
        count++;
        if (debugContinueMessage) {
          log.debug("   count=" + count + " bytesRead=" + bytesRead);
        }

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          if (debugContinueMessage) {
            log.debug(" ---ObjectHeaderContinuation--- ");
          }
          count += readMessagesVersion1(header.getFileOffset(c.offset), maxMess - count, (int) c.length, objectName);
          if (debugContinueMessage) {
            log.debug(" ---ObjectHeaderContinuation return --- ");
          }
        } else if (mess.mtype != MessageType.NIL) {
          messages.add(mess);
        }
      }
      return count;
    }

    private int readMessagesVersion2(long filePos, long maxBytes, boolean creationOrderPresent, String objectName)
        throws IOException {
      if (debugContinueMessage)
        debugOut.println(" readMessages2 starts at =" + filePos + " maxBytes= " + maxBytes);

      // maxBytes is number of bytes of messages to be read. however, a message is at least 4 bytes long, so
      // we are done if we have read > maxBytes - 4. There appears to be an "off by one" possibility
      maxBytes -= 3;

      int count = 0;
      int bytesRead = 0;
      while (bytesRead < maxBytes) {

        HeaderMessage mess = new HeaderMessage();
        // messages.add( mess);
        int n = mess.read(filePos, 2, creationOrderPresent, objectName);
        filePos += n;
        bytesRead += n;
        count++;
        if (debugContinueMessage)
          debugOut.println("   mess size=" + n + " bytesRead=" + bytesRead + " maxBytes=" + maxBytes);

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
          MessageContinue c = (MessageContinue) mess.messData;
          long continuationBlockFilePos = header.getFileOffset(c.offset);
          if (debugContinueMessage)
            debugOut.println(" ---ObjectHeaderContinuation filePos= " + continuationBlockFilePos);

          raf.seek(continuationBlockFilePos);
          String sig = readStringFixedLength(4);
          if (!sig.equals("OCHK"))
            throw new IllegalStateException(" ObjectHeaderContinuation Missing signature");

          count +=
              readMessagesVersion2(continuationBlockFilePos + 4, (int) c.length - 8, creationOrderPresent, objectName);
          if (debugContinueMessage)
            debugOut.println(" ---ObjectHeaderContinuation return --- ");
          if (debugContinueMessage)
            debugOut.println("   continuationMessages =" + count + " bytesRead=" + bytesRead + " maxBytes=" + maxBytes);

        } else if (mess.mtype != MessageType.NIL) {
          messages.add(mess);
        }
      }
      return count;
    }
  } // DataObject

  // type safe enum
  public static class MessageType {
    private static final int MAX_MESSAGE = 23;
    private static final Map<String, MessageType> hash = new HashMap<>(10);
    private static final MessageType[] mess = new MessageType[MAX_MESSAGE];

    public static final MessageType NIL = new MessageType("NIL", 0);
    public static final MessageType SimpleDataspace = new MessageType("SimpleDataspace", 1);
    public static final MessageType GroupNew = new MessageType("GroupNew", 2);
    public static final MessageType Datatype = new MessageType("Datatype", 3);
    public static final MessageType FillValueOld = new MessageType("FillValueOld", 4);
    public static final MessageType FillValue = new MessageType("FillValue", 5);
    public static final MessageType Link = new MessageType("Link", 6);
    public static final MessageType ExternalDataFiles = new MessageType("ExternalDataFiles", 7);
    public static final MessageType Layout = new MessageType("Layout", 8);
    public static final MessageType GroupInfo = new MessageType("GroupInfo", 10);
    public static final MessageType FilterPipeline = new MessageType("FilterPipeline", 11);
    public static final MessageType Attribute = new MessageType("Attribute", 12);
    public static final MessageType Comment = new MessageType("Comment", 13);
    public static final MessageType LastModifiedOld = new MessageType("LastModifiedOld", 14);
    public static final MessageType SharedObject = new MessageType("SharedObject", 15);
    public static final MessageType ObjectHeaderContinuation = new MessageType("ObjectHeaderContinuation", 16);
    public static final MessageType Group = new MessageType("Group", 17);
    public static final MessageType LastModified = new MessageType("LastModified", 18);
    public static final MessageType AttributeInfo = new MessageType("AttributeInfo", 21);
    public static final MessageType ObjectReferenceCount = new MessageType("ObjectReferenceCount", 22);

    private final String name;
    private final int num;

    private MessageType(String name, int num) {
      this.name = name;
      this.num = num;
      hash.put(name, this);
      mess[num] = this;
    }

    /**
     * Find the MessageType that matches this name.
     *
     * @param name find DataTYpe with this name.
     * @return DataType or null if no match.
     */
    public static MessageType getType(String name) {
      if (name == null)
        return null;
      return hash.get(name);
    }

    /**
     * Get the MessageType by number.
     *
     * @param num message number.
     * @return the MessageType
     */
    public static MessageType getType(int num) {
      if ((num < 0) || (num >= MAX_MESSAGE))
        return null;
      return mess[num];
    }

    /**
     * Message name.
     */
    public String toString() {
      return name + "(" + num + ")";
    }

    /**
     * @return Message number.
     */
    public int getNum() {
      return num;
    }

  }

  // Header Message: Level 2A1 and 2A2 (part of Data Object)
  public class HeaderMessage implements Comparable<HeaderMessage> {
    long start;
    byte headerMessageFlags;
    int size;
    short type, header_length;
    Named messData; // header message data

    public MessageType getMtype() {
      return mtype;
    }

    public String getName() {
      return messData.getName();
    }

    public int getSize() {
      return size;
    }

    public short getType() {
      return type;
    }

    public byte getFlags() {
      return headerMessageFlags;
    }

    public long getStart() {
      return start;
    }

    MessageType mtype;

    short creationOrder = -1;

    /**
     * Read a message
     *
     * @param filePos at this filePos
     * @param version header version
     * @param creationOrderPresent true if bit2 of data object header flags is set
     * @return number of bytes read
     * @throws IOException of read error
     */
    int read(long filePos, int version, boolean creationOrderPresent, String objectName) throws IOException {
      this.start = filePos;
      raf.seek(filePos);
      if (debugPos) {
        log.debug("  --> Message Header starts at =" + raf.getFilePointer());
      }

      if (version == 1) {
        type = raf.readShort();
        size = DataType.unsignedShortToInt(raf.readShort());
        headerMessageFlags = raf.readByte();
        raf.skipBytes(3);
        header_length = 8;

      } else {
        type = raf.readByte();
        size = DataType.unsignedShortToInt(raf.readShort());

        headerMessageFlags = raf.readByte();
        header_length = 4;
        if (creationOrderPresent) {
          creationOrder = raf.readShort();
          header_length += 2;
        }
      }
      mtype = MessageType.getType(type);
      if (debug1) {
        log.debug("  -->" + mtype + " messageSize=" + size + " flags = " + Integer.toBinaryString(headerMessageFlags));
        if (creationOrderPresent && debugCreationOrder) {
          log.debug("     creationOrder = " + creationOrder);
        }
      }
      if (debugPos) {
        log.debug("  --> Message Data starts at=" + raf.getFilePointer());
      }

      if ((headerMessageFlags & 2) != 0) { // shared
        messData = getSharedDataObject(mtype).mdt; // eg a shared datatype, eg enums
        return header_length + size;
      }

      if (mtype == MessageType.NIL) { // 0
        // dont do nuttin

      } else if (mtype == MessageType.SimpleDataspace) { // 1
        MessageDataspace data = new MessageDataspace();
        data.read();
        messData = data;

      } else if (mtype == MessageType.GroupNew) { // 2
        MessageGroupNew data = new MessageGroupNew();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Datatype) { // 3
        MessageDatatype data = new MessageDatatype();
        data.read(objectName);
        messData = data;

      } else if (mtype == MessageType.FillValueOld) { // 4
        MessageFillValueOld data = new MessageFillValueOld();
        data.read();
        messData = data;

      } else if (mtype == MessageType.FillValue) { // 5
        MessageFillValue data = new MessageFillValue();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Link) { // 6
        MessageLink data = new MessageLink();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Layout) { // 8
        MessageLayout data = new MessageLayout();
        data.read();
        messData = data;

      } else if (mtype == MessageType.GroupInfo) { // 10
        MessageGroupInfo data = new MessageGroupInfo();
        data.read();
        messData = data;

      } else if (mtype == MessageType.FilterPipeline) { // 11
        MessageFilter data = new MessageFilter();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Attribute) { // 12
        MessageAttribute data = new MessageAttribute();
        data.read(raf.getFilePointer());
        messData = data;

      } else if (mtype == MessageType.Comment) { // 13
        MessageComment data = new MessageComment();
        data.read();
        messData = data;

      } else if (mtype == MessageType.LastModifiedOld) { // 14
        MessageLastModifiedOld data = new MessageLastModifiedOld();
        data.read();
        messData = data;

      } else if (mtype == MessageType.ObjectHeaderContinuation) { // 16
        MessageContinue data = new MessageContinue();
        data.read();
        messData = data;

      } else if (mtype == MessageType.Group) { // 17
        MessageGroup data = new MessageGroup();
        data.read();
        messData = data;

      } else if (mtype == MessageType.LastModified) { // 18
        MessageLastModified data = new MessageLastModified();
        data.read();
        messData = data;

      } else if (mtype == MessageType.AttributeInfo) { // 21
        MessageAttributeInfo data = new MessageAttributeInfo();
        data.read();
        messData = data;

      } else if (mtype == MessageType.ObjectReferenceCount) { // 21
        MessageObjectReferenceCount data = new MessageObjectReferenceCount();
        data.read();
        messData = data;

      } else {
        log.debug("****UNPROCESSED MESSAGE type = " + mtype + " raw = " + type);
        log.warn("SKIP UNPROCESSED MESSAGE type = " + mtype + " raw = " + type);
        // throw new UnsupportedOperationException("****UNPROCESSED MESSAGE type = " + mtype + " raw = " + type);
      }

      return header_length + size;
    }

    public int compareTo(HeaderMessage o) {
      return Short.compare(type, o.type);
    }

    public String toString() {
      return "message type = " + mtype + "; " + messData;
    }

    // debugging
    public void showFractalHeap(Formatter f) {
      if (mtype != MessageType.AttributeInfo) {
        f.format("No fractal heap");
        return;
      }

      MessageAttributeInfo info = (MessageAttributeInfo) messData;
      info.showFractalHeap(f);
    }

    // debugging
    public void showCompression(Formatter f) {
      if (mtype != MessageType.AttributeInfo) {
        f.format("No fractal heap");
        return;
      }

      MessageAttributeInfo info = (MessageAttributeInfo) messData;
      info.showFractalHeap(f);
    }

  }

  private DataObject getSharedDataObject(MessageType mtype) throws IOException {
    byte sharedVersion = raf.readByte();
    byte sharedType = raf.readByte();
    if (sharedVersion == 1)
      raf.skipBytes(6);
    if ((sharedVersion == 3) && (sharedType == 1)) {
      long heapId = raf.readLong();
      if (debug1) {
        log.debug("     Shared Message " + sharedVersion + " type=" + sharedType + " heapId = " + heapId);
      }
      if (debugPos) {
        log.debug("  --> Shared Message reposition to =" + raf.getFilePointer());
      }
      // dunno where is the file's shared object header heap ??
      throw new UnsupportedOperationException("****SHARED MESSAGE type = " + mtype + " heapId = " + heapId);

    } else {
      long address = header.readOffset();
      if (debug1) {
        log.debug("     Shared Message " + sharedVersion + " type=" + sharedType + " address = " + address);
      }
      DataObject dobj = header.getDataObject(address, null);
      if (null == dobj)
        throw new IllegalStateException("cant find data object at" + address);
      if (mtype == MessageType.Datatype) {
        return dobj;
      }
      throw new UnsupportedOperationException("****SHARED MESSAGE type = " + mtype);
    }
  }

  interface Named {
    String getName();
  }

  // Message Type 1 : "Simple Dataspace" = dimension list / shape
  public class MessageDataspace implements Named {
    byte ndims, flags;
    byte type; // 0 A scalar dataspace, i.e. a dataspace with a single, dimensionless element.
    // 1 A simple dataspace, i.e. a dataspace with a a rank > 0 and an appropriate # of dimensions.
    // 2 A null dataspace, i.e. a dataspace with no elements.
    int[] dimLength, maxLength; // , permute;
    // boolean isPermuted;

    public String getName() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("(");
      for (int size : dimLength)
        sbuff.append(size).append(",");
      sbuff.append(")");
      return sbuff.toString();
    }

    public String toString() {
      Formatter sbuff = new Formatter();
      sbuff.format(" ndims=%d flags=%x type=%d ", ndims, flags, type);
      if (dimLength != null) {
        sbuff.format(" length=(");
        for (int size : dimLength)
          sbuff.format("%d,", size);
        sbuff.format(") ");
      }
      if (maxLength != null) {
        sbuff.format("max=(");
        for (int aMaxLength : maxLength)
          sbuff.format("%d,", aMaxLength);
        sbuff.format(")");
      }
      return sbuff.toString();
    }

    void read() throws IOException {
      if (debugPos) {
        log.debug("   *MessageSimpleDataspace start pos= " + raf.getFilePointer());
      }

      byte version = raf.readByte();
      if (version == 1) {
        ndims = raf.readByte();
        flags = raf.readByte();
        type = (byte) ((ndims == 0) ? 0 : 1);
        raf.skipBytes(5); // skip 5 bytes

      } else if (version == 2) {
        ndims = raf.readByte();
        flags = raf.readByte();
        type = raf.readByte();

      } else {
        throw new IllegalStateException("MessageDataspace: unknown version= " + version);
      }

      if (debug1) {
        log.debug("   SimpleDataspace version= " + version + " flags=" + Integer.toBinaryString(flags) + " ndims="
            + ndims + " type=" + type);
      }

      /*
       * if (ndims == 0 && !alreadyWarnNdimZero) {
       * log.warn("ndims == 0 in HDF5 file= " + raf.getLocation());
       * alreadyWarnNdimZero = true;
       * }
       */

      dimLength = new int[ndims];
      for (int i = 0; i < ndims; i++)
        dimLength[i] = (int) header.readLength();

      boolean hasMax = (flags & 0x01) != 0;
      maxLength = new int[ndims];
      if (hasMax) {
        for (int i = 0; i < ndims; i++)
          maxLength[i] = (int) header.readLength();
      } else {
        System.arraycopy(dimLength, 0, maxLength, 0, ndims);
      }

      if (debug1) {
        for (int i = 0; i < ndims; i++) {
          log.debug("    dim length = " + dimLength[i] + " max = " + maxLength[i]);
        }
      }
    }
  }

  // Message Type 17/0x11 "Old Group" or "Symbol Table"
  class MessageGroup implements Named {
    long btreeAddress, nameHeapAddress;

    void read() throws IOException {
      btreeAddress = header.readOffset();
      nameHeapAddress = header.readOffset();
      if (debug1) {
        log.debug("   Group btreeAddress=" + btreeAddress + " nameHeapAddress=" + nameHeapAddress);
      }
    }

    public String toString() {
      return " btreeAddress=" + btreeAddress + " nameHeapAddress=" + nameHeapAddress;
    }

    public String getName() {
      return Long.toString(btreeAddress);
    }

  }

  // Message Type 2 "New Group" or "Link Info" (version 2)
  class MessageGroupNew implements Named {
    long maxCreationIndex = -2, fractalHeapAddress, v2BtreeAddress, v2BtreeAddressCreationOrder = -2;

    public String toString() {
      Formatter f = new Formatter();
      f.format("   GroupNew fractalHeapAddress=%d v2BtreeAddress=%d ", fractalHeapAddress, v2BtreeAddress);
      if (v2BtreeAddressCreationOrder > -2)
        f.format(" v2BtreeAddressCreationOrder=%d ", v2BtreeAddressCreationOrder);
      if (maxCreationIndex > -2)
        f.format(" maxCreationIndex=%d", maxCreationIndex);
      f.format(" %n%n");

      if (fractalHeapAddress > 0) {
        try {
          f.format("%n%n");
          FractalHeap fractalHeap = new FractalHeap(header, "", fractalHeapAddress, memTracker);
          fractalHeap.showDetails(f);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      return f.toString();
    }

    void read() throws IOException {
      if (debugPos) {
        log.debug("   *MessageGroupNew start pos= " + raf.getFilePointer());
      }
      byte version = raf.readByte();
      byte flags = raf.readByte();
      if ((flags & 1) != 0) {
        maxCreationIndex = raf.readLong();
      }

      fractalHeapAddress = header.readOffset();
      v2BtreeAddress = header.readOffset(); // aka name index

      if ((flags & 2) != 0) {
        v2BtreeAddressCreationOrder = header.readOffset();
      }

      if (debug1) {
        log.debug("   MessageGroupNew version= " + version + " flags = " + flags + this);
      }
    }

    public String getName() {
      return Long.toString(fractalHeapAddress);
    }

  }

  // Message Type 10/0xA "Group Info" (version 2)
  class MessageGroupInfo implements Named {
    byte flags;
    short maxCompactValue = -1, minDenseValue = -1, estNumEntries = -1, estLengthEntryName = -1;

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   MessageGroupInfo ");
      if ((flags & 1) != 0)
        sbuff.append(" maxCompactValue=").append(maxCompactValue).append(" minDenseValue=").append(minDenseValue);
      if ((flags & 2) != 0)
        sbuff.append(" estNumEntries=").append(estNumEntries).append(" estLengthEntryName=").append(estLengthEntryName);
      return sbuff.toString();
    }

    void read() throws IOException {
      if (debugPos) {
        log.debug("   *MessageGroupInfo start pos= " + raf.getFilePointer());
      }
      byte version = raf.readByte();
      flags = raf.readByte();

      if ((flags & 1) != 0) {
        maxCompactValue = raf.readShort();
        minDenseValue = raf.readShort();
      }

      if ((flags & 2) != 0) {
        estNumEntries = raf.readShort();
        estLengthEntryName = raf.readShort();
      }

      if (debug1) {
        log.debug("   MessageGroupInfo version= " + version + " flags = " + flags + this);
      }
    }

    public String getName() {
      return "";
    }
  }

  // Message Type 6 "Link" (version 2)
  class MessageLink implements Named {
    byte version, flags, encoding;
    byte linkType; // 0=hard, 1=soft, 64 = external
    long creationOrder;
    String linkName, link;
    long linkAddress;

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   MessageLink ");
      sbuff.append(" name=").append(linkName).append(" type=").append(linkType);
      if (linkType == 0)
        sbuff.append(" linkAddress=" + linkAddress);
      else
        sbuff.append(" link=").append(link);

      if ((flags & 4) != 0)
        sbuff.append(" creationOrder=" + creationOrder);
      if ((flags & 0x10) != 0)
        sbuff.append(" encoding=" + encoding);
      return sbuff.toString();
    }

    void read() throws IOException {
      if (debugPos) {
        log.debug("   *MessageLink start pos= {}", raf.getFilePointer());
      }
      version = raf.readByte();
      flags = raf.readByte();

      if ((flags & 8) != 0)
        linkType = raf.readByte();

      if ((flags & 4) != 0)
        creationOrder = raf.readLong();

      if ((flags & 0x10) != 0)
        encoding = raf.readByte();

      int linkNameLength = (int) readVariableSizeFactor(flags & 3);
      linkName = readStringFixedLength(linkNameLength);

      if (linkType == 0) {
        linkAddress = header.readOffset();

      } else if (linkType == 1) {
        short len = raf.readShort();
        link = readStringFixedLength(len);

      } else if (linkType == 64) {
        short len = raf.readShort();
        link = readStringFixedLength(len); // actually 2 strings - see docs
      }

      if (debug1) {
        log.debug("   MessageLink version= " + version + " flags = " + Integer.toBinaryString(flags) + this);
      }
    }

    public String getName() {
      return linkName;
    }
  }

  // Message Type 3 : "Datatype"
  public class MessageDatatype implements Named {
    int type, version;
    byte[] flags = new byte[3];
    int byteSize;
    int endian; // 0 (LE) or 1 (BE) == RandomAccessFile.XXXXXX_ENDIAN
    boolean isOK = true;
    boolean unsigned;

    // time (2)
    DataType timeType;

    // opaque (5)
    String opaque_desc;

    // compound type (6)
    List<StructureMember> members;

    // reference (7)
    int referenceType; // 0 = object, 1 = region

    // enums (8)
    Map<Integer, String> map;
    String enumTypeName;

    // enum, variable-length, array types have "base" DataType
    MessageDatatype base;
    boolean isVString; // variable length (not a string)
    boolean isVlen; // vlen but not string

    // array (10)
    int[] dim;

    public String toString() {
      Formatter f = new Formatter();
      f.format(" datatype= %d", type);
      f.format(" byteSize= %d", byteSize);
      DataType dtype = header.getNCtype(type, byteSize, unsigned);
      f.format(" NCtype= %s %s", dtype, unsigned ? "(unsigned)" : "");
      f.format(" flags= ");
      for (int i = 0; i < 3; i++)
        f.format(" %d", flags[i]);
      f.format(" endian= %s", endian == RandomAccessFile.BIG_ENDIAN ? "BIG" : "LITTLE");

      if (type == 2) {
        f.format(" timeType= %s", timeType);
      } else if (type == 6) {
        f.format("%n  members%n");
        for (StructureMember mm : members)
          f.format("   %s%n", mm);
      } else if (type == 7) {
        f.format(" referenceType= %s", referenceType);
      } else if (type == 8) {
        f.format(" enumTypeName= %s", enumTypeName);
      } else if (type == 9) {
        f.format(" isVString= %s", isVString);
        f.format(" isVlen= %s", isVlen);
      }
      if ((type == 8) || (type == 9) || (type == 10))
        f.format(" parent base= {%s}", base);
      return f.toString();
    }

    public String getName() {
      DataType dtype = header.getNCtype(type, byteSize, unsigned);
      if (dtype != null)
        return dtype + " size= " + byteSize;
      else
        return "type=" + type + " size= " + byteSize;
    }

    public String getType() {
      DataType dtype = header.getNCtype(type, byteSize, unsigned);
      if (dtype != null)
        return dtype.toString();
      else
        return "type=" + type + " size= " + byteSize;
    }

    void read(String objectName) throws IOException {
      if (debugPos) {
        log.debug("   *MessageDatatype start pos= {}", raf.getFilePointer());
      }

      byte tandv = raf.readByte();
      type = (tandv & 0xf);
      version = ((tandv & 0xf0) >> 4);

      raf.readFully(flags);
      byteSize = raf.readInt();
      endian = ((flags[0] & 1) == 0) ? RandomAccessFile.LITTLE_ENDIAN : RandomAccessFile.BIG_ENDIAN;

      if (debug1) {
        log.debug("   Datatype type=" + type + " version= " + version + " flags = " + flags[0] + " " + flags[1] + " "
            + flags[2] + " byteSize=" + byteSize + " byteOrder="
            + (endian == RandomAccessFile.BIG_ENDIAN ? "BIG" : "LITTLE"));
      }

      if (type == 0) { // fixed point
        unsigned = ((flags[0] & 8) == 0);
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1) {
          log.debug("   type 0 (fixed point): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision
              + " unsigned= " + unsigned);
        }
        isOK = (bitOffset == 0) && (bitPrecision % 8 == 0);

      } else if (type == 1) { // floating point
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        byte expLocation = raf.readByte();
        byte expSize = raf.readByte();
        byte manLocation = raf.readByte();
        byte manSize = raf.readByte();
        int expBias = raf.readInt();
        if (debug1) {
          log.debug("   type 1 (floating point): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision
              + " expLocation= " + expLocation + " expSize= " + expSize + " manLocation= " + manLocation + " manSize= "
              + manSize + " expBias= " + expBias);
        }
      } else if (type == 2) { // time
        short bitPrecision = raf.readShort();
        if (bitPrecision == 16)
          timeType = DataType.SHORT;
        else if (bitPrecision == 32)
          timeType = DataType.INT;
        else if (bitPrecision == 64)
          timeType = DataType.LONG;

        if (debug1) {
          log.debug("   type 2 (time): bitPrecision= " + bitPrecision + " timeType = " + timeType);
        }

      } else if (type == 3) { // string (I think a fixed length seq of chars)
        int ptype = flags[0] & 0xf;
        if (debug1) {
          log.debug("   type 3 (String): pad type= " + ptype);
        }

      } else if (type == 4) { // bit field
        short bitOffset = raf.readShort();
        short bitPrecision = raf.readShort();
        if (debug1) {
          log.debug("   type 4 (bit field): bitOffset= " + bitOffset + " bitPrecision= " + bitPrecision);
        }
        // isOK = (bitOffset == 0) && (bitPrecision % 8 == 0); LOOK

      } else if (type == 5) { // opaque
        byte len = flags[0];
        opaque_desc = (len > 0) ? readString(raf).trim() : null;
        if (debug1) {
          log.debug("   type 5 (opaque): len= " + len + " desc= " + opaque_desc);
        }

      } else if (type == 6) { // compound
        int nmembers = makeUnsignedIntFromBytes(flags[1], flags[0]);
        if (debug1) {
          log.debug("   --type 6(compound): nmembers={}", nmembers);
        }
        members = new ArrayList<>();
        for (int i = 0; i < nmembers; i++) {
          members.add(new StructureMember(version, byteSize));
        }
        if (debugDetail) {
          log.debug("   --done with compound type");
        }

      } else if (type == 7) { // reference
        referenceType = flags[0] & 0xf;
        if (debug1 || debugReference) {
          log.debug("   --type 7(reference): type= {}", referenceType);
        }

      } else if (type == 8) { // enums
        int nmembers = makeUnsignedIntFromBytes(flags[1], flags[0]);
        boolean saveDebugDetail = debugDetail;
        if (debug1 || debugEnum) {
          log.debug("   --type 8(enums): nmembers={}", nmembers);
          debugDetail = true;
        }
        base = new MessageDatatype(); // base type
        base.read(objectName);
        debugDetail = saveDebugDetail;

        // read the enum names
        String[] enumName = new String[nmembers];
        for (int i = 0; i < nmembers; i++) {
          if (version < 3)
            enumName[i] = readString8(raf); // padding
          else
            enumName[i] = readString(raf); // no padding
        }

        // read the enum values; must switch to base byte order (!)
        if (base.endian >= 0) {
          raf.order(base.endian);
        }
        int[] enumValue = new int[nmembers];
        for (int i = 0; i < nmembers; i++) {
          enumValue[i] = (int) header.readVariableSizeUnsigned(base.byteSize); // assume size is 1, 2, or 4
        }
        raf.order(RandomAccessFile.LITTLE_ENDIAN);

        enumTypeName = objectName;
        map = new TreeMap<>();
        for (int i = 0; i < nmembers; i++) {
          map.put(enumValue[i], enumName[i]);
        }
        if (debugEnum) {
          for (int i = 0; i < nmembers; i++) {
            log.debug("   " + enumValue[i] + "=" + enumName[i]);
          }
        }

      } else if (type == 9) { // String (A variable-length sequence of characters) or Sequence (A variable-length
        // sequence of any datatype)
        isVString = (flags[0] & 0xf) == 1;
        if (!isVString) {
          isVlen = true;
        }
        if (debug1) {
          log.debug("   type 9(variable length): type= {}", ((isVString ? "string" : "sequence of type:")));
        }
        base = new MessageDatatype(); // base type
        base.read(objectName);

      } else if (type == 10) { // array
        if (debug1) {
          debugOut.print("   type 10(array) lengths= ");
        }
        int ndims = raf.readByte();
        if (version < 3) {
          raf.skipBytes(3);
        }

        dim = new int[ndims];
        for (int i = 0; i < ndims; i++) {
          dim[i] = raf.readInt();
          if (debug1) {
            debugOut.print(" " + dim[i]);
          }
        }

        if (version < 3) { // not present in version 3, never used anyway
          int[] pdim = new int[ndims];
          for (int i = 0; i < ndims; i++)
            pdim[i] = raf.readInt();
        }
        if (debug1) {
          log.debug("");
        }

        base = new MessageDatatype(); // base type
        base.read(objectName);

      } else if (warnings) {
        log.warn(" WARNING not dealing with type= {}", type);
      }
    }

    int getBaseType() {
      return (base != null) ? base.getBaseType() : type;
    }

    int getBaseSize() {
      return (base != null) ? base.getBaseSize() : byteSize;
    }

    byte[] getFlags() {
      return (base != null) ? base.getFlags() : flags;
    }

    boolean isVlen() {
      return (type == 10 ? base.isVlen() : isVlen);
    }

    boolean isVString() {
      return (type == 10 ? base.isVString() : isVString);
    }
  }

  class StructureMember {
    String name;
    int offset;
    byte dims;
    MessageDatatype mdt;

    StructureMember(int version, int byteSize) throws IOException {
      if (debugPos) {
        log.debug("   *StructureMember now at position={}", raf.getFilePointer());
      }

      name = readString(raf);
      if (version < 3) {
        raf.skipBytes(padding(name.length() + 1, 8));
        offset = raf.readInt();
      } else {
        offset = (int) readVariableSizeMax(byteSize);
      }

      if (debug1) {
        log.debug("   Member name=" + name + " offset= " + offset);
      }

      if (version == 1) {
        dims = raf.readByte();
        raf.skipBytes(3);
        raf.skipBytes(24); // ignore dimension info for now
      }

      // HDFdumpWithCount(buffer, raf.getFilePointer(), 16);
      mdt = new MessageDatatype();
      mdt.read(name);
      if (debugDetail) {
        log.debug("   ***End Member name={}", name);
      }

      // ??
      // HDFdump(ncfile.out, "Member end", buffer, 16);
      // if (HDFdebug) ncfile.log.debug(" Member pos="+raf.getFilePointer());
      // HDFpadToMultiple( buffer, 8);
      // if (HDFdebug) ncfile.log.debug(" Member padToMultiple="+raf.getFilePointer());
      // raf.skipBytes( 4); // huh ??
    }

    @Override
    public String toString() {
      return "StructureMember" + "{name='" + name + '\'' + ", offset=" + offset + ", dims=" + dims + ", mdt=" + mdt
          + '}';
    }
  }

  // Message Type 4 "Fill Value Old" : fill value is stored in the message
  class MessageFillValueOld implements Named {
    byte[] value;
    int size;

    void read() throws IOException {
      size = raf.readInt();
      value = new byte[size];
      raf.readFully(value);

      if (debug1) {
        log.debug("{}", this);
      }
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   FillValueOld size= ").append(size).append(" value=");
      for (int i = 0; i < size; i++)
        sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }

    public String getName() {
      StringBuilder sbuff = new StringBuilder();
      for (int i = 0; i < size; i++)
        sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }
  }

  // Message Type 5 "Fill Value New" : fill value is stored in the message, with extra metadata
  class MessageFillValue implements Named {
    byte version; // 1,2,3
    byte spaceAllocateTime; // 1= early, 2=late, 3=incremental
    byte fillWriteTime;
    int size;
    byte[] value;
    boolean hasFillValue;

    byte flags;

    void read() throws IOException {
      version = raf.readByte();

      if (version < 3) {
        spaceAllocateTime = raf.readByte();
        fillWriteTime = raf.readByte();
        hasFillValue = raf.readByte() != 0;

      } else {
        flags = raf.readByte();
        spaceAllocateTime = (byte) (flags & 3);
        fillWriteTime = (byte) ((flags >> 2) & 3);
        hasFillValue = (flags & 32) != 0;
      }

      if (hasFillValue) {
        size = raf.readInt();
        if (size > 0) {
          value = new byte[size];
          raf.readFully(value);
          hasFillValue = true;
        } else {
          hasFillValue = false;
        }
      }

      if (debug1) {
        log.debug("{}", this);
      }
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   FillValue version= ").append(version).append(" spaceAllocateTime = ").append(spaceAllocateTime)
          .append(" fillWriteTime=").append(fillWriteTime).append(" hasFillValue= ").append(hasFillValue);
      sbuff.append("\n size = ").append(size).append(" value=");
      for (int i = 0; i < size; i++)
        sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }

    public String getName() {
      StringBuilder sbuff = new StringBuilder();
      for (int i = 0; i < size; i++)
        sbuff.append(" ").append(value[i]);
      return sbuff.toString();
    }

  }

  // Message Type 8 "Data Storage Layout" : regular (contiguous), chunked, or compact (stored with the message)
  class MessageLayout implements Named {
    byte type; // 0 = Compact, 1 = Contiguous, 2 = Chunked
    long dataAddress = -1; // -1 means "not allocated"
    long contiguousSize; // size of data allocated contiguous
    int[] chunkSize; // only for chunked, otherwise must use Dataspace
    int dataSize;

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append(" type= ").append(+type).append(" (");
      switch (type) {
        case 0:
          sbuff.append("compact");
          break;
        case 1:
          sbuff.append("contiguous");
          break;
        case 2:
          sbuff.append("chunked");
          break;
        default:
          sbuff.append("unknown type= ").append(type);
      }
      sbuff.append(")");

      if (chunkSize != null) {
        sbuff.append(" storageSize = (");
        for (int i = 0; i < chunkSize.length; i++) {
          if (i > 0)
            sbuff.append(",");
          sbuff.append(chunkSize[i]);
        }
        sbuff.append(")");
      }

      sbuff.append(" dataSize=").append(dataSize);
      sbuff.append(" dataAddress=").append(dataAddress);
      return sbuff.toString();
    }

    public String getName() {
      StringBuilder sbuff = new StringBuilder();
      switch (type) {
        case 0:
          sbuff.append("compact");
          break;
        case 1:
          sbuff.append("contiguous");
          break;
        case 2:
          sbuff.append("chunked");
          break;
        default:
          sbuff.append("unknown type= ").append(type);
      }

      if (chunkSize != null) {
        sbuff.append(" chunk = (");
        for (int i = 0; i < chunkSize.length; i++) {
          if (i > 0)
            sbuff.append(",");
          sbuff.append(chunkSize[i]);
        }
        sbuff.append(")");
      }

      return sbuff.toString();
    }

    void read() throws IOException {
      int ndims;

      byte version = raf.readByte();
      if (version < 3) {
        ndims = raf.readByte();
        type = raf.readByte();
        raf.skipBytes(5); // skip 5 bytes

        boolean isCompact = (type == 0);
        if (!isCompact)
          dataAddress = header.readOffset();
        chunkSize = new int[ndims];
        for (int i = 0; i < ndims; i++)
          chunkSize[i] = raf.readInt();

        if (isCompact) {
          dataSize = raf.readInt();
          dataAddress = raf.getFilePointer();
        }

      } else {
        type = raf.readByte();

        if (type == 0) {
          dataSize = raf.readShort();
          dataAddress = raf.getFilePointer();

        } else if (type == 1) {
          dataAddress = header.readOffset();
          contiguousSize = header.readLength();

        } else if (type == 2) {
          ndims = raf.readByte();
          dataAddress = header.readOffset();
          chunkSize = new int[ndims];
          for (int i = 0; i < ndims; i++)
            chunkSize[i] = raf.readInt();
        }
      }

      if (debug1) {
        log.debug("   StorageLayout version= " + version + this);
      }
    }
  }

  // Message Type 11/0xB "Filter Pipeline" : apply a filter to the "data stream"
  class MessageFilter implements Named {
    Filter[] filters;

    void read() throws IOException {
      byte version = raf.readByte();
      byte nfilters = raf.readByte();
      if (version == 1)
        raf.skipBytes(6);

      filters = new Filter[nfilters];
      for (int i = 0; i < nfilters; i++)
        filters[i] = new Filter(version);

      if (debug1) {
        log.debug("   MessageFilter version=" + version + this);
      }
    }

    public Filter[] getFilters() {
      return filters;
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   MessageFilter filters=\n");
      for (Filter f : filters)
        sbuff.append(" ").append(f).append("\n");
      return sbuff.toString();
    }

    public String getName() {
      StringBuilder sbuff = new StringBuilder();
      for (Filter f : filters)
        sbuff.append(f.name).append(", ");
      return sbuff.toString();
    }
  }

  private static final String[] filterName = {"", "deflate", "shuffle", "fletcher32", "szip", "nbit", "scaleoffset"};

  class Filter {
    short id; // 1=deflate, 2=shuffle, 3=fletcher32, 4=szip, 5=nbit, 6=scaleoffset
    short flags;
    String name;
    short nValues;
    int[] data;

    Filter(byte version) throws IOException {
      this.id = raf.readShort();
      short nameSize = ((version > 1) && (id < 256)) ? 0 : raf.readShort(); // if the filter id < 256 then this field is
      // not stored
      this.flags = raf.readShort();
      nValues = raf.readShort();
      if (version == 1)
        this.name = (nameSize > 0) ? readString8(raf) : getFilterName(id); // null terminated, pad to 8 bytes
      else
        this.name = (nameSize > 0) ? readStringFixedLength(nameSize) : getFilterName(id); // non-null terminated

      data = new int[nValues];
      for (int i = 0; i < nValues; i++)
        data[i] = raf.readInt();
      if ((version == 1) && (nValues & 1) != 0) // check if odd
        raf.skipBytes(4);

      if (debug1) {
        log.debug("{}", this);
      }
    }

    String getFilterName(int id) {
      return (id < filterName.length) ? filterName[id] : "StandardFilter " + id;
    }

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   Filter id= ").append(id).append(" flags = ").append(flags).append(" nValues=").append(nValues)
          .append(" name= ").append(name).append(" data = ");
      for (int i = 0; i < nValues; i++)
        sbuff.append(data[i]).append(" ");
      return sbuff.toString();
    }
  }

  // Message Type 12/0xC "Attribute" : define an Attribute
  public class MessageAttribute implements Named {
    byte version;
    // short typeSize, spaceSize;
    String name;
    MessageDatatype mdt = new MessageDatatype();
    MessageDataspace mds = new MessageDataspace();

    public byte getVersion() {
      return version;
    }

    public MessageDatatype getMdt() {
      return mdt;
    }

    public MessageDataspace getMds() {
      return mds;
    }

    public long getDataPosAbsolute() {
      return dataPos;
    }

    long dataPos; // pointer to the attribute data section, must be absolute file position

    public String toString() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("   Name= ").append(name);
      sbuff.append(" dataPos = ").append(dataPos);
      if (mdt != null) {
        sbuff.append("\n mdt=");
        sbuff.append(mdt);
      }
      if (mds != null) {
        sbuff.append("\n mds=");
        sbuff.append(mds);
      }
      return sbuff.toString();
    }

    public String getName() {
      return name;
    }

    boolean read(long pos) throws IOException {
      raf.seek(pos);
      if (debugPos) {
        log.debug("   *MessageAttribute start pos= {}", raf.getFilePointer());
      }
      short nameSize, typeSize, spaceSize;
      byte flags = 0;
      byte encoding = 0; // 0 = ascii, 1 = UTF-8

      version = raf.readByte();
      if (version == 1) {
        raf.read(); // skip byte
        nameSize = raf.readShort();
        typeSize = raf.readShort();
        spaceSize = raf.readShort();

      } else if ((version == 2) || (version == 3)) {
        flags = raf.readByte();
        nameSize = raf.readShort();
        typeSize = raf.readShort();
        spaceSize = raf.readShort();
        if (version == 3)
          encoding = raf.readByte();

      } else if (version == 72) {
        flags = raf.readByte();
        nameSize = raf.readShort();
        typeSize = raf.readShort();
        spaceSize = raf.readShort();
        log.error("HDF5 MessageAttribute found bad version " + version + " at filePos " + raf.getFilePointer());
        // G:/work/galibert/IMOS_ANMN-NSW_AETVZ_20131127T230000Z_PH100_FV01_PH100-1311-Workhorse-ADCP-109.5_END-20140306T010000Z_C-20140521T053527Z.nc
        // E:/work/antonio/2014_ch.nc
        // return false;
      } else {
        log.error("bad version " + version + " at filePos " + raf.getFilePointer()); // buggery, may be HDF5 "more than
        // 8 attributes" error
        return false;
        // throw new IllegalStateException("MessageAttribute unknown version " + version);
      }

      // read the attribute name
      long filePos = raf.getFilePointer();
      name = readString(raf); // read at current pos
      if (version == 1)
        nameSize += padding(nameSize, 8);
      raf.seek(filePos + nameSize); // make it more robust for errors

      if (debug1) {
        log.debug("   MessageAttribute version= " + version + " flags = " + Integer.toBinaryString(flags)
            + " nameSize = " + nameSize + " typeSize=" + typeSize + " spaceSize= " + spaceSize + " name= " + name);
      }

      // read the datatype
      filePos = raf.getFilePointer();
      if (debugPos) {
        log.debug("   *MessageAttribute before mdt pos= {}", filePos);
      }
      boolean isShared = (flags & 1) != 0;
      if (isShared) {
        mdt = getSharedDataObject(MessageType.Datatype).mdt;
        if (debug1) {
          log.debug("    MessageDatatype: {}", mdt);
        }
      } else {
        mdt.read(name);
        if (version == 1)
          typeSize += padding(typeSize, 8);
      }
      raf.seek(filePos + typeSize); // make it more robust for errors

      // read the dataspace
      filePos = raf.getFilePointer();
      if (debugPos) {
        log.debug("   *MessageAttribute before mds = {}", filePos);
      }
      mds.read();
      if (version == 1)
        spaceSize += padding(spaceSize, 8);
      raf.seek(filePos + spaceSize); // make it more robust for errors

      // the data starts immediately afterward - ie in the message
      dataPos = raf.getFilePointer(); // note this is absolute position (no offset needed)
      if (debug1) {
        log.debug("   *MessageAttribute dataPos= {}", dataPos);
      }
      return true;
    }
  } // MessageAttribute

  // Message Type 21/0x15 "Attribute Info" (version 2)
  class MessageAttributeInfo implements Named {
    byte flags;
    short maxCreationIndex = -1;
    long fractalHeapAddress = -2, v2BtreeAddress = -2, v2BtreeAddressCreationOrder = -2;

    public String getName() {
      long btreeAddress = (v2BtreeAddressCreationOrder > 0) ? v2BtreeAddressCreationOrder : v2BtreeAddress;
      return Long.toString(btreeAddress);
    }

    public String toString() {
      Formatter f = new Formatter();
      f.format("   MessageAttributeInfo ");
      if ((flags & 1) != 0)
        f.format(" maxCreationIndex=" + maxCreationIndex);
      f.format(" fractalHeapAddress=%d v2BtreeAddress=%d", fractalHeapAddress, v2BtreeAddress);
      if ((flags & 2) != 0)
        f.format(" v2BtreeAddressCreationOrder=%d", v2BtreeAddressCreationOrder);

      showFractalHeap(f);

      return f.toString();
    }

    void showFractalHeap(Formatter f) {
      long btreeAddress = (v2BtreeAddressCreationOrder > 0) ? v2BtreeAddressCreationOrder : v2BtreeAddress;
      if ((fractalHeapAddress > 0) && (btreeAddress > 0)) {
        try {
          FractalHeap fractalHeap = new FractalHeap(header, "", fractalHeapAddress, memTracker);
          fractalHeap.showDetails(f);

          f.format(" Btree:%n");
          f.format("  type n m  offset size pos       attName%n");

          BTree2 btree = new BTree2(header, "", btreeAddress);
          for (BTree2.Entry2 e : btree.entryList) {
            byte[] heapId;
            switch (btree.btreeType) {
              case 8:
                heapId = ((BTree2.Record8) e.record).getHeapId();
                break;
              case 9:
                heapId = ((BTree2.Record9) e.record).getHeapId();
                break;
              default:
                f.format(" unknown btreetype %d%n", btree.btreeType);
                continue;
            }

            // the heapId points to an Attribute Message in the fractal Heap
            FractalHeap.DHeapId dh = fractalHeap.getFractalHeapId(heapId);
            dh.show(f);
            if (dh.getPos() > 0) {
              MessageAttribute attMessage = new MessageAttribute();
              attMessage.read(dh.getPos());
              f.format(" %-30s", trunc(attMessage.getName(), 30));
            }
            f.format(" heapId=:%s%n", Arrays.toString(heapId));
          }

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    String trunc(String s, int max) {
      if (s == null)
        return null;
      if (s.length() < max)
        return s;
      return s.substring(0, max);
    }

    void read() throws IOException {
      if (debugPos) {
        log.debug("   *MessageAttributeInfo start pos= {}", raf.getFilePointer());
      }
      byte version = raf.readByte();
      byte flags = raf.readByte();
      if ((flags & 1) != 0)
        maxCreationIndex = raf.readShort();

      fractalHeapAddress = header.readOffset();
      v2BtreeAddress = header.readOffset();

      if ((flags & 2) != 0)
        v2BtreeAddressCreationOrder = header.readOffset();

      if (debug1) {
        log.debug("   MessageAttributeInfo version= " + version + " flags = " + flags + this);
      }
    }
  }

  // Message Type 13/0xD ("Object Comment" : "short description of an Object"
  class MessageComment implements Named {
    String comment;

    void read() throws IOException {
      comment = readString(raf);
    }

    public String toString() {
      return comment;
    }

    public String getName() {
      return comment;
    }

  }

  // Message Type 18/0x12 "Last Modified" : last modified date represented as secs since 1970
  class MessageLastModified implements Named {
    byte version;
    int secs;

    void read() throws IOException {
      version = raf.readByte();
      raf.skipBytes(3); // skip byte
      secs = raf.readInt();
    }

    public String toString() {
      return new Date((long) secs * 1000).toString();
    }

    public String getName() {
      return toString();
    }
  }

  // Message Type 14/0xE ("Last Modified (old)" : last modified date represented as a String YYMM etc. use message type
  // 18 instead
  class MessageLastModifiedOld implements Named {
    String datemod;

    void read() throws IOException {
      datemod = raf.readString(14);
      if (debug1) {
        log.debug("   MessageLastModifiedOld={}", datemod);
      }
    }

    public String toString() {
      return datemod;
    }

    public String getName() {
      return toString();
    }
  }

  // Message Type 16/0x10 "Continue" : point to more messages
  class MessageContinue implements Named {
    long offset, length;

    void read() throws IOException {
      offset = header.readOffset();
      length = header.readLength();
      if (debug1) {
        log.debug("   Continue offset=" + offset + " length=" + length);
      }
    }

    public String getName() {
      return "";
    }
  }

  // Message Type 22/0x11 Object Reference COunt
  class MessageObjectReferenceCount implements Named {
    int refCount;

    void read() throws IOException {
      int version = raf.readByte();
      refCount = raf.readInt();
      if (debug1) {
        log.debug("   ObjectReferenceCount={}", refCount);
      }
    }

    public String getName() {
      return Integer.toString(refCount);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  // Groups

  private void readGroupNew(H5Group group, MessageGroupNew groupNewMessage, DataObject dobj) throws IOException {
    if (debug1) {
      log.debug("\n--> GroupNew read <{}>", group.displayName);
    }

    if (groupNewMessage.fractalHeapAddress >= 0) {
      FractalHeap fractalHeap =
          new FractalHeap(header, group.displayName, groupNewMessage.fractalHeapAddress, memTracker);

      long btreeAddress =
          (groupNewMessage.v2BtreeAddressCreationOrder >= 0) ? groupNewMessage.v2BtreeAddressCreationOrder
              : groupNewMessage.v2BtreeAddress;
      if (btreeAddress < 0)
        throw new IllegalStateException("no valid btree for GroupNew with Fractal Heap");

      // read in btree and all entries
      BTree2 btree = new BTree2(header, group.displayName, btreeAddress);
      for (BTree2.Entry2 e : btree.entryList) {
        byte[] heapId;
        switch (btree.btreeType) {
          case 5:
            heapId = ((BTree2.Record5) e.record).getHeapId();
            break;
          case 6:
            heapId = ((BTree2.Record6) e.record).getHeapId();
            break;
          default:
            continue;
        }

        // the heapId points to a Link message in the Fractal Heap
        FractalHeap.DHeapId fractalHeapId = fractalHeap.getFractalHeapId(heapId);
        long pos = fractalHeapId.getPos();
        if (pos < 0)
          continue;
        raf.seek(pos);
        MessageLink linkMessage = new MessageLink();
        linkMessage.read();
        if (debugBtree2) {
          log.debug("    linkMessage={}", linkMessage);
        }

        group.nestedObjects.add(new DataObjectFacade(group, linkMessage.linkName, linkMessage.linkAddress));
      }

    } else {
      // look for link messages
      for (HeaderMessage mess : dobj.messages) {
        if (mess.mtype == MessageType.Link) {
          MessageLink linkMessage = (MessageLink) mess.messData;
          if (linkMessage.linkType == 0) { // hard link
            group.nestedObjects.add(new DataObjectFacade(group, linkMessage.linkName, linkMessage.linkAddress));
          }
        }
      }
    }

    if (debug1) {
      log.debug("<-- end GroupNew read <" + group.displayName + ">");
    }
  }

  private void readGroupOld(H5Group group, long btreeAddress, long nameHeapAddress) throws IOException {
    // track by address for hard links
    hashGroups.put(btreeAddress, group);

    if (debug1) {
      log.debug("\n--> GroupOld read <" + group.displayName + ">");
    }
    LocalHeap nameHeap = new LocalHeap(group, nameHeapAddress);
    GroupBTree btree = new GroupBTree(group.displayName, btreeAddress);

    // now read all the entries in the btree : Level 1C
    for (SymbolTableEntry s : btree.getSymbolTableEntries()) {
      String sname = nameHeap.getString((int) s.getNameOffset());
      if (debugSoftLink) {
        log.debug("\n   Symbol name={}", sname);
      }
      if (s.cacheType == 2) {
        String linkName = nameHeap.getString(s.linkOffset);
        if (debugSoftLink) {
          log.debug("   Symbolic link name=" + linkName + " symbolName=" + sname);
        }
        group.nestedObjects.add(new DataObjectFacade(group, sname, linkName));
      } else {
        group.nestedObjects.add(new DataObjectFacade(group, sname, s.getObjectAddress()));
      }
    }
    if (debug1) {
      log.debug("<-- end GroupOld read <" + group.displayName + ">");
    }
  }

  // Level 1A
  // this just reads in all the entries into a list
  class GroupBTree {
    protected String owner;
    protected int wantType;
    private final List<SymbolTableEntry> sentries = new ArrayList<>(); // list of type SymbolTableEntry

    GroupBTree(String owner, long address) throws IOException {
      this.owner = owner;

      List<GroupBTree.Entry> entryList = new ArrayList<>();
      readAllEntries(address, entryList);

      // now convert the entries to SymbolTableEntry
      for (GroupBTree.Entry e : entryList) {
        GroupBTree.GroupNode node = new GroupBTree.GroupNode(e.address);
        sentries.addAll(node.getSymbols());
      }
    }

    List<SymbolTableEntry> getSymbolTableEntries() {
      return sentries;
    }

    // recursively read all entries, place them in order in list
    void readAllEntries(long address, List<GroupBTree.Entry> entryList) throws IOException {
      raf.seek(header.getFileOffset(address));
      if (debugGroupBtree) {
        log.debug("\n--> GroupBTree read tree at position={}", raf.getFilePointer());
      }

      String magic = raf.readString(4);
      if (!magic.equals("TREE"))
        throw new IllegalStateException("BtreeGroup doesnt start with TREE");

      int type = raf.readByte();
      int level = raf.readByte();
      int nentries = raf.readShort();
      if (debugGroupBtree) {
        log.debug("    type=" + type + " level=" + level + " nentries=" + nentries);
      }
      if (type != wantType) {
        throw new IllegalStateException("BtreeGroup must be type " + wantType);
      }

      long size = 8 + 2 * header.sizeOffsets + nentries * (header.sizeOffsets + header.sizeLengths);
      if (debugTracker)
        memTracker.addByLen("Group BTree (" + owner + ")", address, size);

      long leftAddress = header.readOffset();
      long rightAddress = header.readOffset();
      if (debugGroupBtree) {
        log.debug("    leftAddress=" + leftAddress + " " + Long.toHexString(leftAddress) + " rightAddress="
            + rightAddress + " " + Long.toHexString(rightAddress));
      }

      // read all entries in this Btree "Node"
      List<GroupBTree.Entry> myEntries = new ArrayList<>();
      for (int i = 0; i < nentries; i++) {
        myEntries.add(new GroupBTree.Entry());
      }

      if (level == 0)
        entryList.addAll(myEntries);
      else {
        for (GroupBTree.Entry entry : myEntries) {
          if (debugDataBtree) {
            log.debug("  nonzero node entry at =" + entry.address);
          }
          readAllEntries(entry.address, entryList);
        }
      }

    }

    // these are part of the level 1A data structure, type = 0
    class Entry {
      long key, address;

      Entry() throws IOException {
        this.key = header.readLength();
        this.address = header.readOffset();
        if (debugGroupBtree) {
          log.debug("     GroupEntry key={} address={}", key, address);
        }
      }
    }

    // level 1B
    class GroupNode {
      long address;
      byte version;
      short nentries;
      List<SymbolTableEntry> symbols = new ArrayList<>(); // SymbolTableEntry

      GroupNode(long address) throws IOException {
        this.address = address;

        raf.seek(header.getFileOffset(address));
        if (debugDetail) {
          log.debug("--Group Node position={}", raf.getFilePointer());
        }

        // header
        String magic = raf.readString(4);
        if (!magic.equals("SNOD")) {
          throw new IllegalStateException(magic + " should equal SNOD");
        }

        version = raf.readByte();
        raf.readByte(); // skip byte
        nentries = raf.readShort();
        if (debugDetail) {
          log.debug("   version={} nentries={}", version, nentries);
        }

        long posEntry = raf.getFilePointer();
        for (int i = 0; i < nentries; i++) {
          SymbolTableEntry entry = new SymbolTableEntry(posEntry);
          posEntry += entry.getSize();
          if (entry.objectHeaderAddress != 0) { // LOOK: Probably a bug in HDF5 file format ?? jc July 16 2010
            if (debug1) {
              log.debug("   add {}", entry);
            }
            symbols.add(entry);
          } else {
            if (debug1) {
              log.debug("   BAD objectHeaderAddress==0 !! {}", entry);
            }
          }
        }
        if (debugDetail) {
          log.debug("-- Group Node end position={}", raf.getFilePointer());
        }
        long size = 8 + nentries * 40;
        if (debugTracker)
          memTracker.addByLen("Group BtreeNode (" + owner + ")", address, size);
      }

      List<SymbolTableEntry> getSymbols() {
        return symbols;
      }
    }


  } // GroupBTree

  // aka Group Entry "level 1C"
  class SymbolTableEntry {
    long nameOffset, objectHeaderAddress;
    long btreeAddress, nameHeapAddress;
    int cacheType, linkOffset;
    long posData;

    boolean isSymbolicLink;

    SymbolTableEntry(long filePos) throws IOException {
      raf.seek(filePos);
      if (debugSymbolTable) {
        log.debug("--> readSymbolTableEntry position={}", raf.getFilePointer());
      }

      nameOffset = header.readOffset();
      objectHeaderAddress = header.readOffset();
      cacheType = raf.readInt();
      raf.skipBytes(4);

      if (debugSymbolTable) {
        log.debug(" nameOffset={} objectHeaderAddress={} cacheType={}", nameOffset, objectHeaderAddress, cacheType);
      }

      // "scratch pad"
      posData = raf.getFilePointer();
      if (debugSymbolTable)
        dump("Group Entry scratch pad", posData, 16, false);

      if (cacheType == 1) {
        btreeAddress = header.readOffset();
        nameHeapAddress = header.readOffset();
        if (debugSymbolTable) {
          log.debug("btreeAddress={} nameHeadAddress={}", btreeAddress, nameHeapAddress);
        }
      }

      // check for symbolic link
      if (cacheType == 2) {
        linkOffset = raf.readInt(); // offset in local heap
        if (debugSymbolTable) {
          log.debug("WARNING Symbolic Link linkOffset={}", linkOffset);
        }
        isSymbolicLink = true;
      }

      if (debugSymbolTable) {
        log.debug("<-- end readSymbolTableEntry position={}", raf.getFilePointer());
      }

      if (debugTracker)
        memTracker.add("SymbolTableEntry", filePos, posData + 16);
    }

    public int getSize() {
      return header.isOffsetLong() ? 40 : 32;
    }

    long getObjectAddress() {
      return objectHeaderAddress;
    }

    long getNameOffset() {
      return nameOffset;
    }

    @Override
    public String toString() {
      return "SymbolTableEntry{" + "nameOffset=" + nameOffset + ", objectHeaderAddress=" + objectHeaderAddress
          + ", btreeAddress=" + btreeAddress + ", nameHeapAddress=" + nameHeapAddress + ", cacheType=" + cacheType
          + ", linkOffset=" + linkOffset + ", posData=" + posData + ", isSymbolicLink=" + isSymbolicLink + '}';
    }
  } // SymbolTableEntry

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Heaps

  /**
   * Fetch a Vlen data array.
   *
   * @param globalHeapIdAddress address of the heapId, used to get the String out of the heap
   * @param dataType type of data
   * @param endian byteOrder of the data (0 = BE, 1 = LE)
   * @return the Array read from the heap
   * @throws IOException on read error
   */
  Array getHeapDataArray(long globalHeapIdAddress, DataType dataType, int endian)
      throws IOException, InvalidRangeException {
    HeapIdentifier heapId = new HeapIdentifier(globalHeapIdAddress);
    if (debugHeap) {
      log.debug(" heapId= {}", heapId);
    }
    return getHeapDataArray(heapId, dataType, endian);
    // Object pa = getHeapDataArray(heapId, dataType, endian);
    // return Array.factory(dataType.getPrimitiveClassType(), new int[]{heapId.nelems}, pa);
  }

  private Array getHeapDataArray(HeapIdentifier heapId, DataType dataType, int endian)
      throws IOException, InvalidRangeException {
    GlobalHeap.HeapObject ho = heapId.getHeapObject();
    if (ho == null) {
      throw new InvalidRangeException("Illegal Heap address, HeapObject = " + heapId);
    }
    if (debugHeap) {
      log.debug(" HeapObject= {}", ho);
    }
    if (endian >= 0) {
      raf.order(endian);
    }

    if (DataType.FLOAT == dataType) {
      float[] pa = new float[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readFloat(pa, 0, pa.length);
      return Array.factory(dataType, new int[] {pa.length}, pa);

    } else if (DataType.DOUBLE == dataType) {
      double[] pa = new double[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readDouble(pa, 0, pa.length);
      return Array.factory(dataType, new int[] {pa.length}, pa);

    } else if (dataType.getPrimitiveClassType() == byte.class) {
      byte[] pa = new byte[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readFully(pa, 0, pa.length);
      return Array.factory(dataType, new int[] {pa.length}, pa);

    } else if (dataType.getPrimitiveClassType() == short.class) {
      short[] pa = new short[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readShort(pa, 0, pa.length);
      return Array.factory(dataType, new int[] {pa.length}, pa);

    } else if (dataType.getPrimitiveClassType() == int.class) {
      int[] pa = new int[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readInt(pa, 0, pa.length);
      return Array.factory(dataType, new int[] {pa.length}, pa);

    } else if (dataType.getPrimitiveClassType() == long.class) {
      long[] pa = new long[heapId.nelems];
      raf.seek(ho.dataPos);
      raf.readLong(pa, 0, pa.length);
      return Array.factory(dataType, new int[] {pa.length}, pa);
    }

    throw new UnsupportedOperationException("getHeapDataAsArray dataType=" + dataType);
  }

  // see "Global Heap Id" in http://www.hdfgroup.org/HDF5/doc/H5.format.html
  HeapIdentifier readHeapIdentifier(long globalHeapIdAddress) throws IOException {
    return new HeapIdentifier(globalHeapIdAddress);
  }

  // the heap id is has already been read into a byte array at given pos
  HeapIdentifier readHeapIdentifier(ByteBuffer bb, int pos) {
    return new HeapIdentifier(bb, pos);
  }

  // see "Global Heap Id" in http://www.hdfgroup.org/HDF5/doc/H5.format.html
  class HeapIdentifier {
    final int nelems; // "number of 'base type' elements in the sequence in the heap"
    private final long heapAddress;
    private final int index;

    // address must be absolute, getFileOffset already added
    HeapIdentifier(long address) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(address);
      nelems = raf.readInt();
      heapAddress = header.readOffset();
      index = raf.readInt();
      if (debugDetail) {
        log.debug("   read HeapIdentifier address=" + address + this);
      }
      if (debugHeap)
        dump("heapIdentifier", header.getFileOffset(address), 16, true);
    }

    // the heap id is has already been read into a byte array at given pos
    HeapIdentifier(ByteBuffer bb, int pos) {
      bb.order(ByteOrder.LITTLE_ENDIAN); // header information is in le byte order
      bb.position(pos); // reletive reading
      nelems = bb.getInt();
      heapAddress = header.isOffsetLong ? bb.getLong() : (long) bb.getInt();
      index = bb.getInt();
      if (debugDetail) {
        log.debug("   read HeapIdentifier from ByteBuffer={}", this);
      }
    }

    public String toString() {
      return " nelems=" + nelems + " heapAddress=" + heapAddress + " index=" + index;
    }

    public boolean isEmpty() {
      return (heapAddress == 0);
    }

    GlobalHeap.HeapObject getHeapObject() throws IOException {
      if (isEmpty())
        return null;
      GlobalHeap gheap;
      if (null == (gheap = heapMap.get(heapAddress))) {
        gheap = new GlobalHeap(heapAddress);
        heapMap.put(heapAddress, gheap);
      }

      GlobalHeap.HeapObject ho = gheap.getHeapObject((short) index);
      if (ho == null)
        throw new IllegalStateException("cant find HeapObject");
      return ho;
    }

  } // HeapIdentifier

  class RegionReference {
    private final long heapAddress;
    private final int index;

    RegionReference(long filePos) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(filePos);
      heapAddress = header.readOffset();
      index = raf.readInt();

      GlobalHeap gheap;
      if (null == (gheap = heapMap.get(heapAddress))) {
        gheap = new GlobalHeap(heapAddress);
        heapMap.put(heapAddress, gheap);
      }

      GlobalHeap.HeapObject want = gheap.getHeapObject((short) index);
      if (debugRegionReference) {
        log.debug(" found ho={}", want);
      }
      /*
       * - The offset of the object header of the object (ie. dataset) pointed to (yes, an object ID)
       * - A serialized form of a dataspace _selection_ of elements (in the dataset pointed to).
       * I don't have a formal description of this information now, but it's encoded in the H5S_<foo>_serialize()
       * routines in
       * src/H5S<foo>.c, where foo = {all, hyper, point, none}.
       * There is _no_ datatype information stored for these kind of selections currently.
       */
      raf.seek(want.dataPos);
      long objId = raf.readLong();
      DataObject ndo = header.getDataObject(objId, null);
      // String what = (ndo == null) ? "none" : ndo.getName();
      if (debugRegionReference) {
        log.debug(" objId=" + objId + " DataObject= " + ndo);
      }
      if (null == ndo)
        throw new IllegalStateException("cant find data object at" + objId);
    }

  } // RegionReference

  // level 1E
  class GlobalHeap {
    private final byte version;
    private final int sizeBytes;
    private final Map<Short, GlobalHeap.HeapObject> hos = new HashMap<>();

    GlobalHeap(long address) throws IOException {
      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(header.getFileOffset(address));

      // header
      String magic = raf.readString(4);
      if (!magic.equals("GCOL"))
        throw new IllegalStateException(magic + " should equal GCOL");

      version = raf.readByte();
      raf.skipBytes(3);
      sizeBytes = raf.readInt();
      if (debugDetail) {
        log.debug("-- readGlobalHeap address=" + address + " version= " + version + " size = " + sizeBytes);
        // log.debug("-- readGlobalHeap address=" + address + " version= " + version + " size = " + sizeBytes);
      }
      raf.skipBytes(4); // pad to 8

      int count = 0;
      int countBytes = 0;
      while (true) {
        long startPos = raf.getFilePointer();
        GlobalHeap.HeapObject o = new GlobalHeap.HeapObject();
        o.id = raf.readShort();
        o.refCount = raf.readShort();
        raf.skipBytes(4);
        o.dataSize = header.readLength();
        o.dataPos = raf.getFilePointer();

        int dsize = ((int) o.dataSize) + padding((int) o.dataSize, 8);
        countBytes += dsize + 16;

        if (o.id == 0)
          break; // ?? look
        if (o.dataSize < 0)
          break; // ran off the end, must be done
        if (countBytes < 0)
          break; // ran off the end, must be done
        if (countBytes > sizeBytes)
          break; // ran off the end

        if (debugDetail) {
          log.debug("   HeapObject  position=" + startPos + " id=" + o.id + " refCount= " + o.refCount + " dataSize = "
              + o.dataSize + " dataPos = " + o.dataPos + " count= " + count + " countBytes= " + countBytes);
        }

        raf.skipBytes(dsize);
        hos.put(o.id, o);
        count++;

        if (countBytes + 16 >= sizeBytes)
          break; // ran off the end, must be done
      }

      if (debugDetail) {
        log.debug("-- endGlobalHeap position=" + raf.getFilePointer());
      }
      if (debugTracker)
        memTracker.addByLen("GlobalHeap", address, sizeBytes);
    }

    GlobalHeap.HeapObject getHeapObject(short id) {
      return hos.get(id);
    }

    class HeapObject {
      short id, refCount;
      long dataSize;
      long dataPos;

      @Override
      public String toString() {
        return "id=" + id + ", refCount=" + refCount + ", dataSize=" + dataSize + ", dataPos=" + dataPos;
      }
    }

  } // GlobalHeap

  // level 1D
  class LocalHeap {
    H5Group group;
    int size;
    long freelistOffset, dataAddress;
    byte[] heap;
    byte version;

    LocalHeap(H5Group group, long address) throws IOException {
      this.group = group;

      // header information is in le byte order
      raf.order(RandomAccessFile.LITTLE_ENDIAN);
      raf.seek(header.getFileOffset(address));

      if (debugDetail) {
        log.debug("-- readLocalHeap position={}", raf.getFilePointer());
      }

      // header
      String magic = raf.readString(4);
      if (!magic.equals("HEAP")) {
        throw new IllegalStateException(magic + " should equal HEAP");
      }

      version = raf.readByte();
      raf.skipBytes(3);
      size = (int) header.readLength();
      freelistOffset = header.readLength();
      dataAddress = header.readOffset();
      if (debugDetail) {
        log.debug(" version=" + version + " size=" + size + " freelistOffset=" + freelistOffset
            + " heap starts at dataAddress=" + dataAddress);
      }
      if (debugPos) {
        log.debug("    *now at position={}", raf.getFilePointer());
      }

      // data
      raf.seek(header.getFileOffset(dataAddress));
      heap = new byte[size];
      raf.readFully(heap);
      // if (debugHeap) printBytes( out, "heap", heap, size, true);

      if (debugDetail) {
        log.debug("-- endLocalHeap position={}", raf.getFilePointer());
      }
      int hsize = 8 + 2 * header.sizeLengths + header.sizeOffsets;
      if (debugTracker)
        memTracker.addByLen("Group LocalHeap (" + group.displayName + ")", address, hsize);
      if (debugTracker)
        memTracker.addByLen("Group LocalHeapData (" + group.displayName + ")", dataAddress, size);
    }

    public String getString(int offset) {
      int count = 0;
      while (heap[offset + count] != 0)
        count++;
      return new String(heap, offset, count, StandardCharsets.UTF_8);
    }

  } // LocalHeap

  // Utilitie routines

  /**
   * Read a zero terminated String. Leave file positioned after zero terminator byte.
   *
   * @param raf from this file
   * @return String (dont include zero terminator)
   * @throws IOException on io error
   */
  private String readString(RandomAccessFile raf) throws IOException {
    long filePos = raf.getFilePointer();

    int count = 0;
    while (raf.readByte() != 0)
      count++;

    raf.seek(filePos);
    String result = raf.readString(count);
    raf.readByte(); // skip the zero byte! nn
    return result;
  }

  /**
   * Read a zero terminated String at current position; advance file to a multiple of 8.
   *
   * @param raf from this file
   * @return String (dont include zero terminator)
   * @throws IOException on io error
   */
  private String readString8(RandomAccessFile raf) throws IOException {
    long filePos = raf.getFilePointer();

    int count = 0;
    while (raf.readByte() != 0)
      count++;

    raf.seek(filePos);
    byte[] s = new byte[count];
    raf.readFully(s);

    // skip to 8 byte boundary, note zero byte is skipped
    count++;
    count += padding(count, 8);
    raf.seek(filePos + count);

    return new String(s, StandardCharsets.UTF_8); // all Strings are UTF-8 unicode
  }

  /**
   * Read a String of known length.
   *
   * @param size number of bytes
   * @return String result
   * @throws IOException on io error
   */
  private String readStringFixedLength(int size) throws IOException {
    return raf.readString(size);
  }

  // size of data depends on "maximum possible number"
  private long readVariableSizeMax(int maxNumber) throws IOException {
    int size = header.getNumBytesFromMax(maxNumber);
    return header.readVariableSizeUnsigned(size);
  }

  private long readVariableSizeFactor(int sizeFactor) throws IOException {
    int size = (int) Math.pow(2, sizeFactor);
    return header.readVariableSizeUnsigned(size);
  }

  // find number of bytes needed to pad to multipleOf byte boundary
  private int padding(int nbytes, int multipleOf) {
    int pad = nbytes % multipleOf;
    if (pad != 0)
      pad = multipleOf - pad;
    return pad;
  }

  private int makeUnsignedIntFromBytes(byte upper, byte lower) {
    return DataType.unsignedByteToShort(upper) * 256 + DataType.unsignedByteToShort(lower);
  }

  private void dump(String head, long filePos, int nbytes, boolean count) throws IOException {
    if (debugOut == null)
      return;
    long savePos = raf.getFilePointer();
    if (filePos >= 0)
      raf.seek(filePos);
    byte[] mess = new byte[nbytes];
    raf.readFully(mess);
    printBytes(head, mess, nbytes, false, debugOut);
    raf.seek(savePos);
  }

  private static void printBytes(String head, byte[] buff, int n, boolean count, PrintWriter ps) {
    ps.print(head + " == ");
    for (int i = 0; i < n; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (count)
        ps.print(i + ":");
      ps.print(ub);
      if (!count) {
        ps.print("(");
        ps.print(b);
        ps.print(")");
      }
      ps.print(" ");
    }
    ps.println();
  }

}
