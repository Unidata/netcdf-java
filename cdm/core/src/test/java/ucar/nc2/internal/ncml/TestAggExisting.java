/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.time.CalendarPeriod.Field;
import ucar.unidata.util.test.Assert2;

/** Test TestNcml - AggExisting in the JUnit framework. */
public class TestAggExisting {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testNcmlDataset() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggExisting.xml";

    NetcdfFile ncfile = NetcdfDatasets.openDataset(filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test
  public void testNcmlDatasetNoProtocolInFilename() throws IOException, InvalidRangeException {
    String filename = "./" + TestNcmlRead.topDir + "aggExisting.xml";

    NetcdfFile ncfile = NetcdfDatasets.openDataset(filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test(expected = IOException.class)
  public void testNcmlDatasetNoProtocolInNcmlAbsPath() throws IOException, InvalidRangeException {
    // if using an absolute path in the NcML file location attr of the element netcdf, then
    // you must prepend file:
    // this should fail with an IOException
    String filename = "file:./" + TestNcmlRead.topDir + "exclude/aggExisting6.xml";

    NetcdfFile ncfile = NetcdfDatasets.openDataset(filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test(expected = IOException.class)
  public void testNcmlDatasetNoProtocolInFilenameOrNcmlAbsPath() throws IOException, InvalidRangeException {
    // if using an absolute path in the NcML file location attr of the element netcdf, then
    // you must prepend file:
    // this should fail with an IOException
    String filename = "./" + TestNcmlRead.topDir + "exclude/aggExisting6.xml";

    NetcdfFile ncfile = NetcdfDatasets.openDataset(filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test
  public void testNcmlDatasetNoProtocolInNcmlRelPath() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggExisting7.xml";

    NetcdfFile ncfile = NetcdfDatasets.openDataset(filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test
  public void testNcmlDatasetNoProtocolInFilenameOrNcmlRelPath() throws IOException, InvalidRangeException {
    String filename = "./" + TestNcmlRead.topDir + "aggExisting7.xml";

    NetcdfFile ncfile = NetcdfDatasets.openDataset(filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test
  public void testNcmlDatasetWcoords() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggExistingWcoords.xml";

    NetcdfFile ncfile = NetcdfDatasets.openDataset(filename, true, null);
    logger.debug(" testNcmlDatasetWcoords.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
    logger.debug(" testNcmlDatasetWcoords.closed ");
  }

  // remove test - now we get a coordinate initialized to missing data, but at least testCoordsAdded works!
  // @Test
  public void testNoCoords() throws IOException {
    String filename = "file:./" + TestNcmlRead.topDir + "exclude/aggExistingNoCoords.xml";
    logger.debug("{}", filename);
    NetcdfDataset ncd = null;

    try {
      ncd = NetcdfDatasets.openDataset(filename, true, null);
      Variable time = ncd.getRootGroup().findVariableLocal("time");
      Array data = time.read();
      // all missing
      // assert data.getInt(0) ==
    } finally {
      if (ncd != null)
        ncd.close();
    }
    // logger.debug("{}", ncd);
    // assert false;
  }

  // LOOK this test expects an Exception, but it seems to work. Why isnt this test failing in travis?
  // @Test
  public void testNoCoordsDir() throws IOException {
    String filename = "file:./" + TestNcmlRead.topDir + "exclude/aggExistingNoCoordsDir.xml";

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(filename, true, null)) {
      System.out.printf("testNoCoordsDir supposed to fail = %s", ncd);
      assert false;
    } catch (Exception e) {
      // expect an Exception
      assert true;
    }
  }

  @Test
  public void testCoordsAdded() throws IOException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggExistingAddCoord.ncml";
    logger.debug("{}", filename);
    NetcdfDataset ncd = null;

    try {
      ncd = NetcdfDatasets.openDataset(filename, true, null);
      logger.debug("{}", ncd);
    } finally {
      if (ncd != null)
        ncd.close();
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testNcmlAggInequivalentCals() throws IOException {
    // Tests that an aggregation with inequivalent calendars across the individual datasets will fail
    String filename = "file:./" + TestNcmlRead.topDir + "agg_with_calendar/aggExistingInequivalentCals.xml";
    NcmlReader.readNcml(filename, null, null);
  }

  @Test
  public void testNcmlAggExistingNoCal() throws IOException {
    // no calendar attribute in the aggregation, which should default to using proleptic_gregorian
    String filename = "file:./" + TestNcmlRead.topDir + "agg_with_calendar/aggExistingNoCal.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);
    Variable timeVar = ncfile.findVariable("time");
    assert timeVar != null;
    int[] shape = timeVar.getShape();
    assertThat(shape.length).isEqualTo(1);

    Array vals = timeVar.read();
    assertThat(vals.getInt(0)).isEqualTo(0);
    timeVar.getShape();
    assertThat(vals.getInt(shape[0] - 1)).isEqualTo(365 * 2 - 1);

    // check start and end dates of aggregation
    String expectedStart = "2017-01-01T00:00:00";
    String expectedEnd = "2018-12-31T00:00:00";
    testStartAndEnd(timeVar, expectedStart, expectedEnd);

    // the difference between two sequential CalendarDate objects should be 1 day
    testTimeDelta(timeVar, new TimeDelta(1, Field.Day));

    // If anyone ever decides to change the default calendar, this should give them pause
    Attribute calAttr = timeVar.findAttributeIgnoreCase(CF.CALENDAR);
    assertThat(calAttr).isNotNull();
    String calName = calAttr.getStringValue();
    assertThat(calName).ignoringCase().isEqualTo("proleptic_gregorian");

    ncfile.close();
  }

  @Test
  public void testNcmlAggExistingNoLeapCal() throws IOException {
    // with calendar = noleap, each year should have 365 days, even in a leap years
    String filename = "file:./" + TestNcmlRead.topDir + "agg_with_calendar/aggExistingNoLeapCal.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);
    Variable timeVar = ncfile.findVariable("time");
    assert timeVar != null;
    int[] shape = timeVar.getShape();
    assertThat(shape.length).isEqualTo(1);

    Array vals = timeVar.read();
    assertThat(vals.getInt(0)).isEqualTo(0);
    timeVar.getShape();
    assertThat(vals.getInt(shape[0] - 1)).isEqualTo(365 * 2 - 1);

    // check start and end dates of aggregation
    String expectedStart = "2016-01-01T00:00:00";
    String expectedEnd = "2017-12-31T00:00:00";
    testStartAndEnd(timeVar, expectedStart, expectedEnd);

    // the difference between two sequential CalendarDate objects should be 1 day
    testTimeDelta(timeVar, new TimeDelta(1, Field.Day));

    ncfile.close();
  }

  @Test
  public void testNcmlAggExistingAllLeapCal() throws IOException {
    // with calendar = all_leap, each year should have 366 days, even in non-leap years
    String filename = "file:./" + TestNcmlRead.topDir + "agg_with_calendar/aggExistingAllLeapCal.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);
    Variable timeVar = ncfile.findVariable("time");
    assert timeVar != null;
    int[] shape = timeVar.getShape();
    assertThat(shape.length).isEqualTo(1);

    Array vals = timeVar.read();
    assertThat(vals.getInt(0)).isEqualTo(0);
    timeVar.getShape();
    assertThat(vals.getInt(shape[0] - 1)).isEqualTo(366 * 2 - 1);

    // check start and end dates of aggregation
    String expectedStart = "2016-01-01T00:00:00";
    String expectedEnd = "2017-12-31T00:00:00";
    testStartAndEnd(timeVar, expectedStart, expectedEnd);

    // the difference between two sequential CalendarDate objects should be 1 day
    testTimeDelta(timeVar, new TimeDelta(1, Field.Day));

    ncfile.close();
  }

  @Test
  public void testNcmlAggExistingUniform30DayCal() throws IOException {
    // with calendar = uniform30day, each year should have 360 days (12 months, each with 30 days)
    String filename = "file:./" + TestNcmlRead.topDir + "agg_with_calendar/aggExistingUniform30DayCal.xml";
    testNcmlAggExisting30DayCals(filename);
  }

  @Test
  public void testNcmlAggExistingEquivalentUniform30DayCals() throws IOException {
    // in this NcML agg, one file uses uniform30day calendar, the other uses 360_day calendar, but
    // those are equivalent, so we let it slide. Otherwise, identical to the ncml agg used in
    // testNcmlAggExistingUniform30DayCal()
    String filename = "file:./" + TestNcmlRead.topDir + "agg_with_calendar/aggExistingEquivalentUniform30DayCals.xml";
    testNcmlAggExisting30DayCals(filename);
  }

  private void testNcmlAggExisting30DayCals(String filename) throws IOException {
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);
    Variable timeVar = ncfile.findVariable("time");
    assert timeVar != null;
    int[] shape = timeVar.getShape();
    assertThat(shape.length).isEqualTo(1);

    Array vals = timeVar.read();
    assertThat(vals.getInt(0)).isEqualTo(0);
    timeVar.getShape();
    assertThat(vals.getInt(shape[0] - 1)).isEqualTo(360 * 2 - 1);

    // check start and end dates of aggregation
    String expectedStart = "2019-01-01T00:00:00";
    String expectedEnd = "2020-12-30T00:00:00"; // every month has 30 days
    testStartAndEnd(timeVar, expectedStart, expectedEnd);

    // the difference between two sequential CalendarDate objects should be 1 day
    testTimeDelta(timeVar, new TimeDelta(1, Field.Day));

    ncfile.close();
  }

  @Test
  public void testNcmlAggExistingJulienCal() throws IOException {
    // with calendar = Julian, 4 October 1582 was followed by 5 October 1582 (unlike gregorian, where 4 October
    // was followed by 15 October).
    String filename = "file:./" + TestNcmlRead.topDir + "agg_with_calendar/aggExistingJulienCal.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);
    Variable timeVar = ncfile.findVariable("time");
    assert timeVar != null;
    int[] shape = timeVar.getShape();
    assertThat(shape.length).isEqualTo(1);

    Array vals = timeVar.read();
    assertThat(vals.getInt(0)).isEqualTo(0);
    timeVar.getShape();
    // 1582 -> 365 days (because we stay on Julian calendar)
    // 1583 -> 365 days
    // expected is 365 days + 365 days = 730 days
    assertThat(vals.getInt(shape[0] - 1)).isEqualTo(730 - 1);

    // the difference between two sequential CalendarDate objects should be 1 day
    testTimeDelta(timeVar, new TimeDelta(1, Field.Day));

    // Check the crossover for Gregorian, just to be sure we have it right for Julian
    // 4 October 1582 would be:
    //
    // 31 days Jan
    // 28 days Feb
    // 31 days March
    // 30 days April
    // 31 days May
    // 30 days June
    // 31 days July
    // 31 days August
    // 30 days September
    // 4 days October
    // --------------
    // 277 days would be 4 October 1582
    // 278 days would be 5 October 1582
    //
    CalendarDateUnit calendarDateUnit = getCalendarDateUnit(timeVar);
    int indexOct4 = 277 - 1;
    CalendarDate calendarDateBefore = calendarDateUnit.makeCalendarDate(indexOct4);
    CalendarDate expected = CalendarDate.parseISOformat(calendarDateUnit.getCalendar().name(), "1582-10-4T0:0:0");
    assertThat(calendarDateBefore).isEqualTo(expected);

    // 278 days -> 5 October 1582
    CalendarDate calendarDateAfter = calendarDateUnit.makeCalendarDate(indexOct4 + 1);
    expected = CalendarDate.parseISOformat(calendarDateUnit.getCalendar().name(), "1582-10-5T0:0:0");
    assertThat(calendarDateAfter).isEqualTo(expected);

    // check start and end dates of aggregation
    String expectedStart = "1582-01-01T00:00:00";
    String expectedEnd = "1583-12-31T00:00:00";
    testStartAndEnd(timeVar, expectedStart, expectedEnd);

    ncfile.close();
  }

  @Test
  public void testNcmlAggExistingGregorianCal() throws IOException {
    // with calendar = gregorian, 4 October 1582 was followed by 15 October 1582
    String filename = "file:./" + TestNcmlRead.topDir + "agg_with_calendar/aggExistingGregorianCal.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);
    Variable timeVar = ncfile.findVariable("time");
    assert timeVar != null;
    int[] shape = timeVar.getShape();
    assertThat(shape.length).isEqualTo(1);

    Array vals = timeVar.read();
    assertThat(vals.getInt(0)).isEqualTo(0);
    timeVar.getShape();
    // 1582 -> 365 - 10 = 355 days
    // 1583 -> 365 days
    // expected is 355 days + 365 days = 720
    assertThat(vals.getInt(shape[0] - 1)).isEqualTo(720 - 1);

    // the difference between two sequential CalendarDate objects should be 1 day
    testTimeDelta(timeVar, new TimeDelta(1, Field.Day));

    // Check the crossover for Gregorian, just to be sure we have it right for Julian
    // 4 October 1582 would be:
    //
    // 31 days Jan
    // 28 days Feb
    // 31 days March
    // 30 days April
    // 31 days May
    // 30 days June
    // 31 days July
    // 31 days August
    // 30 days September
    // 4 days October
    // --------------
    // 277 days would be 4 October 1582
    // 278 days would be 15 October 1582
    //
    CalendarDateUnit calendarDateUnit = getCalendarDateUnit(timeVar);
    int indexOct4 = 277 - 1;
    CalendarDate calendarDateBefore = calendarDateUnit.makeCalendarDate(indexOct4);
    CalendarDate expected = CalendarDate.parseISOformat(calendarDateUnit.getCalendar().name(), "1582-10-4T0:0:0");
    assertThat(calendarDateBefore).isEqualTo(expected);

    // 278 days -> 15 October 1582
    CalendarDate calendarDateAfter = calendarDateUnit.makeCalendarDate(indexOct4 + 1);
    expected = CalendarDate.parseISOformat(calendarDateUnit.getCalendar().name(), "1582-10-15T0:0:0");
    assertThat(calendarDateAfter).isEqualTo(expected);

    // 355 days + 365 days = 720 days in aggregation
    int indexLastDate = 720 - 1;
    CalendarDate lastCalendarDate = calendarDateUnit.makeCalendarDate(indexLastDate);
    expected = CalendarDate.parseISOformat(calendarDateUnit.getCalendar().name(), "1583-12-31T0:0:0");
    assertThat(lastCalendarDate).isEqualTo(expected);

    // check start and end dates of aggregation
    String expectedStart = "1582-01-01T00:00:00";
    String expectedEnd = "1583-12-31T00:00:00";
    testStartAndEnd(timeVar, expectedStart, expectedEnd);

    ncfile.close();
  }

  @Test
  public void testNcmlAggExistingProlepticGregorianCal() throws IOException {
    // with calendar = proleptic_gregorian, 1582 and 1583 should each have 365 days
    // (for a gregorian calendar, 1852 would only have 355
    String filename = "file:./" + TestNcmlRead.topDir + "agg_with_calendar/aggExistingProlepticGregorianCal.xml";

    NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);
    Variable timeVar = ncfile.findVariable("time");
    assert timeVar != null;
    int[] shape = timeVar.getShape();
    assertThat(shape.length).isEqualTo(1);

    Array vals = timeVar.read();
    assertThat(vals.getInt(0)).isEqualTo(0);
    timeVar.getShape();
    assertThat(vals.getInt(shape[0] - 1)).isEqualTo(365 * 2 - 1);

    // check start and end dates of aggregation
    String expectedStart = "1582-01-01T00:00:00";
    String expectedEnd = "1583-12-31T00:00:00";
    testStartAndEnd(timeVar, expectedStart, expectedEnd);
    ncfile.close();
  }

  private class TimeDelta {

    private final Number value;
    private final Field unit;

    TimeDelta(Number value, Field unit) {
      this.value = value;
      this.unit = unit;
    }

    Number getValue() {
      return value;
    }

    Field getUnit() {
      return unit;
    }
  }

  private CalendarDateUnit getCalendarDateUnit(Variable timeVar) {
    Attribute calAttr = timeVar.findAttributeIgnoreCase(CF.CALENDAR);
    assertThat(calAttr).isNotNull();
    Calendar cal = Calendar.get(calAttr.getStringValue());
    return CalendarDateUnit.withCalendar(cal, timeVar.getUnitsString());
  }

  private void testTimeDelta(Variable timeVar, TimeDelta expectedDiff) throws IOException {
    CalendarDateUnit calendarDateUnit = getCalendarDateUnit(timeVar);
    int[] shape = timeVar.getShape();
    Array vals = timeVar.read();
    for (int val = 1; val < shape[0]; val++) {
      CalendarDate today = calendarDateUnit.makeCalendarDate(vals.getInt(val));
      CalendarDate yesterday = calendarDateUnit.makeCalendarDate(vals.getInt(val - 1));
      long diff = today.getDifference(yesterday, expectedDiff.getUnit());
      assertThat(diff).isEqualTo(expectedDiff.getValue());
    }
  }

  private void testStartAndEnd(Variable timeVar, String start, String end) throws IOException {
    Array vals = timeVar.read();
    int[] shape = vals.getShape();

    // check start date of aggregation
    CalendarDateUnit calendarDateUnit = getCalendarDateUnit(timeVar);
    CalendarDate calendarDate = calendarDateUnit.makeCalendarDate(vals.getInt(0));
    CalendarDate expected = CalendarDate.parseISOformat(calendarDateUnit.getCalendar().name(), start);
    assertThat(calendarDate).isEqualTo(expected);

    // check end date of aggregation
    calendarDate = calendarDateUnit.makeCalendarDate(vals.getInt(shape[0] - 1));
    expected = CalendarDate.parseISOformat(calendarDateUnit.getCalendar().name(), end);
    assertThat(calendarDate).isEqualTo(expected);
  }

  public void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("lon");
    assert null != lonDim;
    assert lonDim.getShortName().equals("lon");
    assert lonDim.getLength() == 4;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 59;
  }

  public void testCoordVar(NetcdfFile ncfile) throws IOException {

    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 3;
    assert lat.getShape()[0] == 3;
    assert lat.getDataType() == DataType.FLOAT;

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension("lat"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("degrees_north");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array data = lat.read();
    assert data.getRank() == 1;
    assert data.getSize() == 3;
    assert data.getShape()[0] == 3;
    assert data.getElementType() == float.class;

    IndexIterator dataI = data.getIndexIterator();
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 41.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 40.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 39.0);
  }

  public void testAggCoordVar(NetcdfFile ncfile) {
    Variable time = ncfile.findVariable("time");
    assert null != time;

    String testAtt = time.findAttributeString("ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("timeAtt");

    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 59;
    assert time.getShape()[0] == 59;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 59;
      assert data.getShape()[0] == 59;
      assert data.getElementType() == int.class;

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext())
        assert dataI.getIntNext() == count++ : dataI.getIntCurrent();

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  public void testReadData(NetcdfFile ncfile) {
    Variable v = ncfile.findVariable("T");
    assert null != v;
    assert v.getShortName().equals("T");
    assert v.getRank() == 3;
    assert v.getSize() == 708 : v.getSize();
    assert v.getShape()[0] == 59;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    try {
      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getSize() == 708;
      assert data.getShape()[0] == 59;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      assert data.getElementType() == double.class;

      int[] shape = data.getShape();
      Index tIndex = data.getIndex();
      for (int i = 0; i < shape[0]; i++)
        for (int j = 0; j < shape[1]; j++)
          for (int k = 0; k < shape[2]; k++) {
            double val = data.getDouble(tIndex.set(i, j, k));
            Assert2.assertNearlyEquals(val, 100 * i + 10 * j + k);
          }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable("T");

    Array data = v.read(origin, shape);
    assert data.getRank() == 3;
    assert data.getSize() == shape[0] * shape[1] * shape[2];
    assert data.getShape()[0] == shape[0] : data.getShape()[0] + " " + shape[0];
    assert data.getShape()[1] == shape[1];
    assert data.getShape()[2] == shape[2];
    assert data.getElementType() == double.class;

    Index tIndex = data.getIndex();
    for (int i = 0; i < shape[0]; i++)
      for (int j = 0; j < shape[1]; j++)
        for (int k = 0; k < shape[2]; k++) {
          double val = data.getDouble(tIndex.set(i, j, k));
          Assert2.assertNearlyEquals(val, 100 * (i + origin[0]) + 10 * j + k);
        }

  }

  public void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice(ncfile, new int[] {0, 0, 0}, new int[] {59, 3, 4});
    testReadSlice(ncfile, new int[] {0, 0, 0}, new int[] {2, 3, 2});
    testReadSlice(ncfile, new int[] {25, 0, 0}, new int[] {10, 3, 4});
    testReadSlice(ncfile, new int[] {44, 0, 0}, new int[] {10, 2, 3});
  }
}
