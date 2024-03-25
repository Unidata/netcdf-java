package ucar.nc2.write;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;

public class TestNcmlWriter {
  private static NetcdfFile ncFile;

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  /*
   * Programmatically creates a NetcdfFile with the following (pseudo) CDL:
   * netcdf {
   * types:
   * short enum dessertType { 'pie' = 18, 'donut' = 268, 'cake' = 3284};
   * dimensions:
   * time = UNLIMITED; // (3 currently)
   * variables:
   * enum dessertType dessert(time=3);
   * :zero = ; // long
   * short time(time=3);
   * char charVar(5);
   * String stringVar(4);
   * group: recordsGroup {
   * variables:
   * Structure {
   * int recordsVar(3);
   * } recordsStruct(*);
   * // group attributes:
   * :stooges = "Moe Howard", "Larry Fine", "Curly Howard";
   * }
   * // global attributes:
   * :primes = 2U, 3U, 5U, 7U, 11U; // int
   * }
   * data:
   * dessert =
   * {18, 268, 3284}
   * time =
   * {4, 5, 6}
   * charVar = "abcde"
   * stringVar = "Frodo Baggins", "Samwise Gamgee", "Meriadoc Brandybuck", "Peregrin Took"
   * recordsGroup/recordsStruct = UNREADABLE
   */
  @BeforeClass
  public static void setup() {
    // NetcdfFile's 0-arg constructor is protected, so must use NetcdfFileSubclass
    ncFile = new NetcdfFileSubclass();

    // create shared, unlimited Dimension
    Dimension timeDim = new Dimension("time", 3, true, true, false);
    ncFile.addDimension(null, timeDim);

    // create EnumTypedef and add it to root group
    Map<Integer, String> dessertValues = new HashMap<>();
    dessertValues.put(18, "pie");
    dessertValues.put(268, "donut");
    dessertValues.put(3284, "cake");
    EnumTypedef dessertType = new EnumTypedef("dessertType", dessertValues, DataType.ENUM2);
    ncFile.getRootGroup().addEnumeration(dessertType);

    // create Variable of type dessertType and add it
    Variable dessert = new Variable(ncFile, null, null, "dessert", DataType.ENUM2, "time");
    dessert.setEnumTypedef(dessertType);
    dessert.addAttribute(new Attribute("zero", DataType.ULONG)); // unsigned, zero-length, LONG attribute
    short[] dessertStorage = new short[] {18, 268, 3284};
    // Irregularly-spaced values
    dessert.setCachedData(Array.factory(DataType.SHORT, new int[] {3}, dessertStorage), true);
    ncFile.addVariable(null, dessert);

    // create 'time' coordinate Variable
    Variable time = new Variable(ncFile, null, null, "time", DataType.SHORT, "time");
    short[] timeStorage = new short[] {4, 5, 6};
    time.setCachedData(Array.factory(DataType.SHORT, new int[] {3}, timeStorage), false);
    ncFile.addVariable(null, time);

    // create char-valued Variable with anonymous Dimension
    Variable charVar = new Variable(ncFile, null, null, "charVar", DataType.CHAR, "5");
    char[] charStorage = new char[] {'a', 'b', 'c', 'd', 'e'};
    charVar.setCachedData(Array.factory(DataType.CHAR, new int[] {5}, charStorage), true);
    ncFile.addVariable(null, charVar);

    // create string-valued Variable
    Variable stringVar = new Variable(ncFile, null, null, "stringVar", DataType.STRING, "4");
    String[] stringStorage = new String[] {"Frodo Baggins", "Samwise Gamgee", "Meriadoc Brandybuck", "Peregrin Took"};
    stringVar.setCachedData(Array.factory(DataType.STRING, new int[] {4}, stringStorage), true);
    ncFile.addVariable(null, stringVar);

    // create Group for records
    Group recordsGroup = new Group(ncFile, null, "recordsGroup");
    ncFile.addGroup(null, recordsGroup);

    // create unreadable Structure with variable-length dimension and add it to recordsGroup
    // recordsStruct will be unreadable because we don't cache any data for it. In fact, it's not even possible
    // to cache data for Structures because ArrayStructure.copy() is unsupported, and caching needs that.
    // Besides, there's no sensible way to represent a n>1-dimensional Structure's values in NcML anyway.
    Structure recordsStruct = new Structure(ncFile, null, null, "recordsStruct");
    Dimension numRecords = new Dimension("numRecords", -1, false, false, true); // Variable-length dim
    recordsStruct.setDimensions(Arrays.asList(numRecords));
    recordsGroup.addVariable(recordsStruct);

    // create record Variable and add it to the records Structure
    Variable recordsVar = new Variable(ncFile, recordsGroup, recordsStruct, "recordsVar", DataType.INT, "3");
    recordsStruct.addMemberVariable(recordsVar);

    // create group attribute containing multiple string values
    Attribute stoogesAttrib = new Attribute("stooges", Arrays.asList("Moe Howard", "Larry Fine", "Curly Howard"));
    recordsGroup.addAttribute(stoogesAttrib);

    // create global attribute with multiple unsigned integer values
    Attribute primesAttrib = new Attribute("primes", Arrays.asList(2, 3, 5, 7, 11), true);
    ncFile.addAttribute(null, primesAttrib);

    // finish
    ncFile.finish();
  }

