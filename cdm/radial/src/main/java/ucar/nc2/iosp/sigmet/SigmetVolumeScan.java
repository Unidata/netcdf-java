/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.sigmet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author yuanho
 * @since Apr 7, 2010
 */
public class SigmetVolumeScan {
  private static Logger logger = LoggerFactory.getLogger(SigmetVolumeScan.class);
  /**
   * See IRIS-Programming-Guide-M211318EN: 4.9 Constants
   * empty or undefined types are set to their index number to avoid duplicates
   * if suffix is "_2" it means a two byte value instead of just one byte
   * (2 bytes are experimental at this point - didn't have a file to test)
   */
  public static final String[] data_name = {"ExtendedHeaders", // ? bytes
      "TotalPower", "Reflectivity", "Velocity", "Width", "DifferentialReflectivity", "[6]", "CorrectedReflectivity", // 1
                                                                                                                     // byte
      "TotalPower_2", "Reflectivity_2", "Velocity_2", "Width_2", "DifferentialReflectivity_2", // 2 bytes
      "RainfallRate_2" /* 2 bytes */, "KDPDifferentialPhase" /* 1 byte */, "KDPDifferentialPhase_2" /* 2 bytes */,
      "PhiDPDifferentialPhase" /* 1 byte */, "CorrectedVelocity" /* 1 byte */, "SQI" /* 1 byte */, "RhoHV" /* 1 byte */,
      "RhoHV_2" /* 2 bytes */, "CorrectedReflectivity_2" /* 2 bytes */, "CorrectedVelocity_2" /* 2 bytes */,
      "SQI_2" /* 2 bytes */, "PhiDPDifferentialPhase_2" /* 2 bytes */, "LDRH" /* 1 byte */, "LDRH_2" /* 2 bytes */,
      "LDRV" /* 1 byte */, "LDRV_2" /* 2 bytes */, "[29]", "[30]", "[31]", "Height" /* 1 byte */,
      "LinearLiquid_2" /* 2 bytes */, "RawData" /* ? */, "WindShear" /* 1 byte */, "Divergence_2" /* 2 bytes */,
      "FloatedLiquid_2" /* 2 bytes */, "UserType" /* 1 byte */, "UnspecifiedData" /* 1 byte */,
      "Deformation_2" /* 2 bytes */, "VerticalVelocity_2" /* 2 bytes */, "HorizontalVelocity_2" /* 2 bytes */,
      "HorizontalWindDirection_2" /* 2 bytes */, "AxisOfDilatation_2" /* 2 bytes */, "TimeInSeconds_2" /* 2 bytes */,
      "RHOH" /* 1 byte */, "RHOH_2" /* 2 bytes */, "RHOV" /* 1 byte */, "RHOV_2" /* 2 bytes */, "PHIH" /* 1 byte */,
      "PHIH_2" /* 2 bytes */, "PHIV" /* 1 byte */, "PHIV_2" /* 2 bytes */, "UserType_2" /* 2 bytes */,
      "HydrometeorClass" /* 1 byte */, "HydrometeorClass_2" /* 2 bytes */,
      "CorrectedDifferentialReflectivity" /* 1 byte */, "CorrectedDifferentialReflectivity_2" /* 2 bytes */,
      // 16 empty
      "[59]", "[60]", "[61]", "[62]", "[63]", "[64]", "[65]", "[66]", "[67]", "[68]", "[69]", "[70]", "[71]", "[72]",
      "[73]", "[74]", "PolarimetricMeteoIndex" /* 1 byte */, "PolarimetricMeteoIndex_2" /* 2 bytes */,
      "LOG8" /* 1 byte */, "LOG16_2" /* 2 bytes */, "CSP8" /* 1 byte */, "CSP16_2" /* 2 bytes */, "CCOR8" /* 1 byte */,
      "CCOR16_2" /* 2 bytes */, "AH8" /* 1 byte */, "AH16_2" /* 2 bytes */, "AV8" /* 1 byte */, "AV16_2" /* 2 bytes */,
      "AZDR8" /* 1 byte */, "AZDR16_2" /* 2 bytes */,};

