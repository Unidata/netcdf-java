package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;
import static ucar.ma2.MAMath.nearlyEquals;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.filter.Classifier;
import ucar.unidata.util.test.TestDir;

public class TestEnhanceClassifier {

  private static String dataDir = TestDir.cdmLocalTestDataDir + "ncml/enhance/";
  public static final int[] mixNumbers = {1, 0, 0, 1, 0};
  public static final Array DATA_mixNumbers = Array.makeFromJavaArray(mixNumbers);
  public static final int[] Classification_test =
      {0, -2147483648, 0, 0, 10, 10, 10, 100, 100, 100, -2147483648, 100, 1000, 1000, 1000, 1000, 1000};
  public static final Array CLASSIFICATION_TEST = Array.makeFromJavaArray(Classification_test);

  @Test
  public void testEnhanceClassifier_Ints() throws IOException {

    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testAddToClassifier.ncml", true, null)) {

      Variable classifySpecs = ncfile.findVariable("classify_ints");
      assertThat((Object) classifySpecs).isNotNull();
      assertThat(!classifySpecs.attributes().isEmpty()).isTrue();
      Array Data = classifySpecs.read();
      Classifier classifier = Classifier.createFromVariable(classifySpecs);
      int[] ClassifiedArray = classifier.classifyWithAttributes(Data);
      assertThat(nearlyEquals(Array.makeFromJavaArray(ClassifiedArray), DATA_mixNumbers)).isTrue();


    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  @Test
  public void testEnhanceClassifier_Floats() throws IOException {

    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testAddToClassifier.ncml", true, null)) {

      Variable classifySpecs = ncfile.findVariable("classify_floats");
      assertThat((Object) classifySpecs).isNotNull();
      assertThat(!classifySpecs.attributes().isEmpty()).isTrue();
      Array Data = classifySpecs.read();
      Classifier classifier = Classifier.createFromVariable(classifySpecs);
      int[] ClassifiedArray = classifier.classifyWithAttributes(Data);
      assertThat(nearlyEquals(Array.makeFromJavaArray(ClassifiedArray), DATA_mixNumbers)).isTrue();


    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  @Test
  public void testEnhanceClassifier_classification() throws IOException {

    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataDir + "testAddToClassifier.ncml", true, null)) {

      Variable classifySpecs = ncfile.findVariable("class_specs");
      assertThat((Object) classifySpecs).isNotNull();
      assertThat(!classifySpecs.attributes().isEmpty()).isTrue();
      Array Data = classifySpecs.read();
      Classifier classifier = Classifier.createFromVariable(classifySpecs);
      int[] ClassifiedArray = classifier.classifyWithAttributes(Data);
      assertThat(nearlyEquals(Array.makeFromJavaArray(ClassifiedArray), CLASSIFICATION_TEST)).isTrue();


    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

}
