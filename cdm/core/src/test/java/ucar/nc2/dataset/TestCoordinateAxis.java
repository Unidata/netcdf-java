package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.constants.AxisType;

public class TestCoordinateAxis {

  @Test
  public void shouldSortIntoCanonicalOrder() {
    CoordinateAxis lat = createCoordAxis(AxisType.Lat);
    CoordinateAxis lon = createCoordAxis(AxisType.Lon);
    CoordinateAxis height = createCoordAxis(AxisType.Height);
    CoordinateAxis runtime = createCoordAxis(AxisType.RunTime);
    CoordinateAxis ensemble = createCoordAxis(AxisType.Ensemble);
    CoordinateAxis time = createCoordAxis(AxisType.Time);

    List<CoordinateAxis> sortedAxes = Arrays.asList(lat, lon, height, runtime, ensemble, time);
    sortedAxes.sort(new CoordinateAxis.AxisComparator());

    List<CoordinateAxis> canonicalOrderAxes = Arrays.asList(runtime, ensemble, time, height, lat, lon);
    assertThat(sortedAxes.size()).isEqualTo(canonicalOrderAxes.size());

    for (int i = 0; i < sortedAxes.size(); i++) {
      assertThat(sortedAxes.get(i).getAxisType()).isEqualTo(canonicalOrderAxes.get(i).getAxisType());
    }
  }

  private static CoordinateAxis createCoordAxis(AxisType type) {
    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setDataType(DataType.FLOAT)
        .setUnits("units").setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll());

    return CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(type).build(makeDummyGroup());
  }
}
