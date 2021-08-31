package ucar.nc2.internal.grid;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link ExtractTimeCoordinateValues} */
public class TestCoordTimeHelper {

  @Test
  public void testBasics() {
    String units = "days since 2001-01-01T00:00Z";
    CoordTimeHelper helper = CoordTimeHelper.factory(units, new AttributeContainerMutable(""));

    assertThat(helper.getUdUnit()).isEqualTo(units);
    assertThat(helper.getRefDate()).isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2001-01-01T00:00Z").orElseThrow());

    assertThat(helper.makeDate(10)).isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2001-01-11T00:00Z").orElseThrow());

    assertThat(helper.offsetFromRefDate(helper.makeDate(66))).isEqualTo(66);
  }

}
