/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestOpenInMemory.java 51 2006-07-12 17:13:13Z caron $

package ucar.nc2;

import junit.framework.TestCase;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

public class TestOpenInMemory extends TestCase {

  public TestOpenInMemory(String name) {
    super(name);
  }

  private NetcdfFile openInMemory(String filename) throws IOException {
    String pathname = TestDir.cdmLocalTestDataDir + filename;
    System.out.println("**** OpenInMemory " + pathname);

    byte[] ba = IO.readFileToByteArray(pathname);
    NetcdfFile ncfile = NetcdfFiles.openInMemory("OpenInMemory", ba);
    System.out.println(ncfile);
    return ncfile;
  }

  public void testRead() throws IOException {

    try (NetcdfFile ncfile = openInMemory("testWrite.nc")) {

      assert (null != ncfile.findDimension("lat"));
      assert (null != ncfile.findDimension("lon"));

      Variable temp = null;
      assert (null != (temp = ncfile.findVariable("temperature")));

      // read entire array
      Array<Number> A;
      try {
        A = (Array<Number>) temp.readArray();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();
      assert shape[0] == 64;
      assert shape[1] == 128;

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j)).doubleValue();
          assert (dval == (double) (i * 1000000 + j * 1000)) : dval;
        }
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      try {
        A = (Array<Number>) temp.readArray(new Section(origin2, shape2));
      } catch (InvalidRangeException e) {
        System.err.println("ERROR reading file " + e);
        assert (false);
        return;
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 2);

      for (j = 0; j < shape2[1]; j++) {
        assert (A.get(ima.set(0, j)).doubleValue() == (double) (j * 1000));
      }

      // rank reduction
      Array<Number> Areduce = Arrays.reduce(A);
      Index ima2 = Areduce.getIndex();
      assert (Areduce.getRank() == 1);

      for (j = 0; j < shape2[1]; j++) {
        assert (Areduce.get(ima2.set(j)).doubleValue() == (double) (j * 1000));
      }

      // read char variable
      Array<Byte> Abyte = null;
      Variable c = null;
      assert (null != (c = ncfile.findVariable("svar")));
      try {
        Abyte = (Array<Byte>) c.readArray();
      } catch (IOException e) {
        assert (false);
      }
      assert (Abyte.getArrayType() == ArrayType.CHAR);
      String val = Arrays.makeStringFromChar(Abyte);
      assert val.equals("Testing 1-2-3") : val;
      // System.out.println( "val = "+ val);

      // read char variable 2
      Variable c2 = null;
      assert (null != (c2 = ncfile.findVariable("svar2")));
      try {
        Abyte = (Array<Byte>) c2.readArray();
      } catch (IOException e) {
        assert (false);
      }
      assert (Abyte.getArrayType() == ArrayType.CHAR);
      String val2 = Arrays.makeStringFromChar(Abyte);
      assert (val2.equals("Two pairs of ladies stockings!"));

      // read String Array
      Variable c3 = null;
      assert (null != (c3 = ncfile.findVariable("names")));
      try {
        Abyte = (Array<Byte>) c3.readArray();
      } catch (IOException e) {
        assert (false);
      }
      assert (Abyte.getArrayType() == ArrayType.CHAR);
      Array<String> vals = Arrays.makeStringsFromChar(Abyte);
      ima = vals.getIndex();
      assert (vals.get(ima.set(0)).equals("No pairs of ladies stockings!"));
      assert (vals.get(ima.set(1)).equals("One pair of ladies stockings!"));
      assert (vals.get(ima.set(2)).equals("Two pairs of ladies stockings!"));

      // read String Array - 2
      Variable c4 = null;
      assert (null != (c4 = ncfile.findVariable("names2")));
      try {
        Abyte = (Array<Byte>) c4.readArray();
      } catch (IOException e) {
        assert (false);
      }
      assert (Abyte.getArrayType() == ArrayType.CHAR);
      Array<String> vals3 = Arrays.makeStringsFromChar(Abyte);
      assert (vals3.get(0).equals("0 pairs of ladies stockings!"));
      assert (vals3.get(1).equals("1 pair of ladies stockings!"));
      assert (vals3.get(2).equals("2 pairs of ladies stockings!"));
    }
    System.out.println("**************TestRead done");
  }

}
