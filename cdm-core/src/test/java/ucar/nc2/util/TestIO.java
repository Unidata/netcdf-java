/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.internal.ncml.TestNcmlRead;

/** Test {@link ucar.nc2.util.IO} */
public class TestIO {
  private static String source;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeClass
  public static void setup() {
    source = String.format("What%nAre%nYou%nSaying%d%n", 999);
  }

  @Test
  public void testGetFileResource() throws IOException {
    String filename = TestNcmlRead.topDir + "aggExisting.xml";
    try (InputStream is = IO.getFileResource(filename)) {
      assertThat(is).isNotNull();
    }
  }

  @Test
  public void testCopy() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream stringIS = new ByteArrayInputStream(source.getBytes());

    IO.copy(stringIS, out);
    assertThat(out.toString()).isEqualTo(source);
  }

  @Test
  public void testCopyMaxBytes() throws IOException {
    ByteArrayInputStream stringIS = new ByteArrayInputStream(source.getBytes());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IO.copyMaxBytes(stringIS, out, 22);
    assertThat(out.toByteArray()).hasLength(22);
  }

  @Test
  public void testCopy2null() throws IOException {
    ByteArrayInputStream stringIS = new ByteArrayInputStream(source.getBytes());
    IO.copy2null(stringIS, 10);

    String filename = TestNcmlRead.topDir + "aggExisting.xml";
    try (FileInputStream fos = new FileInputStream(filename); FileChannel channel = fos.getChannel()) {
      IO.copy2null(channel, 10);
    }
  }

  @Test
  public void testReadContents() throws IOException {
    ByteArrayInputStream stringIS = new ByteArrayInputStream(source.getBytes());
    String contents = IO.readContents(stringIS);
    assertThat(contents).isEqualTo(source);
  }

  @Test
  public void testWriteContents() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IO.writeContents(source, out);
    assertThat(out.toString()).isEqualTo(source);
  }

  @Test
  public void testWriteStringToFile() throws IOException {
    File fileout = tempFolder.newFile();
    IO.writeToFile(source, fileout);
    String copy = IO.readFile(fileout.getAbsolutePath());
    assertThat(copy).isEqualTo(source);
  }

  @Test
  public void testWriteStringToFilename() throws IOException {
    File fileout = tempFolder.newFile();
    IO.writeToFile(source, fileout.getAbsolutePath());
    String copy = IO.readFile(fileout.getAbsolutePath());
    assertThat(copy).isEqualTo(source);
  }

  @Test
  public void testWriteBytesToFile() throws IOException {
    File fileout = tempFolder.newFile();
    IO.writeToFile(source.getBytes(), fileout);
    String copy = IO.readFile(fileout.getAbsolutePath());
    assertThat(copy).isEqualTo(source);
  }

  @Test
  public void testWriteStreamToFile() throws IOException {
    File fileout = tempFolder.newFile();
    ByteArrayInputStream stringIS = new ByteArrayInputStream(source.getBytes());
    IO.writeToFile(stringIS, fileout.getAbsolutePath());
    String copy = IO.readFile(fileout.getAbsolutePath());
    assertThat(copy).isEqualTo(source);
  }

  @Test
  public void testAppendStreamToFile() throws IOException {
    File fileout = tempFolder.newFile();
    ByteArrayInputStream stringIS = new ByteArrayInputStream(source.getBytes());
    IO.writeToFile(stringIS, fileout.getAbsolutePath());
    ByteArrayInputStream stringIS2 = new ByteArrayInputStream(source.getBytes());
    IO.appendToFile(stringIS2, fileout.getAbsolutePath());
    String copy = IO.readFile(fileout.getAbsolutePath());
    assertThat(copy.length()).isEqualTo(2 * source.length());
  }

  @Test
  public void testCopyFilename() throws IOException {
    String fileout = tempFolder.newFile().getAbsolutePath();
    String filein = TestNcmlRead.topDir + "aggExisting.xml";
    IO.copyFile(filein, fileout);

    String org = IO.readFile(filein);
    String contents = IO.readFile(fileout);
    assertThat(contents).isEqualTo(org);
  }

  @Test
  public void testCopyFile() throws IOException {
    File fileout = tempFolder.newFile();
    File filein = new File(TestNcmlRead.topDir + "aggExisting.xml");
    IO.copyFile(filein, fileout);

    String org = IO.readFile(filein.getAbsolutePath());
    String contents = IO.readFile(fileout.getAbsolutePath());
    assertThat(contents).isEqualTo(org);
  }

  @Test
  public void testCopy2File() throws IOException {
    String fileout = tempFolder.newFile().getAbsolutePath();
    IO.copy2File(source.getBytes(), fileout);

    String contents = IO.readFile(fileout);
    assertThat(contents).isEqualTo(source);
  }

  @Test
  public void testCopyFileOut() throws IOException {
    String filein = TestNcmlRead.topDir + "aggExisting.xml";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IO.copyFile(filein, out);

    String contents = IO.readFile(filein);
    assertThat(out.toString()).isEqualTo(contents);
  }

  @Test
  public void testCopyRafB() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ucar.unidata.io.RandomAccessFile raf =
        new ucar.unidata.io.RandomAccessFile(TestNcmlRead.topDir + "aggExisting.xml", "r");
    // (ucar.unidata.io.RandomAccessFile raf, long offset, long length, OutputStream out, byte[] buffer
    IO.copyRafB(raf, 11, 44, out, new byte[10]);
    assertThat(out.toByteArray()).hasLength(44);
  }

  @Test
  public void testCopyDirTree() throws IOException {
    IO.copyDirTree(TestNcmlRead.topDir + "exclude", tempFolder.newFolder().getAbsolutePath());
  }


}
