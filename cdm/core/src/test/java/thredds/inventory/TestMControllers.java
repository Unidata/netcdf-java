package thredds.inventory;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

public class TestMControllers {

  @Test
  public void testDirectoryStream() throws IOException {
    String testDir = TestDir.localTestDataDir + "directory_stream";

    ArrayList<String> dsMfiles = new ArrayList<>();
    DirectoryStream<MFile> ds = MControllers.newDirectoryStream(testDir);
    for (MFile item : ds) {
      dsMfiles.add(Paths.get(item.getPath()).toAbsolutePath().toString());
    }
    ds.close();

    ArrayList<String> dsFiles = new ArrayList<>();
    DirectoryStream<Path> ds2 =
        Files.newDirectoryStream(Paths.get(TestDir.localTestDataDir + "directory_stream").toAbsolutePath());
    for (Path item : ds2) {
      dsFiles.add(item.toString());
    }
    ds2.close();

    dsMfiles.sort(null);
    dsFiles.sort(null);
    assertThat(dsMfiles).containsExactlyElementsIn(dsFiles);
  }

  @Test
  public void testSubdirStream() throws IOException {
    String testDir = TestDir.localTestDataDir + "directory_stream";
    ArrayList<String> dsMfiles = new ArrayList<>();
    DirectoryStream<MFile> ds = MControllers.newSubdirStream(testDir);
    for (MFile item : ds) {
      dsMfiles.add(Paths.get(item.getPath()).toAbsolutePath().toString());
    }
    ds.close();

    assertThat(dsMfiles).hasSize(1);
  }
}
