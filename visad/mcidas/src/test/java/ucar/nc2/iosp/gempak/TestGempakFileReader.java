package ucar.nc2.iosp.gempak;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.NetcdfFile;
import ucar.unidata.io.RandomAccessFile;

public class TestGempakFileReader {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();
  GempakStationFileIOSP gempakStationFileIOSP;

  @Before
  public void createGempakIosp() throws IOException {
    final File tempFile = tempFolder.newFile();
    final RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile.getAbsolutePath(), "r");

    gempakStationFileIOSP = new GempakSurfaceIOSP();
    // This also initializes a GempakFileReader which stores this RandomAccessFile
    gempakStationFileIOSP.open(randomAccessFile, NetcdfFile.builder().build(), null);
    assertThat(gempakStationFileIOSP.gemreader.rf).isNotNull();
  }

  @Test
  public void shouldCloseResources() throws IOException {
    gempakStationFileIOSP.close();
    assertThat(gempakStationFileIOSP.gemreader.rf).isNull();
  }

  @Test
  public void shouldReleaseResources() throws IOException {
    gempakStationFileIOSP.release();
    assertThat(gempakStationFileIOSP.gemreader.rf).isNull();
  }

  @Test
  public void shouldReacquireResources() throws IOException {
    gempakStationFileIOSP.reacquire();
    assertThat(gempakStationFileIOSP.gemreader.rf).isNotNull();
  }
}
