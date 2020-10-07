package tests;

import examples.DatasetUrlExamples;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.httpservices.Escape;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.category.NeedsExternalResource;

@Category(NeedsExternalResource.class)
public class TestDatasetUrlExamples {

  // matches the .dods file example in docs/src/public/userguide/pages/netcdfJava/developer/DatasetUrls.md
  private static final String baseFilename = "NAM_20161031_1200.nc";
  private static final String constraintExpression = "time[0:1:0],y[0:100:427],x[0:100:613],lat[0:100:427][0:100:613],lon[0:100:427][0:100:613],Temperature_height_above_ground[0:1:0][0:1:0][0:100:427][0:100:613],height_above_ground1[0:1:1]";
  private static final String tds = "https://thredds.ucar.edu/thredds/dodsC/casestudies/python-gallery/";

  private static final String dodsUrl = tds + baseFilename + ".dods";
  private static final String dasUrl = dodsUrl.replace(".dods", ".das");
  private static final String  ddsUrl = dodsUrl.replace(".dods", ".dds");

  private static File[] tempFiles;
  private static File dodsFile;

  @BeforeClass
  public static void setup() throws IOException {
    Path tmpDownloadDir = Files.createTempDirectory("ncj_tests_");
    tmpDownloadDir.toFile().deleteOnExit();
    String escapedCe = Escape.escapeURLQuery(constraintExpression);
    dodsFile = tmpDownloadDir.resolve(baseFilename + ".dods").toFile();
    File ddsFile = tmpDownloadDir.resolve(baseFilename + ".dds").toFile();
    File dasFile = tmpDownloadDir.resolve(baseFilename + ".das").toFile();
    IO.readURLtoFile(dodsUrl + "?" + escapedCe, dodsFile);
    IO.readURLtoFile(ddsUrl + "?" + escapedCe, ddsFile);
    IO.readURLtoFile(dasUrl, dasFile);
    tempFiles = new File[] {dodsFile, ddsFile, dasFile};
  }

  @Test
  public void testAwsGoes16Example() throws IOException {
    // awsS3Goes16Example method uses Google Truth
    DatasetUrlExamples.awsGoes16Example();
  }

  @Test
  public void testGcsS3Goes16Example() throws IOException {
    // gcsGoes16Example method uses Google Truth
    DatasetUrlExamples.gcsGoes16Example();
  }

  @Test
  public void testOsdcS3Goes16Example() throws IOException {
    // osdcGoes16Example method uses Google Truth
    DatasetUrlExamples.osdcGoes16Example();
  }

  @Test
  public void testDodsFileRead() throws IOException {
    // openDodsBinaryFile method uses Google Truth
    DatasetUrlExamples.openDodsBinaryFile(dodsFile.toString());
  }

  @AfterClass
  public static void cleanup() {
    for (File tempFile : tempFiles) {
      tempFile.delete();
    }
  }

}
