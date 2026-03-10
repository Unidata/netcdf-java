/*
 * Copyright (c) 2025-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package edu.ucar.unidata.compression.jna.libblosc2;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.NativeLongByReference;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JNA access to C-Blosc2. Not a full implementation, just the functions
 * actually used for decoding (and testing). This is a partial transliteration
 * of the C-Blosc2 library file include/blosc2.h.
 *
 * @author sarms
 * @since 5.10.0
 */

public final class LibBlosc2 {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LibBlosc2.class);
  // C-Blosc2 library name on Windows is libblosc2.dll, but JNA does not automatically add
  // "lib" to the name when searching on Windows as it does on other OSs, so we must add
  // it explicitly.
  private static final String libName = System.getProperty("os.name").startsWith("Windows") ? "libblosc2" : "blosc2";

  // manage initialization / destruction of the native library internal state
  private static final AtomicBoolean initialized = new AtomicBoolean(false);
  private static final Object libraryStatusLock = new Object();

  static {
    try {
      File library = Native.extractFromResourcePath(libName);
      Native.register(library.getAbsolutePath());
      log.debug("Using blosc2 library from libblosc2-native.jar");
    } catch (IOException | UnsatisfiedLinkError e) {
      boolean unusableNativeLib = false;
      // The native jar wasn't found on the classpath, OR the native library
      // shipped with the jar is not compatible with the current OS toolchain.
      // Check for the library on the system path.
      if (e instanceof UnsatisfiedLinkError) {
        unusableNativeLib = true;
        log.debug("Error loading libblosc2 from native jar: {}", e.getMessage());
      }
      try {
        Native.register(libName);
        log.debug("Using libblosc2 library from system");
      } catch (UnsatisfiedLinkError ule) {
        String message = unusableNativeLib ? "Usable libblosc2 C library not found. Install libblosc2 on your system."
            : "libblosc2 C library not found. To read this data, include the libblosc2-native jar in your classpath "
                + "(edu.ucar:libblosc2-native) or install libblosc2 on your system.";
        log.error(message);
        throw new RuntimeException(message, ule);
      }
    }
  }

  /**
   * Initialize the Blosc library environment.
   * <p>
   * This method sets up the necessary internal structures and prepares the
   * Blosc2 compression and decompression functionalities for use.
   * It should be called before performing any operations with the library.
   * <p>
   * It is recommended to pair this method with a call to {@link #blosc2_destroy()}
   * to ensure proper cleanup and resource management.
   */
  public static void init() {
    synchronized (libraryStatusLock) {
      if (!initialized.get()) {
        blosc2_init();
        initialized.set(true);
      }
    }
  }

  /**
   * Destroys the Blosc2 library environment and releases all associated resources.
   * <p>
   * This method ensures that any internal structures or resources allocated during
   * the library's initialization are properly cleaned up and released. It should
   * be called after completing all operations with the Blosc2 library to avoid
   * resource leaks.
   * <p>
   * This method is thread-safe. If the library has not been initialized, the
   * method will return without performing any operations.
   */
  public static void destroy() {
    synchronized (libraryStatusLock) {
      if (initialized.get()) {
        blosc2_destroy();
        initialized.set(false);
      }
    }
  }

  public static boolean isInitialized() {
    return initialized.get();
  }

  public static byte[] decode(byte[] src) {
    NativeLongByReference nbytes = new NativeLongByReference();
    NativeLongByReference cbytes = new NativeLongByReference();
    NativeLongByReference blocksize = new NativeLongByReference();
    blosc1_cbuffer_sizes(src, nbytes, cbytes, blocksize);

    Memory decompressed = new Memory(nbytes.getValue().intValue());
    blosc1_decompress(src, decompressed, nbytes.getValue());
    return decompressed.getByteArray(0, nbytes.getValue().intValue());
  }

  // BLOSC_EXPORT void blosc2_init(void);
  static native void blosc2_init();

  // BLOSC_EXPORT void blosc2_init(void);
  static native void blosc2_destroy();

  // BLOSC_EXPORT int blosc1_compress(int clevel, int doshuffle, size_t typesize,
  // size_t nbytes, const void* src, void* dest, size_t destsize);
  static native int blosc1_compress(int clevel, int doshuffle, NativeLong typesize, NativeLong nbytes, Pointer src,
      Pointer dest, NativeLong destsize);

  // BLOSC_EXPORT int blosc1_decompress(const void* src, void* dest, size_t destsize)
  static native int blosc1_decompress(byte[] src, Pointer dest, NativeLong destsize);

  // BLOSC_EXPORT void blosc1_cbuffer_sizes(const void* cbuffer, size_t* nbytes,
  // size_t* cbytes, size_t* blocksize);
  static native void blosc1_cbuffer_sizes(byte[] compressedData, NativeLongByReference nbytes,
      NativeLongByReference cbytes, NativeLongByReference blocksize);

}
