/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.jdom2.Element;
import ucar.array.StructureData;
import ucar.nc2.internal.iosp.netcdf3.N3header;
import ucar.nc2.internal.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.DebugFlags;
import ucar.nc2.internal.util.EscapeStrings;
import ucar.nc2.util.Indent;
import ucar.nc2.internal.cache.FileCacheIF;
import ucar.nc2.internal.cache.FileCacheable;
import ucar.nc2.write.NcmlWriter;

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
@Immutable
public class NetcdfFile implements FileCacheable, Closeable {

  @Deprecated
  public static final String IOSP_MESSAGE_ADD_RECORD_STRUCTURE = "AddRecordStructure";
  public static final String IOSP_MESSAGE_RANDOM_ACCESS_FILE = "RandomAccessFile";
  public static final String IOSP_MESSAGE_GET_IOSP = "IOSP";
  public static final String IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT = "NetcdfFileFormat";

  static boolean debugSPI, debugCompress;
  static boolean debugStructureIterator;
  private static boolean showRequest;

  /**
   * @deprecated do not use
   */
  @Deprecated
  public static void setDebugFlags(DebugFlags debugFlag) {
    debugSPI = debugFlag.isSet("NetcdfFile/debugSPI");
    debugCompress = debugFlag.isSet("NetcdfFile/debugCompress");
    debugStructureIterator = debugFlag.isSet("NetcdfFile/structureIterator");
    N3header.disallowFileTruncation = debugFlag.isSet("NetcdfFile/disallowFileTruncation");
    N3header.debugHeaderSize = debugFlag.isSet("NetcdfFile/debugHeaderSize");
    showRequest = debugFlag.isSet("NetcdfFile/showRequest");
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Close all resources (files, sockets, etc) associated with this file. If the underlying file was acquired, it will
   * be released,
   * otherwise closed. if isClosed() already, nothing will happen
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

  //////////////////////////////////////////////////////////////////////////////////////

  /**
   * Find an attribute, with the specified (escaped full) name. It may be nested in multiple groups and/or structures.
   * An embedded "." is interpreted as structure.member.
   * An embedded "/" is interpreted as group/group or group/variable.
   * An embedded "@" is interpreted as variable@attribute.
   * A name without an "@" is interpreted as an attribute in the root group.
   * If the name actually has a ".", you must escape it (call NetcdfFiles.makeValidPathName(varname)).
   * Any other chars may also be escaped, as they are removed before testing.
   *
   * @param fullNameEscaped eg "attName", "@attName", "var@attname", "struct.member.@attName",
   *        "/group/subgroup/@attName", "/group/subgroup/var@attName", or "/group/subgroup/struct.member@attName"
   * @return Attribute or null if not found.
   */
  @Nullable
  public Attribute findAttribute(String fullNameEscaped) {
    if (Strings.isNullOrEmpty(fullNameEscaped)) {
      return null;
    }

    int posAtt = fullNameEscaped.indexOf('@');
    if (posAtt < 0) {
      return rootGroup.findAttribute(fullNameEscaped);
    }
    if (posAtt == 0) {
      return rootGroup.findAttribute(fullNameEscaped.substring(1));
    }
    if (posAtt == fullNameEscaped.length() - 1) {
      return null;
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
        if (g == null) {
          return null;
        }
      }
    }
    if (varName == null) { // group attribute
      return g.findAttribute(attName);
    }

    // heres var.var - tokenize respecting the possible escaped '.'
    List<String> snames = EscapeStrings.tokenizeEscapedName(varName);
    if (snames.isEmpty()) {
      return null;
    }

    String varShortName = NetcdfFiles.makeNameUnescaped(snames.get(0));
    Variable v = g.findVariableLocal(varShortName);
    if (v == null) {
      return null;
    }

    int memberCount = 1;
    while (memberCount < snames.size()) {
      if (!(v instanceof Structure)) {
        return null;
      }
      String name = NetcdfFiles.makeNameUnescaped(snames.get(memberCount++));
      v = ((Structure) v).findVariable(name);
      if (v == null) {
        return null;
      }
    }

    return v.findAttribute(attName);
  }

