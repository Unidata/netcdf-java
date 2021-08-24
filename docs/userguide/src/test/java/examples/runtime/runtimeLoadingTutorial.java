package examples.runtime;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.constants.FeatureType;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;

public class runtimeLoadingTutorial {

  public static void registerIOSP(String classNameAsString)
      throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    ucar.nc2.NetcdfFiles.registerIOProvider(classNameAsString);
  }

  public static void registerCoordSystemBuilder(String conventionNameAsString,
      String classNameAsString) throws ClassNotFoundException {
    ucar.nc2.internal.dataset.CoordSystemFactory.registerConvention(conventionNameAsString,
        classNameAsString);
  }

  public static void registerCoordTransBuilder(String transformNameAsString,
      String classNameAsString) throws ClassNotFoundException {
    ucar.nc2.internal.dataset.CoordTransformFactory.registerTransform(transformNameAsString,
        classNameAsString);
  }

  public static void registerFeatureDatasetFactory(FeatureType featureType,
      String classNameAsString) {
    ucar.nc2.ft.FeatureDatasetFactoryManager.registerFactory(featureType, classNameAsString);
  }

  public static void registerGRIBTable(int centerAsInt, int subcenterAsInt, int tableVersionAsInt,
      String yourFileNameAsString) {
    ucar.nc2.grib.grib1.tables.Grib1ParamTables.addParameterTable(centerAsInt, subcenterAsInt,
        tableVersionAsInt, yourFileNameAsString);
  }

  public static void registerGRIBLookupTable(String yourLookupFileNameAsString) throws IOException {
    ucar.nc2.grib.grib1.tables.Grib1ParamTables.addParameterTableLookup(yourLookupFileNameAsString);
  }

  public static void registerBUFRTable(String yourLookupFileNameAsString) throws IOException {
    ucar.nc2.iosp.bufr.tables.BufrTables.addLookupFile(yourLookupFileNameAsString);
  }

  public static Class<Object> yourScriptThis = null;

  public static void passConfigurationToCDM(String yourFileNameAsString) throws IOException {
    // Example 1: read from file
    Formatter errlog = new Formatter();
    FileInputStream fis = new FileInputStream(yourFileNameAsString);
    ucar.nc2.internal.util.xml.RuntimeConfigParser.read(fis, errlog);
    System.out.println(errlog);

    // Example 2: read from resource
    ClassLoader cl = yourScriptThis.getClassLoader();
    InputStream is = cl.getResourceAsStream("resources/nj22/configFile.xml");
    ucar.nc2.internal.util.xml.RuntimeConfigParser.read(is, errlog);

    // Example 3: extract JDOM element from a larger XML document:
    Document doc;
    SAXBuilder saxBuilder = new SAXBuilder();
    saxBuilder.setExpandEntities(false);
    try {
      doc = saxBuilder.build(yourFileNameAsString);
    } catch (JDOMException | IOException e) {
      throw new IOException(e.getMessage());
    }
    Element root = doc.getRootElement();
    Element elem = root.getChild("nj22Config");
    if (elem != null)
      ucar.nc2.internal.util.xml.RuntimeConfigParser.read(elem, errlog);
  }

  public static String[] args;

  public static void passConfigurationToolsUI() {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-nj22Config") && (i < args.length - 1)) {
        String runtimeConfig = args[i + 1];
        i++;
        try {
          Formatter errlog = new Formatter();
          FileInputStream fis = new FileInputStream(runtimeConfig);
          ucar.nc2.internal.util.xml.RuntimeConfigParser.read(fis, errlog);
          System.out.println(errlog);
        } catch (IOException ioe) {
          System.out.println("Error reading " + runtimeConfig + "=" + ioe.getMessage());
        }
      }
    }
  }

}
