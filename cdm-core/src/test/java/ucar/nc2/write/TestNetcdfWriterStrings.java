/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import com.google.common.base.Charsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test using non ascii identifiers with Netcdf3 */
public class TestNetcdfWriterStrings {

  static int[] helloGreekCode =
      new int[] {0xce, 0x9a, 0xce, 0xb1, 0xce, 0xbb, 0xce, 0xb7, 0xce, 0xbc, 0xe1, 0xbd, 0xb3, 0xcf, 0x81, 0xce, 0xb1};
  static int helloGreekLen = 20;
  static int ngreeks = 3;
  static String geeks = "geeks";

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void writeNetCDFchar() throws IOException, InvalidRangeException {
    String helloGreek = makeString(helloGreekCode, true);
    System.out.printf("writeNetCDFchar= %s%n", showBoth(helloGreek));
    String helloGreek2 = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.printf(" normalized= %s%n", showBoth(helloGreek2));

    String filename = tempFolder.newFile().getPath();
    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(new Dimension(helloGreek, helloGreekLen));
    writerb.addVariable(helloGreek, ArrayType.CHAR, helloGreek).addAttribute(new Attribute("units", helloGreek));

    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable(helloGreek);
      byte[] helloBytes = helloGreek.getBytes();
      Array<Byte> data = Arrays.factory(ArrayType.CHAR, new int[] {helloBytes.length}, helloBytes);
      writer.write(v, data.getIndex(), data);
    }

