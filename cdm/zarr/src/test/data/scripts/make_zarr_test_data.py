#!/usr/bin/env python
# coding: utf-8

# In[ ]:


import zarr
## toggle comment to switch between directory store and zip store
store = zarr.DirectoryStore('../zarr_test_data.zarr')
# store = zarr.ZipStore('../zarr_test_data.zip', mode='w')
root_grp = zarr.group(store, overwrite=True)


# In[ ]:


# make group with '/' separator and 'F' order
attrs_grp = root_grp.create_group('group_with_attrs', overwrite=True)


# In[ ]:


# add attributes to group
attrs_grp.attrs['group_attr'] = 'foo'


# In[ ]:


# add array to group with 'F' order
a = attrs_grp.create_dataset('F_order_array', shape=(20, 20), chunks=(4, 5), dtype='i4', order='F', overwrite=True, compressor=None)


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


# create uninitialized array
a = attrs_grp.create_dataset('uninitialized', shape=(20, 20), chunks=(10, 10), dtype='f4', fill_value=999.0, overwrite=True, compressor=None)


# In[ ]:


# create partially initialized arrays
a = attrs_grp.create_dataset('partial_fill1', shape=(20, 20), chunks=(10, 10), dtype='f4', fill_value=999.0, overwrite=True, compressor=None)
a[0:10,0:10]=0
a = attrs_grp.create_dataset('partial_fill2', shape=(20, 20), chunks=(10, 10), dtype='f4', fill_value=999.0, overwrite=True, compressor=None)
a[15:20,10:20]=0


# In[ ]:


# create nested arrays
# dimension_separator keyword does not work for now, data is manually edited
a = attrs_grp.create_dataset('nested', shape=(20, 20), chunks=(10, 10), dtype='i2', overwrite=True, compressor=None) #, dimension_separator='/')
a[:]=0


# In[ ]:


# make group for multidimensonal data
dims_grp = root_grp.create_group('group_with_dims', overwrite=True)


# In[ ]:


# add 1D array
a1 = dims_grp.create_dataset('var1D', shape=(20,), chunks=(5,), dtype='i4', overwrite=True, compressor=None)
data = np.arange(20)
a1[:] = data


# In[ ]:


# add 2D array
a2 = dims_grp.create_dataset('var2D', shape=(20,20), chunks=(5,5), dtype='i4', overwrite=True, compressor=None)
a2[:] = np.tile(data, (20,1))


# In[ ]:


# add 3D array
a3 = dims_grp.create_dataset('var3D', shape=(20,20,20), chunks=(5,5,5), dtype='i4', overwrite=True, compressor=None)
a3[:] = np.tile(data, (20,20,1))


# In[ ]:


# add 4D array
a4 = dims_grp.create_dataset('var4D', shape=(20,20,20,20), chunks=(5,5,5,5), dtype='i4', overwrite=True, compressor=None)
a4[:] = np.tile(data, (20,20,20,1))


# In[ ]:


## uncomment for zip store
# store.close()

