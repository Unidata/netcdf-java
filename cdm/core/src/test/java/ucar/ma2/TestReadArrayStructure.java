package ucar.ma2;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;

public class TestReadArrayStructure {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String testDir = "../../dap4/src/test/data/resources/nctestfiles/";

  @Test
  public void shouldReadScalarStructure() throws IOException {
    final String filename = testDir + "test_struct1.nc";

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      final Structure structure = (Structure) ncfile.findVariable("s");
      assertThat((Object) structure).isNotNull();

      assertThat(structure.getVariables().size()).isEqualTo(2);
      final Variable x = structure.getVariables().get(0);
      assertThat(x.getShortName()).isEqualTo("x");
      final Variable y = structure.getVariables().get(1);
      assertThat(y.getShortName()).isEqualTo("y");

      final Array xValues = x.read();
      assertThat(xValues.getSize()).isEqualTo(1);
      assertThat(xValues.getInt(0)).isEqualTo(1);

      final Array yValues = y.read();
      assertThat(yValues.getSize()).isEqualTo(1);
      assertThat(yValues.getInt(0)).isEqualTo(-2);
    }
  }

  @Test
  public void shouldGetArrayForVlenMember() throws IOException {
    final String filename = testDir + "test_vlen3.nc";
    final int[] expectedValues = new int[] {1, 3, 5, 7};

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      final Structure structure = (Structure) ncfile.findVariable("v1");
      assertThat((Object) structure).isNotNull();

      final Array data = structure.read();
      assertThat(data).isInstanceOf(ArrayStructure.class);
      final ArrayStructure arrayStructure = (ArrayStructure) data;
      final Array values = arrayStructure.getStructureData(0).getArray("f1");
      checkValues(values, expectedValues);
    }
  }

  @Test
  public void shouldGetArrayForVlenMember2() throws IOException {
    final String filename = testDir + "test_vlen4.nc";

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      final Structure structure = (Structure) ncfile.findVariable("v1");
      assertThat((Object) structure).isNotNull();

      final Array data = structure.read();
      assertThat(data).isInstanceOf(ArrayStructure.class);
      final ArrayStructure arrayStructure = (ArrayStructure) data;
      final Array values = arrayStructure.getStructureData(0).getArray("f1");
      assertThat(values.getSize()).isEqualTo(2);
      assertThat(values.getObject(0)).isInstanceOf(ArrayInt.D1.class);
      assertThat(values.getObject(1)).isInstanceOf(ArrayInt.D1.class);
    }
  }

  @Ignore("Fails because of ArrayStructure::extractMemberArray not handling vlens correctly")
  @Test
  public void shouldExtractMemberArrayForVlenMember() throws IOException {
    final String filename = testDir + "test_vlen3.nc";
    final int[] expectedValues = new int[] {1, 3, 5, 7};

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      final Structure structure = (Structure) ncfile.findVariable("v1");
      assertThat((Object) structure).isNotNull();

      assertThat(structure.getVariables().size()).isEqualTo(1);
      final Variable f1 = structure.getVariables().get(0);
      assertThat(f1.getShortName()).isEqualTo("f1");
      assertThat(f1.isVariableLength()).isTrue();
      final Array values = f1.read();
      logger.debug("values: " + values.toString());
      checkValues(values, expectedValues);
    }
  }

  @Ignore("Fails because of ArrayStructure::extractMemberArray not handling vlens correctly")
  @Test
  public void shouldExtractMemberArrayForVlenMember2() throws IOException {
    final String filename = testDir + "test_vlen4.nc";

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      final Structure structure = (Structure) ncfile.findVariable("v1");
      assertThat((Object) structure).isNotNull();

      assertThat(structure.getVariables().size()).isEqualTo(1);
      final Variable f1 = structure.getVariables().get(0);
      assertThat(f1.getShortName()).isEqualTo("f1");
      assertThat(f1.isVariableLength()).isTrue();
      final Array values = f1.read();
      logger.debug("values: " + values.toString());
      assertThat(values.getSize()).isEqualTo(2);
      assertThat(values.getObject(0)).isInstanceOf(ArrayInt.D1.class);
      assertThat(values.getObject(1)).isInstanceOf(ArrayInt.D1.class);
    }
  }

  private static void checkValues(Array values, int[] expectedValues) {
    assertThat(values.getSize()).isEqualTo(expectedValues.length);
    final IndexIterator iter = values.getIndexIterator();
    int i = 0;
    while (iter.hasNext()) {
      assertThat(iter.next()).isEqualTo(expectedValues[i++]);
    }
  }
}
