/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package thredds.filesystem.s3;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import thredds.inventory.CollectionConfig;
import thredds.inventory.MController;
import thredds.inventory.MControllerProvider;
import thredds.inventory.MFile;
import thredds.inventory.s3.MFileS3;
import ucar.unidata.io.s3.CdmS3Client;
import ucar.unidata.io.s3.CdmS3Uri;

/**
 * Implements an MController without caching, reading from the object store each time.
 * recheck is ignored (always true)
 */
public class ControllerS3 implements MController {

  private static final Logger logger = LoggerFactory.getLogger(ControllerS3.class);

  private S3Client client;
  private CdmS3Uri initialUri;

  // package private for testing
  boolean limit = false;
  static final int LIMIT_COUNT_MAX = MFileS3Iterator.LIMIT_COUNT_MAX;

  public ControllerS3() {}

  private void init(CollectionConfig mc) {
    if (mc != null) {
      try {
        initUri(mc.getDirectoryName());
        initClient();
      } catch (IOException e) {
        logger.error("Error initializing ControllerS3 for {}.", mc.getDirectoryName(), e);
      }
    }
  }

  private void initUri(String location) throws IOException {
    try {
      initialUri = new CdmS3Uri(location);
    } catch (URISyntaxException ue) {
      throw new IOException("Cannot create a CdmS3Uri from " + location, ue);
    }
  }

  private void initClient() throws IOException {
    client = CdmS3Client.acquire(initialUri);
  }

  @Override
  public Iterator<MFile> getInventoryAll(CollectionConfig mc, boolean recheck) {
    init(mc);
    String prefix = null;
    if (initialUri.getKey().isPresent()) {
      prefix = initialUri.getKey().get();
    }
    // to get all inventory, we need to make the listObject call in MFileS3Iterator without a delimiter.
    // but, we want the resulting MFile object to retain CdmS3Uri objects that continue to have a delimiter.
    return new FilteredIterator(mc, new MFileS3Iterator(client, initialUri, prefix, limit, true), true, true);
  }

  @Override
  public Iterator<MFile> getInventoryTop(CollectionConfig mc, boolean recheck) {
    init(mc);
    String prefix = null;
    if (initialUri.getKey().isPresent()) {
      prefix = initialUri.getKey().get();
    }
    return new FilteredIterator(mc, new MFileS3Iterator(client, initialUri, prefix, limit, false), false);
  }

  @Override
  public Iterator<MFile> getSubdirs(CollectionConfig mc, boolean recheck) {
    init(mc);
    String prefix = null;
    if (initialUri.getKey().isPresent()) {
      prefix = initialUri.getKey().get();
    }

    List<CommonPrefix> commonPrefixes;
    if (initialUri.isAws()) {
      ListObjectsV2Request listObjects = MFileS3Iterator.getListObjectsRequestV2(initialUri, prefix);
      ListObjectsV2Response res = client.listObjectsV2(listObjects);
      commonPrefixes = res.commonPrefixes();
    } else {
      ListObjectsRequest listObjects = MFileS3Iterator.getListObjectsRequestV1(initialUri, prefix);
      ListObjectsResponse res = client.listObjects(listObjects);
      commonPrefixes = res.commonPrefixes();
    }

    List<MFile> mFiles = new ArrayList<>(commonPrefixes.size());
    for (CommonPrefix commonPrefix : commonPrefixes) {
      CdmS3Uri cdmS3Uri;
      try {
        cdmS3Uri = initialUri.resolveNewKey(commonPrefix.prefix());
        mFiles.add(new MFileS3(cdmS3Uri));
      } catch (URISyntaxException e) {
        logger.error("Error creating MFile for {} bucket {}", commonPrefix, initialUri.getBucket(), e);
      }
    }
    return mFiles.iterator();
  }

  @Override
  public void close() {} // NO-OP

  // handles filtering and removing/including subdirectories
  private static class FilteredIterator implements Iterator<MFile> {
    private final Iterator<MFile> orgIter;
    private final CollectionConfig mc;
    private final boolean wantDirs;
    private final boolean fullInventory;

    private MFile next;

    FilteredIterator(CollectionConfig mc, Iterator<MFile> iter, boolean wantDirs) {
      this.orgIter = iter;
      this.mc = mc;
      this.wantDirs = wantDirs;
      this.fullInventory = false;
    }

    FilteredIterator(CollectionConfig mc, Iterator<MFile> iter, boolean wantDirs, boolean fullInventory) {
      this.orgIter = iter;
      this.mc = mc;
      this.wantDirs = wantDirs;
      this.fullInventory = fullInventory;
    }

    public boolean hasNext() {
      next = nextFilteredFile();
      return (next != null);
    }

    public MFile next() {
      if (next == null)
        throw new NoSuchElementException();
      return next;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Nullable
    private MFile nextFilteredFile() {
      if (orgIter == null)
        return null;

      if (!orgIter.hasNext())
        return null;

      MFile pdata = orgIter.next();
      // skip directories if they should be skipped, and skip things excluded by the configuration
      while ((pdata.isDirectory() != wantDirs && !fullInventory) || !mc.accept(pdata)) {
        if (!orgIter.hasNext()) {
          pdata = null;
          break;
        }
        pdata = orgIter.next();
      }
      return pdata;
    }
  }

  // returns all objects with the current prefix
  private static class MFileS3Iterator implements Iterator<MFile> {

    static final Logger logger = LoggerFactory.getLogger(MFileS3Iterator.class);

