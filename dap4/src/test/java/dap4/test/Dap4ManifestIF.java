/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.test;

/**
 * This class defines the set of all possible test base names.
 * It is the union of all names used any any test.
 * Each test excludes names that it cannot process.
 */

public interface Dap4ManifestIF {

  // List of all file names
  static public final String[][] dap4_manifest = {{"test_one_var"}, {"test_atomic_array"}, {"test_atomic_types"},
      {"test_enum_1"}, {"test_enum_2"}, {"test_enum_3"}, {"test_enum_array"}, {"test_fill"}, {"test_fill_2"},
      {"test_groups1"}, {"test_misc1"}, {"test_one_vararray"}, {"test_opaque"}, {"test_opaque_array"}, {"test_struct1"},
      {"test_struct_array"}, {"test_struct_nested"}, {"test_struct_nested3"}, {"test_struct_type"}, {"test_test"},
      {"test_unlim"}, {"test_unlim1"}, {"test_utf8"}, {"test_vlen1"}, {"test_vlen11"}, {"test_vlen2"}, {"test_vlen3"},
      {"test_vlen4"}, {"test_vlen5"}, {"test_vlen6"}, {"test_vlen7"}, {"test_vlen8"}, {"test_zerodim"}}; // dap4manifest

  // Define the Manifest of constrained tests
  static public final String[][] constraint_manifest =
      {{"test_atomic_array", "1", "/vu8[1][0:2:2];/vd[1];/vs[1][0];/vo[0][1]"},
          {"test_atomic_array", "2", "/v16[0:1,3]"}, {"test_atomic_array", "3", "/v16[3,0:1]"},
          {"test_one_vararray", "4", "/t[1]"}, {"test_one_vararray", "5", "/t[0:1]"},
          {"test_enum_array", "6", "/primary_cloud[1:2:4]"}, {"test_opaque_array", "7", "/vo2[1][0:1]"},
          {"test_struct_array", "8", "/s[0:2:3][0:1]"}};

} // Dap4Manifest

