/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import ucar.nc2.time2.CalendarDate;
import ucar.nc2.time2.CalendarDateFormatter;
import ucar.nc2.time2.CalendarPeriod;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Partition the files by a user-defined time period.
 * The date is extracted from the filename using the non-optional dateExtractor.
 * All filenames must be read into memory at once, then partitioned.
 *
 * @author caron
 * @since 12/23/2014
 */
public class TimePartition extends CollectionPathMatcher implements PartitionManager {
  CalendarPeriod timePeriod;
  CalendarDateFormatter cdf;

  public TimePartition(FeatureCollectionConfig config, CollectionSpecParser specp, org.slf4j.Logger logger) {
    super(config, specp, logger);
    timePeriod = config.timePeriod;
    cdf = factory(timePeriod);
  }

  public static CalendarDateFormatter factory(CalendarPeriod period) {
    switch (period.getField()) {
      case Year:
        return new CalendarDateFormatter("yyyy");
      case Month:
        return new CalendarDateFormatter("yyyy-MM");
      case Day:
        return new CalendarDateFormatter("yyyy-MM-dd");
      case Hour:
        return new CalendarDateFormatter("yyyy-MM-ddTHH");
      default:
        return new CalendarDateFormatter("yyyy-MM-ddTHH:mm:ss");
    }
  }

  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {

    List<MCollection> result = new ArrayList<>();
    CollectionListRange curr = null;
    CalendarDate startDate;
    CalendarDate endDate = null;

    for (MFile mfile : getFilesSorted()) {
      CalendarDate cdate = dateExtractor.getCalendarDate(mfile);
      if (cdate == null)
        continue; // skip - error should be logged
      if ((curr == null) || !endDate.isAfter(cdate)) {
        startDate = cdate.truncate(timePeriod.getField()); // start on a boundary
        endDate = startDate.add(timePeriod);
        String name = collectionName + "-" + cdf.toString(startDate);
        curr = new CollectionListRange(name, root, startDate, endDate, this.logger);
        curr.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, this.config);
        if (!wasRemoved(curr))
          result.add(curr); // skip if in removed list
      }
      curr.addFile(mfile);
    }

    return result;
  }

  /////////////////////////////////////////////////////////////
  private List<String> removed;

  public void removePartition(MCollection partition) {
    if (removed == null)
      removed = new ArrayList<>();
    removed.add(partition.getCollectionName());
  }

  private boolean wasRemoved(MCollection partition) {
    return removed != null && (removed.contains(partition.getCollectionName()));
  }
}
