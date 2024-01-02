package ucar.ma2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.StructureMembers.MemberBuilder;

@RunWith(Enclosed.class)
public class TestArray {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @RunWith(Parameterized.class)
  public static class TestArrayParameterized {
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getTestParameters() {
      final int[] shape = new int[] {1};
      List<Object[]> testCases = new ArrayList<>();

      testCases.add(new Object[] {DataType.BOOLEAN, shape, new boolean[] {false}, new boolean[] {false, false}});
      testCases.add(new Object[] {DataType.BYTE, shape, new byte[] {1}, new byte[] {1, 1}});
      testCases.add(new Object[] {DataType.CHAR, shape, new char[] {'a'}, new char[] {'a', 'a'}});
      testCases.add(new Object[] {DataType.SHORT, shape, new short[] {1}, new short[] {1, 1}});
      testCases.add(new Object[] {DataType.INT, shape, new int[] {1}, new int[] {1, 1}});
      testCases.add(new Object[] {DataType.LONG, shape, new long[] {1}, new long[] {1, 1}});
      testCases.add(new Object[] {DataType.FLOAT, shape, new float[] {1.1f}, new float[] {1.1f, 1.1f}});
      testCases.add(new Object[] {DataType.DOUBLE, shape, new double[] {1.1}, new double[] {1.1, 1.1}});
      testCases.add(new Object[] {DataType.SEQUENCE, shape, new Object[] {1}, new Object[] {1, 1}});
      testCases.add(new Object[] {DataType.STRING, shape, new Object[] {1}, new Object[] {1, 1}});
      testCases.add(new Object[] {DataType.STRUCTURE, shape, new Object[] {1}, new Object[] {1, 1}});
      testCases.add(new Object[] {DataType.ENUM1, shape, new byte[] {1}, new byte[] {1, 1}});
      testCases.add(new Object[] {DataType.ENUM2, shape, new short[] {1}, new short[] {1, 1}});
      testCases.add(new Object[] {DataType.ENUM4, shape, new int[] {1}, new int[] {1, 1}});
      testCases.add(new Object[] {DataType.OPAQUE, shape, new Object[] {1}, new Object[] {1, 1}});
      // Cannot be created through the factory
      // testCases.add(new Object[] {DataType.OBJECT, shape, new Object[]{1}, new Object[]{1, 1}});
      testCases.add(new Object[] {DataType.UBYTE, shape, new byte[] {1}, new byte[] {1, 1}});
      testCases.add(new Object[] {DataType.USHORT, shape, new short[] {1}, new short[] {1, 1}});
      testCases.add(new Object[] {DataType.UINT, shape, new int[] {1}, new int[] {1, 1}});
      testCases.add(new Object[] {DataType.ULONG, shape, new long[] {1}, new long[] {1, 1}});

      return testCases;
    }

    private final DataType dataType;
    private final int[] shape;
    private final Object storage;
    private final Object expectedCombinedArray;

    public TestArrayParameterized(DataType dataType, int[] shape, Object storage, Object expectedCombinedArray) {
      this.dataType = dataType;
      this.shape = shape;
      this.storage = storage;
      this.expectedCombinedArray = expectedCombinedArray;
    }

    @Test
    public void shouldCreateArray() {
      final Array array = Array.factory(dataType, shape);
      assertThat(array.getShape()).isEqualTo(shape);
    }

    @Test
    public void shouldCreateArrayWithStorage() {
      final Array array = Array.factory(dataType, shape, storage);
      assertThat(array.getShape()).isEqualTo(shape);
    }

    @Test
    public void shouldCreateArrayWithIndexAndStorage() {
      final Array array = Array.factory(dataType, Index.factory(shape), storage);
      assertThat(array.getShape()).isEqualTo(shape);
    }

    @Test
    public void shouldCombineTwoArrays() {
      // tested separately below
      assumeTrue(dataType != DataType.STRUCTURE);

      final int[] combinedShape = new int[] {2};

      final Array array = Array.factory(dataType, shape, storage);
      final List<Array> arrays = new ArrayList<>();
      arrays.add(array);
      arrays.add(array);

      final Array combinedArray = Array.factoryCopy(dataType, combinedShape, arrays);

      assertThat(combinedArray.getShape()).isEqualTo(combinedShape);
      assertThat(combinedArray.getStorage()).isEqualTo(expectedCombinedArray);
    }

    @Test
    public void shouldCombineTwoArrayStructures() {
      // skip structures within structures
      assumeTrue(dataType != DataType.STRUCTURE);

      final Array array = Array.factory(dataType, shape, storage);
      final ArrayStructure arrayStructure = createArrayStructure("name", dataType, shape, array);

      final int[] combinedShape = new int[] {2};
      final List<Array> arrays = new ArrayList<>();
      arrays.add(arrayStructure);
      arrays.add(arrayStructure);

      final ArrayStructure combinedArray =
          (ArrayStructure) Array.factoryCopy(DataType.STRUCTURE, combinedShape, arrays);

      assertThat(combinedArray.getShape()).isEqualTo(combinedShape);
      assertThat(combinedArray.getArray(0, arrayStructure.findMember("name"))).isEqualTo(array);
      assertThat(combinedArray.getArray(1, arrayStructure.findMember("name"))).isEqualTo(array);
    }
  }

  public static class TestArrayNonParameterized {

    @Test
    public void shouldCombineTwoArrayStructuresWithDifferentValues() {
      final DataType dataType = DataType.INT;
      final int[] shape = new int[] {1};
      final Array array = Array.factory(dataType, shape);
      array.setInt(0, 5);
      final Array array2 = Array.factory(dataType, shape);
      array.setInt(0, 42);

      final ArrayStructure arrayStructure = createArrayStructure("name", dataType, shape, array);
      final ArrayStructure arrayStructure2 = createArrayStructure("name", dataType, shape, array2);

      final int[] combinedShape = new int[] {2};
      final List<Array> arrays = new ArrayList<>();
      arrays.add(arrayStructure);
      arrays.add(arrayStructure2);

      final ArrayStructure combinedArray =
          (ArrayStructure) Array.factoryCopy(DataType.STRUCTURE, combinedShape, arrays);

      assertThat(combinedArray.getShape()).isEqualTo(combinedShape);
      assertThat(combinedArray.getArray(0, arrayStructure.findMember("name"))).isEqualTo(array);
      assertThat(combinedArray.getArray(1, arrayStructure.findMember("name"))).isEqualTo(array2);
    }

    @Test
    public void shouldRefuseToCombineArrayStructuresWithDifferentMembers() {
      final DataType dataType = DataType.INT;
      final int[] shape = new int[] {1};
      final Array array = Array.factory(dataType, shape);

      final int[] combinedShape = new int[] {2};
      final List<Array> arrays = new ArrayList<>();
      arrays.add(createArrayStructure("name", DataType.INT, new int[] {1}, array));
      arrays.add(createArrayStructure("name2", DataType.INT, new int[] {1}, array));

      assertThrows(IllegalArgumentException.class, () -> Array.factoryCopy(DataType.STRUCTURE, combinedShape, arrays));
    }
  }

  private static ArrayStructure createArrayStructure(String memberName, DataType dataType, int[] shape, Array array) {
    final MemberBuilder memberBuilder =
        StructureMembers.builder().addMember(memberName, "desc", "units", dataType, shape);
    final StructureMembers members = StructureMembers.builder().addMember(memberBuilder).build();
    final StructureDataW structureData = new StructureDataW(members);
    structureData.setMemberData(memberName, array);
    return new ArrayStructureW(structureData);
  }
}
