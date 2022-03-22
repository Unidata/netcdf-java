package thredds.inventory.zarr;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestMFileZip {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}, {1}")
  static public List<Integer[]> getTestParameters() {
    List<Integer[]> result = new ArrayList<>();
    result.add(new Integer[] {0, 0});
    result.add(new Integer[] {0, 1});
    result.add(new Integer[] {1, 1});
    result.add(new Integer[] {1000, 3});
    return result;
  }

  @Parameterized.Parameter(0)
  public int expectedSize;

  @Parameterized.Parameter(1)
  public int expectedNumberOfFiles;

  @Test
  public void shouldWriteZipToStream() throws IOException {
    final ZipFile zipFile = createTemporaryZipFile(expectedSize, expectedNumberOfFiles);
    final MFileZip mFile = new MFileZip(zipFile.getName());

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    mFile.writeToStream(outputStream);
    assertThat(outputStream.size()).isEqualTo(expectedSize * expectedNumberOfFiles);
  }

  private ZipFile createTemporaryZipFile(int size, int numberOfFiles) throws IOException {
    final File zipFile = tempFolder.newFile("TestMFileZip" + size + "-" + numberOfFiles +".zip");

    try (FileOutputStream fos = new FileOutputStream(zipFile.getPath());
        ZipOutputStream zipOS = new ZipOutputStream(fos)) {
      for (int i = 0; i < numberOfFiles; i++) {
        final File file = createTemporaryFile(size);
        writeToZipFile(file, zipOS);
      }
    }

    return new ZipFile(zipFile.getPath());
  }

  private void writeToZipFile(File file, ZipOutputStream zipStream) throws IOException {
    final ZipEntry zipEntry = new ZipEntry(file.getPath());
    zipStream.putNextEntry(zipEntry);
    zipStream.write(Files.readAllBytes(file.toPath()), 0, (int) file.length());
    zipStream.closeEntry();
  }

  private File createTemporaryFile(int size) throws IOException {
    final File tempFile = tempFolder.newFile();

    byte[] bytes = new byte[size];
    new Random().nextBytes(bytes);
    Files.write(tempFile.toPath(), bytes);

    return tempFile;
  }
}
