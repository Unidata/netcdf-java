package ucar.ma2;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.StructureMembers.Member;
import ucar.ma2.StructureMembers.MemberBuilder;

@RunWith(Parameterized.class)
public class TestStructureMembers {
  @Parameterized.Parameters()
  public static List<Object[]> getTestParameters() {
    final int[] shape = new int[] {1};
    List<Object[]> testCases = new ArrayList<>();

    testCases.add(new Object[] {"name", "description", "units", DataType.BOOLEAN, shape, true});
    testCases.add(new Object[] {"otherName", "description", "units", DataType.BOOLEAN, shape, false});
    testCases.add(new Object[] {"name", "otherDescription", "units", DataType.BOOLEAN, shape, false});
    testCases.add(new Object[] {"name", null, "units", DataType.BOOLEAN, shape, false});
    testCases.add(new Object[] {"name", "description", "OtherUnits", DataType.BOOLEAN, shape, false});
    testCases.add(new Object[] {"name", "description", null, DataType.BOOLEAN, shape, false});
    testCases.add(new Object[] {"name", "description", "units", DataType.INT, shape, false});
    testCases.add(new Object[] {"name", "description", "units", DataType.BOOLEAN, new int[] {2}, false});
    testCases.add(new Object[] {"name", "description", "units", DataType.BOOLEAN, new int[] {-1}, false});
    testCases.add(new Object[] {"name", "description", "units", DataType.BOOLEAN, new int[] {1, 1}, false});

    return testCases;
  }

  private final String name;
  private final String description;
  private final String units;
  private final DataType dataType;
  private final int[] shape;
  private final boolean isEqual;

  public TestStructureMembers(String name, String description, String units, DataType dataType, int[] shape,
      boolean isEqual) {
    this.name = name;
    this.description = description;
    this.units = units;
    this.dataType = dataType;
    this.shape = shape;
    this.isEqual = isEqual;
  }

  @Test
  public void shouldCompareMembers() {
    final Member member =
        StructureMembers.builder().addMember("name", "description", "units", DataType.BOOLEAN, new int[] {1}).build();

    final Member otherMember = StructureMembers.builder().addMember(name, description, units, dataType, shape).build();

    assertThat(member.equals(otherMember)).isEqualTo(isEqual);
    assertThat(otherMember.equals(member)).isEqualTo(isEqual);
  }

  @Test
  public void shouldCompareStructureMembers() {
    final MemberBuilder memberBuilder =
        StructureMembers.builder().addMember("name", "description", "units", DataType.BOOLEAN, new int[] {1});
    final StructureMembers structureMembers = StructureMembers.builder().addMember(memberBuilder).build();

    final MemberBuilder otherMember = StructureMembers.builder().addMember(name, description, units, dataType, shape);
    final StructureMembers otherStructureMembers = StructureMembers.builder().addMember(otherMember).build();

    assertThat(structureMembers.equals(otherStructureMembers)).isEqualTo(isEqual);
    assertThat(otherStructureMembers.equals(structureMembers)).isEqualTo(isEqual);
  }

  @Test
  public void shouldCompareStructureMembersWithMultipleMembers() {
    final MemberBuilder memberBuilder =
        StructureMembers.builder().addMember("name1", "description1", "units1", DataType.INT, new int[] {2});
    final MemberBuilder memberBuilder2 =
        StructureMembers.builder().addMember("name", "description", "units", DataType.BOOLEAN, new int[] {1});
    final StructureMembers structureMembers =
        StructureMembers.builder().addMember(memberBuilder).addMember(memberBuilder2).build();

    final MemberBuilder otherMemberBuilder =
        StructureMembers.builder().addMember("name1", "description1", "units1", DataType.INT, new int[] {2});
    final MemberBuilder otherMemberBuilder2 =
        StructureMembers.builder().addMember(name, description, units, dataType, shape);
    final StructureMembers otherStructureMembers =
        StructureMembers.builder().addMember(otherMemberBuilder).addMember(otherMemberBuilder2).build();

    assertThat(structureMembers.equals(otherStructureMembers)).isEqualTo(isEqual);
    assertThat(otherStructureMembers.equals(structureMembers)).isEqualTo(isEqual);
  }

  @Test
  public void shouldCompareMembersThatAreStructures() {
    final MemberBuilder innerMemberBuilder =
        StructureMembers.builder().addMember("name", "description", "units", DataType.BOOLEAN, new int[] {1});
    final StructureMembers innerStructureMembers = StructureMembers.builder().addMember(innerMemberBuilder).build();
    final Member member =
        StructureMembers.builder().addMember("name2", "description2", "units2", DataType.STRUCTURE, new int[] {1})
            .setStructureMembers(innerStructureMembers).build();

    final MemberBuilder otherInnerMember =
        StructureMembers.builder().addMember(name, description, units, dataType, shape);
    final StructureMembers otherInnerStructureMembers = StructureMembers.builder().addMember(otherInnerMember).build();
    final Member otherMember =
        StructureMembers.builder().addMember("name2", "description2", "units2", DataType.STRUCTURE, new int[] {1})
            .setStructureMembers(otherInnerStructureMembers).build();

    assertThat(member.equals(otherMember)).isEqualTo(isEqual);
    assertThat(otherMember.equals(member)).isEqualTo(isEqual);
  }
}
