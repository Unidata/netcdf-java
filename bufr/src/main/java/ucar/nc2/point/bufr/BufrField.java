/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.point.bufr;

import ucar.nc2.ft.point.bufr.BufrCdmIndexProto;

import java.util.List;

/**
 * Abstraction for BUFR field.
 * Used in writing index, so we can make changes in BufrCdmIndexPanel
 */
public interface BufrField {
  String getName();

  String getDesc();

  String getUnits();

  short getFxy();

  String getFxyName();

  BufrCdmIndexProto.FldAction getAction();

  BufrCdmIndexProto.FldType getType();

  boolean isSeq();

  int getMin();

  int getMax();

  int getScale();

  int getReference();

  int getBitWidth();

  List<? extends BufrField> getChildren();

}
