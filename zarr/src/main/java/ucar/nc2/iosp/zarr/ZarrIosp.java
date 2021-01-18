package ucar.nc2.iosp.zarr;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

public class ZarrIosp extends AbstractIOServiceProvider {
  private static final String fileTypeId = "Zarr";
  private static final String fileTypeDescription = "Zarr file";

  private RandomAccessFile raf;

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    this.raf = raf;
    raf.seek(0);

    return false;
  }

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return null;
  }

  @Override
  public String getFileTypeId() {
    return fileTypeId;
  }

  @Override
  public String getFileTypeDescription() {
    return fileTypeDescription;
  }
}
