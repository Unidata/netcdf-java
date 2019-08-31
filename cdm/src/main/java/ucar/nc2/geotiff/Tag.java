/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geotiff;

import java.util.*;

/**
 * Type-safe enumeration of Tiff Tags. Not complete, just the ones weve actually seen.
 * *
 * 
 * @author caron
 */

class Tag implements Comparable<Tag> {
  private static Map<Integer, Tag> map = new HashMap<>();

  // general tiff tags
  public static final Tag NewSubfileType = new Tag("NewSubfileType", 254);
  public static final Tag ImageWidth = new Tag("ImageWidth", 256);
  public static final Tag ImageLength = new Tag("ImageLength", 257);
  public static final Tag BitsPerSample = new Tag("BitsPerSample", 258);
  public static final Tag Compression = new Tag("Compression", 259);
  public static final Tag PhotometricInterpretation = new Tag("PhotometricInterpretation", 262);
  public static final Tag FillOrder = new Tag("FillOrder", 266);
  public static final Tag DocumentName = new Tag("DocumentName", 269);
  public static final Tag ImageDescription = new Tag("ImageDescription", 270);
  public static final Tag StripOffsets = new Tag("StripOffsets", 273);
  public static final Tag Orientation = new Tag("Orientation", 274);
  public static final Tag SamplesPerPixel = new Tag("SamplesPerPixel", 277);
  public static final Tag RowsPerStrip = new Tag("RowsPerStrip", 278);
  public static final Tag StripByteCounts = new Tag("StripByteCounts", 279);
  public static final Tag XResolution = new Tag("XResolution", 282);
  public static final Tag YResolution = new Tag("YResolution", 283);
  public static final Tag PlanarConfiguration = new Tag("PlanarConfiguration", 284);
  public static final Tag ResolutionUnit = new Tag("ResolutionUnit", 296);
  public static final Tag PageNumber = new Tag("PageNumber", 297);
  public static final Tag Software = new Tag("Software", 305);
  public static final Tag ColorMap = new Tag("ColorMap", 320);
  public static final Tag TileWidth = new Tag("TileWidth", 322);
  public static final Tag TileLength = new Tag("TileLength", 323);
  public static final Tag TileOffsets = new Tag("TileOffsets", 324);
  public static final Tag TileByteCounts = new Tag("TileByteCounts", 325);
  public static final Tag SampleFormat = new Tag("SampleFormat", 339);
  public static final Tag SMinSampleValue = new Tag("SMinSampleValue", 340);
  public static final Tag SMaxSampleValue = new Tag("SMaxSampleValue", 341);

  // tiff tags used for geotiff
  public static final Tag ModelPixelScaleTag = new Tag("ModelPixelScaleTag", 33550);
  public static final Tag IntergraphMatrixTag = new Tag("IntergraphMatrixTag", 33920);
  public static final Tag ModelTiepointTag = new Tag("ModelTiepointTag", 33922);
  public static final Tag ModelTransformationTag = new Tag("ModelTransformationTag", 34264);
  public static final Tag GeoKeyDirectoryTag = new Tag("GeoKeyDirectoryTag", 34735);
  public static final Tag GeoDoubleParamsTag = new Tag("GeoDoubleParamsTag", 34736);
  public static final Tag GeoAsciiParamsTag = new Tag("GeoAsciiParamsTag", 34737);

  // Gdal tiff tags
  public static final Tag GDALNoData = new Tag("GDALNoDataTag", 42113);

  /**
   * Get the Tag by number.
   * 
   * @param code Tiff Tag number.
   * @return Tag or null if no match.
   */
  static Tag get(int code) {
    return map.get(code);
  }

  private String name;
  private int code;

  private Tag(String name, int code) {
    this.name = name;
    this.code = code;
    map.put(code, this);
  }

  /** for unknown tags */
  Tag(int code) {
    this.code = code;
    // map.put( new Integer(code), this);
  }

  /** get the Tag name. may be null */
  public String getName() {
    return name;
  }

  /** get the Tag number */
  public int getCode() {
    return code;
  }

  /* String representation */
  public String toString() {
    return name == null ? code + " " : code + " (" + name + ")";
  }

  /** sort by number */
  public int compareTo(Tag o) {
    return Integer.compare(code, o.getCode());
  }

}
