package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import ucar.ma2.Array;
import ucar.ma2.DataType;

public class TestDimensions {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // From a bug report by Bob Simons.
  @Test
  public void testAnonDimFullName() throws Exception {
    Dimension dim = new Dimension(null, 5, false, false, false); // Anonymous dimension.
    Assert.assertNull(dim.getShortName());
    Assert.assertNull(dim.getFullName()); // This used to cause a NullPointerException.
  }

  @Test
  public void testBuilder() {
    Dimension dim = Dimension.builder().setName("name").setLength(7).setIsUnlimited(true).build();
    assertThat(dim.getShortName()).isEqualTo("name");
    assertThat(dim.isUnlimited()).isTrue();

    Dimension dim2 = dim.toBuilder().setName("name2").build();
    assertThat(dim2).isEqualTo(new Dimension("name2", 7, true, true, false));
  }
}
