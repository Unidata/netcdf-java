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
 * of the C-Blosc2 library files include/blosc2.h and include/b2nd.h.
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
        log.debug("Reason: ", ule);
        throw new RuntimeException(message);
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

  /**
   * Decode one chunk written by the HDF5 Blosc2 filter (id 32026, reference implementation
   * {@code H5Zblosc2.c} in the HDF Group hdf5_plugins repository).
   * <p>
   * Each HDF5 chunk is stored as one Blosc2 contiguous frame. Chunks of rank 2 or more carry a b2nd
   * metalayer and are padded to a multiple of the block shape, so they are decoded with
   * {@code b2nd_to_cbuffer}, which removes the padding. Rank 1 chunks hold a single Blosc2 chunk and
   * are decoded with {@code blosc2_schunk_decompress_chunk}. This follows the decompression branch of
   * {@code H5Zblosc2.c}.
   *
   * @param frame the chunk bytes as stored in the file (a Blosc2 frame)
   * @param nbytes the size of the decoded HDF5 chunk in bytes ({@code cd_values[3]} of the filter); the
   *        uncompressed size in the frame header cannot be used because it may include padding
   * @return the decoded chunk, exactly {@code nbytes} long
   * @throws IOException if the frame is not a valid Blosc2 frame, is truncated or corrupt, or does not
   *         decode to {@code nbytes} bytes
   */
  public static byte[] decodeFrame(byte[] frame, int nbytes) throws IOException {
    if (frame == null || frame.length < FRAME_MAGIC_OFFSET + FRAME_MAGIC.length) {
      throw new IOException("Blosc2 frame is too short: " + (frame == null ? "null" : frame.length + " bytes"));
    }
    for (int i = 0; i < FRAME_MAGIC.length; i++) {
      if (frame[FRAME_MAGIC_OFFSET + i] != FRAME_MAGIC[i]) {
        throw new IOException("Not a Blosc2 frame (missing \"b2frame\" magic)");
      }
    }
    if (nbytes <= 0) {
      throw new IOException("Invalid decoded chunk size: " + nbytes);
    }
    init();
    // copy == false: the super-chunk refers to this memory, which must outlive it
    try (Memory in = new Memory(frame.length);
        Memory out = new Memory(nbytes);
        Memory arrayRef = new Memory(Native.POINTER_SIZE)) {
      in.write(0, frame, 0, frame.length);
      Pointer schunk = blosc2_schunk_from_buffer(in, frame.length, (byte) 0);
      if (schunk == null) {
        throw new IOException(
            "blosc2_schunk_from_buffer failed: corrupt or truncated frame (" + frame.length + " bytes)");
      }
      try {
        arrayRef.setPointer(0, null);
        if (b2nd_from_schunk(schunk, arrayRef) >= 0) {
          // rank >= 2: the b2nd array takes ownership of the super-chunk
          Pointer array = arrayRef.getPointer(0);
          schunk = null;
          if (array == null) {
            throw new IOException("b2nd_from_schunk did not return an array");
          }
          try {
            int rc = b2nd_to_cbuffer(array, out, nbytes);
            if (rc < 0) {
              throw new IOException("b2nd_to_cbuffer failed with error code " + rc);
            }
          } finally {
            b2nd_free(array);
          }
        } else {
          // rank 1: a single chunk
          int written = blosc2_schunk_decompress_chunk(schunk, 0, out, nbytes);
          if (written < 0) {
            throw new IOException("blosc2_schunk_decompress_chunk failed with error code " + written);
          }
          if (written != nbytes) {
            throw new IOException("Decoded " + written + " bytes, expected " + nbytes);
          }
        }
      } finally {
        if (schunk != null) {
          blosc2_schunk_free(schunk);
        }
      }
      return out.getByteArray(0, nbytes);
    }
  }

  // A Blosc2 contiguous frame starts with a msgpack header whose third byte begins "b2frame\0"
  private static final byte[] FRAME_MAGIC = {'b', '2', 'f', 'r', 'a', 'm', 'e', 0};
  private static final int FRAME_MAGIC_OFFSET = 2;

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

  // BLOSC_EXPORT blosc2_schunk* blosc2_schunk_from_buffer(uint8_t *cframe, int64_t len, bool copy);
  static native Pointer blosc2_schunk_from_buffer(Pointer cframe, long len, byte copy);

  // BLOSC_EXPORT int blosc2_schunk_free(blosc2_schunk *schunk);
  static native int blosc2_schunk_free(Pointer schunk);

  // BLOSC_EXPORT int blosc2_schunk_decompress_chunk(blosc2_schunk *schunk, int64_t nchunk, void *dest,
  // int32_t nbytes);
  static native int blosc2_schunk_decompress_chunk(Pointer schunk, long nchunk, Pointer dest, int nbytes);

  // BLOSC_EXPORT int b2nd_from_schunk(blosc2_schunk *schunk, b2nd_array_t **array);
  static native int b2nd_from_schunk(Pointer schunk, Pointer array);

  // BLOSC_EXPORT int b2nd_to_cbuffer(const b2nd_array_t *array, void *buffer, int64_t buffersize);
  static native int b2nd_to_cbuffer(Pointer array, Pointer buffer, long buffersize);

  // BLOSC_EXPORT int b2nd_free(b2nd_array_t *array);
  static native int b2nd_free(Pointer array);

}