  /*
   * Some units are unknown, some were correlated and assumed by ChatGPT.
   * Not all might be correct!
   * Just extending the initial list here from previous version of SigmetIOServiceProvider
   * // TODO fill and double check
   */
  public static final String[] data_unit = {"?", // DB_XHDR: Extended Headers
      "dBm", // DB_DBT: Total Power (1 byte)
      "dBZ", // DB_DBZ: Reflectivity (1 byte)
      "m/s", // DB_VEL: Velocity (1 byte)
      "m/s", // DB_WIDTH: Width (1 byte)
      "dB", // DB_ZDR: Differential Reflectivity (1 byte)
      "?", // empty
      "dBZ", // DB_DBZC: Corrected Reflectivity (1 byte)
      "dBm", // DB_DBT2: Total Power (2 byte)
      "dBZ", // DB_DBZ2: Reflectivity (2 byte)
      "m/s", // DB_VEL2: Velocity (2 byte)
      "m/s", // DB_WIDTH2: Width (2 byte)
      "dB", // DB_ZDR2: Differential Reflectivity (2 byte)
      "mm/hr", // DB_RAINRATE2: Rainfall Rate (2 byte)
      "°/km", // DB_KDP: KDP (Differential Phase) (1 byte)
      "°/km", // DB_KDP2: KDP (Differential Phase) (2 byte)
      "°", // DB_PHIDP: PhiDP (Differential Phase) (1 byte)
      "m/s", // DB_VELC: Corrected Velocity (1 byte)
      "?", // DB_SQI: SQI (Signal Quality Index) (1 byte)
      "?", // DB_RHOHV: RhoHV (1 byte)
      "?", // DB_RHOHV2: RhoHV (2 byte)
      "dBZ", // DB_DBZC2: Corrected Reflectivity (2 byte)
      "m/s", // DB_VELC2: Corrected Velocity (2 byte)
      "?", // DB_SQI2: SQI (Signal Quality Index) (2 byte)
      "°", // DB_PHIDP2: PhiDP (Differential Phase) (2 byte)
      "?", // DB_LDRH: LDR xmt H rcv V (1 byte)
      "?", // DB_LDRH2: LDR xmt H rcv V (2 byte)
      "?", // DB_LDRV: LDR xmt V rcv H (1 byte)
      "?", // DB_LDRV2: LDR xmt V rcv H (2 byte)
      // 3 empty
      "?", "?", "?", "1/10 km", // DB_HEIGHT: Height (1/10 km) (1 byte)
      ".001mm", // DB_VIL2: Linear liquid (.001mm) (2 byte)
      "?", // DB_RAW: Unknown unit or unitless (Raw Data)
      "m/s", // DB_SHEAR: Shear (Velocity difference)
      "?", // DB_DIVERGE2: Divergence (2 byte)
      "mm", // DB_FLIQUID2: Liquid equivalent (2 byte)
      "?", // DB_USER: User-defined (unit depends on definition)
      "?", // DB_OTHER: Other data type (unit depends on definition)
      "1/s", // DB_DEFORM2: Deformation (2 byte)
      "m/s", // DB_VVEL2: Vertical Velocity (2 byte)
      "m/s", // DB_HVEL2: Horizontal Velocity (2 byte)
      "°", // DB_HDIR2: Horizontal Direction (2 byte)
      "1/s", // DB_AXDIL2: Axis of Dilation (2 byte)
      "s", // DB_TIME2: Time (2 byte)
      "?", // DB_RHOH: RhoH (1 byte)
      "?", // DB_RHOH2: RhoH (2 byte)
      "?", // DB_RHOV: RhoV (1 byte)
      "?", // DB_RHOV2: RhoV (2 byte)
      "°", // DB_PHIH: PhiH (1 byte)
      "°", // DB_PHIH2: PhiH (2 byte)
      "°", // DB_PHIV: PhiV (1 byte)
      "°", // DB_PHIV2: PhiV (2 byte)
      "?", // DB_USER2: User-defined (2 byte)
      "?", // DB_HCLASS: Hydrometeor Classification (1 byte)
      "?", // DB_HCLASS2: Hydrometeor Classification (2 byte)
      "dB", // DB_ZDRC: Corrected Differential Reflectivity (1 byte)
      "dB", // DB_ZDRC2: Corrected Differential Reflectivity (2 byte)
      // 16 empty
      "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", "?", // PolarimetricMeteoIndex (1
                                                                                           // byte)
      "?", // PolarimetricMeteoIndex2 (2 bytes)
      "?", // LOG8 (1 byte)
      "?", // LOG16 (2 bytes)
      "?", // CSP8 (1 byte)
      "?", // CSP16 (2 bytes)
      "?", // CCOR8 (1 byte)
      "?", // CCOR16 (2 bytes)
      "?", // AH8 (1 byte)
      "?", // AH16 (2 bytes)
      "?", // AV8 (1 byte)
      "?", // AV16 (2 bytes)
      "?", // AZDR8 (1 byte)
      "?", // AZDR16 (2 bytes)
  };

