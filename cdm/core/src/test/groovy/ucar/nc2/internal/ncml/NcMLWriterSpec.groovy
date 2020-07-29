/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */

package ucar.nc2.internal.ncml

import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.output.Format
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification
import ucar.ma2.Array
import ucar.ma2.DataType
import ucar.nc2.*
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.dataset.NetcdfDatasets
import ucar.nc2.write.NcmlWriter

import java.util.function.Predicate

/**
 * @author cwardgar
 * @since 2015/08/05
 */
class NcMLWriterSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(NcMLWriterSpec)
    
    @Shared
    NetcdfFile ncFile

    /* Programmatically creates a NetcdfFile with the following (pseudo) CDL:
netcdf {
  types:
    short enum dessertType { 'pie' = 18, 'donut' = 268, 'cake' = 3284};
  dimensions:
    time = UNLIMITED;   // (3 currently)
  variables:
    enum dessertType dessert(time=3);
      :zero = ; // long
    short time(time=3);
    char charVar(5);
    String stringVar(4);
  group: recordsGroup {
    variables:
      Structure {
        int recordsVar(3);
      } recordsStruct(*);
    // group attributes:
    :stooges = "Moe Howard", "Larry Fine", "Curly Howard";
  }
  // global attributes:
  :primes = 2U, 3U, 5U, 7U, 11U; // int
}
data:
dessert =
  {18, 268, 3284}
time =
  {4, 5, 6}
charVar = "abcde"
stringVar = "Frodo Baggins", "Samwise Gamgee", "Meriadoc Brandybuck", "Peregrin Took"
recordsGroup/recordsStruct = UNREADABLE
     */
    def setupSpec() {
        setup: "NetcdfFile's 0-arg constructor is protected, so must use NetcdfFileSubclass"
        Group.Builder root = Group.builder();

        and: "create shared, unlimited Dimension"
        Dimension timeDim = new Dimension("time", 3, true, true, false)
        root.addDimension(timeDim)

        and: "create EnumTypedef and add it to root group"
        EnumTypedef dessertType = new EnumTypedef("dessertType", [18: 'pie', 268: 'donut', 3284: 'cake'], DataType.ENUM2)
        root.addEnumTypedef(dessertType)

        and: "create Variable of type dessertType and add it"
        Variable.Builder dessert = Variable.builder().setName("dessert").setDataType(DataType.ENUM2)
            .setParentGroupBuilder(root).setDimensionsByName("time").setEnumTypeName("dessertType")
            .addAttribute(Attribute.emptyValued("zero", DataType.ULONG)) ; // unsigned, zero-length, LONG attribute
        short[] dessertStorage = [18, 268, 3284] as short[]
        dessert.setCachedData(Array.factory(DataType.SHORT, [3] as int[], dessertStorage), true)  // Irregularly-spaced values
        root.addVariable(dessert)

        and: "create 'time' coordinate Variable"
        Variable.Builder time = Variable.builder().setName("time").setDataType(DataType.SHORT)
                .setParentGroupBuilder(root).setDimensionsByName("time");
        short[] timeStorage = [4, 5, 6] as short[]
        time.setCachedData(Array.factory(DataType.SHORT, [3] as int[], timeStorage), false)
        root.addVariable(time)

        and: "create char-valued Variable with anonymous Dimension"
        Variable.Builder charVar = Variable.builder().setName("charVar").setDataType(DataType.CHAR)
                .setParentGroupBuilder(root).setDimensionsByName("5");
        char[] charStorage = ['a', 'b', 'c', 'd', 'e'] as char[]
        charVar.setCachedData(Array.factory(DataType.CHAR, [5] as int[], charStorage), true)
        root.addVariable(charVar)

        and: "create string-valued Variable"
        Variable.Builder stringVar = Variable.builder().setName("stringVar").setDataType(DataType.STRING)
                .setParentGroupBuilder(root).setDimensionsByName("4");
        String[] stringStorage = ['Frodo Baggins', 'Samwise Gamgee', 'Meriadoc Brandybuck', 'Peregrin Took'] as String[]
        stringVar.setCachedData(Array.factory(DataType.STRING, [4] as int[], stringStorage), true)
        root.addVariable(stringVar)

        and: "create Group for records"
        Group.Builder recordsGroup = Group.builder().setName("recordsGroup");
        root.addGroup(recordsGroup)

        and: "create unreadable Structure with variable-length dimension and add it to recordsGroup"
        // recordsStruct will be unreadable because we don't cache any data for it. In fact, it's not even possible
        // to cache data for Structures because ArrayStructure.copy() is unsupported, and caching needs that.
        // Besides, there's no sensible way to represent a n>1-dimensional Structure's values in NcML anyway.
        Structure.Builder recordsStruct = Structure.builder().setName("recordsStruct");
        Dimension numRecords = new Dimension("numRecords", -1, false, false, true)  // Variable-length dim
        recordsStruct.setDimensions([numRecords])
        recordsGroup.addVariable(recordsStruct)

        and: "create record Variable and add it to the records Structure"
        Variable.Builder recordsVar = Variable.builder().setName("recordsVar").setDataType(DataType.INT)
                .setParentGroupBuilder(recordsGroup).setDimensionsByName("3");
        recordsStruct.addMemberVariable(recordsVar)

        and: "create group attribute containing multiple string values"
        Attribute stoogesAttrib = Attribute.builder("stooges").setValues( ['Moe Howard', 'Larry Fine', 'Curly Howard'], false).build()
        recordsGroup.addAttribute(stoogesAttrib)

        and: "create global attribute with multiple unsigned integer values"
        Attribute primesAttrib = Attribute.builder("primes").setValues([2, 3, 5, 7, 11], true).build();
        root.addAttribute(primesAttrib)

        and: "finish"
        ncFile = NetcdfFile.builder().setRootGroup(root).build();
        ncFile.finish();

        printf "CDL %s%n", ncFile
    }

    @Shared String expectedNcmlResult = '''\
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
  <explicit />
  <enumTypedef name="dessertType" type="enum2">
    <enum key="18">pie</enum>
    <enum key="268">donut</enum>
    <enum key="3284">cake</enum>
  </enumTypedef>
  <dimension name="time" length="3" isUnlimited="true" />
  <variable name="dessert" shape="time" type="enum2" typedef="dessertType">
    <attribute name="zero" type="ulong" />
    <values>18.0 268.0 3284.0</values>
  </variable>
  <variable name="time" shape="time" type="short">
    <values start="4.0" increment="1.0" npts="3" />
  </variable>
  <variable name="charVar" shape="5" type="char">
    <values>abcde</values>
  </variable>
  <variable name="stringVar" shape="4" type="String">
    <values separator="|">Frodo Baggins|Samwise Gamgee|Meriadoc Brandybuck|Peregrin Took</values>
  </variable>
  <group name="recordsGroup">
    <variable name="recordsStruct" shape="*" type="Structure">
      <variable name="recordsVar" shape="3" type="int" />
    </variable>
    <attribute name="stooges" value="Moe Howard|Larry Fine|Curly Howard" separator="|" />
  </group>
  <attribute name="primes" type="uint" value="2 3 5 7 11" />
</netcdf>
'''

    NcmlWriter ncmlWriter
    def setup() {
        ncmlWriter = new NcmlWriter();
    }

    def "set NetcdfFile properties and exercise namespace and xmlFormat getters/setters"() {
        Namespace namespace = Namespace.NO_NAMESPACE   // Exercise setter.
        Format xmlFormat = Format.rawFormat.setOmitDeclaration(true)
        NcmlWriter ncmlWriterO = new NcmlWriter(namespace, xmlFormat, null);

        NetcdfFile emptyNcFile = new NetcdfFileSubclass()
        emptyNcFile.setLocation("file:SOME_FILE");
        emptyNcFile.setId("SOME_ID")
        emptyNcFile.setTitle("NcmlWriter Test")
        emptyNcFile.finish()

        expect:
        Element netcdfElem = ncmlWriterO.makeNetcdfElement(emptyNcFile, null)
        ncmlWriterO.writeToString(netcdfElem) ==
                '<netcdf location="file:SOME_FILE" id="SOME_ID" title="NcmlWriter Test" />\r\n'

        and: "getter returns namespace"
        ncmlWriterO.getNamespace() == namespace  // Exercise getter.

        and: "getter returns format"
        ncmlWriterO.getXmlFormat() == xmlFormat  // Exercise getter.
    }

    def "makeDimensionElement() throws exception for private Dimension"() {
        when:
        ncmlWriter.makeDimensionElement(Dimension.builder("private", 8).setIsShared(false).build());

        then:
        IllegalArgumentException e = thrown()
        e.message == "Cannot create private dimension: in NcML, <dimension> elements are always shared."
    }

    def "'time' is a coordinate variable"() {
        expect:
        NcmlWriter.writeCoordinateVariablesPredicate.test(ncFile.findVariable("time"))
    }

    def "'charVar', 'stringVar', and 'dessert' are metadata variables"() {
        expect:
        ['charVar', 'stringVar', 'dessert'].every {
            NcmlWriter.writeMetadataVariablesPredicate.test(ncFile.findVariable(it))
        }
    }

    def "'recordsGroup/recordsStruct' can be selected with WriteVariablesWithNamesPredicate"() {
        setup:
        Predicate<Variable> writeVarsPred =
                new NcmlWriter.WriteVariablesWithNamesPredicate(['recordsGroup/recordsStruct'])

        expect:
        writeVarsPred.test(ncFile.findVariable('recordsGroup/recordsStruct'))
    }

    def "write to String using compound writeVariablesPredicate"() {
        Predicate<? super Variable> compoundPred =
                NcmlWriter.writeCoordinateVariablesPredicate   // "time"
                .or(NcmlWriter.writeMetadataVariablesPredicate)     // "charVar", "stringVar", "dessert"
                .or(new NcmlWriter.WriteVariablesWithNamesPredicate(['recordsGroup/recordsStruct']));

        NcmlWriter ncmlWriterO = new NcmlWriter(null, null, compoundPred);

        for (Variable v : ncFile.variables) {
            printf "%s == %s isCoord = %s%n", v.getShortName(), compoundPred.test(v), NcmlWriter.writeCoordinateVariablesPredicate.test(v)
        }

        expect: "getter returns instance just set"
        ncmlWriterO.getWriteValuesPredicate() == compoundPred  // Exercise getter.

        and: "compoundPred applies to every Variable in ncFile"
        ncFile.variables.every { compoundPred.test(it) }

        and: "generated NcML string will match expectedNcmlResult"
        Element netcdfElem = ncmlWriterO.makeExplicitNetcdfElement(ncFile, null)
        println ncmlWriterO.writeToString(netcdfElem)
        println "\n\n" + expectedNcmlResult
        ncmlWriterO.writeToString(netcdfElem) == expectedNcmlResult
    }

    // TODO: This is an integration test and probably runs much slower than the other tests in this class.
    // How to categorize it and only execute it in certain environments?
    def "round-trip: write to File and read back in, using NcMLReader"() {
        // Don't try to write values 'recordsGroup/recordsStruct' this time; that already failed in previous method.
        // Also, the NetcdfDataset that NcMLReader returns will try to generate missing values, which we don't want.
        Predicate<? super Variable> compoundPred =
                NcmlWriter.writeCoordinateVariablesPredicate   // "time"
                .or(NcmlWriter.writeMetadataVariablesPredicate)     // "charVar", "stringVar", "dessert"

        NcmlWriter ncmlWriterO = new NcmlWriter(null, null, compoundPred);
        File outFile = File.createTempFile("NcMLWriterSpec", ".ncml")

        when: "write NcML to file"
        Element netcdfElem = ncmlWriterO.makeExplicitNetcdfElement(ncFile, null)
        ncmlWriterO.writeToFile(netcdfElem, outFile)

        then: "file's content matches expectedNcmlResult"
        outFile.text == expectedNcmlResult

        when: "read in NcML file and create a NetcdfDataset"
        NetcdfDataset readerDataset = NetcdfDatasets.openDataset(outFile.toURI().toURL().toString(), false, null)

        and: "get the NcML representation of the dataset"
        readerDataset.setLocation(null)  // Leaving this non-null would screw up our comparison.
        Element readerNetcdfElem = ncmlWriterO.makeExplicitNetcdfElement(readerDataset, null)

        then: "it matches expectedNcmlResult"
        ncmlWriterO.writeToString(readerNetcdfElem) == expectedNcmlResult

        cleanup:
        readerDataset?.close()
        outFile?.delete()
    }
}
