/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * Multidimensional arrays stored in memory.
 * <p>
 * Primitive arrays are byte, char, double, float, int, long, short, String.
 * The unsigned integral types use their signed versions, and the caller must widen if desired.
 * The enums are mapped to byte, short or int.
 * Boolean is represented as byte, 0 = false, non-zero = true.
 * Vlen is Array of variable length primitive arrays of T, eg double[fixed][].
 * Opaque is Vlen of byte[].
 * Vlen and Structure are handled seperately, eg you cant use Arrays.factory().
 * </p>
 */
package ucar.array;
