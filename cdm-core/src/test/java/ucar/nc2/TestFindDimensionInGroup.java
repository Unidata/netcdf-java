/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author cwardgar
 * @since 2015/08/21
 */
public class TestFindDimensionInGroup {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void findDim() {

    Group.Builder rootGroup = Group.builder().setName("");

    Group.Builder subGroup = Group.builder().setName("subGroup");
    rootGroup.addGroup(subGroup);

    Group.Builder subSubGroup = Group.builder().setName("subsubGroup");
    subGroup.addGroup(subSubGroup);

    Dimension dim = new Dimension("dim", 12);
    rootGroup.addDimension(dim);

    Dimension subDim = new Dimension("subDim", 7);
    subGroup.addDimension(subDim);

    Dimension subSubDim = new Dimension("subSubDim", 3);
    subSubGroup.addDimension(subSubDim);

    NetcdfFile ncFile = NetcdfFile.builder().setRootGroup(rootGroup).build();

    /*
     * ncFile looks like:
     * netcdf {
     * dimensions:
     * dim = 12;
     * 
     * group: subGroup {
     * dimensions:
     * subDim = 7;
     * 
     * group: subSubGroup {
     * dimensions:
     * subSubDim = 3;
     * }
     * }
     * }
     */

    System.out.printf("%s%n", ncFile);

    assertThat(dim == ncFile.findDimension("dim")).isTrue();
    assertThat(dim == ncFile.findDimension("/dim")).isTrue();
    assertThat(subDim == ncFile.findDimension("subGroup/subDim")).isTrue();
    assertThat(subDim == ncFile.findDimension("/subGroup/subDim")).isTrue();
    Group ssg = ncFile.findGroup("subGroup/subsubGroup/");
    assertThat(ssg).isNotNull();
    assertThat(subSubDim == ncFile.findDimension("subGroup/subsubGroup/subSubDim")).isTrue();

    assertThat(ncFile.findDimension("subGroup/nonExistentDim")).isNull();
    assertThat(ncFile.findDimension("/subGroup/subDim/")).isNull();
  }
}
