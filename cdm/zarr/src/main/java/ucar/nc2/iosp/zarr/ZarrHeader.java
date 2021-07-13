/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
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

  private static final Logger logger = LoggerFactory.getLogger(ZarrHeader.class);

  private final RandomAccessDirectory rootRaf;
  private final Group.Builder rootGroup;
  private final String rootLocation;

  private List<Attribute> attrs;

  private static ObjectMapper objectMapper = new ObjectMapper();

  public ZarrHeader(RandomAccessDirectory raf, Group.Builder rootGroup) {
    this.rootRaf = raf;
    this.rootGroup = rootGroup;
    this.rootLocation = ZarrPathUtils.trimLocation(this.rootRaf.getLocation());
    this.attrs = null;
  }

  /**
   * Create CDM object on 'rootGroup' from RandomAccessFile
   * 
   * @throws IOException
   */
  public void read() throws IOException {
    List<RandomAccessDirectoryItem> items = this.rootRaf.getFilesInPath(this.rootLocation);
    // because items are ordered alphabetically, it is safe to assume groups will be created before subgroups and vars
    // However, we need to assure we make attrs before their groups and vars
    // assumes no files will be read between .zarray and .zattrs

    RandomAccessDirectoryItem var = null;
    for (RandomAccessDirectoryItem item : items) {
      String filepath = ZarrPathUtils.trimLocation(item.getLocation());
      if (filepath.endsWith(ZarrKeys.ZATTRS)) {
        try {
          attrs = makeAttributes(item); // process .zattrs into attributes list
        } catch (ZarrFormatException ex) {
          logger.error(ex.getMessage());
          attrs = null;
        }
      } else {
        if (var != null) { // make variable after we've checked if next file is .zattrs
          try {
            makeVariable(var);
            var = null;
          } catch (ZarrFormatException ex) {
            logger.error(ex.getMessage());
            attrs = null;
            var = null;
          }
        }
        if (filepath.endsWith(ZarrKeys.ZGROUP)) {
          try {
            makeGroup(item); // .zattrs will always be processed before .zgroup, so we make groups immediately
          } catch (ZarrFormatException ex) {
            logger.error(ex.getMessage());
          }
        } else if (filepath.endsWith(ZarrKeys.ZARRAY)) {
          var = item; // .zarray appears before .zattrs, so we delay processing until next file is read
        }
      }
    }
    if (var != null) { // build any unbuilt var
      try {
        makeVariable(var);
      } catch (ZarrFormatException ex) {
        logger.error(ex.getMessage());
      }
    }
  }

  private void makeGroup(RandomAccessDirectoryItem item) throws ZarrFormatException {
    // make new Group
    Group.Builder group = Group.builder();
    String location = ZarrPathUtils.trimLocation(item.getLocation());
    if (location.equals(this.rootLocation + ZarrKeys.ZGROUP)) {
      group = this.rootGroup;
    }
    // set Group name
    group.setName(ZarrPathUtils.getObjectNameFromPath(location));

    // add current attributes, if any exist
    if (attrs != null) {
      group.addAttributes(attrs);
      attrs = null; // reset attrs
    }

    if (group != this.rootGroup) {
      // set parent group or throws if non-existent
      Group.Builder parentGroup = findGroup(location);
      group.setParentGroup(parentGroup);
      parentGroup.addGroup(group);
    }
  }

  private void makeVariable(RandomAccessDirectoryItem item) throws IOException, ZarrFormatException {
    // make new Variable
    Variable.Builder var = Variable.builder();
    String location = ZarrPathUtils.trimLocation(item.getLocation());

    // set var name
    var.setName(ZarrPathUtils.getObjectNameFromPath(location));
    // get RandomAccessFile for JSON parsing
    RandomAccessFile raf = item.getOrOpenRaf();
    raf.seek(0); // reset in case file has previously been opened by another iosp

    try {
      ZArray zarray = objectMapper.readValue(raf, ZArray.class);

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

      // create VInfo
      VInfo vinfo = new VInfo(zarray.getChunks(), zarray.getFillValue(), zarray.getCompressor(), zarray.getByteOrder(),
          zarray.getOrder(), zarray.getSeparator(), zarray.getFilters());
      var.setSPobject(vinfo);
    } catch (IOException | ClassCastException ex) {
      throw new ZarrFormatException();
    }

    // add current attributes, if any exist
    if (attrs != null) {
      var.addAttributes(attrs);
      attrs = null; // reset attrs
    }

    // find variable's group or throw if non-existent
    Group.Builder parentGroup = findGroup(location);
    parentGroup.addVariable(var);
  }

  private List<Attribute> makeAttributes(RandomAccessDirectoryItem item) throws IOException, ZarrFormatException {
    // get RandomAccessFile for JSON parsing
    String location = ZarrPathUtils.trimLocation(item.getLocation());
    RandomAccessFile raf = item.getOrOpenRaf();

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
  }

  /**
   * Find Group builder matching provided name
   * 
   * @throws ZarrFormatException if group is not found
   */
  private Group.Builder findGroup(String location) throws ZarrFormatException {
    // set Group parent
    String groupName = ZarrPathUtils.getParentGroupNameFromPath(location, this.rootLocation);
    return this.rootGroup.findGroupNested(groupName).orElseThrow(ZarrFormatException::new);
  }

  /**
   * Contains .zarray properties that do not map directly to CDM
   */
  class VInfo {
    private final int[] chunks;
    private final Number fillValue;
    private final ZarrFilter compressor;
    private final ByteOrder byteOrder;
    private final ZArray.Order order;
    private final String separator;
    private final List<ZarrFilter> filters;

    VInfo(int[] chunks, Number fillValue, ZarrFilter compressor, ByteOrder byteOrder, ZArray.Order order,
        String separator, List<ZarrFilter> filters) {
      this.chunks = chunks;
      this.fillValue = fillValue;
      this.byteOrder = byteOrder;
      this.compressor = compressor;
      this.order = order;
      this.separator = separator;
      this.filters = filters;
    }

    public int[] getChunks() {
      return this.chunks;
    }

    public Number getFillValue() {
      return this.fillValue;
    }

    public ZarrFilter getCompressor() {
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

    public List<ZarrFilter> getFilters() {
      return this.filters;
    }

  }

}
