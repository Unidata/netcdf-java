/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.stream.NcStream;

/** Test {@link ucar.nc2.iosp.IospHelper} */
public class TestIospHelper {

  @Test
  public void testCopyToOutputStreamFloat() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    float[] floatdata = new float[] {1f, 2f, 3.2f};
    Array data = Array.factory(DataType.FLOAT, new int[] {3}, floatdata);
    long bytesCopied = IospHelper.copyToOutputStream(data, bos);
    assertThat(bytesCopied).isEqualTo(12);
    FloatBuffer floatbuff = ByteBuffer.wrap(bos.toByteArray()).asFloatBuffer();
    assertThat(floatbuff.get()).isEqualTo(1f);
    assertThat(floatbuff.get()).isEqualTo(2f);
    assertThat(floatbuff.get()).isEqualTo(3.2f);
    assertThat(floatbuff.hasRemaining()).isFalse();
  }

  @Test
  public void testCopyToOutputStreamInt() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    int[] data = new int[] {-1, 0, 1};
    Array array = Array.factory(DataType.INT, new int[] {3}, data);
    long bytesCopied = IospHelper.copyToOutputStream(array, bos);
    assertThat(bytesCopied).isEqualTo(12);
    IntBuffer buff = ByteBuffer.wrap(bos.toByteArray()).asIntBuffer();
    assertThat(buff.get()).isEqualTo(-1);
    assertThat(buff.get()).isEqualTo(0);
    assertThat(buff.get()).isEqualTo(1);
    assertThat(buff.hasRemaining()).isFalse();
  }

  @Test
  public void testCopyToOutputStreamShort() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    short[] data = new short[] {99, 999, 9999};
    Array array = Array.factory(DataType.SHORT, new int[] {3}, data);
    long bytesCopied = IospHelper.copyToOutputStream(array, bos);
    assertThat(bytesCopied).isEqualTo(6);
    ShortBuffer buff = ByteBuffer.wrap(bos.toByteArray()).asShortBuffer();
    assertThat(buff.get()).isEqualTo(99);
    assertThat(buff.get()).isEqualTo(999);
    assertThat(buff.get()).isEqualTo(9999);
    assertThat(buff.hasRemaining()).isFalse();
  }

  @Test
  public void testCopyToOutputStreamBoolean() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    boolean[] data = new boolean[] {true, false, true};
    Array array = Array.factory(DataType.BOOLEAN, new int[] {3}, data);
    long bytesCopied = IospHelper.copyToOutputStream(array, bos);
    assertThat(bytesCopied).isEqualTo(3);
    ByteBuffer buff = ByteBuffer.wrap(bos.toByteArray());
    assertThat(buff.get()).isEqualTo(1);
    assertThat(buff.get()).isEqualTo(0);
    assertThat(buff.get()).isEqualTo(1);
    assertThat(buff.hasRemaining()).isFalse();
  }

  @Test
  public void testCopyToOutputStreamString() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    String[] data = new String[] {"yes", "no", "maybe"};
    Array array = Array.factory(DataType.STRING, new int[] {3}, data);
    long bytesCopied = IospHelper.copyToOutputStream(array, bos);
    assertThat(bytesCopied).isEqualTo(13);

    ByteArrayInputStream bas = new ByteArrayInputStream(bos.toByteArray());
    DataInputStream dox = new DataInputStream(bas);

    assertThat(NcStream.readString(dox)).isEqualTo("yes");
    assertThat(NcStream.readString(dox)).isEqualTo("no");
    assertThat(NcStream.readString(dox)).isEqualTo("maybe");
    assertThat(dox.available()).isEqualTo(0);
  }

  @Test
  public void testCopyToOutputStreamBB() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ByteBuffer[] data = new ByteBuffer[3];
    data[0] = ByteBuffer.wrap(new byte[] {1});
    data[1] = ByteBuffer.wrap(new byte[] {1, 2});
    data[2] = ByteBuffer.wrap(new byte[] {1, 2, 3});
    Array array = Array.factory(DataType.OPAQUE, new int[] {3}, data);
    long bytesCopied = IospHelper.copyToOutputStream(array, bos);
    assertThat(bytesCopied).isEqualTo(9);

    ByteArrayInputStream bas = new ByteArrayInputStream(bos.toByteArray());
    DataInputStream dox = new DataInputStream(bas);

    assertThat(NcStream.readByteBuffer(dox)).isEqualTo(data[0]);
    assertThat(NcStream.readByteBuffer(dox)).isEqualTo(data[1]);
    assertThat(NcStream.readByteBuffer(dox)).isEqualTo(data[2]);
    assertThat(dox.available()).isEqualTo(0);
  }

  @Test
  public void testCopyToOutputStreamVlen() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Array[] data = new Array[3];
    data[0] = Array.factory(DataType.BYTE, new int[] {1}, new byte[] {1});
    data[1] = Array.factory(DataType.INT, new int[] {2}, new int[] {1, 2});
    data[2] = Array.factory(DataType.DOUBLE, new int[] {3}, new double[] {1, 2, 3.14});
    Array array = ArrayObject.factory(DataType.OBJECT, new int[] {3}, data);
    long bytesCopied = IospHelper.copyToOutputStream(array, bos);
    assertThat(bytesCopied).isEqualTo(36);

    ByteArrayInputStream bas = new ByteArrayInputStream(bos.toByteArray());
    DataInputStream dox = new DataInputStream(bas);

    assertThat(NcStream.readByteBuffer(dox)).isEqualTo(data[0].getDataAsByteBuffer());
    assertThat(NcStream.readByteBuffer(dox)).isEqualTo(data[1].getDataAsByteBuffer());
    assertThat(NcStream.readByteBuffer(dox)).isEqualTo(data[2].getDataAsByteBuffer());
    assertThat(dox.available()).isEqualTo(0);
  }

  @Test
  public void testMakePrimitiveArray() throws InvalidRangeException {
    Object sresult = IospHelper.makePrimitiveArray(10, DataType.STRING);
    String[] stringFill = (String[]) sresult;
    for (String s : stringFill) {
      assertThat(s).isEqualTo(null);
    }
  }

  @Test
  public void testMakePrimitiveArrayWithFill() throws InvalidRangeException {
    assertThat(IospHelper.makePrimitiveArray(11, DataType.OPAQUE, 22l)).isEqualTo(new byte[11]);

    Object sresult = IospHelper.makePrimitiveArray(10, DataType.STRING, "fill");
    String[] stringFill = (String[]) sresult;
    for (String s : stringFill) {
      assertThat(s).isEqualTo("fill");
    }

    Object stresult = IospHelper.makePrimitiveArray(10, DataType.STRUCTURE, new byte[] {1, 2, 3});
    byte[] fill = (byte[]) stresult;
    for (int i = 0; i < 10; i++) {
      assertThat(fill[i]).isEqualTo(i < 3 ? i + 1 : 0);
    }
  }

}
