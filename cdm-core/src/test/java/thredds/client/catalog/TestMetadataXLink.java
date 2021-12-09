/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Test;
import thredds.client.catalog.tools.CatalogXmlWriter;
import java.io.IOException;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test metadata element has XLink
 * see https://www.unidata.ucar.edu/software/thredds/v4.6/tds/catalog/InvCatalogSpec.html#metadataElement
 */
public class TestMetadataXLink {

  /*
   * <?xml version="1.0" encoding="UTF-8"?>
   * <catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
   * xmlns:xlink="http://www.w3.org/1999/xlink" name="Unidata THREDDS-IDD Server" version="1.0.7">
   * <service name="this" serviceType="QueryCapability" base="" />
   * <dataset name="Unidata THREDDS-IDD Server">
   * <metadata inherited="true">
   * <authority>unidata.ucar.edu:</authority>
   * </metadata>
   * <dataset name="Realtime data from IDD">
   * <catalogRef xlink:href="http://motherlode.ucar.edu:8080/cataloggen/cataloggen/catalogs/uniModels.xml"
   * xlink:title="NCEP Model Data" name="" />
   * <dataset name="Satellite Images from NOAAPort and Unidata/Wisconsin Data Streams">
   * <dataType>Image</dataType>
   * <catalogRef xlink:href="adde/motherlode/catalog.xml" xlink:title="Catalog" name="" />
   * <dataset name="Dataset Query Capability" urlPath="adde/motherlode/dqc.xml">
   * <serviceName>this</serviceName>
   * <dataType>Image</dataType>
   * </dataset>
   * </dataset>
   * <dataset name="Station data">
   * <metadata inherited="true">
   * <serviceName>this</serviceName>
   * <dataType>Station</dataType>
   * </metadata>
   * <dataset name="Metar data" ID="Metars" urlPath="ldm/MetarDQC.xml">
   * <project vocabulary="DIF">test1</project>
   * </dataset>
   * <dataset name="Level 3 Radar data" ID="Radars" urlPath="ldm/RadarDQC.xml">
   * <project vocabulary="DIF">test2</project>
   * </dataset>
   * <dataset name="Profiler data" urlPath="ldm/ProfilerDQC.xml" />
   * <dataset name="Upperair data" urlPath="ldm/UpperairDQC.xml" />
   * <dataset name="Synoptic data" urlPath="ldm/SynopticDQC.xml" />
   * <dataset name="Buoy data" urlPath="ldm/BuoyDQC.xml" />
   * <dataset name="Zonal data" ID="Zonal" urlPath="ldm/ZoneDQC.xml">
   * <documentation type="summary">The SAGE III Ozone Loss and Validation Experiment (SOLVE) was a measurement campaign
   * designed to examine the processes controlling ozone levels at mid- to high latitudes. Measurements were made in the
   * Arctic high-latitude region in winter using the NASA DC-8 and ER-2 aircraft, as well as balloon platforms and
   * ground-based instruments.</documentation>
   * <documentation type="rights">Users of these data files are expected to follow the NASA ESPO Archive guidelines for
   * use of the SOLVE data, including consulting with the PIs of the individual measurements for interpretation and
   * credit.</documentation>
   * <keyword>Ocean Biomass</keyword>
   * <keyword vocabulary="DIF-Discipline">Earth Science</keyword>
   * <project vocabulary="DIF">NASA Earth Science Project Office, Ames Research Center</project>
   * </dataset>
   * </dataset>
   * <dataset name="A comparative study on floral ecology between Malaysia and Antarctica" ID="ASAC_2372">
   * <documentation type="summary">This project aims to determine the physiological adaptations of algae to the extreme
   * conditions of Antarctica. The major aim is to understand the effect of global changes such as temperature and
   * ultraviolet radiation increases arising from the Antarctic ozone hole and to detect environmental changes in the
   * Antarctic due to human activities.</documentation>
   * <contributor role="Investigator">Mashor Mashnor</contributor>
   * <keyword>ANTARTICA</keyword>
   * <keyword>ECOLOGY</keyword>
   * <keyword>FLORA</keyword>
   * <publisher>
   * <name vocabulary="DIF">Australian Antarctic Data Centre, Australia</name>
   * <contact url="http://www.aad.gov.au/default.asp?casid=3786" email="metadata@aad.gov.au" />
   * </publisher>
   * <variables vocabulary="DIF">
   * <variable name="species"
   * vocabulary_name="EARTH SCIENCE &gt; BIOSPHERE &gt; ECOLOGICAL DYNAMICS &gt; VEGETATION SPECIES" />
   * <variable name="structure"
   * vocabulary_name="EARTH SCIENCE &gt; BIOSPHERE &gt; ECOLOGICAL DYNAMICS &gt; COMMUNITY STRUCTURE" />
   * </variables>
   * </dataset>
   * </dataset>
   * <dataset name="Case Studies">
   * <catalogRef xlink:href="casestudy/vgeeCatalog.xml" xlink:title="Data for VGEE Curricula" name="" />
   * <catalogRef xlink:href="casestudy/ccs034Catalog.xml" xlink:title="Data for Comet CaseStudy 034" name="" />
   * </dataset>
   * </dataset>
   * </catalog>
   */

