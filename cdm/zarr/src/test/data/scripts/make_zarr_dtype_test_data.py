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
store = zarr.DirectoryStore('../test_dtypes.zarr')


# In[ ]:


root_grp = zarr.group(store, overwrite=True)
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
b = unordered_group.create_dataset('boolean_data', shape=(4,5), chunks=(2,5), dtype='|b1', overwrite=True, compressor=None)
b[:] = boolean_data
byte = unordered_group.create_dataset('byte_data', shape=(10,8), chunks=(5,4), dtype='|i1', overwrite=True, compressor=None)
byte[:] = bdata
ubyte = unordered_group.create_dataset('ubyte_data', shape=(10,8), chunks=(5,4), dtype='|u1', overwrite=True, compressor=None)
ubyte[:] = bdata


# In[ ]:


# add data to big endian group
shorts = big_endian.create_dataset('short_data', shape=(4,5), chunks=(2,5), dtype='>i2', overwrite=True, compressor=None)
shorts[:] = be_short_data
ushorts = big_endian.create_dataset('ushort_data', shape=(4,5), chunks=(2,5), dtype='>u2', overwrite=True, compressor=None)
ushorts[:] = be_short_data
ints = big_endian.create_dataset('int_data', shape=(4,5), chunks=(2,5), dtype='>i4', overwrite=True, compressor=None)
ints[:] = be_int_data
uints = big_endian.create_dataset('uint_data', shape=(4,5), chunks=(2,5), dtype='>u4', overwrite=True, compressor=None)
uints[:] = be_int_data
longs = big_endian.create_dataset('long_data', shape=(5,4), chunks=(5,2), dtype='>i8', overwrite=True, compressor=None)
longs[:] = be_long_data
ulongs = big_endian.create_dataset('ulong_data', shape=(5,4), chunks=(5,2), dtype='>u8', overwrite=True, compressor=None)
ulongs[:] = be_long_data
floats = big_endian.create_dataset('float_data', shape=(4,5), chunks=(2,5), dtype='>f4', overwrite=True, compressor=None)
floats[:] = be_float_data
doubles = big_endian.create_dataset('double_data', shape=(5,4), chunks=(5,2), dtype='>f8', overwrite=True, compressor=None)
doubles[:] = be_double_data


# In[ ]:


# add data to little endian group
shorts = little_endian.create_dataset('short_data', shape=(4,5), chunks=(2,5), dtype='<i2', overwrite=True, compressor=None)
shorts[:] = le_short_data
ushorts = little_endian.create_dataset('ushort_data', shape=(4,5), chunks=(2,5), dtype='<u2', overwrite=True, compressor=None)
ushorts[:] = le_short_data
ints = little_endian.create_dataset('int_data', shape=(4,5), chunks=(2,5), dtype='<i4', overwrite=True, compressor=None)
ints[:] = le_int_data
uints = little_endian.create_dataset('uint_data', shape=(4,5), chunks=(2,5), dtype='<u4', overwrite=True, compressor=None)
uints[:] = le_int_data
longs = little_endian.create_dataset('long_data', shape=(5,4), chunks=(5,2), dtype='<i8', overwrite=True, compressor=None)
longs[:] = le_long_data
ulongs = little_endian.create_dataset('ulong_data', shape=(5,4), chunks=(5,2), dtype='<u8', overwrite=True, compressor=None)
ulongs[:] = le_long_data
floats = little_endian.create_dataset('float_data', shape=(4,5), chunks=(2,5), dtype='<f4', overwrite=True, compressor=None)
floats[:] = le_float_data
doubles = little_endian.create_dataset('double_data', shape=(5,4), chunks=(5,2), dtype='<f8', overwrite=True, compressor=None)
doubles[:] = le_double_data


# In[ ]:


# add string data
chars = string_group.create_dataset('char_data', shape=(10,12), chunks=(5,3), dtype='S1', overwrite=True, compressor=None)
chars[:] = charar
strs = string_group.create_dataset('str_data', shape=(10,12), chunks=(5,6), dtype='S4', overwrite=True, compressor=None)
strs[:] = charar
unicode = string_group.create_dataset('unicode_data', shape=(10,12), chunks=(5,6), dtype='U4', overwrite=True, compressor=None)
unicode[:] = charar

