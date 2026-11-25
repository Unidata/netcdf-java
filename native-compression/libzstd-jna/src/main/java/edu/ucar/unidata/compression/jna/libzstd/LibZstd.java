/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package edu.ucar.unidata.compression.jna.libzstd;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.io.File;
import java.io.IOException;

/**
 * JNA access to the zstd library. Not a full implementation, just the functions used for decoding. This is a
 * partial transliteration of the zstd library file lib/zstd.h.
 * <p>
 * {@code size_t} is mapped to {@code long}, so a 64-bit JVM is required (all platforms in libzstd-native are 64-bit).
 *
 * @since 5.12.0
 */
public final class LibZstd {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LibZstd.class);
  // JNA maps "zstd" to libzstd.so / libzstd.dylib, and to zstd.dll on Windows (the name used by libzstd-native)
  private static final String libName = "zstd";

  // ZSTD_CONTENTSIZE_UNKNOWN (0ULL - 1) and ZSTD_CONTENTSIZE_ERROR (0ULL - 2)
  static final long CONTENTSIZE_UNKNOWN = -1L;
  static final long CONTENTSIZE_ERROR = -2L;

  static {
    if (Native.SIZE_T_SIZE != 8) {
      String message = "libzstd-jna requires a 64-bit JVM (size_t is " + Native.SIZE_T_SIZE + " bytes)";
      log.error(message);
      throw new RuntimeException(message);
    }
    try {
      File library = Native.extractFromResourcePath(libName);
      Native.register(library.getAbsolutePath());
      log.debug("Using zstd library from libzstd-native.jar");
    } catch (IOException | UnsatisfiedLinkError e) {
      boolean unusableNativeLib = false;
      // The native jar wasn't found on the classpath, OR the native library
      // shipped with the jar is not compatible with the current OS toolchain.
      // Check for the library on the system path.
      if (e instanceof UnsatisfiedLinkError) {
        unusableNativeLib = true;
        log.debug("Error loading zstd from native jar: {}", e.getMessage());
      }
      try {
        Native.register(libName);
        log.debug("Using zstd library from system");
      } catch (UnsatisfiedLinkError ule) {
        String message = unusableNativeLib ? "Usable zstd C library not found. Install zstd on your system."
            : "zstd C library not found. To read this data, include the libzstd-native jar in your classpath "
                + "(edu.ucar.unidata:libzstd-native) or install zstd on your system.";
        log.error(message);
        log.debug("Reason: ", ule);
        throw new RuntimeException(message);
      }
    }
  }

  private LibZstd() {}

  /**
   * Decode zstd compressed data, such as one chunk written by the HDF5 Zstandard filter (id 32015, reference
   * implementation {@code H5Zzstd.c} in the HDF Group hdf5_plugins repository and in netCDF-C).
   * <p>
   * The input may hold one or more zstd frames. Each frame must record its decompressed size, which zstd does by
   * default and which the HDF5 filter relies on as well.
   *
   * @param src the compressed bytes
   * @return the decompressed bytes
   * @throws IOException if the input is not valid zstd data, is truncated or corrupt, or does not record its
   *         decompressed size
   */
  public static byte[] decode(byte[] src) throws IOException {
    if (src == null || src.length == 0) {
      throw new IOException("No zstd data to decode");
    }
    try (Memory in = new Memory(src.length)) {
      in.write(0, src, 0, src.length);
      long size = ZSTD_findDecompressedSize(in, src.length);
      if (size == CONTENTSIZE_ERROR) {
        throw new IOException("Not valid zstd data, or truncated (" + src.length + " bytes)");
      }
      if (size == CONTENTSIZE_UNKNOWN) {
        throw new IOException("zstd frame does not record its decompressed size");
      }
      if (size < 0 || size > Integer.MAX_VALUE - 8) {
        throw new IOException("Decompressed size too large for a Java array: " + Long.toUnsignedString(size));
      }
      if (size == 0) {
        return new byte[0];
      }
      try (Memory out = new Memory(size)) {
        long written = ZSTD_decompress(out, size, in, src.length);
        if (ZSTD_isError(written) != 0) {
          throw new IOException("zstd decompression failed: " + ZSTD_getErrorName(written));
        }
        if (written != size) {
          throw new IOException("Decoded " + written + " bytes, expected " + size);
        }
        return out.getByteArray(0, (int) size);
      }
    }
  }

  /** @return the version of the loaded zstd library, for example "1.5.7" */
  public static String version() {
    return ZSTD_versionString();
  }

  // ZSTDLIB_API unsigned long long ZSTD_findDecompressedSize(const void* src, size_t srcSize);
  static native long ZSTD_findDecompressedSize(Pointer src, long srcSize);

  // ZSTDLIB_API size_t ZSTD_decompress(void* dst, size_t dstCapacity, const void* src, size_t compressedSize);
  static native long ZSTD_decompress(Pointer dst, long dstCapacity, Pointer src, long compressedSize);

  // ZSTDLIB_API unsigned ZSTD_isError(size_t code);
  static native int ZSTD_isError(long code);

  // ZSTDLIB_API const char* ZSTD_getErrorName(size_t code);
  static native String ZSTD_getErrorName(long code);

  // ZSTDLIB_API const char* ZSTD_versionString(void);
  static native String ZSTD_versionString();
}
