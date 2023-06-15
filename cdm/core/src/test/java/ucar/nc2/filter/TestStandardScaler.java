package ucar.nc2.filter;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;

public class TestStandardScaler {

  @Test
  public void shouldEncodeDecode() {
    StandardScaler filter = new StandardScaler();
    byte[] array = new byte[10];
    byte[] encoded = filter.encode(array);
    byte[] decoded = filter.decode(encoded);
    assertThat(decoded).isEqualTo(array);
  }
}
