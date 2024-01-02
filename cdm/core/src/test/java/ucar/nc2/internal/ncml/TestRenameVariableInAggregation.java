package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class TestRenameVariableInAggregation {
  private static final double TOLERANCE = 1.0e-6;

  @Test
  public void shouldRenameVariableUsedInAggregation() throws IOException, InvalidRangeException {
    final String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" // leavit
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" // leavit
        + "  <variable name='temperature' orgName='T' />\n" // leavit
        + "  <aggregation  dimName='time' type='joinNew'>\n" // leavit
        + "    <variableAgg name='T'/>\n" // leavit
        + "    <netcdf location='src/test/data/ncml/nc/scalarTime0.nc'/>\n" // leavit
        + "    <netcdf location='src/test/data/ncml/nc/scalarTime1.nc'/>\n" // leavit
        + "    <netcdf location='src/test/data/ncml/nc/scalarTime2.nc'/>\n" // leavit
        + "  </aggregation>\n" // leavit
        + "</netcdf>"; // leavit

    checkNcmlDataset(ncml);
  }

  @Test
  public void shouldRenameVariableUsedInAggregationWithScan() throws IOException, InvalidRangeException {
    final String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" // leavit
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" // leavit
        + "  <variable name='temperature' orgName='T' />\n" // leavit
        + "  <aggregation  dimName='time' type='joinNew'>\n" // leavit
        + "    <variableAgg name='T'/>\n" // leavit
        + "    <scan location='src/test/data/ncml/nc/' subdirs='false' regExp='scalarTime.*nc'/>\n" // leavit
        + "  </aggregation>\n" // leavit
        + "</netcdf>"; // leavit

    checkNcmlDataset(ncml);
  }

  private static void checkNcmlDataset(String ncml) throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = NcmlReader.readNcml(new StringReader(ncml), null, null).build()) {
      final Variable oldVariable = ncfile.findVariable("T");
      assertThat((Object) oldVariable).isNull();
      final Variable newVariable = ncfile.findVariable("temperature");
      assertThat((Object) newVariable).isNotNull();

      final Array partialArray = newVariable.read(new Section("0:2, 0:0, 0:0"));
      assertThat(partialArray.getSize()).isEqualTo(3);
      assertThat(partialArray.getDouble(0)).isWithin(TOLERANCE).of(0.0);
      assertThat(partialArray.getDouble(1)).isWithin(TOLERANCE).of(100.0);
      assertThat(partialArray.getDouble(2)).isWithin(TOLERANCE).of(200.0);

      final Array array = newVariable.read();
      assertThat(array.getSize()).isEqualTo(36);
      assertThat(array.getDouble(0)).isWithin(TOLERANCE).of(0.0);
      assertThat(array.getDouble(12)).isWithin(TOLERANCE).of(100.0);
      assertThat(array.getDouble(24)).isWithin(TOLERANCE).of(200.0);
    }
  }
}
