/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.tools.CatalogXmlWriter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.units.TimeUnit;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for client catalogs
 */
public class TestClientCatalog {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testResolve() throws IOException {
    Catalog cat = ClientCatalogUtil.open("testCatref.xml");
    Assert.assertEquals("catrefURI", ClientCatalogUtil.makeFilepath("test2.xml"),
        getCatrefURI(cat.getDatasets(), "catref"));

    String catrefURIn = getCatrefNestedURI(cat, "top", "catref-nested");
    assertThat(catrefURIn).isEqualTo((ClientCatalogUtil.makeFilepath("test0.xml")));
  }

  private CatalogRef getCatrefNested(Catalog cat, String id, String catName) {
    Dataset ds = cat.findDatasetByID(id);
    assertThat(ds).isNotNull();
    return getCatref(ds.getDatasets(), catName);
  }

  private CatalogRef getCatref(List<Dataset> list, String name) {
    for (Dataset ds : list) {
      if (ds.getName().equals(name)) {
        assertThat(ds).isInstanceOf(CatalogRef.class);
        CatalogRef catref = (CatalogRef) ds;
        logger.debug("{} = {} == {}", name, catref.getXlinkHref(), catref.getURI());
        return catref;
      }
    }
    return null;
  }

  private String getCatrefURI(List<Dataset> list, String name) {
    CatalogRef catref = getCatref(list, name);
    if (catref != null)
      return catref.getURI().toString();
    return null;
  }

  private String getCatrefNestedURI(Catalog cat, String id, String catName) {
    return getCatrefNested(cat, id, catName).getURI().toString();
  }


  @Test
  public void testDeferredRead() throws IOException {
    Catalog cat = ClientCatalogUtil.open("testCatref.xml");

    CatalogRef catref = getCatref(cat.getDatasets(), "catref");
    assertThat(catref.isRead()).isFalse();

    catref = getCatrefNested(cat, "top", "catref-nested");
    assertThat(catref.isRead()).isFalse();
  }

  ////////////////////////

  @Test
  public void testNested() throws IOException {
    Catalog cat = ClientCatalogUtil.open("nestedServices.xml");
    assertThat(cat).isNotNull();

    Dataset ds = cat.findDatasetByID("top");
    assertThat(ds).isNotNull();
    assertThat(ds.getServiceDefault()).isNotNull();

    ds = cat.findDatasetByID("nest1");
    assertThat(ds).isNotNull();
    assertThat(ds.getServiceDefault()).isNotNull();

    ds = cat.findDatasetByID("nest2");
    assertThat(ds).isNotNull();
    assertThat(ds.getServiceDefault()).isNotNull();

    logger.debug("OK");
  }

  ////////////////////////////

  @Test
  public void testGC() throws Exception {
    Catalog cat = ClientCatalogUtil.open("MissingGCProblem.xml");
    assertThat(cat).isNotNull();

    Dataset ds = cat.findDatasetByID("hasGC");
    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    assertThat(gc).isNotNull();
    assertThat(gc.getHeightStart()).isEqualTo(5.0);
    assertThat(gc.getHeightExtent()).isEqualTo(47.0);

    assertThat(gc.getEastWestRange()).isNull();
    assertThat(gc.getNorthSouthRange()).isNull();
  }

