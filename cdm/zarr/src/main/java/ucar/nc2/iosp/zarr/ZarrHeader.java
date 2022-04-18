/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import com.fasterxml.jackson.databind.ObjectMapper;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.filter.Filter;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.zarr.RandomAccessDirectory;
import ucar.unidata.io.zarr.RandomAccessDirectoryItem;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Class to read Zarr metadata from a RandomAccessDirectory and map it to a CDM Object
 */
public class ZarrHeader {

  private final RandomAccessDirectory rootRaf;
  private final Group.Builder rootGroup;
  private final String rootLocation;
  private static ObjectMapper objectMapper = new ObjectMapper();

  public ZarrHeader(RandomAccessDirectory raf, Group.Builder rootGroup) {
    this.rootRaf = raf;
    this.rootGroup = rootGroup;
    this.rootLocation = ZarrUtils.trimLocation(this.rootRaf.getLocation());
  }


  /**
   * class used to delay the creation of a variable until other files related to the variable have been read
   * i.e. check for attrs and missing chunks before instantiating Variable
   */
  private class DelayedVarMaker {
    private RandomAccessDirectoryItem var;
    private ZArray zarray;
    private Map<Integer, Long> initializedChunks; // track any uninitialized chunks for var
    private List<Attribute> attrs; // list of variable attributes
    private long dataOffset; // byte position where data starts

    void setAttrs(List<Attribute> attrs) {
      this.attrs = attrs;
    }

    void setVar(RandomAccessDirectoryItem var) {
      this.var = var;
      this.attrs = null;
      this.initializedChunks = new HashMap<>();
      this.dataOffset = -1;
      if (var != null) {
        try {
          // get RandomAccessFile for JSON parsing and read metadata
          RandomAccessFile raf = var.getOrOpenRaf();
          raf.seek(0); // reset in case file has previously been opened by another iosp
          this.zarray = objectMapper.readValue(raf, ZArray.class);
        } catch (IOException | ClassCastException ex) {
          ZarrIosp.logger.error(new ZarrFormatException(ex.getMessage()).getMessage());
          // skip var if metadata invalid
          this.var = null;
        }
      }
    }

    // check if attribute file belongs to current variable
    boolean myAttrs(RandomAccessDirectoryItem attrs) {
      if (var == null || attrs == null) {
        return false;
      }
      String attrPath = ZarrUtils.trimLocation(attrs.getLocation());
      String varPath = ZarrUtils.trimLocation(var.getLocation());
      // true if zarray and zattrs have same parent object
      return ZarrUtils.getObjectNameFromPath(attrPath).equals(ZarrUtils.getObjectNameFromPath(varPath));
    }

    void processItem(RandomAccessDirectoryItem item) {
      if (var == null) {
        return;
      }
      // get index of chunks
      int index = getChunkIndex(item, this.zarray);
      if (index < 0) { // not data files, skip rest of var
        ZarrIosp.logger.error(new ZarrFormatException().getMessage());
        this.var = null; // skip rest of var is unrecognized files found
      }
      this.initializedChunks.put(index, item.length());
      // if data offset is uninitialized, set here
      if (this.dataOffset < 0) {
        this.dataOffset = item.startIndex();
      }
    }

    void makeVar() {
      if (var == null) {
        return; // do nothing if no variable is in progress
      }
      try {
        makeVariable(var, dataOffset, zarray, initializedChunks, attrs);
      } catch (ZarrFormatException ex) {
        ZarrIosp.logger.error(ex.getMessage());
      }
      var = null; // reset var
    }
  }

