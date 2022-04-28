package thredds.inventory.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class TestCollectionSpecParserS3 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String BUCKET = "cdms3:thredds-test-data";
  public static final String FRAGMENT = "#delimiter=/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][] {
        {"u00/FNMOC/NAVGEM/pressure/**/US058GMET-GR1mdl.0018_0056_00000F0..#yyyyMMddHH#_0102_000000-000000pres$",
            "u00/FNMOC/NAVGEM/pressure/", true,
            "US058GMET-GR1mdl.0018_0056_00000F0............_0102_000000-000000pres$",
            "US058GMET-GR1mdl.0018_0056_00000F0..#yyyyMMddHH"},

        {"data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/**/GFS_Alaska_191km_#yyyyMMdd_HHmm#\\.grib1$",
            "data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/", true, "GFS_Alaska_191km_.............\\.grib1$",
            "GFS_Alaska_191km_#yyyyMMdd_HHmm"},

        {"grid/grib/grib1/data/agg/.*\\.grb", "grid/grib/grib1/data/agg/", false, ".*\\.grb", null},

        {"data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#\\.nc",
            "data/ldm/pub/decoded/netcdf/surface/metar/", true, "Surface_METAR_.............\\.nc",
            "Surface_METAR_#yyyyMMdd_HHmm"},

        {"data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#.nc",
            "data/ldm/pub/decoded/netcdf/surface/metar/", true, "Surface_METAR_..............nc",
            "Surface_METAR_#yyyyMMdd_HHmm"},

        {"data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm",
            "data/ldm/pub/decoded/netcdf/surface/metar/", true, "Surface_METAR_*", "Surface_METAR_#yyyyMMdd_HHmm"},

        {"data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm",
            "data/ldm/pub/decoded/netcdf/surface/metar/", false, "Surface_METAR_*", "Surface_METAR_#yyyyMMdd_HHmm"},

        {"data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc",
            "data/ldm/pub/decoded/netcdf/surface/metar/", false, "Surface_METAR_..............nc",
            "Surface_METAR_#yyyyMMdd_HHmm"},

        {"data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_yyyyMMdd_HHmm.nc",
            "data/ldm/pub/decoded/netcdf/surface/metar/", false, "Surface_METAR_yyyyMMdd_HHmm.nc", null},

        {"data/ldm/pub/decoded/netcdf/surface/metar/", "data/ldm/pub/decoded/netcdf/surface/metar/", false, null, null},

        {"data/ldm/pub/decoded/netcdf/surface/metar/**/", "data/ldm/pub/decoded/netcdf/surface/metar/", true, null,
            null},

        {"data/ldm/pub/decoded/netcdf/surface/metar/**/.*", "data/ldm/pub/decoded/netcdf/surface/metar/", true, ".*",
            null},

        {"data/ldm/pub/decoded/netcdf/surface/metar/.*", "data/ldm/pub/decoded/netcdf/surface/metar/", false, ".*",
            null},

        {"data/ldm/pub/decoded/netcdf/surface/metar/T*.T", "data/ldm/pub/decoded/netcdf/surface/metar/", false, "T*.T",
            null},

        {"", "", false, null, null},

        {".*grib1", "", false, ".*grib1", null},

        {".*\\.grib1", "", false, ".*\\.grib1", null},

        {"dir/**/subdir/.*grib1", "dir/", true, "subdir/.*grib1", null},});
  }

  private final String spec;
  private final String expectedPrefix;
  private final String expectedRootDir;
  private final boolean expectedWantSubDirs;
  private final String expectedFilter;
  private final String expectedDateFormatMark;

  public TestCollectionSpecParserS3(String key, String expectedPrefix, boolean expectedWantSubDirs,
      String expectedFilter, String expectedDateFormatMark) {
    this.spec = key.isEmpty() ? BUCKET : BUCKET + "?" + key;
    this.expectedPrefix = expectedPrefix;
    this.expectedRootDir = expectedPrefix.isEmpty() ? BUCKET : BUCKET + "?" + expectedPrefix;
    this.expectedWantSubDirs = expectedWantSubDirs;
    this.expectedFilter = expectedFilter;
    this.expectedDateFormatMark = expectedDateFormatMark;
  }

  @Test
  public void shouldParseS3CollectionSpecWithDelimiter() {
    final Formatter errlog = new Formatter();
    final CollectionSpecParserS3 specParser = CollectionSpecParserS3.create(spec + FRAGMENT, errlog);
    final String filter = (specParser.getFilter() != null) ? specParser.getFilter().toString() : null;

    assertThat(specParser.getRootDir()).isEqualTo(expectedRootDir);
    assertThat(specParser.getDelimiter()).isEqualTo("/");
    assertThat(specParser.getFragment()).isEqualTo(FRAGMENT);
    assertThat(specParser.wantSubdirs()).isEqualTo(expectedWantSubDirs);
    assertThat(filter).isEqualTo(expectedFilter);
    assertThat(specParser.getDateFormatMark()).isEqualTo(expectedDateFormatMark);
  }

  @Test
  public void shouldParseS3CollectionSpecWithoutDelimiter() {
    if (spec.contains("**")) {
      assertException();
    } else {
      assertResultWithoutDelimiter();
    }
  }

  private void assertResultWithoutDelimiter() {
    final Formatter errlog = new Formatter();
    final CollectionSpecParserS3 specParser = CollectionSpecParserS3.create(spec, errlog);
    final String filter = (specParser.getFilter() != null) ? specParser.getFilter().toString() : null;

    // Without a delimiter the root directory is just the bucket
    assertThat(specParser.getRootDir()).isEqualTo(BUCKET);
    assertThat(specParser.getDelimiter()).isEqualTo("");
    assertThat(specParser.getFragment()).isEqualTo("");
    assertThat(specParser.wantSubdirs()).isEqualTo(expectedWantSubDirs);
    // Without a delimiter the whole key is used in the filter and date format mark
    final String expectedFilter = this.expectedFilter == null ? expectedPrefix.isEmpty() ? null : expectedPrefix
        : expectedPrefix + this.expectedFilter;
    assertThat(filter).isEqualTo(expectedFilter);
    assertThat(specParser.getDateFormatMark())
        .isEqualTo(expectedDateFormatMark == null ? null : expectedPrefix + expectedDateFormatMark);
  }

  private void assertException() {
    try {
      CollectionSpecParserS3.create(spec, null);
      fail("Expected exception with spec: " + spec);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Must set a delimiter in order to use pattern '**' in spec: " + spec);
    }
  }
}