  public static final int REC_SIZE = 6144;

  public static final float MISSING_VALUE_FLOAT = -999.99f;
  public static final double MISSING_VALUE_DOUBLE = -999.99;
  public static final byte MISSING_VALUE_BYTE = 0;

  public static final ByteBuffer MISSING_VALUE_BYTE_ARRAY_BB = ByteBuffer.wrap(new byte[] {MISSING_VALUE_BYTE});
  public static final Array MISSING_VALUE_BYTE_ARRAY =
      Array.factoryConstant(DataType.OPAQUE, new int[] {1}, new ByteBuffer[] {MISSING_VALUE_BYTE_ARRAY_BB});

  private HashMap<String, List<List<Ray>>> allGroups = new HashMap<>();

  private short[] data_type;
  private int[] num_gates;
  public int[] base_time;
  public short[] year;
  public short[] month;
  public short[] day;
  public Ray firstRay;
  public Ray lastRay;
  public RandomAccessFile raf;

  /**
   * Read all the values from SIGMET-IRIS file which are necessary to fill in the ncfile.
   *
   */
  SigmetVolumeScan(RandomAccessFile raf) throws java.io.IOException {
    int len = 12288; // ---- Read from the 3d record----------- 6144*2=12288
    short nrec = 0, nsweep = 1, nray = 0, byteoff = 0;
    int nwords, end_words, data_read = 0, num_zero, rays_count = 0, nb = 0, posInRay_relative = 0,
        posInRay_absolute = 0, pos_ray_hdr = 0, t = 0;
    short a0, a00, dty;
    short beg_az = 0, beg_elev = 0, end_az = 0, end_elev = 0, num_bins = 0;
    // int because UINT2 data type (Unsigned 16-bit integer)
    int time_start_sw = 0;
    float az, elev, d = 0.0f, step;
    // byte data = 0;
    boolean beg_rec = true, end_rec = true, read_ray_hdr = true, begin = true;
    int cur_len, beg = 1, kk, col = 0, nu = 0, bt0 = 0, bt1 = 0;
    int start_sweep = 1, end_sweep = 1, start_ray = 1, end_ray = 1;

    // Input
    this.raf = raf;
    raf.order(RandomAccessFile.LITTLE_ENDIAN);

    int fileLength = (int) raf.length();
    Map<String, Number> recHdr = SigmetIOServiceProvider.readRecordsHdr(raf);
    int nparams = recHdr.get("nparams").intValue();
    short number_sweeps = recHdr.get("number_sweeps").shortValue();

    short num_rays = recHdr.get("num_rays").shortValue();

    int range_1st = recHdr.get("range_first").intValue();
    float range_first = range_1st * 0.01f;
    int stepp = recHdr.get("range_last").intValue();

    float range_last = stepp * 0.01f;
    short bins = recHdr.get("bins").shortValue();

    short[] num_sweep = new short[nparams];
    short[] num_rays_swp = new short[nparams];
    short[] indx_1ray = new short[nparams];
    short[] num_rays_exp = new short[nparams];
    short[] num_rays_act = new short[nparams];
    short[] angl_swp = new short[nparams];
    short[] bin_len = new short[nparams];
    data_type = new short[nparams];
    // float[] dd = new float[bins];
    num_gates = new int[number_sweeps];
    base_time = new int[nparams * number_sweeps];
    year = new short[nparams * number_sweeps];
    month = new short[nparams * number_sweeps];
    day = new short[nparams * number_sweeps];
    // Array of Ray objects is 2D. Number of columns=number of rays
    // Number of raws = number of types of data if number_sweeps=1,
    // or number of raws = number_sweeps
    HashMap<String, List<Ray>> all = new HashMap<>();

    int irays = (int) num_rays;
    Ray ray = null;
    // init array
    float[] val = new float[bins];

    while (len < fileLength) {
      int rayoffset = 0;
      int rayoffset1;
      int datalen = 0;

      cur_len = len;

      if (nsweep == number_sweeps & rays_count == beg) {
        return;
      }

      if (beg_rec) {

        // --- <raw_prod_bhdr> 12bytes -----------
        raf.seek(cur_len);
        nrec = raf.readShort(); // cur_len
        nsweep = raf.readShort(); // cur_len+2
        byteoff = raf.readShort();
        len = len + 2; // cur_len+4
        nray = raf.readShort();

        // ---- end of <raw_prod_bhdr> -------------
        cur_len = cur_len + 12;
        beg_rec = false;
      }

      if ((nsweep <= number_sweeps) & (rays_count % beg == 0)) {

        // --Read <ingest_data_hdrs> Number of them=nparams*number_sweeps -----
        // ---Len of <ingest_data_hdr>=76 bytes -----------------
        beg = 0;

        for (int i = 0; i < nparams; i++) {
          int idh_len = cur_len + 12 + i * 76; // + 12 == skipping over <structure_header>

          /*
           * debug structure_header
           * raf.seek(idh_len-12);
           * // Structure identifier
           * short si = raf.readShort();
           * raf.readShort(); // format version
           * int nob = raf.readInt();
           * if (si != (short)24)
           * throw new IllegalStateException("not a Ingest_header");
           * raf.readShort(); // reserved
           * raf.readShort(); // flags
           */

          raf.seek(idh_len);

          // Read seconds since midnight
          base_time[nu] = raf.readInt(); // idh_len
          raf.skipBytes(2);
          year[nu] = raf.readShort(); // idh_len+6
          month[nu] = raf.readShort(); // idh_len+8
          day[nu] = raf.readShort(); // idh_len+10
          nu++;
          num_sweep[i] = raf.readShort(); // idh_len+12
          num_rays_swp[i] = raf.readShort(); // idh_len+14
          indx_1ray[i] = raf.readShort(); // idh_len+16
          num_rays_exp[i] = raf.readShort();
          beg += num_rays_exp[i]; // before num_rays_act[i] was used but it does seem not work
          num_rays_act[i] = raf.readShort();
          angl_swp[i] = raf.readShort(); // idh_len+22
          bin_len[i] = raf.readShort(); // idh_len+24 (Number of bits per bin for this data type)
          data_type[i] = raf.readShort(); // idh_len+26
        }

        cur_len = cur_len + nparams * 76;
      }

      if (end_rec) {

        // --- Read compression code=2 bytes from cur_len
        raf.seek(cur_len);
        a0 = raf.readShort();
        cur_len = cur_len + 2;

        // --- Check if the code=1 ("1" means an end of a ray)
        if (a0 == (short) 1) {
          if (cur_len % REC_SIZE == 0) {
            beg_rec = true;
            end_rec = true;
            rays_count++;
            read_ray_hdr = true;
            posInRay_relative = 0;
            posInRay_absolute = 0;
            data_read = 0;
            nb = 0;
            len = cur_len;
          } else {
            end_rec = true;
            len = cur_len;
            rays_count++;
          }

          continue;
        }

        nwords = a0 & 0x7fff;
        end_words = nwords - 6; // because of raw_prod_bhdr 12-byte structure
        data_read = end_words * 2;
        end_rec = false;

        if (cur_len % REC_SIZE == 0) {
          len = cur_len;
          read_ray_hdr = true;
          beg_rec = true;

          continue;
        }
      }

      len = cur_len;

      // ---Define output data files for each data_type (= nparams)/sweep ---------
      dty = data_type[0];
      int bitsToRead = bin_len[0];

      if (nparams > 1) {
        kk = rays_count % nparams;
        dty = data_type[kk];
        bitsToRead = bin_len[kk];

      } else if (number_sweeps > 1) {
      }

      if (bitsToRead <= 0 || bitsToRead % 8 != 0)
        throw new IllegalStateException("Not compatible! Number of bits per bin for this data type = " + bitsToRead);
      int bytesToRead = bitsToRead / 8;

      String var_name = data_name[dty];

      // --- read ray_header (size=12 bytes=6 words)---------------------------------------
      if (read_ray_hdr) {
        if (ray != null)
          throw new IllegalStateException("ray != null");

        if (pos_ray_hdr < 2) {
          raf.seek(cur_len);
          beg_az = raf.readShort();
          cur_len = cur_len + 2;
          len = cur_len;

          if (cur_len % REC_SIZE == 0) {
            pos_ray_hdr = 2;
            beg_rec = true;
            read_ray_hdr = true;

            continue;
          }
        }
        if (pos_ray_hdr < 4) {
          raf.seek(cur_len);
          beg_elev = raf.readShort();
          cur_len = cur_len + 2;
          len = cur_len;

          if (cur_len % REC_SIZE == 0) {
            pos_ray_hdr = 4;
            beg_rec = true;
            read_ray_hdr = true;
            continue;
          }
        }
        if (pos_ray_hdr < 6) {
          raf.seek(cur_len);
          end_az = raf.readShort();
          cur_len = cur_len + 2;
          len = cur_len;

          if (cur_len % REC_SIZE == 0) {
            pos_ray_hdr = 6;
            beg_rec = true;
            read_ray_hdr = true;
            continue;
          }
        }
        if (pos_ray_hdr < 8) {
          raf.seek(cur_len);
          end_elev = raf.readShort();
          cur_len = cur_len + 2;
          len = cur_len;

          if (cur_len % REC_SIZE == 0) {
            pos_ray_hdr = 8;
            beg_rec = true;
            read_ray_hdr = true;

            continue;
          }
        }

        if (pos_ray_hdr < 10) {
          raf.seek(cur_len);
          num_bins = raf.readShort();
          cur_len = cur_len + 2;
          len = cur_len;

          if (num_bins % 2 != 0) {
            num_bins = (short) (num_bins + 1);
          }
          num_gates[nsweep - 1] = (int) num_bins;
          if (cur_len % REC_SIZE == 0) {
            pos_ray_hdr = 10;
            beg_rec = true;
            read_ray_hdr = true;

            continue;
          }
        }

        if (pos_ray_hdr < 12) {
          raf.seek(cur_len);
          time_start_sw = raf.readUnsignedShort();
          cur_len = cur_len + 2;
          len = cur_len;
        }
      }

      // ---------- end of ray header ----------------------------------------------
      az = SigmetIOServiceProvider.calcAz(beg_az, end_az);
      elev = SigmetIOServiceProvider.calcElev(end_elev);
      step = SigmetIOServiceProvider.calcStep(range_first, range_last, num_bins);

      if (cur_len % REC_SIZE == 0) {
        len = cur_len;
        beg_rec = true;
        read_ray_hdr = false;

        continue;
      }

      if (posInRay_relative > 0) {
        data_read = data_read - posInRay_relative;
        posInRay_relative = 0;
      }

      if (data_read > 0) {
        raf.seek(cur_len);
        rayoffset = cur_len;
        datalen = data_read;

        for (int i = 0; i < data_read; i++) {
          // data = raf.readByte();
          // dd[nb] = SigmetIOServiceProvider.calcData(recHdr, dty, data);
          cur_len++;
          posInRay_absolute++;
          if (posInRay_absolute % bytesToRead == 0)
            nb++;

          if (cur_len % REC_SIZE == 0) {
            posInRay_relative = i + 1;
            beg_rec = true;
            read_ray_hdr = false;
            len = cur_len;
            raf.seek(cur_len);
            break;
          }
        }
        raf.seek(cur_len);
        if (posInRay_relative > 0) {
          continue;
        }
      }

      if (cur_len % REC_SIZE == 0) {
        posInRay_relative = 0;
        beg_rec = true;
        read_ray_hdr = false;
        data_read = 0;
        len = cur_len;

        continue;
      }

      raf.seek(cur_len);
      rayoffset1 = cur_len;

      while (nb < (int) num_bins) {
        a00 = raf.readShort();
        cur_len = cur_len + 2;

        // --- Check if the code=1 ("1" means an end of a ray)
        if (a00 == (short) 1) {
          ray = new Ray(range_first, step, az, elev, num_bins, (short) nb, time_start_sw, rayoffset, datalen,
              rayoffset1, nsweep, var_name, dty, bytesToRead);
          rays_count++;
          beg_rec = false;
          end_rec = true;

          break;
        }

        if (a00 < 0) { // -- This is data
          nwords = a00 & 0x7fff;
          data_read = nwords * 2;

          if (cur_len % REC_SIZE == 0) {
            posInRay_relative = 0;
            beg_rec = true;
            end_rec = false;
            read_ray_hdr = false;

            break;
          }

          raf.seek(cur_len);

          for (int ii = 0; ii < data_read; ii++) {
            // data = raf.readByte();
            // dd[nb] = SigmetIOServiceProvider.calcData(recHdr, dty, data);
            cur_len++;
            posInRay_absolute++;
            if (posInRay_absolute % bytesToRead == 0)
              nb++;

            if (cur_len % REC_SIZE == 0) {
              posInRay_relative = ii + 1;
              beg_rec = true;
              end_rec = false;
              read_ray_hdr = false;
              raf.seek(cur_len);
              break;
            }
          }
          raf.seek(cur_len);
          if (posInRay_relative > 0) {
            break;
          }
        } else if (a00 > 0 & a00 != 1) {
          num_zero = a00 * 2;

          // for (int k = 0; k < num_zero; k++) {
          // dd[nb + k] = SigmetIOServiceProvider.calcData(recHdr, dty, (byte) 0);
          // }

          int nb_before = posInRay_absolute / bytesToRead;
          // sanity check
          if (nb_before != nb)
            throw new IllegalStateException("nb_before != nb");
          posInRay_absolute += num_zero;
          int nb_after = posInRay_absolute / bytesToRead;
          nb = nb + (nb_after - nb_before);

          if (cur_len % REC_SIZE == 0) {
            beg_rec = true;
            end_rec = false;
            read_ray_hdr = false;
            posInRay_relative = 0;
            data_read = 0;

            break;
          }
        }
      } // ------ end of while for num_bins---------------------------------

      if (cur_len % REC_SIZE == 0) {
        len = cur_len;

        // will get lost otherwise
        if (ray != null) {
          beg_rec = true;
          end_rec = true;
          read_ray_hdr = true;
          posInRay_relative = 0;
          posInRay_absolute = 0;
          data_read = 0;
          nb = 0;
          len = cur_len;

          if (ray == null)
            throw new IllegalStateException("ray == null");

          // using a universal structure that works for all data types
          List<Ray> varL = all.get(var_name.trim());
          if (varL == null) {
            varL = new ArrayList<>();
            all.put(var_name.trim(), varL);
          }
          varL.add(ray);
          lastRay = ray;
          ray = null;
        }

        continue;
      }

      raf.seek(cur_len);

      if (nb == (int) num_bins) {
        a00 = raf.readShort(); // should be 1 == end of ray
        if (a00 != 1)
          // should not be able to get here
          logger.warn("nb == num_bins but a00 != 1 - something seems wrong");
        cur_len = cur_len + 2;
        end_rec = true;
        ray = new Ray(range_first, step, az, elev, num_bins, (short) nb, time_start_sw, rayoffset, datalen, rayoffset1,
            nsweep, var_name, dty, bytesToRead);
        rays_count++;

        // last ray of last sweep -> end of file
        if ((nsweep == number_sweeps) & (rays_count % beg == 0)) {
          if (ray == null)
            throw new IllegalStateException("ray == null");

          // using a universal structure that works for all data types
          List<Ray> varL = all.get(var_name.trim());
          if (varL == null) {
            varL = new ArrayList<>();
            all.put(var_name.trim(), varL);
          }
          varL.add(ray);
          lastRay = ray;
          ray = null;
          break;
        }

        if (cur_len % REC_SIZE == 0) {
          beg_rec = true;
          end_rec = true;
          read_ray_hdr = true;
          posInRay_relative = 0;
          posInRay_absolute = 0;
          data_read = 0;
          nb = 0;
          len = cur_len;

          if (ray == null)
            throw new IllegalStateException("ray == null");

          // using a universal structure that works for all data types
          List<Ray> varL = all.get(var_name.trim());
          if (varL == null) {
            varL = new ArrayList<>();
            all.put(var_name.trim(), varL);
          }
          varL.add(ray);
          lastRay = ray;
          ray = null;

          continue;
        }
      }

      if (firstRay == null)
        firstRay = ray;

      if (ray == null)
        throw new IllegalStateException("ray == null");

      // using a universal structure that works for all data types
      List<Ray> additionalL = all.get(var_name.trim());
      if (additionalL == null) {
        additionalL = new ArrayList<>();
        all.put(var_name.trim(), additionalL);
      }
      additionalL.add(ray);
      lastRay = ray;
      ray = null;

      posInRay_relative = 0;
      posInRay_absolute = 0;
      data_read = 0;
      nb = 0;
      read_ray_hdr = true;
      pos_ray_hdr = 0;

      if ((nsweep <= number_sweeps) & (rays_count % beg == 0)) {
        beg_rec = true;
        end_rec = true;
        rays_count = 0;
        nb = 0;
        cur_len = REC_SIZE * (nrec + 1);
        read_ray_hdr = true;
      }

      len = cur_len;
    } // ------------end of outer while ---------------

    // using a universal structure that works for all data types
    for (String var_name : all.keySet()) {
      List<Ray> additionalL = all.get(var_name);
      if (!additionalL.isEmpty())
        allGroups.put(var_name, sortScans(var_name, additionalL, 1000));
    }

    // --------- fill all of values in the ncfile ------
  } // ----------- end of doData -----------------------

