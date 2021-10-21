package ucar.nc2.jni.netcdf

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Unroll
import ucar.array.ArrayType
import ucar.array.Array
import ucar.array.Arrays
import ucar.ma2.ArrayChar
import ucar.nc2.Attribute
import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFiles
import ucar.nc2.Variable
import ucar.nc2.iosp.NetcdfFileFormat
import ucar.nc2.write.NetcdfFormatWriter

/**
 * Tests miscellaneous aspects of Nc4Iosp.
 *
 * @author cwardgar
 * @since 2017-03-27
 */
class Nc4IospMiscSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(Nc4IospMiscSpec)
    
    @Rule TemporaryFolder tempFolder = new TemporaryFolder()
    
    /*
     * Demonstrates bug from https://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/2017/msg00012.html
     * Prior to fix, this test would fail for 'u_short', 'u_int', and 'u_long' variables with
     * "Unknown userType == 8", "Unknown userType == 9", and "Unknown userType == 11" errors respectively.
     */
    @Unroll  // Report iterations of method independently.
    def "Nc4Iosp.readDataSection() can read '#varName' variables"() {
        setup: "locate test file"
        File file = new File(this.class.getResource("unsigned.nc4").toURI())
        assert file.exists()
        
        and: "open it as a NetcdfFile using Nc4Iosp"
        NetcdfFile ncFile = NetcdfFiles.open(file.absolutePath, Nc4reader.class.canonicalName, -1, null, null)
        
        and: "grab the Nc4Iosp instance within so that we can test Nc4Iosp.readDataSection()"
        Nc4reader nc4reader = ncFile.iosp as Nc4reader
        
        when: "read all of var's data using readDataSection()"
        Variable var = ncFile.findVariable(varName)
        Nc4reader.Vinfo vinfo = var.SPobject as Nc4reader.Vinfo
        Array<?> array = nc4reader.readDataSection(vinfo.g4.grpid, vinfo.varid, vinfo.typeid, var.section);
        
        then: "actual data equals expected data"
        Arrays.copyPrimitiveArray(array) == expectedData
    
        cleanup: "close NetcdfFile"
        ncFile?.close()
        
        where: "data are too big for their type. Overflow expected because Java doesn't support unsigned types"
        varName << [ "u_byte", "u_short", "u_int", "u_long" ]
        expectedData << [
                [(1  << 7),  (1  << 7)  + 1, (1  << 7)  + 2] as byte[],  // Will overflow to [-128, -127, -126]
                [(1  << 15), (1  << 15) + 1, (1  << 15) + 2] as short[],
                [(1  << 31), (1  << 31) + 1, (1  << 31) + 2] as int[],
                [(1L << 63), (1L << 63) + 1, (1L << 63) + 2] as long[]
        ];
    }
    
    /*
     * Demonstrates bug from
     * https://andy.unidata.ucar.edu/esupport/staff/index.php?_m=tickets&_a=viewticket&ticketid=28098
     * Prior to fix, primary2Dim and primary3Dim were not being identified as unlimited.
     */
    def "Nc4Iosp supports multiple groups, each containing an unlimited dimension"() {
        setup: "locate test file"
        File file = new File(this.class.getResource("DBP-690959.nc4").toURI())
        assert file.exists()
    
        and: "open it as a NetcdfFile using Nc4Iosp"
        NetcdfFile ncFile = NetcdfFiles.open(file.absolutePath, Nc4reader.class.canonicalName, -1, null, null)
    
        and: "find unlimited dimensions"
        Dimension primary1Dim = ncFile.findDimension("/group1/primary")
        Dimension primary2Dim = ncFile.findDimension("/group2/primary")
        Dimension primary3Dim = ncFile.findDimension("/group3/primary")
        
        expect: "all dimensions are unlimited"
        primary1Dim.isUnlimited()
        primary2Dim.isUnlimited()
        primary3Dim.isUnlimited()
    
        cleanup: "close NetcdfFile"
        ncFile?.close()
    }

    def "create NetCDF-4 file with unlimited dimension"() {
        setup: "create temp file that will be deleted after test by TemporaryFolder @Rule"
        File tempFile = new File(tempFolder.root, "Nc4IospMiscSpec.nc4")

        and: "open a NetcdfFormatWriter that will write NetCDF-4 to tempFile"
        NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, tempFile.absolutePath, null);

        and: "add an unlimited dimension and create the file on disk"
        Dimension dimBefore = Dimension.builder( "dim", 3).setIsUnlimited(true).build();
        writerb.addDimension( dimBefore);

        NetcdfFormatWriter writer = writerb.build();

        and: "close the file for writing and reopen it for reading"
        writer.close()
        NetcdfFile ncFile = NetcdfFiles.open(tempFile.absolutePath)

        expect: "the dimension is the same after the write/read round-trip"
        Dimension dimAfter = ncFile.findDimension(dimBefore.getShortName())
        // Failed prior to fix, because dimAfter was not unlimited.
        dimBefore.equals dimAfter

        cleanup: "close writer and reader"
        writer?.close()  // Under normal circumstances, this will already be closed. Luckily method is idempotent.
        ncFile?.close()
    }

    def "create NetCDF-4 file null valued attributes"() {
        setup: "create temp file that will be deleted after test by TemporaryFolder @Rule"
        File tempFile = new File(tempFolder.root, "Nc4IospMiscSpec.nc4")

        and: "open a NetcdfFormatWriter that will write NetCDF-4 to tempFile"
        NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, tempFile.absolutePath, null);

        and: "add a numerical valued attribute with a null value"
        Attribute attrNumBefore = Attribute.emptyValued("nullvalnum", ArrayType.INT)
        writerb.addAttribute(attrNumBefore)

        and: "add a string valued attribute with a null value"
        Attribute attrStrBefore = Attribute.emptyValued("nullvalstr", ArrayType.STRING)
        writerb.addAttribute(attrStrBefore)

        and: "add a character valued attribute with a null value"
        Attribute attrCharBefore = Attribute.emptyValued("nullvalchar", ArrayType.CHAR)
        writerb.addAttribute(attrCharBefore)

        and: "write file"

        NetcdfFormatWriter writer = writerb.build();

        and: "close the file for writing and reopen it for reading"
        writer.close()
        NetcdfFile ncFile = NetcdfFiles.open(tempFile.absolutePath)

        expect: "the value of the attributes are null"
        Attribute attrNumAfter = ncFile.findAttribute(attrNumBefore.getShortName())
        attrNumBefore.getArrayValues().equals attrNumAfter.getArrayValues()
        attrNumBefore.getArrayValues() == null

        Attribute attrStrAfter = ncFile.findAttribute(attrStrBefore.getShortName())
        attrStrBefore.getArrayValues().equals attrStrAfter.getArrayValues()
        attrStrBefore.getArrayValues() == null

        Attribute attrCharAfter = ncFile.findAttribute(attrCharBefore.getShortName())
        attrCharBefore.getArrayValues().equals attrCharAfter.getArrayValues()
        attrCharBefore.getArrayValues() == null

        Attribute attrNullCharAfter = ncFile.findAttribute(attrNullCharBefore.getShortName())
        attrNullCharBefore.getArrayValues().getSize() == attrNullCharAfter.getArrayValues().getSize()
        attrNullCharBefore.getArrayValues().getSize() == 1
        String val1 = attrNullCharBefore.getStringValue();
        String val2 = attrNullCharAfter.getStringValue();
        val1.length() == 0;
        val2.length() == 0;
        val1.equals(val2)

        cleanup: "close writer and reader"
        writer?.close()  // Under normal circumstances, this will already be closed. Luckily method is idempotent.
        ncFile?.close()
    }
}