  @Test
  public void testXLink() throws IOException {
    Catalog cat = ClientCatalogUtil.open("catalogDev.xml");
    assertThat(cat).isNotNull();

    CatalogXmlWriter writer = new CatalogXmlWriter();
    writer.writeXML(cat, System.out);

    getProject(cat, "Metars", "DIF", "test1");
    getProject(cat, "Radars", "DIF", "test2");

    // XLink content
    getProject(cat, "Zonal", "DIF", "NASA Earth Science Project Office, Ames Research Center");
    getKeyword(cat, "Zonal", null, "Ocean Biomass");
    getKeyword(cat, "Zonal", "DIF-Discipline", "Earth Science");
  }

  private String getMetadataURL(Catalog cat, String name, String mtype) {
    Dataset ds = cat.findDatasetByID(name);
    assertThat(ds).isNotNull();
    List<ThreddsMetadata.MetadataOther> list = ds.getMetadata(mtype);
    assertThat(list).isNotNull();
    assertThat(list).isNotEmpty();
    ThreddsMetadata.MetadataOther m = list.get(0);
    assertThat(m).isNotNull();
    System.out.println(name + " = " + m.getXlinkHref());
    assertThat(m.getXlinkHref()).isNotNull();
    return m.getXlinkHref();
  }

  private void getProject(Catalog cat, String datasetId, String vocab, String text) {
    Dataset ds = cat.findDatasetByID(datasetId);
    assertThat(ds).isNotNull();
    List<ThreddsMetadata.Vocab> projects = ds.getProjects();
    assertThat(projects).isNotNull();
    assertThat(projects).isNotEmpty();

    ThreddsMetadata.Vocab p = projects.get(0);
    assertThat(p).isNotNull();
    assertThat(p.getVocabulary()).isEqualTo(vocab);
    assertThat(p.getText()).isEqualTo(text);
  }

  private void getKeyword(Catalog cat, String datasetId, String vocab, String text) {
    Dataset ds = cat.findDatasetByID(datasetId);
    assertThat(ds).isNotNull();
    List<ThreddsMetadata.Vocab> list = ds.getKeywords();
    assertThat(list).isNotNull();
    assertThat(list).isNotEmpty();
    for (ThreddsMetadata.Vocab keyword : list) {
      if (vocab == null) {
        if ((keyword.getVocabulary() == null) && keyword.getText().equals(text)) {
          return;
        }
      } else {
        if ((keyword.getVocabulary() != null) && keyword.getVocabulary().equals(vocab)
            && keyword.getText().equals(text)) {
          return;
        }
      }
    }
    assert false : "cant find keyword " + text + " vocab " + vocab;
  }

  @Test
  public void testNamespaces() throws IOException {
    Catalog cat = ClientCatalogUtil.open("testMetadata.xml");
    assertThat(cat).isNotNull();

    ThreddsMetadata.MetadataOther m = getMetadataByNamespace(cat, "solve", "somethingdifferent");
    assertThat(m.isInherited()).isFalse();
    assertThat(m.getXlinkHref()).isNull();
    assertThat(m.getContentObject()).isNotNull();

    m = getMetadataByType(cat, "solve", "ADN");
    assertThat(m.isInherited()).isFalse();
    assertThat(m.getXlinkHref()).isNotNull();

    m = getMetadataByType(cat, "solve", "DIF");
    assertThat(m.isInherited()).isFalse();
    assertThat(m.getXlinkHref()).isNotNull();
  }

  private ThreddsMetadata.MetadataOther getMetadataByType(Catalog cat, String name, String mtype) {
    Dataset ds = cat.findDatasetByID(name);
    assertThat(ds).isNotNull();
    List<ThreddsMetadata.MetadataOther> list = ds.getMetadata(mtype);
    assertThat(list).isNotNull();
    assertThat(list).isNotEmpty();
    ThreddsMetadata.MetadataOther m = list.get(0);
    assertThat(m).isNotNull();
    return m;
  }

  public ThreddsMetadata.MetadataOther getMetadataByNamespace(Catalog cat, String name, String wantNs) {
    Dataset ds = cat.findDatasetByID(name);
    assertThat(ds).isNotNull();
    List<ThreddsMetadata.MetadataOther> mlist = ds.getMetadataOther();
    assertThat(mlist).isNotNull();
    assertThat(mlist).isNotEmpty();

    for (ThreddsMetadata.MetadataOther m : mlist) {
      String ns = m.getNamespaceURI();
      if (ns.equals(wantNs)) {
        return m;
      }
    }
    return null;
  }
}
