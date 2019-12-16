package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;

public class TestDimensionBuilder {

  @Test
  public void testBuilder() {
    Dimension dim = Dimension.builder().setName("name").setLength(99).setIsUnlimited(true).setIsShared(false).build();
    assertThat(dim.getShortName()).isEqualTo("name");
    assertThat(dim.getLength()).isEqualTo(99);
    assertThat(dim.isUnlimited()).isEqualTo(true);
    assertThat(dim.isShared()).isEqualTo(false);
  }

  @Test
  public void testVariableLengthBuilder() {
    Dimension dim = Dimension.builder().setName("name").setLength(99).setIsUnlimited(true).setIsShared(true)
        .setIsVariableLength(true) // overrides
        .build();
    assertThat(dim.getShortName()).isEqualTo("name");
    assertThat(dim.getLength()).isEqualTo(-1);
    assertThat(dim.isUnlimited()).isEqualTo(false);
    assertThat(dim.isShared()).isEqualTo(false);
    assertThat(dim.isVariableLength()).isEqualTo(true);
  }

  @Test
  public void testConstructor() {
    Dimension dim = Dimension.builder().setName("name").setLength(99).build();
    assertThat(dim).isEqualTo(new Dimension("name", 99));
    assertThat(dim.isUnlimited()).isEqualTo(false);
    assertThat(dim.isShared()).isEqualTo(true);
    assertThat(dim.isVariableLength()).isEqualTo(false);
  }
}