  private static String expectedNcmlResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<netcdf xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">\n" + "  <explicit />\n"
      + "  <enumTypedef name=\"dessertType\" type=\"enum2\">\n" + "    <enum key=\"18\">pie</enum>\n"
      + "    <enum key=\"268\">donut</enum>\n" + "    <enum key=\"3284\">cake</enum>\n" + "  </enumTypedef>\n"
      + "  <dimension name=\"time\" length=\"3\" isUnlimited=\"true\" />\n"
      + "  <variable name=\"dessert\" shape=\"time\" type=\"enum2\" typedef=\"dessertType\">\n"
      + "    <attribute name=\"zero\" type=\"ulong\" />\n" + "    <values>18 268 3284</values>\n" + "  </variable>\n"
      + "  <variable name=\"time\" shape=\"time\" type=\"short\">\n"
      + "    <values start=\"4.0\" increment=\"1.0\" npts=\"3\" />\n" + "  </variable>\n"
      + "  <variable name=\"charVar\" shape=\"5\" type=\"char\">\n" + "    <values>abcde</values>\n" + "  </variable>\n"
      + "  <variable name=\"stringVar\" shape=\"4\" type=\"String\">\n"
      + "    <values separator=\"|\">Frodo Baggins|Samwise Gamgee|Meriadoc Brandybuck|Peregrin Took</values>\n"
      + "  </variable>\n" + "  <group name=\"recordsGroup\">\n"
      + "    <variable name=\"recordsStruct\" shape=\"*\" type=\"Structure\">\n"
      + "      <variable name=\"recordsVar\" shape=\"3\" type=\"int\" />\n" + "    </variable>\n"
      + "    <attribute name=\"stooges\" value=\"Moe Howard|Larry Fine|Curly Howard\" separator=\"|\" />\n"
      + "  </group>\n" + "  <attribute name=\"primes\" type=\"uint\" value=\"2 3 5 7 11\" />\n" + "</netcdf>\n";

  private static final NcmlWriter ncmlWriter = new NcmlWriter();

  @Test
  public void testGettersAndSetters() {
    // set NetcdfFile properties and exercise namespace and xmlFormat getters/setters"

    Namespace namespace = Namespace.NO_NAMESPACE; // Exercise setter.
    Format xmlFormat = Format.getRawFormat().setOmitDeclaration(true);
    NcmlWriter ncmlWriterO = new NcmlWriter(namespace, xmlFormat, null);

    NetcdfFile emptyNcFile = new NetcdfFileSubclass();
    emptyNcFile.setLocation("file:SOME_FILE");
    emptyNcFile.setId("SOME_ID");
    emptyNcFile.setTitle("NcmlWriter Test");
    emptyNcFile.finish();

    Element netcdfElem = ncmlWriterO.makeNetcdfElement(emptyNcFile, null);
    assertThat(ncmlWriterO.writeToString(netcdfElem))
        .isEqualTo("<netcdf location=\"file:SOME_FILE\" id=\"SOME_ID\" title=\"NcmlWriter Test\" />\r\n");

    assertThat(ncmlWriterO.getNamespace()).isEqualTo(namespace); // Exercise getter.

    assertThat(ncmlWriterO.getXmlFormat()).isEqualTo(xmlFormat); // Exercise getter.
  }

