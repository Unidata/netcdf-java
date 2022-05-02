package ucar.nc2.grib.collection;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class TestGcMFile {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  @RunWith(Parameterized.class)
  public static class TestGcMFileParameterized {

    @Parameterized.Parameters(name = "{0}")
    static public List<Integer> getTestParameters() {
      return Arrays.asList(0, 1, 60000, 100000);
    }

    @Parameterized.Parameter()
    public int expectedSize;

    @Test
    public void shouldWriteFileToStream() throws IOException {
      final File file = createTemporaryFile(expectedSize);
      final GcMFile mFile = new GcMFile(tempFolder.getRoot(), file.getName(), file.lastModified(), file.length(), 0);

      final long length = mFile.getLength();
      assertThat(length).isEqualTo(expectedSize);

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      mFile.writeToStream(outputStream);
      assertThat(outputStream.size()).isEqualTo(expectedSize);
      assertThat(outputStream.toByteArray()).isEqualTo(Files.readAllBytes(file.toPath()));
    }

    @Test
    public void shouldWritePartialFileToStream() throws IOException {
      final File file = createTemporaryFile(expectedSize);
      GcMFile mFile = new GcMFile(tempFolder.getRoot(), file.getName(), file.lastModified(), file.length(), 0);

      final long length = mFile.getLength();
      assertThat(length).isEqualTo(expectedSize);

      final int[][] testCases = {{0, 0}, {10, 10}, {0, (int) length}, {0, 100}, {42, 100}};

      for (int[] testCase : testCases) {
        final int offset = testCase[0];
        final int maxBytes = testCase[1];

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mFile.writeToStream(outputStream, offset, maxBytes);

        final int startPosition = Math.min(offset, expectedSize);
        final int endPosition = Math.min(offset + maxBytes, expectedSize);

        assertThat(outputStream.size()).isEqualTo(Math.max(0, endPosition - startPosition));

        final byte[] partialFile = Arrays.copyOfRange(Files.readAllBytes(file.toPath()), startPosition, endPosition);
        assertThat(outputStream.toByteArray()).isEqualTo(partialFile);
      }
    }
  }

  public static class TestGcMFileNonParameterized {
    @Test
    public void shouldReturnTrueForExistingFile() throws IOException {
      final File file = createTemporaryFile(0);
      final GcMFile mFile = new GcMFile(tempFolder.getRoot(), file.getName(), file.lastModified(), file.length(), 0);
      assertThat(mFile.exists()).isEqualTo(true);
    }

    @Test
    public void shouldReturnFalseForNonExistingFile() {
      final GcMFile mFile = new GcMFile(tempFolder.getRoot(), "notARealFile", 0, 0, 0);
      assertThat(mFile.exists()).isEqualTo(false);
    }

    @Test
    public void shouldGetInputStream() throws IOException {
      final File file = createTemporaryFile(1);
      final GcMFile mFile = new GcMFile(tempFolder.getRoot(), file.getName(), file.lastModified(), file.length(), 0);
      try (final InputStream inputStream = mFile.getInputStream()) {
        assertThat(inputStream.read()).isNotEqualTo(-1);
      }
    }
  }


  private static File createTemporaryFile(int size) throws IOException {
    final File tempFile = tempFolder.newFile();

    byte[] bytes = new byte[size];
    new Random().nextBytes(bytes);
    Files.write(tempFile.toPath(), bytes);

    return tempFile;
  }
}
