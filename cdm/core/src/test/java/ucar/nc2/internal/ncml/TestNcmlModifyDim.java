package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.ncml.TestNcmlRead;

public class TestNcmlModifyDim {
  @Test
  public void shouldRenameDim() throws IOException {
    final String filename = "file:./" + TestNcmlRead.topDir + "modifyDim.xml";

    try (NetcdfFile ncfile = NcmlReader.readNcml(filename, null, null).build()) {
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

  @Test
  public void shouldRenameDimInAggregation() throws IOException {
    final String filename = "file:./" + TestNcmlRead.topDir + "aggregationRenameDim.xml";
    checkDimIsRenamed(filename);
  }

  @Test
  public void shouldRenameDimInAggregationScan() throws IOException {
    final String filename = "file:./" + TestNcmlRead.topDir + "aggregationScanRenameDim.xml";
    checkDimIsRenamed(filename);
  }

  private void checkDimIsRenamed(String filename) throws IOException {
    try (NetcdfFile ncfile = NcmlReader.readNcml(filename, null, null).build()) {
      Dimension newDim = ncfile.findDimension("newTime");
      assertThat(newDim).isNotNull();

      Dimension oldDim = ncfile.findDimension("time");
      assertThat(oldDim).isNull();

      Variable newTime = ncfile.findVariable("newTime");
      assertThat((Object) newTime).isNotNull();
      Array data = newTime.read();
      assertThat(data.getSize()).isEqualTo(59);

      Variable time = ncfile.findVariable("time");
      assertThat((Object) time).isNull();
    }
  }
}
