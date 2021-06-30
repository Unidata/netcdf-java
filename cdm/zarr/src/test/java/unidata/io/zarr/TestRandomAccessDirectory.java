/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package unidata.io.zarr;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.iosp.zarr.ZarrTestsCommon;
import ucar.unidata.io.KMPMatch;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.google.common.truth.Truth.assertThat;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.zarr.RandomAccessDirectory;
import ucar.unidata.io.zarr.RandomAccessDirectoryItem;

public class TestRandomAccessDirectory {

  private static final int TEST_BUFFER_SIZE = 100;
  private static final int EXPECTED_SIZE = 360;

  // contents of each test file
  private static final String TEST_FILE_STRING = "Hello world, this is a test.\r\nThis is a second line of text.";
  private static final byte[] UTF8_BYTES = TEST_FILE_STRING.getBytes(StandardCharsets.UTF_8);
  private static final int FILE_SIZE = UTF8_BYTES.length;

  // Object store
  private static final String S3_FILENAME = "object_store";
  private static final String OBJECT_STORE_ZARR_URI = ZarrTestsCommon.S3_PREFIX + ZarrTestsCommon.AWS_BUCKET_NAME + "?"
      + S3_FILENAME + "/" + "#" + ZarrTestsCommon.S3_FRAGMENT;

  // Local stores
  private static final String TEST_DATA_PATH =
      ZarrTestsCommon.LOCAL_TEST_DATA_PATH + "preserveLineEndings/directory_test_data/";
  private static final String DIRECTORY_FILENAME = "directory_store";
  private static final String ZIP_FILENAME = "zip_store.zip";
  private static final String DIRECTORY_STORE_URI = TEST_DATA_PATH + DIRECTORY_FILENAME;
  private static final String ZIP_STORE_URI = TEST_DATA_PATH + ZIP_FILENAME;

  // not directory paths
  private static final String FILENAME = "file1.txt";
  private static final String TEST_NOT_DIRECTORY_LOCAL = DIRECTORY_STORE_URI + "/" + FILENAME;
  private static final String TEST_NOT_DIRECTORY_OBJECT_STORE = ZarrTestsCommon.S3_PREFIX
      + ZarrTestsCommon.AWS_BUCKET_NAME + "?" + S3_FILENAME + "/" + FILENAME + "#" + ZarrTestsCommon.S3_FRAGMENT;

  private static List<RandomAccessFile> stores;


  @BeforeClass
  public static void setUpTests() throws IOException {
    stores = new ArrayList<>();
    stores.add(NetcdfFiles.getRaf(DIRECTORY_STORE_URI, TEST_BUFFER_SIZE));
    stores.add(NetcdfFiles.getRaf(OBJECT_STORE_ZARR_URI, TEST_BUFFER_SIZE));
    stores.add(NetcdfFiles.getRaf(ZIP_STORE_URI, TEST_BUFFER_SIZE));
  }

  @AfterClass
  public static void cleanUpTests() throws IOException {
    for (RandomAccessFile raf : stores) {
      raf.close();
    }
  }

  @Test
  public void testIsOwnerOf() throws IOException {
    // not directory
    RandomAccessFile invalidStore;
    invalidStore = NetcdfFiles.getRaf(TEST_NOT_DIRECTORY_LOCAL, -1);
    assertThat(invalidStore).isNotInstanceOf(RandomAccessDirectory.class);
    invalidStore = NetcdfFiles.getRaf(TEST_NOT_DIRECTORY_OBJECT_STORE, -1);
    assertThat(invalidStore).isNotInstanceOf(RandomAccessDirectory.class);

    stores.forEach(raf -> {
      assertThat(raf).isInstanceOf(RandomAccessDirectory.class);
    });
  }

  @Test
  public void testSetBufferSize() {
    stores.forEach(raf -> {
      _testSetBufferSize(raf);
    });
  }

  private void _testSetBufferSize(RandomAccessFile raf) {
    int bufferSize = TEST_BUFFER_SIZE * 2;
    raf.setBufferSize(bufferSize);
    assertThat(raf.getBufferSize()).isEqualTo(bufferSize);
    raf.setBufferSize(TEST_BUFFER_SIZE);
    assertThat(raf.getBufferSize()).isEqualTo(TEST_BUFFER_SIZE);
  }