    try (NetcdfFile ncout = NetcdfFiles.open(filename)) {
      Variable vr = ncout.findVariable(helloGreek);
      assertThat(vr).isNotNull();
      assertThat(vr.getShortName()).isEqualTo(helloGreek);

      Array<?> vrdata = vr.readArray();
      assertThat(vrdata.getArrayType()).isEqualTo(ArrayType.CHAR); // writing to netcdf3 turns it into a char
      assertThat(vrdata.getShape()).isEqualTo(new int[] {helloGreekLen});
      System.out.printf(" writeNetCDFchar printArray = %ss%n", NcdumpArray.printArray(vrdata));
      System.out.printf(" writeNetCDFchar showBytes  = %ss%n", showBytes((Array<Byte>) vrdata));

      Array<String> sdata = Arrays.makeStringsFromChar((Array<Byte>) vrdata);
      String strData = sdata.getScalar();
      System.out.printf(" writeNetCDFchar read = %s%n", showBoth(strData));
      assertThat(strData).isEqualTo(helloGreek);

      Attribute att = vr.findAttribute("units");
      assertThat(att).isNotNull();
      assertThat(att.isString()).isTrue();
      assertThat(att.getStringValue()).isEqualTo(helloGreek);
    }
  }

  @Test
  public void writeNetCDFcharArray() throws IOException, InvalidRangeException {
    String helloGreek = makeString(helloGreekCode, true);
    // helloGreek = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.printf("writeNetCDFcharArray=%s%n", showBoth(helloGreek));

    String filename = tempFolder.newFile().getPath();
    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(new Dimension(geeks, ngreeks));
    writerb.addDimension(new Dimension(helloGreek, helloGreekLen));
    writerb.addVariable(helloGreek, ArrayType.CHAR, geeks + " " + helloGreek)
        .addAttribute(new Attribute("units", helloGreek));

    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable(helloGreek);
      byte[] helloBytes = helloGreek.getBytes();
      Array<Byte> data = Arrays.factory(ArrayType.CHAR, new int[] {1, helloBytes.length}, helloBytes);
      Index index = data.getIndex();
      for (int i = 0; i < ngreeks; i++) {
        writer.write(v, index.set0(i), data);
      }
    }

    try (NetcdfFile ncout = NetcdfFiles.open(filename)) {
      Variable vr = ncout.findVariable(helloGreek);
      assertThat(vr).isNotNull();
      assertThat(vr.getShortName()).isEqualTo(helloGreek);

      Array<?> vrdata = vr.readArray();
      assertThat(vrdata.getArrayType()).isEqualTo(ArrayType.CHAR); // writing to netcdf3 turns it into a char
      assertThat(vrdata.getShape()).isEqualTo(new int[] {ngreeks, helloGreekLen});
      Array<String> sdata = Arrays.makeStringsFromChar((Array<Byte>) vrdata);
      for (int i = 0; i < ngreeks; i++) {
        String strData = sdata.get(i);
        System.out.printf(" writeNetCDFcharArray read = %s%n", showBoth(strData));
      }
      for (int i = 0; i < ngreeks; i++) {
        String strData = sdata.get(i);
        assertThat(strData).isEqualTo(helloGreek);
      }
    }
  }

  @Test
  public void writeNetCDFstring() throws IOException, InvalidRangeException {
    String helloGreek = makeString(helloGreekCode, true);
    helloGreek = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.printf("writeNetCDFstring=%s%n", showBoth(helloGreek));

    String filename = tempFolder.newFile().getPath();
    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(new Dimension(helloGreek, helloGreekLen));
    writerb.addVariable(helloGreek, ArrayType.STRING, helloGreek).addAttribute(new Attribute("units", helloGreek));

    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable(helloGreek);
      Array<String> data = Arrays.factory(ArrayType.STRING, new int[] {1}, new String[] {helloGreek});
      writer.write(v, data.getIndex(), data);
    }

    try (NetcdfFile ncout = NetcdfFiles.open(filename)) {
      Variable vr = ncout.findVariable(helloGreek);
      assertThat(vr).isNotNull();
      assertThat(vr.getShortName()).isEqualTo(helloGreek);

      Array<?> vrdata = vr.readArray();
      assertThat(vrdata.getArrayType()).isEqualTo(ArrayType.CHAR); // writing to netcdf3 turns it into a char
      assertThat(vrdata.getShape()).isEqualTo(new int[] {helloGreekLen});
      Array<String> sdata = Arrays.makeStringsFromChar((Array<Byte>) vrdata);
      String strData = sdata.getScalar();
      System.out.printf(" writeNetCDFstring read = %s%n", showBoth(strData));
      assertThat(strData).isEqualTo(helloGreek);
    }
  }

  @Test
  public void testWriteStringData() throws IOException, InvalidRangeException {
    String helloGreek = makeString(helloGreekCode, false);
    helloGreek = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.printf("testWriteStringData=%s%n", showBoth(helloGreek));

    String filename = tempFolder.newFile().getPath();
    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(new Dimension(geeks, ngreeks));
    writerb.addDimension(new Dimension(helloGreek, helloGreekLen));
    writerb.addVariable(helloGreek, ArrayType.CHAR, geeks + " " + helloGreek)
        .addAttribute(new Attribute("units", helloGreek));

    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable(helloGreek);
      Index index = Index.ofRank(v.getRank());
      for (int i = 0; i < ngreeks; i++) {
        writer.writeStringData(v, index.set0(i), helloGreek);
      }
    }

    try (NetcdfFile ncout = NetcdfFiles.open(filename)) {
      Variable vr = ncout.findVariable(helloGreek);
      assertThat(vr).isNotNull();
      assertThat(vr.getShortName()).isEqualTo(helloGreek);

      Array<?> vrdata = vr.readArray();
      assertThat(vrdata.getArrayType()).isEqualTo(ArrayType.CHAR); // writing to netcdf3 turns it into a char
      assertThat(vrdata.getShape()).isEqualTo(new int[] {ngreeks, helloGreekLen});
      Array<String> sdata = Arrays.makeStringsFromChar((Array<Byte>) vrdata);
      for (int i = 0; i < ngreeks; i++) {
        String strData = sdata.get(i);
        System.out.printf(" testWriteStringData read = %s%n", showBoth(strData));
        assertThat(strData).isEqualTo(helloGreek);
      }
    }
  }

  ///////////////////////////////////////////
  private String makeString(int[] codes, boolean debug) throws UnsupportedEncodingException {
    byte[] b = new byte[codes.length];
    for (int i = 0; i < codes.length; i++)
      b[i] = (byte) codes[i];
    if (debug)
      System.out.println(" orgBytes= " + showBytes(b));
    String s = new String(b, StandardCharsets.UTF_8);
    if (debug)
      System.out.println("convBytes= " + showString(s));
    return s;
  }

  private String showBytes(byte[] buff) {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (i > 0)
        sbuff.append(" ");
      sbuff.append(Integer.toHexString(ub));
    }
    return sbuff.toString();
  }

  private String showBytes(Array<Byte> buff) {
    StringBuffer sbuff = new StringBuffer();
    for (byte b : buff) {
      int ub = (b < 0) ? b + 256 : b;
      sbuff.append(" ");
      sbuff.append(Integer.toHexString(ub));
    }
    return sbuff.toString();
  }

  private String showString(String s) {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i);
      if (i > 0)
        sbuff.append(" ");
      sbuff.append(Integer.toHexString(c));
    }
    return sbuff.toString();
  }

  private String showBoth(String s) {
    Formatter sbuff = new Formatter();
    sbuff.format(" %s == %s", s, showBytes(s.getBytes(Charsets.UTF_8)));
    return sbuff.toString();
  }

}
