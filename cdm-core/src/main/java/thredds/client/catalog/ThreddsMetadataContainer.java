/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import java.util.List;

/**
 * Container of ThreddsMetadata: Dataset or ThreddsMetadata
 *
 * @author caron
 * @since 1/11/2015
 */
public interface ThreddsMetadataContainer {
  Object getLocalField(String fldName);

  // TODO will be ImmutableList in ver7
  List getLocalFieldAsList(String fldName);
}
