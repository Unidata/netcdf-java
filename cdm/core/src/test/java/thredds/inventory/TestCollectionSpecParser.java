package thredds.inventory;

import static com.google.common.truth.Truth.assertThat;

import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class TestCollectionSpecParser {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() throws URISyntaxException {
    return Arrays.asList(new Object[][] {
        {"/u00/FNMOC/NAVGEM/pressure/**/US058GMET-GR1mdl.0018_0056_00000F0..#yyyyMMddHH#_0102_000000-000000pres$",
            "/u00/FNMOC/NAVGEM/pressure", true,
            "US058GMET-GR1mdl.0018_0056_00000F0............_0102_000000-000000pres$",
            "US058GMET-GR1mdl.0018_0056_00000F0..#yyyyMMddHH"},

        {"/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/**/GFS_Alaska_191km_#yyyyMMdd_HHmm#\\.grib1$",
            "/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km", true, "GFS_Alaska_191km_.............\\.grib1$",
            "GFS_Alaska_191km_#yyyyMMdd_HHmm"},

        {"Q:/grid/grib/grib1/data/agg/.*\\.grb", "Q:/grid/grib/grib1/data/agg", false, ".*\\.grb", null},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#\\.nc",
            "/data/ldm/pub/decoded/netcdf/surface/metar", true, "Surface_METAR_.............\\.nc",
            "Surface_METAR_#yyyyMMdd_HHmm"},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#.nc",
            "/data/ldm/pub/decoded/netcdf/surface/metar", true, "Surface_METAR_..............nc",
            "Surface_METAR_#yyyyMMdd_HHmm"},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm",
            "/data/ldm/pub/decoded/netcdf/surface/metar", true, "Surface_METAR_*", "Surface_METAR_#yyyyMMdd_HHmm"},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm",
            "/data/ldm/pub/decoded/netcdf/surface/metar", false, "Surface_METAR_*", "Surface_METAR_#yyyyMMdd_HHmm"},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc",
            "/data/ldm/pub/decoded/netcdf/surface/metar", false, "Surface_METAR_..............nc",
            "Surface_METAR_#yyyyMMdd_HHmm"},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_yyyyMMdd_HHmm.nc",
            "/data/ldm/pub/decoded/netcdf/surface/metar", false, "Surface_METAR_yyyyMMdd_HHmm.nc", null},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/", "/data/ldm/pub/decoded/netcdf/surface/metar", false, null,
            null},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/**/", "/data/ldm/pub/decoded/netcdf/surface/metar", true, null,
            null},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/**/*", "/data/ldm/pub/decoded/netcdf/surface/metar", true, null,
            null},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/*", "/data/ldm/pub/decoded/netcdf/surface/metar", false, null,
            null},

        {"/data/ldm/pub/decoded/netcdf/surface/metar/T*.T", "/data/ldm/pub/decoded/netcdf/surface/metar", false, "T*.T",
            null},

        {"", System.getProperty("user.dir"), false, null, null},

        {".*grib1", System.getProperty("user.dir"), false, ".*grib1", null},

        {".*\\.grib1", System.getProperty("user.dir"), false, ".*\\.grib1", null},

        {"dir/**/subdir/.*grib1", "dir", true, "subdir/.*grib1", null},});
  }

  private final String spec;
  private final String expectedRootDir;
  private final boolean expectedWantSubDirs;
  private final String expectedFilter;
  private final String expectedDateFormatMark;

  public TestCollectionSpecParser(String spec, String expectedRootDir, boolean expectedWantSubDirs,
      String expectedFilter, String expectedDateFormatMark) {
    this.spec = spec;
    this.expectedRootDir = expectedRootDir;
    this.expectedWantSubDirs = expectedWantSubDirs;
    this.expectedFilter = expectedFilter;
    this.expectedDateFormatMark = expectedDateFormatMark;
  }

  @Test
  public void shouldParseCollectionSpec() {
    final Formatter errlog = new Formatter();
    final CollectionSpecParser specp = new CollectionSpecParser(spec, errlog);
    logger.debug("spec= " + spec + "\n" + specp);
    logger.debug(errlog.toString());
    final String filter = (specp.getFilter() != null) ? specp.getFilter().toString() : null;

    assertThat(specp.getRootDir()).isEqualTo(expectedRootDir);
    assertThat(specp.wantSubdirs()).isEqualTo(expectedWantSubDirs);
    assertThat(filter).isEqualTo(expectedFilter);
    assertThat(specp.getDateFormatMark()).isEqualTo(expectedDateFormatMark);
  }
}
