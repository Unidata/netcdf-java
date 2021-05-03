package unidata.io.zarr;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.NetcdfFiles;
import ucar.unidata.io.KMPMatch;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Executable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import static com.google.common.truth.Truth.assertThat;
import software.amazon.awssdk.regions.Region;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.zarr.RandomAccessDirectory;
import ucar.unidata.io.zarr.RandomAccessDirectoryItem;

public class TestRandomAccessDirectory {
  ////////////////////////////////////
  // Test data uses the following structure:
  // -root directory
  // -byte-ordered-data
  // -byte-order-independent-data | -byte-ordered-data
  // -booleans -strings | -little-endian-data | -big-endian-data
  // -ints -longs -floats -doubles | -ints -longs -floats -doubles

  // AWS props
  public static final String AWS_REGION_PROP_NAME = "aws.region";
  public static final String AWS_REGION = Region.US_EAST_1.toString();
  public static final String AWS_BUCKET_NAME = "unidata-zarr-test-data";
  public static final String S3_PREFIX = "cdms3:";
  public static final String S3_FRAGMENT = "delimiter=/";

  // test file names
  public static final String ZARR_FILENAME = "test_data.zarr/";
  public static final String COMPRESSED_ZARR_FILENAME = "test_data.zip";

  // Object stores
  public static final String OBJECT_STORE_ZARR_URI =
      S3_PREFIX + AWS_BUCKET_NAME + "?" + ZARR_FILENAME + "#" + S3_FRAGMENT;

  // Local stores
  public static final String LOCAL_TEST_DATA_PATH = "src/test/data/preserveLineEndings/";
  public static final String DIRECTORY_STORE_URI = LOCAL_TEST_DATA_PATH + ZARR_FILENAME;
  public static final String ZIP_STORE_URI = LOCAL_TEST_DATA_PATH + COMPRESSED_ZARR_FILENAME;


  private static final String TEST_NOT_DIRECTORY_LOCAL = DIRECTORY_STORE_URI + ".zgroup";
  private static final String TEST_NOT_DIRECTORY_OBJECT_STORE =
      S3_PREFIX + AWS_BUCKET_NAME + "?" + ZARR_FILENAME + ".zgroup" + "#" + S3_FRAGMENT;

  private static final int TEST_BUFFER_SIZE = 100;
  private static final int EXPECTED_SIZE = 5499;

  private static RandomAccessFile directoryStore;
  private static RandomAccessFile objectStore;
  private static RandomAccessFile zipStore;


  @BeforeClass
  public static void setUpTests() throws IOException {
    directoryStore = NetcdfFiles.getRaf(DIRECTORY_STORE_URI, TEST_BUFFER_SIZE);
    objectStore = NetcdfFiles.getRaf(OBJECT_STORE_ZARR_URI, TEST_BUFFER_SIZE);
    // zipStore = NetcdfFiles.getRaf(ZIP_STORE_URI, TEST_BUFFER_SIZE);
  }

  @AfterClass
  public static void cleanUpTests() throws IOException {
    directoryStore.close();
    objectStore.close();
    // zipStore.close();
  }

  @Test
  public void testIsOwnerOf() throws IOException {
    // not directory
    RandomAccessFile invalidStore;
    invalidStore = NetcdfFiles.getRaf(TEST_NOT_DIRECTORY_LOCAL, -1);
    assertThat(invalidStore).isNotInstanceOf(RandomAccessDirectory.class);
    invalidStore = NetcdfFiles.getRaf(TEST_NOT_DIRECTORY_OBJECT_STORE, -1);
    assertThat(invalidStore).isNotInstanceOf(RandomAccessDirectory.class);

    // directory store types
    assertThat(directoryStore).isInstanceOf(RandomAccessDirectory.class);
    assertThat(objectStore).isInstanceOf(RandomAccessDirectory.class);
    // assertThat(zipStore).isInstanceOf(RandomAccessDirectory.class);
  }

  @Test
  public void testSetBufferSize() {
    _testSetBufferSize(directoryStore);
    _testSetBufferSize(objectStore);
    // _testSetBufferSize(zipStore);
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
    // directory
    directoryStore.seek(EXPECTED_SIZE);
    assertThat(directoryStore.isAtEndOfFile()).isTrue();
    directoryStore.seek(0);
    assertThat(directoryStore.isAtEndOfFile()).isFalse();

    // object store
    objectStore.seek(EXPECTED_SIZE);
    assertThat(objectStore.isAtEndOfFile()).isTrue();
    objectStore.seek(0);
    assertThat(objectStore.isAtEndOfFile()).isFalse();

    // // zip store
    // zipStore.seek(EXPECTED_SIZE);
    // assertThat(zipStore.isAtEndOfFile()).isTrue();
    // zipStore.seek(0);
    // assertThat(zipStore.isAtEndOfFile()).isFalse();
  }

  @Test
  public void testSeek() throws IOException {
    long pos = 100;
    // directory
    directoryStore.seek(pos);
    assertThat(directoryStore.getFilePointer()).isEqualTo(pos);
    // object store
    objectStore.seek(pos);
    assertThat(objectStore.getFilePointer()).isEqualTo(pos);
  }

