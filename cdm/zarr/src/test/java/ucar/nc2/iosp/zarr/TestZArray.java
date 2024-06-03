package ucar.nc2.iosp.zarr;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.nio.ByteOrder;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.iosp.zarr.ZArray.Order;

public class TestZArray {
  private static final String DTYPE = ">f8";
  private static final String ORDER = "C";
  private static final String DELIMITER = ".";

  @Test
  public void shouldParseDataType() throws ZarrFormatException {
    final String dtype = ">f8";
    final ZArray zArray = new ZArray(null, null, null, dtype, null, ORDER, null, DELIMITER);
    assertThat(zArray.getDataType()).isEqualTo(DataType.DOUBLE);
    assertThat(zArray.getDtype()).isEqualTo(dtype);
  }

  @Test
  public void shouldRefuseInvalidDataType() {
    final String dtype = ">a5";
    assertThrows(ZarrFormatException.class, () -> new ZArray(null, null, null, dtype, null, ORDER, null, DELIMITER));
  }

  @Test
  public void shouldParseBigEndianByteOrder() throws ZarrFormatException {
    final String dtype = ">f4";
    final ZArray zArray = new ZArray(null, null, null, dtype, null, ORDER, null, DELIMITER);
    assertThat(zArray.getByteOrder()).isEqualTo(ByteOrder.BIG_ENDIAN);
  }

  @Test
  public void shouldParseLittleEndianByteOrder() throws ZarrFormatException {
    final String dtype = "<f4";
    final ZArray zArray = new ZArray(null, null, null, dtype, null, ORDER, null, DELIMITER);
    assertThat(zArray.getByteOrder()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
  }

  @Test
  public void shouldParseNativeByteOrder() throws ZarrFormatException {
    final String dtype = "|f4";
    final ZArray zArray = new ZArray(null, null, null, dtype, null, ORDER, null, DELIMITER);
    assertThat(zArray.getByteOrder()).isEqualTo(ByteOrder.nativeOrder());
  }

  @Test
  public void shouldRefuseInvalidByteOrder() {
    final String dtype = "f4";
    assertThrows(ZarrFormatException.class, () -> new ZArray(null, null, null, dtype, null, ORDER, null, DELIMITER));
  }

  @Test
  public void shouldParseRowMajorOrder() throws ZarrFormatException {
    final String order = "C";
    final ZArray zArray = new ZArray(null, null, null, DTYPE, null, order, null, DELIMITER);
    assertThat(zArray.getOrder()).isEqualTo(Order.C);
  }

  @Test
  public void shouldParseColumnMajorOrder() throws ZarrFormatException {
    final String order = "F";
    final ZArray zArray = new ZArray(null, null, null, DTYPE, null, order, null, DELIMITER);
    assertThat(zArray.getOrder()).isEqualTo(Order.F);
  }

  @Test
  public void shouldRefuseInvalidOrder() {
    final String order = "A";
    assertThrows(ZarrFormatException.class, () -> new ZArray(null, null, null, DTYPE, null, order, null, DELIMITER));
  }

  @Test
  public void shouldParseDotSeparator() throws ZarrFormatException {
    final String separator = ".";
    final ZArray zArray = new ZArray(null, null, null, DTYPE, null, ORDER, null, separator);
    assertThat(zArray.getSeparator()).isEqualTo(separator);
  }

  @Test
  public void shouldParseSlashSeparator() throws ZarrFormatException {
    final String separator = "/";
    final ZArray zArray = new ZArray(null, null, null, DTYPE, null, ORDER, null, separator);
    assertThat(zArray.getSeparator()).isEqualTo(separator);
  }

  @Test
  public void shouldRefuseInvalidSeparator() {
    final String separator = "-";
    assertThrows(ZarrFormatException.class, () -> new ZArray(null, null, null, DTYPE, null, ORDER, null, separator));
  }
}
