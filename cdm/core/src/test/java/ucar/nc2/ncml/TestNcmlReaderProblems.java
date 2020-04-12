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
    // This appears to be wrong (time(31)) in NcmlReader, and correct NcmlReaderNew (time(59)).
    // compare("file:" + TestNcMLRead.topDir + "exclude/aggExistingNoCoordsDir.xml");

    // This used to fail in NcmlReader. Succeeds in NcmlReaderNew, but doesnt get the time coordinates right.
    // compare("file:" + TestNcMLRead.topDir + "exclude/aggExistingNoCoordsDir.xml");

    compare("file:" + TestNcMLRead.topDir + "enhance/testStandaloneEnhance.ncml");
  }

  private void compare(String ncmlLocation) throws IOException {
    System.out.printf("Compare %s%n", ncmlLocation);
    logger.info("TestNcmlReaders on {}%n", ncmlLocation);
    try (NetcdfDataset org = NcMLReader.readNcML(ncmlLocation, null)) {
      System.out.printf("NcMLReader == %s%n", org);
      try (NetcdfDataset withBuilder = NcMLReaderNew.readNcML(ncmlLocation, null, null).build()) {
        System.out.printf("NcMLReaderNew == %s%n", withBuilder);
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, true, true, true);
        boolean ok = compare.compare(org, withBuilder, new TestNcmlReadersCompare.CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

}
