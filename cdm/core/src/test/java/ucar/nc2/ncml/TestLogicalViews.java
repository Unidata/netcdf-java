package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Tests the logical view NcML elements: logicalSection, logicalSlice, and logicalReduce.
 */
public class TestLogicalViews {
  private static NetcdfDataset ncDataset;

  @BeforeClass
  public static void setupSpec() throws URISyntaxException, IOException {
    File testFile = new File(TestLogicalViews.class.getResource("afterLogicalViews.ncml").toURI());
    ncDataset = NetcdfDataset.openDataset(testFile.getAbsolutePath());
  }

  @AfterClass
  public static void cleanupSpec() throws IOException {
    ncDataset.close();
  }

  @Test
  public void testLogicalSection() throws IOException {
    // get logicalSection of the original 'section' variable
    Variable sectionVar = ncDataset.findVariable("section");

    // new shape is the same as the section's shape (':,0:2:2,1:2')
    assertThat(sectionVar.getShape()).isEqualTo(new int[] {2, 2, 2}); // Shape was originally [ 2, 3, 4 ].

    // data only includes values that are part of the section
    assertThat(sectionVar.read().getStorage()).isEqualTo(new int[] {2, 3, 10, 11, 14, 15, 22, 23});
  }

  @Test
  public void testLogicalSlice() throws IOException {
    // get logicalSlice of the original 'slice' variable
    Variable sliceVar = ncDataset.findVariable("slice");

    // new shape excludes the dimension we sliced out
    assertThat(sliceVar.getShape()).isEqualTo(new int[] {3, 4}); // Shape was originally [ 2, 3, 4 ].

    // data only includes values that are part of the slice
    assertThat(sliceVar.read().getStorage()).isEqualTo(new int[] {13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24});
  }

  @Test
  public void testLogicalReduce() throws IOException {
    // get logicalReduce of the original 'reduce' variable
    Variable reduceVar = ncDataset.findVariable("reduce");

    // new shape excludes the dimensions of length==1
    assertThat(reduceVar.getShape()).isEqualTo(new int[] {2, 3}); // Shape was originally [ 2, 1, 3, 1 ].

    // data includes all of the values from the original variable
    assertThat(reduceVar.read().getStorage()).isEqualTo(new int[] {1, 2, 3, 4, 5, 6});
  }
}
