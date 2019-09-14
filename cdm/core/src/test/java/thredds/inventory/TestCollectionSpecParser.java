package thredds.inventory;

import java.util.Formatter;
import org.junit.Test;

public class TestCollectionSpecParser {

  private static void doit(String spec, Formatter errlog) {
    CollectionSpecParser specp = new CollectionSpecParser(spec, errlog);
    System.out.printf("spec= %s%n%s%n", spec, specp);
    String err = errlog.toString();
    if (err.length() > 0)
      System.out.printf("%s%n", err);
    System.out.printf("-----------------------------------%n");
  }

  @Test
  public void testStuff() {
    doit("/u00/FNMOC/NAVGEM/pressure/**/US058GMET-GR1mdl.0018_0056_00000F0..#yyyyMMddHH#_0102_000000-000000pres$",
        new Formatter());

    doit("/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/**/GFS_Alaska_191km_#yyyyMMdd_HHmm#\\.grib1$",
        new Formatter());
    doit("Q:/grid/grib/grib1/data/agg/.*\\.grb", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#\\.nc", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm#.nc", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/Surface_METAR_#yyyyMMdd_HHmm", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_yyyyMMdd_HHmm.nc", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/**/*", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/*", new Formatter());
    doit("/data/ldm/pub/decoded/netcdf/surface/metar/T*.T", new Formatter());
  }


}
