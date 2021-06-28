#!/usr/bin/env python
# coding: utf-8

# In[ ]:


import numpy as np
# create data
be_short_data = np.arange(100, dtype='>i2').reshape(10, 10) 
le_short_data = np.arange(100, dtype='<i2').reshape(10, 10)
be_int_data = np.arange(100, dtype='>i4').reshape(10, 10) 
le_int_data = np.arange(100, dtype='<i4').reshape(10, 10)


# In[ ]:


be_long_data = np.arange(100, dtype='>i8').reshape(10, 10)
le_long_data  = np.arange(100, dtype='<i8').reshape(10, 10)


# In[ ]:


be_float_data = np.arange(200, dtype='>f4').reshape(20, 10)
le_float_data = np.arange(200, dtype='<f4').reshape(20, 10)


# In[ ]:


be_double_data = np.arange(100, dtype='>f8').reshape(10, 10)
le_double_data = np.arange(100, dtype='<f8').reshape(10, 10)


# In[ ]:


boolean_data = np.full((10, 4), [0, 1, 0, 1], dtype='|b1')


# In[ ]:


byte_data = be_int_data.tobytes()
bdata = np.frombuffer(byte_data, dtype='|i1').reshape(20, 20);


# In[ ]:


charar = np.chararray((10, 12), itemsize=4)
charar[:] = 'abcd'


# In[ ]:


import zarr
store = zarr.DirectoryStore('../test_dtypes.zarr')


# In[ ]:


root_grp = zarr.group(store, overwrite=True)
# create a group for byte-order independent data types
unordered_group = root_grp.create_group('unordered_group')


# create a group for byte-ordered data types
byte_ordered_group = root_grp.create_group('byte_ordered_group')
# add groups for big and little endian
big_endian = byte_ordered_group.create_group('big_endian')
little_endian = byte_ordered_group.create_group('little_endian')

# create group for string types
string_group = root_grp.create_group('string_types')


# In[ ]:


# add data to unordered group
b = unordered_group.create_dataset('boolean_data', shape=(10, 4), chunks=(2, 2), dtype='|b1', overwrite=True)
b[:] = boolean_data
byte = unordered_group.create_dataset('byte_data', shape=(20, 20), chunks=(3, 3), dtype='|i1', overwrite=True)
byte[:] = bdata
ubyte = unordered_group.create_dataset('ubyte_data', shape=(20, 20), chunks=(4, 5), dtype='|u1', overwrite=True)
ubyte[:] = bdata


# In[ ]:


# add data to big endian group
shorts = big_endian.create_dataset('short_data', shape = (10, 10), chunks=(10, 5), dtype='>i2', overwrite=True)
shorts[:] = be_short_data
ushorts = big_endian.create_dataset('ushort_data', shape = (10, 10), chunks=(10, 5), dtype='>u2', overwrite=True)
ushorts[:] = be_short_data
ints = big_endian.create_dataset('int_data', shape = (10, 10), chunks=(10, 5), dtype='>i4', overwrite=True)
ints[:] = be_int_data
uints = big_endian.create_dataset('uint_data', shape = (10, 10), chunks=(10, 5), dtype='>u4', overwrite=True)
uints[:] = be_int_data
longs = big_endian.create_dataset('long_data', shape = (10, 10), chunks=(5, 10), dtype='>i8', overwrite=True)
longs[:] = be_long_data
ulongs = big_endian.create_dataset('ulong_data', shape = (10, 10), chunks=(5, 10), dtype='>u8', overwrite=True)
ulongs[:] = be_long_data
floats = big_endian.create_dataset('float_data', shape = (20, 10), chunks=(5, 5), dtype='>f4', overwrite=True)
floats[:] = be_float_data
doubles = big_endian.create_dataset('double_data', shape = (10, 10), chunks=(10, 10), dtype='>f8', overwrite=True)
doubles[:] = be_double_data


# In[ ]:


# add data to little endian group
shorts = little_endian.create_dataset('short_data', shape = (10, 10), chunks=(10, 5), dtype='<i2', overwrite=True)
shorts[:] = le_short_data
ushorts = little_endian.create_dataset('ushort_data', shape = (10, 10), chunks=(10, 5), dtype='<u2', overwrite=True)
ushorts[:] = le_short_data
ints = little_endian.create_dataset('int_data', shape = (10, 10), chunks=(10, 5), dtype='<i4', overwrite=True)
ints[:] = le_int_data
uints = little_endian.create_dataset('uint_data', shape = (10, 10), chunks=(10, 5), dtype='<u4', overwrite=True)
uints[:] = le_int_data
longs = little_endian.create_dataset('long_data', shape = (10, 10), chunks=(5, 10), dtype='<i8', overwrite=True)
longs[:] = le_long_data
ulongs = little_endian.create_dataset('ulong_data', shape = (10, 10), chunks=(5, 10), dtype='<u8', overwrite=True)
ulongs[:] = le_long_data
floats = little_endian.create_dataset('float_data', shape = (20, 10), chunks=(5, 5), dtype='<f4', overwrite=True)
floats[:] = le_float_data
doubles = little_endian.create_dataset('double_data', shape = (10, 10), chunks=(10, 10), dtype='<f8', overwrite=True)
doubles[:] = le_double_data


# In[ ]:


# add string data
chars = string_group.create_dataset('char_data', shape=(10,12), chunks=(2,4), dtype='S1', overwrite=True)
chars[:] = charar
strs = string_group.create_dataset('str_data', shape=(10,12), chunks=(2,2), dtype='S4', overwrite=True)
strs[:] = charar
unicode = string_group.create_dataset('unicode_data', shape=(10,12), chunks=(2,2), dtype='U4', overwrite=True)
unicode[:] = charar

