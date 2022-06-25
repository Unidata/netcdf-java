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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestCfSubConventionProvider {

  private static final NetcdfDataset.Builder<?> CF_NCDB = NetcdfDataset.builder();
  private static final NetcdfDataset.Builder<?> YOLO_EXACT_NCDB = NetcdfDataset.builder();
  private static final NetcdfDataset.Builder<?> YOLO_PARTIAL_NCDB = NetcdfDataset.builder();

  @BeforeClass
  public static void makeNcdBuilders() throws IOException {
    NetcdfFile.Builder<?> cfNcfb = NetcdfFile.builder();
    NetcdfFile.Builder<?> yoloExactNcfb = NetcdfFile.builder();
    NetcdfFile.Builder<?> yoloPartialNcfb = NetcdfFile.builder();

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

    Group.Builder yoloExactRoot = Group.builder().setName("");
    yoloExactNcfb.setRootGroup(yoloExactRoot);
    // exactly match the convention name
    Attribute yoloExactConvAttr =
        Attribute.builder(CDM.CONVENTIONS).setStringValue(CfSubConvForTest.CONVENTION_NAME).build();
    yoloExactRoot.addAttribute(yoloExactConvAttr);
    try (NetcdfFile yoloExactNcf = yoloExactNcfb.build()) {
      YOLO_EXACT_NCDB.copyFrom(yoloExactNcf);
      YOLO_EXACT_NCDB.setOrgFile(yoloExactNcf);
    } catch (IOException ioe) {
      throw new IOException("Error building NetcdfFile object to mock a CF/YOLO netCDF file for testing.", ioe);
    }

    Group.Builder yoloPartialRoot = Group.builder().setName("");
    yoloPartialNcfb.setRootGroup(yoloPartialRoot);
    // change up the convention name used so that it is not an exact match with the test convention name
    Attribute yoloPartialConvAttr = Attribute.builder(CDM.CONVENTIONS)
        .setStringValue(
            CfSubConvForTest.CONVENTION_NAME.replaceFirst(CfSubConvForTest.CONVENTAION_NAME_STARTS_WITH, "CF-1.200"))
        .build();
    yoloPartialRoot.addAttribute(yoloPartialConvAttr);
    try (NetcdfFile yoloPartialNcf = yoloPartialNcfb.build()) {
      YOLO_PARTIAL_NCDB.copyFrom(yoloPartialNcf);
      YOLO_PARTIAL_NCDB.setOrgFile(yoloPartialNcf);
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

    // both datasets (exact and partial matches of the sub-convention name) should result in the
    // sub-convention being used.
    List<NetcdfDataset.Builder<?>> subConvBuilders = Arrays.asList(YOLO_EXACT_NCDB, YOLO_PARTIAL_NCDB);
    for (NetcdfDataset.Builder<?> subConvBuilder : subConvBuilders) {
      Optional<CoordSystemBuilder> yoloFacOpt = CoordSystemFactory.factory(subConvBuilder, null);
      assertThat(yoloFacOpt.isPresent());
      CoordSystemBuilder yoloFac = yoloFacOpt.get();
      assertThat(yoloFac).isInstanceOf(CfSubConvForTest.class);
      assertThat(yoloFac.getConventionUsed()).isEqualTo(CfSubConvForTest.CONVENTION_NAME);
    }
  }
}
