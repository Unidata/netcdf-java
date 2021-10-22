/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import javax.annotation.concurrent.Immutable;

import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.util.CancelTask;
import java.io.IOException;

/**
 * A ProxyReader for logical sections of a Variable.
 * 
 * {@link Variable#section(Section)}
 */
@Immutable
class SectionReader implements ProxyReader {
  private final Section orgSection; // section of the original
  private final Variable orgClient;

  // section must be filled
  SectionReader(Variable orgClient, Section section) {
    this.orgClient = orgClient;
    this.orgSection = section;
  }

  @Override
  public Array proxyReadArray(Variable client, CancelTask cancelTask) throws IOException {
    try {
      return orgClient._read(orgSection);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Array proxyReadArray(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    Section want = orgSection.compose(section);
    return orgClient._read(want);
  }

}
