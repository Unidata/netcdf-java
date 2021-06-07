package thredds.featurecollection;

import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.filesystem.MFileOS7;
import thredds.inventory.*;
import thredds.inventory.filter.WildcardMatchOnName;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CollectionSpecParser} */
public class TestFeatureCollectionConfig {

  @Test
  public void testParseCalendarDate() {
    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(
        "/data/ldm/pub/native/grid/NCEP/GFS/CONUS_95km/GFS_CONUS_95km_#yyyyMMdd_HHmm#.grib2", errlog);
    System.out.printf("errlog=%s%n", errlog);
    System.out.printf("specp=%s%n", specp);

    // test parsing
    assertThat(specp.getRootDir()).isEqualTo("/data/ldm/pub/native/grid/NCEP/GFS/CONUS_95km");
    assertThat(specp.wantSubdirs()).isFalse();
    assertThat(specp.getFilter().toString()).isEqualTo("GFS_CONUS_95km_..............grib2");
    assertThat(specp.getDateFormatMark()).isEqualTo("GFS_CONUS_95km_#yyyyMMdd_HHmm");

    // test filter
    MFileFilter mfilter = new WildcardMatchOnName(specp.getFilter());
    String path = "/data/ldm/pub/native/grid/NCEP/GFS/CONUS_95km/GFS_CONUS_95km_20141203_0000.grib2";
    MFile mfile = new MFileOS7(Paths.get(path), null);
    assertThat(mfilter.accept(mfile)).isTrue();

    // 2014-12-03T16:43:59.433 -0700 ERROR - ucar.nc2.units.DateFromString - Must delineate Date between 2 '#' chars,
    // dateFormatString = GFS_CONUS_95km_#yyyyMMdd_HHmm

    // test date extractor
    DateExtractor extractor = new DateExtractorFromName(specp.getDateFormatMark(), true);

    CalendarDate cd = extractor.getCalendarDate(mfile);
    assertThat(cd).isNotNull();
    System.out.printf("%s -> %s%n", path, cd);
    assertThat(cd.toString()).isEqualTo("2014-12-03T00:00Z");
  }

  @Test
  public void testCalendarDateFromPath() {
    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(
        "[DATA_DIR]/native/grid/FNMOC/COAMPS/Equatorial_America/FNMOC_COAMPS_Equatorial_America_#yyyyMMdd_HHmm#.grib1",
        errlog);
    System.out.printf("errlog=%s%n", errlog);
    System.out.printf("specp=%s%n", specp);

    // test parsing
    assertThat(specp.getRootDir()).isEqualTo("[DATA_DIR]/native/grid/FNMOC/COAMPS/Equatorial_America");
    assertThat(specp.wantSubdirs()).isFalse();
    assertThat(specp.getFilter().toString()).isEqualTo("FNMOC_COAMPS_Equatorial_America_..............grib1");
    assertThat(specp.getDateFormatMark()).isEqualTo("FNMOC_COAMPS_Equatorial_America_#yyyyMMdd_HHmm");

    // test filter
    String path = "[DATA_DIR]/native/grid/FNMOC/COAMPS/FNMOC_COAMPS_Equatorial_America_20141207_1200.grib1.ncx3";
    MFile mfile = new MFileOS7(Paths.get(path), null);

    // test date extractor
    DateExtractor extractor = new DateExtractorFromName(specp.getDateFormatMark(), true);

    CalendarDate cd = extractor.getCalendarDateFromPath(mfile.toString());
    assertThat(cd).isNotNull();
    System.out.printf("%s -> %s%n", path, cd);
    assertThat(cd.toString()).isEqualTo("2014-12-07T12:00Z");
  }

  @Test
  public void testFeatureCollectionConfig() {
    FeatureCollectionConfig config = new FeatureCollectionConfig("fnmoc", "test/fnmoc", FeatureCollectionType.GRIB1,
        "[DATA_DIR]/native/grid/FNMOC/COAMPS/Equatorial_America/FNMOC_COAMPS_Equatorial_America_#yyyyMMdd_HHmm#.grib1",
        null, null, null, "file", null);

    DateExtractor extractor = config.getDateExtractor();

    String path = "[DATA_DIR]/native/grid/FNMOC/COAMPS/FNMOC_COAMPS_Equatorial_America_20141207_1200.grib1.ncx3";
    MFile mfile = new MFileOS7(Paths.get(path), null);
    CalendarDate cd = extractor.getCalendarDate(mfile);
    assertThat(cd).isNotNull();
    System.out.printf("%s -> %s%n", path, cd);
    assertThat(cd.toString()).isEqualTo("2014-12-07T12:00Z");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testFeatureCollectionReader() throws IOException, JDOMException {
    File configFile = new File(TestDir.cdmUnitTestDir + "tds/config/fnmoc.xml");
    org.jdom2.Document doc;
    SAXBuilder builder = new SAXBuilder();
    doc = builder.build(configFile);

    XMLOutputter xmlOut = new XMLOutputter();
    System.out.println(xmlOut.outputString(doc));

    Formatter errlog = new Formatter();
    FeatureCollectionConfigBuilder fcb = new FeatureCollectionConfigBuilder(errlog);
    FeatureCollectionConfig config = fcb.readConfig(doc.getRootElement());
    DateExtractor extractor = config.getDateExtractor();

    String path = "[DATA_DIR]/native/grid/FNMOC/COAMPS/FNMOC_COAMPS_Equatorial_America_20141207_1200.grib1.ncx3";
    MFile mfile = new MFileOS7(Paths.get(path), null);
    CalendarDate cd = extractor.getCalendarDate(mfile);
    assertThat(cd).isNotNull();
    System.out.printf("%s -> %s%n", path, cd);
    assertThat(cd.toString()).isEqualTo("2014-12-07T12:00Z");
  }


  // [DATA_DIR]/native/grid/FNMOC/COAMPS/Equatorial_America/FNMOC_COAMPS_Equatorial_America_#yyyyMMdd_HHmm#.grib1


}