  @Test
  public void testIsAtEndOfFile() throws IOException {
    for (RandomAccessFile raf : stores) {
      raf.seek(EXPECTED_SIZE);
      assertThat(raf.isAtEndOfFile()).isTrue();
      raf.seek(0);
      assertThat(raf.isAtEndOfFile()).isFalse();
    }
  }

  @Test
  public void testSeek() throws IOException {
    long pos = 101;
    for (RandomAccessFile raf : stores) {
      raf.seek(pos);
      assertThat(raf.getFilePointer()).isEqualTo(pos);
    }
  }

  @Test
  public void testByteOrder() throws IOException {
    for (RandomAccessFile raf : stores) {
      _testByteOrder(raf);
    }
  }

  private void _testByteOrder(RandomAccessFile raf) throws IOException {
    // directory store
    int BE_int = getInt(0, true);
    int LE_int = getInt(0, false);
    int pos = 0;
    raf.order(ByteOrder.LITTLE_ENDIAN);
    raf.seek(pos);
    // check is read as little endian
    assertThat(raf.readInt()).isEqualTo(LE_int);
    raf.order(ByteOrder.BIG_ENDIAN);
    raf.seek(pos);
    // check is read as big endian
    assertThat(raf.readInt()).isEqualTo(BE_int);
  }

  @Test
  public void testLength() throws IOException {
    for (RandomAccessFile raf : stores) {
      assertThat(raf.length()).isEqualTo(EXPECTED_SIZE);
    }
  }

  @Test
  public void testRead() throws IOException {
    for (RandomAccessFile raf : stores) {
      _testRead(raf);
    }
  }

  private void _testRead(RandomAccessFile raf) throws IOException {
    // read several known bytes in several files
    int pos = 95; // second file
    int expected = UTF8_BYTES[pos - FILE_SIZE];
    raf.seek(pos);
    assertThat(raf.read()).isEqualTo(expected);

    pos = 20; // first file
    expected = UTF8_BYTES[pos];;
    raf.seek(pos);
    assertThat(raf.read()).isEqualTo(expected);

    pos = 340; // last file
    expected = UTF8_BYTES[pos - (FILE_SIZE * 5)];;
    raf.seek(pos);
    assertThat(raf.read()).isEqualTo(expected);

    // test readBytes within file
    int nbytes = 4;
    byte[] bytes = new byte[nbytes];
    pos = 0; // start first file
    raf.seek(pos);
    byte[] expectedBytes = Arrays.copyOfRange(UTF8_BYTES, 0, 4);
    raf.readBytes(bytes, 0, nbytes);
    assertThat(bytes).isEqualTo(expectedBytes);

    // test readBytes across files/directories
    bytes = new byte[nbytes];
    pos = FILE_SIZE * 4 - 2; // last two bytes of fourth file
    raf.seek(pos);
    // first two bytes should come from fourth file, second two bytes from fifth file
    expectedBytes = new byte[] {UTF8_BYTES[FILE_SIZE - 2], UTF8_BYTES[FILE_SIZE - 1], UTF8_BYTES[0], UTF8_BYTES[1]};
    raf.readBytes(bytes, 0, nbytes);
    assertThat(bytes).isEqualTo(expectedBytes);
  }

  @Test
  public void testReadToByteChannel() throws IOException {
    for (RandomAccessFile raf : stores) {
      _testReadToByteChannel(raf);
    }
  }

  private void _testReadToByteChannel(RandomAccessFile raf) throws IOException {
    TestWritableByteChannel dest;
    byte[] out;
    long n;
    int nbytes = 4;

    // read within a file
    int offset = 95; // second file
    byte[] expectedBytes = Arrays.copyOfRange(UTF8_BYTES, offset - FILE_SIZE, offset - FILE_SIZE + 4);
    dest = new TestWritableByteChannel();
    n = raf.readToByteChannel(dest, offset, nbytes);
    assertThat(n).isEqualTo(nbytes);
    assertThat(dest.getBytes()).isEqualTo(expectedBytes);
    dest.reset();

    // test read across files/directories
    offset = FILE_SIZE * 4 - 2; // last two bytes of fourth file
    // first two bytes should come from fourth file, second two bytes from fifth file
    expectedBytes = new byte[] {UTF8_BYTES[FILE_SIZE - 2], UTF8_BYTES[FILE_SIZE - 1], UTF8_BYTES[0], UTF8_BYTES[1]};
    n = raf.readToByteChannel(dest, offset, nbytes);
    assertThat(n).isEqualTo(nbytes);
    assertThat(dest.getBytes()).isEqualTo(expectedBytes);
    dest.reset();

    // read past EOF
    offset = EXPECTED_SIZE - nbytes + 1;
    n = raf.readToByteChannel(dest, offset, nbytes);
    assertThat(n).isLessThan(nbytes);
  }

