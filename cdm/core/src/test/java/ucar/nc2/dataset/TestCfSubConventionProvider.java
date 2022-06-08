/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.conv.CfSubConvForTest;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.CoordSystemFactory;
import ucar.nc2.internal.dataset.conv.CF1Convention;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.Optional;

public class TestCfSubConventionProvider {

  private static final NetcdfDataset.Builder<?> CF_NCDB = NetcdfDataset.builder();
  private static final NetcdfDataset.Builder<?> YOLO_NCDB = NetcdfDataset.builder();

  @BeforeClass
  public static void makeNcdBuilders() throws IOException {
    NetcdfFile.Builder<?> cfNcfb = NetcdfFile.builder();
    NetcdfFile.Builder<?> yoloNcfb = NetcdfFile.builder();

    Group.Builder cfRoot = Group.builder().setName("");
    cfNcfb.setRootGroup(cfRoot);
    Attribute cfConvAttr = Attribute.builder(CDM.CONVENTIONS).setStringValue("CF-1.6").build();
    cfRoot.addAttribute(cfConvAttr);
    try (NetcdfFile cfNcf = cfNcfb.build()) {
      CF_NCDB.copyFrom(cfNcf);
      CF_NCDB.setOrgFile(cfNcf);
    } catch (IOException ioe) {
      throw new IOException("Error building NetcdfFile object to mock a CF netCDF file for testing.", ioe);
    }

    Group.Builder yoloRoot = Group.builder().setName("");
    yoloNcfb.setRootGroup(yoloRoot);
    Attribute yoloConvAttr =
        Attribute.builder(CDM.CONVENTIONS).setStringValue(CfSubConvForTest.CONVENTION_NAME).build();
    yoloRoot.addAttribute(yoloConvAttr);
    try (NetcdfFile yoloNcf = yoloNcfb.build()) {
      YOLO_NCDB.copyFrom(yoloNcf);
      YOLO_NCDB.setOrgFile(yoloNcf);
    } catch (IOException ioe) {
      throw new IOException("Error building NetcdfFile object to mock a CF/YOLO netCDF file for testing.", ioe);
    }
  }

  @Test
  public void testCfSubLoadOrder() throws IOException {
    Optional<CoordSystemBuilder> cfFacOpt = CoordSystemFactory.factory(CF_NCDB, null);
    assertThat(cfFacOpt.isPresent());
    CoordSystemBuilder cfFac = cfFacOpt.get();
    assertThat(cfFac).isInstanceOf(CF1Convention.class);

    Optional<CoordSystemBuilder> yoloFacOpt = CoordSystemFactory.factory(YOLO_NCDB, null);
    assertThat(yoloFacOpt.isPresent());
    CoordSystemBuilder yoloFac = yoloFacOpt.get();
    assertThat(yoloFac).isInstanceOf(CfSubConvForTest.class);
    assertThat(yoloFac.getConventionUsed()).isEqualTo(CfSubConvForTest.CONVENTION_NAME);
  }
}