  @Test
  public void testMakeDimensionThrowsForPrivateDimension() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> ncmlWriter.makeDimensionElement(Dimension.builder("private", 8).setIsShared(false).build()));
    assertThat(exception.getMessage())
        .isEqualTo("Cannot create private dimension: in NcML, <dimension> elements are always shared.");
  }

  @Test
  public void testTimeIsCoordinate() {
    assertThat(NcmlWriter.writeCoordinateVariablesPredicate.test(ncFile.findVariable("time"))).isTrue();
  }

  @Test
  public void testMetadataVariables() {
    assertThat(NcmlWriter.writeMetadataVariablesPredicate.test(ncFile.findVariable("charVar"))).isTrue();
    assertThat(NcmlWriter.writeMetadataVariablesPredicate.test(ncFile.findVariable("stringVar"))).isTrue();
    assertThat(NcmlWriter.writeMetadataVariablesPredicate.test(ncFile.findVariable("dessert"))).isTrue();
  }

  @Test
  public void testWriteVariablesWithNamesPredicate() {
    // recordsGroup/recordsStruct can be selected with WriteVariablesWithNamesPredicate

    Predicate<Variable> writeVarsPred =
        new NcmlWriter.WriteVariablesWithNamesPredicate(Arrays.asList("recordsGroup/recordsStruct"));

    assertThat(writeVarsPred.test(ncFile.findVariable("recordsGroup/recordsStruct"))).isTrue();
  }

  @Test
  public void testWriteUsingCompoundVariablesPredicate() {
    // "time"
    Predicate<Variable> predicate1 = (Predicate<Variable>) NcmlWriter.writeCoordinateVariablesPredicate;
    // "charVar", "stringVar", "dessert"
    Predicate<Variable> predicate2 = (Predicate<Variable>) NcmlWriter.writeMetadataVariablesPredicate;
    Predicate<Variable> predicate3 =
        new NcmlWriter.WriteVariablesWithNamesPredicate(Arrays.asList("recordsGroup/recordsStruct"));
    Predicate<Variable> compoundPred = predicate1.or(predicate2).or(predicate3);

    NcmlWriter ncmlWriterO = new NcmlWriter(null, null, compoundPred);

    assertThat(ncmlWriterO.getWriteValuesPredicate()).isEqualTo(compoundPred); // Exercise getter.

    for (Variable var : ncFile.getVariables()) {
      assertThat(compoundPred.test(var)).isTrue();
    }

    Element netcdfElem = ncmlWriterO.makeExplicitNetcdfElement(ncFile, null);
    assertThat(ncmlWriterO.writeToString(netcdfElem)).isEqualTo(expectedNcmlResult);
  }

  @Test
  public void testWriteAndRead() throws IOException {
    // Don't try to write values 'recordsGroup/recordsStruct' this time; that already failed in previous method.
    // Also, the NetcdfDataset that NcMLReader returns will try to generate missing values, which we don't want.

    // "time"
    Predicate<Variable> predicate1 = (Predicate<Variable>) NcmlWriter.writeCoordinateVariablesPredicate;
    // "charVar", "stringVar", "dessert"
    Predicate<Variable> predicate2 = (Predicate<Variable>) NcmlWriter.writeMetadataVariablesPredicate;
    Predicate<Variable> compoundPred = predicate1.or(predicate2);


    NcmlWriter ncmlWriterO = new NcmlWriter(null, null, compoundPred);
    File outFile = tempFolder.newFile("TestNcmlWriter.ncml");

    Element netcdfElem = ncmlWriterO.makeExplicitNetcdfElement(ncFile, null);
    ncmlWriterO.writeToFile(netcdfElem, outFile);

    String content = new String(Files.readAllBytes(outFile.toPath()));
    assertThat(content).isEqualTo(expectedNcmlResult);

    try (NetcdfDataset readerDataset = NcMLReader.readNcML(outFile.toURI().toURL().toString(), null)) {
      readerDataset.setLocation(null); // Leaving this non-null would screw up our comparison.
      Element readerNetcdfElem = ncmlWriterO.makeExplicitNetcdfElement(readerDataset, null);

      assertThat(ncmlWriterO.writeToString(readerNetcdfElem)).isEqualTo(expectedNcmlResult);
    }
  }
}
