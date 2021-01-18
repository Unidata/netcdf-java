package examples;

import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

public class CdmDataTypesExamples {

  /**
   * Code snippet to read a variable into an Array
   * @param variableInstance
   * @throws IOException
   */
  public static void readToArrayExample(Variable variableInstance) throws IOException {
    Array data = variableInstance.read();
  }

  /**
   * Code snippet to read StructureData into an Array
   * @param structureData
   * @param memberName
   * @throws IOException
   */
  public static void readStructureDataExample(StructureData structureData, String memberName) {
    Array data = structureData.getArray(memberName);
  }

  /**
   * Code snippet to read a variable length dimension
   * @param ncfile
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static void readVariableLengthDimensionExample(NetcdfFile ncfile, String varName) throws IOException, InvalidRangeException {
    // read data
    Variable v = ncfile.findVariable(varName);
    boolean vlen = v.isVariableLength();
    Array data = v.read();

    // loop over outer, variable length dimension
    while (data.hasNext()) {
      Array arr= (Array) data.next(); // inner fixed length array of ints
    }

    // subset ok on inner, fixed length dimension
    data = v.read(":, :");

    // subset using Section
    data = v.read(Section.builder().appendRange(0, 9, 2).appendRange(null).build());
  }

  /**
   * Code snippet demonstrating bad read for a variable dimension
   * @param v
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static void badReadExample(Variable v) throws IOException, InvalidRangeException {
    int[] origin = new int[] {0, 0};
    int[] size = new int[] {3, -1};
    Array data = v.read(origin, size); // throws
  }

  public static void readStructureDataTypesExample(StructureData structureData, String memberName) {
    structureData.getScalarDouble(memberName);
    structureData.getJavaArrayInt(memberName);
  }

  public static void readNestedStructureDataExample(StructureData structureData, String memberName) {
    StructureData sd = structureData.getScalarStructure(memberName);
    ArrayStructure arr_struct = structureData.getArrayStructure(memberName);
    ArraySequence arr_seq = structureData.getArraySequence(memberName);
  }

  public static void readEnumValuesExample(Variable var) throws IOException {
    if (var.getDataType().isEnum()) {
      Array rawValues = var.read();
      Array enumValues = Array.factory(DataType.STRING, rawValues.getShape());
      IndexIterator ii = enumValues.getIndexIterator();


      // use implicit Array iterator
      while (rawValues.hasNext()) {
        String sval = var.lookupEnumString(rawValues.nextInt());
        ii.setObjectNext(sval);
      }
    }
  }
}
