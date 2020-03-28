/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import java.io.IOException;

/*
 * From https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_8h_source.html on 3/26/2020
 * #define NC_FORMAT_CLASSIC (1)
 * 
 * After adding CDF5 support, the NC_FORMAT_64BIT flag is somewhat confusing. So, it is renamed.
 * Note that the name in the contributed code NC_FORMAT_64BIT was renamed to NC_FORMAT_CDF2
 * 
 * #define NC_FORMAT_64BIT_OFFSET (2)
 * #define NC_FORMAT_64BIT (NC_FORMAT_64BIT_OFFSET)
 * #define NC_FORMAT_NETCDF4 (3)
 * #define NC_FORMAT_NETCDF4_CLASSIC (4)
 * #define NC_FORMAT_64BIT_DATA (5)
 * 
 * /// Alias
 * #define NC_FORMAT_CDF5 NC_FORMAT_64BIT_DATA
 * 
 * #define NC_FORMATX_NC3 (1)
 * #define NC_FORMATX_NC_HDF5 (2)
 * #define NC_FORMATX_NC4 NC_FORMATX_NC_HDF5
 * #define NC_FORMATX_NC_HDF4 (3)
 * #define NC_FORMATX_PNETCDF (4)
 * #define NC_FORMATX_DAP2 (5)
 * #define NC_FORMATX_DAP4 (6)
 * #define NC_FORMATX_UDF0 (8)
 * #define NC_FORMATX_UDF1 (9)
 * #define NC_FORMATX_ZARR (10)
 * #define NC_FORMATX_UNDEFINED (0)
 * 
 * To avoid breaking compatibility (such as in the python library), we need to retain the NC_FORMAT_xxx format as well.
 * This may come
 * out eventually, as the NC_FORMATX is more clear that it's an extended format specifier.
 * 
 * #define NC_FORMAT_NC3 NC_FORMATX_NC3
 * #define NC_FORMAT_NC_HDF5 NC_FORMATX_NC_HDF5
 * #define NC_FORMAT_NC4 NC_FORMATX_NC4
 * #define NC_FORMAT_NC_HDF4 NC_FORMATX_NC_HDF4
 * #define NC_FORMAT_PNETCDF NC_FORMATX_PNETCDF
 * #define NC_FORMAT_DAP2 NC_FORMATX_DAP2
 * #define NC_FORMAT_DAP4 NC_FORMATX_DAP4
 * #define NC_FORMAT_UNDEFINED NC_FORMATX_UNDEFINED
 */

/*
 * From https://www.unidata.ucar.edu/software/netcdf/docs/faq.html#How-many-netCDF-formats-are-there-and-what-are-the-
 * differences-among-them
 * 
 * Q: How many netCDF formats are there, and what are the differences among them?
 * 
 * A: There are four netCDF format variants:
 * 
 * - the classic format
 * - the 64-bit offset format
 * - the 64-bit data format
 * - the netCDF-4 format
 * - the netCDF-4 classic model format
 * 
 * (In addition, there are two textual representations for netCDF data, though these are not usually thought of as
 * formats: CDL and NcML.)
 * 
 * The classic format was the only format for netCDF data created between 1989 and 2004 by the reference software from
 * Unidata.
 * It is still the default format for new netCDF data files, and the form in which most netCDF data is stored.
 * This format is also referred as CDF-1 format.
 * 
 * In 2004, the 64-bit offset format variant was added. Nearly identical to netCDF classic format, it allows users to
 * create and access
 * far larger datasets than were possible with the original format. (A 64-bit platform is not required to write or read
 * 64-bit offset
 * netCDF files.) This format is also referred as CDF-2 format.
 * 
 * In 2008, the netCDF-4 format was added to support per-variable compression, multiple unlimited dimensions, more
 * complex data types,
 * and better performance, by layering an enhanced netCDF access interface on top of the HDF5 format.
 * 
 * At the same time, a fourth format variant, netCDF-4 classic model format, was added for users who needed the
 * performance benefits
 * of the new format (such as compression) without the complexity of a new programming interface or enhanced data model.
 * 
 * In 2016, the 64-bit data format variant was added. To support large variables with more than 4-billion array
 * elements, it replaces
 * most of the 32-bit integers used in the format specification with 64-bit integers. It also adds support for several
 * new data types
 * including unsigned byte, unsigned short, unsigned int, signed 64-bit int and unsigned 64-bit int. A 64-bit platform
 * is required to
 * write or read 64-bit data netCDF files. This format is also referred as CDF-5 format.
 * 
 * With each additional format variant, the C-based reference software from Unidata has continued to support access to
 * data stored in
 * previous formats transparently, and to also support programs written using previous programming interfaces.
 * 
 * Although strictly speaking, there is no single "netCDF-3 format", that phrase is sometimes used instead of the more
 * cumbersome but
 * correct "netCDF classic CDF-1, 64-bit offset CDF-2, or 64-bit data CDF-5 format" to describe files created by the
 * netCDF-3
 * (or netCDF-1 or netCDF-2) libraries. Similarly "netCDF-4 format" is sometimes used informally to mean "either the
 * general netCDF-4 format
 * or the restricted netCDF-4 classic model format". We will use these shorter phrases in FAQs below when no confusion
 * is likely.
 * 
 * A more extensive description of the netCDF formats and a formal specification of the classic and 64-bit formats is
 * available as a
 * NASA ESDS community standard
 * (https://earthdata.nasa.gov/sites/default/files/esdswg/spg/rfc/esds-rfc-011/ESDS-RFC-011v2.00.pdf)
 * 
 * The 64-bit data CDF-5 format specification is available in
 * http://cucis.ece.northwestern.edu/projects/PnetCDF/CDF-5.html.
 */

