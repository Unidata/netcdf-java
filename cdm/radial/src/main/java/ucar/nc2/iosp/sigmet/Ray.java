/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.sigmet;

import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Formatter;

/**
 * @author yuanho
 * @since Apr 7, 2010
 */
public class Ray {

  private short bins, bins_actual;
  int dataRead;
  int offset;
  int offset1;
  private float range, step, az, elev;
  // int because UINT2 data type (Unsigned 16-bit integer)
  private int time;
  // private float[] val;
  String varName;
  int nsweep;
  short datatype;
  int bytesPerBin;

  public Ray(float range, float step, float az, float elev, short bins, int time, int offset, int dataRead, int offset1,
      int nsweep, String name, short datatype, int bytesPerBin) {
    this(range, step, az, elev, bins, bins, time, offset, dataRead, offset1, nsweep, name, datatype, 1);
  }

  public Ray(float range, float step, float az, float elev, short bins, short bins_actual, int time, int offset,
      int dataRead, int offset1, int nsweep, String name, short datatype, int bytesPerBin) {

    setRange(range);
    setStep(step);
    setAz(az);
    setElev(elev);
    setBins(bins);
    setBinsActual(bins_actual);
    setTime(time);
    setOffset(offset);
    setDataRead(dataRead);
    setOffset1(offset1);
    // setVal(val);
    setName(name);
    setNsweep(nsweep);
    setDataType(datatype);
    setBytesPerBin(bytesPerBin);
  }

  public short getDataType() {
    return datatype;
  }

  public void setDataType(short datatype) {
    this.datatype = datatype;
  }

  public int getBytesPerBin() {
    return bytesPerBin;
  }

  public void setBytesPerBin(int bytesPerBin) {
    this.bytesPerBin = bytesPerBin;
  }

  public float getRange() {
    return range;
  }

  public void setRange(float range) {
    this.range = range;
  }

  public float getStep() {
    return step;
  }

  public void setStep(float step) {
    this.step = step;
  }

  public int getNsweep() {
    return nsweep;
  }

  public void setNsweep(int nsweep) {
    this.nsweep = nsweep;
  }

  public float getAz() {
    if (az < 0.0f & az > -361.0f) {
      az = 360.0f + az;
    }

    return az;
  }

  public void setAz(float az) {
    this.az = az;
  }

  public float getElev() {
    return elev;
  }

  public void setElev(float elev) {
    this.elev = elev;
  }

  public short getBins() {
    return bins;
  }

  public void setBins(short bins) {
    this.bins = bins;
  }

  public short getBinsActual() {
    return bins_actual;
  }

  public void setBinsActual(short bins_actual) {
    this.bins_actual = bins_actual;
  }

  public int getTime() {
    return time;
  }

  public void setTime(int time) {
    this.time = time;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public int getDataRead() {
    return dataRead;
  }

  public void setDataRead(int dataRead) {
    this.dataRead = dataRead;
  }

  public int getOffset1() {
    return offset1;
  }

  public void setOffset1(int offset1) {
    this.offset1 = offset1;
  }

  public void setName(String name) {
    this.varName = name;
  }

  public String getName() {
    return varName;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof Ray) {
      Ray oo = (Ray) o;

      return (range == oo.range & step == oo.step & az == oo.az & elev == oo.elev & bins == oo.bins
          & bins_actual == oo.bins_actual & time == oo.time & bytesPerBin == oo.bytesPerBin);
    } else {
      return false;
    }
  }

  public int hashCode() {
    return new Float(range).hashCode() + new Float(step).hashCode() + new Float(az).hashCode()
        + new Float(elev).hashCode() + new Short(bins).hashCode() + new Short(bins_actual).hashCode()
        + new Integer(time).hashCode() + new Integer(bytesPerBin).hashCode();

    // val.hashCode();
  }

  public String toString() {
    Formatter sb = new Formatter();

    sb.format("Range=%f Step=%f", range, step);
    if (az > -361.0f & az < 0.0f) {
      az = 360.0f + az;
    }

    sb.format(" Az=%f Elev=%f Bins=%d (%d) Time=%d", az, elev, bins, bins_actual, time);
    // for (int i=0; i<bins; i++) { sb.append(" "+val[i]); }
    return sb.toString();
  }


