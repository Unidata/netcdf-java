package tests.writingiosp;

import examples.writingiosp.OverviewIospTutorial;
import org.junit.Test;
import static com.google.common.truth.Truth.assertThat;

public class TestOverviewIospTutorial {
  @Test
  public void testImplementIospTutorial() {
    assertThat(OverviewIospTutorial.getIOSP()).isNotNull();
  }
}
