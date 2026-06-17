#!/usr/bin/env python
# coding: utf-8

# In[ ]:


import numpy as np
# create data
be_short_data = np.arange(20, dtype='>i2').reshape(4,5)
le_short_data = np.arange(20, dtype='<i2').reshape(4,5)
be_int_data = np.arange(20, dtype='>i4').reshape(4,5)
le_int_data = np.arange(20, dtype='<i4').reshape(4,5)


# In[ ]:


be_long_data = np.arange(20, dtype='>i8').reshape(5,4)
le_long_data  = np.arange(20, dtype='<i8').reshape(5,4)


# In[ ]:


be_float_data = np.arange(20, dtype='>f4').reshape(4,5)
le_float_data = np.arange(20, dtype='<f4').reshape(4,5)


# In[ ]:


be_double_data = np.arange(20, dtype='>f8').reshape(5,4)
le_double_data = np.arange(20, dtype='<f8').reshape(5,4)


# In[ ]:


boolean_data = np.full((4,5), [0, 1, 0, 1,0], dtype='|b1')


# In[ ]:


byte_data = be_int_data.tobytes()
bdata = np.frombuffer(byte_data, dtype='|i1').reshape(10,8);


# In[ ]:


charar = np.chararray((10,12), itemsize=4)
charar[:] = 'abcd'


# In[ ]:


import zarr
# Zarr-Python 3 API. The store is written using the Zarr format 2
# specification by passing zarr_format=2 to the top level API.
store = zarr.storage.LocalStore('../test_dtypes.zarr')


# In[ ]:


root_grp = zarr.group(store, overwrite=True, zarr_format=2)
# create a group for byte-order independent data types
unordered_group = root_grp.create_group('unordered_group', overwrite=True)


# create a group for byte-ordered data types
byte_ordered_group = root_grp.create_group('byte_ordered_group', overwrite=True)
# add groups for big and little endian
big_endian = byte_ordered_group.create_group('big_endian', overwrite=True)
little_endian = byte_ordered_group.create_group('little_endian', overwrite=True)

# create group for string types
string_group = root_grp.create_group('string_types', overwrite=True)


# In[ ]:


# add data to unordered group
b = unordered_group.create_array('boolean_data', shape=(4,5), chunks=(2,5), dtype='|b1', overwrite=True, compressors=None)
b[:] = boolean_data
byte = unordered_group.create_array('byte_data', shape=(10,8), chunks=(5,4), dtype='|i1', overwrite=True, compressors=None)
byte[:] = bdata
ubyte = unordered_group.create_array('ubyte_data', shape=(10,8), chunks=(5,4), dtype='|u1', overwrite=True, compressors=None)
ubyte[:] = bdata


# In[ ]:


# add data to big endian group
shorts = big_endian.create_array('short_data', shape=(4,5), chunks=(2,5), dtype='>i2', overwrite=True, compressors=None)
shorts[:] = be_short_data
ushorts = big_endian.create_array('ushort_data', shape=(4,5), chunks=(2,5), dtype='>u2', overwrite=True, compressors=None)
ushorts[:] = be_short_data
ints = big_endian.create_array('int_data', shape=(4,5), chunks=(2,5), dtype='>i4', overwrite=True, compressors=None)
ints[:] = be_int_data
uints = big_endian.create_array('uint_data', shape=(4,5), chunks=(2,5), dtype='>u4', overwrite=True, compressors=None)
uints[:] = be_int_data
longs = big_endian.create_array('long_data', shape=(5,4), chunks=(5,2), dtype='>i8', overwrite=True, compressors=None)
longs[:] = be_long_data
ulongs = big_endian.create_array('ulong_data', shape=(5,4), chunks=(5,2), dtype='>u8', overwrite=True, compressors=None)
ulongs[:] = be_long_data
floats = big_endian.create_array('float_data', shape=(4,5), chunks=(2,5), dtype='>f4', overwrite=True, compressors=None)
floats[:] = be_float_data
doubles = big_endian.create_array('double_data', shape=(5,4), chunks=(5,2), dtype='>f8', overwrite=True, compressors=None)
doubles[:] = be_double_data


# In[ ]:


# add data to little endian group
shorts = little_endian.create_array('short_data', shape=(4,5), chunks=(2,5), dtype='<i2', overwrite=True, compressors=None)
shorts[:] = le_short_data
ushorts = little_endian.create_array('ushort_data', shape=(4,5), chunks=(2,5), dtype='<u2', overwrite=True, compressors=None)
ushorts[:] = le_short_data
ints = little_endian.create_array('int_data', shape=(4,5), chunks=(2,5), dtype='<i4', overwrite=True, compressors=None)
ints[:] = le_int_data
uints = little_endian.create_array('uint_data', shape=(4,5), chunks=(2,5), dtype='<u4', overwrite=True, compressors=None)
uints[:] = le_int_data
longs = little_endian.create_array('long_data', shape=(5,4), chunks=(5,2), dtype='<i8', overwrite=True, compressors=None)
longs[:] = le_long_data
ulongs = little_endian.create_array('ulong_data', shape=(5,4), chunks=(5,2), dtype='<u8', overwrite=True, compressors=None)
ulongs[:] = le_long_data
floats = little_endian.create_array('float_data', shape=(4,5), chunks=(2,5), dtype='<f4', overwrite=True, compressors=None)
floats[:] = le_float_data
doubles = little_endian.create_array('double_data', shape=(5,4), chunks=(5,2), dtype='<f8', overwrite=True, compressors=None)
doubles[:] = le_double_data


# In[ ]:


# add string data
chars = string_group.create_array('char_data', shape=(10,12), chunks=(5,3), dtype='S1', overwrite=True, compressors=None)
chars[:] = charar
strs = string_group.create_array('str_data', shape=(10,12), chunks=(5,6), dtype='S4', overwrite=True, compressors=None)
strs[:] = charar
strs2 = string_group.create_array('str_data_2', shape=(10,12), chunks=(5,6), dtype='S2', overwrite=True, compressors=None)
strs2[:] = charar
unicode = string_group.create_array('unicode_data', shape=(10,12), chunks=(5,6), dtype='U4', overwrite=True, compressors=None)
unicode[:] = charar
unicode2 = string_group.create_array('unicode_data_2', shape=(10,12), chunks=(5,6), dtype='U2', overwrite=True, compressors=None)
unicode2[:] = charar
