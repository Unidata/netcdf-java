import numpy as np
import zarr

store = zarr.storage.LocalStore('../test_o10_multichunk.zarr')

# create array
data = np.arange(10000).reshape((100,100))

root_group = zarr.group(store, overwrite=True, zarr_format=2)

# create array with more than 10 chunks in each dimension
# 10 chunks in first dimension, 20 chunks in second
# so chunks will be [0-9].[0-19]
multichunk = root_group.create_array('ten_by_five', shape=data.shape, chunks=(10,5), dtype='<u8', overwrite=True, compressors=None)
multichunk[:] = data

multichunk_blosc = root_group.create_array('ten_by_five_blosc', shape=data.shape, chunks=(10,5), dtype='<u8', overwrite=True)
multichunk_blosc[:] = data

compressors=None
print(multichunk)
print(multichunk[:])

print(multichunk_blosc)
print(multichunk_blosc[:])