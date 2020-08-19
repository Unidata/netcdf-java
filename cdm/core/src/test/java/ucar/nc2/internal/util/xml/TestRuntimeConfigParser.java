/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.util.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;

/** Test {@link ucar.nc2.internal.util.xml.RuntimeConfigParser} */
public class TestRuntimeConfigParser {

  private final String config = "<?xml version='1.0' encoding='UTF-8'?>\n" + "<runtimeConfig>\n"
      + "  <ioServiceProvider  class='edu.univ.ny.stuff.FooFiles'/>\n"
      + "  <coordSystemFactory convention='foo' class='test.Foo'/>\n"
      + "  <coordTransBuilder name='atmos_ln_sigma_coordinates' type='vertical' class='my.stuff.atmosSigmaLog'/>\n"
      + "  <typedDatasetFactory datatype='Point' class='gov.noaa.obscure.file.Flabulate'/>\n" + "</runtimeConfig>";

  @Test
  public void testRead() throws IOException {
    Formatter errlog = new Formatter();
    ByteArrayInputStream stringIS = new ByteArrayInputStream(config.getBytes());
    RuntimeConfigParser.read(stringIS, errlog);
    System.out.printf("%s%n", errlog);
  }

}
