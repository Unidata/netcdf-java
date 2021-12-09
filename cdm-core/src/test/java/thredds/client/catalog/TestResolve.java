/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test relative URL resolution. */
public class TestResolve {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  String base = "http://www.unidata.ucar.edu/";
  String urlString = "TestResolvURI.1.0.xml";

  @Test
  public void testResolve() throws IOException {
    Catalog cat = ClientCatalogUtil.open(urlString);
    assertThat(cat).isNotNull();

    Service s = cat.findService("ACD");
    assertThat(s).isNotNull();
    logger.debug("ACD service= {}", s);

    assertThat(getAccessURL(cat, "nest11")).isEqualTo("http://www.acd.ucar.edu/dods/testServer/flux/CO2.nc");
    assertThat(getAccessURL(cat, "nest12")).isEqualTo(base + "netcdf/data/flux/NO2.nc");

    assertThat(getMetadataURL(cat, "nest1", "NETCDF")).isEqualTo("any.xml");
    assertThat(getMetadataURL(cat, "nest1", "ADN")).isEqualTo("http://you/corrupt.xml");

    String docUrl = getDocURL(cat, "nest1", "absolute");
    assertThat(docUrl).isEqualTo("http://www.unidata.ucar.edu/");

    docUrl = getDocURL(cat, "nest1", "relative");
    assertThat(docUrl).isEqualTo(base + "any.xml");

    assertThat(getCatref(cat.getDatasets(), "ETA data"))
        .isEqualTo("http://www.unidata.ucar.edu/projects/thredds/xml/InvCatalog5.part2.xml");
    assertThat(getCatref(cat.getDatasets(), "BETA data")).isEqualTo("/xml/InvCatalog5.part2.xml");
  }

  private String getAccessURL(Catalog cat, String name) {
    Dataset ds = cat.findDatasetByID(name);
    List list = ds.getAccess();
    assertThat(list).isNotNull();
    assertThat(list).isNotEmpty();
    Access a = (Access) list.get(0);
    logger.debug("{} = {}", name, a.getStandardUrlName());
    return a.getStandardUrlName();
  }

  private String getMetadataURL(Catalog cat, String name, String mtype) {
    Dataset ds = cat.findDatasetByID(name);
    List<ThreddsMetadata.MetadataOther> list = ds.getMetadata(mtype);
    assertThat(list).isNotNull();
    assertThat(list).isNotEmpty();
    ThreddsMetadata.MetadataOther m = list.get(0);
    assertThat(m).isNotNull();
    logger.debug("{} = {}", name, m.getXlinkHref());
    assertThat(m.getXlinkHref()).isNotNull();
    return m.getXlinkHref();
  }

  private String getDocURL(Catalog cat, String name, String title) {
    Dataset ds = cat.findDatasetByID(name);
    List<Documentation> list = ds.getDocumentation();
    assertThat(list).isNotNull();
    assertThat(list).isNotEmpty();
    for (Documentation elem : list) {
      if (elem.hasXlink() && elem.getXlinkTitle().equals(title)) {
        logger.debug("{} {} = {}", name, title, elem.getURI());
        return elem.getURI().toString();
      }
    }
    return null;
  }

  private String getCatref(List list, String name) {
    for (int i = 0; i < list.size(); i++) {
      Dataset elem = (Dataset) list.get(i);
      logger.debug("elemname = {}", elem.getName());
      if (elem.getName().equals(name)) {
        assertThat(elem).isInstanceOf(CatalogRef.class);
        CatalogRef catref = (CatalogRef) elem;
        logger.debug("{} = {}", name, catref.getXlinkHref());
        return catref.getXlinkHref();
      }
    }
    return null;
  }
}
