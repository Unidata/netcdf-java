package thredds.inventory;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static ucar.unidata.util.test.TestDir.cdmUnitTestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestMFileCollectionManager {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][] {

        {cdmUnitTestDir + ".*", true},

        {cdmUnitTestDir + "ncss/GFS/.*", true},

        {cdmUnitTestDir + "ncss/GFS/CONUS_80km/.*grib1", true},

        {cdmUnitTestDir + "**/GFS/CONUS_80km/.*grib1", false}, // can't scan /**/ higher in the path

        {cdmUnitTestDir + "ncss/**/CONUS_80km/.*grib1", false}, // can't scan /**/ higher in the path

        {cdmUnitTestDir + "ncss/GFS/**/.*grib1", true},

        {cdmUnitTestDir + "ncss/GFS/CONUS_80km/.*notAFileEnding", false},});
  }

  private final String spec;
  private final Pattern pattern;
  private final boolean haveFiles;

  public TestMFileCollectionManager(String spec, boolean haveFiles) {
    this.spec = spec;
    this.pattern = Pattern.compile(spec.replace("?", Pattern.quote("?")).replace("**", ".*"));
    this.haveFiles = haveFiles;
  }

  @Test
  public void shouldGetFilteredFiles() throws IOException {
    final CollectionManager collectionManager = MFileCollectionManager.open("testWithDelimiter", spec, null, null);
    final List<String> fileList = collectionManager.getFilenames();
    assertThat(!fileList.isEmpty()).isEqualTo(haveFiles);
    assertFileNamesMatchRegEx(fileList);
    assertFilesInDirectory(fileList);
  }

  private void assertFileNamesMatchRegEx(List<String> fileList) {
    for (String file : fileList) {
      assertWithMessage(file + " should match " + pattern).that(pattern.matcher(file).matches()).isTrue();
    }
  }

  private void assertFilesInDirectory(List<String> fileList) {
    final long numberOfSlashesInPattern = pattern.toString().chars().filter(c -> c == '/').count();

    for (String file : fileList) {
      final long numberOfSlashesInFile = file.chars().filter(c -> c == '/').count();

      assertWithMessage("Expected file " + file + "to be in directory defined by spec: " + spec)
          .that(numberOfSlashesInFile).isEqualTo(numberOfSlashesInPattern);
    }
  }
}
