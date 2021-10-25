/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test aggregation where timeUnitsChange='true' */
@Category(NeedsCdmUnitTest.class)
public class TestOffAggExistingTimeUnitsChange extends TestCase {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestOffAggExistingTimeUnitsChange(String name) {
    super(name);
  }
  // String location = "file:R:/testdata2/ncml/nc/narr/narr.ncml";

  public void testNamExtract() throws IOException {
    String location = TestDir.cdmUnitTestDir + "ncml/nc/namExtract/test_agg.ncml";
    logger.debug(" TestOffAggExistingTimeUnitsChange.open {}", location);

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(location, null)) {
      Variable v = ncfile.findVariable("time");
      assertThat(v).isNotNull();
      assertThat(v.getDataType()).isEqualTo(DataType.DOUBLE);

      String units = v.getUnitsString();
      assertThat(units).isNotNull();
      Optional<CalendarDateUnit> expectedCalendarDateUnit =
          CalendarDateUnit.fromUdunitString(null, "hours since 2006-09-25T06:00:00Z");

      assertThat(expectedCalendarDateUnit).isPresent();
      assertThat(units).ignoringCase().isEqualTo(expectedCalendarDateUnit.get().toString());

      int count = 0;
      Array data = v.read();

      while (data.hasNext()) {
        Assert2.assertNearlyEquals(data.nextInt(), (count + 1) * 3);
        count++;
      }
    }
  }

  public void testNarrGrib() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n"
        + " <aggregation type='joinExisting' dimName='time' timeUnitsChange='true' >\n"
        + "  <netcdf location='narr-a_221_20070411_0000_000.grb'/>\n"
        + "  <netcdf location='narr-a_221_20070411_0300_000.grb'/>\n"
        + "  <netcdf location='narr-a_221_20070411_0600_000.grb'/>\n" + " </aggregation>\n" + "</netcdf>";

    String location = "file:" + TestDir.cdmUnitTestDir + "ncml/nc/narr/";
    logger.debug(" TestOffAggExistingTimeUnitsChange.testNarrGrib={}\n{}", location, ncml);
    try (NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), location, null)) {
      Variable v = ncfile.findVariable("time");
      assertThat(v).isNotNull();
      assert v.getDataType() == DataType.DOUBLE;

      String units = v.getUnitsString();
      assertThat(units).isNotNull();
      assertThat(units).isEqualTo("hours since 2007-04-11T00:00Z");

      int count = 0;
      Array data = v.read();

      while (data.hasNext()) {
        assert data.nextInt() == count * 3;
        count++;
      }
    }
  }
}
