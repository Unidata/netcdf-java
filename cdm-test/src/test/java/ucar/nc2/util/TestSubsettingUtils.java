package ucar.nc2.util;

import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import java.io.IOException;
import java.util.Random;

import ucar.nc2.internal.util.CompareArrayToArray;

import static com.google.common.truth.Truth.assertThat;

/**
 * Utilities to read and subset data
 */
public class TestSubsettingUtils {

  public static void subsetVariables(String filename, String varName, int ntrials)
      throws InvalidRangeException, IOException {
    System.out.println("testVariableSubset=" + filename + "," + varName);

    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {

      Variable v = ncfile.findVariable(varName);
      if (v == null) {
        System.out.printf("Cant Find %s%n", varName);
        for (Variable v2 : ncfile.getVariables()) {
          System.out.printf("  %s%n", v2.getFullName());
        }
      }
      assertThat(v).isNotNull();
      int[] shape = v.getShape();

      // read entire array
      Array<?> A = v.readArray();

      int[] dataShape = A.getShape();
      assertThat(dataShape.length).isEqualTo(shape.length);
      for (int i = 0; i < shape.length; i++) {
        assertThat(dataShape[i]).isEqualTo(shape[i]);
      }
      Section all = v.getSection();
      System.out.println("  Entire dataset=" + all);

      for (int k = 0; k < ntrials; k++) {
        // create a random subset, read and compare
        subsetVariable(v, randomSubset(all, 1), A);
        subsetVariable(v, randomSubset(all, 2), A);
        subsetVariable(v, randomSubset(all, 3), A);
      }
    }
  }

  public static void subsetVariables(String filename, String varName, Section s)
      throws InvalidRangeException, IOException {
    System.out.println("testVariableSubset=" + filename + "," + varName);

    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable v = ncfile.findVariable(varName);
      assertThat(v).isNotNull();
      subsetVariable(v, s, v.readArray());
    }
  }

  private static void subsetVariable(Variable v, Section s, Array<?> fullData)
      throws IOException, InvalidRangeException {
    System.out.println("   section=" + s);

    // read just that
    Array<?> sdata = v.readArray(s);
    assertThat(sdata.getRank()).isEqualTo(s.getRank());
    int[] sshape = sdata.getShape();
    for (int i = 0; i < sshape.length; i++)
      assertThat(sshape[i]).isEqualTo(s.getShape(i));

    // compare with logical section
    Array<?> Asection = Arrays.section(fullData, s);
    int[] ashape = Asection.getShape();
    assertThat(ashape.length).isEqualTo(sdata.getRank());
    for (int i = 0; i < ashape.length; i++)
      assertThat(sshape[i]).isEqualTo(ashape[i]);

    CompareArrayToArray.compareData(v.getShortName(), sdata, Asection);
  }

  private static Section randomSubset(Section all, int stride) throws InvalidRangeException {
    Section.Builder sb = Section.builder();
    for (Range r : all.getRanges()) {
      int first = random(r.first(), r.last() / 2);
      int last = random(r.last() / 2, r.last());
      sb.appendRange(first, last, stride);
    }
    return sb.build();
  }

  private static Random r = new Random(System.currentTimeMillis());

  private static int random(int first, int last) {
    return first + r.nextInt(last - first + 1);
  }

}
