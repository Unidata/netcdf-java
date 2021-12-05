/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.util;

import static com.google.common.truth.Truth.assertThat;

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
    assertThat("TLX").isEqualTo(rec.get(0));
    assertThat("      000001").isEqualTo(rec.get(1));
    assertThat(" OKLAHOMA_CITY/Norman             OK US").isEqualTo(rec.get(2));
    assertThat(3532.0).isEqualTo((Double) rec.get(3));
    assertThat(-9727.0).isEqualTo((Double) rec.get(4));
    assertThat(370.0).isEqualTo((Double) rec.get(5));

    rec = recs.get(20);
    assertThat("TWX").isEqualTo(rec.get(0));
    assertThat("      000554").isEqualTo(rec.get(1));
    assertThat(" TOPEKA/Alma                      KS US").isEqualTo(rec.get(2));
    assertThat(3898.0).isEqualTo((Double) rec.get(3));
    assertThat(-9622.0).isEqualTo((Double) rec.get(4));
    assertThat(417.0).isEqualTo((Double) rec.get(5));
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