  @Test
  public void testByteOrder() throws IOException {
    _testByteOrder(directoryStore);
    _testByteOrder(objectStore);
  }

  private void _testByteOrder(RandomAccessFile raf) throws IOException {
    // directory store
    int BE_int = 33632516;
    int LE_int = 70320386;
    int pos = 1650; // start of int data
    raf.order(ByteOrder.LITTLE_ENDIAN);
    raf.seek(pos);
    // check is read as little endian
    assertThat(raf.readInt()).isEqualTo(LE_int);
    raf.order(ByteOrder.BIG_ENDIAN);
    raf.seek(pos);
    assertThat(raf.readInt()).isEqualTo(BE_int);
  }

  @Test
  public void testLength() throws IOException {
    assertThat(directoryStore.length()).isEqualTo(EXPECTED_SIZE);
    assertThat(objectStore.length()).isEqualTo(EXPECTED_SIZE);
  }

  @Test
  public void testRead() throws IOException {
    _testRead(directoryStore);
    _testRead(objectStore);
  }

  private void _testRead(RandomAccessFile raf) throws IOException {
    // read several known bytes in several files
    int pos = 337; // start of big endian double data
    int expected = 2;
    raf.seek(pos);
    assertThat(raf.read()).isEqualTo(expected);

    pos = 3992; // 10 bytes into little endian int data
    expected = 0;
    raf.seek(pos);
    assertThat(raf.read()).isEqualTo(expected);

    pos = 5043; // start of second boolean data file
    expected = 2;
    raf.seek(pos);
    assertThat(raf.read()).isEqualTo(expected);

    // test readBytes within file
    int nbytes = 4;
    byte[] bytes = new byte[nbytes];
    pos = 1650; // start of big endian int data
    raf.seek(pos);
    byte[] expectedBytes = new byte[] {2, 1, 49, 4};
    raf.readBytes(bytes, 0, nbytes);
    assertThat(bytes).isEqualTo(expectedBytes);

    // test readBytes across files/directories
    bytes = new byte[nbytes];
    pos = 1809; // last two bytes of big endian ints
    raf.seek(pos);
    // first two bytes should come from BE ints, second two bytes from BE longs directory
    expectedBytes = new byte[] {98, 99, 123, 10};
    raf.readBytes(bytes, 0, nbytes);
    assertThat(bytes).isEqualTo(expectedBytes);
  }

  @Test
  public void testReadToByteChannel() throws IOException {
    _testReadToByteChannel(directoryStore);
    // _testReadToByteChannel(objectStore);
  }

  private void _testReadToByteChannel(RandomAccessFile raf) throws IOException {
    TestWritableByteChannel dest;
    byte[] out;
    long n;
    int nbytes = 4;

    // read within a file
    int offset = 1650; // start of big endian int data
    byte[] expectedBytes = new byte[] {2, 1, 49, 4};
    dest = new TestWritableByteChannel();
    n = raf.readToByteChannel(dest, offset, nbytes);
    assertThat(n).isEqualTo(nbytes);
    assertThat(dest.getBytes()).isEqualTo(expectedBytes);
    dest.reset();

    // test read across files/directories
    offset = 1809; // last two bytes of big endian ints
    // first two bytes should come from BE ints, second two bytes from BE longs directory
    expectedBytes = new byte[] {98, 99, 123, 10};
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
    _testReadFully(directoryStore, EOFException.class);
    // TODO: RemoteRandomAccessFile is throwing this exception - should probably be caught and turned into
    //      EOFException somewhere
    _testReadFully(objectStore, com.google.common.util.concurrent.UncheckedExecutionException.class);
  }

  private void _testReadFully(RandomAccessFile raf, Class exceptionclass) throws IOException {
    // read fully across files/directories
    int nbytes = 4;
    byte[] bytes = new byte[nbytes];
    int pos = 1809; // last two bytes of big endian ints
    raf.seek(pos);
    // first two bytes should come from BE ints, second two bytes from BE longs directory
    byte[] expectedBytes = new byte[] {98, 99, 123, 10};
    raf.readFully(bytes);
    assertThat(bytes).isEqualTo(expectedBytes);

    // read fully, buff > file length
    raf.seek(0);
    byte[] finalBuff = new byte[EXPECTED_SIZE + 1];
    Assert.assertThrows(exceptionclass, () -> {
      raf.readFully(finalBuff);
    });
  }

  @Test
  public void testSearchForward() throws IOException {
    _testSearchForward(directoryStore);
    _testSearchForward(objectStore);
  }

  private void _testSearchForward(RandomAccessFile raf) throws IOException {
    KMPMatch match = new KMPMatch("zarr_format".getBytes(StandardCharsets.UTF_8));
    KMPMatch notMatch = new KMPMatch("Not a match".getBytes(StandardCharsets.UTF_8));
    raf.seek(0);
    assertThat(raf.searchForward(match, 100)).isFalse();
    assertThat(raf.searchForward(match, -1)).isTrue();
    assertThat(raf.searchForward(notMatch, -1)).isFalse();
    raf.seek(EXPECTED_SIZE - 100);
    assertThat(raf.searchForward(match, 100)).isTrue();
  }

  @Test
  public void testGetLastModified() {

  }

  @Test
  public void testLazyLoad() {

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
