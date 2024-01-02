package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.ncml.TestNcmlRead;

public class TestNcmlModifyDim {
  private static final String filename = "file:./" + TestNcmlRead.topDir + "modifyDim.xml";
  private static NetcdfFile ncfile = null;

  @BeforeClass
  public static void setUp() throws IOException {
    ncfile = NcmlReader.readNcml(filename, null, null).build();
  }

  @AfterClass
  public static void tearDown() throws IOException {
    ncfile.close();
  }

  @Test
  public void shouldRenameDim() throws IOException {
    assertThat(ncfile.getRootGroup().getDimensions().size()).isEqualTo(3);

    Dimension newDim = ncfile.findDimension("newTime");
    assertThat(newDim).isNotNull();
    assertThat(newDim.isVariableLength()).isFalse();
    assertThat(newDim.isShared()).isTrue();
    assertThat(newDim.isUnlimited()).isTrue();

    Dimension oldDim = ncfile.findDimension("time");
    assertThat(oldDim).isNull();

    Variable time = ncfile.findVariable("time");
    assertThat((Object) time).isNotNull();
    assertThat(time.getDimensionsString()).isEqualTo("newTime");

    Array data = time.read();
    assertThat(data.getRank()).isEqualTo(1);
    assertThat(data.getSize()).isEqualTo(4);
    assertThat(data.getShape()[0]).isEqualTo(4);
    assertThat(data.getElementType()).isEqualTo(int.class);

    IndexIterator dataIter = data.getIndexIterator();
    assertThat(dataIter.getIntNext()).isEqualTo(6);
    assertThat(dataIter.getIntNext()).isEqualTo(18);
  }
}
