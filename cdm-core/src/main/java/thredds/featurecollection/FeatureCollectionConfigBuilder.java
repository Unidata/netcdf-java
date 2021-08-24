/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.featurecollection;

import java.io.IOException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import thredds.client.catalog.Catalog;
import thredds.inventory.CollectionAbstract;
import ucar.nc2.internal.util.AliasTranslator;
import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Read the <featureCollection> element. create a FeatureCollectionConfig
 *
 * @author John
 * @since 1/17/2015
 */
public class FeatureCollectionConfigBuilder {
  Formatter errlog;
  public boolean fatalError;

  public FeatureCollectionConfigBuilder(Formatter errlog) {
    this.errlog = errlog;
  }

  // input is xml file with just the <featureCollection>
  public FeatureCollectionConfig readConfigFromFile(String filename) throws IOException {
    SAXBuilder builder = new SAXBuilder();
    builder.setExpandEntities(false);
    try {
      org.jdom2.Document doc = builder.build(filename);
      return readConfig(doc.getRootElement());
    } catch (JDOMException e) {
      throw new IOException(e);
    }
  }

  /**
   * Read a catalog and extract a FeatureCollectionConfig from it
   *
   * @param catalogAndPath catalog filename, or catalog#featureName
   * @return FeatureCollectionConfig or null
   */
  public FeatureCollectionConfig readConfigFromCatalog(String catalogAndPath) {
    String catFilename;
    String fcName = null;

    int pos = catalogAndPath.indexOf("#");
    if (pos > 0) {
      catFilename = catalogAndPath.substring(0, pos);
      fcName = catalogAndPath.substring(pos + 1);
    } else {
      catFilename = catalogAndPath;
    }

    File cat = new File(catFilename);
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      builder.setExpandEntities(false);
      doc = builder.build(cat);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    try {
      List<Element> fcElems = new ArrayList<>();
      findFeatureCollection(doc.getRootElement(), fcName, fcElems);
      if (!fcElems.isEmpty())
        return readConfig(fcElems.get(0));

    } catch (IllegalStateException e) {
      e.printStackTrace();
    }

    return null;
  }

  private void findFeatureCollection(Element parent, String name, List<Element> fcElems) {
    List<Element> elist = parent.getChildren("featureCollection", Catalog.defNS);
    if (name == null)
      fcElems.addAll(elist);
    else {
      for (Element elem : elist) {
        if (name.equals(elem.getAttributeValue("name")))
          fcElems.add(elem);
      }
    }
    for (Element child : parent.getChildren("dataset", Catalog.defNS))
      findFeatureCollection(child, name, fcElems);
  }