  /**
   * Create CDM object on 'rootGroup' from RandomAccessFile
   * 
   * @throws IOException
   */
  public void read() throws IOException {
    List<RandomAccessDirectoryItem> items = this.rootRaf.getFilesInPath(this.rootLocation);
    DelayedVarMaker delayedVarMaker = new DelayedVarMaker();

    List<Attribute> grp_attrs = null;

    for (RandomAccessDirectoryItem item : items) {
      String filepath = ZarrUtils.trimLocation(item.getLocation());
      if (filepath.endsWith(ZarrKeys.ZATTRS)) { // attributes
        List<Attribute> attrs = makeAttributes(item);
        // assign attrs to either variable or group
        if (delayedVarMaker.myAttrs(item)) {
          delayedVarMaker.setAttrs(attrs);
        } else {
          // if .zattrs file does not belong to current var, we are in a new object and need to finish variable build
          delayedVarMaker.makeVar();
          grp_attrs = attrs;
        }
      } else if (filepath.endsWith(ZarrKeys.ZGROUP)) { // groups
        // build any vars in progress
        delayedVarMaker.makeVar();
        makeGroup(item, grp_attrs); // .zattrs will always be processed before .zgroup, so we can make group immediately
        grp_attrs = null; // reset
      } else if (filepath.endsWith(ZarrKeys.ZARRAY)) { // variables
        // build any vars in progress
        delayedVarMaker.makeVar();
        // set up variable to be created after processing the rest of the files in the folder
        delayedVarMaker.setVar(item);
      } else {
        delayedVarMaker.processItem(item);
      }
    }
    // finish making any vars in progress at end of file
    delayedVarMaker.makeVar();
  }

  private void makeGroup(RandomAccessDirectoryItem item, List<Attribute> attrs) {
    // make new Group
    Group.Builder group = Group.builder();
    String location = ZarrUtils.trimLocation(item.getLocation());
    if (location.equals(this.rootLocation + '/' + ZarrKeys.ZGROUP)) {
      group = this.rootGroup;
    }
    // set Group name
    group.setName(ZarrUtils.getObjectNameFromPath(location));

    // add current attributes, if any exist
    if (attrs != null) {
      group.addAttributes(attrs);
    }

    if (group != this.rootGroup) {
      try {
        // set parent group or throws if non-existent
        Group.Builder parentGroup = findGroup(location);
        group.setParentGroup(parentGroup);
        parentGroup.addGroup(group);
      } catch (ZarrFormatException ex) {
        ZarrIosp.logger.error(ex.getMessage());
      }
    }
  }

  private void makeVariable(RandomAccessDirectoryItem item, long dataOffset, ZArray zarray,
      Map<Integer, Long> initializedChunks, List<Attribute> attrs) throws ZarrFormatException {
    // make new Variable
    Variable.Builder var = Variable.builder();
    String location = ZarrUtils.trimLocation(item.getLocation());

    // set var name
    var.setName(ZarrUtils.getObjectNameFromPath(location));

    // set variable datatype
    var.setDataType(zarray.getDataType());

    // create and set dimensions
    int[] shape = zarray.getShape();
    List<Dimension> dims = new ArrayList<>();
    for (int d = 0; d < shape.length; d++) {
      // TODO: revisit dimension props and names (especially for nczarr)
      Dimension.Builder dim = Dimension.builder(String.format("dim%d", d), shape[d]);
      dim.setIsVariableLength(false);
      dim.setIsUnlimited(false);
      dim.setIsShared(false);
      dims.add(dim.build());
    }
    var.addDimensions(dims);

    // check that dimensions and chunks match
    int[] chunks = zarray.getChunks();
    if (shape.length != chunks.length) {
      throw new ZarrFormatException();
    }

    // create VInfo
    VInfo vinfo = new VInfo(chunks, zarray.getFillValue(), zarray.getCompressor(), zarray.getByteOrder(),
        zarray.getOrder(), zarray.getSeparator(), zarray.getFilters(), dataOffset, initializedChunks);
    var.setSPobject(vinfo);

    // add current attributes, if any exist
    if (attrs != null) {
      var.addAttributes(attrs);
    }

    // find variable's group or throw if non-existent
    Group.Builder parentGroup = findGroup(location);
    parentGroup.addVariable(var);
  }

