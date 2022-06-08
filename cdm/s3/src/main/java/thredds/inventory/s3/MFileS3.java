/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.s3;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest.Builder;
import thredds.inventory.MFile;
import thredds.inventory.MFileProvider;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.s3.CdmS3Client;
import ucar.unidata.io.s3.CdmS3Uri;
import ucar.unidata.io.s3.S3RandomAccessFile;

/**
 * Implements {@link thredds.inventory.MFile} for objects stored on AWS S3 compatible object stores.
 *
 * @since 5.4.0
 */
public class MFileS3 implements MFile {

  private static final Logger logger = LoggerFactory.getLogger(MFileS3.class);

  private final Supplier<HeadObjectResponse> headObjectResponse;
  private final CdmS3Uri cdmS3Uri;
  private final String key;
  private final String delimiter;

  private long length;
  private long lastMod;
  private Boolean exists;

  private Object auxInfo;


  public MFileS3(String s3Uri) throws IOException {
    this(s3Uri, -1, -1);
  }

  public MFileS3(CdmS3Uri s3Uri) {
    this(s3Uri, -1, -1);
  }

  public MFileS3(String s3Uri, long len, long lm) throws IOException {
    try {
      cdmS3Uri = new CdmS3Uri(s3Uri);
    } catch (URISyntaxException e) {
      throw new IOException("Unable to create a CdmS3Uri from: " + s3Uri, e);
    }
    key = getKey();
    delimiter = getDelimiter();
    length = len;
    lastMod = lm;

    // This can take some time, so wait to execute until the first time headObjectResponse is accessed
    this.headObjectResponse = () -> getHeadObjectResponse();
  }

  public MFileS3(CdmS3Uri s3Uri, long len, long lm) {
    cdmS3Uri = s3Uri;
    key = getKey();
    delimiter = getDelimiter();
    length = len;
    lastMod = lm;
    // This can take some time, so wait to execute until the first time headObjectResponse is accessed
    this.headObjectResponse = () -> getHeadObjectResponse();
  }

  @Nullable
  private String getKey() {
    return cdmS3Uri.getKey().isPresent() ? cdmS3Uri.getKey().get() : null;
  }

  @Nullable
  private String getDelimiter() {
    return cdmS3Uri.getDelimiter().isPresent() ? cdmS3Uri.getDelimiter().get() : null;
  }

  @Nullable
  private HeadObjectResponse getHeadObjectResponse() {
    HeadObjectResponse response = null;
    S3Client client = getClient();

    if (client != null) {
      Builder headObjectRequestBuilder = HeadObjectRequest.builder().bucket(cdmS3Uri.getBucket());
      if (key != null) {
        headObjectRequestBuilder.key(key);
      }
      response = client.headObject(headObjectRequestBuilder.build());
    }
    return response;
  }

  @Nullable
  private S3Client getClient() {
    S3Client client = null;
    try {
      client = CdmS3Client.acquire(cdmS3Uri);
    } catch (IOException ioe) {
      logger.error("Could not create a CdmS3Client for {}", cdmS3Uri, ioe);
    }
    return client;
  }

  @Override
  public long getLastModified() {
    // negative values indicate unavailable, get from head request
    return this.lastMod < 0 ? updateLastModified() : this.lastMod;
  }

  /** Update last modified by fetching from a head request */
  public long updateLastModified() {
    this.lastMod = headObjectResponse.get().lastModified().toEpochMilli();
    return this.lastMod;
  }

  @Override
  public long getLength() {
    return this.length < 0 ? updateLength() : this.length;
  }

  /** Update file length by fetching from a head request */
  public long updateLength() {
    this.length = headObjectResponse.get().contentLength();
    return this.length;
  }

  @Override
  public boolean isDirectory() {
    // Object stores do not have "directories", so to say. But, a key can have a hierarchical structure encoded into
    // it. As an example, often people simply name the object store keys the same as the path, or partial path, of the
    // file they upload to the object store from their disk. Because hierarchical keys are common, the S3 API includes
    // the concept of a "delimiter", which is the delimiter used in the hierarchy. Here we deal with trying to figure
    // out if an MFile should be interpreted as a directory based on the value of key and delimiter.
    boolean isDirectory = false;
    // Really only need to consider the case where a delimiter is set - without a delimiter, there is no concept of a
    // directory
    if (delimiter != null) {
      if ((key != null) && (key.endsWith(delimiter))) {
        // There is a key and delimiter, and the key ends with the delimiter - consider this a "directory"
        isDirectory = true;
      } else if (key == null) {
        // There is a delimiter, but no key. Essentially the "root" of the bucket, but the delimiter signals that the
        // keys are considered to have a hierarchy. Consider this a directory as well.
        isDirectory = true;
      }
    }

    return isDirectory;
  }

