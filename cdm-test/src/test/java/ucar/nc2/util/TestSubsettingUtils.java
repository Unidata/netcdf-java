package ucar.nc2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Random;

import ucar.nc2.internal.util.CompareArrayToArray;

import static com.google.common.truth.Truth.assertThat;

/**
 * Utilities to read and subset data
 *
 * @author caron
 * @since 3/25/12
 */
public class TestSubsettingUtils {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void subsetVariables(String filename, String varName, int ntrials)
      throws InvalidRangeException, IOException {
    // varName = NetcdfFile.makeValidCdmObjectName(varName);
    System.out.println("testVariableSubset=" + filename + "," + varName);

    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {

      Variable v = ncfile.findVariable(varName);
      if (v == null) {
        System.out.printf("Cant Find %s%n", varName);
        for (Variable v2 : ncfile.getAllVariables())
          System.out.printf("  %s%n", v2.getFullName());
      }
      assertThat(v).isNotNull();
      int[] shape = v.getShape();

      // read entire array
      Array<?> A;
      try {
        A = v.readArray();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }

      int[] dataShape = A.getShape();
      assert dataShape.length == shape.length;
      for (int i = 0; i < shape.length; i++)
        assert dataShape[i] == shape[i];
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
    assert sdata.getRank() == s.getRank();
    int[] sshape = sdata.getShape();
    for (int i = 0; i < sshape.length; i++)
      assert sshape[i] == s.getShape(i);

    // compare with logical section
    Array<?> Asection = Arrays.section(fullData, s);
    int[] ashape = Asection.getShape();
    assert (ashape.length == sdata.getRank());
    for (int i = 0; i < ashape.length; i++)
      assert sshape[i] == ashape[i];

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
