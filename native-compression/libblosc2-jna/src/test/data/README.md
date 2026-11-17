# Blosc2 (HDF5 filter 32026) test data

| File | Content |
|---|---|
| `blosc2_raw.nc` | netCDF-4, no filters (reference values) |
| `blosc2_32026.nc` | same data, every variable compressed with the HDF5 Blosc2 filter (id 32026) |

Variables (dimensions `time = 12`, `y = 20`, `x = 30`):

| Variable | Chunks | `_Filter` (user parameters) | Decoding path |
|---|---|---|---|
| `double time(time)` | 5 | `32026,0,0,0,0,3,0,5` (level 3, no shuffle, zstd) | rank 1: `blosc2_schunk_decompress_chunk` |
| `float temp(time, y, x)` | 5, 8, 16 | `32026,0,0,0,0,5,2,5` (level 5, bitshuffle, zstd) | rank 3: `b2nd_to_cbuffer` |
| `int count(y, x)` | 8, 16 | `32026,0,0,0,0,5,1,1` (level 5, byte shuffle, lz4) | rank 2: `b2nd_to_cbuffer` |

The chunk sizes leave partial edge chunks in every dimension.

Values:

- `time[t] = 6 * t`
- `temp[t][j][i] = 250 + 0.5 t + 20 sin(0.3 j) cos(0.2 i) + 0.01 i` (written with 4 decimals)
- `count[j][i] = (31 j + 7 i) % 1000 - 500`

Generated with netCDF-C 4.9.3, HDF5 1.14.6 and the HDF5 Blosc2 plugin (`H5Zblosc2.c` with C-Blosc2 2.23.1):
a CDL file with the values above was converted with `ncgen -k nc4`, once as is and once with
`_ChunkSizes` and `_Filter` attributes added. `ncdump` of both files gives identical data.

Single chunks (for decoder unit tests), extracted from `blosc2_32026.nc` with `H5Dread_chunk`
(`.b2frame`, bytes as stored in the file) and `H5Dread` with the HDF5 Blosc2 plugin (`.expected`):

| File | Chunk | Frame bytes | Decoded bytes | `cd_values` |
|---|---|---:|---:|---|
| `blosc2_temp_chunk0.*` | `temp` chunk 0 (rank 3, 5 x 8 x 16 floats) | 1203 | 2560 | `1,0,4,2560,5,2,5,3,5,8,16` |
| `blosc2_time_chunk0.*` | `time` chunk 0 (rank 1, 5 doubles) | 244 | 40 | `1,0,8,40,3,0,5` |
