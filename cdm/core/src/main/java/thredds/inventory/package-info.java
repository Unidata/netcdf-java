/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * NOT PUBLIC API - DO NOT USE: Abstractions for tracking dataset inventory using "managed files".
 * MFile - Abstraction of java.io.File
 * MCollection - An abstract description of a collection of MFile.
 * MController - Something that knows how to obtain the MFiles using a MCollection.
 * Collection - Manages a collection of MFile objects.
 * CollectionManager - Manages a dynamic collection of MFile objects. Allows storing key/value pairs on MFiles
 * CollectionUpdater - singleton class for background updating of Collections.
 * TimePartitionCollections - collections partitioned by time range
 */
package thredds.inventory;