  private List<Attribute> makeAttributes(RandomAccessDirectoryItem item) {
    // get RandomAccessFile for JSON parsing
    try (RandomAccessFile raf = item.getOrOpenRaf()) {
      // read attributes from file
      raf.seek(0);
      Map<String, Object> attrMap = objectMapper.readValue(raf, HashMap.class);

      // create Attribute objects
      List<Attribute> attrs = new ArrayList<>();
      attrMap.keySet().forEach(key -> {
        Attribute.Builder attr = Attribute.builder(key);
        Object val = attrMap.get(key);
        if (val instanceof Collection<?>) {
          attr.setValues(Arrays.asList(((Collection) val).toArray()), false);
        } else if (val instanceof Number) {
          attr.setNumericValue((Number) val, false);
        } else {
          attr.setStringValue((String) val);
        }
        attrs.add(attr.build());
      });
      return attrs;
    } catch (IOException ioe) {
      ZarrIosp.logger.error(new ZarrFormatException().getMessage());
    }
    return null;
  }

  /**
   * Get chunk number from file name
   */
  private static int getChunkIndex(RandomAccessDirectoryItem item, ZArray zarray) {
    String fileName = ZarrUtils.getDataFileName(item.getLocation());
    // return -1 if filename can't be resolved
    if (fileName.isEmpty()) {
      return -1;
    }

    int nDims = zarray.getShape().length;
    // verify is data file, else return -1
    String pattern = String.format("([0-9]+%c){%d}[0-9]+", zarray.getSeparator().charAt(0), nDims - 1);
    if (!fileName.matches(pattern)) {
      return -1;
    }

    // split by dimension separator and convert to ints
    String[] dims = fileName.split(String.format("\\%c", zarray.getSeparator().charAt(0)));
    int[] subs = Arrays.stream(dims).mapToInt(dim -> Integer.parseInt(dim)).toArray();

    // get number of chunks in each dimension
    int[] nChunks = new int[nDims];
    int[] shape = zarray.getShape();
    int[] chunkSize = zarray.getChunks();
    for (int i = 0; i < nDims; i++) {
      nChunks[i] = (int) Math.ceil(shape[i] / chunkSize[i]);
    }
    return ZarrUtils.subscriptsToIndex(subs, nChunks);
  }

  /**
   * Find Group builder matching provided name
   * 
   * @throws ZarrFormatException if group is not found
   */
  private Group.Builder findGroup(String location) throws ZarrFormatException {
    // set Group parent
    String groupName = ZarrUtils.getParentGroupNameFromPath(location, this.rootLocation);
    return this.rootGroup.findGroupNested(groupName).orElseThrow(ZarrFormatException::new);
  }

  /**
   * Contains .zarray properties that do not map directly to CDM
   */
  class VInfo {
    private final int[] chunks;
    private final Object fillValue;
    private final Filter compressor;
    private final ByteOrder byteOrder;
    private final ZArray.Order order;
    private final String separator;
    private final List<Filter> filters;
    private final long offset;
    private final Map<Integer, Long> initializedChunks;

    VInfo(int[] chunks, Object fillValue, Filter compressor, ByteOrder byteOrder, ZArray.Order order, String separator,
        List<Filter> filters, long offset, Map<Integer, Long> initializedChunks) {
      this.chunks = chunks;
      this.fillValue = fillValue;
      this.byteOrder = byteOrder;
      this.compressor = compressor;
      this.order = order;
      this.separator = separator;
      this.filters = filters;
      this.offset = offset;
      this.initializedChunks = initializedChunks;
    }

    public int[] getChunks() {
      return this.chunks;
    }

    public Object getFillValue() {
      return this.fillValue;
    }

    public Filter getCompressor() {
      return this.compressor;
    }

    public ByteOrder getByteOrder() {
      return this.byteOrder;
    }

    public ZArray.Order getOrder() {
      return this.order;
    }

    public String getSeparator() {
      return this.separator;
    }

    public List<Filter> getFilters() {
      return this.filters;
    }

    public long getOffset() {
      return this.offset;
    }

    public Map<Integer, Long> getInitializedChunks() {
      return this.initializedChunks;
    }

  }

}
