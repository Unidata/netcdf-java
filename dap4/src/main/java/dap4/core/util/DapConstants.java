/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.util;

import dap4.core.dmr.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * DAP4 Related Constants both client and server side.
 */

public abstract class DapConstants {
  //////////////////////////////////////////////////
  // Constants

  // Default http protocol
  // Default http protocol
  public static final String HTTPSCHEME = "http:";

  // XML Tags for DAP4
  public static final String X_DAP_SERVER = "TDS-5";
  public static final String X_DAP_VERSION = "4.0";
  public static final String X_DMR_VERSION = "1.0";
  public static final String X_DAP_NS = "http://xml.opendap.org/ns/DAP/4.0#";

  // Known dap4 specific query tags
  public static final String CONSTRAINTTAG = "dap4.ce";
  public static final String CHECKSUMTAG = "dap4.checksum"; // May also be in fragment

  // Define the Serialization Constants common to servlet and client

  // Use Bit flags to avoid heavyweight enumset
  public static final int CHUNK_DATA = 0; // bit 0 : value 0
  public static final int CHUNK_END = 1; // bit 0 : value 1
  public static final int CHUNK_ERROR = 2; // bit 1 : value 1
  public static final int CHUNK_LITTLE_ENDIAN = 4; // bit 2: value 1

  // Construct the union of all flags
  public static final int CHUNK_ALL = CHUNK_DATA | CHUNK_ERROR | CHUNK_END | CHUNK_LITTLE_ENDIAN;
  public static final int COUNTSIZE = 8; // databuffer as specified by the DAP4 spec
  public static final int CHECKSUMSIZE = 4; // bytes if CRC32
  public static final String DIGESTER = "CRC32";
  public static final String CHECKSUMATTRNAME = "_DAP4_Checksum_CRC32";
  public static final String LITTLEENDIANATTRNAME = "_DAP4_Little_Endian";
  public static final String CEATTRNAME = "_" + CONSTRAINTTAG;


  //////////////////////////////////////////////////
  // Constants via Enumerations

  public enum ChecksumSource {
    LOCAL, REMOTE
  };

  //////////////////////////////////////////////////
  // Dap4 Annotations

  public static final String DAP4ENDIANTAG = "ucar.littleendian"; // value = 1 | 0
  // public static final String DAP4TESTTAG = "ucar.testing"; // value = NONE|DAP|DMR|ALL

}
