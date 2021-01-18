package tests;

import examples.CdmDataTypesExamples;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

public class TestCdmDataTypesExamples {
  private static String dataPathStr = TestDir.cdmLocalFromTestDataDir + "testWriteRecord.nc";

  private static NetcdfFile ncfile;

  @BeforeClass
  public static void setUpTests() throws IOException {
    ncfile = NetcdfFiles.open(dataPathStr,  -1, null,
            "AddRecordStructure");
  }

  @AfterClass
  public static void cleanUpTests() throws IOException {
    ncfile.close();
  }

  @Test
  public void testReadToArrayExample() throws IOException {
    Variable v = ncfile.findVariable("lat");
    CdmDataTypesExamples.readToArrayExample(v);
  }

  @Test
  public void testReadStructureDataExample() throws IOException {
    Structure record = (Structure) ncfile.findVariable("record");
    StructureDataIterator iter = record.getStructureIterator();
    StructureData data = iter.next();

    CdmDataTypesExamples.readStructureDataExample(data, "rh");
  }

  @Test
  public void testReadVariableLengthDimensionExample() throws IOException, InvalidRangeException {
    CdmDataTypesExamples.readVariableLengthDimensionExample(ncfile, "rh");
  }

  @Test
  public void testBadReadExample() throws IOException, InvalidRangeException {

  }

  @Test
  public void testReadStructureDataTypesExample() {

  }

  @Test
  public void testReadNestedStructureDataExmaple() {

  }

  @Test
  public void testReadEnumValuesExample() {

  }
}
