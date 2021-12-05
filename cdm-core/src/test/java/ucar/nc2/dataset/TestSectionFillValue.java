/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.Lists;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.IndexFn;
import ucar.array.Range;
import java.util.ArrayList;
import java.util.List;

import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.internal.dataset.EnhanceScaleMissingUnsigned;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.unidata.util.test.TestDir;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test fill values when reading sections of a Variable.
 * from (WUB-664639) (Didier Earith)
 */
public class TestSectionFillValue {

  @Test
  public void testExplicitFillValue() throws Exception {
    String filename = TestDir.cdmLocalTestDataDir + "standardVar.nc";
    try (NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename)) {
      VariableDS v = (VariableDS) ncfile.findVariable("t3");
      assertThat(v).isNotNull();
      EnhanceScaleMissingUnsigned proxy = v.scaleMissingUnsignedProxy();
      assertThat(proxy.hasFillValue()).isTrue();
      assertThat(v.findAttribute("_FillValue")).isNotNull();

      int rank = v.getRank();
      List<Range> ranges = new ArrayList<>();
      ranges.add(null);
      for (int i = 1; i < rank; i++) {
        ranges.add(new Range(0, 1));
      }

      VariableDS v_section = (VariableDS) v.section(new Section(ranges));
      assertThat(v_section.findAttribute("_FillValue")).isNotNull();
      System.out.println(v_section.findAttribute("_FillValue"));
      assertThat(v_section.scaleMissingUnsignedProxy().hasFillValue()).isTrue();
    }
  }

  @Test
  public void testImplicitFillValue() throws Exception {
    String filename = TestDir.cdmLocalTestDataDir + "testWriteFill.nc";
    List<String> varWithFill = Lists.newArrayList("temperature", "rtemperature");
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        NetcdfDataset ncd = NetcdfDatasets.openDataset(filename)) {

      for (Variable v : ncfile.getAllVariables()) {
        if (!v.getArrayType().isNumeric())
          continue;
        System.out.printf("testImplicitFillValue for %s type=%s%n", v.getShortName(), v.getArrayType());

        VariableDS ve = (VariableDS) ncd.findVariable(v.getFullName());
        assertThat(ve).isNotNull();
        if (varWithFill.contains(v.getShortName())) {
          assertThat(v.findAttribute("_FillValue")).isNotNull();
          assertThat(ve.scaleMissingUnsignedProxy().hasFillValue()).isTrue();
          Number fillValue = v.findAttribute("_FillValue").getNumericValue();

          Array<Number> data = (Array<Number>) v.readArray();
          Array<Number> dataE = (Array<Number>) ve.readArray();
          IndexFn idxf = IndexFn.builder(data.getShape()).build();

          for (int idx = 0; idx < data.getSize(); idx++) {
            double vald = data.get(idxf.odometer(idx)).doubleValue();
            double valde = dataE.get(idxf.odometer(idx)).doubleValue();
            if (ve.scaleMissingUnsignedProxy().isFillValue(vald)) {
              if (v.getArrayType().isFloatingPoint()) {
                assertThat(valde).isNaN();
              } else {
                assertThat(vald).isEqualTo(fillValue);
              }
            }
          }
        } else {
          assertThat(v.findAttribute("_FillValue")).isNull();
          assertThat(ve.scaleMissingUnsignedProxy().hasFillValue()).isTrue();
          Number fillValue = NetcdfFormatUtils.getFillValueDefault(v.getArrayType());
          assertThat(fillValue).isNotNull();

          Array<Number> data = (Array<Number>) v.readArray();
          Array<Number> dataE = (Array<Number>) ve.readArray();
          IndexFn idxf = IndexFn.builder(data.getShape()).build();

          for (int idx = 0; idx < data.getSize(); idx++) {
            double vald = data.get(idxf.odometer(idx)).doubleValue();
            double valde = dataE.get(idxf.odometer(idx)).doubleValue();

            if (fillValue.equals(vald)) {
              assertThat(ve.scaleMissingUnsignedProxy().isFillValue(vald)).isTrue();
            }

            if (ve.scaleMissingUnsignedProxy().isFillValue(vald)) {
              if (v.getArrayType().isFloatingPoint()) {
                assertThat(valde).isNaN();
              } else {
                assertThat(valde).isEqualTo(fillValue.doubleValue());
              }
            }
          }
        }


      }
    }
  }


}
