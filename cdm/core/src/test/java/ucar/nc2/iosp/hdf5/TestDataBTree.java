package ucar.nc2.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.hdf5.H5header.Vinfo;
import ucar.unidata.util.test.TestDir;

public class TestDataBTree {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String TEST_FILE = TestDir.cdmLocalTestDataDir + "chunked.h5";

  private NetcdfFile netcdfFile;

  @Before
  public void open() throws Exception {
    netcdfFile = NetcdfFile.open(TEST_FILE);
  }

  @After
  public void close() throws Exception {
    netcdfFile.close();
  }

  @Test
  public void shouldReleaseRafs() throws IOException {
    final Variable variable = netcdfFile.findVariable("data");
    assertThat((Object) variable).isNotNull();
    final DataBTree bTree = ((Vinfo) variable.getSPobject()).btree;

    assertThat(bTree.getRandomAccessFile()).isNotNull();
    netcdfFile.release();
    assertThat(bTree.getRandomAccessFile()).isNull();
  }

  @Test
  public void shouldCloseRafs() throws IOException {
    final Variable variable = netcdfFile.findVariable("data");
    assertThat((Object) variable).isNotNull();
    final DataBTree bTree = ((Vinfo) variable.getSPobject()).btree;

    assertThat(bTree.getRandomAccessFile()).isNotNull();
    netcdfFile.close();
    assertThat(bTree.getRandomAccessFile()).isNull();
  }
}
