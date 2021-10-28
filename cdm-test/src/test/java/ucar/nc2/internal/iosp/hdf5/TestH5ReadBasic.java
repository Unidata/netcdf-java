/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;

/** Test nc2 read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadBasic {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @org.junit.Test
  /* attribute array of String */
  public void testReadH5attributeArrayString() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/astrarr.h5")) {

      Variable dset = null;
      assert (null != (dset = ncfile.findVariable("dset")));
      assert (dset.getArrayType() == ArrayType.INT);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 4);
      d = dset.getDimension(1);
      assert (d.getLength() == 6);

      Attribute att = dset.findAttribute("string-att");
      assert (null != att);
      assert (att.isArray());
      assert (att.getLength() == 4);
      assert (att.isString());
      assert (att.getStringValue().equals("test "));
      assert (att.getStringValue(0).equals("test "));
      assert (att.getStringValue(1).equals("left "));
      assert (att.getStringValue(2).equals("call "));
      assert (att.getStringValue(3).equals("mesh "));

      // read entire array
      Array<Integer> A = (Array<Integer>) dset.readArray();
      assert (A.getRank() == 2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assert (A.get(ima.set(i, j)) == 0);
        }
      }

    }
  }

  @org.junit.Test
  public void testReadH5attributeString() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/attstr.h5")) {

      Group g = ncfile.getRootGroup().findGroupLocal("MyGroup");
      assert null != g;
      Attribute att = g.findAttribute("data_contents");
      assert (null != att);
      assert (!att.isArray());
      assert (att.isString());
      assert (att.getStringValue().equals("important_data"));

    }
  }

  @org.junit.Test
  public void testReadH5boolean() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/bool.h5")) {

      Variable dset = null;
      assert (null != (dset = ncfile.findVariable("dset")));
      assert (dset.getArrayType() == ArrayType.INT);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 4);
      d = dset.getDimension(1);
      assert (d.getLength() == 6);

      // read entire array
      Array<Integer> A = (Array<Integer>) dset.readArray();

      assert (A.getRank() == 2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assert (A.get(ima.set(i, j)) == (j < 3 ? 1 : 0));
        }
      }

    }
  }

  @org.junit.Test
  public void testReadH5StringFixed() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/dstr.h5")) {

      Variable v = null;
      assert (null != (v = ncfile.findVariable("Char_Data")));
      assert (v.getArrayType() == ArrayType.CHAR);

      Dimension d = v.getDimension(0);
      assert (d.getLength() == 16);

      // read entire array
      Array<Byte> A = (Array<Byte>) v.readArray();

      assert (A.getRank() == 1);

      String s = Arrays.makeStringFromChar(A);
      assert (s.equals("This is a test."));

    }
  }

  @org.junit.Test
  public void testReadH5StringArray() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/dstrarr.h5")) {

      Variable v = null;
      assert (null != (v = ncfile.findVariable("strdata")));
      assert (v.getRank() == 3);
      assert (v.getArrayType() == ArrayType.CHAR);

      int[] shape = v.getShape();
      assert (shape[0] == 2);
      assert (shape[1] == 2);
      assert (shape[2] == 5);

      // read entire array
      Array<Byte> A = (Array<Byte>) v.readArray();

      assert (A.getRank() == 3);
      assert (A.getArrayType() == ArrayType.CHAR);

      Array<String> ss = Arrays.makeStringsFromChar(A);
      Iterator<String> siter = ss.iterator();
      assert (siter.next().equals("test "));
      assert (siter.next().equals("left "));
      assert (siter.next().equals("call "));
      assert (siter.next().equals("mesh "));

    }
  }

  @org.junit.Test
  public void testReadH5ShortArray() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = TestH5.openH5("support/short.h5")) {

      Variable dset = null;
      assert (null != (dset = ncfile.findVariable("IntArray")));
      assert (dset.getArrayType() == ArrayType.SHORT);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 5);
      d = dset.getDimension(1);
      assert (d.getLength() == 6);

      // read entire array
      Array<Short> As = (Array<Short>) dset.readArray();
      assert (As.getRank() == 2);

      int i, j;
      Index ima = As.getIndex();
      int[] shape = As.getShape();

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assert (As.get(ima.set(i, j)) == i + j);
        }
      }

      // read part of array
      dset.setCaching(false); // turn off caching to test read subset
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = dset.getShape()[1];
      As = (Array<Short>) dset.readArray(new Section(origin2, shape2));

      assert (As.getRank() == 2);

      for (j = 0; j < shape2[1]; j++) {
        assert (As.get(ima.set(0, j)) == j);
      }

      // rank reduction
      Array<Short> Areduce = Arrays.reduce(As);
      Index ima2 = Areduce.getIndex();
      assert (Areduce.getRank() == 1);

      for (j = 0; j < shape2[1]; j++) {
        assert (Areduce.get(ima2.set(j)) == j);
      }

    }
  }

}
