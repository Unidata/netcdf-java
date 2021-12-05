/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import java.util.HashSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.IsMissingEvaluator;
import ucar.array.MinMax;
import ucar.array.Section;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.dataset.EnhanceScaleMissingUnsigned;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.util.Misc;
import ucar.nc2.write.NcdumpArray;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class TestScaleOffsetMissingUnsigned {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testWrite() throws Exception {
    String filename = tempFolder.newFile().getAbsolutePath();
    ScaleOffset so;

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);

    // define dimensions
    Dimension latDim = writerb.addDimension("lat", 200);
    Dimension lonDim = writerb.addDimension("lon", 300);
    int n = lonDim.getLength();

    // create an array
    double[] parray = new double[latDim.getLength() * lonDim.getLength()];
    int count = 0;
    for (int i = 0; i < latDim.getLength(); i++) {
      for (int j = 0; j < lonDim.getLength(); j++) {
        parray[count++] = (i * n + j) + 30.0;
      }
    }
    Array<Number> unpacked =
        Arrays.factory(ArrayType.DOUBLE, new int[] {latDim.getLength(), lonDim.getLength()}, parray);

    double missingValue = -9999;
    int nbits = 16;

    // convert to packed form
    so = calcScaleOffsetSkipMissingData(unpacked, missingValue, nbits);
    writerb.addVariable("unpacked", ArrayType.DOUBLE, "lat lon");
    writerb.addVariable("packed", ArrayType.SHORT, "lat lon")
        .addAttribute(new Attribute(CDM.MISSING_VALUE, (short) -9999))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, so.scale)).addAttribute(new Attribute(CDM.ADD_OFFSET, so.offset));

    // create and write to the file
    Array<Number> packed = convert2packedShort(unpacked, missingValue, nbits);
    try (NetcdfFormatWriter writer = writerb.build()) {
      writer.write(writer.findVariable("unpacked"), unpacked.getIndex(), unpacked);
      writer.write(writer.findVariable("packed"), packed.getIndex(), packed);
    }

    // read the packed form, compare to original
    Array<Number> readPacked;
    try (NetcdfFile ncfileRead = NetcdfFiles.open(filename)) {
      Variable v = ncfileRead.findVariable("packed");
      assertThat(v).isNotNull();
      readPacked = (Array<Number>) v.readArray();
      CompareNetcdf2.compareData("packed", packed, readPacked);
    }

    Array<Number> readEnhanced;

    // read the packed form, enhance using scale/offset (but not missing), compare to original
    Set<Enhance> enhance = new HashSet<>(NetcdfDataset.getDefaultEnhanceMode());
    enhance.remove(Enhance.ConvertMissing);
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(filename, enhance, null)) {
      VariableDS vs = (VariableDS) ncd.findVariable("packed");
      readEnhanced = (Array<Number>) vs.readArray();

      nearlyEquals(packed, unpacked, readEnhanced, 1.0 / so.scale);
    }

    Array<Number> convertPacked = convert2Unpacked(readPacked, so);
    nearlyEquals(packed, convertPacked, readEnhanced, 1.0 / so.scale);

    doSubset(filename);
  }

  private void nearlyEquals(Array<Number> packed, Array<Number> data1, Array<Number> data2, double close) {
    Iterator<Number> iterp = packed.iterator();
    Iterator<Number> iter1 = data1.iterator();
    Iterator<Number> iter2 = data2.iterator();

    while (iterp.hasNext()) {
      double p = iterp.next().doubleValue();
      double v1 = iter1.next().doubleValue();
      double v2 = iter2.next().doubleValue();
      double diff = Math.abs(v1 - v2);
      assertThat(diff).isLessThan(close);
    }
  }

  // check section of scale/offset only applies it once
  private void doSubset(String filename) throws IOException, InvalidRangeException {
    // read the packed form, enhance using scale/offset, compare to original
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(filename)) {
      Variable vs = ncd.findVariable("packed");
      assertThat(vs).isNotNull();

      Section s = Section.builder().appendRange(1, 1).appendRange(1, 1).build();
      Array<?> readEnhanced = vs.readArray(s);
      logger.debug(NcdumpArray.printArray(readEnhanced));

      Variable sec = vs.section(s);
      Array<?> readSection = sec.readArray();
      logger.debug(NcdumpArray.printArray(readSection));
      CompareArrayToArray.compareData(vs.getShortName(), readEnhanced, readSection);
    }
  }

  // Asserts that "scale_factor" is applied to "_FillValue".
  // This test demonstrated the bug in https://github.com/Unidata/thredds/issues/1065.
  @Test
  public void testScaledFillValue() throws Exception {
    File testResource = new File(getClass().getResource("testScaledFillValue.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS fooVar = (VariableDS) ncd.findVariable("foo");
      assertThat(fooVar).isNotNull();
      EnhanceScaleMissingUnsigned proxy = fooVar.scaleMissingUnsignedProxy();

      double expectedFillValue = .99999;
      double actualFillValue = proxy.getFillValue();

      // Scale factor of "1.e-05" has been applied to original "99999".
      assertThat(Misc.nearlyEquals(expectedFillValue, actualFillValue)).isTrue();

      double fooValWithNaNs = (Double) fooVar.readArray().getScalar();

      // "foo" value was converted to NaN because it was equal to _FillValue.
      assertThat(Double.isNaN(fooValWithNaNs)).isTrue();

      // Note that we can't use isFillValue() because we've applied the ConvertMissing enhancement.
      // See the EnhanceScaleMissingUnsignedImpl Javadoc.
      assertThat(fooVar.isMissing(fooValWithNaNs)).isTrue();
    }

    Set<Enhance> enhance = new HashSet<>(NetcdfDataset.getDefaultEnhanceMode());
    enhance.remove(Enhance.ConvertMissing);
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testResource.getAbsolutePath(), enhance, null)) {
      VariableDS fooVar = (VariableDS) ncd.findVariable("foo");
      assertThat(fooVar).isNotNull();
      double fooValWithoutNaNs = (Double) fooVar.readArray().getScalar();
      EnhanceScaleMissingUnsigned proxy = fooVar.scaleMissingUnsignedProxy();

      // "foo" value is equals to fill value. Scale factor has been applied to both.
      double actualFillValue = proxy.getFillValue();
      assertThat(Misc.nearlyEquals(actualFillValue, fooValWithoutNaNs)).isTrue();

      // "foo" value is considered a fill.
      assertThat(proxy.isFillValue(fooValWithoutNaNs)).isTrue();
    }
  }

  // Asserts that EnhanceScaleMissingUnsignedImplImpl compares floating-point values in a "fuzzy" manner.
  // This test demonstrated the bug in https://github.com/Unidata/thredds/issues/1068.
  @Test
  public void testScaleMissingFloatingPointComparisons() throws Exception {
    File testResource = new File(getClass().getResource("testScaleMissingFloatingPointComparisons.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS fooVar = (VariableDS) ncd.findVariable("foo");
      assertThat(fooVar).isNotNull();
      EnhanceScaleMissingUnsigned proxy = fooVar.scaleMissingUnsignedProxy();

      // Values have been multiplied by scale_factor == 0.01f. scale_factor is a float, meaning that we can't compare
      // its products with nearlyEquals() using the default Misc.defaultMaxRelativeDiffDouble.
      assertThat(Misc.nearlyEquals(0, proxy.getValidMin(), Misc.defaultMaxRelativeDiffFloat)).isTrue();
      assertThat(Misc.nearlyEquals(1, proxy.getValidMax(), Misc.defaultMaxRelativeDiffFloat)).isTrue();

      // Argument is a double, which has higher precision that our scaled _FillValue (float).
      // This assertion failed before the bug was fixed.
      assertThat(proxy.isFillValue(-0.01)).isTrue();

      Array<Float> fooVals = (Array<Float>) fooVar.readArray();
      assertThat(4).isEqualTo(fooVals.getSize());

      // foo[0] == -1 (raw); Double.NaN (scaled). It is equal to fill value and outside of valid_range.
      assertThat(fooVals.get(0)).isNaN();
      assertThat(proxy.isFillValue(-0.01)).isTrue();
      assertThat(proxy.isMissingValue(-0.01)).isTrue();
      assertThat(proxy.isInvalidData(-0.01)).isTrue();
      assertThat(proxy.isMissing(-0.01)).isTrue();

      // foo[1] == 0 (raw); 0.00 (scaled). It is within valid_range.
      assertThat(Misc.nearlyEquals(0.00, fooVals.get(1), Misc.defaultMaxRelativeDiffFloat)).isTrue();
      assertThat(proxy.isInvalidData(0.00)).isFalse();
      assertThat(fooVar.isMissing(0.00)).isFalse();

      // foo[2] == 100 (raw); 1.00 (scaled). It is within valid_range.
      assertThat(Misc.nearlyEquals(1.00, fooVals.get(2), Misc.defaultMaxRelativeDiffFloat)).isTrue();
      // These assertions failed before the bug was fixed.
      assertThat(proxy.isInvalidData(1.00)).isFalse();
      assertThat(fooVar.isMissing(1.00)).isFalse();

      // foo[3] == 101 (raw); Double.NaN, (scaled). It is outside of valid_range.
      assertThat(Misc.nearlyEquals(Double.NaN, fooVals.get(3), Misc.defaultMaxRelativeDiffFloat)).isTrue();
      assertThat(proxy.isMissingValue(1.01)).isTrue();
      assertThat(proxy.isInvalidData(1.01)).isTrue();
      assertThat(fooVar.isMissing(1.01)).isTrue();
    }
  }

  // Asserts that EnhanceScaleMissingUnsignedImplImpl compares floating-point values in a "fuzzy" manner.
  // This test demonstrated the bug in https://github.com/Unidata/thredds/issues/1068.
  @Test
  public void testScaleMissingFloatingPointComparisonsNoConvertMissing() throws Exception {
    File testResource = new File(getClass().getResource("testScaleMissingFloatingPointComparisons.ncml").toURI());

    Set<Enhance> enhance = new HashSet<>(NetcdfDataset.getDefaultEnhanceMode());
    enhance.remove(Enhance.ConvertMissing);
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testResource.getAbsolutePath(), enhance, null)) {
      VariableDS fooVar = (VariableDS) ncd.findVariable("foo");
      assertThat(fooVar).isNotNull();
      EnhanceScaleMissingUnsigned proxy = fooVar.scaleMissingUnsignedProxy();

      // Values have been multiplied by scale_factor == 0.01f. scale_factor is a float, meaning that we can't compare
      // its products with nearlyEquals() using the default Misc.defaultMaxRelativeDiffDouble.
      assertThat(Misc.nearlyEquals(0, proxy.getValidMin(), Misc.defaultMaxRelativeDiffFloat)).isTrue();
      assertThat(Misc.nearlyEquals(1, proxy.getValidMax(), Misc.defaultMaxRelativeDiffFloat)).isTrue();

      // Argument is a double, which has higher precision that our scaled _FillValue (float).
      // This assertion failed before the bug was fixed.
      assertThat(proxy.isFillValue(-0.01)).isTrue();

      Array<Float> fooVals = (Array<Float>) fooVar.readArray();
      assertThat(4).isEqualTo(fooVals.getSize());

      // foo[0] == -1 (raw); -0.01 (scaled). It is equal to fill value and outside of valid_range.
      assertThat(fooVals.get(0)).isWithin(Misc.defaultMaxRelativeDiffFloat).of(-0.01f);
      assertThat(proxy.isFillValue(-0.01)).isTrue();
      assertThat(proxy.isMissingValue(-0.01)).isTrue();
      assertThat(proxy.isInvalidData(-0.01)).isTrue();
      assertThat(proxy.isMissing(-0.01)).isTrue();

      // foo[1] == 0 (raw); 0.00 (scaled). It is within valid_range.
      assertThat(Misc.nearlyEquals(0.00, fooVals.get(1), Misc.defaultMaxRelativeDiffFloat)).isTrue();
      assertThat(proxy.isInvalidData(0.00)).isFalse();
      assertThat(fooVar.isMissing(0.00)).isFalse();

      // foo[2] == 100 (raw); 1.00 (scaled). It is within valid_range.
      assertThat(Misc.nearlyEquals(1.00, fooVals.get(2), Misc.defaultMaxRelativeDiffFloat)).isTrue();
      // These assertions failed before the bug was fixed.
      assertThat(proxy.isInvalidData(1.00)).isFalse();
      assertThat(fooVar.isMissing(1.00)).isFalse();

      // foo[3] == 101 (raw); 1.01 (scaled). It is outside of valid_range.
      assertThat(Misc.nearlyEquals(1.01, fooVals.get(3), Misc.defaultMaxRelativeDiffFloat)).isTrue();
      assertThat(proxy.isMissingValue(1.01)).isTrue();
      assertThat(proxy.isInvalidData(1.01)).isTrue();
      assertThat(fooVar.isMissing(1.01)).isTrue();
    }
  }

  @Test
  public void testMissingUnsigned() throws Exception {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("missingUnsigned");
      assertThat(var).isNotNull();
      EnhanceScaleMissingUnsigned proxy = var.scaleMissingUnsignedProxy();

      // Packed valid_min == -106. Interpreting bit pattern as unsigned, we get 150.
      assertThat(Misc.nearlyEquals(150, proxy.getValidMin())).isTrue();

      // Packed valid_min == -6. Interpreting bit pattern as unsigned, we get 250.
      assertThat(Misc.nearlyEquals(250, proxy.getValidMax())).isTrue();

      // Packed _FillValue and missing_value are -1. Interpreting bit pattern as unsigned, we get 255.
      assertThat(Misc.nearlyEquals(255, proxy.getFillValue())).isTrue();
      assertThat(Misc.nearlyEquals(255, proxy.getMissingValues()[0])).isTrue();

      // "missingUnsigned" was originally UBYTE, but was widened to accommodate unsigned conversion.
      assertThat(ArrayType.USHORT).isEqualTo(var.getArrayType());

      // Packed values are: -107, -106, -6, -5, -1, 80. Interpreting them as unsigned yields:
      Array<Number> expecteds =
          Arrays.factory(ArrayType.SHORT, new int[] {6}, new short[] {149, 150, 250, 251, 255, 80});
      Array<Number> actuals = (Array<Number>) var.readArray();
      assertThat(Arrays.equalNumbers(expecteds, actuals)).isTrue();
    }
  }

  @Test
  public void testScaleOffsetMissingUnsigned() throws Exception {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("scaleOffsetMissingUnsigned");
      assertThat(var).isNotNull();
      EnhanceScaleMissingUnsigned proxy = var.scaleMissingUnsignedProxy();

      // These vals are the same as ones from "missingUnsigned", but with a scale_factor of 100 and offset of 1 applied.
      assertThat(15001.0).isEqualTo(proxy.getValidMin());
      assertThat(25001.0).isEqualTo(proxy.getValidMax());

      assertThat(25501.0).isEqualTo(proxy.getFillValue());
      assertThat(25501.0).isEqualTo(proxy.getMissingValues()[0]);

      // "scaleOffsetMissingUnsigned" was originally UBYTE, but scale_factor (SHORT) and add_offset (INT) caused it to
      // be UINT due to:
      /*
       * <li>The data type of the variable will be set to the
       * {@link EnhanceScaleMissingUnsignedImplImpl#largestOf largest of}:
       * <ul>
       * <li>the original data type</li>
       * <li>the unsigned conversion type, if applicable</li>
       * <li>the {@code scale_factor} attribute type</li>
       * <li>the {@code add_offset} attribute type</li>
       * </ul>
       * The signedness of the variable's data type will be preserved. For example, if the variable was originally
       * unsigned, then {@link #getScaledOffsetMissingType()} will be unsigned as well.
       * </li>
       */
      assertThat(ArrayType.UINT).isEqualTo(var.getArrayType());

      // These vals are the same as ones from "missingUnsigned", but with a scale_factor of 100 and offset of 1 applied.
      Array<Number> expecteds =
          Arrays.factory(ArrayType.INT, new int[] {6}, new int[] {14901, 15001, 25001, 25101, 25501, 8001});
      Array<Number> actuals = (Array<Number>) var.readArray();
      assertThat(Arrays.equalNumbers(expecteds, actuals)).isTrue();
    }
  }

  // This test demonstrated the bug in https://github.com/Unidata/netcdf-java/issues/572, but for unsigned variables.
  @Test
  public void testNegativeScaleOffsetValidRangeUnsigned() throws Exception {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());
    float fpTol = Misc.defaultMaxRelativeDiffFloat;

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("scaleOffsetMissingUnsignedValidRange");
      assertThat(var).isNotNull();
      EnhanceScaleMissingUnsigned proxy = var.scaleMissingUnsignedProxy();

      assertThat(proxy.getValidMin()).isWithin(fpTol).of(-25001);
      assertThat(proxy.getValidMax()).isWithin(fpTol).of(-15001);

      assertThat(proxy.getFillValue()).isWithin(fpTol).of(-25501);
      assertThat(proxy.getMissingValues()[0]).isWithin(fpTol).of(-25501);

      // Because scale and offset are now float (to preserve negative values), var is float
      assertThat(ArrayType.FLOAT).isEqualTo(var.getArrayType());

      // These vals are the same as ones from "missingUnsigned", but with a scale_factor of -100 and offset of -1
      Array<Float> expecteds = Arrays.factory(ArrayType.FLOAT, new int[] {6},
          new float[] {Float.NaN, -15001, -25001, Float.NaN, Float.NaN, Float.NaN});
      Array<Float> actuals = (Array<Float>) var.readArray();
      assertThat(Arrays.equalFloats(expecteds, actuals)).isTrue();
    }
  }

  @Test
  public void testScaleValidRange() throws Exception {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("scaleValidRange");
      assertThat(var).isNotNull();
      EnhanceScaleMissingUnsigned proxy = var.scaleMissingUnsignedProxy();

      assertThat(Misc.nearlyEquals(9.9f, (float) proxy.getValidMin())).isTrue();
      assertThat(Misc.nearlyEquals(10.1f, (float) proxy.getValidMax())).isTrue();

      assertThat(ArrayType.FLOAT).isEqualTo(var.getArrayType()); // scale_factor is float.

      Array<Float> expecteds =
          Arrays.factory(ArrayType.FLOAT, new int[] {5}, new float[] {Float.NaN, 9.9f, 10.0f, 10.1f, Float.NaN});
      Array<Float> actuals = (Array<Float>) var.readArray();
      assertThat(Arrays.equalFloats(expecteds, actuals)).isTrue();
    }
  }

  @Test
  public void testUnpackedValidRange() throws Exception {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());
    DatasetUrl location = DatasetUrl.findDatasetUrl(testResource.getAbsolutePath());
    Set<Enhance> enhanceMode = EnumSet.of(Enhance.ConvertUnsigned, Enhance.ApplyScaleOffset); // No ConvertMissing!

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(location, enhanceMode, -1, null, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("unpackedValidRange");
      assertThat(var).isNotNull();
      EnhanceScaleMissingUnsigned proxy = var.scaleMissingUnsignedProxy();

      // valid_range will be interpreted as unpacked because of:
      /*
       * If valid_range is the same type as scale_factor (actually the wider of scale_factor and add_offset) and this
       * is wider than the external data, then it will be interpreted as being in the units of the internal (unpacked)
       * data. Otherwise it is in the units of the external (packed) data.</li>
       */
      // As a result, scale_factor will not be applied to it.
      assertThat(Misc.nearlyEquals(9.9f, (float) proxy.getValidMin())).isTrue();
      assertThat(Misc.nearlyEquals(10.1f, (float) proxy.getValidMax())).isTrue();

      assertThat(ArrayType.FLOAT).isEqualTo(var.getArrayType()); // scale_factor is float.

      Array<Float> expecteds =
          Arrays.factory(ArrayType.FLOAT, new int[] {5}, new float[] {9.8f, 9.9f, 10.0f, 10.1f, 10.2f});
      Array<Float> actuals = (Array<Float>) var.readArray();
      assertThat(Arrays.equalFloats(expecteds, actuals)).isTrue();
    }
  }

  @Test
  public void testUnsignedOffsetAttribute() throws Exception {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("unsignedOffsetAttribute");
      assertThat(var).isNotNull();
      EnhanceScaleMissingUnsigned proxy = var.scaleMissingUnsignedProxy();

      assertThat(156.0).isEqualTo(proxy.getOffset());
      assertThat(ArrayType.BYTE).isEqualTo(var.getArrayType()); // No change to data type.

      assertThat((byte) 106).isEqualTo(var.readArray().getScalar()); // -50 + 156 == 106
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Calculate the scale/offset for an array of numbers.
   *
   * <pre>
   * If signed:
   *   then
   *     max value unpacked = 2^(n-1) - 1 packed
   *     min value unpacked = -(2^(n-1) - 1) packed
   *   note that -2^(n-1) is unused, and a good place to map missing values
   *   by solving 2 eq in 2 unknowns, we get:
   *     scale = (max - min) / (2^n - 2)
   *     offset = (max + min) / 2
   * If unsigned then
   *     max value unpacked = 2^n - 1 packed
   *     min value unpacked = 0 packed
   *   and:
   *     scale = (max - min) / (2^n - 1)
   *     offset = min
   *   One could modify this to allow a holder for missing values.
   * </pre>
   *
   * @param a array to convert (not changed)
   * @param missingValue skip these
   * @param nbits map into this many bits
   * @return ScaleOffset, calculated as above.
   */
  public static ScaleOffset calcScaleOffsetSkipMissingData(Array<Number> a, double missingValue, int nbits) {
    MinMax minmax = Arrays.getMinMaxSkipMissingData(a, new IsMissingEvaluator() {
      public boolean hasMissing() {
        return true;
      }

      public boolean isMissing(double val) {
        return val == missingValue;
      }
    });

    if (a.getArrayType().isUnsigned()) {
      long size = (1L << nbits) - 1;
      double offset = minmax.min();
      double scale = (minmax.max() - minmax.min()) / size;
      return new ScaleOffset(scale, offset);

    } else {
      long size = (1L << nbits) - 2;
      double offset = (minmax.max() + minmax.min()) / 2;
      double scale = (minmax.max() - minmax.min()) / size;
      return new ScaleOffset(scale, offset);
    }
  }

  public static Array<Number> convert2packedShort(Array<Number> unpacked, double missingValue, int nbits) {
    ScaleOffset scaleOffset = calcScaleOffsetSkipMissingData(unpacked, missingValue, nbits);
    short[] parray = new short[(int) Arrays.computeSize(unpacked.getShape())];
    int count = 0;
    for (Number val : unpacked) {
      double uv = val.doubleValue();
      parray[count++] = (short) ((uv - scaleOffset.offset) / scaleOffset.scale);
    }
    return Arrays.factory(ArrayType.SHORT, unpacked.getShape(), parray);
  }

  public static Array<Number> convert2Unpacked(Array<Number> packed, ScaleOffset scaleOffset) {
    double[] parray = new double[(int) Arrays.computeSize(packed.getShape())];
    int count = 0;
    for (Number val : packed) {
      parray[count++] = val.doubleValue() * scaleOffset.scale + scaleOffset.offset;
    }
    return Arrays.factory(ArrayType.DOUBLE, packed.getShape(), parray);
  }

  /**
   * Holds a scale and offset.
   */
  public static class ScaleOffset {
    public double scale, offset;

    public ScaleOffset(double scale, double offset) {
      this.scale = scale;
      this.offset = offset;
    }
  }
}
