/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.CompareNetcdf;
import ucar.unidata.util.test.TestDir;

/**
 * Compare NcmlReader and NcmlReaderNew
 */
@RunWith(Parameterized.class)
public class TestNcmlReaders {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      TestDir.actOnAllParameterized(
          TestNcMLRead.topDir,
          pathname -> !pathname.getName().startsWith("agg") && pathname.getName().endsWith("ml"),
          filenames, false);
    } catch (IOException e) {
      filenames.add(new Object[]{e.getMessage()});
    }
    return filenames;
  }

  private String ncmlLocation;
  public TestNcmlReaders(String filename) {
    this.ncmlLocation = "file:" + filename;
  }

  // @Test
  public void compareReaders() throws IOException {
    logger.info("TestNcmlReaders on {}%n", ncmlLocation);
    try (NetcdfDataset org = NcMLReader.readNcML(ncmlLocation, null)) {
      TestDir.readAllData(org);
      try (NetcdfDataset withBuilder = NcMLReaderNew.readNcML(ncmlLocation, null, null)) {
        assertThat(CompareNetcdf.compareFiles(org, withBuilder)).isTrue();
        TestDir.readAllData(withBuilder);
      }
    }
  }

  @Test
  public void problem() throws IOException {
    String ncmlLocation = "file:C:/dev/github/netcdf-java/cdm/core/src/test/data/ncml/notes.xml";
    logger.info("TestNcmlReaders on {}%n", ncmlLocation);
    try (NetcdfDataset org = NcMLReader.readNcML(ncmlLocation, null)) {
      TestDir.readAllData(org);
      try (NetcdfDataset withBuilder = NcMLReaderNew.readNcML(ncmlLocation, null, null)) {
        assertThat(CompareNetcdf.compareFiles(org, withBuilder)).isTrue();
        TestDir.readAllData(withBuilder);
      }
    }
  }
}

