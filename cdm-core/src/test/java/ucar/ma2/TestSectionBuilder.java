package ucar.ma2;

import static com.google.common.truth.Truth.assertThat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Section.Builder;

public class TestSectionBuilder {

  Builder sb;

  @Before
  public void setupSectionBuilder() {
    sb = Section.builder();
    // add three ranges
    sb.appendRange(Range.make(0, 2));
    sb.appendRange(Range.make(1, 6));
    sb.appendRange(Range.make(5, 7));
  }

  @Test
  public void testBuilderRemoveN() {
    int numberOfRanges = sb.ranges.size();
    Range last = sb.ranges.get(numberOfRanges - 1);

    // remove the first n ranges
    int n = 2;
    sb.removeFirst(n);
    assertThat(sb.ranges).hasSize(numberOfRanges - n);

    // the only thing left should be Range third.
    assertThat(sb.ranges.get(0)).isEqualTo(last);
  }

  @Test
  public void testRemoveLast() {
    int numberOfRanges = sb.ranges.size();
    Range rangeToRemove = sb.ranges.get(numberOfRanges - 1);

    // make a copy of the original ranges
    List<Range> copy = new ArrayList<Range>(sb.ranges);

    // remove the last one
    sb.removeLast();
    assertThat(sb.ranges).hasSize(numberOfRanges - 1);
    int updatedNumberOfRanges = sb.ranges.size();

    // Check that only the last one was removed
    for (int index = 0; index < updatedNumberOfRanges - 2; index++) {
      assertThat(sb.ranges.get(index)).isEqualTo(copy.get(index));
    }

    assertThat(sb.ranges.get(updatedNumberOfRanges - 1)).isNotEqualTo(rangeToRemove);
  }


}
