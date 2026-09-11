/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.filesystem.zarr;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import thredds.inventory.CollectionConfig;
import thredds.inventory.MFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestControllerZip {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  private static File zipFile, zipFileBad;

  @BeforeClass
  public static void setUp() throws IOException {
    zipFile = tempFolder.newFile("test.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
      // file1 (top level)
      zos.putNextEntry(new ZipEntry("file1"));
      zos.write("content1".getBytes());
      zos.closeEntry();

      // dir1/file2
      zos.putNextEntry(new ZipEntry("dir1/file2"));
      zos.write("content2".getBytes());
      zos.closeEntry();

      // dir1/file3
      zos.putNextEntry(new ZipEntry("dir1/file3"));
      zos.write("content3".getBytes());
      zos.closeEntry();
    }

    zipFileBad = tempFolder.newFile("test_bad.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFileBad.toPath()))) {
      // good entries
      zos.putNextEntry(new ZipEntry("dir1/file_good1"));
      zos.write("content1".getBytes());
      zos.closeEntry();

      zos.putNextEntry(new ZipEntry("dir1/file_good2"));
      zos.write("content2".getBytes());
      zos.closeEntry();

      // bad entries
      // reference outside of zip
      zos.putNextEntry(new ZipEntry("../../file_bad"));
      zos.write("content3".getBytes());
      zos.closeEntry();

      // reference outside of zip
      zos.putNextEntry(new ZipEntry("dir3/../../file_bad2"));
      zos.write("content4".getBytes());
      zos.closeEntry();
    }
  }

  @Test
  public void testFilteredIteratorFiles() throws IOException {
    ControllerZip controller = new ControllerZip();
    CollectionConfig mc = new CollectionConfig("test", zipFile.getAbsolutePath(), false, null, null);
    try (DirectoryStream<MFile> stream = controller.getInventoryTop(mc, false)) {
      assertThat(stream).isNotNull();
      List<String> names = new ArrayList<>();
      for (MFile mfile : stream) {
        names.add(mfile.getName());
      }
      // only one item in the top level of the zip
      assertThat(names).containsExactly(File.separator + "file1");
    }
  }

  @Test
  public void testFilteredIteratorDirs() throws IOException {
    ControllerZip controller = new ControllerZip();
    CollectionConfig mc = new CollectionConfig("test", zipFile.getAbsolutePath(), false, null, null);
    try (DirectoryStream<MFile> stream = controller.getSubdirs(mc, false)) {
      assertThat(stream).isNotNull();
      List<String> names = new ArrayList<>();
      for (MFile mfile : stream) {
        names.add(mfile.getName());
      }
      assertThat(names).containsExactly(File.separator + "dir1");
    }
  }

  @Test
  public void testFilteredFilesBad() throws IOException {
    ControllerZip controller = new ControllerZip();
    CollectionConfig mc = new CollectionConfig("test", zipFileBad.getAbsolutePath(), false, null, null);
    try (DirectoryStream<MFile> stream = controller.getInventoryAll(mc, false)) {
      assertThat(stream).isNotNull();
      List<String> names = new ArrayList<>();
      for (MFile mfile : stream) {
        names.add(mfile.getName());
      }
      assertThat(names).containsExactlyElementsIn(
          Arrays.asList(File.separator + "dir1/file_good1", File.separator + "dir1/file_good2"));
    }
  }

  @Test
  public void testFilteredIteratorDirsBad() throws IOException {
    ControllerZip controller = new ControllerZip();
    CollectionConfig mc = new CollectionConfig("test", zipFileBad.getAbsolutePath(), false, null, null);
    try (DirectoryStream<MFile> stream = controller.getSubdirs(mc, false)) {
      assertThat(stream).isNotNull();
      List<String> names = new ArrayList<>();
      for (MFile mfile : stream) {
        names.add(mfile.getName());
      }
      assertThat(names).containsExactlyElementsIn(Collections.singletonList(File.separator + "dir1"));
    }
  }
}
