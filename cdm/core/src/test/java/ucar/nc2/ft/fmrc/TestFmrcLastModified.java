/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.fmrc;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.time.CalendarDate;

/** Virtual datasets produced by an FMRC should report the last-modified time of the newest file in the collection. */
public class TestFmrcLastModified {
  private static final Path SRC_DIR = Paths.get("src/test/data/ncml/fmrc");
  private static final String NCML = "testAggFmrcScan.ncml";
  private static final List<String> FILES = Arrays.asList("GFS_Puerto_Rico_191km_20090729_0000.nc",
      "GFS_Puerto_Rico_191km_20090730_0000.nc", "GFS_Puerto_Rico_191km_20090731_0000.nc");

  // whole seconds, so filesystem timestamp resolution doesn't matter
  private static final long OLDER = 1_600_000_000_000L;
  private static final long NEWEST = 1_700_000_000_000L;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private File dir;

  @Before
  public void copyCollection() throws IOException {
    // make a copy of the collection files so that we can control the last modified date without touching
    // the actual test files
    dir = tempFolder.newFolder();
    Files.copy(SRC_DIR.resolve(NCML), dir.toPath().resolve(NCML));
    for (int i = 0; i < FILES.size(); i++) {
      Path copy = Files.copy(SRC_DIR.resolve(FILES.get(i)), dir.toPath().resolve(FILES.get(i)));
      // make the middle file the newest, so we know the max is used rather than the first or last file
      // simulates a mid collection file update
      Files.setLastModifiedTime(copy, FileTime.fromMillis(i == 1 ? NEWEST : OLDER + i * 1000L));
    }
  }

  @Test
  public void shouldReportNewestFileLastModified() throws IOException {
    try (Fmrc fmrc = Fmrc.open(new File(dir, NCML).getPath(), null)) {
      assertThat(fmrc).isNotNull();
      CalendarDate run = fmrc.getRunDates().get(0);
      double offset = fmrc.getForecastOffsets()[0];
      CalendarDate forecast = fmrc.getForecastDates().get(0);

      try (GridDataset best = fmrc.getDatasetBest();
          GridDataset twoD = fmrc.getDataset2D(null);
          GridDataset runDs = fmrc.getRunTimeDataset(run);
          GridDataset constOffset = fmrc.getConstantOffsetDataset(offset);
          GridDataset constForecast = fmrc.getConstantForecastDataset(forecast)) {
        for (GridDataset gds : Arrays.asList(best, twoD, runDs, constOffset, constForecast)) {
          assertThat(gds.getLastModified()).isEqualTo(NEWEST);
          assertThat(gds.getNetcdfFile().getLastModified()).isEqualTo(NEWEST);
        }
      }
    }
  }
}
