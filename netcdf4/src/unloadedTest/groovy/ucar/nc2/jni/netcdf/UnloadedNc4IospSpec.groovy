package ucar.nc2.jni.netcdf

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import ucar.nc2.Attribute

/**
 * Test various aspects of Nc4Iosp when the C lib is NOT loaded.
 *
 * @author cwardgar
 * @since 2016-12-27
 */
class UnloadedNc4IospSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(UnloadedNc4IospSpec)
    
    def "flush in define mode, without C lib loaded"() {
        setup:
        Nc4reader nc4Iosp = new Nc4reader()
        
        when: "flush while still in define mode"
        nc4Iosp.flush()
        
        then: "no NullPointerException is thrown"
        notThrown NullPointerException  // Would fail before the bug fix in this commit.
    }
    
    def "updateAttribute in define mode, without C lib loaded"() {
        setup:
        Nc4reader nc4Iosp = new Nc4reader()
        
        when: "updateAttribute while still in define mode"
        nc4Iosp.updateAttribute(null, new Attribute("foo", "bar"))
        
        then: "no IOException is thrown"
        notThrown NullPointerException  // Would fail before the bug fix in this commit.
    }
}
