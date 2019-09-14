/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geotiff;

import java.util.*;

/**
 * A GeoKey is a GeoKey.Tag and a value.
 * The value may be a String, an array of integers, or an array of doubles.
 *
 * @author caron
 */

class GeoKey {
  boolean isDouble, isString;
  private int count;
  private int[] value;
  private double[] dvalue;
  private String valueS;

  private GeoKey.Tag tag;
  private GeoKey.TagValue tagValue;

  private int id;

  /**
   * Construct a GeoKey with a Tag and TagValue
   * 
   * @param tag GeoKey.Tag number
   * @param tagValue value
   */
  GeoKey(GeoKey.Tag tag, GeoKey.TagValue tagValue) {
    this.tag = tag;
    this.tagValue = tagValue;
    count = 1;
  }

  GeoKey(GeoKey.Tag tag, int value) {
    this.tag = tag;
    this.value = new int[1];
    this.value[0] = value;
    count = 1;
  }

  GeoKey(GeoKey.Tag tag, int[] value) {
    this.tag = tag;
    this.value = value;
    count = value.length;
  }

  GeoKey(GeoKey.Tag tag, double[] value) {
    this.tag = tag;
    this.dvalue = value;
    count = value.length;
    isDouble = true;
  }

  GeoKey(GeoKey.Tag tag, double value) {
    this.tag = tag;
    this.dvalue = new double[1];
    this.dvalue[0] = value;
    count = 1;
    isDouble = true;
  }

  GeoKey(GeoKey.Tag tag, String value) {
    this.tag = tag;
    this.valueS = value;
    count = 1;
    isString = true;
  }

  int count() {
    return count;
  }

  int tagCode() {
    if (tag != null)
      return tag.code();
    return id;
  }

  int value() {
    if (tagValue != null)
      return tagValue.value();
    return value[0];
  }

  int value(int idx) {
    if (idx == 0)
      return value();
    return value[idx];
  }

  double valueD(int idx) {
    return dvalue[idx];
  }

  String valueString() {
    return valueS;
  }

  /**
   * Construct a GeoKey with a single integer value.
   * 
   * @param id GeoKey.Tag number
   * @param v value
   */
  GeoKey(int id, int v) {
    this.id = id;
    this.count = 1;

    this.tag = GeoKey.Tag.get(id);
    this.tagValue = TagValue.get(tag, v);

    if (tagValue == null) {
      this.value = new int[1];
      this.value[0] = v;
    }
  }

  public String toString() {
    StringBuilder sbuf = new StringBuilder();
    if (tag != null)
      sbuf.append(" geoKey = " + tag);
    else
      sbuf.append(" geoKey = " + id);

    if (tagValue != null)
      sbuf.append(" value = " + tagValue);
    else {

      sbuf.append(" values = ");

      if (valueS != null)
        sbuf.append(valueS);
      else if (isDouble)
        for (int i = 0; i < count; i++)
          sbuf.append(dvalue[i] + " ");
      else
        for (int i = 0; i < count; i++)
          sbuf.append(value[i] + " ");
    }

    return sbuf.toString();
  }

  /** Type-safe enumeration of GeoKeys */
  static class Tag implements Comparable {
    private static Map<Integer, Tag> map = new HashMap<>();

    public static final Tag GTModelTypeGeoKey = new Tag("GTModelTypeGeoKey", 1024);
    public static final Tag GTRasterTypeGeoKey = new Tag("GTRasterTypeGeoKey", 1025);
    public static final Tag GTCitationGeoKey = new Tag("GTCitationGeoKey", 1026);

