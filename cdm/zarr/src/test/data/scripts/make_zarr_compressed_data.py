#!/usr/bin/env python
# coding: utf-8

# In[ ]:


import zarr
store = zarr.DirectoryStore('../zarr_compressed_data.zarr')
root_grp = zarr.group(store, overwrite=True)


# In[ ]:


# create data array (200x200)
import numpy as np
data = np.tile(np.arange(200), (200,1))


# In[ ]:


# import codecs
from numcodecs import Zlib, Shuffle, CRC32, Adler32, FixedScaleOffset


# In[ ]:


# add uncompressed data array
a = root_grp.create_dataset('null_compressor', shape=(200,200), chunks=(50,50), dtype='f4', overwrite=True, compressor=None)
a[:] = data


# In[ ]:


# add groups
compressed_grp = root_grp.create_group('compressed', overwrite=True)
filtered_grp = root_grp.create_group('filtered', overwrite=True)
comp_filt_grp = root_grp.create_group('comp_filt', overwrite=True)


# In[ ]:


# add compressed data arrays (no filters)
# deflate
a = compressed_grp.create_dataset('deflate1', shape=(200, 200), chunks=(50, 50), dtype='i4', overwrite=True, 
                            compressor=Zlib(level=1))
a[:] = data
a = compressed_grp.create_dataset('deflate9', shape=(200, 200), chunks=(50, 50), dtype='i4', overwrite=True, 
                            compressor=Zlib(level=0))
a[:] = data
# shuffle
a = compressed_grp.create_dataset('shuffle', shape=(200, 200), chunks=(50, 50), dtype='i4', overwrite=True, 
                            compressor=Shuffle())
a[:] = data
# 32 bit checksum
a = compressed_grp.create_dataset('crc32', shape=(200, 200), chunks=(50, 50), dtype='i4', overwrite=True, 
                            compressor=CRC32())
a[:] = data
a = compressed_grp.create_dataset('adler32', shape=(200, 200), chunks=(50, 50), dtype='i4', overwrite=True, 
                            compressor=Adler32())
a[:] = data
# fixedscaleoffset
a = compressed_grp.create_dataset('scaleOffset', shape=(200, 200), chunks=(50, 50), dtype='f4', overwrite=True, 
                            compressor=FixedScaleOffset(offset=1000, scale=10, dtype='<f4', astype='u1'))
data2 = np.tile(np.arange(1000, 1020, .1), (200,1))
a[:] = data2


# In[ ]:


# add filtered data arrays (no compressor)
a = filtered_grp.create_dataset('adler32', shape=(200, 200), chunks=(50, 50), dtype='i4', overwrite=True, 
                            filters=[Adler32()], compressor=None)
a[:] = data

a = filtered_grp.create_dataset('adler_shuffle', shape=(200, 200), chunks=(50, 50), dtype='i4', overwrite=True, 
                            filters=[Adler32(), Shuffle()], compressor=None)
a[:] = data


# In[ ]:


# add compressed and filtered data arrays
a = comp_filt_grp.create_dataset('shuffle_deflate', shape=(200, 200), chunks=(50, 50), dtype='i4', overwrite=True, 
                            filters=[Shuffle()], compressor=Zlib(level=1))
a[:] = data

a = comp_filt_grp.create_dataset('Adler_shuffle_deflate', shape=(200, 200), chunks=(50, 50), dtype='i4', overwrite=True, 
                            filters=[Adler32(), Shuffle()], compressor=Zlib(level=1))
a[:] = data

