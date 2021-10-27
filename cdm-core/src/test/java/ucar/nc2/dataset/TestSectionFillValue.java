/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.Lists;
import org.junit.Assert;
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
      Assert.assertNotNull("t3", v);
      EnhanceScaleMissingUnsigned proxy = v.scaleMissingUnsignedProxy();
      Assert.assertTrue(proxy.hasFillValue());
      Assert.assertNotNull(v.findAttribute("_FillValue"));

      int rank = v.getRank();
      List<Range> ranges = new ArrayList<>();
      ranges.add(null);
      for (int i = 1; i < rank; i++) {
        ranges.add(new Range(0, 1));
      }

      VariableDS v_section = (VariableDS) v.section(new Section(ranges));
      Assert.assertNotNull(v_section.findAttribute("_FillValue"));
      System.out.println(v_section.findAttribute("_FillValue"));
      Assert.assertTrue(v_section.scaleMissingUnsignedProxy().hasFillValue());
    }
  }

  @Test
  public void testImplicitFillValue() throws Exception {
    String filename = TestDir.cdmLocalTestDataDir + "testWriteFill.nc";
    List<String> varWithFill = Lists.newArrayList("temperature", "rtemperature");
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        NetcdfDataset ncd = NetcdfDatasets.openDataset(filename)) {

      for (Variable v : ncfile.getVariables()) {
        if (!v.getArrayType().isNumeric())
          continue;
        System.out.printf("testImplicitFillValue for %s type=%s%n", v.getShortName(), v.getArrayType());

        VariableDS ve = (VariableDS) ncd.findVariable(v.getFullName());
        if (varWithFill.contains(v.getShortName())) {
          Assert.assertNotNull(v.findAttribute("_FillValue"));
          Assert.assertTrue(ve.scaleMissingUnsignedProxy().hasFillValue());
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
          Assert.assertNull(v.findAttribute("_FillValue"));
          Assert.assertTrue(ve.scaleMissingUnsignedProxy().hasFillValue());
          Number fillValue = NetcdfFormatUtils.getFillValueDefault(v.getArrayType());
          Assert.assertNotNull(v.getArrayType().toString(), fillValue);

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