    public static final Tag GeographicTypeGeoKey = new Tag("GeographicTypeGeoKey", 2048);
    public static final Tag GeogCitationGeoKey = new Tag("GeogCitationGeoKey", 2049);
    public static final Tag GeogGeodeticDatumGeoKey = new Tag("GeogGeodeticDatumGeoKey", 2050);
    public static final Tag GeogPrimeMeridianGeoKey = new Tag("GeogPrimeMeridianGeoKey", 2051);
    public static final Tag GeogLinearUnitsGeoKey = new Tag("GeogLinearUnitsGeoKey", 2052);
    public static final Tag GeogAngularUnitsGeoKey = new Tag("GeogAngularUnitsGeoKey", 2054);
    public static final Tag GeogAngularUnitsSizeGeoKey = new Tag("GeogAngularUnitsSizeGeoKey", 2055);
    public static final Tag GeogSemiMajorAxisGeoKey = new Tag("GeogSemiMajorAxisGeoKey", 2056);
    public static final Tag GeogSemiMinorAxisGeoKey = new Tag("GeogSemiMinorAxisGeoKey", 2057);
    public static final Tag GeogInvFlatteningGeoKey = new Tag("GeogInvFlatteningGeoKey", 2058);
    public static final Tag GeogAzimuthUnitsGeoKey = new Tag("GeogAzimuthUnitsGeoKey", 2060);

    public static final Tag ProjectedCSTypeGeoKey = new Tag("ProjectedCSTypeGeoKey,", 3072);
    public static final Tag PCSCitationGeoKey = new Tag("PCSCitationGeoKey,", 3073);
    public static final Tag ProjectionGeoKey = new Tag("ProjectionGeoKey,", 3074);
    public static final Tag ProjCoordTransGeoKey = new Tag("ProjCoordTransGeoKey", 3075);
    public static final Tag ProjLinearUnitsGeoKey = new Tag("ProjLinearUnitsGeoKey", 3076);
    public static final Tag ProjLinearUnitsSizeGeoKey = new Tag("ProjLinearUnitsSizeGeoKey", 3077);
    public static final Tag ProjStdParallel1GeoKey = new Tag("ProjStdParallel1GeoKey", 3078);
    public static final Tag ProjStdParallel2GeoKey = new Tag("ProjStdParallel2GeoKey", 3079);
    public static final Tag ProjNatOriginLongGeoKey = new Tag("ProjNatOriginLongGeoKey", 3080);
    public static final Tag ProjNatOriginLatGeoKey = new Tag("ProjNatOriginLatGeoKey", 3081);
    public static final Tag ProjFalseEastingGeoKey = new Tag("ProjFalseEastingGeoKey", 3082);
    public static final Tag ProjFalseNorthingGeoKey = new Tag("ProjFalseNorthingGeoKey", 3083);
    public static final Tag ProjFalseOriginLongGeoKey = new Tag("ProjFalseOriginLongGeoKey", 3084);
    public static final Tag ProjFalseOriginLatGeoKey = new Tag("ProjFalseOriginLatGeoKey", 3085);
    public static final Tag ProjFalseOriginEastingGeoKey = new Tag("ProjFalseOriginEastingGeoKey", 3086);
    public static final Tag ProjFalseOriginNorthingGeoKey = new Tag("ProjFalseOriginNorthingGeoKey", 3087);
    public static final Tag ProjCenterLongGeoKey = new Tag("ProjCenterLongGeoKey", 3088);
    public static final Tag ProjCenterLatGeoKey = new Tag("ProjCenterLatGeoKey", 3089);
    public static final Tag ProjCenterEastingGeoKey = new Tag("ProjCenterEastingGeoKey", 3090);
    public static final Tag ProjCenterNorthingGeoKey = new Tag("ProjCenterNorthingGeoKey", 3091);
    public static final Tag ProjScaleAtNatOriginGeoKey = new Tag("ProjScaleAtNatOriginGeoKey", 3092);
    public static final Tag ProjScaleAtCenterGeoKey = new Tag("ProjScaleAtCenterGeoKey", 3093);
    public static final Tag ProjAzimuthAngleGeoKey = new Tag("ProjAzimuthAngleGeoKey", 3094);
    public static final Tag ProjStraightVertPoleLongGeoKey = new Tag("ProjStraightVertPoleLongGeoKey", 3095);

    public static final Tag VerticalCSTypeGeoKey = new Tag("VerticalCSTypeGeoKey", 4096);
    public static final Tag VerticalCitationGeoKey = new Tag("VerticalCitationGeoKey", 4097);
    public static final Tag VerticalDatumGeoKey = new Tag("VerticalDatumGeoKey", 4098);
    public static final Tag VerticalUnitsGeoKey = new Tag("VerticalUnitsGeoKey", 4099);

