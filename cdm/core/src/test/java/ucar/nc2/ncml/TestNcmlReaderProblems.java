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
    // This used to fail in NcmlReader. Succeeds in NcmlReaderNew, but doesnt get the time coordinates right.
    // compare("file:" + TestNcMLRead.topDir + "exclude/aggExistingNoCoordsDir.xml");

    // This fails in NcmlReaderNew, because "NcML Variable dtype is required for new variables".
    // Implies that an aggregation element must (at least in this case) add stuff to the Dataset.Builder,
    // before the non-agg elements are processed.
    // compare("file:" + TestNcMLRead.topDir + "aggSynRename.xml");
    // compare("file:" + TestNcMLRead.topDir + "aggUnionRename.xml");

    // This is failing on DIFF time: element type double !== int
    // I think original is wrong, since ncml has <variable name="time" type="int">
    // compare("file:" + TestNcMLRead.topDir + "aggSynthetic.xml");

    compare("file:" + TestNcMLRead.topDir + "aggSynthetic.xml");
  }

  private void compare(String ncmlLocation) throws IOException {
    System.out.printf("Compare %s%n", ncmlLocation);
    logger.info("TestNcmlReaders on {}%n", ncmlLocation);
    try (NetcdfDataset org = NcMLReader.readNcML(ncmlLocation, null)) {
      try (NetcdfDataset withBuilder = NcMLReaderNew.readNcML(ncmlLocation, null, null).build()) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, true, true, true);
        boolean ok = compare.compare(org, withBuilder);
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

}
