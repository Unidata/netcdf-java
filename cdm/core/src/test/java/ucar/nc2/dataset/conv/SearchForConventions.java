/* Copyright Unidata */
package ucar.nc2.dataset.conv;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordSysBuilderIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.CoordSystemFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class SearchForConventions {
  private static final int MAX_SHOW = 1000;
  private static final String tempDir = "C:/Temp/conv/";
  private static String convDir = TestDir.cdmUnitTestDir + "/conventions";
  private static List<String> pointDirs =
      ImmutableList.of(TestDir.cdmUnitTestDir + "/ft", TestDir.cdmUnitTestDir + "/cfPoint");
  private static List<String> hdfDirs =
      ImmutableList.of(TestDir.cdmUnitTestDir + "/formats/hdf4/", TestDir.cdmUnitTestDir + "/formats/hdf5/");
  private static Multimap<String, String> convMap = ArrayListMultimap.create();
  private static Multimap<String, String> builderMap = ArrayListMultimap.create();

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      /*
       * TestDir.actOnAllParameterized(convDir, (file) -> !file.getPath().endsWith(".pdf"), filenames, true);
       * for (String dir : pointDirs) {
       * TestDir.actOnAllParameterized(dir, (file) -> file.getPath().endsWith(".nc"), filenames, true);
       * }
       */
      for (String dir : hdfDirs) {
        TestDir.actOnAllParameterized(dir, (file) -> !file.getPath().endsWith(".xml"), filenames, true);
      }
    } catch (IOException e) {
      filenames.add(new Object[] {e.getMessage()});
    }
    return filenames;
  }

  @AfterClass
  public static void showResults() throws IOException {
    Path tempPath = Paths.get(tempDir);
    if (!Files.exists(tempPath)) {
      System.out.printf("%s does not exist. Cannot save results.%n", tempDir);
    } else {
      FileWriter out = new FileWriter(tempDir + "conventions.txt");
      showResults(out, convMap);
      out.close();

      out = new FileWriter(tempDir + "builder.txt");
      showResults(out, builderMap);
      out.close();
    }
  }

  private static void showResults(FileWriter out, Multimap<String, String> mmap) throws IOException {
    List<String> keys = mmap.asMap().keySet().stream().sorted().collect(Collectors.toList());
    for (String key : keys) {
      out.write(String.format("%n%s%n", key));
      int count = 0;
      for (String filename : mmap.get(key)) {
        out.write(String.format("   %s%n", filename));
        if (count > MAX_SHOW)
          break;
        count++;
      }
    }
  }

  private String filename;

  public SearchForConventions(String filename) {
    this.filename = filename;
  }

  @Test
  // @Ignore("Not a test - really a utility program")
  public void findConventions() throws IOException {
    System.out.printf("%s%n", filename);
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      String convName = ncfile.getRootGroup().attributes().findAttValueIgnoreCase("Conventions", null);
      if (convName == null)
        convName = ncfile.getRootGroup().attributes().findAttValueIgnoreCase("Convention", null);
      if (convName != null) {
        convMap.put(convName, filename);
      }
    }

    try (NetcdfDataset withBuilder = NetcdfDatasets.openDataset(filename)) {
      String coordBuilderUsed =
          withBuilder.getRootGroup().attributes().findAttValueIgnoreCase(_Coordinate._CoordSysBuilder, null);
      if (coordBuilderUsed == null) {
        System.out.printf("****coordBuilderUsed is null for %s%n", filename);
      } else {
        builderMap.put(coordBuilderUsed, filename);
      }
    } catch (Throwable t) {
      System.out.printf("****Failed on %s (%s)%n", filename, t.getMessage());
      t.printStackTrace();
    }
  }
}
