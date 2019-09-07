package ucar.nc2.dt.image;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Precursor to using ImageDatasetFactory I think.
 */
@Category(NeedsCdmUnitTest.class)
public class TestBufferedImage {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public byte[] convert(String srcPath, double a, double b) throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(srcPath)) {
      Variable v = ncfile.findVariable("image1/image_data");
      Array array = v.read();

      int[] cmap = new int[256]; // palette
      cmap[0] = 0x00FFFFFF; // transparent and white
      for (int i = 1; i != 256; i++) {
        // 1 to 255 renders as (almost) white to black
        cmap[i] = 0xFF000000 | ((0xFF - i) * 0x010101);
      }
      IndexColorModel colorModel =
          new IndexColorModel(8, cmap.length, cmap, 0, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

      int[] shape = array.getShape();
      BufferedImage bi = new BufferedImage(shape[1], shape[0], BufferedImage.TYPE_BYTE_INDEXED, colorModel);

      Index index = array.getIndex();
      for (int y = 0; y < shape[0]; y++) {
        for (int x = 0; x < shape[1]; x++) {
          index.set(y, x);

          byte bval = array.getByte(index);
          double dval = v.getDataType().isUnsigned() ? (double) DataType.unsignedByteToShort(bval) : (double) bval;

          // double dval = array.getDouble(index);
          // Fix for NetCDF returning all values larger than 127 as (value - 256):
          // if (dval < -1) {
          // dval += 256;
          // }
          int pval = (int) Math.round(a * dval + b);
          pval = Math.min(Math.max(pval, 0), 255);
          bi.getRaster().setSample(x, y, 0, pval);
        }
      }

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ImageIO.write(bi, "png", os);
      return os.toByteArray();
    }
  }

  @Test
  public void testStuff() throws IOException {
    convert(TestDir.cdmUnitTestDir + "formats/hdf5/exclude/RAD_NL25_PCP_NA_200804110600.h5", 1.0, 0.0);
  }

}
