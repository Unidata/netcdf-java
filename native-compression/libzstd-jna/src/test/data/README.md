# zstd (HDF5 filter 32015) test data

| File | Content |
|---|---|
| `zstd_raw.nc` | netCDF-4, no filters (reference values) |
| `zstd_32015.nc` | same data, every variable compressed with the HDF5 Zstandard filter (id 32015) |
| `zstd_frame.bin` | 2560 bytes of float data |
| `zstd_frame.zst` | `zstd_frame.bin` as one zstd frame (level 19, decompressed size recorded) |
| `zstd_two_frames.zst` | `zstd_frame.zst` twice (decodes to `zstd_frame.bin` twice) |
| `zstd_frame_no_size.zst` | `zstd_frame.bin` as one zstd frame without the decompressed size (streamed, `--no-content-size`) |

Variables in the netCDF files (dimensions `time = 12`, `y = 20`, `x = 30`):

| Variable | Chunks | Filters |
|---|---|---|
| `double time(time)` | 5 | `_Filter = "32015,3"` (level 3) |
| `float temp(time, y, x)` | 5, 8, 16 | `_Shuffle = "true"`, `_Filter = "32015,5"` (shuffle, then zstd level 5) |
| `int count(y, x)` | 8, 16 | `_Filter = "32015,1"` (level 1) |

The chunk sizes leave partial edge chunks in every dimension.

Values:

- `time[t] = 6 * t`
- `temp[t][j][i] = 250 + 0.5 t + 20 sin(0.3 j) cos(0.2 i) + 0.01 i` (written with 4 decimals)
- `count[j][i] = (31 j + 7 i) % 1000 - 500`

The netCDF files were generated with netCDF-C 4.9.3, HDF5 1.14.6 and zstd 1.5.7: a CDL file with the values above
was converted with `ncgen -k nc4`, once as is and once with `_ChunkSizes`, `_Shuffle` and `_Filter` attributes added.
`ncdump` of both files gives identical data. The `.zst` files were made with the zstd 1.5.7 command line tool.
