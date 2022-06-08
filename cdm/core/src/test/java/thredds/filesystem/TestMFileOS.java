package thredds.filesystem;

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
public class TestMFileOS {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  @RunWith(Parameterized.class)
  public static class TestMFileOSParameterized {

    @Parameterized.Parameters(name = "{0}")
    public static List<Integer> getTestParameters() {
      return Arrays.asList(0, 1, 60000, 100000);
    }

    @Parameterized.Parameter()
    public int expectedSize;

    @Test
    public void shouldWriteFileToStream() throws IOException {
      final File file = createTemporaryFile(expectedSize);
      final MFileOS mFile = new MFileOS(file);
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
      final MFileOS mFile = new MFileOS(file);
      final long length = mFile.getLength();
      assertThat(length).isEqualTo(expectedSize);

      final int offset = 42;
      final int maxBytes = 100;

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      mFile.writeToStream(outputStream, offset, maxBytes);

      final int startPosition = Math.min(offset, expectedSize);
      final int endPosition = Math.min(offset + maxBytes, expectedSize);

      assertThat(outputStream.size()).isEqualTo(Math.max(0, endPosition - startPosition));

      final byte[] partialFile = Arrays.copyOfRange(Files.readAllBytes(file.toPath()), startPosition, endPosition);
      assertThat(outputStream.toByteArray()).isEqualTo(partialFile);
    }
  }

  public static class TestMFileOSNonParameterized {
    @Test
    public void shouldReturnTrueForExistingFile() throws IOException {
      final MFileOS mFile = new MFileOS(createTemporaryFile(0));
      assertThat(mFile.exists()).isEqualTo(true);
    }

    @Test
    public void shouldReturnFalseForNonExistingFile() {
      final MFileOS mFile = new MFileOS("NotARealFile");
      assertThat(mFile.exists()).isEqualTo(false);
    }

    @Test
    public void shouldGetInputStream() throws IOException {
      final MFileOS mFile = new MFileOS(createTemporaryFile(1));
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