  private int max_radials;
  private int min_radials = Integer.MAX_VALUE;
  private boolean debugRadials;


  private List<List<Ray>> sortScans(String name, List<Ray> scans, int siz) {

    // now group by elevation_num
    Map<Short, List<Ray>> groupHash = new HashMap<>(siz);

    for (Ray ray : scans) {
      List<Ray> group = groupHash.computeIfAbsent((short) ray.nsweep, k -> new ArrayList<>());

      group.add(ray);
    }

    for (Short aShort : groupHash.keySet()) {
      List<Ray> group = groupHash.get(aShort);
      Ray[] rr = new Ray[group.size()];
      group.toArray(rr);
      checkSort(rr);
    }

    // sort the groups by elevation_num
    List<List<Ray>> groups = new ArrayList<>(groupHash.values());
    groups.sort(new GroupComparator());

    // use the maximum radials
    for (List<Ray> group : groups) {
      max_radials = Math.max(max_radials, group.size());
      min_radials = Math.min(min_radials, group.size());
    }

    if (debugRadials) {
      System.out.println(name + " min_radials= " + min_radials + " max_radials= " + max_radials);

      for (List<Ray> group : groups) {
        Ray lastr = group.get(0);

        for (int j = 1; j < group.size(); j++) {
          Ray r = group.get(j);
          if (r.getTime() < lastr.getTime()) {
            System.out.println(" out of order " + j);
          }
          lastr = r;
        }
      }
    }


    return groups;
  }

