/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.array.ArraysConvert;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.util.CancelTask; // ??
import java.io.IOException;

/** Reader of the data for a Variable. */
public interface ProxyReader {
  /**
   * Read all the data for a Variable.
   * 
   * @param client the client Variable
   * @param cancelTask user may cancel
   * @return memory resident Array containing the data. Will have same shape as the Variable.
   * @deprecated use proxyReadArray()
   */
  @Deprecated
  default ucar.ma2.Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    return ArraysConvert.convertFromArray(proxyReadArray(client, cancelTask));
  }

  /**
   * Read a section of the data for a Variable.
   *
   * @param client the client Variable
   * @param oldSection the section of data to read.
   * @param cancelTask user may cancel
   *
   * @return memory resident Array containing the data. Will have same shape as the Section.
   * @throws ucar.ma2.InvalidRangeException if section has incorrect rank or illegal shape.
   * @deprecated use proxyReadArray()
   */
  @Deprecated
  default ucar.ma2.Array reallyRead(Variable client, ucar.ma2.Section oldSection, CancelTask cancelTask)
      throws IOException, ucar.ma2.InvalidRangeException {
    try {
      Section section = ArraysConvert.convertSection(oldSection);
      return ArraysConvert.convertFromArray(proxyReadArray(client, section, cancelTask));
    } catch (InvalidRangeException e) {
      throw new ucar.ma2.InvalidRangeException(e.getMessage());
    }
  }

  /** Read all the data for a Variable, returning ucar.array.Array. */
  ucar.array.Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException;

  /** Read a section of the data for a Variable, returning ucar.array.Array. */
  ucar.array.Array<?> proxyReadArray(Variable client, ucar.array.Section section, CancelTask cancelTask)
      throws IOException, ucar.array.InvalidRangeException;

}
