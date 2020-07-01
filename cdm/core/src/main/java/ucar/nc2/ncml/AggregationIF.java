package ucar.nc2.ncml;

import java.io.IOException;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 10/4/2019.
 * @deprecated do not use
 */
@Deprecated
public interface AggregationIF {

  void close() throws IOException;

  boolean syncExtend() throws IOException;

  // LOOK could also use syncExtend()
  long getLastModified();

  String getFileTypeId();

  String getFileTypeDescription();

  void persistWrite() throws IOException;

  void getDetailInfo(Formatter f);
}