/** Enumeration of the kinds of NetCDF file formats. NETCDF3_64BIT_DATA is not currently supported. */
public enum NetcdfFileFormat {
  INVALID(0, "Invalid"), //
  NETCDF3(1, "netcdf-3"), //
  NETCDF3_64BIT_OFFSET(2, "netcdf-3 64bit-offset"), //
  NETCDF4(3, "netcdf-4"), // This is really just HDF-5, dont know yet if its written by netcdf4.
  NETCDF4_CLASSIC(4, "netcdf-4 classic"), // psuedo format I think
  NETCDF3_64BIT_DATA(5, "netcdf-5"), // what is this ?

  NCSTREAM(42, "ncstream"); // No assigned version, not part of C library. Does this belong here?

  private static final int MAGIC_NUMBER_LEN = 8;
  private static final long MAXHEADERPOS = 50000; // header's gotta be within this range

  private static final byte[] H5HEAD = {(byte) 0x89, 'H', 'D', 'F', '\r', '\n', (byte) 0x1a, '\n'};
  private static final byte[] CDF1HEAD = {(byte) 'C', (byte) 'D', (byte) 'F', (byte) 0x01};
  private static final byte[] CDF2HEAD = {(byte) 'C', (byte) 'D', (byte) 'F', (byte) 0x02};
  private static final byte[] CDF5HEAD = {(byte) 'C', (byte) 'D', (byte) 'F', (byte) 0x05};

  private final int version;
  private final String formatName;

  NetcdfFileFormat(int version, String formatName) {
    this.version = version;
    this.formatName = formatName;
  }

  public int version() {
    return version;
  }

  public String formatName() {
    return formatName;
  }

  public boolean isNetdf3format() {
    return this == NETCDF3 || this == NETCDF3_64BIT_OFFSET || this == NETCDF3_64BIT_DATA;
  }

  public boolean isNetdf4format() {
    return this == NETCDF4 || this == NETCDF4_CLASSIC;
  }

  public boolean isExtendedModel() {
    return this == NETCDF4 || this == NCSTREAM;
  }

  /**
   * Figure out what kind of netcdf-related file we have.
   * Constraint: leave raf read pointer to point just after the magic number.
   *
   * @param raf to test type
   * @return NetcdfFileFormat that matches constants in netcdf-c/include/netcdf.h, or INVALID if not a netcdf file.
   */
  public static NetcdfFileFormat findNetcdfFormatType(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    byte[] magic = new byte[MAGIC_NUMBER_LEN];

    // If this is not an HDF5 file, then the magic number is at
    // position 0; If it is an HDF5 file, then we need to search
    // forward for it.

    // Look for the relevant leading tag
    raf.seek(0);
    if (raf.readBytes(magic, 0, MAGIC_NUMBER_LEN) < MAGIC_NUMBER_LEN) // why read 8, why not 4 ??
      return NetcdfFileFormat.INVALID;

    int hdrlen = CDF1HEAD.length; // all CDF headers are assumed to be same length

    NetcdfFileFormat format = null;
    if (memequal(CDF1HEAD, magic, CDF1HEAD.length))
      format = NETCDF3;
    else if (memequal(CDF2HEAD, magic, CDF2HEAD.length))
      format = NETCDF3_64BIT_OFFSET;
    else if (memequal(CDF5HEAD, magic, CDF5HEAD.length))
      format = NETCDF3_64BIT_DATA;

    if (format != null) {
      raf.seek(hdrlen);
      return format;
    }

    // For HDF5, we need to search forward
    long filePos = 0;
    long size = raf.length();
    while ((filePos < size - 8) && (filePos < MAXHEADERPOS)) {
      boolean match;
      raf.seek(filePos);
      if (raf.readBytes(magic, 0, MAGIC_NUMBER_LEN) < MAGIC_NUMBER_LEN)
        return INVALID; // unknown
      // Test for HDF5
      if (memequal(H5HEAD, magic, H5HEAD.length)) {
        format = NETCDF4;
        break;
      }
      filePos = (filePos == 0) ? 512 : 2 * filePos;
    }
    if (format != null)
      raf.seek(filePos + H5HEAD.length);
    return format == null ? INVALID : format;
  }

