/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.metadata;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.ThreddsMetadata;
import thredds.client.catalog.TimeCoverage;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.client.catalog.tools.CatalogXmlWriter;
import thredds.client.catalog.tools.ThreddsMetadataAcdd;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test MetadataExtractor and MetadataExtractorAcdd */
@Category(NeedsCdmUnitTest.class)
public class TestMetadataExtractor {

  @Test
  public void testMetadataExtractorAcdd() throws IOException {
    String filename =
        TestDir.cdmUnitTestDir + "agg/pointFeatureCollection/netCDFbuoydata/BOD001_000_20050627_20051109.nc";
    NetcdfFile ncfile = NetcdfFiles.open(filename);

    CatalogBuilder catb = new CatalogBuilder();
    DatasetBuilder dsb = new DatasetBuilder(null);
    dsb.setName("testAcdd");
    dsb.put(Dataset.Id, "testAcdd");
    ThreddsMetadataAcdd acdd = new ThreddsMetadataAcdd(makeMap(ncfile.getRootGroup().attributes()), dsb);
    acdd.extract();
    ncfile.close();

    catb.addDataset(dsb);
    Catalog cat = catb.makeCatalog();

    CatalogXmlWriter writer = new CatalogXmlWriter();
    writer.writeXML(cat, System.out, false);

    Dataset ds = cat.findDatasetByID("testAcdd");
    assertThat(ds).isNotNull();
    assertThat(FeatureType.STATION).isEqualTo(ds.getFeatureType());

    assertThat(ds.getDocumentation("title")).startsWith("Seawater temperature data collected from Bodega Head");
    assertThat(ds.getDocumentation("summary")).startsWith("These seawater data are collected");
    assertThat(ds.getDocumentation("history")).startsWith("2012-03-06 09:41:58 UTC:");
    assertThat(ds.getDocumentation("comment")).startsWith("Supplementary information: West Coast Observing");
    assertThat(ds.getDocumentation("rights")).startsWith("Please cite Gulf of the Farallones National");

    TimeCoverage tc = ds.getTimeCoverageNew();
    assertThat(tc).isNotNull();
    assertThat(tc.getStart().toCalendarDate())
        .isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2005-06-27T21:48:00").orElseThrow());
    assertThat(tc.getEnd().toCalendarDate())
        .isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2005-11-09T00:53:59").orElseThrow());

    ThreddsMetadata.GeospatialCoverage geo = ds.getGeospatialCoverage();
    assertThat(geo).isNotNull();
    assertThat(geo.getEastWestRange().getSize()).isEqualTo(0.0);
    assertThat(geo.getNorthSouthRange().getSize()).isEqualTo(0.0);

    List<ThreddsMetadata.Source> creators = ds.getCreators();
    assertThat(creators).hasSize(1);
    ThreddsMetadata.Source creator = creators.get(0);
    assertThat(creator.getName()).startsWith("Data Manager");
    assertThat(creator.getEmail()).isEqualTo("Data Manager (bmldata@ucdavis.edu)");
    assertThat(creator.getUrl()).isEqualTo("http://bml.ucdavis.edu");

    List<ThreddsMetadata.Source> publishers = ds.getPublishers();
    assertThat(publishers).hasSize(1);
    ThreddsMetadata.Source pub = publishers.get(0);
    assertThat(pub.getName()).startsWith("US NATIONAL OCEANOGRAPHIC DATA CENTER");
    assertThat(pub.getEmail()).isEqualTo("NODC.Services@noaa.gov");
    assertThat(pub.getUrl()).isEqualTo("http://www.nodc.noaa.gov/");

    List<ThreddsMetadata.Vocab> vocab = ds.getKeywords();
    assertThat(vocab).hasSize(5);
    ThreddsMetadata.Vocab voc = vocab.get(0);
    assertThat(voc.getVocabulary()).startsWith("GCMD Earth Science Keywords. Version 5.3.3");
  }

  public static Map<String, Attribute> makeMap(Iterable<Attribute> atts) {
    Map<String, Attribute> result = new HashMap<>();
    if (atts == null) {
      return result;
    }
    for (Attribute att : atts) {
      result.put(att.getShortName(), att);
    }
    return result;
  }
}
