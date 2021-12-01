/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf4;

import javax.annotation.concurrent.Immutable;

/** HDF4 Tags */
@Immutable
public class TagEnum {
  private static final java.util.Map<Short, TagEnum> hash = new java.util.HashMap<>(100);

  public static final int SPECIAL_LINKED = 1; /* Fixed-size Linked blocks */
  public static int SPECIAL_EXT = 2; /* External */
  public static final int SPECIAL_COMP = 3; /* Compressed */
  public static int SPECIAL_VLINKED = 4; /* Variable-length linked blocks */
  public static final int SPECIAL_CHUNKED = 5; /* chunked element */
  public static int SPECIAL_BUFFERED = 6; /* Buffered element */
  public static int SPECIAL_COMPRAS = 7; /* Compressed Raster element */

  public static int COMP_CODE_NONE; // don't encode at all, just store
  public static int COMP_CODE_RLE = 1; // for simple RLE encoding
  public static final int COMP_CODE_NBIT = 2; // for N-bit encoding
  public static int COMP_CODE_SKPHUFF = 3; // for Skipping huffman encoding
  public static final int COMP_CODE_DEFLATE = 4; // for gzip 'deflate' encoding
  public static int COMP_CODE_SZIP = 5; // for szip encoding

  public static final TagEnum NONE = new TagEnum("NONE", "", (short) 0);
  public static final TagEnum NULL = new TagEnum("NULL", "", (short) 1);
  public static final TagEnum RLE = new TagEnum("RLE", "Run length encoding", (short) 11);
  public static final TagEnum IMC = new TagEnum("IMC", "IMCOMP compression alias", (short) 12);
  public static final TagEnum IMCOMP = new TagEnum("IMCOMP", "IMCOMP compression", (short) 12);
  public static final TagEnum JPEG = new TagEnum("JPEG", "JPEG compression (24-bit data)", (short) 13);
  public static final TagEnum GREYJPEG = new TagEnum("GREYJPEG", "JPEG compression (8-bit data)", (short) 14);
  public static final TagEnum JPEG5 = new TagEnum("JPEG5", "JPEG compression (24-bit data)", (short) 15);
  public static final TagEnum GREYJPEG5 = new TagEnum("GREYJPEG5", "JPEG compression (8-bit data)", (short) 16);

  public static final TagEnum LINKED = new TagEnum("LINKED", "Linked-block special element", (short) 20);
  public static final TagEnum VERSION = new TagEnum("VERSION", "Version", (short) 30);
  public static final TagEnum COMPRESSED = new TagEnum("COMPRESSED", "Compressed special element", (short) 40); // 0x28
  public static final TagEnum VLINKED = new TagEnum("VLINKED", "Variable-len linked-block header", (short) 50);
  public static final TagEnum VLINKED_DATA = new TagEnum("VLINKED_DATA", "Variable-len linked-block data", (short) 51);
  public static final TagEnum CHUNKED = new TagEnum("CHUNKED", "Chunked special element header", (short) 60);
  public static final TagEnum CHUNK = new TagEnum("CHUNK", "Chunk element", (short) 61); // 0x3d

  public static final TagEnum FID = new TagEnum("FID", "File identifier", (short) 100);
  public static final TagEnum FD = new TagEnum("FD", "File description", (short) 101);
  public static final TagEnum TID = new TagEnum("TID", "Tag identifier", (short) 102);
  public static final TagEnum TD = new TagEnum("TD", "Tag descriptor", (short) 103);
  public static final TagEnum DIL = new TagEnum("DIL", "Data identifier label", (short) 104);
  public static final TagEnum DIA = new TagEnum("DIA", "Data identifier annotation", (short) 105);
  public static final TagEnum NT = new TagEnum("NT", "Number type", (short) 106);
  public static final TagEnum MT = new TagEnum("MT", "Machine type", (short) 107);
  public static final TagEnum FREE = new TagEnum("FREE", "Free space in the file", (short) 108);

  public static final TagEnum ID8 = new TagEnum("ID8", "8-bit Image dimension", (short) 200); // obsolete
  public static final TagEnum IP8 = new TagEnum("IP8", "8-bit Image palette", (short) 201); // obsolete
  public static final TagEnum RI8 = new TagEnum("RI8", "Raster-8 image", (short) 202); // obsolete
  public static final TagEnum CI8 = new TagEnum("CI8", "RLE compressed 8-bit image", (short) 203); // obsolete
  public static final TagEnum II8 = new TagEnum("II8", "IMCOMP compressed 8-bit image", (short) 204); // obsolete

