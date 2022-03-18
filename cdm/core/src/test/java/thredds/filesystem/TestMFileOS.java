package thredds.filesystem;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestMFileOS {

  @Parameterized.Parameters(name = "{0}")
  static public List<Integer> getTestParameters() {
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

  private File createTemporaryFile(int size) throws IOException {
    final File tempFile = File.createTempFile("TestMFileOS-", ".tmp");
    tempFile.deleteOnExit();

    byte[] bytes = new byte[size];
    new Random().nextBytes(bytes);
    Files.write(tempFile.toPath(), bytes);

    return tempFile;
  }
}
