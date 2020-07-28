/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.collect.ImmutableList;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.internal.iosp.netcdf3.N3headerNew;
import ucar.nc2.internal.iosp.netcdf3.N3iospNew;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DebugFlags;
import ucar.nc2.util.EscapeStrings;
import ucar.nc2.util.Indent;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.write.NcmlWriter;
import ucar.unidata.io.RandomAccessFile;

/**
 * <p>
 * Read-only scientific datasets that are accessible through the netCDF API.
 * Immutable after {@code setImmutable()} is called. Reading data is not
 * thread-safe because of the use of {@code RandomAccessFile}.
 * <p>
 * Using this class's {@code Builder} scheme to create a {@code NetcdfFile} object could, for
 * example, be accomplished as follows, using a try/finally block to ensure that the
 * {@code NetcdfFile} is closed when done.
 * 
 * <pre>
 * NetcdfFile ncfile = null;
 * try {
 *   ncfile = NetcdfFile.builder().setLocation(fileName).build();
 *   // do stuff
 * } finally {
 *   if (ncfile != null) {
 *     ncfile.close();
 *   }
 * }
 * </pre>
 * 
 * More conveniently, a {@code NetcdfFile} object may be created using one of the static methods
 * in {@code NetcdfFiles}:
 * 
 * <pre>
 * NetcdfFile ncfile = null;
 * try {
 *   ncfile = NetcdfFiles.open(fileName);
 *   // do stuff
 * } finally {
 *   if (ncfile != null) {
 *     ncfile.close();
 *   }
 * }
 * </pre>
 * 
 * Or better yet, use try-with-resources:
 * 
 * <pre>
 * try (NetcdfFile ncfile = NetcdfFiles.open(fileName)) {
 *   // do stuff
 * }
 * </pre>
 *
 * <h3>Naming</h3>
 * Each object has a name (aka "full name") that is unique within the entire netcdf file, and
 * a "short name" that is unique within the parent group.
 * These coincide for objects in the root group, and so are backwards compatible with version
 * 3 files.
 * <ol>
 * <li>Variable: group1/group2/varname
 * <li>Structure member Variable: group1/group2/varname.s1.s2
 * <li>Group Attribute: group1/group2@attName
 * <li>Variable Attribute: group1/group2/varName@attName
 * </ol>
 * </p>
 */
public class NetcdfFile implements FileCacheable, Closeable {
  private static final Logger log = LoggerFactory.getLogger(NetcdfFile.class);

  @Deprecated
  public static final String IOSP_MESSAGE_ADD_RECORD_STRUCTURE = "AddRecordStructure";
  @Deprecated
  public static final String IOSP_MESSAGE_CONVERT_RECORD_STRUCTURE = "ConvertRecordStructure"; // not implemented yet
  @Deprecated
  public static final String IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE = "RemoveRecordStructure";
  public static final String IOSP_MESSAGE_RANDOM_ACCESS_FILE = "RandomAccessFile";

  static boolean debugSPI, debugCompress;
  static boolean debugStructureIterator;
  private static boolean showRequest;

