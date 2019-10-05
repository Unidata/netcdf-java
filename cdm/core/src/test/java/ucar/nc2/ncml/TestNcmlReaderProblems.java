/* Copyright Unidata */
package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.ncml.NcMLReaderNew;
import ucar.nc2.util.CompareNetcdf2;

/**
 * Compare old and new NcmlReaders on specific problem datasets.
 *
 * @author caron
 * @since 10/4/2019.
 */
public class TestNcmlReaderProblems {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void problem() throws IOException {
    compare("file:C:/dev/github/netcdf-java/cdm/core/src/test/data/ncml/aggJoinNewScalarCoord.xml");
  }

  private void compare(String ncmlLocation) throws IOException {
    System.out.printf("Compare %s%n", ncmlLocation);
    logger.info("TestNcmlReaders on {}%n", ncmlLocation);
    try (NetcdfDataset org = NcMLReader.readNcML(ncmlLocation, null)) {
      try (NetcdfDataset withBuilder = NcMLReaderNew.readNcML(ncmlLocation, null, null)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, true, true, true);
        boolean ok = compare.compare(org, withBuilder);
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

}
