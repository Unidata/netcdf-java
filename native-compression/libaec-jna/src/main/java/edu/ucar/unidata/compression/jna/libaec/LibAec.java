/*
 * Copyright (c) 2025-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package edu.ucar.unidata.compression.jna.libaec;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JNA access to libaec. Not a full implementation, just the functions
 * actually used for decoding (and testing). This is a transliteration
 * of the libaec library file include/libaec.h.
 *
 * @author sarms
 * @since 5.7.1
 */

public final class LibAec {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LibAec.class);
  private static final String libName = "aec";

  static {
    try {
      File library = Native.extractFromResourcePath(libName);
      Native.register(library.getAbsolutePath());
      log.debug("Using libaec library from libaec-native.jar");
    } catch (IOException | UnsatisfiedLinkError e) {
      boolean unusableNativeLib = false;
      // The native jar wasn't found on the classpath, OR the native library
      // shipped with the jar is not compatible with the current OS toolchain.
      // Check for the library on the system path.
      if (e instanceof UnsatisfiedLinkError) {
        unusableNativeLib = true;
        log.debug("Error loading libaec from native jar: {}", e.getMessage());
      }
      try {
        Native.register(libName);
        log.debug("Using libaec library from system");
      } catch (UnsatisfiedLinkError ule) {
        String message = unusableNativeLib ? "Usable libaec C library not found. Install libaec on your system."
            : "libaec C library not found. To read this data, include the libaec-native jar in your classpath "
                + "(edu.ucar:libaec-native) or install libaec on your system.";
        log.error(message);
        throw new RuntimeException(message, ule);
      }
    }
  }

  public static class AecStream extends Structure {
    public static AecStream create(int bitsPerSample, int blockSize, int rsi, int flags) {
      AecStream aecStream = new AecStream();
      aecStream.bits_per_sample = bitsPerSample;
      aecStream.block_size = blockSize;
      aecStream.rsi = rsi;
      aecStream.flags = flags;

      return aecStream;
    }

    public void setInputMemory(Memory inputMemory) {
      this.next_in = inputMemory;
      this.avail_in = new SizeT(inputMemory.size());
    }

    public void setOutputMemory(Memory outputMemory) {
      this.next_out = outputMemory;
      this.avail_out = new SizeT(outputMemory.size());
    }

    public Pointer next_in;
    // number of bytes available at next_in
    public SizeT avail_in;

    // total number of input bytes read so far
    public SizeT total_in;

    public Pointer next_out;

    // remaining free space at next_out
    public SizeT avail_out;

    // total number of bytes output so far
    public SizeT total_out;

    // resolution in bits per sample (n = 1, ..., 32)
    public int bits_per_sample;

    // block size in samples
    public int block_size;

    // Reference sample interval, the number of blocks
    // between consecutive reference samples (up to 4096)
    public int rsi;

    public int flags;

    public volatile PointerByReference state;

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("next_in", "avail_in", "total_in", "next_out", "avail_out", "total_out", "bits_per_sample",
          "block_size", "rsi", "flags", "state");
    }
  }

  // Sample data description flags

  // Samples are signed. Telling libaec this results in a slightly
  // better compression ratio. Default is unsigned.
  static final int AEC_DATA_SIGNED = 1;

  // 24 bit samples are coded in 3 bytes
  static final int AEC_DATA_3BYTE = 2;

  // Samples are stored with their most significant bit first. This has
  // nothing to do with the endianness of the host. Default is LSB.
  static final int AEC_DATA_MSB = 4;

  // Set if preprocessor should be used
  static final int AEC_DATA_PREPROCESS = 8;

  // Use restricted set of code options
  static final int AEC_RESTRICTED = 16;


  // Pad RSI to byte boundary. Only used for decoding some CCSDS sample
  // data. Do not use this to produce new data as it violates the
  // standard.
  static final int AEC_PAD_RSI = 32;

  // Do not enforce standard regarding legal block sizes.
  static final int AEC_NOT_ENFORCE = 64;

  // Return codes of library functions

  public static final int AEC_OK = 0;
  public static final int AEC_CONF_ERROR = (-1);
  public static final int AEC_STREAM_ERROR = (-2);
  public static final int AEC_DATA_ERROR = (-3);
  public static final int AEC_MEM_ERROR = (-4);
  public static final int AEC_RSI_OFFSETS_ERROR = (-5);

  // Options for flushing

  // Do not enforce output flushing. More input may be provided with
  // later calls. So far only relevant for encoding.
  public static final int AEC_NO_FLUSH = 0;

  // Flush output and end encoding. The last call to aec_encode() must
  // set AEC_FLUSH to drain all output.
  // It is not possible to continue encoding of the same stream after it
  // has been flushed. For one, the last block may be padded zeros after
  // preprocessing. Secondly, the last encoded byte may be padded with
  // fill bits.
  public static final int AEC_FLUSH = 1;

  // Declare native methods corresponding to the functions in libaec.h
  // package private - for round trip testing only
  static native int aec_buffer_encode(AecStream strm);

  public static native int aec_buffer_decode(AecStream strm);
}