  public static boolean memequal(byte[] b1, byte[] b2, int len) {
    if (b1 == b2)
      return true;
    if (b1 == null || b2 == null)
      return false;
    if (b1.length < len || b2.length < len)
      return false;
    for (int i = 0; i < len; i++) {
      if (b1[i] != b2[i])
        return false;
    }
    return true;
  }

  /**
   * Determine if the given name can be used for a NetCDF object, i.e. a Dimension, Attribute, or Variable.
   * The allowed name syntax (in RE form) is:
   *
   * <pre>
   * ([a-zA-Z0-9_]|{UTF8})([^\x00-\x1F\x7F/]|{UTF8})*
   * </pre>
   *
   * where UTF8 represents a multi-byte UTF-8 encoding. Also, no trailing spaces are permitted in names. We do not
   * allow '/' because HDF5 does not permit slashes in names as slash is used as a group separator. If UTF-8 is
   * supported, then a multi-byte UTF-8 character can occur anywhere within an identifier.
   *
   * @param name the name to validate.
   * @return {@code true} if the name is valid.
   */
  // Implements "int NC_check_name(const char *name)" from NetCDF-C:
  // https://github.com/Unidata/netcdf-c/blob/v4.3.3.1/libdispatch/dstring.c#L169
  // Should match makeValidNetcdfObjectName()
  public static boolean isValidNetcdfObjectName(String name) {
    if (name == null || name.isEmpty()) { // Null and empty names disallowed
      return false;
    }

    int cp = name.codePointAt(0);

    // First char must be [a-z][A-Z][0-9]_ | UTF8
    if (cp <= 0x7f) {
      if (!('A' <= cp && cp <= 'Z') && !('a' <= cp && cp <= 'z') && !('0' <= cp && cp <= '9') && cp != '_') {
        return false;
      }
    }

    for (int i = 1; i < name.length(); ++i) {
      cp = name.codePointAt(i);

      // handle simple 0x00-0x7f characters here
      if (cp <= 0x7f) {
        if (cp < ' ' || cp > 0x7E || cp == '/') { // control char, DEL, or forward-slash
          return false;
        }
      }
    }

    // trailing spaces disallowed
    return cp > 0x7f || !Character.isWhitespace(cp);

  }

  /**
   * Convert a name to a legal netcdf-3 name.
   *
   * @param name the name to convert.
   * @return the converted name.
   * @see #isValidNetcdfObjectName(String)
   */
  public static String makeValidNetcdfObjectName(String name) {
    StringBuilder sb = new StringBuilder(name);

    while (sb.length() > 0) {
      int cp = sb.codePointAt(0);

      // First char must be [a-z][A-Z][0-9]_ | UTF8
      if (cp <= 0x7f) {
        if (!('A' <= cp && cp <= 'Z') && !('a' <= cp && cp <= 'z') && !('0' <= cp && cp <= '9') && cp != '_') {
          sb.deleteCharAt(0);
          continue;
        }
      }
      break;
    }

    for (int pos = 1; pos < sb.length(); ++pos) {
      int cp = sb.codePointAt(pos);

      // handle simple 0x00-0x7F characters here
      if (cp <= 0x7F) {
        if (cp < ' ' || cp > 0x7E || cp == '/') { // control char, DEL, or forward-slash
          sb.deleteCharAt(pos);
          --pos;
        }
      }
    }

    while (sb.length() > 0) {
      int cp = sb.codePointAt(sb.length() - 1);

      if (cp <= 0x7f && Character.isWhitespace(cp)) {
        sb.deleteCharAt(sb.length() - 1);
      } else {
        break;
      }
    }

    if (sb.length() == 0) {
      throw new IllegalArgumentException(String.format("Illegal NetCDF object name: '%s'", name));
    }

    return sb.toString();
  }
}
