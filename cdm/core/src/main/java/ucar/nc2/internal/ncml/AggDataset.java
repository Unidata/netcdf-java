/* Copyright Unidata */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.jdom2.Element;
import thredds.inventory.MFile;
import thredds.inventory.MFiles;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.CancelTask;

/** Encapsolates a NetcdfFile, used by AggProxyReader. */
@Immutable
public class AggDataset implements Comparable<AggDataset> {
  private static final boolean debugOpenFile = false;
  private static final boolean debugRead = false;

  private final MFile mfile;
  private final Set<Enhance> enhance; // used by Fmrc to read enhanced datasets
  @Nullable
  protected final String cacheLocation;
  @Nullable
  private final String id; // id attribute on the netcdf element
  @Nullable
  private final Element ncmlElem;
  @Nullable
  protected final ucar.nc2.internal.cache.FileFactory reader;
  @Nullable
  protected final Object spiObject; // pass to NetcdfFiles.open()

  // deferred opening LOOK
  protected DatasetUrl durl;

  protected AggDataset(MFile mfile, @Nullable Object spiObject, @Nullable Element ncmlElem) {
    this.mfile = mfile;
    this.cacheLocation = mfile.getPath();
    Set<NetcdfDataset.Enhance> wantEnhance = (Set<NetcdfDataset.Enhance>) mfile.getAuxInfo();
    this.enhance = (wantEnhance == null) ? NetcdfDataset.getEnhanceNone() : wantEnhance;
    this.id = null;
    this.reader = null;
    this.spiObject = spiObject;
    this.ncmlElem = ncmlElem;
  }

  /**
   * Dataset constructor.
   * With this constructor, the actual opening of the dataset is deferred, and done by the reader.
   * Used with explicit netcdf elements, and scanned files.
   *
   * @param cacheLocation a unique name to use for caching
   * @param location attribute "location" on the netcdf element
   * @param id attribute "id" on the netcdf element
   * @param wantEnhance open dataset in enhance mode, may be null
   * @param reader factory for reading this netcdf dataset; if null, use NetcdfDatasets.open( location)
   */
  protected AggDataset(String cacheLocation, String location, @Nullable String id,
      @Nullable EnumSet<Enhance> wantEnhance, @Nullable ucar.nc2.internal.cache.FileFactory reader,
      @Nullable Object spiObject, @Nullable Element ncmlElem) {
    this.mfile = MFiles.create(location); // may be null
    this.cacheLocation = cacheLocation;
    this.id = id;
    this.enhance = (wantEnhance == null) ? NetcdfDataset.getEnhanceNone() : wantEnhance;
    this.reader = reader;
    this.spiObject = spiObject;
    this.ncmlElem = ncmlElem;
  }

  public String getLocation() {
    return (mfile == null) ? cacheLocation : mfile.getPath();
  }

  public MFile getMFile() {
    return mfile;
  }

  public String getCacheLocation() {
    return cacheLocation;
  }

  public String getId() {
    if (id != null)
      return id;
    if (mfile != null)
      return mfile.getPath();
    return Integer.toString(this.hashCode());
  }

  public NetcdfFile acquireFile(CancelTask cancelTask) throws IOException {
    if (debugOpenFile)
      System.out.println(" try to acquire " + cacheLocation);
    long start = System.currentTimeMillis();

    if (durl == null) {
      // cache the ServiceType so we dont have to keep figuring it out
      durl = DatasetUrl.findDatasetUrl(cacheLocation);
    }

    NetcdfFile ncfile = NetcdfDatasets.acquireFile(reader, null, durl, -1, cancelTask, spiObject);
    if (ncmlElem == null && (enhance.isEmpty()))
      return ncfile;

    NetcdfDataset.Builder<?> builder = NcmlReader.mergeNcml(ncfile, ncmlElem); // create new dataset
    builder.setEnhanceMode(enhance);

    if (debugOpenFile)
      System.out.println(" acquire (enhance) " + cacheLocation + " took " + (System.currentTimeMillis() - start));
    return builder.build();
  }

  protected void close(NetcdfFile ncfile) throws IOException {
    if (ncfile == null)
      return;
    cacheVariables(ncfile);
    ncfile.close();
  }

  // overridden in DatasetOuterDimension
  protected void cacheVariables(NetcdfFile ncfile) throws IOException {}

  public void show(Formatter f) {
    f.format("   %s%n", mfile.getPath());
  }

  protected Array read(Variable mainv, CancelTask cancelTask) throws IOException {
    NetcdfFile ncd = null;
    try {
      ncd = acquireFile(cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Variable v = findVariable(ncd, mainv);
      if (debugRead)
        System.out.printf("Agg.read %s from %s in %s%n", mainv.getNameAndDimensions(), v.getNameAndDimensions(),
            getLocation());
      return v.read();

    } finally {
      close(ncd);
    }
  }

  /**
   * Read a section of the local Variable.
   *
   * @param mainv aggregated Variable
   * @param cancelTask let user cancel
   * @param section reletive to the local Variable
   * @return the complete Array for mainv
   * @throws IOException on I/O error
   * @throws InvalidRangeException on section error
   */
  protected Array read(Variable mainv, CancelTask cancelTask, List<Range> section)
      throws IOException, InvalidRangeException {
    NetcdfFile ncd = null;
    try {
      ncd = acquireFile(cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Variable v = findVariable(ncd, mainv);
      if (debugRead) {
        Section want = new Section(section);
        System.out.printf("Agg.read(%s) %s from %s in %s%n", want, mainv.getNameAndDimensions(),
            v.getNameAndDimensions(), getLocation());
      }

      return v.read(section);

    } finally {
      close(ncd);
    }
  }

  protected Variable findVariable(NetcdfFile ncfile, Variable mainV) {
    Variable v = ncfile.findVariable(NetcdfFiles.makeFullName(mainV));
    if (v == null && mainV instanceof VariableDS) { // might be renamed
      VariableDS ve = (VariableDS) mainV;
      v = ncfile.findVariable(ve.getOriginalName()); // LOOK not escaped
    }
    return v;
  }

  // Datasets with the same locations are equal
  @Override
  public boolean equals(Object oo) {
    if (this == oo)
      return true;
    if (!(oo instanceof AggDataset))
      return false;
    AggDataset other = (AggDataset) oo;
    return getLocation().equals(other.getLocation());
  }

  @Override
  public int hashCode() {
    return getLocation().hashCode();
  }

  @Override
  public int compareTo(AggDataset o) {
    return getLocation().compareTo(o.getLocation());
  }
}