  @Override
  public String getPath() {
    // string representation of the cdms3 uri
    return cdmS3Uri.toString();
  }

  @Override
  public String getName() {
    // default - assume delimiter is null, in which case the object name will simply be the key
    String name = key;
    // No matter what the case may be with the delimiter, if the key is null, then the name of the object is an
    // empty string.
    if (key == null) {
      name = "";
    } else if (delimiter != null) {
      // If there is no delimiter, the name is the key. However, if there is a delimiter, the name will be the rightmost
      // part of the path.

      // First off, if the key ends with the delimiter, pop it off
      if (key.endsWith(delimiter)) {
        name = name.substring(0, name.length() - delimiter.length());
      }

      // Now, find the location of the rightmost delimiter
      int lastDelimiter = name.lastIndexOf(delimiter);

      if (lastDelimiter >= 0) {
        // the "name" is everything after that last delimiter
        name = name.substring(lastDelimiter);
        if (name.startsWith(delimiter)) {
          name = name.substring(delimiter.length());
        }
      }
    }

    return name;
  }

  @Override
  @Nullable
  public MFile getParent() throws IOException {
    // In general, objects to do not have parents. However, if a delimiter is set, we have a pseudo path, and then
    // the object can have a parent.
    MFile parentMfile = null;
    if (delimiter != null) {
      // get the full path
      String currentUri = getPath();
      String frag = "";
      int chop = currentUri.lastIndexOf("#");
      if (chop > 0) {
        frag = currentUri.substring(chop);
        currentUri = currentUri.substring(0, chop);
      }

      // if the uri ends with the delimiter, remove it
      if (currentUri.endsWith(delimiter)) {
        currentUri = currentUri.substring(0, currentUri.length() - delimiter.length());
      }

      // Now we essentially want to remove the current name to get the parent
      String currentName = getName();
      if (currentName != null) {
        // If the childName is empty, then we are at the top of the bucket already, so the parent is null. However,
        // if the childName isn't empty, then keep digging.
        if (!currentName.isEmpty()) {
          int childLoc = currentUri.lastIndexOf(currentName);
          if (childLoc > 0) {
            String parentUri = currentUri.substring(0, childLoc);
            parentMfile = new MFileS3(parentUri + frag);
          }
        }
      }
    }
    return parentMfile;
  }

  @Override
  public int compareTo(MFile o) {
    // compare object uri strings
    return getPath().compareTo(o.getPath());
  }

  @Override
  @Nullable
  public Object getAuxInfo() {
    return auxInfo;
  }

  @Override
  public void setAuxInfo(Object auxInfo) {
    this.auxInfo = auxInfo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cdmS3Uri, key, delimiter, auxInfo);
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }

    if (!(o instanceof MFileS3)) {
      return false;
    }

    MFileS3 mFileS3 = (MFileS3) o;

    return (cdmS3Uri.equals(mFileS3.cdmS3Uri) && Objects.equals(key, mFileS3.key)
        && Objects.equals(delimiter, mFileS3.delimiter) && Objects.equals(auxInfo, mFileS3.auxInfo));
  }

  @Override
  public boolean exists() {
    if (exists == null) {
      updateExists();
    }

    return exists;
  }

  // Update file exists by fetching from a head request
  private void updateExists() {
    try {
      headObjectResponse.get();
      exists = true;
    } catch (NoSuchKeyException e) {
      exists = false;
    }
  }

  @Override
  public ResponseInputStream<GetObjectResponse> getInputStream() {
    S3Client client = getClient();

    if (client == null) {
      return null;
    }

    GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(cdmS3Uri.getBucket()).key(key).build();
    return client.getObject(getObjectRequest);
  }

  @Override
  public void writeToStream(OutputStream outputStream) throws IOException {
    ResponseInputStream<GetObjectResponse> responseInputStream = getInputStream();

    IO.copy(responseInputStream, outputStream);
  }

  @Override
  public void writeToStream(OutputStream outputStream, long offset, long maxBytes) throws IOException {
    final S3RandomAccessFile.Provider provider = new S3RandomAccessFile.Provider();

    try (RandomAccessFile randomAccessFile = provider.open(cdmS3Uri.toString())) {
      IO.copyRafB(randomAccessFile, offset, maxBytes, outputStream);
    }
  }

  public static class Provider implements MFileProvider {

    private static String protocol = CdmS3Uri.SCHEME_CDM_S3;

    @Override
    public String getProtocol() {
      return protocol;
    }

    @Nullable
    @Override
    public MFile create(String location) throws IOException {
      try {
        return new MFileS3(location);
      } catch (IOException ioe) {
        throw new IOException("Error creating MFileS3 for " + location, ioe);
      }
    }
  }
}
