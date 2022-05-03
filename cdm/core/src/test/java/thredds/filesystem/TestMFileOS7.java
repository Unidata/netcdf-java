package thredds.filesystem;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class TestMFileOS7 {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  @RunWith(Parameterized.class)
  public static class TestMFileOS7Parameterized {

    @Parameterized.Parameters(name = "{0}")
    static public List<Integer> getTestParameters() {
      return Arrays.asList(0, 1, 60000, 100000);
    }

    @Parameterized.Parameter()
    public int expectedSize;

    @Test
    public void shouldWriteFileToStream() throws IOException {
      final File file = createTemporaryFile(expectedSize);
      final MFileOS7 mFile = new MFileOS7(file.toPath());
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
      final MFileOS7 mFile = new MFileOS7(file.toPath());
      final long length = mFile.getLength();
      assertThat(length).isEqualTo(expectedSize);

      final long[][] testCases = {{0, 0}, {10, 10}, {0, length}, {0, 100}, {42, 100}};

      for (long[] testCase : testCases) {
        final long offset = testCase[0];
        final long maxBytes = testCase[1];

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mFile.writeToStream(outputStream, offset, maxBytes);

        final long startPosition = Math.min(offset, expectedSize);
        final long endPosition = Math.min(offset + maxBytes, expectedSize);

        assertThat(outputStream.size()).isEqualTo(Math.max(0, endPosition - startPosition));

        final byte[] partialFile =
            Arrays.copyOfRange(Files.readAllBytes(file.toPath()), (int) startPosition, (int) endPosition);
        assertThat(outputStream.toByteArray()).isEqualTo(partialFile);
      }
    }
  }

  public static class TestMFileOS7NonParameterized {
    @Test
    public void shouldReturnTrueForExistingFile() throws IOException {
      final File file = createTemporaryFile(0);
      final MFileOS7 mFile = new MFileOS7(file.toPath());
      assertThat(mFile.exists()).isEqualTo(true);
    }

    @Test
    public void shouldReturnFalseForNonExistingFile() {
      final MFileOS7 mFile = new MFileOS7(Paths.get("NotARealFile"), null);
      assertThat(mFile.exists()).isEqualTo(false);
    }

    @Test
    public void shouldGetInputStream() throws IOException {
      final File file = createTemporaryFile(1);
      final MFileOS7 mFile = new MFileOS7(file.toPath());
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
