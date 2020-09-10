/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.util.xml;

import java.util.Formatter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.constants.FeatureType;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import ucar.nc2.internal.dataset.CoordTransformFactory;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.internal.dataset.CoordSystemFactory;

/**
 * Read Runtime Configuration.
 * <p/>
 * 
 * <pre>
 * <runtimeConfig>
 *   <ioServiceProvider  class="edu.univ.ny.stuff.FooFiles"/>
 *   <coordSystemFactory convention="foo" class="test.Foo"/>
 *   <coordTransBuilder name="atmos_ln_sigma_coordinates" type="vertical" class="my.stuff.atmosSigmaLog"/>
 *   <typedDatasetFactory datatype="Point" class="gov.noaa.obscure.file.Flabulate"/>
 * </runtimeConfig>
 * </pre>
 *
 * @author caron
 */
public class RuntimeConfigParser {

  public static void read(InputStream is, Formatter errlog) throws IOException {
    Document doc;
    SAXBuilder saxBuilder = new SAXBuilder();
    try {
      doc = saxBuilder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    read(doc.getRootElement(), errlog);
  }

  public static void read(org.jdom2.Element root, Formatter errlog) {

    for (Element elem : root.getChildren()) {
      switch (elem.getName()) {
        case "ioServiceProvider": {
          String className = elem.getAttributeValue("class");
          try {
            NetcdfFiles.registerIOProvider(className);
            errlog.format("IOServiceProvider added %s%n", className);
          } catch (ClassNotFoundException e) {
            errlog.format("IOServiceProvider class %s not found; check your classpath%n", className);
          } catch (Exception e) {
            errlog.format("IOServiceProvider %s error='%s'%n", className, e.getMessage());
          }
          break;
        }

        case "coordSystemBuilderFactory": {
          String conventionName = elem.getAttributeValue("convention");
          String className = elem.getAttributeValue("class");
          try {
            CoordSystemFactory.registerConvention(conventionName, className);
            errlog.format("CoordSystemBuilderFactory added %s for Convention '%s'%n", className, conventionName);
          } catch (ClassNotFoundException e) {
            errlog.format("CoordSystemBuilderFactory class '%s' not found; check your classpath%n", className);
          } catch (Exception e) {
            errlog.format("CoordSystemBuilderFactory %s error='%s'%n", className, e.getMessage());
          }
          break;
        }

        case "coordTransBuilder": {
          String transformName = elem.getAttributeValue("name");
          String className = elem.getAttributeValue("class");
          try {
            CoordTransformFactory.registerTransform(transformName, className);
            errlog.format("CoordTransBuilder added %s%n", className);
          } catch (ClassNotFoundException e) {
            errlog.format("CoordTransBuilder class %s not found; check your classpath%n", className);
          } catch (Exception e) {
            errlog.format("CoordTransBuilder %s error='%s'%n", className, e.getMessage());
          }
          break;
        }

        case "featureDatasetFactory": {
          String typeName = elem.getAttributeValue("featureType");
          String className = elem.getAttributeValue("class");
          FeatureType featureType = FeatureType.getType(typeName.toUpperCase());
          if (null == featureType) {
            errlog.format("FeatureDatasetFactory %s unknown datatype='%s'%n", className, typeName);
            continue;
          }
          try {
            boolean ok = FeatureDatasetFactoryManager.registerFactory(featureType, className);
            if (!ok) {
              errlog.format("FeatureDatasetFactory %s not loaded; check your classpath%n", className);
            } else {
              errlog.format("FeatureDatasetFactory added %s%n", className);
            }
          } catch (Exception e) {
            errlog.format("FeatureDatasetFactory %s error='%s'%n", className, e.getMessage());
          }
          break;
        }

        case "gribParameterTable": {
          String editionS = elem.getAttributeValue("edition");
          String centerS = elem.getAttributeValue("center");
          String subcenterS = elem.getAttributeValue("subcenter");
          String versionS = elem.getAttributeValue("version");
          String filename = elem.getText();
          if ((centerS == null) || (versionS == null) || (filename == null)) {
            errlog.format("table element must have center, version and filename attributes%n");
            continue;
          }
          try {
            int center = Integer.parseInt(centerS);
            int subcenter = (subcenterS == null) ? -1 : Integer.parseInt(subcenterS);
            int version = Integer.parseInt(versionS);
            // Grib1ParamTables.addParameterTable(int center, int subcenter, int tableVersion, String tableFilename)
            Class<?> c =
                RuntimeConfigParser.class.getClassLoader().loadClass("ucar.nc2.grib.grib1.tables.Grib1ParamTables");
            Method m = c.getMethod("addParameterTable", int.class, int.class, int.class, String.class);
            // TODO this only loads the table, we should open it so that it fails if not present
            m.invoke(null, center, subcenter, version, filename);
            errlog.format("GribParameterTable.addParameterTable added %s%n", filename);

          } catch (Exception e) {
            e.printStackTrace();
            errlog.format("GribParameterTable.addParameterTable() error='%s'%n", e.getMessage());
          }
          break;
        }

        case "grib1Table": {
          String strictS = elem.getAttributeValue("strict");
          if (strictS != null) {
            boolean notStrict = strictS.equalsIgnoreCase("false");
            try {
              Class<?> c =
                  RuntimeConfigParser.class.getClassLoader().loadClass("ucar.nc2.grib.grib1.tables.Grib1ParamTables");
              Method m = c.getMethod("setStrict", boolean.class);
              m.invoke(null, !notStrict);
              errlog.format("Grib1ParamTables.setStrict to %s%n", !notStrict);

            } catch (Exception e) {
              e.printStackTrace();
              errlog.format("Grib1ParamTables.setStrict() error='%s'%n", e.getMessage());
            }
            continue;
          }
          break;
        }

        case "gribParameterTableLookup": {
          String editionS = elem.getAttributeValue("edition");
          String filename = elem.getText();
          // ucar.nc2.grib.grib1.tables.Grib1ParamTables.addParameterTableLookup(String lookupFilename)
          try {
            Class<?> c =
                RuntimeConfigParser.class.getClassLoader().loadClass("ucar.nc2.grib.grib1.tables.Grib1ParamTables");
            Method m = c.getMethod("addParameterTableLookup", String.class);
            Boolean ok = (Boolean) m.invoke(null, filename);
            if (!ok) {
              errlog.format("GribParameterTable.addParameterTableLookup() can't open the file%n", filename);
            } else {
              errlog.format("GribParameterTable.addParameterTableLookup() added %s%n", filename);
            }

          } catch (Exception e) {
            errlog.format("GribParameterTable.addParameterTableLookup() error='%s'%n", e.getMessage());
          }
          break;
        }

        case "bufrtable": {
          String filename = elem.getAttributeValue("filename");
          if (filename == null) {
            errlog.format("bufrtable must have filename attribute%n");
            continue;
          }

          // reflection is used to decouple optional jars
          Class<?> bufrTablesClass;
          try {
            // only load if bufr.jar is present
            bufrTablesClass =
                RuntimeConfigParser.class.getClassLoader().loadClass("ucar.nc2.iosp.bufr.tables.BufrTables");
          } catch (Throwable e) {
            errlog.format("BufrTables was not loaded; check class path err='%s'%n", e.getCause().getMessage());
            break;
          }
          try {
            Class<?>[] params = new Class[1];
            params[0] = String.class;
            Method method = bufrTablesClass.getMethod("addLookupFile", params);
            Object[] args = new Object[1];
            args[0] = filename;
            // TODO this only loads the table, we should open it so that it fails if not present
            method.invoke(null, args); // static method has null for object
            errlog.format("BufrTables.addLookupFile() added %s%n", filename);

          } catch (Throwable e) {
            errlog.format("BufrTables.addLookupFile error='%s'%n", e.getCause().getMessage());
          }
          break;
        }

        case "Netcdf4Clibrary":
          // cdm does not have a dependency on netcdf4 (and we don't want to introduce one),
          // so we cannot refer to the Nc4Iosp.class object.
          String nc4IospClassName = "ucar.nc2.jni.netcdf.Nc4Iosp";
          /*
           * <Netcdf4Clibrary>
           * <libraryPath>/usr/local/lib</libraryPath>
           * <libraryName>netcdf</libraryName>
           * <useForReading>false</useForReading>
           * </Netcdf4Clibrary>
           */
          String path = elem.getChildText("libraryPath");
          String name = elem.getChildText("libraryName");

          if (path != null && name != null) {
            // reflection is used to decouple optional jars
            Class<?> nc4IospClass;
            try {
              nc4IospClass = RuntimeConfigParser.class.getClassLoader().loadClass(nc4IospClassName);
            } catch (Throwable e) {
              errlog.format("Nc4Iosp was not loaded='%s'; check class path%n", e.getMessage());
              break;
            }
            try {
              Method method = nc4IospClass.getMethod("setLibraryAndPath", String.class, String.class);
              method.invoke(null, path, name); // static method has null for object
            } catch (Throwable e) {
              errlog.format("Nc4Iosp.setLibraryAndPath error='%s'%n", e.getMessage());
            }
          }
          boolean useForReading = Boolean.parseBoolean(elem.getChildText("useForReading"));
          if (useForReading) {
            try {
              // Registers Nc4Iosp in front of all the other IOSPs already registered in NetcdfFile.<clinit>().
              // Crucially, this means that we'll try to open a file with Nc4Iosp before we try it with H5iosp.
              NetcdfFiles.registerIOProvider(nc4IospClassName);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
              errlog.format("Could not register IOSP '%s': %s%n", nc4IospClassName, e.getMessage());
            }
          }
          break;
      }
    }
  }
}
