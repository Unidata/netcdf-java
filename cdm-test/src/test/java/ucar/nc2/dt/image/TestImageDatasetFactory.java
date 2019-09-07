package ucar.nc2.dt.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import javax.imageio.ImageIO;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestImageDatasetFactory {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public byte[] convert(String srcPath) throws IOException {
      try (GridDataset gds = GridDataset.open(srcPath)) {
        GeoGrid grid = gds.findGridByName("Pressure_surface");
        ImageDatasetFactory factory = new ImageDatasetFactory();
        BufferedImage image = factory.openDataset(grid);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        return os.toByteArray();
      }
    }

    @Test
    public void testStuff() throws IOException {
      convert(TestDir.cdmUnitTestDir + "ft/grid/GFS_Global_onedeg_20081229_1800.grib2.nc");
    }

}