  @Test
  public void testReadFully() throws IOException {
    for (RandomAccessFile raf : stores) {
      _testReadFully(raf);
    }
  }

  private void _testReadFully(RandomAccessFile raf) throws IOException {
    // read fully across files/directories
    int nbytes = 4;
    byte[] bytes = new byte[nbytes];
    int pos = FILE_SIZE * 4 - 2; // last two bytes of big endian ints
    raf.seek(pos);
    // first two bytes should come from BE ints, second two bytes from BE longs directory
    byte[] expectedBytes =
        new byte[] {UTF8_BYTES[FILE_SIZE - 2], UTF8_BYTES[FILE_SIZE - 1], UTF8_BYTES[0], UTF8_BYTES[1]};
    raf.readFully(bytes);
    assertThat(bytes).isEqualTo(expectedBytes);

    // read fully, buff > file length
    raf.seek(0);
    byte[] finalBuff = new byte[EXPECTED_SIZE + 1];
    // TODO: RemoteRandomAccessFile is throwing inconsistent exception - should probably be caught and turned into
    // EOFException somewhere
    Assert.assertThrows(Exception.class, () -> {
      raf.readFully(finalBuff);
    });
  }

  @Test
  public void testSearchForward() throws IOException {
    for (RandomAccessFile raf : stores) {
      _testSearchForward(raf);
    }
  }

  private void _testSearchForward(RandomAccessFile raf) throws IOException {
    KMPMatch match = new KMPMatch("second line".getBytes(StandardCharsets.UTF_8));
    KMPMatch notMatch = new KMPMatch("Not a match".getBytes(StandardCharsets.UTF_8));
    raf.seek(0);
    assertThat(raf.searchForward(match, 10)).isFalse();
    assertThat(raf.searchForward(match, 100)).isTrue();
    assertThat(raf.searchForward(match, -1)).isTrue();
    assertThat(raf.searchForward(notMatch, -1)).isFalse();
    raf.seek(EXPECTED_SIZE - 100);
    assertThat(raf.searchForward(match, 100)).isTrue();
  }

  @Test
  public void testGetFiles() throws IOException {
    for (RandomAccessFile raf : stores) {
      _testGetFiles((RandomAccessDirectory) raf);
    }
  }

  private void _testGetFiles(RandomAccessDirectory raf) throws IOException {
    // get one file by name
    List<RandomAccessDirectoryItem> files = raf.getFilesInPath("dir1/nested1/file1.txt");
    assertThat(files).hasSize(1);

    // get all files under path
    files = raf.getFilesInPath("dir1");
    assertThat(files.size()).isEqualTo(4);
  }

  private int getInt(int startIndex, boolean bigEndian) throws IOException {
    int ch1 = UTF8_BYTES[startIndex];
    int ch2 = UTF8_BYTES[startIndex + 1];
    int ch3 = UTF8_BYTES[startIndex + 2];
    int ch4 = UTF8_BYTES[startIndex + 3];
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
      throw new EOFException();
    }

    if (bigEndian) {
      return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    } else {
      return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
    }
  }

  /**
   * simple WritableByteChannel implementation
   * Writes to outputstream
   */
  private class TestWritableByteChannel implements WritableByteChannel {

    private boolean open;
    private ByteArrayOutputStream dest;

    public TestWritableByteChannel() {
      open = true;
      dest = new ByteArrayOutputStream();
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
      byte[] out = src.array();
      dest.write(out);
      return out.length;
    }

    @Override
    public boolean isOpen() {
      return open;
    }

    @Override
    public void close() throws IOException {
      open = false;
    }

    public byte[] getBytes() {
      return dest.toByteArray();
    }

    public void reset() throws IOException {
      dest.reset();
    }
  }
}