  @Test
  public void testTimeCoverage() throws Exception {
    Catalog cat = ClientCatalogUtil.open("TestTimeCoverage.xml");
    assertThat(cat).isNotNull();

    Dataset ds = cat.findDatasetByID("test1");
    TimeCoverage tc = ds.getTimeCoverageNew();
    assertThat(tc).isNotNull();
    logger.debug("tc = {}", tc);
    assertThat(tc.getEnd().isPresent()).isTrue();
    assertThat(tc.getResolution()).isNull();
    assertThat(tc.getDuration()).isEqualTo(TimeDuration.parse("14 days"));

    ds = cat.findDatasetByID("test2");
    tc = ds.getTimeCoverageNew();
    assertThat(tc).isNotNull();
    logger.debug("tc = {}", tc);
    CalendarDate got = tc.getStart().getCalendarDate();
    CalendarDate want = CalendarDateFormatter.isoStringToCalendarDate(null, "1999-11-16T12:00:00");
    assertThat(got).isEqualTo(want);
    assertThat(tc.getResolution()).isNull();
    TimeDuration gott = tc.getDuration();
    TimeDuration wantt = TimeDuration.parse("P3M");
    assertThat(gott).isEqualTo(wantt);

    ds = cat.findDatasetByID("test3");
    tc = ds.getTimeCoverageNew();
    assertThat(tc).isNotNull();
    logger.debug("tc = {}", tc);
    assertThat(tc.getResolution()).isNull();
    assertThat(tc.getDuration()).isEqualTo(TimeDuration.parse("48.0 hours"));

    ds = cat.findDatasetByID("test4");
    tc = ds.getTimeCoverageNew();
    assertThat(tc).isNotNull();
    logger.debug("tc = {}", tc);
    TimeDuration r = tc.getResolution();
    assertThat(r).isNotNull();
    TimeDuration r2 = TimeDuration.parse("3 hour");
    assertThat(r).isEqualTo(r2);
    TimeDuration d = tc.getDuration();
    TimeUnit tu = d.getTimeUnit();
    assertThat(tu.getUnitString()).isEqualTo("days"); // LOOK should be 3 hours, or hours or ??

    ds = cat.findDatasetByID("test5");
    tc = ds.getTimeCoverageNew();
    assertThat(tc).isNotNull();
    logger.debug("tc = {}", tc);

    CalendarDate start = tc.getStart().getCalendarDate();
    assertThat(start.getCalendar()).isEqualTo(Calendar.uniform30day); // Using non-default calendar.

    // This date is valid in the uniform30day calendar. If we tried it with the standard calendar, we'd get an error:
    // Illegal base time specification: '2017-02-30' Value 30 for dayOfMonth must be in the range [1,28]
    assertThat(CalendarDateFormatter.toDateString(start)).isEqualTo("2017-02-30");

    CalendarDate end = tc.getEnd().getCalendarDate();
    assertThat(CalendarDateFormatter.toDateString(end)).isEqualTo("2017-04-01");

    // In the uniform30day calendar, the difference between 2017-02-30 and 2017-04-01 is 31 days.
    assertThat(end.getDifference(start, CalendarPeriod.Field.Day)).isEqualTo(31);
  }

  /////////////

  @Test
  public void testVariables() throws IOException {
    Catalog cat = ClientCatalogUtil.open("TestHarvest.xml");
    assertThat(cat).isNotNull();

    Dataset ds = cat.findDatasetByID("solve1.dc8");
    assertThat(ds).isNotNull();

    List<ThreddsMetadata.VariableGroup> list = ds.getVariables();
    assertThat(list).isNotNull();
    assertThat(list.size()).isGreaterThan(1);

    ThreddsMetadata.VariableGroup vars = getType(list, "CF-1.0");
    assertThat(vars).isNotNull();
    checkVariable(vars, "wv", "Wind Speed");
    checkVariable(vars, "o3c", "Ozone Concentration");

    ThreddsMetadata.VariableGroup dif = getType(list, "DIF");
    assertThat(dif).isNotNull();
    checkVariable(dif, "wind_from_direction",
        "EARTH SCIENCE > Atmosphere > Atmosphere Winds > Surface Winds > wind_from_direction");
  }

  ThreddsMetadata.VariableGroup getType(List<ThreddsMetadata.VariableGroup> list, String type) {
    for (ThreddsMetadata.VariableGroup vars : list) {
      if (vars.getVocabulary().equals(type))
        return vars;
    }
    return null;
  }

  void checkVariable(ThreddsMetadata.VariableGroup vars, String name, String vname) {
    List<ThreddsMetadata.Variable> list = vars.getVariableList();
    for (ThreddsMetadata.Variable var : list) {
      if (var.getName().equals(name)) {
        assertThat(var.getVocabularyName()).isEqualTo(vname);
        return;
      }
    }
    fail();
  }

  /////////////////

  @Test
  public void testSubset() throws IOException {
    Catalog cat = ClientCatalogUtil.open("InvCatalog-1.0.xml");
    CatalogXmlWriter writer = new CatalogXmlWriter();
    logger.debug("{}", writer.writeXML(cat));

    Dataset ds = cat.findDatasetByID("testSubset");
    assertThat(ds).isNotNull();
    assertThat(ds.getFeatureType()).isEqualTo(FeatureType.GRID);

    Catalog subsetCat = cat.subsetCatalogOnDataset(ds);
    logger.debug("{}", writer.writeXML(subsetCat));

    List<Dataset> dss = subsetCat.getDatasets();
    assertThat(dss.size()).isEqualTo(1);
    Dataset first = dss.get(0);
    assertThat(first.getServiceNameDefault()).isNotNull();
    assertThat(first.getServiceNameDefault()).isEqualTo("ACD");

    Dataset subsetDs = subsetCat.findDatasetByID("testSubset");
    assertThat(subsetDs).isNotNull();
    assertThat(subsetDs.getServiceNameDefault()).isNotNull();
    assertThat(subsetDs.getServiceNameDefault()).isEqualTo("ACD");
    assertThat(subsetDs.getFeatureTypeName()).isNotNull();
    assertThat(subsetDs.getFeatureTypeName().equalsIgnoreCase("Grid")).isTrue();
  }
}
