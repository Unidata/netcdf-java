/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.gempak;


/**
 * Class to support the GEMPRM.PRM constants
 */
public interface GempakConstants {

    // Machine types

    /** VAX */
    int MTVAX = 2;

    /** Sun (SPARC) */
    int MTSUN = 3;

    /** Irix */
    int MTIRIS = 4;

    /** APOL */
    int MTAPOL = 5;

    /** IBM */
    int MTIBM = 6;

    /** Intergraph */
    int MTIGPH = 7;

    /** Ultrix */
    int MTULTX = 8;

    /** HP */
    int MTHP = 9;

    /** Alpha */
    int MTALPH = 10;

    /** Linux */
    int MTLNUX = 11;

    /** Integer missing value */
    int IMISSD = -9999;

    /** float missing value */
    float RMISSD = -9999.f;

    /** missing value fuzziness */
    float RDIFFD = 0.1f;

    // Data file types

    /** Surface file type */
    int MFSF = 1;

    /** Sounding File Type */
    int MFSN = 2;

    /** Grid file type */
    int MFGD = 3;

    // Data types

    /** No packing */
    int MDREAL = 1;

    /** Integer packing */
    int MDINTG = 2;

    /** Character packing */
    int MDCHAR = 3;

    /** real packing */
    int MDRPCK = 4;

    /** Grid packing */
    int MDGRID = 5;

    // Grid params

    /** Grid nav block length */
    int LLNNAV = 256;

    /** Grid anl block length */
    int LLNANL = 128;

    /** Max header size */
    int LLSTHL = 20;

    /** Max grid hdr length */
    int LLGDHD = 128;

    // Grid packing types

    /** no packing */
    int MDGNON = 0;

    /** GRIB1 packing */
    int MDGGRB = 1;

    /** NMC packing */
    int MDGNMC = 2;

    /** DIF packing */
    int MDGDIF = 3;

    /** decimal packing? */
    int MDGDEC = 4;

    /** GRIB2 packing */
    int MDGRB2 = 5;

    // DM stuff

    /** row identifier */
    String ROW = "ROW";

    /** column identifier */
    String COL = "COL";

    /** Block size */
    int MBLKSZ = 128;
}

