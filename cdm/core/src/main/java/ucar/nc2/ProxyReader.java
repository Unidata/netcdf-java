/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import ucar.array.ArraysConvert;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.util.CancelTask; // ??
import java.io.IOException;

/** Something that knows how to read the data for a Variable. */
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
  ucar.ma2.Array reallyRead(Variable client, CancelTask cancelTask) throws IOException;

  /**
   * Read a section of the data for a Variable.
   *
   * @param client the client Variable
   * @param section the section of data to read.
   * @param cancelTask user may cancel
   *
   * @return memory resident Array containing the data. Will have same shape as the Section.
   * @throws ucar.ma2.InvalidRangeException if section has incorrect rank or illegal shape.
   * @deprecated use proxyReadArray()
   */
  @Deprecated
  ucar.ma2.Array reallyRead(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException;

  /** Read all the data for a Variable, returning ucar.array.Array. */
  default ucar.array.Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException {
    return ArraysConvert.convertToArray(reallyRead(client, cancelTask));
  }

  /** Read a section of the data for a Variable, returning ucar.array.Array. */
  default ucar.array.Array<?> proxyReadArray(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    return ArraysConvert.convertToArray(reallyRead(client, section, cancelTask));
  }

}
