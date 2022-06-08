package thredds.inventory.s3;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.inventory.CollectionManager;
import thredds.inventory.MFileCollectionManager;

@RunWith(Parameterized.class)
public class TestMFileCollectionManagerS3 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String BUCKET = "cdms3:thredds-test-data";
  private static final String DELIMITER = "#delimiter=/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][] {

        {BUCKET + "?" + ".*", true, true, false},

        {BUCKET + "?" + ".*grib1", true, true, false},

        {BUCKET + "?" + ".*\\.grib1", true, true, false},

        {BUCKET + "?" + "CONUS_80km/.*grib1", true, true, false},

        {BUCKET + "?" + "ncss/GFS/.*", true, false, false},

        {BUCKET + "?" + "ncss/GFS/CONUS_80km/.*", true, true, false},

        {BUCKET + "?" + "**/GFS/CONUS_80km/.*", true /* unused */, false, true}, // can't scan /**/ higher in the path

        {BUCKET + "?" + "ncss/**/CONUS_80km/.*", true /* unused */, false, true}, // can't scan /**/ higher in the path

        {BUCKET + "?" + "ncss/GFS/**/.*", true /* unused */, true, true},

        {BUCKET + "?" + "ncss/GFS/CONUS_80km/.*notAFileEnding", false, false, false},});
  }

  private final String spec;
  private final Pattern pattern;
  private final boolean haveFilesWithoutDelimiter;
  private final boolean haveFilesWithDelimiter;
  private final boolean throwsWithoutDelimiter;

  public TestMFileCollectionManagerS3(String spec, boolean haveFilesWithoutDelimiter, boolean haveFilesWithDelimiter,
      boolean throwsWithoutDelimiter) {
    this.spec = spec;
    this.pattern = Pattern.compile(spec.replace("?", Pattern.quote("?")).replace("**", ".*"));

    this.haveFilesWithoutDelimiter = haveFilesWithoutDelimiter;
    this.haveFilesWithDelimiter = haveFilesWithDelimiter;
    this.throwsWithoutDelimiter = throwsWithoutDelimiter;
  }

  @Test
  public void shouldGetFilteredS3FilesWithoutDelimiter() throws IOException {
    try {
      final CollectionManager collectionManager = MFileCollectionManager.open("testWithoutDelimiter", spec, null, null);
      final List<String> fileList = collectionManager.getFilenames();
      assertThat(!fileList.isEmpty()).isEqualTo(haveFilesWithoutDelimiter);
      assertFileNamesMatchRegEx(fileList);
    } catch (IllegalArgumentException e) {
      if (throwsWithoutDelimiter) {
        assertThat(e.getMessage()).contains("**");
      } else {
        assertWithMessage("Unexpected exception: " + e.getMessage());
      }
    }
  }

  @Test
  public void shouldGetFilteredS3FilesWithDelimiter() throws IOException {
    final CollectionManager collectionManager =
        MFileCollectionManager.open("testWithDelimiter", spec + DELIMITER, null, null);
    final List<String> fileList = collectionManager.getFilenames();
    assertThat(!fileList.isEmpty()).isEqualTo(haveFilesWithDelimiter);
    assertFileNamesMatchRegEx(fileList);
    assertFilesInDirectory(fileList);
  }

  private void assertFileNamesMatchRegEx(List<String> fileList) {
    for (String file : fileList) {
      final String filename = file.replace(DELIMITER, "");
      assertWithMessage(filename + " should match " + pattern).that(pattern.matcher(filename).matches()).isTrue();
    }
  }

  private void assertFilesInDirectory(List<String> fileList) {
    final long numberOfSlashesInPattern = pattern.toString().chars().filter(c -> c == '/').count();

    for (String file : fileList) {
      final String filename = file.replace(DELIMITER, "");
      final long numberOfSlashesInFile = filename.chars().filter(c -> c == '/').count();

      assertWithMessage("Expected file " + filename + "to be in directory defined by spec: " + spec)
          .that(numberOfSlashesInFile).isEqualTo(numberOfSlashesInPattern);
    }
  }
}
