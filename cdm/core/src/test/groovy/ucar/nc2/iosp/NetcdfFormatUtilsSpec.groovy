/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */

package ucar.nc2.iosp

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

/**
 * @author cwardgar
 * @since 2015/09/16
 */
class NetcdfFormatUtilsSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(NetcdfFormatUtilsSpec)
    
    def "test invalid NetCDF object names: null or empty"() {
        expect: "null names are invalid"
        !NetcdfFormatUtils.isValidNetcdfObjectName(null)

        when:
        NetcdfFormatUtils.makeValidNetcdfObjectName(null)
        then:
        thrown(NullPointerException)

        expect: "empty names are invalid"
        !NetcdfFormatUtils.isValidNetcdfObjectName("")

        when:
        NetcdfFormatUtils.makeValidNetcdfObjectName("")
        then:
        IllegalArgumentException e = thrown()
        e.message == "Illegal NetCDF object name: ''"
    }

    def "test invalid NetCDF object names: first char"() {
        expect: "names with first chars not in ([a-zA-Z0-9_]|{UTF8}) are invalid"
        !NetcdfFormatUtils.isValidNetcdfObjectName(" blah")
        NetcdfFormatUtils.makeValidNetcdfObjectName(" blah") == "blah"

        !NetcdfFormatUtils.isValidNetcdfObjectName("\n/blah")
        NetcdfFormatUtils.makeValidNetcdfObjectName("\n/blah") == "blah"

        !NetcdfFormatUtils.isValidNetcdfObjectName("\u001F\u007F blah")  // Unit separator and DEL
        NetcdfFormatUtils.makeValidNetcdfObjectName("\u001F\u007F blah") == "blah"
    }

    def "test invalid NetCDF object names: remaining chars"() {
        expect: "names with remaining chars not in ([^\\x00-\\x1F\\x7F/]|{UTF8})* are invalid"
        !NetcdfFormatUtils.isValidNetcdfObjectName("1\u000F2\u007F3/4")
        NetcdfFormatUtils.makeValidNetcdfObjectName("1\u000F2\u007F3/4") == "1234"

        and: "names may not have trailing spaces"
        !NetcdfFormatUtils.isValidNetcdfObjectName("foo     ")
        NetcdfFormatUtils.makeValidNetcdfObjectName("foo     ") == "foo"
    }

    def "test valid NetCDF object names"() {
        expect: "valid names have syntax: ([a-zA-Z0-9_]|{UTF8})([^\\x00-\\x1F\\x7F/]|{UTF8})*"
        NetcdfFormatUtils.isValidNetcdfObjectName("_KfS9Jn_s9__")
        NetcdfFormatUtils.makeValidNetcdfObjectName("_KfS9Jn_s9__") == "_KfS9Jn_s9__"

        and: "unicode characters greater than 0x7F can appear anywhere"
        NetcdfFormatUtils.isValidNetcdfObjectName("\u0123\u1234\u2345\u3456")
        NetcdfFormatUtils.makeValidNetcdfObjectName("\u0123\u1234\u2345\u3456") == "\u0123\u1234\u2345\u3456"
    }
}