  /** @deprecated do not use */
  @Deprecated
  public static void setDebugFlags(DebugFlags debugFlag) {
    debugSPI = debugFlag.isSet("NetcdfFile/debugSPI");
    debugCompress = debugFlag.isSet("NetcdfFile/debugCompress");
    debugStructureIterator = debugFlag.isSet("NetcdfFile/structureIterator");
    N3headerNew.disallowFileTruncation = debugFlag.isSet("NetcdfFile/disallowFileTruncation");
    N3headerNew.debugHeaderSize = debugFlag.isSet("NetcdfFile/debugHeaderSize");
    showRequest = debugFlag.isSet("NetcdfFile/showRequest");
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Close all resources (files, sockets, etc) associated with this file.
   * If the underlying file was acquired, it will be released, otherwise closed.
   * if isClosed() already, nothing will happen
   *
   * @throws IOException if error when closing
   */
  public synchronized void close() throws IOException {
    if (cache != null) {
      if (cache.release(this))
        return;
    }

    try {
      if (null != iosp) {
        // log.warn("NetcdfFile.close called for ncfile="+this.hashCode()+" for iosp="+spi.hashCode());
        iosp.close();
      }
    } finally {
      iosp = null;
    }
  }

  /**
   * Public by accident.
   * Release any resources like file handles
   * 
   * @deprecated do not use
   */
  @Deprecated
  public void release() throws IOException {
    if (iosp != null)
      iosp.release();
  }

  /**
   * Public by accident.
   * Reacquire any resources like file handles
   * 
   * @deprecated do not use
   */
  @Deprecated
  public void reacquire() throws IOException {
    if (iosp != null)
      iosp.reacquire();
  }

  /**
   * Public by accident.
   * Optional file caching.
   * 
   * @deprecated do not use
   */
  @Deprecated
  public synchronized void setFileCache(FileCacheIF cache) {
    this.cache = cache;
  }

  /**
   * Public by accident.
   * Get the name used in the cache, if any.
   *
   * @return name in the cache.
   * @deprecated do not use
   */
  @Deprecated
  public String getCacheName() {
    return cacheName;
  }

  /**
   * Public by accident.
   *
   * @param cacheName name in the cache, should be unique for this NetcdfFile. Usually the location.
   * @deprecated do not use
   */
  @Deprecated
  protected void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  /**
   * Get the NetcdfFile location. This is a URL, or a file pathname.
   *
   * @return location URL or file pathname.
   */
  public String getLocation() {
    return location;
  }

  /**
   * Get the globally unique dataset identifier, if it exists.
   *
   * @return id, or null if none.
   */
  @Nullable
  public String getId() {
    return id;
  }

  /**
   * Get the human-readable title, if it exists.
   *
   * @return title, or null if none.
   */
  @Nullable
  public String getTitle() {
    return title;
  }

  /**
   * Get the root group.
   *
   * @return root group
   */
  public Group getRootGroup() {
    return rootGroup;
  }

  /**
   * Find a Group, with the specified (full) name.
   * A full name should start with a '/'. For backwards compatibility, we accept full names that omit the leading '/'.
   * An embedded '/' separates subgroup names.
   *
   * @param fullName eg "/group/subgroup/wantGroup". Null or empty string returns the root group.
   * @return Group or null if not found.
   */
  @Nullable
  public Group findGroup(@Nullable String fullName) {
    if (fullName == null || fullName.isEmpty())
      return rootGroup;

    Group g = rootGroup;
    StringTokenizer stoke = new StringTokenizer(fullName, "/");
    while (stoke.hasMoreTokens()) {
      String groupName = NetcdfFiles.makeNameUnescaped(stoke.nextToken());
      g = g.findGroupLocal(groupName);
      if (g == null)
        return null;
    }
    return g;
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // compatibilty API

  /**
   * Find a Variable by short name, in the given group.
   *
   * @param g A group in this file. Null for root group.
   * @param shortName short name of the Variable.
   * @return Variable if found, else null.
   * @deprecated use g.findVariable(shortName)
   */
  @Deprecated
  @Nullable
  public Variable findVariable(Group g, String shortName) {
    if (g == null)
      return findVariable(shortName);
    return g.findVariableLocal(shortName);
  }

  /**
   * Find a Variable, with the specified (escaped full) name.
   * It may possibly be nested in multiple groups and/or structures.
   * An embedded "." is interpreted as structure.member.
   * An embedded "/" is interpreted as group/variable.
   * If the name actually has a ".", you must escape it (call NetcdfFiles.makeValidPathName(varname))
   * Any other chars may also be escaped, as they are removed before testing.
   *
   * @param fullNameEscaped eg "/group/subgroup/name1.name2.name".
   * @return Variable or null if not found.
   */
  @Nullable
  public Variable findVariable(String fullNameEscaped) {
    if (fullNameEscaped == null || fullNameEscaped.isEmpty()) {
      return null;
    }

    Group g = rootGroup;
    String vars = fullNameEscaped;

    // break into group/group and var.var
    int pos = fullNameEscaped.lastIndexOf('/');
    if (pos >= 0) {
      String groups = fullNameEscaped.substring(0, pos);
      vars = fullNameEscaped.substring(pos + 1);
      StringTokenizer stoke = new StringTokenizer(groups, "/");
      while (stoke.hasMoreTokens()) {
        String token = NetcdfFiles.makeNameUnescaped(stoke.nextToken());
        g = g.findGroupLocal(token);
        if (g == null)
          return null;
      }
    }

    // heres var.var - tokenize respecting the possible escaped '.'
    List<String> snames = EscapeStrings.tokenizeEscapedName(vars);
    if (snames.isEmpty())
      return null;

    String varShortName = NetcdfFiles.makeNameUnescaped(snames.get(0));
    Variable v = g.findVariableLocal(varShortName);
    if (v == null)
      return null;

    int memberCount = 1;
    while (memberCount < snames.size()) {
      if (!(v instanceof Structure))
        return null;
      String name = NetcdfFiles.makeNameUnescaped(snames.get(memberCount++));
      v = ((Structure) v).findVariable(name);
      if (v == null)
        return null;
    }
    return v;
  }

  /**
   * Look in the given Group and in its nested Groups for a Variable with a String valued Attribute with the given name
   * and value.
   *
   * @param g start with this Group, null for the root Group.
   * @param attName look for an Attribuite with this name.
   * @param attValue look for an Attribuite with this value.
   * @return the first Variable that matches, or null if none match.
   * @deprecated use g.findVariableByAttribute(String attName, String attValue)
   */
  @Deprecated
  @Nullable
  public Variable findVariableByAttribute(Group g, String attName, String attValue) {
    if (g == null)
      g = getRootGroup();
    for (Variable v : g.getVariables()) {
      for (Attribute att : v.attributes())
        if (attName.equals(att.getShortName()) && attValue.equals(att.getStringValue()))
          return v;
    }
    for (Group nested : g.getGroups()) {
      Variable v = findVariableByAttribute(nested, attName, attValue);
      if (v != null)
        return v;
    }
    return null;
  }

  /**
   * Finds a Dimension with the specified full name. It may be nested in multiple groups.
   * An embedded "/" is interpreted as a group separator. A leading slash indicates the root group. That slash may be
   * omitted, but the {@code fullName} will be treated as if it were there. In other words, the first name token in
   * {@code fullName} is treated as the short name of a Group or Dimension, relative to the root group.
   *
   * @param fullName Dimension full name, e.g. "/group/subgroup/dim".
   * @return the Dimension or {@code null} if it wasn't found.
   */
  @Nullable
  public Dimension findDimension(String fullName) {
    if (fullName == null || fullName.isEmpty()) {
      return null;
    }

    Group group = rootGroup;
    String dimShortName = fullName;

    // break into group/group and dim
    int pos = fullName.lastIndexOf('/');
    if (pos >= 0) {
      String groups = fullName.substring(0, pos);
      dimShortName = fullName.substring(pos + 1);

      StringTokenizer stoke = new StringTokenizer(groups, "/");
      while (stoke.hasMoreTokens()) {
        String token = NetcdfFiles.makeNameUnescaped(stoke.nextToken());
        group = group.findGroupLocal(token);

        if (group == null) {
          return null;
        }
      }
    }

    return group.findDimensionLocal(dimShortName);
  }

  /**
   * Return true if this file has one or more unlimited (record) dimension.
   *
   * @return if this file has an unlimited Dimension(s)
   */
  public boolean hasUnlimitedDimension() {
    return getUnlimitedDimension() != null;
  }

  /**
   * Return the unlimited (record) dimension, or null if not exist.
   * If there are multiple unlimited dimensions, it will return the first one.
   *
   * @return the unlimited Dimension, or null if none.
   */
  @Nullable
  public Dimension getUnlimitedDimension() {
    for (Dimension d : dimensions) {
      if (d.isUnlimited())
        return d;
    }
    return null;
  }

  /**
   * Get the shared Dimensions used in this file.
   * <p>
   * If the dimensions are in a group, the dimension name will have the
   * group name, in order to disambiguate the dimensions. This means that
   * a Variable's dimensions will not match Dimensions in this list.
   * Therefore it is better to get the shared Dimensions directly from the Groups.
   * 
   * @deprecated use ncfile.getRootGroup().getDimensions() for files without nested groups,
   *             or recurse through nested groups to get dimensions.
   */
  @Deprecated
  public ImmutableList<Dimension> getDimensions() {
    return ImmutableList.copyOf(dimensions);
  }

  /** Get all of the variables in the file, in all groups. Alternatively, use groups. */
  public ImmutableList<Variable> getVariables() {
    return ImmutableList.copyOf(variables);
  }

  /**
   * Returns the set of global attributes associated with this file, which are the attributes associated
   * with the root group, or any subgroup. Alternatively, use groups.
   */
  public ImmutableList<Attribute> getGlobalAttributes() {
    return ImmutableList.copyOf(gattributes);
  }

  /**
   * Look up an Attribute by (short) name in the root Group or nested Groups, exact match.
   *
   * @param attName the name of the attribute
   * @return the first Group attribute with given name, or null if not found
   */
  @Nullable
  public Attribute findGlobalAttribute(String attName) {
    for (Attribute a : gattributes) {
      if (attName.equals(a.getShortName()))
        return a;
    }
    return null;
  }

  /**
   * Look up an Attribute by (short) name in the root Group or nested Groups, ignore case.
   *
   * @param name the name of the attribute
   * @return the first group attribute with given Attribute name, ignoronmg case, or null if not found
   */
  @Nullable
  public Attribute findGlobalAttributeIgnoreCase(String name) {
    for (Attribute a : gattributes) {
      if (name.equalsIgnoreCase(a.getShortName()))
        return a;
    }
    return null;
  }

  /**
   * Find an attribute, with the specified (escaped full) name.
   * It may possibly be nested in multiple groups and/or structures.
   * An embedded "." is interpreted as structure.member.
   * An embedded "/" is interpreted as group/group or group/variable.
   * An embedded "@" is interpreted as variable@attribute
   * If the name actually has a ".", you must escape it (call NetcdfFiles.makeValidPathName(varname))
   * Any other chars may also be escaped, as they are removed before testing.
   *
   * @param fullNameEscaped eg "@attName", "/group/subgroup/@attName" or "/group/subgroup/varname.name2.name@attName"
   * @return Attribute or null if not found.
   */
  @Nullable
  public Attribute findAttribute(String fullNameEscaped) {
    if (fullNameEscaped == null || fullNameEscaped.isEmpty()) {
      return null;
    }

    int posAtt = fullNameEscaped.indexOf('@');
    if (posAtt < 0 || posAtt >= fullNameEscaped.length() - 1)
      return null;
    if (posAtt == 0) {
      return findGlobalAttribute(fullNameEscaped.substring(1));
    }

    String path = fullNameEscaped.substring(0, posAtt);
    String attName = fullNameEscaped.substring(posAtt + 1);

    // find the group
    Group g = rootGroup;
    int pos = path.lastIndexOf('/');
    String varName = (pos > 0 && pos < path.length() - 1) ? path.substring(pos + 1) : null;
    if (pos >= 0) {
      String groups = path.substring(0, pos);
      StringTokenizer stoke = new StringTokenizer(groups, "/");
      while (stoke.hasMoreTokens()) {
        String token = NetcdfFiles.makeNameUnescaped(stoke.nextToken());
        g = g.findGroupLocal(token);
        if (g == null)
          return null;
      }
    }
    if (varName == null) // group attribute
      return g.findAttribute(attName);

    // heres var.var - tokenize respecting the possible escaped '.'
    List<String> snames = EscapeStrings.tokenizeEscapedName(varName);
    if (snames.isEmpty())
      return null;

    String varShortName = NetcdfFiles.makeNameUnescaped(snames.get(0));
    Variable v = g.findVariableLocal(varShortName);
    if (v == null)
      return null;

    int memberCount = 1;
    while (memberCount < snames.size()) {
      if (!(v instanceof Structure))
        return null;
      String name = NetcdfFiles.makeNameUnescaped(snames.get(memberCount++));
      v = ((Structure) v).findVariable(name);
      if (v == null)
        return null;
    }

    return v.findAttribute(attName);
  }

  /**
   * Find a String-valued global or variable Attribute by
   * Attribute name (ignore case), return the Value of the Attribute.
   * If not found return defaultValue
   *
   * @param v the variable or null to look in the root group.
   * @param attName the (full) name of the attribute, case insensitive
   * @param defaultValue return this if attribute not found
   * @return the attribute value, or defaultValue if not found
   * @deprecated use getRootGroup() or Variable attributes().findAttributeString().
   */
  @Deprecated
  public String findAttValueIgnoreCase(Variable v, String attName, String defaultValue) {
    if (v == null)
      return rootGroup.attributes().findAttributeString(attName, defaultValue);
    else
      return v.attributes().findAttributeString(attName, defaultValue);
  }

  /** @deprecated use use getRootGroup() or Variable attributes().findAttributeDouble */
  @Deprecated
  public double readAttributeDouble(Variable v, String attName, double defValue) {
    Attribute att;

    if (v == null)
      att = rootGroup.findAttributeIgnoreCase(attName);
    else
      att = v.findAttributeIgnoreCase(attName);

    if (att == null)
      return defValue;
    if (att.isString())
      return Double.parseDouble(att.getStringValue());
    else
      return att.getNumericValue().doubleValue();
  }

  /** @deprecated use use getRootGroup() or Variable attributes().findAttributeInteger */
  @Deprecated
  public int readAttributeInteger(Variable v, String attName, int defValue) {
    Attribute att;

    if (v == null)
      att = rootGroup.findAttributeIgnoreCase(attName);
    else
      att = v.findAttributeIgnoreCase(attName);

    if (att == null)
      return defValue;
    if (att.isString())
      return Integer.parseInt(att.getStringValue());
    else
      return att.getNumericValue().intValue();
  }

  //////////////////////////////////////////////////////////////////////////////////////

  /** CDL representation of Netcdf header info, non strict */
  @Override
  public String toString() {
    Formatter f = new Formatter();
    writeCDL(f, new Indent(2), false);
    return f.toString();
  }

  /** NcML representation of Netcdf header info, non strict */
  public String toNcml(String url) {
    NcmlWriter ncmlWriter = new NcmlWriter(null, null, NcmlWriter.writeNoVariablesPredicate);
    Element netcdfElement = ncmlWriter.makeNetcdfElement(this, url);
    return ncmlWriter.writeToString(netcdfElement);
  }

  /**
   * Write the NcML representation: dont show coordinate values
   *
   * @param os : write to this OutputStream. Will be closed at end of the method.
   * @param uri use this for the url attribute; if null use getLocation(). // ??
   * @throws IOException if error
   */
  public void writeNcml(OutputStream os, String uri) throws IOException {
    NcmlWriter ncmlWriter = new NcmlWriter();
    Element netcdfElem = ncmlWriter.makeNetcdfElement(this, uri);
    ncmlWriter.writeToStream(netcdfElem, os);
  }

  /**
   * Write the NcML representation: dont show coordinate values
   *
   * @param writer : write to this Writer, should have encoding of UTF-8. Will be closed at end of the
   *        method.
   * @param uri use this for the url attribute; if null use getLocation().
   * @throws IOException if error
   */
  public void writeNcml(Writer writer, String uri) throws IOException {
    NcmlWriter ncmlWriter = new NcmlWriter();
    Element netcdfElem = ncmlWriter.makeNetcdfElement(this, uri);
    ncmlWriter.writeToWriter(netcdfElem, writer);
  }

  ///////////////////////////////////////////////////////////////////
  // old stuff for backwards compatilibilty, esp with NCdumpW
  // TODO: Move to helper class.

  /**
   * Write CDL representation to OutputStream.
   *
   * @param out write to this OutputStream
   * @param strict if true, make it stricly CDL, otherwise, add a little extra info
   * @deprecated use CDLWriter
   */
  @Deprecated
  public void writeCDL(OutputStream out, boolean strict) {
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    toStringStart(pw, strict);
    toStringEnd(pw);
    pw.flush();
  }

  /**
   * Write CDL representation to PrintWriter.
   *
   * @param pw write to this PrintWriter
   * @param strict if true, make it stricly CDL, otherwise, add a little extra info
   * @deprecated use CDLWriter
   */
  @Deprecated
  public void writeCDL(PrintWriter pw, boolean strict) {
    toStringStart(pw, strict);
    toStringEnd(pw);
    pw.flush();
  }

  /** @deprecated do not use */
  @Deprecated
  void toStringStart(PrintWriter pw, boolean strict) {
    Formatter f = new Formatter();
    toStringStart(f, new Indent(2), strict);
    pw.write(f.toString());
  }

  /** @deprecated do not use */
  @Deprecated
  void toStringEnd(PrintWriter pw) {
    pw.print("}\n");
  }

  //////////////////////////////////////////////////////////
  // the actual work is here

  protected void writeCDL(Formatter f, Indent indent, boolean strict) {
    toStringStart(f, indent, strict);
    f.format("%s}%n", indent);
  }

  private void toStringStart(Formatter f, Indent indent, boolean strict) {
    String name = getLocation();
    if (strict) {
      if (name.endsWith(".nc"))
        name = name.substring(0, name.length() - 3);
      if (name.endsWith(".cdl"))
        name = name.substring(0, name.length() - 4);
      name = NetcdfFiles.makeValidCDLName(name);
    }
    f.format("%snetcdf %s {%n", indent, name);
    indent.incr();
    rootGroup.writeCDL(f, indent, strict);
    indent.decr();
  }

  /**
   * Extend the file if needed, in a way that is compatible with the current metadata, that is,
   * does not invalidate structural metadata held by the application.
   * For example, ok if dimension lengths, data has changed.
   * All previous object references (variables, dimensions, etc) remain valid.
   *
   * @return true if file was extended.
   * @throws IOException if error
   * @deprecated do not use
   */
  @Deprecated
  public boolean syncExtend() throws IOException {
    // unlocked = false;
    return (iosp != null) && iosp.syncExtend();
  }

  /** @deprecated */
  @Deprecated
  @Override
  public long getLastModified() {
    if (iosp != null && iosp instanceof AbstractIOServiceProvider) {
      AbstractIOServiceProvider aspi = (AbstractIOServiceProvider) iosp;
      return aspi.getLastModified();
    }
    return 0;
  }

  /**
   * Open an existing netcdf file, passing in the iosp and the raf.
   * Use NetcdfFileSubclass to access this constructor
   *
   * @param spi use this IOServiceProvider instance
   * @param raf read from this RandomAccessFile
   * @param cancelTask allow user to cancel
   * @param location location of data
   */
  NetcdfFile(IOServiceProvider spi, RandomAccessFile raf, String location, CancelTask cancelTask) throws IOException {

    this.iosp = spi;
    this.location = location;

    if (debugSPI)
      log.info("NetcdfFile uses iosp = {}", spi.getClass().getName());

    try {
      spi.open(raf, this, cancelTask);

    } catch (IOException | RuntimeException e) {
      try {
        spi.close();
      } catch (Throwable t1) {
      }
      try {
        raf.close();
      } catch (Throwable t2) {
      }
      this.iosp = null;
      throw e;

    } catch (Throwable t) {
      try {
        spi.close();
      } catch (Throwable t1) {
      }
      try {
        raf.close();
      } catch (Throwable t2) {
      }
      this.iosp = null;
      throw new RuntimeException(t);
    }

    if (id == null)
      setId(rootGroup.findAttributeString("_Id", null));
    if (title == null)
      setTitle(rootGroup.findAttributeString("_Title", null));

    finish();
  }

  /**
   * Open an existing netcdf file (read only) , but dont do nuttin else
   * Use NetcdfFileSubclass to access this constructor
   *
   * @param spi use this IOServiceProvider instance
   * @param location location of data
   * @deprecated use NetcdfFile.builder()
   */
  @Deprecated
  NetcdfFile(IOServiceProvider spi, String location) {
    this.iosp = spi;
    this.location = location;
  }

  /**
   * For subclass construction.
   * Use NetcdfFileSubclass to access this constructor
   *
   * @deprecated use NetcdfFile.builder()
   */
  @Deprecated
  protected NetcdfFile() {}

  /**
   * Copy constructor, used by NetcdfDataset.
   * Shares the iosp.
   *
   * @param ncfile copy from here
   * @deprecated use NetcdfFile.builder()
   */
  @Deprecated
  protected NetcdfFile(NetcdfFile ncfile) {
    this.location = ncfile.getLocation();
    this.id = ncfile.getId();
    this.title = ncfile.getTitle();
    this.iosp = ncfile.iosp;
  }

  /**
   * Add an attribute to a group.
   *
   * @param parent add to this group. If group is null, use root group
   * @param att add this attribute
   * @return the attribute that was added
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public Attribute addAttribute(Group parent, Attribute att) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (parent == null)
      parent = rootGroup;
    parent.addAttribute(att);
    return att;
  }

  /**
   * Add optional String attribute to a group.
   *
   * @param parent add to this group. If group is null, use root group
   * @param name attribute name, may not be null
   * @param value attribute value, may be null, in which case, do not addd
   * @return the attribute that was added
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public Attribute addAttribute(Group parent, String name, String value) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (value == null)
      return null;
    if (parent == null)
      parent = rootGroup;
    Attribute att = new Attribute(name, value);
    parent.addAttribute(att);
    return att;
  }

  /**
   * Add a group to the parent group.
   *
   * @param parent add to this group. If group is null, use root group
   * @param g add this group
   * @return the group that was added
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public Group addGroup(Group parent, Group g) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (parent == null)
      parent = rootGroup;
    parent.addGroup(g);
    return g;
  }

  /**
   * Public by accident.
   *
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public void setRootGroup(Group rootGroup) {
    this.rootGroup = rootGroup;
  }

  /**
   * Add a shared Dimension to a Group.
   *
   * @param parent add to this group. If group is null, use root group
   * @param d add this Dimension
   * @return the dimension that was added
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public Dimension addDimension(Group parent, Dimension d) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (parent == null)
      parent = rootGroup;
    parent.addDimension(d);
    return d;
  }

  /**
   * Remove a shared Dimension from a Group by name.
   *
   * @param g remove from this group. If group is null, use root group
   * @param dimName name of Dimension to remove.
   * @return true if found and removed.
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public boolean removeDimension(Group g, String dimName) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (g == null)
      g = rootGroup;
    return g.removeDimension(dimName);
  }

  /**
   * Add a Variable to the given group.
   *
   * @param g add to this group. If group is null, use root group
   * @param v add this Variable
   * @return the variable that was added
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public Variable addVariable(Group g, Variable v) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (g == null)
      g = rootGroup;
    if (v != null)
      g.addVariable(v);
    return v;
  }

  /**
   * Create a new Variable, and add to the given group.
   *
   * @param g add to this group. If group is null, use root group
   * @param shortName short name of the Variable
   * @param dtype data type of the Variable
   * @param dims list of dimension names
   * @return the new Variable
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public Variable addVariable(Group g, String shortName, DataType dtype, String dims) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (g == null)
      g = rootGroup;
    Variable v = new Variable(this, g, null, shortName);
    v.setDataType(dtype);
    v.setDimensions(dims);
    g.addVariable(v);
    return v;
  }

  /**
   * Create a new Variable of type Datatype.CHAR, and add to the given group.
   *
   * @param g add to this group. If group is null, use root group
   * @param shortName short name of the Variable
   * @param dims list of dimension names
   * @param strlen dimension length of the inner (fastest changing) dimension
   * @return the new Variable
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public Variable addStringVariable(Group g, String shortName, String dims, int strlen) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (g == null)
      g = rootGroup;
    String dimName = shortName + "_strlen";
    addDimension(g, new Dimension(dimName, strlen));
    Variable v = new Variable(this, g, null, shortName);
    v.setDataType(DataType.CHAR);
    v.setDimensions(dims + " " + dimName);
    g.addVariable(v);
    return v;
  }

  /**
   * Remove a Variable from the given group by name.
   *
   * @param g remove from this group. If group is null, use root group
   * @param varName name of variable to remove.
   * @return true is variable found and removed
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public boolean removeVariable(Group g, String varName) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    if (g == null)
      g = rootGroup;
    return g.removeVariable(varName);
  }

  /**
   * Add a variable attribute.
   *
   * @param v add to this Variable.
   * @param att add this attribute
   * @return the added Attribute
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public Attribute addVariableAttribute(Variable v, Attribute att) {
    return v.addAttribute(att);
  }

  /**
   * Generic way to send a "message" to the underlying IOSP.
   * This message is sent after the file is open. To affect the creation of the file,
   * use a factory method like NetcdfFile.open().
   * In ver6, IOSP_MESSAGE_ADD_RECORD_STRUCTURE, IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE will not work here.
   *
   * @param message iosp specific message
   * @return iosp specific return, may be null
   */
  public Object sendIospMessage(Object message) {
    if (null == message)
      return null;

    if (message == IOSP_MESSAGE_ADD_RECORD_STRUCTURE) {
      Variable v = rootGroup.findVariableLocal("record");
      boolean gotit = (v instanceof Structure);
      return gotit || makeRecordStructure();

    } else if (message == IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE) {
      Variable v = rootGroup.findVariableLocal("record");
      boolean gotit = (v instanceof Structure);
      if (gotit) {
        rootGroup.remove(v);
        variables.remove(v);
        removeRecordStructure();
      }
      return (gotit);
    }

    if (iosp != null)
      return iosp.sendIospMessage(message);
    return null;
  }

  /**
   * If there is an unlimited dimension, make all variables that use it into a Structure.
   * A Variable called "record" is added.
   * You can then access these through the record structure.
   *
   * @return true if it has a Nectdf-3 record structure
   */
  @Deprecated
  protected boolean makeRecordStructure() {
    if (immutable)
      throw new IllegalStateException("Cant modify");

    Boolean didit = false;
    if ((iosp != null) && (iosp instanceof N3iospNew) && hasUnlimitedDimension()) {
      didit = (Boolean) iosp.sendIospMessage(IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    }
    return (didit != null) && didit;
  }

  @Deprecated
  protected boolean removeRecordStructure() {
    if (immutable)
      throw new IllegalStateException("Cant modify");

    Boolean didit = false;
    if ((iosp != null) && (iosp instanceof N3iospNew) && hasUnlimitedDimension()) {
      didit = (Boolean) iosp.sendIospMessage(IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE);
    }
    return (didit != null) && didit;
  }

  /**
   * Set the globally unique dataset identifier.
   *
   * @param id the id
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public void setId(String id) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    this.id = id;
  }

  /**
   * Set the dataset "human readable" title.
   *
   * @param title the title
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public void setTitle(String title) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    this.title = title;
  }

  /**
   * Set the location, a URL or local filename.
   *
   * @param location the location
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public void setLocation(String location) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    this.location = location;
  }

  /**
   * Make this immutable.
   *
   * @return this
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public NetcdfFile setImmutable() {
    if (immutable)
      return this;
    immutable = true;
    setImmutable(rootGroup);
    variables = Collections.unmodifiableList(variables);
    dimensions = Collections.unmodifiableList(dimensions);
    gattributes = Collections.unmodifiableList(gattributes);
    return this;
  }

  private void setImmutable(Group g) {
    for (Group nested : g.getGroups())
      setImmutable(nested);
  }

  /**
   * Completely empty the objects in the netcdf file.
   * Used for rereading the file on a sync().
   *
   * @deprecated
   */
  @Deprecated
  public void empty() {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    variables = new ArrayList<>();
    gattributes = new ArrayList<>();
    dimensions = new ArrayList<>();
    rootGroup = makeRootGroup();
    // addedRecordStructure = false;
  }

  private Group makeRootGroup() {
    return Group.builder().setNcfile(this).setName("").build();
  }

  /**
   * Finish constructing the object model.
   * This construsts the "global" variables, attributes and dimensions.
   * It also looks for coordinate variables.
   *
   * @deprecated Use NetcdfFile.builder()
   */
  @Deprecated
  public void finish() {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    variables = new ArrayList<>();
    dimensions = new ArrayList<>();
    gattributes = new ArrayList<>();
    finishGroup(rootGroup);
  }

  private void finishGroup(Group g) {
    variables.addAll(g.variables);

    // LOOK should group atts be promoted to global atts?
    for (Attribute oldAtt : g.attributes()) {
      if (g == rootGroup) {
        gattributes.add(oldAtt);
      } else {
        String newName = NetcdfFiles.makeFullNameWithString(g, oldAtt.getShortName()); // LOOK fishy
        gattributes.add(oldAtt.toBuilder().setName(newName).build());
      }
    }

    // LOOK this wont match the variables' dimensions if there are groups: what happens if we remove this ??
    for (Dimension oldDim : g.dimensions) {
      if (oldDim.isShared()) {
        if (g == rootGroup) {
          dimensions.add(oldDim);
        } else {
          String newName = NetcdfFiles.makeFullNameWithString(g, oldDim.getShortName()); // LOOK fishy
          dimensions.add(oldDim.toBuilder().setName(newName).build());

        }
      }
    }

    List<Group> groups = g.getGroups();
    for (Group nested : groups) {
      finishGroup(nested);
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////
  // Service Provider calls
  // All IO eventually goes through these calls.
  // LOOK: these should not be public !!! not hitting variable cache
  // used in NetcdfDataset - try to refactor

  // this is for reading non-member variables
  // section is null for full read

  /**
   * Do not call this directly, use Variable.read() !!
   * Ranges must be filled (no nulls)
   */
  protected Array readData(Variable v, Section ranges) throws IOException, InvalidRangeException {
    long start = 0;
    if (showRequest) {
      log.info("Data request for variable: {} section {}...", v.getFullName(), ranges);
      start = System.currentTimeMillis();
    }

    /*
     * if (unlocked) {
     * String info = cache.getInfo(this);
     * throw new IllegalStateException("File is unlocked - cannot use\n" + info);
     * }
     */

    if (iosp == null) {
      throw new IOException("iosp is null, perhaps file has been closed. Trying to read variable " + v.getFullName());
    }
    Array result = iosp.readData(v, ranges);

    if (showRequest) {
      long took = System.currentTimeMillis() - start;
      log.info(" ...took= {} msecs", took);
    }
    return result;
  }

  /**
   * Read a variable using the given section specification.
   * The result is always an array of the type of the innermost variable.
   * Its shape is the accumulation of all the shapes of its parent structures.
   *
   * @param variableSection the constraint expression.
   * @return data requested
   * @throws IOException if error
   * @throws InvalidRangeException if variableSection is invalid
   * @see <a href=
   *      "https://www.unidata.ucar.edu/software/netcdf-java/reference/SectionSpecification.html">SectionSpecification</a>
   */
  public Array readSection(String variableSection) throws IOException, InvalidRangeException {
    /*
     * if (unlocked)
     * throw new IllegalStateException("File is unlocked - cannot use");
     */

    ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(this, variableSection);
    if (cer.child == null) {
      return cer.v.read(cer.section);
    }

    if (iosp == null)
      return IospHelper.readSection(cer);
    else
      // allow iosp to optimize
      return iosp.readSection(cer);
  }


  /**
   * Read data from a top level Variable and send data to a WritableByteChannel. Experimental.
   *
   * @param v a top-level Variable
   * @param section the section of data to read.
   *        There must be a Range for each Dimension in the variable, in order.
   *        Note: no nulls allowed. IOSP may not modify.
   * @param wbc write data to this WritableByteChannel
   * @return the number of bytes written to the channel
   * @throws IOException if read error
   * @throws InvalidRangeException if invalid section
   * @deprecated do not use
   */
  @Deprecated
  protected long readToByteChannel(Variable v, Section section, WritableByteChannel wbc)
      throws IOException, InvalidRangeException {

    // if (unlocked)
    // throw new IllegalStateException("File is unlocked - cannot use");

    if ((iosp == null) || v.hasCachedData())
      return IospHelper.copyToByteChannel(v.read(section), wbc);

    return iosp.readToByteChannel(v, section, wbc);
  }

  protected long readToOutputStream(Variable v, Section section, OutputStream out)
      throws IOException, InvalidRangeException {

    // if (unlocked)
    // throw new IllegalStateException("File is unlocked - cannot use");

    if ((iosp == null) || v.hasCachedData())
      return IospHelper.copyToOutputStream(v.read(section), out);

    return iosp.readToOutputStream(v, section, out);
  }

  protected StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws IOException {
    return iosp.getStructureIterator(s, bufferSize);
  }

  ///////////////////////////////////////////////////////////////////////////////////

  // public I/O

  /**
   * Do a bulk read on a list of Variables and
   * return a corresponding list of Array that contains the results
   * of a full read on each Variable.
   * This is mostly here so DODSNetcdf can override it with one call to the server.
   *
   * @param variables List of type Variable
   * @return List of Array, one for each Variable in the input.
   * @throws IOException if read error
   * @deprecated will be moved to DODSNetcdfFile in version 6.
   */
  @Deprecated
  public List<Array> readArrays(List<Variable> variables) throws IOException {
    List<Array> result = new ArrayList<>();
    for (Variable variable : variables)
      result.add(variable.read());
    return result;
  }

  /**
   * Read a variable using the given section specification.
   *
   * @param variableSection the constraint expression.
   * @param flatten MUST BE TRUE
   * @return Array data read.
   * @throws IOException if error
   * @throws InvalidRangeException if variableSection is invalid
   * @see <a href=
   *      "https://www.unidata.ucar.edu/software/netcdf-java/reference/SectionSpecification.html">SectionSpecification</a>
   * @deprecated use readSection(), flatten=false no longer supported
   */
  @Deprecated
  public Array read(String variableSection, boolean flatten) throws IOException, InvalidRangeException {
    if (!flatten)
      throw new UnsupportedOperationException("NetdfFile.read(String variableSection, boolean flatten=false)");
    return readSection(variableSection);
  }

  /**
   * Access to iosp debugging info.
   *
   * @param o must be a Variable, Dimension, Attribute, or Group
   * @return debug info for this object.
   */
  protected String toStringDebug(Object o) {
    return (iosp == null) ? "" : iosp.toStringDebug(o);
  }

  /**
   * Access to iosp debugging info.
   *
   * @return debug / underlying implementation details
   * @deprecated do not use
   */
  @Deprecated
  public String getDetailInfo() {
    Formatter f = new Formatter();
    getDetailInfo(f);
    return f.toString();
  }

  /** @deprecated do not use */
  @Deprecated
  public void getDetailInfo(Formatter f) {
    f.format("NetcdfFile location= %s%n", getLocation());
    f.format("  title= %s%n", getTitle());
    f.format("  id= %s%n", getId());
    f.format("  fileType= %s%n", getFileTypeId());
    f.format("  fileDesc= %s%n", getFileTypeDescription());
    f.format("  fileVersion= %s%n", getFileTypeVersion());

    f.format("  class= %s%n", getClass().getName());
    if (iosp == null) {
      f.format("  has no IOSP%n");
    } else {
      f.format("  iosp= %s%n%n", iosp.getClass());
      f.format("%s", iosp.getDetailInfo());
    }
    showCached(f);
    showProxies(f);
  }

  /** @deprecated do not use */
  @Deprecated
  protected void showCached(Formatter f) {
    int maxNameLen = 8;
    for (Variable v : getVariables()) {
      maxNameLen = Math.max(maxNameLen, v.getShortName().length());
    }

    long total = 0;
    long totalCached = 0;
    f.format("%n%-" + maxNameLen + "s isCaching  size     cachedSize (bytes) %n", "Variable");
    for (Variable v : getVariables()) {
      long vtotal = v.getSize() * v.getElementSize();
      total += vtotal;
      f.format(" %-" + maxNameLen + "s %5s %8d ", v.getShortName(), v.isCaching(), vtotal);
      if (v.hasCachedData()) {
        Array data;
        try {
          data = v.read();
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
        long size = data.getSizeBytes();
        f.format(" %8d", size);
        totalCached += size;
      }
      f.format("%n");
    }
    f.format(" %" + maxNameLen + "s                  --------%n", " ");
    f.format(" %" + maxNameLen + "s total %8d Mb cached= %8d Kb%n", " ", total / 1000 / 1000, totalCached / 1000);
  }

  protected void showProxies(Formatter f) {
    int maxNameLen = 8;
    boolean hasProxy = false;
    for (Variable v : getVariables()) {
      if (v.proxyReader != v)
        hasProxy = true;
      maxNameLen = Math.max(maxNameLen, v.getShortName().length());
    }
    if (!hasProxy)
      return;

    f.format("%n%-" + maxNameLen + "s  proxyReader   Variable.Class %n", "Variable");
    for (Variable v : getVariables()) {
      if (v.proxyReader != v)
        f.format(" %-" + maxNameLen + "s  %s %s%n", v.getShortName(), v.proxyReader.getClass().getName(),
            v.getClass().getName());
    }
    f.format("%n");
  }

  /**
   * @deprecated do not use.
   */
  @Deprecated
  public IOServiceProvider getIosp() {
    return iosp;
  }

  /**
   * Get the file type id for the underlying data source.
   *
   * @return registered id of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  public String getFileTypeId() {
    if (iosp != null)
      return iosp.getFileTypeId();
    return "N/A";
  }

  /**
   * Get a human-readable description for this file type.
   *
   * @return description of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  public String getFileTypeDescription() {
    if (iosp != null)
      return iosp.getFileTypeDescription();
    return "N/A";
  }


  /**
   * Get the version of this file type.
   *
   * @return version of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  public String getFileTypeVersion() {
    if (iosp != null)
      return iosp.getFileTypeVersion();
    return "N/A";
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  // TODO make these final and immutable in 6.
  protected String location, id, title;
  protected Group rootGroup = makeRootGroup();
  protected IOServiceProvider iosp;
  private boolean immutable;

  // LOOK can we get rid of internal caching?
  private String cacheName;
  protected FileCacheIF cache;

  // TODO get rid of these in ver 6, or make them private.
  // "global view" over all groups.
  @Deprecated
  protected List<Variable> variables;
  @Deprecated
  protected List<Dimension> dimensions;
  @Deprecated
  protected List<Attribute> gattributes;

  protected NetcdfFile(Builder<?> builder) {
    this.location = builder.location;
    this.id = builder.id;
    this.title = builder.title;
    this.iosp = builder.iosp;
    if (builder.rootGroup != null) {
      builder.rootGroup.setNcfile(this);
      this.rootGroup = builder.rootGroup.build();
    }
    if (builder.iosp != null) {
      builder.iosp.setNetcdfFile(this);
      this.iosp = builder.iosp;
    }
    finish(); // LOOK
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return b.setLocation(this.location).setId(this.id).setTitle(this.title).setRootGroup(this.rootGroup.toBuilder())
        .setIosp((AbstractIOServiceProvider) this.iosp);
  }

  /**
   * Get Builder for this class.
   * Allows subclassing.
   *
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  /** A builder of NetcdfFile objects. */
  public static abstract class Builder<T extends Builder<T>> {
    public Group.Builder rootGroup = Group.builder().setName("");
    private String id;
    private String title;
    public String location;
    protected AbstractIOServiceProvider iosp;
    private boolean built;

    protected abstract T self();

    public T setRootGroup(Group.Builder rootGroup) {
      this.rootGroup = rootGroup;
      return self();
    }

    public T setIosp(AbstractIOServiceProvider iosp) {
      this.iosp = iosp;
      return self();
    }

    public T setId(String id) {
      this.id = id;
      return self();
    }

    /** Set the dataset "human readable" title. */
    public T setTitle(String title) {
      this.title = title;
      return self();
    }

    /** Set the location, a URL or local filename. */
    public T setLocation(String location) {
      this.location = location;
      return self();
    }

    public NetcdfFile build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new NetcdfFile(this);
    }
  }

}