  public FeatureCollectionConfig readConfig(Element featureCollectionElement) {
    String name = featureCollectionElement.getAttributeValue("name");
    String path = featureCollectionElement.getAttributeValue("path");
    String fcTypeS = featureCollectionElement.getAttributeValue("featureType");

    FeatureCollectionType fcType = FeatureCollectionType.valueOf(fcTypeS);
    if (fcType == null) {
      errlog.format("featureCollection %s must have a valid FeatureCollectionType attribute, found '%s'%n", name,
          fcTypeS);
      fatalError = true;
    }

    // collection element required
    Element collElem = featureCollectionElement.getChild("collection", Catalog.defNS);
    if (collElem == null) {
      errlog.format("featureCollection %s must have a <collection> element%n", name);
      fatalError = true;
      return null;
    }
    String collectionName = collElem.getAttributeValue("name");
    collectionName = CollectionAbstract.cleanName(collectionName != null ? collectionName : name);

    String spec = collElem.getAttributeValue("spec");
    spec = expandAliasForCollectionSpec(spec);
    String timePartition = collElem.getAttributeValue("timePartition");
    String dateFormatMark = collElem.getAttributeValue("dateFormatMark");
    String olderThan = collElem.getAttributeValue("olderThan");
    String rootDir = collElem.getAttributeValue("rootDir");
    String regExp = collElem.getAttributeValue("regExp");
    if (spec == null && rootDir == null) {
      errlog.format("featureCollection " + name + " must have a spec or rootDir attribute.");
      fatalError = true;
      return null;
    }
    Element innerNcml = featureCollectionElement.getChild("netcdf", Catalog.ncmlNS);
    FeatureCollectionConfig config = new FeatureCollectionConfig(name, path, fcType, spec, collectionName,
        dateFormatMark, olderThan, timePartition, innerNcml);
    config.setFilter(rootDir, regExp);
    config.setFilesSort(featureCollectionElement.getChild("filesSort", Catalog.defNS));

    // tds and update elements
    Element tdmElem = featureCollectionElement.getChild("tdm", Catalog.defNS);
    config.tdmConfig = readUpdateElement(tdmElem);
    Element updateElem = featureCollectionElement.getChild("update", Catalog.defNS);
    config.updateConfig = readUpdateElement(updateElem);

    // protoDataset element
    Element protoElem = featureCollectionElement.getChild("protoDataset", Catalog.defNS);
    if (protoElem != null) {
      String choice = protoElem.getAttributeValue("choice");
      String change = protoElem.getAttributeValue("change");
      String param = protoElem.getAttributeValue("param");
      Element ncmlElem = protoElem.getChild("netcdf", Catalog.ncmlNS);
      config.protoConfig = new FeatureCollectionConfig.ProtoConfig(choice, change, param, ncmlElem);
    }

    // fmrcConfig element
    Element fmrcElem = featureCollectionElement.getChild("fmrcConfig", Catalog.defNS);
    if (fmrcElem != null) {
      String regularize = fmrcElem.getAttributeValue("regularize");
      config.fmrcConfig = new FeatureCollectionConfig.FmrcConfig(regularize);

      String datasetTypes = fmrcElem.getAttributeValue("datasetTypes");
      if (null != datasetTypes)
        config.fmrcConfig.addDatasetType(datasetTypes);

      List<Element> bestElemsOld = fmrcElem.getChildren("dataset", Catalog.defNS);
      for (Element best : bestElemsOld) {
        String bestName = best.getAttributeValue("name");
        String offs = best.getAttributeValue("offsetsGreaterEqual");
        double off = Double.parseDouble(offs);
        config.fmrcConfig.addBestDataset(bestName, off);
      }
      List<Element> bestElems = fmrcElem.getChildren("bestDataset", Catalog.defNS);
      for (Element best : bestElems) {
        String bestName = best.getAttributeValue("name");
        String offs = best.getAttributeValue("offsetsGreaterEqual");
        double off = Double.parseDouble(offs);
        config.fmrcConfig.addBestDataset(bestName, off);
      }
    }

    // pointConfig element optional
    Element pointElem = featureCollectionElement.getChild("pointConfig", Catalog.defNS);
    if (pointElem != null) {
      String datasetTypes = pointElem.getAttributeValue("datasetTypes");
      if (null != datasetTypes)
        config.pointConfig.addDatasetType(datasetTypes);
    }

    // gribConfig element optional
    Element gribConfig = featureCollectionElement.getChild("gribConfig", Catalog.defNS);
    if (gribConfig != null) {
      config.gribConfig.configFromXml(gribConfig, Catalog.defNS);
    }

    // done reading - do anything needed
    config.finish();

    return config;
  }

  private FeatureCollectionConfig.UpdateConfig readUpdateElement(Element updateElem) {
    if (updateElem == null) {
      return new FeatureCollectionConfig.UpdateConfig(); // default
    }

    String startup = updateElem.getAttributeValue("startup");
    String rewrite = updateElem.getAttributeValue("rewrite");
    String recheckAfter = updateElem.getAttributeValue("recheckAfter");
    String rescan = updateElem.getAttributeValue("rescan");
    String trigger = updateElem.getAttributeValue("trigger");
    String deleteAfter = updateElem.getAttributeValue("deleteAfter");

    return new FeatureCollectionConfig.UpdateConfig(startup, rewrite, recheckAfter, rescan, trigger, deleteAfter);
  }

  private String expandAliasForCollectionSpec(String location) {
    String result = AliasTranslator.translateAlias(location);
    if (result != null)
      return result;
    return location;
  }

}

