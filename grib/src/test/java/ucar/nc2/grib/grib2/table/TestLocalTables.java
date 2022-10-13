package ucar.nc2.grib.grib2.table;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.grib.grib2.table.EccodesCodeTable.LATEST_VERSION;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig.TimeUnitConverterHash;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFile;
import thredds.inventory.MFiles;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.coord.TimeCoordIntvDateValue;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@RunWith(JUnit4.class)
public class TestLocalTables {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testLocalTables() throws IOException {
    ImmutableList<Grib2Tables> tables = Grib2Tables.getAllRegisteredTables();
    for (Grib2Tables t : tables) {
      for (GribTables.Parameter p : t.getParameters()) {
        assertThat(p.getName()).isNotEmpty();
        assertThat(p.getId()).isNotEmpty();
      }
    }
  }

  @Test
  public void testKmaTable() {
    Grib2Tables kma = Grib2Tables.factory(40, -1, -1, -1, -1);
    assertThat(kma).isNotNull();
    assertThat(kma.getType()).isEqualTo(Grib2TablesId.Type.kma);
    assertThat(kma.getParameters()).isNotEmpty();
    for (GribTables.Parameter p : kma.getParameters())
      System.out.printf("%s%n", p);
  }

  @Test
  public void testEcmwfCodeTables() throws IOException {
    ImmutableSet<String> tableOverrides = ImmutableSet.of("4.230", "4.233", "4.192", "5.40000", "5.50002");

    Grib2Tables ecmwfTable = Grib2Tables.factory(98, -1, -1, -1, -1);
    assertThat(ecmwfTable).isNotNull();
    assertThat(ecmwfTable.getType()).isEqualTo(Grib2TablesId.Type.eccodes);

    for (String tableName : tableOverrides) {
      Iterator<String> tokens = Splitter.on('.').trimResults().omitEmptyStrings().split(tableName).iterator();

      int discipline = Integer.parseInt(tokens.next());
      int category = Integer.parseInt(tokens.next());

      EccodesCodeTable ecmwfCodeTable = EccodesCodeTable.factory(LATEST_VERSION, discipline, category);
      assertThat(ecmwfCodeTable).isNotNull();
      for (Grib2CodeTableInterface.Entry entry : ecmwfCodeTable.getEntries()) {
        assertThat(ecmwfTable.getCodeTableValue(tableName, entry.getCode())).isEqualTo(entry.getName());
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void shouldUseUnitConverterWhenCreatingForecastInterval() throws IOException {
    final MFile file =
        MFiles.create(TestDir.cdmUnitTestDir + "/tds/ncep/NDFD_NWS_CONUS_conduit_2p5km_20220816_0030.grib2");
    assertThat(file).isNotNull();

    final Grib2Index gribIndex =
        (Grib2Index) GribIndex.readOrCreateIndexFromSingleFile(false, file, CollectionUpdateType.always, logger);
    final List<Grib2Record> records = gribIndex.getRecords();
    assertThat(records).isNotEmpty();
    final Grib2Record record = records.get(0);
    final Grib2Tables tables = Grib2Tables.factory(record);

    final CalendarDate start = CalendarDate.parseISOformat(null, "2022-08-18T00:00:00Z");
    final CalendarDate end = CalendarDate.parseISOformat(null, "2022-08-18T12:00:00Z");
    final TimeCoordIntvDateValue expectedInterval = new TimeCoordIntvDateValue(start, end);

    // default units
    final TimeCoordIntvDateValue interval = tables.getForecastTimeInterval(record);
    assertThat(interval).isEqualTo(expectedInterval);

    // convert hours to minutes
    final TimeUnitConverterHash hoursToMinutesConverter = new TimeUnitConverterHash();
    hoursToMinutesConverter.map.put(1, 0);
    tables.setTimeUnitConverter(hoursToMinutesConverter);

    final TimeCoordIntvDateValue intervalHoursToMinutes = tables.getForecastTimeInterval(record);
    assertThat(intervalHoursToMinutes).isEqualTo(expectedInterval);

    // convert minutes to hour
    final TimeUnitConverterHash minutesToHoursConverter = new TimeUnitConverterHash();
    minutesToHoursConverter.map.put(0, 1);
    tables.setTimeUnitConverter(minutesToHoursConverter);

    final TimeCoordIntvDateValue intervalMinutesToHours = tables.getForecastTimeInterval(record);
    assertThat(intervalMinutesToHours).isEqualTo(expectedInterval);
  }
}
