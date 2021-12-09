/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Before;
import org.junit.Test;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.TestDir;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test ClientCatalog inheritence. */
public class TestClientCatalogInherit {
  private static final String urlString = "file:" + TestDir.cdmLocalTestDataDir + "thredds/catalog/TestInherit.1.0.xml";
  private Catalog cat;

  @Before
  public void openCatalog() {
    CatalogBuilder builder = new CatalogBuilder();
    cat = builder.buildFromLocation(urlString, null);
    if (builder.hasFatalError()) {
      System.out.printf("ERRORS %s%n", builder.getErrorMessage());
    }
  }

  @Test
  public void testPropertyInherit() {
    Dataset top = cat.findDatasetByID("top");
    String val = top.findProperty("GoodThing");
    assertThat(val).isNull();

    Dataset nest1 = cat.findDatasetByID("nest1");
    val = nest1.findProperty("GoodThing");
    assertThat(val).isNotNull();
    assertThat(val).isEqualTo("Where have you gone?");

    Dataset nest2 = cat.findDatasetByID("nest2");
    val = nest2.findProperty("GoodThing");
    assertThat(val).isNull();

    Dataset nest11 = cat.findDatasetByID("nest11");
    val = nest11.findProperty("GoodThing");
    assertThat(val).isNotNull();
    assertThat(val).isEqualTo("Where have you gone?");

    Dataset nest12 = cat.findDatasetByID("nest12");
    assertThat(nest12.hasProperty(new Property("GoodThing", "override"))).isTrue();
  }

  @Test
  public void testServiceInherit() {
    Dataset ds;
    Service s;
    String val;

    ds = cat.findDatasetByID("top");
    s = ds.getServiceDefault();
    assertThat(s).isNull();

    ds = cat.findDatasetByID("nest1");
    s = ds.getServiceDefault();
    assertThat(s).isNotNull();
    val = s.getName();
    assertThat(val).isNotNull();
    assertThat(val).isEqualTo("ACD");

    ds = cat.findDatasetByID("nest11");
    s = ds.getServiceDefault();
    assertThat(s).isNotNull();
    val = s.getName();
    assertThat(val).isNotNull();
    assertThat(val).isEqualTo("ACD");

    ds = cat.findDatasetByID("nest12");
    s = ds.getServiceDefault();
    assertThat(s).isNotNull();
    val = s.getName();
    assertThat(val).isNotNull();
    assertThat(val).isEqualTo("local");

    ds = cat.findDatasetByID("nest121");
    s = ds.getServiceDefault();
    assertThat(s).isNotNull();
    val = s.getName();
    assertThat(val).isNotNull();
    assertThat(val).isEqualTo("ACD");

    ds = cat.findDatasetByID("nest2");
    s = ds.getServiceDefault();
    assertThat(s).isNull();
  }

  @Test
  public void testdataTypeInherit() {
    Dataset ds;
    FeatureType s;

    ds = cat.findDatasetByID("top");
    s = ds.getFeatureType();
    assertThat(s).isNull();

    ds = cat.findDatasetByID("nest1");
    s = ds.getFeatureType();
    assertThat(s).isEqualTo(FeatureType.GRID);

    ds = cat.findDatasetByID("nest11");
    s = ds.getFeatureType();
    assertThat(s).isEqualTo(FeatureType.GRID);

    ds = cat.findDatasetByID("nest12");
    s = ds.getFeatureType();
    assertThat(s.toString()).isEqualTo("IMAGE");

    ds = cat.findDatasetByID("nest121");
    s = ds.getFeatureType();
    assertThat(s).isEqualTo(FeatureType.GRID);

    ds = cat.findDatasetByID("nest2");
    s = ds.getFeatureType();
    assertThat(s).isNull();
  }

  @Test
  public void testAuthorityInherit() {
    Dataset ds;
    String val;

    ds = cat.findDatasetByID("top");
    val = ds.getAuthority();
    assertThat(val).isEqualTo("ucar");

    ds = cat.findDatasetByID("nest1");
    val = ds.getAuthority();
    assertThat(val).isEqualTo("divine");

    ds = cat.findDatasetByID("nest11");
    val = ds.getAuthority();
    assertThat(val).isEqualTo("divine");

    ds = cat.findDatasetByID("nest12");
    val = ds.getAuthority();
    assertThat(val).isEqualTo("human");

    ds = cat.findDatasetByID("nest121");
    val = ds.getAuthority();
    assertThat(val).isEqualTo("divine");

    ds = cat.findDatasetByID("nest2");
    val = ds.getAuthority();
    assertThat(val).isNull();
  }

  @Test
  public void testMetadataInherit() {
    Dataset ds;
    List list;

    ds = cat.findDatasetByID("top");
    list = ds.getMetadata("NetCDF");
    assertThat(list).isEmpty();

    ds = cat.findDatasetByID("nest1");
    list = ds.getMetadata("NetCDF");
    assertThat(list).hasSize(1);
    ThreddsMetadata.MetadataOther m = (ThreddsMetadata.MetadataOther) list.get(0);
    assertThat(m).isNotNull();

    ds = cat.findDatasetByID("nest11");
    list = ds.getMetadata("NetCDF");
    assertThat(list).isEmpty();

    ds = cat.findDatasetByID("nest12");
    list = ds.getMetadata("NetCDF");
    assertThat(list).isEmpty();

    ds = cat.findDatasetByID("nest121");
    list = ds.getMetadata("NetCDF");
    assertThat(list).isEmpty();

    ds = cat.findDatasetByID("nest2");
    list = ds.getMetadata("NetCDF");
    assertThat(list).isEmpty();
  }

  @Test
  public void testDocInherit() {
    Dataset ds;
    List list;
    Documentation d;

    ds = cat.findDatasetByID("top");
    assertThat(ds).isNotNull();
    list = ds.getDocumentation();
    assertThat(list).isEmpty();

    ds = cat.findDatasetByID("nest1");
    assertThat(ds).isNotNull();
    list = ds.getDocumentation();
    d = (Documentation) list.get(0);
    assertThat(d).isNotNull();
    assertThat(d.getInlineContent()).isEqualTo("HEY");

    ds = cat.findDatasetByID("nest11");
    assertThat(ds).isNotNull();
    list = ds.getDocumentation();
    assertThat(list).isEmpty();

    ds = cat.findDatasetByID("nest2");
    assertThat(ds).isNotNull();
    list = ds.getDocumentation();
    assertThat(list).isEmpty();
  }
}
