/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.partition;

import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import java.io.IOException;

/**
 * Manages time partitions
 *
 * @author caron
 * @since 11/11/13
 */
public interface PartitionManager extends MCollection {

  Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException;

  void removePartition(MCollection partition);

}
