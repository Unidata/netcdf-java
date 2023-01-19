/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog.builder;

import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;


/**
 * Unit tests for catalog builder port validation
 */
public class TestCatalogBuilder {

  public static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  CatalogBuilder catalogBuilder = new CatalogBuilder();
  String host = "https://thredds-test.unidata.ucar.edu";
  String catalog = "/thredds/catalog/grib/NCEP/GFS/Global_0p25deg_ana/latest.xml";

  @Test
  public void testValidationNoPort() throws URISyntaxException {
    // no port specified; should be -1
    URI testURI = new URI(host + catalog);
    Assert.assertEquals(Long.valueOf(-1), Long.valueOf(testURI.getPort()));

    // no port specified; -1 is OK for port validation
    catalogBuilder.buildFromURI(testURI);
    Assert.assertFalse(catalogBuilder.hasFatalError());
  }

  @Test
  public void testValidationValidRootPort() throws URISyntaxException {
    String port = "443";
    // valid root privileged port specified
    URI testURI = new URI(host + ":" + port + catalog);
    Assert.assertEquals(Long.valueOf(port), Long.valueOf(testURI.getPort()));

    // 443 is OK for port validation
    catalogBuilder.buildFromURI(testURI);
    Assert.assertFalse(catalogBuilder.hasFatalError());
  }

  @Test
  public void testValidationInvalidRootPort() throws URISyntaxException {
    String port = "22";
    // invalid root privileged port specified
    URI testURI = new URI(host + ":" + port + catalog);
    Assert.assertEquals(Long.valueOf(port), Long.valueOf(testURI.getPort()));

    // fails port validation
    catalogBuilder.buildFromURI(testURI);
    Assert.assertTrue(catalogBuilder.hasFatalError());
  }

  @Test
  public void testValidationInvalidNonRootPort() throws URISyntaxException {
    String port = "8081";
    // valid non-root privileged port specified
    URI testURI = new URI(host + ":" + port + catalog);
    Assert.assertEquals(Long.valueOf(port), Long.valueOf(testURI.getPort()));

    // non-root privileged port is OK for port validation, but will fail against Unidata TDS server
    catalogBuilder.buildFromURI(testURI);
    Assert.assertTrue(catalogBuilder.hasFatalError());
  }
 }