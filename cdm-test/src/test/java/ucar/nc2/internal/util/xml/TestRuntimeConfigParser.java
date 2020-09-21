/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.util.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;

/** Test {@link RuntimeConfigParser} */
public class TestRuntimeConfigParser {

  // needs grib and bufr loaded, so doing it in cdm-test
  private final String config = "<?xml version='1.0' encoding='UTF-8'?>\n" + " <runtimeConfig>\n"
      + "   <ioServiceProvider class='ucar.nc2.internal.iosp.netcdf3.N3iospNew'/>\n"
      + "   <coordSystemBuilderFactory convention='foo' class='ucar.nc2.internal.dataset.conv.CF1Convention$Factory'/>\n"
      + "   <coordTransBuilder name='atmos_ln_sigma_coordinates' type='vertical' class='ucar.nc2.dataset.transform.CsmSigma'/>\n"
      + "   <featureDatasetFactory featureType='SIMPLE_GEOMETRY' class='ucar.nc2.ft.SimpleGeometryStandardFactory'/>\n"
      + "   <gribParameterTable edition='1' center='58' subcenter='-1' version='128'>resources/grib1/ncep/ncepGrib1-130.xml</gribParameterTable>\n"
      + "   <gribParameterTableLookup edition='1'>resources/grib1/dss/lookupTables.txt</gribParameterTableLookup>\n"
      + "   <bufrtable filename='resource:/resources/bufrTables/local/tableLookup.csv' />\n"
      + "   <grib1Table strict='false'/>\n" + "   <Netcdf4Clibrary>\n"
      + "     <libraryPath>/usr/local/lib</libraryPath>\n" + "     <libraryName>netcdf</libraryName>\n"
      + "     <useForReading>false</useForReading>\n" + "   </Netcdf4Clibrary>\n" + " </runtimeConfig>";

  private final String failures = "<?xml version='1.0' encoding='UTF-8'?>\n" + " <runtimeConfig>\n"
      + "   <ioServiceProvider  class='edu.univ.ny.stuff.FooFiles'/>\n"
      + "   <coordSystemBuilderFactory convention='foo' class='test.Foo'/>\n"
      + "   <coordTransBuilder name='atmos_ln_sigma_coordinates' type='vertical' class='my.stuff.atmosSigmaLog'/>\n"
      + "   <featureDatasetFactory featureType='Point' class='gov.noaa.obscure.file.Flabulate'/>\n"
      + "   <gribParameterTable edition='1' center='58' subcenter='-1' version='128'>bad/grib1/ncep/ncepGrib1-130.xml</gribParameterTable>\n"
      + "   <gribParameterTableLookup edition='1'>bad/grib1/dss/lookupTables.txt</gribParameterTableLookup>\n"
      + "   <bufrtable filename='C:/my/files/lookup.txt' />\n" + "   <grib1Table strict='false'/>\n"
      + "   <Netcdf4Clibrary>\n" + "     <libraryPath>/usr/local/lib</libraryPath>\n"
      + "     <libraryName>netcdf</libraryName>\n" + "     <useForReading>false</useForReading>\n"
      + "   </Netcdf4Clibrary>\n" + " </runtimeConfig>";


  private final String failure2 = "<?xml version='1.0' encoding='UTF-8'?>\n" + " <runtimeConfig>\n" //
      + "   <ioServiceProvider  class='edu.univ.ny.stuff.FooFiles'/>\n" //
      + "   <coordSystemBuilderFactory convention='foo' class='test.Foo'/>\n" //
      + "   <coordTransBuilder name='atmos_ln_sigma_coordinates' type='vertical' class='my.stuff.atmosSigmaLog'/>\n" //
      + "   <featureDatasetFactory featureType='Point' class='gov.noaa.obscure.file.Flabulate'/>\n" //
      + "   <gribParameterTable edition='1' centeer='58' subcenter='-1' version='128'/>\n" //
      + "   <gribParameterTableLookup edition='1'/>\n" //
      + "   <bufrtable filesname='C:/my/files/lookup.txt' />\n" //
      + "   <grib1Table strict='true'/>\n" + "   <Netcdf4Clibrary>\n" //
      + "     <libraryPath>/usr/local/lib</libraryPath>\n" //
      + "     <libraryName>netcdf</libraryName>\n" //
      + "     <useForReading>false</useForReading>\n" //
      + "   </Netcdf4Clibrary>\n" //
      + " </runtimeConfig>"; //

  @Test
  public void testConfig() throws IOException {
    Formatter errlog = new Formatter();
    ByteArrayInputStream stringIS = new ByteArrayInputStream(config.getBytes());
    RuntimeConfigParser.read(stringIS, errlog);
    System.out.printf("%s%n", errlog);
  }

  @Test
  public void testFailures() throws IOException {
    Formatter errlog = new Formatter();
    ByteArrayInputStream stringIS = new ByteArrayInputStream(failures.getBytes());
    RuntimeConfigParser.read(stringIS, errlog);
    System.out.printf("%s%n", errlog);
  }

  @Test
  public void testFailure2() throws IOException {
    Formatter errlog = new Formatter();
    ByteArrayInputStream stringIS = new ByteArrayInputStream(failure2.getBytes());
    RuntimeConfigParser.read(stringIS, errlog);
    System.out.printf("%s%n", errlog);
  }

}