    public static final Tag GeoKey_ProjCoordTrans = new Tag("GeoKey_ProjCoordTrans", 3075);
    public static final Tag GeoKey_ProjStdParallel1 = new Tag("GeoKey_ProjStdParallel1", 3078);
    public static final Tag GeoKey_ProjStdParallel2 = new Tag("GeoKey_ProjStdParallel2", 3079);
    public static final Tag GeoKey_ProjNatOriginLong = new Tag("GeoKey_ProjNatOriginLong", 3080);
    public static final Tag GeoKey_ProjNatOriginLat = new Tag("GeoKey_ProjNatOriginLat", 3081);
    public static final Tag GeoKey_ProjCenterLong = new Tag("GeoKey_ProjCenterLong", 3088);
    public static final Tag GeoKey_ProjFalseEasting = new Tag("GeoKey_ProjFalseEasting", 3082);
    public static final Tag GeoKey_ProjFalseNorthing = new Tag("GeoKey_ProjFalseNorthing", 3083);
    public static final Tag GeoKey_ProjFalseOriginLong = new Tag("GeoKey_ProjFalseOriginLong", 3084);
    public static final Tag GeoKey_ProjFalseOriginLat = new Tag("GeoKey_ProjFalseOriginLat", 3085);

    static Tag get(int code) {
      return map.get(code);
    }

    static Tag getOrMake(int code) {
      Tag tag = Tag.get(code);
      return (tag != null) ? tag : new Tag(code);
    }

    String name;
    int code;

    private Tag(String name, int code) {
      this.name = name;
      this.code = code;
      map.put(code, this);
    }

    Tag(int code) {
      this.code = code;
      // map.put( new Integer(code), this);
    }

    public int code() {
      return code;
    }

    public String toString() {
      return name == null ? code + " " : code + " (" + name + ")";
    }

    public int compareTo(Object o) {
      if (!(o instanceof Tag))
        return 0;
      Tag to = (Tag) o;
      return code - to.code;
    }
  }

  /** Type-safe enumeration of GeoKey values */
  static class TagValue implements Comparable {
    private static Map<String, TagValue> map = new HashMap<>();

    public static final TagValue ModelType_Projected = new TagValue(Tag.GTModelTypeGeoKey, "Projected", 1);
    public static final TagValue ModelType_Geographic = new TagValue(Tag.GTModelTypeGeoKey, "Geographic", 2);
    public static final TagValue ModelType_Geocentric = new TagValue(Tag.GTModelTypeGeoKey, "Geocentric", 3);

    public static final TagValue RasterType_Area = new TagValue(Tag.GTRasterTypeGeoKey, "Area", 1);
    public static final TagValue RasterType_Point = new TagValue(Tag.GTRasterTypeGeoKey, "Point", 2);

    // "ellipsoidal only", so you should also specify the GeogPrimeMeridian if not default = Greenwich
    public static final TagValue GeographicType_Clarke1866 = new TagValue(Tag.GeographicTypeGeoKey, "Clarke1866", 4008);
    public static final TagValue GeographicType_GRS_80 = new TagValue(Tag.GeographicTypeGeoKey, "GRS_80", 4019);
    public static final TagValue GeographicType_Sphere = new TagValue(Tag.GeographicTypeGeoKey, "Sphere", 4035);

    // these include the prime meridian, so are preferred
    public static final TagValue GeographicType_NAD83 = new TagValue(Tag.GeographicTypeGeoKey, "GCS_NAD83", 4269);
    public static final TagValue GeographicType_WGS_84 = new TagValue(Tag.GeographicTypeGeoKey, "WGS_84", 4326);
    public static final TagValue GeographicType_GCS_NAD27 = new TagValue(Tag.GeographicTypeGeoKey, "GCS_NAD27", 4267);

