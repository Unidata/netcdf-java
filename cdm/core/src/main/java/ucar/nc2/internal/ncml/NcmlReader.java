/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.net.URL;
import javax.annotation.Nullable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import ucar.array.Arrays;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.SequenceDS;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.dataset.DatasetEnhancer;
import ucar.nc2.internal.util.AliasTranslator;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.IO;
import ucar.nc2.internal.util.URLnaming;
import ucar.unidata.util.StringUtil2;

/**
 * Read NcML and create NetcdfDataset.Builder, using builders and immutable objects.
 * <p>
 * This is an internal class, users should usually call {@link NetcdfDatasets#openDataset(String)}
 */
public class NcmlReader {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcmlReader.class);

  private static final Namespace ncNSHttp = thredds.client.catalog.Catalog.ncmlNS;
  private static final Namespace ncNSHttps = thredds.client.catalog.Catalog.ncmlNSHttps;

  private static boolean debugURL, debugXML, showParsedXML;
  private static boolean debugOpen, debugConstruct, debugCmd;
  private static boolean debugAggDetail;

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugURL = debugFlag.isSet("NcML/debugURL");
    debugXML = debugFlag.isSet("NcML/debugXML");
    showParsedXML = debugFlag.isSet("NcML/showParsedXML");
    debugCmd = debugFlag.isSet("NcML/debugCmd");
    debugOpen = debugFlag.isSet("NcML/debugOpen");
    debugConstruct = debugFlag.isSet("NcML/debugConstruct");
    debugAggDetail = debugFlag.isSet("NcML/debugAggDetail");
  }

  /**
   * Retrieve the set of Enhancements described by the enhanceMode NcML string.
   * 
   * @return the set corresponding to {@code enhanceMode}, or {@code null} if enhanceMode is null.
   */
  private static Set<Enhance> parseEnhanceMode(String enhanceMode) {
    if (enhanceMode == null) {
      return null;
    }

    switch (enhanceMode.toLowerCase()) {
      case "all":
        return NetcdfDataset.getEnhanceAll();
      case "none":
        return NetcdfDataset.getEnhanceNone();
      case "convertenums":
        return EnumSet.of(Enhance.ConvertEnums);
      case "convertunsigned":
        return EnumSet.of(Enhance.ConvertUnsigned);
      case "applyscaleoffset":
        return EnumSet.of(Enhance.ApplyScaleOffset);
      case "convertmissing":
        return EnumSet.of(Enhance.ConvertMissing);
      case "coordsystems":
        return EnumSet.of(Enhance.CoordSystems);
      case "incompletecoordsystems":
        return EnumSet.of(Enhance.CoordSystems, Enhance.IncompleteCoordSystems);

      // Legacy strings, retained for backwards compatibility:
      case "true":
        return NetcdfDataset.getEnhanceAll();
      case "scalemissingdefer":
        return NetcdfDataset.getEnhanceNone();
      case "alldefer":
        return EnumSet.of(Enhance.ConvertEnums, Enhance.CoordSystems);
      case "scalemissing":
        return EnumSet.of(Enhance.ConvertUnsigned, Enhance.ApplyScaleOffset, Enhance.ConvertMissing);
      // Return null by default, since some valid strings actually return an empty set.
      default:
        return null;
    }
  }

  /**
   * Use NCML to modify the dataset, getting NcML from a URL. Used by CoordSysFactory.
   *
   * @param ncDataset modify this dataset
   * @param ncmlLocation URL location of NcML
   * @param cancelTask allow user to cancel task; may be null
   * @throws IOException on read error
   */
  public static void wrapNcml(NetcdfDataset.Builder<?> ncDataset, String ncmlLocation, CancelTask cancelTask)
      throws IOException {
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      if (debugURL) {
        System.out.println(" NetcdfDataset URL = <" + ncmlLocation + ">");
      }
      doc = builder.build(ncmlLocation);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) {
      System.out.println(" SAXBuilder done");
    }

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element netcdfElem = doc.getRootElement();

    NcmlReader reader = new NcmlReader();
    reader.readNetcdf(ncmlLocation, ncDataset, netcdfElem, cancelTask);
    if (debugOpen) {
      System.out.println("***NcmlReader.wrapNcml result= \n" + ncDataset);
    }
  }

  /**
   * Use NCML to modify a dataset, getting the NcML document as a resource stream. Uses
   * ClassLoader.getResourceAsStream(ncmlResourceLocation),
   * so the NcML can be inside of a jar file, for example.
   *
   * @param ncDataset modify this dataset
   * @param ncmlResourceLocation resource location of NcML
   * @param cancelTask allow user to cancel task; may be null
   * @throws IOException on read error
   */
  public static void wrapNcmlResource(NetcdfDataset.Builder<?> ncDataset, String ncmlResourceLocation,
      CancelTask cancelTask) throws IOException {
    ClassLoader cl = ncDataset.getClass().getClassLoader();
    try (InputStream is = cl.getResourceAsStream(ncmlResourceLocation)) {
      if (is == null) {
        throw new FileNotFoundException(ncmlResourceLocation);
      }

      if (debugXML) {
        System.out.println(" NetcdfDataset URL = <" + ncmlResourceLocation + ">");
        try (InputStream is2 = cl.getResourceAsStream(ncmlResourceLocation)) {
          System.out.println(" contents=\n" + IO.readContents(is2));
        }
      }

      org.jdom2.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        if (debugURL) {
          System.out.println(" NetcdfDataset URL = <" + ncmlResourceLocation + ">");
        }
        doc = builder.build(is);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }
      if (debugXML) {
        System.out.println(" SAXBuilder done");
      }

      if (showParsedXML) {
        XMLOutputter xmlOut = new XMLOutputter();
        System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
      }

      Element netcdfElem = doc.getRootElement();

      NcmlReader reader = new NcmlReader();
      reader.readNetcdf(ncDataset.location, ncDataset, netcdfElem, cancelTask);
      if (debugOpen) {
        System.out.println("***NcmlReader.wrapNcml result= \n" + ncDataset);
      }
    }
  }

  /**
   * Use NCML to modify the referenced dataset, create a new dataset with the merged info Used to wrap each dataset of
   * an aggregation before its aggregated.
   *
   * @param ref referenced dataset
   * @param ncmlElem parent element - usually the aggregation element of the ncml
   * @return new dataset with the merged info
   */
  public static NetcdfDataset.Builder<?> mergeNcml(NetcdfFile ref, @Nullable Element ncmlElem) {
    NetcdfDataset.Builder<?> targetDS = new NetcdfDataset(ref.toBuilder()).toBuilder(); // no enhance

    if (ncmlElem != null) {
      NcmlReader reader = new NcmlReader();
      reader.readGroup(targetDS, null, null, ncmlElem);
    }

    return targetDS;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read NcML doc from a Reader, and construct a NetcdfDataset.Builder.
   * This is an internal method, users should use {@link NetcdfDatasets#openNcmlDataset(Reader, String, CancelTask)}
   *
   * @param r the Reader containing the NcML document
   * @param ncmlLocation the URL location string of the NcML document, used to resolve reletive path of the referenced
   *        dataset,
   *        or may be just a unique name for caching purposes.
   * @param cancelTask allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset.Builder
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  public static NetcdfDataset.Builder<?> readNcml(Reader r, String ncmlLocation, CancelTask cancelTask)
      throws IOException {
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(r);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML)
      System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }
    Element netcdfElem = doc.getRootElement();

    // the ncml probably refers to another dataset, but doesnt have to
    String referencedDatasetUri = netcdfElem.getAttributeValue("location");
    if (referencedDatasetUri == null)
      referencedDatasetUri = netcdfElem.getAttributeValue("url");
    if (referencedDatasetUri != null)
      referencedDatasetUri = AliasTranslator.translateAlias(referencedDatasetUri);

    NcmlReader reader = new NcmlReader();
    return reader.readNcml(ncmlLocation, referencedDatasetUri, netcdfElem, cancelTask);
  }

  /**
   * Read an NcML file from a URL location, and construct a NetcdfDataset.
   *
   * @param ncmlLocation the URL location string of the NcML document
   * @param referencedDatasetUri if null (usual case) get this from NcML, otherwise use URI as the location of the
   *        referenced dataset.
   * @param cancelTask allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  public static NetcdfDataset.Builder<?> readNcml(String ncmlLocation, String referencedDatasetUri,
      CancelTask cancelTask) throws IOException {
    URL url = new URL(ncmlLocation);

    if (debugURL) {
      System.out.println(" NcmlReader open " + ncmlLocation);
      System.out.println("   URL = " + url);
      System.out.println("   external form = " + url.toExternalForm());
      System.out.println("   protocol = " + url.getProtocol());
      System.out.println("   host = " + url.getHost());
      System.out.println("   path = " + url.getPath());
      System.out.println("  file = " + url.getFile());
    }

    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      if (debugURL) {
        System.out.println(" NetcdfDataset URL = <" + url + ">");
      }
      doc = builder.build(url);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) {
      System.out.println(" SAXBuilder done");
    }

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element netcdfElem = doc.getRootElement();

    if (referencedDatasetUri == null) {
      // the ncml probably refers to another dataset, but doesnt have to
      referencedDatasetUri = netcdfElem.getAttributeValue("location");
      if (referencedDatasetUri == null) {
        referencedDatasetUri = netcdfElem.getAttributeValue("url");
      }
    }
    if (referencedDatasetUri != null) {
      referencedDatasetUri = AliasTranslator.translateAlias(referencedDatasetUri);
    }

    NcmlReader reader = new NcmlReader();
    return reader.readNcml(ncmlLocation, referencedDatasetUri, netcdfElem, cancelTask);
  }

  //////////////////////////////////////////////////////////////////////////////////////
  private Namespace ncNS;
  private String location;
  private boolean explicit;
  private @Nullable NetcdfFile refFile; // the referenced dataset
  private final Formatter errlog = new Formatter();

  /**
   * This sets up the target dataset and the referenced dataset. only place that iospParam is processed, so everything
   * must go through here
   *
   * @param ncmlLocation the URL location string of the NcML document, used to resolve reletive path of the referenced
   *        dataset, or
   *        may be just a unique name for caching purposes.
   * @param referencedDatasetUri refers to this dataset (may be null)
   * @param netcdfElem JDOM netcdf element
   * @param cancelTask allow user to cancel the task; may be null
   * @return NetcdfDataset the constructed dataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  private NetcdfDataset.Builder<?> readNcml(String ncmlLocation, @Nullable String referencedDatasetUri,
      Element netcdfElem, @Nullable CancelTask cancelTask) throws IOException {

    // get ncml namespace and set namespace variable
    this.ncNS = ncNSHttp;
    if (netcdfElem.getNamespaceURI().startsWith("https")) {
      this.ncNS = ncNSHttps;
    }

    // augment URI.resolve(), by also dealing with base file: URIs
    referencedDatasetUri = URLnaming.resolve(ncmlLocation, referencedDatasetUri);

    // common error causing infinite regression
    if ((referencedDatasetUri != null) && referencedDatasetUri.equals(ncmlLocation)) {
      throw new IllegalArgumentException(
          "NcML location attribute refers to the NcML document itself" + referencedDatasetUri);
    }

    // they can specify the iosp to use - but must be file based
    String iospClassName = netcdfElem.getAttributeValue("iosp");
    Object iospParam = netcdfElem.getAttributeValue("iospParam");
    if (iospParam == null) {
      // can pass iosp a JDOM tree
      iospParam = netcdfElem.getChild("iospParam", ncNS); // LOOK namespace ??
    }

    String bufferSizeS = netcdfElem.getAttributeValue("buffer_size");
    int buffer_size = -1;
    if (bufferSizeS != null) {
      buffer_size = Integer.parseInt(bufferSizeS);
    }

    // Doesnt have to have a referenced dataset, Ncml can be self-contained.
    // If it exists, open the referenced dataset - do NOT use acquire, and dont enhance.
    if (referencedDatasetUri != null) {
      if (iospClassName != null) {
        try {
          this.refFile = NetcdfFiles.open(referencedDatasetUri, iospClassName, buffer_size, cancelTask, iospParam);
        } catch (Exception e) {
          throw new IOException(e);
        }
      } else {
        DatasetUrl durl = DatasetUrl.findDatasetUrl(referencedDatasetUri);
        this.refFile = NetcdfDatasets.openFile(durl, buffer_size, cancelTask, null);
      }
    }

    // explicit means all the metadata is specified in the XML, and the referenced dataset is used only for data access
    Element elemE = netcdfElem.getChild("explicit", ncNS);
    explicit = (elemE != null);

    NetcdfDataset.Builder<?> builder = NetcdfDataset.builder().setOrgFile(this.refFile);
    if (this.refFile != null && !explicit) {
      // copy all the metadata from the original file.
      builder.copyFrom(this.refFile);
    }

    // Read the Ncml into the builder
    readNetcdf(ncmlLocation, builder, netcdfElem, cancelTask);

    return builder;
  }

  ///////// Heres where the parsing work starts

  /**
   * parse a netcdf JDOM Element, and add contents to the targetDS NetcdfDataset.
   * <p/>
   * This is a bit tricky, because it handles several cases When targetDS == refds, we are just modifying targetDS. When
   * targetDS != refds,
   * we keep them seperate, and copy from refds to newds.
   * <p/>
   * The user may be defining new elements or modifying old ones. The only way to tell is by seeing if the elements
   * already exist.
   *
   * @param ncmlLocation NcML URL location, or may be just a unique name for caching purposes.
   * @param builder add the info to this one
   * @param netcdfElem JDOM netcdf element
   * @param cancelTask allow user to cancel the task; may be null
   * @throws IOException on read error
   */
  private void readNetcdf(String ncmlLocation, NetcdfDataset.Builder<?> builder, Element netcdfElem,
      @Nullable CancelTask cancelTask) throws IOException {
    this.location = ncmlLocation; // log messages need this

    // detect incorrect namespace
    Namespace use = netcdfElem.getNamespace();
    if (!use.equals(ncNSHttp) && !use.equals(ncNSHttps)) {
      String message = String.format("Namespace specified in NcML must be either '%s' or '%s', but was '%s'.",
          ncNSHttp.getURI(), ncNSHttps.getURI(), use.getURI());
      throw new IllegalArgumentException(message);
    }

    if (ncmlLocation != null) {
      builder.setLocation(ncmlLocation);
    }
    builder.setId(netcdfElem.getAttributeValue("id"));
    builder.setTitle(netcdfElem.getAttributeValue("title"));

    Element aggElem = netcdfElem.getChild("aggregation", ncNS);
    if (aggElem != null) {
      Aggregation agg = readAgg(aggElem, ncmlLocation, builder, cancelTask);
      builder.setAggregation(agg);
      agg.build(cancelTask);

      // LOOK seems like we should add the agg metadata here, so that it can be modified.
    }

    // read the root group and recurse
    readGroup(builder, null, null, netcdfElem);
    String errors = errlog.toString();
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("NcML had fatal errors:" + errors);
    }

    // enhance means do scale/offset and/or add CoordSystems
    Set<NetcdfDataset.Enhance> mode = parseEnhanceMode(netcdfElem.getAttributeValue("enhance"));
    if (mode != null) {
      // cant just set enhance mode
      if (DatasetEnhancer.enhanceNeeded(mode, null)) {
        DatasetEnhancer enhancer = new DatasetEnhancer(builder, mode, cancelTask);
        enhancer.enhance();
        builder.setEnhanceMode(mode);
      }
    }

    /*
     * LOOK optionally add record structure to netcdf-3
     * String addRecords = netcdfElem.getAttributeValue("addRecords");
     * if ("true".equalsIgnoreCase(addRecords))
     * targetDS.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
     */
  }

  /**
   * Read the NcML group element, and nested elements.
   *
   * @param parent the parent group builder, or null when its the root group.
   * @param refParent parent Group in referenced dataset, may be null
   * @param groupElem ncml group element
   */
  private Group.Builder readGroup(NetcdfDataset.Builder<?> builder, @Nullable Group.Builder parent,
      @Nullable Group refParent, Element groupElem) {
    Group.Builder groupBuilder;
    Group refGroup = null;

    if (parent == null) {
      refGroup = this.refFile == null ? null : this.refFile.getRootGroup();
      groupBuilder = builder.rootGroup;

    } else {
      String name = groupElem.getAttributeValue("name");
      if (name == null) {
        errlog.format("NcML Group name is required (%s)%n", groupElem);
        return null;
      }
      String nameInFile = groupElem.getAttributeValue("orgName");
      if (nameInFile == null) {
        nameInFile = name;
      }
      // see if it exists in referenced dataset
      if (refParent != null) {
        refGroup = refParent.findGroupLocal(nameInFile);
      }
      if (refGroup == null) { // new
        groupBuilder = Group.builder().setName(name);
        parent.addGroup(groupBuilder);
        if (debugConstruct) {
          System.out.println(" add new group = " + name);
        }

      } else { // exists in refGroup.
        if (explicit) {
          groupBuilder = Group.builder();
          parent.addGroup(groupBuilder);
        } else {
          String finalName = nameInFile;
          groupBuilder = parent.findGroupLocal(finalName)
              .orElseThrow(() -> new IllegalStateException("Cant find Group " + finalName));
        }
        groupBuilder.setName(name);
      }
    }

    // look for attributes
    AttributeContainer atts = refGroup == null ? null : refGroup.attributes();
    java.util.List<Element> attList = groupElem.getChildren("attribute", ncNS);
    for (Element attElem : attList) {
      readAtt(groupBuilder.getAttributeContainer(), atts, attElem);
    }

    // look for enumTypedef
    java.util.List<Element> etdList = groupElem.getChildren("enumTypedef", ncNS);
    for (Element elem : etdList) {
      readEnumTypedef(groupBuilder, elem);
    }

    // look for dimensions
    java.util.List<Element> dimList = groupElem.getChildren("dimension", ncNS);
    for (Element dimElem : dimList) {
      readDim(groupBuilder, refGroup, dimElem);
    }

    // look for variables
    java.util.List<Element> varList = groupElem.getChildren("variable", ncNS);
    for (Element varElem : varList) {
      readVariable(groupBuilder, refGroup, varElem);
    }

    // process remove command
    java.util.List<Element> removeList = groupElem.getChildren("remove", ncNS);
    for (Element e : removeList) {
      cmdRemove(groupBuilder, e.getAttributeValue("type"), e.getAttributeValue("name"));
    }

    // look for nested groups
    java.util.List<Element> groupList = groupElem.getChildren("group", ncNS);
    for (Element gElem : groupList) {
      readGroup(builder, groupBuilder, refGroup, gElem);
    }
    return groupBuilder;
  }

  /**
   * Read an NcML attribute element.
   *
   * @param dest Group or Variable attribute container
   * @param ref Group or Variable in reference dataset, may be null
   * @param attElem ncml attribute element
   */
  private void readAtt(AttributeContainerMutable dest, @Nullable AttributeContainer ref, Element attElem) {
    String refName = ref == null ? "no reference object" : ref.getName();
    String name = attElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML Attribute name is required (%s)%n", attElem);
      return;
    }
    String nameInFile = attElem.getAttributeValue("orgName");
    boolean newName = (nameInFile != null) && !nameInFile.equals(name);
    if (nameInFile == null) {
      nameInFile = name;
    } else if (null == findAttribute(ref, nameInFile)) { // has to exists
      errlog.format("NcML attribute orgName '%s' doesnt exist. att=%s in=%s%n", nameInFile, name, refName);
      return;
    }

    // see if its new
    ucar.nc2.Attribute oldatt = findAttribute(ref, nameInFile);
    if (oldatt == null) { // new
      if (debugConstruct) {
        System.out.println(" add new att = " + name);
      }
      try {
        ucar.ma2.Array values = readAttributeValues(attElem);
        dest.addAttribute(Attribute.fromArray(name, values));
      } catch (RuntimeException e) {
        errlog.format("NcML new Attribute Exception: %s att=%s in=%s%n", e.getMessage(), name, refName);
      }

    } else { // already exists

      if (debugConstruct) {
        System.out.println(" modify existing att = " + name);
      }
      boolean hasValue = attElem.getAttribute("value") != null;
      if (hasValue) { // has a new value
        try {
          ucar.ma2.Array values = readAttributeValues(attElem); // Handles "isUnsigned".
          dest.addAttribute(Attribute.fromArray(name, values));
        } catch (RuntimeException e) {
          errlog.format("NcML existing Attribute Exception: %s att=%s in=%s%n", e.getMessage(), name, refName);
          return;
        }

      } else { // use the old values
        Array oldval = oldatt.getValues();
        if (oldval != null) {
          dest.addAttribute(Attribute.builder(name).setValues(oldatt.getValues()).build());
        } else { // weird corner case of attribute with no value - must use the type
          String unS = attElem.getAttributeValue("isUnsigned"); // deprecated but must deal with
          boolean isUnsignedSet = "true".equalsIgnoreCase(unS);
          String typeS = attElem.getAttributeValue("type");
          DataType dtype = typeS == null ? DataType.STRING : DataType.getType(typeS);
          if (isUnsignedSet) {
            dtype = dtype.withSignedness(DataType.Signedness.UNSIGNED);
          }
          dest.addAttribute(Attribute.builder(name).setDataType(dtype).build());
        }
      }

      // remove the old one ??
      if (newName && !explicit) {
        dest.remove(oldatt);
        if (debugConstruct) {
          System.out.println(" remove old att = " + nameInFile);
        }
      }

    }
  }

  /**
   * Parse the values element
   *
   * @param s JDOM element to parse
   * @return Array with parsed values
   * @throws IllegalArgumentException if string values not parsable to specified data type
   */
  public static ucar.ma2.Array readAttributeValues(Element s) throws IllegalArgumentException {
    String valString = s.getAttributeValue("value");

    // can also be element text
    if (valString == null) {
      valString = s.getTextNormalize();
    }

    // no value specified hmm technically this is not illegal !!
    if (valString == null) {
      throw new IllegalArgumentException("No value specified");
    }

    String type = s.getAttributeValue("type");
    DataType dtype = (type == null) ? DataType.STRING : DataType.getType(type);
    if (dtype == DataType.CHAR) {
      dtype = DataType.STRING;
    }

    // backwards compatibility with deprecated isUnsigned attribute
    String unS = s.getAttributeValue("isUnsigned");
    boolean isUnsignedSet = "true".equalsIgnoreCase(unS);
    if (isUnsignedSet && dtype.isIntegral() && !dtype.isUnsigned()) {
      dtype = dtype.withSignedness(DataType.Signedness.UNSIGNED);
    }

    String sep = s.getAttributeValue("separator");
    if ((sep == null) && (dtype == DataType.STRING)) {
      List<String> list = new ArrayList<>();
      list.add(valString);
      return Array.makeArray(dtype, list);
    }

    if (sep == null) {
      sep = " "; // default whitespace separated
    }

    List<String> stringValues = new ArrayList<>();
    StringTokenizer tokn = new StringTokenizer(valString, sep);
    while (tokn.hasMoreTokens()) {
      stringValues.add(tokn.nextToken());
    }

    return Array.makeArray(dtype, stringValues);
  }

  private ucar.nc2.Attribute findAttribute(AttributeContainer atts, String name) {
    if (atts == null) {
      return null;
    }
    return atts.findAttribute(name);
  }

  /**
   * Read an NcML dimension element.
   *
   * @param groupBuilder put dimension into this group
   * @param refGroup parent Group in referenced dataset, may be null
   * @param dimElem ncml dimension element
   */
  private void readDim(Group.Builder groupBuilder, @Nullable Group refGroup, Element dimElem) {
    String name = dimElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML Dimension name is required (%s)%n", dimElem);
      return;
    }

    String nameInFile = dimElem.getAttributeValue("orgName");
    if (nameInFile == null) {
      nameInFile = name;
    }

    // LOOK this is wrong, groupBuilder may already have the dimension.
    // see if it already exists
    Dimension dim = (refGroup == null) ? null : refGroup.findDimension(nameInFile).orElse(null);
    if (dim == null) { // nope - create it
      String lengthS = dimElem.getAttributeValue("length");
      if (lengthS == null) {
        errlog.format("NcML Dimension length is required (%s)%n", dimElem);
        return;
      }

      String isUnlimitedS = dimElem.getAttributeValue("isUnlimited");
      String isSharedS = dimElem.getAttributeValue("isShared");
      String isVariableLengthS = dimElem.getAttributeValue("isVariableLength");

      boolean isUnlimited = "true".equalsIgnoreCase(isUnlimitedS);
      boolean isVariableLength = "true".equalsIgnoreCase(isVariableLengthS);
      boolean isShared = true;
      if ("false".equalsIgnoreCase(isSharedS)) {
        isShared = false;
      }

      int len;
      if (isVariableLength) {
        len = Dimension.VLEN.getLength();
      } else {
        len = Integer.parseInt(lengthS);
      }

      if (debugConstruct) {
        System.out.println(" add new dim = " + name);
      }
      // LOOK change to replaceDimension to get fort.54 working.
      groupBuilder.replaceDimension(Dimension.builder().setName(name).setIsShared(isShared).setIsUnlimited(isUnlimited)
          .setIsVariableLength(isVariableLength).setLength(len).build());

    } else { // existing - modify it
      Dimension.Builder newDim = this.explicit ? Dimension.builder() : dim.toBuilder();
      newDim.setName(name);

      String lengthS = dimElem.getAttributeValue("length");
      String isUnlimitedS = dimElem.getAttributeValue("isUnlimited");
      String isSharedS = dimElem.getAttributeValue("isShared");
      String isUnknownS = dimElem.getAttributeValue("isVariableLength");

      if (isUnlimitedS != null) {
        newDim.setIsUnlimited(isUnlimitedS.equalsIgnoreCase("true"));
      }

      if (isSharedS != null) {
        newDim.setIsShared(!isSharedS.equalsIgnoreCase("false"));
      }

      if (isUnknownS != null) {
        newDim.setIsVariableLength(isUnknownS.equalsIgnoreCase("true"));
      }

      if ((lengthS != null) && !dim.isVariableLength()) {
        int len = Integer.parseInt(lengthS);
        newDim.setLength(len);
      }

      if (debugConstruct) {
        System.out.println(" modify existing dim = " + name);
      }

      groupBuilder.removeDimension(name);
      groupBuilder.addDimension(newDim.build());
    }
  }

  /**
   * Read an NcML enumTypedef element.
   *
   * @param g put enumTypedef into this group
   * @param etdElem ncml enumTypedef element
   */
  private void readEnumTypedef(Group.Builder g, Element etdElem) {
    String name = etdElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML enumTypedef name is required (%s)%n", etdElem);
      return;
    }
    String typeS = etdElem.getAttributeValue("type");
    DataType baseType = (typeS == null) ? DataType.ENUM1 : DataType.getType(typeS);

    Map<Integer, String> map = new HashMap<>(100);
    for (Element e : etdElem.getChildren("enum", ncNS)) {
      String key = e.getAttributeValue("key");
      String value = e.getTextNormalize();
      if (key == null) {
        errlog.format("NcML enumTypedef enum key attribute is required (%s)%n", e);
        continue;
      }
      if (value == null) {
        errlog.format("NcML enumTypedef enum value is required (%s)%n", e);
        continue;
      }
      try {
        int keyi = Integer.parseInt(key);
        map.put(keyi, value);
      } catch (Exception e2) {
        errlog.format("NcML enumTypedef enum key attribute not an integer (%s)%n", e);
      }
    }

    EnumTypedef td = new EnumTypedef(name, map, baseType);
    g.addEnumTypedef(td);
  }

  /**
   * Read the NcML variable element, and nested elements.
   *
   * @param groupBuilder put dimension into this group
   * @param refGroup parent Group in referenced dataset, may be null
   * @param varElem ncml variable element
   */
  private void readVariable(Group.Builder groupBuilder, @Nullable Group refGroup, Element varElem) {
    String name = varElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML Variable name is required (%s)%n", varElem);
      return;
    }
    String nameInFile = Optional.ofNullable(varElem.getAttributeValue("orgName")).orElse(name);

    DataType dtype = null;
    String typeS = varElem.getAttributeValue("type");
    if (typeS != null) {
      dtype = DataType.getType(typeS);
    }

    // see if it already exists
    Variable refv = (refGroup == null) ? null : refGroup.findVariableLocal(nameInFile);
    Optional<Variable.Builder<?>> addedFromAgg = groupBuilder.findVariableLocal(nameInFile);
    if (refv == null && !addedFromAgg.isPresent()) { // new
      if (dtype == null) {
        errlog.format("NcML Variable dtype is required for new variable (%s)%n", name);
        return;
      }
      if (dtype == DataType.STRUCTURE || dtype == DataType.SEQUENCE) {
        groupBuilder.addVariable(readStructureNew(groupBuilder, varElem));
      } else {
        groupBuilder.addVariable(readVariableNew(groupBuilder, dtype, varElem));
      }
      return;
    }

    // refv exists
    if (refv != null) {
      if (dtype == null) {
        dtype = refv.getDataType();
      }
      if (dtype == DataType.STRUCTURE || dtype == DataType.SEQUENCE) {
        readStructureExisting(groupBuilder, null, dtype, (Structure) refv, varElem)
            .ifPresent(groupBuilder::addVariable);
      } else {
        readVariableExisting(groupBuilder, null, dtype, refv, varElem).ifPresent(groupBuilder::addVariable);
      }
      return;
    }

    // refv does not exist, but addedFromAgg may be present
    DataType finalDtype = dtype;
    addedFromAgg.ifPresent(agg -> {
      if (agg instanceof VariableDS.Builder<?>) {
        VariableDS.Builder<?> aggDs = (VariableDS.Builder<?>) agg;
        aggDs.setOriginalName(nameInFile);
      }
      agg.setParentGroupBuilder(groupBuilder);
      DataType reallyFinalDtype = finalDtype != null ? finalDtype : agg.dataType;
      augmentVariableNew(agg, reallyFinalDtype, varElem);
    });
  }

  private Optional<Variable.Builder<?>> readVariableExisting(Group.Builder groupBuilder,
      @Nullable Structure.Builder<?> parentStructure, DataType dtype, Variable refv, Element varElem) {
    String name = varElem.getAttributeValue("name");
    String typedefS = dtype.isEnum() ? varElem.getAttributeValue("typedef") : null;
    String nameInFile = Optional.ofNullable(varElem.getAttributeValue("orgName")).orElse(name);

    VariableDS.Builder<?> vb;
    if (this.explicit) { // all metadata is in the ncml, do not copy
      vb = VariableDS.builder().setOriginalVariable(refv);
    } else { // modify existing
      if (parentStructure != null) {
        vb = (VariableDS.Builder<?>) parentStructure.findMemberVariable(nameInFile)
            .orElseThrow(() -> new IllegalStateException("Cant find variable " + nameInFile));
      } else {
        vb = (VariableDS.Builder<?>) groupBuilder.findVariableLocal(nameInFile)
            .orElseThrow(() -> new IllegalStateException("Cant find variable " + nameInFile));
      }
    }
    vb.setParentGroupBuilder(groupBuilder);
    vb.setName(name).setDataType(dtype);
    if (typedefS != null) {
      vb.setEnumTypeName(typedefS);
    }

    String dimNames = varElem.getAttributeValue("shape"); // list of dimension names
    if (dimNames != null) {
      List<Dimension> varDims = groupBuilder.makeDimensionsList(dimNames);
      vb.setDimensions(varDims); // TODO check conformable
    }

    java.util.List<Element> attList = varElem.getChildren("attribute", ncNS);
    for (Element attElem : attList) {
      readAtt(vb.getAttributeContainer(), refv.attributes(), attElem);
    }

    // deal with legacy use of attribute with Unsigned = true
    Attribute att = vb.getAttributeContainer().findAttribute(CDM.UNSIGNED);
    boolean isUnsignedSet = att != null && att.getStringValue().equalsIgnoreCase("true");
    if (isUnsignedSet) {
      dtype = dtype.withSignedness(DataType.Signedness.UNSIGNED);
      vb.setDataType(dtype);
    }

    // process remove command
    java.util.List<Element> removeList = varElem.getChildren("remove", ncNS);
    for (Element remElem : removeList) {
      cmdRemove(vb, remElem.getAttributeValue("type"), remElem.getAttributeValue("name"));
    }

    Element valueElem = varElem.getChild("values", ncNS);
    if (valueElem != null) {
      readValues(vb, dtype, varElem, valueElem);
    }
    /*
     * else {
     * // see if we need to munge existing data. use case : aggregation
     * if (v.hasCachedData()) {
     * Array data;
     * try {
     * data = v.read();
     * } catch (IOException e) {
     * throw new IllegalStateException(e.getMessage());
     * }
     * if (data.getClass() != v.getDataType().getPrimitiveClassType()) {
     * Array newData = Array.factory(v.getDataType(), v.getShape());
     * MAMath.copy(newData, data);
     * v.setCachedData(newData, false);
     * }
     * }
     * }
     */

    // look for logical views
    // processLogicalViews(v, refGroup, varElem);
    // only return if it needs to be added
    return (this.explicit) ? Optional.of(vb) : Optional.empty();
  }

  /**
   * Read a NcML variable element that does not have a reference variable
   *
   * @param groupBuilder group that the variable is part of
   * @param varElem ncml variable element
   * @return return new Variable.Builder
   */
  private VariableDS.Builder<?> readVariableNew(Group.Builder groupBuilder, DataType dtype, Element varElem) {
    String name = varElem.getAttributeValue("name");
    VariableDS.Builder<?> v = VariableDS.builder().setName(name).setDataType(dtype).setParentGroupBuilder(groupBuilder);

    // list of dimension names
    String dimNames = varElem.getAttributeValue("shape");
    if (dimNames != null) {
      v.setDimensionsByName(dimNames);
    }
    Element valueElem = varElem.getChild("values", ncNS);
    if (valueElem != null) {
      readValues(v, dtype, varElem, valueElem);
    }

    // look for attributes
    java.util.List<Element> attList = varElem.getChildren("attribute", ncNS);
    for (Element attElem : attList) {
      readAtt(v.getAttributeContainer(), null, attElem);
    }

    String typedefS = dtype.isEnum() ? varElem.getAttributeValue("typedef") : null;
    if (typedefS != null) {
      v.setEnumTypeName(typedefS);
    }

    return v;
  }

  private void augmentVariableNew(Variable.Builder<?> addedFromAgg, DataType dtype, Element varElem) {
    String name = varElem.getAttributeValue("name");
    addedFromAgg.setName(name).setDataType(dtype);

    // list of dimension names
    String dimNames = varElem.getAttributeValue("shape");
    if (dimNames != null) {
      addedFromAgg.setDimensionsByName(dimNames);
    }
    Element valueElem = varElem.getChild("values", ncNS);
    if (valueElem != null) {
      readValues(addedFromAgg, dtype, varElem, valueElem);
    }

    // look for attributes
    java.util.List<Element> attList = varElem.getChildren("attribute", ncNS);
    for (Element attElem : attList) {
      readAtt(addedFromAgg.getAttributeContainer(), null, attElem);
    }

    String typedefS = dtype.isEnum() ? varElem.getAttributeValue("typedef") : null;
    if (typedefS != null) {
      addedFromAgg.setEnumTypeName(typedefS);
    }
  }

  private Optional<Structure.Builder<?>> readStructureExisting(Group.Builder groupBuilder,
      @Nullable Structure.Builder<?> parentStructure, DataType dtype, Structure refStructure, Element varElem) {
    String name = varElem.getAttributeValue("name");
    String nameInFile = Optional.ofNullable(varElem.getAttributeValue("orgName")).orElse(name);

    Structure.Builder<?> structBuilder;
    if (this.explicit) { // all metadata is in the ncml, do not copy
      if (dtype == DataType.STRUCTURE) {
        structBuilder = StructureDS.builder().setName(name).setOriginalVariable(refStructure);
      } else {
        structBuilder = SequenceDS.builder().setName(name).setOriginalSequence((Sequence) refStructure);
      }
    } else { // modify existing
      if (parentStructure != null) {
        structBuilder = (StructureDS.Builder<?>) parentStructure.findMemberVariable(nameInFile)
            .orElseThrow(() -> new IllegalStateException("Cant find variable " + nameInFile));
      } else {
        structBuilder = (StructureDS.Builder<?>) groupBuilder.findVariableLocal(nameInFile)
            .orElseThrow(() -> new IllegalStateException("Cant find variable " + nameInFile));
      }
    }

    String dimNames = varElem.getAttributeValue("shape"); // list of dimension names
    if (dimNames != null) {
      List<Dimension> varDims = groupBuilder.makeDimensionsList(dimNames);
      structBuilder.addDimensions(varDims); // TODO check conformable
    }

    java.util.List<Element> attList = varElem.getChildren("attribute", ncNS);
    for (Element attElem : attList) {
      readAtt(structBuilder.getAttributeContainer(), refStructure.attributes(), attElem);
    }

    java.util.List<Element> varList = varElem.getChildren("variable", ncNS);
    for (Element vElem : varList) {
      readMemberVariable(groupBuilder, structBuilder, refStructure, vElem);
    }

    // process remove command
    java.util.List<Element> removeList = varElem.getChildren("remove", ncNS);
    for (Element remElem : removeList) {
      cmdRemove(structBuilder, remElem.getAttributeValue("type"), remElem.getAttributeValue("name"));
    }

    // look for logical views
    // processLogicalViews(v, refGroup, varElem);
    return (this.explicit) ? Optional.of(structBuilder) : Optional.empty();
  }

  private Structure.Builder<?> readStructureNew(Group.Builder groupBuilder, Element varElem) {
    String name = varElem.getAttributeValue("name");
    String type = varElem.getAttributeValue("type");
    DataType dtype = DataType.getType(type);

    // list of dimension names
    String dimNames = varElem.getAttributeValue("shape");
    if (dimNames == null) {
      dimNames = ""; // deprecated, prefer explicit ""
    }
    List<Dimension> varDims = groupBuilder.makeDimensionsList(dimNames);

    Structure.Builder<?> structBuilder;
    if (dtype == DataType.STRUCTURE) {
      structBuilder = StructureDS.builder().setName(name).addDimensions(varDims);
    } else {
      structBuilder = SequenceDS.builder().setName(name);
    }

    java.util.List<Element> varList = varElem.getChildren("variable", ncNS);
    for (Element vElem : varList) {
      readMemberVariable(groupBuilder, structBuilder, null, vElem);
    }

    // look for attributes
    java.util.List<Element> attList = varElem.getChildren("attribute", ncNS);
    for (Element attElem : attList) {
      readAtt(structBuilder.getAttributeContainer(), null, attElem);
    }

    return structBuilder;
  }

  private void readMemberVariable(Group.Builder groupBuilder, Structure.Builder<?> parentStructure,
      @Nullable Structure refParentStructure, Element varElem) {
    String name = varElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML Variable name is required (%s)%n", varElem);
      return;
    }
    String nameInFile = Optional.ofNullable(varElem.getAttributeValue("orgName")).orElse(name);

    DataType dtype = null;
    String typeS = varElem.getAttributeValue("type");
    if (typeS != null) {
      dtype = DataType.getType(typeS);
    }

    // see if it already exists
    Variable refv = (refParentStructure == null) ? null : refParentStructure.findVariable(nameInFile);
    if (refv == null) { // new
      if (dtype == null) {
        errlog.format("NcML Variable dtype is required for new (nested) variable (%s)%n", name);
        return;
      }
      if (dtype == DataType.STRUCTURE || dtype == DataType.SEQUENCE) {
        parentStructure.addMemberVariable(readStructureNew(groupBuilder, varElem));
      } else {
        parentStructure.addMemberVariable(readVariableNew(groupBuilder, dtype, varElem));
      }
      return;
    }

    // refv exists
    if (dtype == null) {
      dtype = refv.getDataType();
    }

    if (dtype == DataType.STRUCTURE || dtype == DataType.SEQUENCE) {
      readStructureExisting(groupBuilder, parentStructure, dtype, (Structure) refv, varElem)
          .ifPresent(parentStructure::addMemberVariable);
    } else {
      readVariableExisting(groupBuilder, parentStructure, dtype, refv, varElem)
          .ifPresent(parentStructure::addMemberVariable);
    }
  }

  private void readValues(Variable.Builder<?> v, DataType dtype, Element varElem, Element valuesElem) {
    try {
      // check if values are specified by attribute
      String fromAttribute = valuesElem.getAttributeValue("fromAttribute");
      if (fromAttribute != null) {
        if (this.refFile == null) {
          errlog.format("NcML fromAttribute '%s' with no referenced Dataset%n", fromAttribute);
          return;
        }
        Attribute att;
        int pos = fromAttribute.indexOf('@'); // varName@attName
        if (pos > 0) {
          String varName = fromAttribute.substring(0, pos);
          String attName = fromAttribute.substring(pos + 1);
          Variable vFrom = this.refFile.findVariable(varName);
          if (vFrom == null) {
            errlog.format("Cant find variable %s (%s) %n", fromAttribute, v.shortName);
            return;
          }
          att = vFrom.findAttribute(attName);

        } else { // attName or @attName
          String attName = (pos == 0) ? fromAttribute.substring(1) : fromAttribute;
          att = this.refFile.getRootGroup().findAttribute(attName);
          // att = this.refFile.findAttribute(attName);
        }
        if (att == null) {
          errlog.format("Cant find attribute %s %n", fromAttribute);
          return;
        }
        v.setCachedData(att.getArrayValues());
        return;
      }

      // check if values are specified by start / increment
      String startS = valuesElem.getAttributeValue("start");
      String incrS = valuesElem.getAttributeValue("increment");
      // String nptsS = valuesElem.getAttributeValue("npts");
      // int npts = (nptsS == null) ? 0 : Integer.parseInt(nptsS); NOT USED

      // start, increment are specified
      if ((startS != null) && (incrS != null)) {
        double start = Double.parseDouble(startS);
        double incr = Double.parseDouble(incrS);
        // this defers creation until build(), when all dimension sizes are known.
        // must also set dimensions by name.
        v.setAutoGen(start, incr);
        if (v.getRank() > 0) {
          v.setDimensionsByName(v.makeDimensionsString());
        }
        return;
      }

      // otherwise values are listed in text
      String values = varElem.getChildText("values", ncNS);
      String sep = valuesElem.getAttributeValue("separator");

      if (dtype == DataType.CHAR) {
        int nhave = values.length();
        int nwant = (int) Dimensions.getSize(v.getDimensions());
        char[] data = new char[nwant];
        for (int i = 0; i < nhave && i < nwant; i++) {
          data[i] = values.charAt(i);
        }
        ucar.array.Array<?> dataArray = Arrays.factory(DataType.CHAR, Dimensions.makeShape(v.getDimensions()), data);
        v.setCachedData(dataArray);

      } else {
        List<String> valList = getTokens(values, sep);
        Array data = Array.makeArray(dtype, valList);
        if (v.getDimensions().size() != 1) { // dont have to reshape for rank 1
          data = data.reshape(Dimensions.makeShape(v.getDimensions()));
        }
        v.setCachedData(data);
      }

    } catch (Throwable t) {
      throw new RuntimeException("NCML Reading on " + v.shortName, t);
    }
  }

  static List<String> getTokens(String fullString, String sep) {

    List<String> strs = new ArrayList<>();
    if (sep != null) {
      int sepLength = sep.length();
      switch (sepLength) {
        case 0:
          strs = StringUtil2.splitList(fullString);
          break;

        case 1:
          StringTokenizer tokenizer = new StringTokenizer(fullString, sep); // maybe use StreamTokenizer?
          while (tokenizer.hasMoreTokens())
            strs.add(tokenizer.nextToken());
          break;

        default:
          String remainderString = fullString; // multicharacter separator
          int location = remainderString.indexOf(sep);
          while (location != -1) { // watch out for off-by-one errors on the string splitting indices!!!
            if (location == 0) { // remainderString starts with the separator, cut it off
              remainderString = remainderString.substring(location + sepLength);
              location = remainderString.indexOf(sep);
            } else {
              String token = remainderString.substring(0, location); // pull the token off the front of the string
              strs.add(token); // add the token to our list
              remainderString = remainderString.substring(location + sepLength); // cut out both the token and the
              // separator
              location = remainderString.indexOf(sep);
            }
          } // close while loop
          if (!remainderString.isEmpty())
            strs.add(remainderString); // add the last token, post last separator
      } // close switch (sepLength)
    } else { // default to white space separator if sep is null
      strs = StringUtil2.splitList(fullString);
    }
    if (strs.isEmpty())
      strs.add(""); // maybe thrown an exception instead? return null?
    return strs;
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  private Aggregation readAgg(Element aggElem, String ncmlLocation, NetcdfDataset.Builder<?> builder,
      CancelTask cancelTask) {
    String dimName = aggElem.getAttributeValue("dimName");
    String type = aggElem.getAttributeValue("type");
    String recheck = aggElem.getAttributeValue("recheckEvery");

    Aggregation agg;
    if (type.equalsIgnoreCase("joinExisting")) {
      agg = new AggregationExisting(builder, dimName, recheck);

    } else if (type.equalsIgnoreCase("joinNew")) {
      agg = new AggregationNew(builder, dimName, recheck);

    } else if (type.equalsIgnoreCase("union")) {
      agg = new AggregationUnion(builder, dimName, recheck);

      /*
       * } else if (type.equalsIgnoreCase("tiled")) {
       * agg = new AggregationTiled(builder, dimName, recheck);
       *
       * } else if (type.equalsIgnoreCase("forecastModelRunCollection")
       * || type.equalsIgnoreCase("forecastModelRunSingleCollection")) {
       * AggregationFmrc aggc = new AggregationFmrc(newds, dimName, recheck);
       * agg = aggc;
       *
       * // nested scanFmrc elements
       * java.util.List<Element> scan2List = aggElem.getChildren("scanFmrc", ncNS);
       * for (Element scanElem : scan2List) {
       * String dirLocation = scanElem.getAttributeValue("location");
       * if (dirLocation != null)
       * dirLocation = AliasTranslator.translateAlias(dirLocation);
       * String regexpPatternString = scanElem.getAttributeValue("regExp");
       * String suffix = scanElem.getAttributeValue("suffix");
       * String subdirs = scanElem.getAttributeValue("subdirs");
       * String olderS = scanElem.getAttributeValue("olderThan");
       *
       * String runMatcher = scanElem.getAttributeValue("runDateMatcher");
       * String forecastMatcher = scanElem.getAttributeValue("forecastDateMatcher");
       * String offsetMatcher = scanElem.getAttributeValue("forecastOffsetMatcher");
       *
       * // possible relative location
       * dirLocation = URLnaming.resolve(ncmlLocation, dirLocation);
       *
       * if (dirLocation != null) {
       * aggc.addDirectoryScanFmrc(dirLocation, suffix, regexpPatternString, subdirs, olderS, runMatcher,
       * forecastMatcher, offsetMatcher);
       * }
       *
       * if ((cancelTask != null) && cancelTask.isCancel())
       * return agg;
       * if (debugAggDetail)
       * System.out.println(" debugAgg: nested dirLocation = " + dirLocation);
       * }
       *
       * // add explicit files to the agg (i.e. not from a scanned directory)
       * Map<String, String> realLocationRunTimeMap = new HashMap<>();
       * List<String> realLocationList = new ArrayList<>();
       * java.util.List<Element> ncList = aggElem.getChildren("netcdf", ncNS);
       * for (Element netcdfElemNested : ncList) {
       * String location = netcdfElemNested.getAttributeValue("location");
       * if (location == null)
       * location = netcdfElemNested.getAttributeValue("url");
       * if (location != null)
       * location = AliasTranslator.translateAlias(location);
       *
       * String runTime = netcdfElemNested.getAttributeValue("coordValue");
       * if (runTime == null) {
       * Formatter f = new Formatter();
       * f.format("runtime must be explicitly defined for each netcdf element using the attribute coordValue");
       * log.error(f.toString());
       * }
       *
       * String realLocation = URLnaming.resolveFile(ncmlLocation, location);
       * realLocationRunTimeMap.put(realLocation, runTime);
       * realLocationList.add(realLocation);
       *
       * if ((cancelTask != null) && cancelTask.isCancel())
       * return aggc;
       * if (debugAggDetail)
       * System.out.println(" debugAgg: nested dataset = " + location);
       * }
       *
       * if (!realLocationRunTimeMap.isEmpty()) {
       * aggc.addExplicitFilesAndRunTimes(realLocationRunTimeMap);
       * }
       */
    } else {
      throw new IllegalArgumentException("Unsupported aggregation type=" + type);
    }

    if (agg instanceof AggregationOuter) {
      AggregationOuter aggo = (AggregationOuter) agg;

      String timeUnitsChange = aggElem.getAttributeValue("timeUnitsChange");
      if (timeUnitsChange != null) {
        aggo.setTimeUnitsChange(timeUnitsChange.equalsIgnoreCase("true"));
      }

      // look for variables that need to be aggregated (aggNew)
      java.util.List<Element> list = aggElem.getChildren("variableAgg", ncNS);
      for (Element vaggElem : list) {
        String varName = vaggElem.getAttributeValue("name");
        aggo.addVariable(varName);
      }

      // look for attributes to promote to variables
      list = aggElem.getChildren("promoteGlobalAttribute", ncNS);
      for (Element gattElem : list) {
        String varName = gattElem.getAttributeValue("name");
        String orgName = gattElem.getAttributeValue("orgName");
        aggo.addVariableFromGlobalAttribute(varName, orgName);
      }

      // look for attributes to promote to variables
      list = aggElem.getChildren("promoteGlobalAttributeCompose", ncNS);
      for (Element gattElem : list) {
        String varName = gattElem.getAttributeValue("name");
        String format = gattElem.getAttributeValue("format");
        String orgName = gattElem.getAttributeValue("orgName");
        aggo.addVariableFromGlobalAttributeCompose(varName, format, orgName);
      }

      // look for variable to cache
      list = aggElem.getChildren("cacheVariable", ncNS);
      for (Element gattElem : list) {
        String varName = gattElem.getAttributeValue("name");
        aggo.addCacheVariable(varName, null);
      }
    }

    // nested netcdf elements
    java.util.List<Element> ncList = aggElem.getChildren("netcdf", ncNS);
    for (Element netcdfElemNested : ncList) {
      String location = netcdfElemNested.getAttributeValue("location");
      if (location == null) {
        location = netcdfElemNested.getAttributeValue("url");
      }
      if (location != null) {
        location = AliasTranslator.translateAlias(location);
      }

      String id = netcdfElemNested.getAttributeValue("id");
      String ncoords = netcdfElemNested.getAttributeValue("ncoords");
      String coordValueS = netcdfElemNested.getAttributeValue("coordValue");
      String sectionSpec = netcdfElemNested.getAttributeValue("section");

      // must always open through a NcML reader, in case the netcdf element modifies the dataset
      NcmlElementReader reader = new NcmlElementReader(ncmlLocation, location, netcdfElemNested);
      String cacheName = (location != null) ? location : ncmlLocation;
      cacheName += "#" + netcdfElemNested.hashCode(); // need a unique name, in case file has been modified by ncml

      String realLocation = URLnaming.resolveFile(ncmlLocation, location);

      agg.addExplicitDataset(cacheName, realLocation, id, ncoords, coordValueS, sectionSpec, reader);

      if ((cancelTask != null) && cancelTask.isCancel()) {
        return agg;
      }
      if (debugAggDetail) {
        System.out.println(" debugAgg: nested dataset = " + location);
      }
    }

    // nested scan elements
    java.util.List<Element> dirList = aggElem.getChildren("scan", ncNS);
    for (Element scanElem : dirList) {
      String dirLocation = scanElem.getAttributeValue("location");
      if (dirLocation == null) {
        throw new IllegalArgumentException("scan element must have location attribute");
      }

      dirLocation = AliasTranslator.translateAlias(dirLocation);

      String regexpPatternString = scanElem.getAttributeValue("regExp");
      String suffix = scanElem.getAttributeValue("suffix");
      String subdirs = scanElem.getAttributeValue("subdirs");
      String olderS = scanElem.getAttributeValue("olderThan");

      String dateFormatMark = scanElem.getAttributeValue("dateFormatMark");
      Set<NetcdfDataset.Enhance> enhanceMode = parseEnhanceMode(scanElem.getAttributeValue("enhance"));

      // possible relative location
      dirLocation = URLnaming.resolve(ncmlLocation, dirLocation);

      // can embed a full-blown crawlableDatasetImpl element
      Element cdElement = scanElem.getChild("crawlableDatasetImpl", ncNS); // ok if null
      agg.addDatasetScan(cdElement, dirLocation, suffix, regexpPatternString, dateFormatMark, enhanceMode, subdirs,
          olderS);

      if ((cancelTask != null) && cancelTask.isCancel()) {
        return agg;
      }
      if (debugAggDetail) {
        System.out.println(" debugAgg: nested dirLocation = " + dirLocation);
      }
    }

    // experimental
    Element collElem = aggElem.getChild("collection", ncNS);
    if (collElem != null) {
      agg.addCollection(collElem.getAttributeValue("spec"), collElem.getAttributeValue("olderThan"));
    }

    /*
     * <!-- experimental - modify each dataset in aggregation -->
     * <xsd:choice minOccurs="0" maxOccurs="unbounded">
     * <xsd:element ref="group"/>
     * <xsd:element ref="dimension"/>
     * <xsd:element ref="variable"/>
     * <xsd:element ref="attribute"/>
     * <xsd:element ref="remove"/>
     * </xsd:choice>
     */
    boolean needMerge = !aggElem.getChildren("attribute", ncNS).isEmpty();
    if (!needMerge) {
      needMerge = !aggElem.getChildren("variable", ncNS).isEmpty();
    }
    if (!needMerge) {
      needMerge = !aggElem.getChildren("dimension", ncNS).isEmpty();
    }
    if (!needMerge) {
      needMerge = !aggElem.getChildren("group", ncNS).isEmpty();
    }
    if (!needMerge) {
      needMerge = !aggElem.getChildren("remove", ncNS).isEmpty();
    }
    if (needMerge) {
      agg.setModifications(aggElem);
    }

    return agg;
  }

  private class NcmlElementReader implements ucar.nc2.internal.cache.FileFactory {
    private final Element netcdfElem;
    private final String ncmlLocation;
    private final String location;

    NcmlElementReader(String ncmlLocation, String location, Element netcdfElem) {
      this.ncmlLocation = ncmlLocation;
      this.location = location;
      this.netcdfElem = netcdfElem;
    }

    public NetcdfFile open(DatasetUrl cacheName, int buffer_size, CancelTask cancelTask, Object spiObject)
        throws IOException {
      if (debugAggDetail) {
        System.out.println(" NcmlElementReader open nested dataset " + cacheName);
      }
      NetcdfFile.Builder<?> result = readNcml(ncmlLocation, location, netcdfElem, cancelTask);
      result.setLocation(ncmlLocation + "#" + location);
      return result.build();
    }
  }

  /////////////////////////////////////////////
  // command procesing

  private void cmdRemove(Group.Builder g, String type, String name) {
    boolean err = false;
    switch (type) {
      case "dimension":
        if (!g.removeDimension(name)) {
          err = true;
        }

        break;
      case "variable":
        if (!g.removeVariable(name)) {
          err = true;
        }
        break;
      case "attribute":
        if (!g.getAttributeContainer().removeAttribute(name)) {
          err = true;
        }
        break;
    }

    if (err) {
      Formatter f = new Formatter();
      f.format("CMD remove %s CANT find %s location %s%n", type, name, location);
      log.info(f.toString());
    }
  }

  private void cmdRemove(Variable.Builder<?> v, String type, String name) {
    boolean err = false;

    if (type.equals("attribute")) {
      if (!v.getAttributeContainer().removeAttribute(name)) {
        err = true;
      }
    } else if (type.equals("variable") && v instanceof Structure.Builder<?>) {
      Structure.Builder<?> s = (Structure.Builder<?>) v;
      if (!s.removeMemberVariable(name)) {
        err = true;
      }
    }

    if (err) {
      Formatter f = new Formatter();
      f.format("CMD remove %s CANT find %s location %s%n", type, name, location);
      log.info(f.toString());
    }
  }
}
