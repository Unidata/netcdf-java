package thredds.inventory;

import static com.google.common.truth.Truth.assertThat;

import java.util.Formatter;
import org.junit.Test;

/** Sanity check on CollectionSpecParser */
public class TestCollectionSpecParser {

  private static void doit(String spec, String filterExpect) {
    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(spec, errlog);
    System.out.printf("spec= %s%n%s%n", spec, specp);
    String err = errlog.toString();
    System.out.printf("-----------------------------------%n");
    String filterHave = (specp.getFilter() != null) ? specp.getFilter().toString() : "null";
    assertThat(filterHave).isEqualTo(filterExpect);
  }

  @Test
  public void testStuff() {
    doit("/u00/FNMOC/NAVGEM/pressure/**/US058GMET-GR1mdl.0018_0056_00000F0..#yyyyMMddHH#_0102_000000-000000pres$",
        "US058GMET-GR1mdl.0018_0056_00000F0............_0102_000000-000000pres$");

    doit("/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/**/GFS_Alaska_191km_#yyyyMMdd_HHmm#\\.grib1$",
        "GFS_Alaska_191km_.............\\.grib1$");
    doit("Q:/grid/grib/grib1/data/agg/.*\\.grb", ".*\\.grb");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#\\.nc", "Surface_METAR_.............\\.nc");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#.nc", "Surface_METAR_..............nc");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm", "Surface_METAR_*");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm", "Surface_METAR_*");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc", "Surface_METAR_..............nc");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_yyyyMMdd_HHmm.nc", "Surface_METAR_yyyyMMdd_HHmm.nc");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/", "null");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/", "null");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/*", "null");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/*", "null");
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/T*.T", "T*.T");
  }


}
