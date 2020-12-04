/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import software.amazon.awssdk.regions.Region;

public class S3TestsCommon {
  public static final String AWS_REGION_PROP_NAME = "aws.region";
  public static final String AWS_G16_REGION = Region.US_EAST_1.toString();

  public static final String TOP_LEVEL_AWS_BUCKET = "cdms3:noaa-goes16";
  public static final String TOP_LEVEL_GCS_BUCKET = "cdms3://storage.googleapis.com/gcp-public-data-goes-16";
  public static final String TOP_LEVEL_OSDC_BUCKET =
      "cdms3://griffin-objstore.opensciencedatacloud.org/noaa-goes16-hurricane-archive-2017";
}