  private static class GroupComparator implements Comparator<List<Ray>> {
    public int compare(List<Ray> group1, List<Ray> group2) {
      Ray record1 = group1.get(0);
      Ray record2 = group2.get(0);

      // if (record1.elevation_num != record2.elevation_num)
      return record1.nsweep - record2.nsweep;

      // return record1.cut - record2.cut;
    }
  }

  // using a universal structure that works for all data types
  public List<List<Ray>> getGroup(String name) {
    return allGroups.get(name);
  }

  public short[] getDataTypes() {
    return data_type;
  }

  public int[] getNumberGates() {
    return num_gates;
  }

  public int[] getStartSweep() {
    return base_time;
  }

  /**
   * Sort Ray objects in the same sweep according to the ascended azimuth (from 0 to 360)
   * and time.
   *
   * @param r the array of Ray objects in a sweep. Its length=number_rays
   */
  void checkSort(Ray[] r) {
    int j = 0, n = 0, n1, n2;
    int time1, time2;
    int[] k1 = new int[300];
    int[] k2 = new int[300];
    // define the groups of rays with the same "time". For ex.:
    // group1 - ray[0]={time=1,az=344}, ray[1]={time=1,az=345}, ... ray[11]={time=1,az=359}
    // group2 - ray[12]={time=1,az=0}, ray[13]={time=1,az=1}, ... ray[15]={time=1,az=5}
    // k1- array of begin indx (0,12), k2- array of end indx (11,15)
    for (int i = 0; i < r.length - 1; i++) {
      time1 = r[i].getTime();
      time2 = r[i + 1].getTime();
      if (time1 != time2) {
        k2[j] = i;
        j = j + 1;
        k1[j] = i + 1;
      }
    }
    if (k2[j] < r.length - 1 && j > 0) {
      k1[j] = k2[j - 1] + 1;
      k2[j] = r.length - 1;
    }

    // if different groups have the same value of "time" (may be 2 and more groups) -
    // it1= indx of "k1" of 1st group, it2= indx of "k2" of last group
    int it1 = 0, it2 = 0;
    for (int ii = 0; ii < j + 1; ii++) {
      n1 = k1[ii];
      for (int i = 0; i < j + 1; i++) {
        if (i != ii) {
          n2 = k1[i];
          if (r[n1].getTime() == r[n2].getTime()) {
            it1 = ii;
            it2 = i;
          }
        }
      }
    }

    n1 = k1[it1];
    n2 = k1[it2];
    int s1 = k2[it1] - k1[it1] + 1;
    int s2 = k2[it2] - k1[it2] + 1;
    float[] t0 = new float[s1];
    float[] t00 = new float[s2];
    for (int i = 0; i < s1; i++) {
      t0[i] = r[n1 + i].getAz();
    }
    for (int i = 0; i < s2; i++) {
      t00[i] = r[n2 + i].getAz();
    }
    float mx0 = t0[0];
    for (int i = 0; i < s1; i++) {
      if (mx0 < t0[i])
        mx0 = t0[i];
    }
    float mx00 = t00[0];
    for (int i = 0; i < s2; i++) {
      if (mx00 < t00[i])
        mx00 = t00[i];
    }
    if ((mx0 > 330.0f & mx00 < 50.0f)) {
      for (int i = 0; i < s1; i++) {
        float q = r[n1 + i].getAz();
        r[n1 + i].setAz(q - 360.0f);
      }
    }
    Arrays.sort(r, new RayComparator());
    for (Ray ray : r) {
      float a = ray.getAz();
      if (a < 0 & a > -361.0f) {
        float qa = ray.getAz();
        ray.setAz(qa + 360.0f);
      }
    }

  }

  static class RayComparator implements Comparator<Ray> {
    public int compare(Ray ray1, Ray ray2) {
      if (ray1.getTime() < ray2.getTime()) {
        return -1;
      } else if (ray1.getTime() == ray2.getTime()) {
        if (ray1.getAz() < ray2.getAz()) {
          return -1;
        }
        if (ray1.getAz() > ray2.getAz()) {
          return 1;
        }
        if (ray1.getAz() == ray2.getAz()) {
          return 0;
        }
      } else if (ray1.getTime() > ray2.getTime()) {
        return 1;
      }
      return 0;
    }
  } // class RayComparator end ----------------------------------


}