    private final CdmS3Uri bucketUri;
    private final S3Client client;
    private final String prefix;
    private final boolean fullInventory;
    private final boolean useV2;
    final List<String> errorMessages = new ArrayList<>();

    // used for testing the ability to create MFilesS3 objects from a CdmS3Uri without a prefix.
    // will limit the iterator to a maximum of 2000 objects.
    static final int LIMIT_COUNT_MAX = 2000;
    private final boolean limit;

    private List<S3Object> objects;
    private ListObjectsResponse responseV1;
    private ListObjectsV2Response responseV2;
    private int count;
    private int totalObjectCount = 0;

    MFileS3Iterator(S3Client cdmClient, CdmS3Uri bucketUri, @Nullable String prefix, boolean limit,
        boolean fullInventory) {
      this.bucketUri = bucketUri;
      this.prefix = prefix;
      client = cdmClient;
      this.limit = limit;
      this.fullInventory = fullInventory;
      // Only use v2 api of aws S3 SDK on amazon resources
      useV2 = bucketUri.isAws();
      // make first call to get a list of objects from the object store
      updateObjectList(null);
    }

    private void updateObjectList(String continuationToken) {
      objects = useV2 ? getObjectsV2(continuationToken) : getObjectsV1(continuationToken);
      // we have a new objects list, reset the local index tracker to zero
      count = 0;
      if (limit) {
        totalObjectCount += objects.size();
      }
    }

    private List<S3Object> getObjectsV1(String continuationToken) {
      ListObjectsRequest request = getListObjectsRequestV1(bucketUri, prefix, continuationToken, fullInventory);
      responseV1 = client.listObjects(request);
      return responseV1.contents();
    }

    private List<S3Object> getObjectsV2(String continuationToken) {
      ListObjectsV2Request request = getListObjectsRequestV2(bucketUri, prefix, continuationToken, fullInventory);
      responseV2 = client.listObjectsV2(request);
      return responseV2.contents();
    }

    // package private so that we can access them from ControllerS3 class
    static ListObjectsRequest getListObjectsRequestV1(CdmS3Uri uri, String prefix) {
      return getListObjectsRequestV1(uri, prefix, null, false);
    }

    static ListObjectsV2Request getListObjectsRequestV2(CdmS3Uri uri, String prefix) {
      return getListObjectsRequestV2(uri, prefix, null, false);
    }

    private static ListObjectsRequest getListObjectsRequestV1(CdmS3Uri uri, String prefix, String continuationToken,
        boolean fullInventory) {
      ListObjectsRequest.Builder listObjectsBuilder = ListObjectsRequest.builder().bucket(uri.getBucket());
      // Add the delimiter to the list objects request if the prefix is set and we are not wanting to get the full
      // inventory.
      if (uri.getDelimiter().isPresent() && !fullInventory) {
        listObjectsBuilder.delimiter(uri.getDelimiter().get());
      }

      if (prefix != null) {
        listObjectsBuilder.prefix(prefix);
      }

      if (continuationToken != null) {
        listObjectsBuilder.marker(continuationToken);
      }

      return listObjectsBuilder.build();
    }

    private static ListObjectsV2Request getListObjectsRequestV2(CdmS3Uri uri, String prefix, String continuationToken,
        boolean fullInventory) {
      Builder listObjectsBuilder = ListObjectsV2Request.builder().bucket(uri.getBucket());
      // Add the delimiter to the list objects request if the prefix is set and we are not wanting to get the full
      // inventory.
      if (uri.getDelimiter().isPresent() && !fullInventory) {
        listObjectsBuilder.delimiter(uri.getDelimiter().get());
      }

      if (prefix != null) {
        listObjectsBuilder.prefix(prefix);
      }

      if (continuationToken != null) {
        listObjectsBuilder.continuationToken(continuationToken);
      }
      return listObjectsBuilder.build();
    }

    public boolean hasNext() {
      boolean hasMore;
      if (count < objects.size()) {
        hasMore = true;
      } else {
        // It is possible that the first response contained zero objects and that we end up here.
        // Check that condition first.
        if (objects.isEmpty()) {
          hasMore = false;
        } else {
          // if we've exhausted the current list of S3Objects, and that list was not empty, check to see if the
          // response from which the object list was generated is truncated.
          hasMore = useV2 ? responseV2.isTruncated() : responseV1.isTruncated();
          if (hasMore) {
            // The response was truncated. Make a new request using the next continuation token to pick up where we
            // left off.
            String continuationToken = useV2 ? responseV2.nextContinuationToken() : responseV1.nextMarker();
            updateObjectList(continuationToken);
          }
        }
      }

      if (limit && totalObjectCount > LIMIT_COUNT_MAX) {
        hasMore = false;
      }

      return hasMore;
    }

    public MFile next() {
      S3Object object = objects.get(count++);
      MFile nextMFile = null;
      try {
        CdmS3Uri newUri = bucketUri.resolveNewKey(object.key());
        nextMFile = new MFileS3(newUri, object.size(), object.lastModified().toEpochMilli());
      } catch (URISyntaxException e) {
        logger.warn("Cannot create MFile for {} in bucket {}", object.key(), bucketUri.getBucket(), e);
      }
      return nextMFile;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  public static class Provider implements MControllerProvider {

    private static String protocol = CdmS3Uri.SCHEME_CDM_S3;

    @Override
    public String getProtocol() {
      return protocol;
    }

    @Override
    public MController create() {
      return new ControllerS3();
    }
  }
}
