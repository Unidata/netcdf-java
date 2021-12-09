/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.Section;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.*;
import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;

/** Test nc2 read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadBasic {

  @org.junit.Test
  /* attribute array of String */
  public void testReadH5attributeArrayString() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/astrarr.h5")) {
      Variable dset = ncfile.findVariable("dset");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.INT);

      Dimension d = dset.getDimension(0);
      assertThat(d.getLength()).isEqualTo(4);
      d = dset.getDimension(1);
      assertThat(d.getLength()).isEqualTo(6);

      Attribute att = dset.findAttribute("string-att");
      assertThat(att).isNotNull();
      assertThat(att.isArray()).isTrue();
      assertThat(att.getLength()).isEqualTo(4);
      assertThat(att.isString()).isTrue();
      assertThat(att.getStringValue()).isEqualTo(("test "));
      assertThat(att.getStringValue(0)).isEqualTo(("test "));
      assertThat(att.getStringValue(1)).isEqualTo(("left "));
      assertThat(att.getStringValue(2)).isEqualTo(("call "));
      assertThat(att.getStringValue(3)).isEqualTo(("mesh "));

      // read entire array
      Array<Integer> A = (Array<Integer>) dset.readArray();
      assertThat(A.getRank()).isEqualTo(2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();
      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assertThat(A.get(ima.set(i, j))).isEqualTo(0);
        }
      }

    }
  }

  @org.junit.Test
  public void testReadH5attributeString() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/attstr.h5")) {
      Group g = ncfile.getRootGroup().findGroupLocal("MyGroup");
      assertThat(g).isNotNull();
      Attribute att = g.findAttribute("data_contents");
      assertThat(att).isNotNull();
      assertThat(!att.isArray()).isTrue();
      assertThat(att.isString()).isTrue();
      assertThat(att.getStringValue()).isEqualTo(("important_data"));
    }
  }

  @org.junit.Test
  public void testReadH5boolean() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/bool.h5")) {

      Variable dset = ncfile.findVariable("dset");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.INT);

      Dimension d = dset.getDimension(0);
      assertThat(d.getLength()).isEqualTo(4);
      d = dset.getDimension(1);
      assertThat(d.getLength()).isEqualTo(6);

      // read entire array
      Array<Integer> A = (Array<Integer>) dset.readArray();
      assertThat(A.getRank()).isEqualTo(2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assertThat(A.get(ima.set(i, j))).isEqualTo((j < 3 ? 1 : 0));
        }
      }

    }
  }

  @org.junit.Test
  public void testReadH5StringFixed() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/dstr.h5")) {

      Variable v = ncfile.findVariable("Char_Data");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.CHAR);

      Dimension d = v.getDimension(0);
      assertThat(d.getLength()).isEqualTo(16);

      // read entire array
      Array<Byte> A = (Array<Byte>) v.readArray();
      assertThat(A.getRank()).isEqualTo(1);

      String s = Arrays.makeStringFromChar(A);
      assertThat(s).isEqualTo(("This is a test."));
    }
  }

  @org.junit.Test
  public void testReadH5StringArray() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/dstrarr.h5")) {

      Variable v = ncfile.findVariable("strdata");
      assertThat(v).isNotNull();
      assertThat(v.getRank()).isEqualTo(3);
      assertThat(v.getArrayType()).isEqualTo(ArrayType.CHAR);

      int[] shape = v.getShape();
      assertThat(shape[0]).isEqualTo(2);
      assertThat(shape[1]).isEqualTo(2);
      assertThat(shape[2]).isEqualTo(5);

      // read entire array
      Array<Byte> A = (Array<Byte>) v.readArray();

      assertThat(A.getRank()).isEqualTo(3);
      assertThat(A.getArrayType()).isEqualTo(ArrayType.CHAR);

      Array<String> ss = Arrays.makeStringsFromChar(A);
      Iterator<String> siter = ss.iterator();
      assertThat(siter.next()).isEqualTo(("test "));
      assertThat(siter.next()).isEqualTo(("left "));
      assertThat(siter.next()).isEqualTo(("call "));
      assertThat(siter.next()).isEqualTo(("mesh "));
    }
  }

  @org.junit.Test
  public void testReadH5ShortArray() throws Exception {
    try (NetcdfFile ncfile = TestH5.openH5("support/short.h5")) {

      Variable dset = ncfile.findVariable("IntArray");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.SHORT);

      Dimension d = dset.getDimension(0);
      assertThat(d.getLength()).isEqualTo(5);
      d = dset.getDimension(1);
      assertThat(d.getLength()).isEqualTo(6);

      // read entire array
      Array<Short> As = (Array<Short>) dset.readArray();
      assertThat(As.getRank()).isEqualTo(2);

      int i, j;
      Index ima = As.getIndex();
      int[] shape = As.getShape();
      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assertThat(As.get(ima.set(i, j))).isEqualTo(i + j);
        }
      }

      // read part of array
      dset.setCaching(false); // turn off caching to test read subset
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = dset.getShape()[1];
      As = (Array<Short>) dset.readArray(new Section(origin2, shape2));

      assertThat(As.getRank()).isEqualTo(2);
      for (j = 0; j < shape2[1]; j++) {
        assertThat(As.get(ima.set(0, j))).isEqualTo(j);
      }

      // rank reduction
      Array<Short> Areduce = Arrays.reduce(As);
      Index ima2 = Areduce.getIndex();
      assertThat(Areduce.getRank()).isEqualTo(1);

      for (j = 0; j < shape2[1]; j++) {
        assertThat(Areduce.get(ima2.set(j))).isEqualTo(j);
      }

    }
  }

}
