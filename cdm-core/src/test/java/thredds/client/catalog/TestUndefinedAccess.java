/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;


import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

public class TestUndefinedAccess {

  private static final String urlString = "file:" + TestDir.cdmLocalTestDataDir + "thredds/catalog/BadAccess.xml";

  @Test
  public void testServiceGoodAccessBad() throws IOException {
    Catalog catalog = ClientCatalogUtil.open(urlString);
    assertThat(catalog).isNotNull();
    Dataset ds = catalog.findDatasetByID("gov.noaa.ncdc:C00833_ersstv3Agg");
    Service service = ds.getServiceDefault();
    assertThat(service).isNotNull();
    assertThat(service.getName()).isEqualTo("AGG");
    List<Service> nestedServices = service.getNestedServices();
    assertThat(nestedServices).isNotNull();
    assertThat(nestedServices).isNotEmpty();
    int expectedNumberOfServices = nestedServices.size();
    List<Access> allAccessMethods = ds.getAccess();
    // AGG is a Compound service with 7 services
    assertThat(allAccessMethods).hasSize(expectedNumberOfServices);
  }
}