  /**
   * Finds a Dimension with the specified full name. It may be nested in multiple groups. An embedded "/" is interpreted
   * as a group separator. A leading slash indicates the root group. That slash may be omitted, but the {@code fullName}
   * will be treated as if it were there. In other words, the first name token in {@code fullName} is treated as the
   * short name of a Group or Dimension, relative to the root group.
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
   * Find a Group, with the specified (full) name. A full name should start with a '/'. For backwards compatibility, we
   * accept full names
   * that omit the leading '/'. An embedded '/' separates subgroup names.
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

  /**
   * Find a Variable, with the specified (escaped full) name. It may possibly be nested in multiple groups and/or
   * structures. An embedded
   * "." is interpreted as structure.member. An embedded "/" is interpreted as group/variable. If the name actually has
   * a ".", you must
   * escape it (call NetcdfFiles.makeValidPathName(varname)) Any other chars may also be escaped, as they are removed
   * before testing.
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
   * Get all shared Dimensions used in this file.
   */
  public ImmutableList<Dimension> getDimensions() {
    return allDimensions;
  }

  /**
   * Get the file type id for the underlying data source.
   *
   * @return registered id of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  @Nullable
  public String getFileTypeId() {
    if (iosp != null)
      return iosp.getFileTypeId();
    return null;
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
   * Get the root group.
   *
   * @return root group
   */
  public Group getRootGroup() {
    return rootGroup;
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

  /** Get all of the variables in the file, in all groups. Alternatively, use groups. */
  public ImmutableList<Variable> getVariables() {
    return allVariables;
  }

  /**
   * Return the unlimited (record) dimension, or null if not exist.
   * If there are multiple unlimited dimensions, it will return the first one.
   *
   * @return the unlimited Dimension, or null if none.
   */
  @Nullable
  public Dimension getUnlimitedDimension() {
    for (Dimension d : allDimensions) {
      if (d.isUnlimited())
        return d;
    }
    return null;
  }

  /**
   * Return true if this file has one or more unlimited (record) dimension.
   *
   * @return if this file has an unlimited Dimension(s)
   */
  public boolean hasUnlimitedDimension() {
    return getUnlimitedDimension() != null;
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // Service Provider calls
  // All IO eventually goes through these calls.

  protected Iterator<StructureData> getSequenceIterator(Sequence s, int bufferSize) throws IOException {
    Preconditions.checkNotNull(iosp);
    return iosp.getSequenceIterator(s, bufferSize);
  }

  /**
   * Do not call this directly, use Variable.readArray() !!
   * Ranges must be filled (no nulls)
   */
  @Nullable
  protected ucar.array.Array<?> readArrayData(Variable v, ucar.array.Section ranges)
      throws IOException, ucar.array.InvalidRangeException {
    if (iosp == null) {
      throw new IOException("iosp is null, perhaps file has been closed. Trying to read variable " + v.getFullName());
    }
    return iosp.readArrayData(v, ranges);
  }

  /**
   * Read a variable using the given section specification.
   * The result is always an array of the type of the innermost variable.
   * Its shape is the accumulation of all the shapes of its parent structures.
   *
   * @param variableSection the constraint expression.
   * @see <a href=
   *      "https://www.unidata.ucar.edu/software/netcdf-java/reference/SectionSpecification.html">SectionSpecification</a>
   */
  public ucar.array.Array<?> readSectionArray(String variableSection)
      throws IOException, ucar.array.InvalidRangeException {
    ParsedArraySectionSpec cer = ParsedArraySectionSpec.parseVariableSection(this, variableSection);
    if (cer.getChild() == null) {
      return cer.getVariable().readArray(cer.getSection());
    }
    throw new UnsupportedOperationException();
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

    if (message == IOSP_MESSAGE_GET_IOSP) {
      return this.iosp;
    }

    if (message == IOSP_MESSAGE_ADD_RECORD_STRUCTURE) {
      Variable v = rootGroup.findVariableLocal("record");
      boolean gotit = (v instanceof Structure);
      return gotit || makeRecordStructure();
    }

    if (iosp != null) {
      return iosp.sendIospMessage(message);
    }
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
  private boolean makeRecordStructure() {
    Boolean didit = false;
    if ((iosp != null) && (iosp instanceof N3iosp) && hasUnlimitedDimension()) {
      didit = (Boolean) iosp.sendIospMessage(IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    }
    return (didit != null) && didit;
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

  void writeCDL(Formatter f, Indent indent, boolean strict) {
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

  ///////////////////////////////////////////////////////////////////////////////////
  // caching

  @Override
  public long getLastModified() {
    if (iosp != null && iosp instanceof AbstractIOServiceProvider) {
      AbstractIOServiceProvider aspi = (AbstractIOServiceProvider) iosp;
      return aspi.getLastModified();
    }
    return 0;
  }

  /**
   * Public by accident.
   * Release any resources like file handles
   */
  public void release() throws IOException {
    if (iosp != null) {
      iosp.release();
    }
  }

  /**
   * Public by accident.
   * Reacquire any resources like file handles
   */
  public void reacquire() throws IOException {
    if (iosp != null) {
      iosp.reacquire();
    }
  }

  /**
   * Public by accident.
   * Optional file caching.
   */
  public synchronized void setFileCache(FileCacheIF cache) {
    this.cache = cache;
  }

  /** Show debug / underlying implementation details */
  public String getDetailInfo() {
    Formatter f = new Formatter();
    getDetailInfo(f);
    return f.toString();
  }

  /** Show debug / underlying implementation details */
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
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final String location;
  private final String id;
  private final String title;
  private final Group rootGroup;

  @Nullable
  private IOServiceProvider iosp; // may be null when completely self contained, eg NcML

  // This allows cache management when closing the NetcdfFile object
  protected FileCacheIF cache;

  // "global view" over all groups.
  private final ImmutableList<Variable> allVariables;
  private final ImmutableList<Dimension> allDimensions;
  private final ImmutableList<Attribute> allAttributes;

  protected NetcdfFile(Builder<?> builder) {
    this.location = builder.location;
    this.id = builder.id;
    this.title = builder.title;

    if (builder.rootGroup != null) {
      builder.rootGroup.setNcfile(this);
      this.rootGroup = builder.rootGroup.build();
    } else {
      rootGroup = Group.builder().setNcfile(this).setName("").build();
    }
    if (builder.iosp != null) {
      this.iosp.buildFinish(this);
    }
    this.iosp = builder.iosp;

    // all global attributes, dimensions, variables
    ImmutableList.Builder<Attribute> alist = ImmutableList.builder();
    ImmutableList.Builder<Dimension> dlist = ImmutableList.builder();
    ImmutableList.Builder<Variable> vlist = ImmutableList.builder();
    extractAll(rootGroup, alist, dlist, vlist);
    allAttributes = alist.build();
    allDimensions = dlist.build();
    allVariables = vlist.build();
  }

  private void extractAll(Group group, ImmutableList.Builder<Attribute> alist, ImmutableList.Builder<Dimension> dlist,
      ImmutableList.Builder<Variable> vlist) {

    alist.addAll(group.attributes());
    group.getDimensions().stream().filter(Dimension::isShared).forEach(dlist::add);
    vlist.addAll(group.getVariables());

    for (Group nested : group.getGroups()) {
      extractAll(nested, alist, dlist, vlist);
    }
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

  public static abstract class Builder<T extends Builder<T>> {
    public Group.Builder rootGroup = Group.builder().setName("");
    private String id;
    private String title;
    public String location;
    protected AbstractIOServiceProvider iosp;
    private boolean built;

    protected abstract T self();

    public T setRootGroup(Group.Builder rootGroup) {
      Preconditions.checkArgument(rootGroup.shortName.equals(""), "root group name must be empty string");
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
