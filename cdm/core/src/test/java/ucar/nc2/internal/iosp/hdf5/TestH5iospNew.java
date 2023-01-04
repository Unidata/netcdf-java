package ucar.nc2.internal.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.unidata.util.test.TestDir;

public class TestH5iospNew {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String TEST_FILE = TestDir.cdmLocalTestDataDir + "hdf5/structmetadata_eos.h5";

  private NetcdfFile netcdfFile;
  private H5iospNew h5iospNew;

  @Before
  public void open() throws Exception {
    netcdfFile = NetcdfFiles.open(TEST_FILE);
    final IOServiceProvider iosp = netcdfFile.getIosp();
    assertThat(iosp).isInstanceOf(H5iospNew.class);
    h5iospNew = (H5iospNew) iosp;
    assertThat(h5iospNew).isNotNull();
  }

  @After
  public void close() throws Exception {
    netcdfFile.close();
  }

  @Test
  public void shouldReleaseRafs() throws IOException {
    assertThat(h5iospNew.getRandomAccessFile()).isNotNull();
    assertThat(h5iospNew.getHeader().getRandomAccessFile()).isNotNull();
    assertThat(h5iospNew.getHeader().getH5objects().getRandomAccessFile()).isNotNull();

    netcdfFile.release();

    assertThat(h5iospNew.getRandomAccessFile()).isNull();
    assertThat(h5iospNew.getHeader().getRandomAccessFile()).isNull();
    assertThat(h5iospNew.getHeader().getH5objects().getRandomAccessFile()).isNull();
  }

  @Test
  public void shouldCloseRafs() throws IOException {
    assertThat(h5iospNew.getRandomAccessFile()).isNotNull();
    assertThat(h5iospNew.getHeader().getRandomAccessFile()).isNotNull();
    assertThat(h5iospNew.getHeader().getH5objects().getRandomAccessFile()).isNotNull();

    netcdfFile.close();

    assertThat(h5iospNew.getRandomAccessFile()).isNull();
    assertThat(h5iospNew.getHeader().getRandomAccessFile()).isNull();
    assertThat(h5iospNew.getHeader().getH5objects().getRandomAccessFile()).isNull();
  }
}