    //
    public static final TagValue GeogGeodeticDatum_WGS_84 = new TagValue(Tag.GeogGeodeticDatumGeoKey, "WGS_84", 4326);
    public static final TagValue GeogPrimeMeridian_GREENWICH =
        new TagValue(Tag.GeogPrimeMeridianGeoKey, "Greenwich", 8901);

    // projections
    public static final TagValue ProjectedCSType_UserDefined =
        new TagValue(Tag.ProjectedCSTypeGeoKey, "UserDefined", 32767);
    public static final TagValue ProjCoordTrans_LambertConfConic_2SP =
        new TagValue(Tag.ProjCoordTransGeoKey, "LambertConfConic_2SP", 8);
    public static final TagValue ProjCoordTrans_LambertConfConic_1SP =
        new TagValue(Tag.ProjCoordTransGeoKey, "LambertConfConic_1SP", 9);
    public static final TagValue ProjCoordTrans_Stereographic =
        new TagValue(Tag.ProjCoordTransGeoKey, "Stereographic", 14);
    public static final TagValue ProjCoordTrans_TransverseMercator =
        new TagValue(Tag.ProjCoordTransGeoKey, "TransverseMercator", 1);
    public static final TagValue ProjCoordTrans_AlbersConicalEqualArea =
        new TagValue(Tag.ProjCoordTransGeoKey, "AlbersConicalEqualArea", 11);
    public static final TagValue ProjCoordTrans_AlbersEqualAreaEllipse =
        new TagValue(Tag.ProjCoordTransGeoKey, "AlbersEqualAreaEllipse", 11);
    public static final TagValue ProjCoordTrans_Mercator = new TagValue(Tag.ProjCoordTransGeoKey, "Mercator", 7);
    // units
    public static final TagValue ProjLinearUnits_METER = new TagValue(Tag.ProjLinearUnitsGeoKey, "Meter", 9001);
    public static final TagValue GeogAngularUnits_DEGREE = new TagValue(Tag.GeogAngularUnitsGeoKey, "Degree", 9102);
    // add
    public static final TagValue GeogGeodeticDatum6267 =
        new TagValue(Tag.GeogGeodeticDatumGeoKey, "North_American_1927", 6267);

    static TagValue get(Tag tag, int code) {
      if (tag == null)
        return null;
      return map.get(tag.name + code);
    }

    private Tag tag;
    private String name;
    private int value;

    private TagValue(Tag tag, String name, int value) {
      this.tag = tag;
      this.name = name;
      this.value = value;
      map.put(tag.name + value, this);
    }


    public Tag tag() {
      return tag;
    }

    public int value() {
      return value;
    }

    public String toString() {
      return value + " (" + name + ")";
    }

    public int compareTo(Object o) {
      if (!(o instanceof TagValue))
        return 0;
      int ret = tag.compareTo(o);
      if (ret != 0)
        return ret;
      return value - ((TagValue) o).value;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    GeoKey geoKey = (GeoKey) o;

    if (isDouble != geoKey.isDouble)
      return false;
    if (isString != geoKey.isString)
      return false;
    if (count != geoKey.count)
      return false;
    if (id != geoKey.id)
      return false;
    if (!Arrays.equals(value, geoKey.value))
      return false;
    if (!Arrays.equals(dvalue, geoKey.dvalue))
      return false;
    if (!Objects.equals(valueS, geoKey.valueS))
      return false;
    if (!Objects.equals(tag, geoKey.tag))
      return false;
    return !(!Objects.equals(tagValue, geoKey.tagValue));

  }

  @Override
  public int hashCode() {
    int result = (isDouble ? 1 : 0);
    result = 31 * result + (isString ? 1 : 0);
    result = 31 * result + count;
    result = 31 * result + (value != null ? Arrays.hashCode(value) : 0);
    result = 31 * result + (dvalue != null ? Arrays.hashCode(dvalue) : 0);
    result = 31 * result + (valueS != null ? valueS.hashCode() : 0);
    result = 31 * result + (tag != null ? tag.hashCode() : 0);
    result = 31 * result + (tagValue != null ? tagValue.hashCode() : 0);
    result = 31 * result + id;
    return result;
  }
}