  /**
   * Read data from this record.
   *
   * @param raf read from this file
   * @param gateRange handles the possible subset of data to return
   * @param ii put the data here
   * @throws IOException on read error
   */
  public void readData(RandomAccessFile raf, Range gateRange, IndexIterator ii) throws IOException {
    // first need to read all the data (because bytesPerBin could be > 1)
    Byte[] dd = new Byte[bins * bytesPerBin];
    int nb = 0;
    short a00;

    short dty = getDataType();

    int posInRay_absolute = 0;

    if (dataRead > 0) {
      raf.seek(offset);

      for (int i = 0; i < dataRead; i++) {
        dd[i] = raf.readByte();
        posInRay_absolute++;
        if (posInRay_absolute % bytesPerBin == 0)
          nb++;
      }
    }
    raf.seek(offset1);
    int cur_len = offset1;

    // this only works for 1 additional record, not for more. Only a theoretical possibility for now.
    // (relevant only for data types with a lot of bytes per bin)
    while (nb < (int) bins) {
      // --- Check if the code=1 ("1" means an end of a ray)
      a00 = raf.readShort();
      cur_len = cur_len + 2;

      // end of ray
      if (a00 == (short) 1) {
        // don't need to do anything as the rest of dd is already null
        break;
      }

      if (a00 < 0) { // -- This is data
        int nwords = a00 & 0x7fff;
        int dataRead1 = nwords * 2;
        boolean breakOuterLoop = false;
        if (cur_len % SigmetVolumeScan.REC_SIZE == 0) {
          break;
        }
        raf.seek(cur_len);
        for (int i = 0; i < dataRead1 && nb < bins; i++) {
          dd[posInRay_absolute] = raf.readByte();
          posInRay_absolute++;
          if (posInRay_absolute % bytesPerBin == 0)
            nb++;

          cur_len++;

          if (cur_len % SigmetVolumeScan.REC_SIZE == 0) {
            breakOuterLoop = true;
            break;
          }
        }
        if (breakOuterLoop) {
          break;
        }

      } else if (a00 > 0 & a00 != 1) {
        int num_zero = a00 * 2;
        int dataRead1 = num_zero;
        for (int k = 0; k < dataRead1 && nb < bins; k++) {
          dd[posInRay_absolute] = 0;
          posInRay_absolute++;
          if (posInRay_absolute % bytesPerBin == 0)
            nb++;

          if (cur_len % SigmetVolumeScan.REC_SIZE == 0) {
            break;
          }
        }
      }

    } // ------ end of while for num_bins---------------------------------

    // only supporting float, double, byte and byte[]/Object for now
    DataType dType = SigmetIOServiceProvider.calcDataType(dty, bytesPerBin);
    switch (dType) {
      case FLOAT:
        for (int gateIdx : gateRange) {
          if (gateIdx >= bins)
            ii.setFloatNext(Float.NaN);
          else if (gateIdx >= bins_actual)
            ii.setFloatNext(SigmetVolumeScan.MISSING_VALUE_FLOAT);
          else {
            int offset = gateIdx * bytesPerBin;
            Byte ddx = dd[offset];
            if (ddx == null)
              ii.setFloatNext(SigmetVolumeScan.MISSING_VALUE_FLOAT);
            else
              ii.setFloatNext(SigmetIOServiceProvider.calcData(SigmetIOServiceProvider.recHdr, dty, ddx));
          }
        }
        break;
      case DOUBLE:
        for (int gateIdx : gateRange) {
          if (gateIdx >= bins)
            ii.setDoubleNext(Double.NaN);
          else if (gateIdx >= bins_actual)
            ii.setDoubleNext(SigmetVolumeScan.MISSING_VALUE_DOUBLE);
          else {
            int offset = gateIdx * bytesPerBin;
            Byte ddx = dd[offset];
            if (ddx == null)
              ii.setDoubleNext(SigmetVolumeScan.MISSING_VALUE_DOUBLE);
            else {
              int ddx2 = dd[offset + 1];
              int rawValue = (ddx & 0xFF) | ((ddx2 & 0xFF) << 8);
              ii.setDoubleNext(SigmetIOServiceProvider.calcData(SigmetIOServiceProvider.recHdr, dty, rawValue));
            }
          }
        }
        break;
      case BYTE:
        for (int gateIdx : gateRange) {
          if (gateIdx >= bins)
            ii.setByteNext(SigmetVolumeScan.MISSING_VALUE_BYTE);
          else if (gateIdx >= bins_actual)
            ii.setByteNext(SigmetVolumeScan.MISSING_VALUE_BYTE);
          else {
            int offset = gateIdx * bytesPerBin;
            Byte ddx = dd[offset];
            if (ddx == null)
              ii.setByteNext(SigmetVolumeScan.MISSING_VALUE_BYTE);
            else
              ii.setByteNext(ddx);
          }
        }
        break;
      default:
        for (int gateIdx : gateRange) {
          if (gateIdx >= bins)
            ii.setObjectNext(SigmetVolumeScan.MISSING_VALUE_BYTE_ARRAY_BB);
          else if (gateIdx >= bins_actual)
            ii.setObjectNext(SigmetVolumeScan.MISSING_VALUE_BYTE_ARRAY_BB);
          else {
            int offset = gateIdx * bytesPerBin;
            Byte ddx = dd[offset];
            if (ddx == null)
              ii.setObjectNext(SigmetVolumeScan.MISSING_VALUE_BYTE_ARRAY_BB);
            else {
              byte[] b = new byte[bytesPerBin];
              for (int i = 0; i < bytesPerBin; i++) {
                ddx = dd[offset + i];
                b[i] = ddx == null ? SigmetVolumeScan.MISSING_VALUE_BYTE : ddx;
              }
              ii.setObjectNext(ByteBuffer.wrap(b));
            }
          }
        }
    }
  } // end of readData
} // class Ray end------------------------------------------