  public static final TagEnum ID = new TagEnum("ID", "Image DimRec", (short) 300);
  public static final TagEnum LUT = new TagEnum("LUT", "Image Palette", (short) 301);
  public static final TagEnum RI = new TagEnum("RI", "Raster Image", (short) 302);
  public static final TagEnum CI = new TagEnum("CI", "Compressed Image", (short) 303);
  public static final TagEnum NRI = new TagEnum("NRI", "New-format Raster Image", (short) 304);
  public static final TagEnum RIG = new TagEnum("RIG", "Raster Image Group", (short) 306);
  public static final TagEnum LD = new TagEnum("LD", "Palette DimRec", (short) 307);
  public static final TagEnum MD = new TagEnum("MD", "Matte DimRec", (short) 308);
  public static final TagEnum MA = new TagEnum("MA", "Matte Data", (short) 309);
  public static final TagEnum CCN = new TagEnum("CCN", "Color correction", (short) 310);
  public static final TagEnum CFM = new TagEnum("CFM", "Color format", (short) 311);
  public static final TagEnum AR = new TagEnum("AR", "Cspect ratio", (short) 312);

  public static final TagEnum DRAW = new TagEnum("DRAW", "Draw these images in sequence", (short) 400);
  public static final TagEnum RUN = new TagEnum("RUN", "Cun this as a program/script", (short) 401);

  public static final TagEnum XYP = new TagEnum("XYP", "X-Y position", (short) 500);
  public static final TagEnum MTO = new TagEnum("MTO", "Machine-type override", (short) 501);

  public static final TagEnum T14 = new TagEnum("T14", "TEK 4014 data", (short) 602);
  public static final TagEnum T105 = new TagEnum("T105", "TEK 4105 data", (short) 603);

  public static final TagEnum SDG = new TagEnum("SDG", "Scientific Data Group", (short) 700); // obsolete
  public static final TagEnum SDD = new TagEnum("SDD", "Scientific Data DimRec", (short) 701);
  public static final TagEnum SD = new TagEnum("SD", "Scientific Data", (short) 702);
  public static final TagEnum SDS = new TagEnum("SDS", "Scales", (short) 703);
  public static final TagEnum SDL = new TagEnum("SDL", "Labels", (short) 704);
  public static final TagEnum SDU = new TagEnum("SDU", "Units", (short) 705);
  public static final TagEnum SDF = new TagEnum("SDF", "Formats", (short) 706);
  public static final TagEnum SDM = new TagEnum("SDM", "Max/Min", (short) 707);
  public static final TagEnum SDC = new TagEnum("SDC", "Coord sys", (short) 708);
  public static final TagEnum SDT = new TagEnum("SDT", "Transpose", (short) 709); // obsolete
  public static final TagEnum SDLNK = new TagEnum("SDLNK", "Links related to the dataset", (short) 710);
  public static final TagEnum NDG = new TagEnum("NDG", "Numeric Data Group", (short) 720);
  public static final TagEnum CAL = new TagEnum("CAL", "Calibration information", (short) 731);
  public static final TagEnum FV = new TagEnum("FV", "Fill Value information", (short) 732);
  public static final TagEnum BREQ = new TagEnum("BREQ", "Beginning of required tags", (short) 799);
  public static final TagEnum SDRAG = new TagEnum("SDRAG", "List of ragged array line lengths", (short) 781);
  public static final TagEnum EREQ = new TagEnum("EREQ", "Current end of the range", (short) 780);

  public static final TagEnum VG = new TagEnum("VG", "Vgroup", (short) 1965);
  public static final TagEnum VH = new TagEnum("VH", "Vdata Header", (short) 1962);
  public static final TagEnum VS = new TagEnum("VS", "Vdata Storage", (short) 1963);

  private final String name;
  private final String desc;
  private final short code;

  private TagEnum(String name, String desc, short code) {
    this.name = name;
    this.desc = desc;
    this.code = code;
    hash.put(code, this);
  }

  public String getDesc() {
    return desc;
  }

  public String getName() {
    return name;
  }

  public short getCode() {
    return code;
  }

  public String toString() {
    return name + " (" + code + ") " + desc;
  }

  /**
   * Find the Tag that matches the code.
   *
   * @param code find Tag with this code.
   * @return Tag or null if no match.
   */
  public static TagEnum getTag(short code) {
    TagEnum te = hash.get(code);
    if (te == null)
      te = new TagEnum("UNKNOWN", "UNKNOWN", code);
    return te;
  }
}
