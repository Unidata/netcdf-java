/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class TestTimeUnits {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testTimeUnitConstruction() throws Exception {
    TimeUnit tu = new TimeUnit(3.0, "hours");
    logger.debug("TimeUnit.toString: {}", tu.toString());
    logger.debug("TimeUnit.getValue: {}", tu.getValue());
    logger.debug("TimeUnit.getUnitString: {}", tu.getUnitString());

    String unitBefore = tu.getUnitString();
    double secsBefore = tu.getValueInSeconds();

    tu.setValue(33.0);
    logger.debug("NewTimeUnit.toString: {}", tu.toString());

    assertThat(tu.getValue()).isEqualTo(33.0);
    assertThat(3600.0 * tu.getValue()).isEqualTo(tu.getValueInSeconds());
    assertThat(tu.getUnitString()).isEqualTo(unitBefore);
    assertThat(tu.getValueInSeconds()).isEqualTo(11.0 * secsBefore);

    tu.setValueInSeconds(3600.0);
    logger.debug("NewTimeUnitSecs.toString: {}", tu.toString());

    assertThat(tu.getValue()).isEqualTo(1.0);
    assertThat(tu.getValueInSeconds()).isEqualTo(3600.0);
    assertThat(tu.getUnitString()).isEqualTo(unitBefore);
    assertThat(3.0 * tu.getValueInSeconds()).isEqualTo(secsBefore);

    TimeUnit day = new TimeUnit(1.0, "day");
    double hoursInDay = day.convertTo(1.0, tu);
    assertThat(hoursInDay).isEqualTo(24.0);

    // note the value is ignored, only the base unit is used
    day = new TimeUnit(10.0, "day");
    hoursInDay = day.convertTo(1.0, tu);
    assertThat(hoursInDay).isEqualTo(24.0);

    hoursInDay = day.convertTo(10.0, tu);
    assertThat(hoursInDay).isEqualTo(240.0);

    assertThrows(IllegalArgumentException.class, () -> new TimeUnit(""));
  }

  @Test
  public void testCopyConstructor() throws Exception {
    TimeUnit tu = new TimeUnit(3.0, "hours");
    TimeUnit tuCopy = new TimeUnit(tu);
    assertThat(tuCopy).isEqualTo(tu);
    assertThat(tuCopy.hashCode()).isEqualTo(tu.hashCode());
  }

}
