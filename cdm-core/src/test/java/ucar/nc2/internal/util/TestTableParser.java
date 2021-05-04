/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import ucar.unidata.util.test.TestDir;

/** Test {@link ucar.nc2.internal.util.TableParser} */
public class TestTableParser {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  ////////////////////////////////////////////////////////////////////////////////////
  static final String fromResource = "/resources/nj22/tables/nexrad.tbl";
  static final String fromFile = TestDir.cdmLocalTestDataDir + "kma-ver5.txt";

  @Test
  public void testReadNexradTableAsStream() throws IOException {
    Class<?> c = TableParser.class;
    InputStream is = c.getResourceAsStream(fromResource);
    List<TableParser.Record> recs = TableParser.readTable(is, "3,15,54,60d,67d,73d", 50000);

    TableParser.Record rec = recs.get(0);
    Assert.assertEquals("TLX", rec.get(0));
    Assert.assertEquals("      000001", rec.get(1));
    Assert.assertEquals(" OKLAHOMA_CITY/Norman             OK US", rec.get(2));
    Assert.assertEquals(3532.0, (Double) rec.get(3), 0.1);
    Assert.assertEquals(-9727.0, (Double) rec.get(4), 0.1);
    Assert.assertEquals(370.0, (Double) rec.get(5), 0.1);

    rec = recs.get(20);
    Assert.assertEquals("TWX", rec.get(0));
    Assert.assertEquals("      000554", rec.get(1));
    Assert.assertEquals(" TOPEKA/Alma                      KS US", rec.get(2));
    Assert.assertEquals(3898.0, (Double) rec.get(3), 0.1);
    Assert.assertEquals(-9622.0, (Double) rec.get(4), 0.1);
    Assert.assertEquals(417.0, (Double) rec.get(5), 0.1);
  }

  @Test
  public void testReadNexradTable() throws IOException {
    List<TableParser.Record> recs = TableParser.readTable(fromFile, "41,112,124i,136i,148i,160", 50000);

    TableParser.Record rec = recs.get(0);
    assertThat(rec.get(0).toString().trim()).isEqualTo("U_COMPNT_OF_WIND_AFTER_TIMESTEP");
    assertThat(rec.get(2)).isEqualTo(0);
    assertThat(rec.get(3)).isEqualTo(2);
    assertThat(rec.get(4)).isEqualTo(2);
    assertThat(rec.get(5).toString().trim()).isEqualTo("m/s");
  }

}
