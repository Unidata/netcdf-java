/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestConventionFeatureTypes {
  static String base = TestDir.cdmUnitTestDir + "conventions/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {"atd", FeatureType.GRID});
    result.add(new Object[] {"atd-radar", FeatureType.GRID});
    result.add(new Object[] {"avhrr", FeatureType.GRID});
    result.add(new Object[] {"awips", FeatureType.GRID});
    result.add(new Object[] {"cedric", FeatureType.GRID});
    result.add(new Object[] {"cf", FeatureType.GRID});
    // result.add(new Object[] {"cf/dsc", FeatureType.POINT}); not supported in ver7
    // result.add(new Object[] {"cfradial", FeatureType.RADIAL}); not supported in ver7
    result.add(new Object[] {"coards", FeatureType.GRID});
    result.add(new Object[] {"csm", FeatureType.GRID});
    result.add(new Object[] {"gdv", FeatureType.GRID});
    result.add(new Object[] {"gief", FeatureType.GRID});
    result.add(new Object[] {"ifps", FeatureType.GRID});
    result.add(new Object[] {"m3io", FeatureType.GRID});
    result.add(new Object[] {"mars", FeatureType.GRID});
    // result.add(new Object[]{"mm5", FeatureType.GRID}); // Dataset lacks X and Y axes.
    result.add(new Object[] {"nuwg", FeatureType.GRID});
    result.add(new Object[] {"wrf", FeatureType.GRID});
    result.add(new Object[] {"zebra", FeatureType.GRID});

    return result;
  }

  FeatureType type;
  File dir;

  public TestConventionFeatureTypes(String dir, FeatureType type) {
    this.type = type;
    this.dir = new File(base + dir);
  }

  @Test
  public void testOpenGridDataset() throws IOException {
    Formatter errlog = new Formatter();
    for (File f : getAllFilesInDirectoryStandardFilter(dir)) {
      System.out.printf("GridDatasetFactory.openGridDataset %s%n", f.getPath());
      try (GridDataset gds = GridDatasetFactory.openGridDataset(f.getPath(), errlog)) {
        assertThat(gds).isNotNull();
        if (type == FeatureType.GRID) {
          assertThat(gds.getFeatureType().isCoverageFeatureType()).isTrue();
        }
      }
    }
  }

  @Test
  public void testWrapGridDataset() throws IOException {
    if (type != FeatureType.GRID) {
      return;
    }
    Formatter errlog = new Formatter();
    for (File f : getAllFilesInDirectoryStandardFilter(dir)) {
      System.out.printf("GridDatasetFactory.wrapGridDataset %s%n", f.getPath());
      try (NetcdfDataset ds = NetcdfDatasets.openDataset(f.getPath());
          GridDataset gds = GridDatasetFactory.wrapGridDataset(ds, errlog).orElseThrow()) {
        assertThat(gds).isNotNull();
        if (type == FeatureType.GRID) {
          assertThat(gds.getFeatureType().isCoverageFeatureType()).isTrue();
        }
      }
    }
  }

  private static List<File> getAllFilesInDirectoryStandardFilter(File topDir) {
    if (topDir == null || !topDir.exists()) {
      return Collections.emptyList();
    }

    if ((topDir.getName().equals("exclude")) || (topDir.getName().equals("problem"))) {
      return Collections.emptyList();
    }

    // get list of files
    File[] fila = topDir.listFiles();
    if (fila == null) {
      return Collections.emptyList();
    }

    List<File> files = new ArrayList<>();
    for (File f : fila) {
      if (!f.isDirectory()) {
        files.add(f);
      }
    }

    // eliminate redundant files
    // ".Z", ".zip", ".gzip", ".gz", or ".bz2"
    if (files.size() > 0) {
      Collections.sort(files);
      ArrayList<File> files2 = new ArrayList<>(files);

      File prev = null;
      for (File f : files) {
        String name = f.getName();
        String stem = stem(name);
        if (name.contains(".gbx") || name.contains(".ncx") || name.endsWith(".xml") || name.endsWith(".pdf")
            || name.endsWith(".txt") || name.endsWith(".tar")) {
          files2.remove(f);

        } else if (name.contains("2003021212_avn-x.nc")) { // valtime not monotonic
          files2.remove(f);

        } else if (prev != null) {
          if (name.endsWith(".ncml")) {
            if (prev.getName().equals(stem) || prev.getName().equals(stem + ".nc")) {
              files2.remove(prev);
            }
          } else if (name.endsWith(".bz2")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          } else if (name.endsWith(".gz")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          } else if (name.endsWith(".gzip")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          } else if (name.endsWith(".zip")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          } else if (name.endsWith(".Z")) {
            if (prev.getName().equals(stem)) {
              files2.remove(f);
            }
          }
        }
        prev = f;
      }

      return files2;
    }

    return files;
  }

  private static String stem(String name) {
    int pos = name.lastIndexOf('.');
    return (pos > 0) ? name.substring(0, pos) : name;
  }
}
