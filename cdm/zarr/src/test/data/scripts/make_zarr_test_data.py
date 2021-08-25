#!/usr/bin/env python
# coding: utf-8

# In[ ]:


import zarr
## toggle comment to switch between directory store and zip store
store = zarr.DirectoryStore('../zarr_test_data.zarr')
# store = zarr.ZipStore('../zarr_test_data.zip', mode='w')
root_grp = zarr.group(store, overwrite=True)


# In[ ]:


# make array without group and uninitialized data, set fill_vaue
a = zarr.create(shape=(20, 20), chunks=(10, 10), dtype='f4', fill_value=999.0, store=store, overwrite=True)
a[:] = 0


# In[ ]:


# make group with '/' separator and 'F' order
attrs_grp = root_grp.create_group('group_with_attrs', overwrite=True)


# In[ ]:


# add attributes to group
attrs_grp.attrs['group_attr'] = 'foo'


# In[ ]:


# add array to group with 'F' order
a = attrs_grp.create_dataset('F_order_array', shape=(20, 20), chunks=(4, 5), dtype='i4', order='F', overwrite=True)


# In[ ]:


# add data to array
import numpy as np
data = np.tile(np.arange(20), (20,1))
a[:] = data


# In[ ]:


# add attributes to array
a.attrs['foo'] = 42
a.attrs['bar'] = 'apples'
a.attrs['baz'] = [1, 2, 3, 4]


# In[ ]:


# make group for multidimensonal data
dims_grp = root_grp.create_group('group_with_dims', overwrite=True)


# In[ ]:


# add 1D array
a1 = dims_grp.create_dataset('var1D', shape=(20,), chunks=(4,), dtype='i4', overwrite=True, compressor=None)
data = np.arange(20)
a1[:] = data


# In[ ]:


# add 2D array
a2 = dims_grp.create_dataset('var2D', shape=(20,20), chunks=(4,4), dtype='i4', overwrite=True, compressor=None)
a2[:] = np.tile(data, (20,1))


# In[ ]:


# add 3D array
a3 = dims_grp.create_dataset('var3D', shape=(20,20,20), chunks=(4,4,4), dtype='i4', overwrite=True, compressor=None)
a3[:] = np.tile(data, (20,20,1))


# In[ ]:


# add 4D array
a4 = dims_grp.create_dataset('var4D', shape=(20,20,20,20), chunks=(4,4,4,4), dtype='i4', overwrite=True, compressor=None)
a4[:] = np.tile(data, (20,20,20,1))


# In[ ]:


store.close()

