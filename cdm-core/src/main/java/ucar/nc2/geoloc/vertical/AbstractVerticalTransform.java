/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/** Superclass for implementations of a VerticalTransform. */
@Immutable
abstract class AbstractVerticalTransform implements VerticalTransform {

  static Array<Number> readArray(NetcdfDataset ncd, String varName) throws IOException {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    return (Array<Number>) v.readArray();
  }

  static double readScalarDouble(NetcdfDataset ncd, String varName) throws IOException {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    Array<Number> data = (Array<Number>) v.readArray();
    return data.getScalar().doubleValue();
  }

  static String getUnits(NetcdfDataset ncd, String varName) {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    return v.getUnitsString();
  }

  static int getRank(NetcdfDataset ncd, String varName) {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    return v.getRank();
  }

  static Array<Number> readArray(NetcdfDataset ncd, String varName, int timeIdx)
      throws IOException, InvalidRangeException {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    int[] shape = v.getShape();
    int[] origin = new int[v.getRank()];

    // assume the time dimension exists and is the outer dimension
    shape[0] = 1;
    origin[0] = timeIdx;

    Array<Number> data = (Array<Number>) v.readArray(new Section(origin, shape));
    return Arrays.reduce(data, 0);
  }

  //////////////////////////////////////////////////////////

  protected final NetcdfDataset ds;
  protected final String name;
  protected final String ctvName;
  protected final String units;

  AbstractVerticalTransform(NetcdfDataset ds, String name, String ctvName, String units) {
    this.ds = ds;
    this.name = name;
    this.ctvName = ctvName;
    this.units = units;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getCtvName() {
    return ctvName;
  }

  @Override
  @Nullable
  public String getUnitString() {
    return units;
  }

  // Implementation that reads the full 3D array and just picks out the specified x, y.
  // Optimization could cache at least one time index
  @Override
  public Array<Number> getCoordinateArray1D(int timeIndex, int xIndex, int yIndex)
      throws IOException, InvalidRangeException {
    Array<Number> array3D = getCoordinateArray3D(timeIndex);
    int nz = array3D.getShape()[0];
    double[] result = new double[nz];

    int count = 0;
    for (int z = 0; z < nz; z++) {
      result[count++] = array3D.get(z, yIndex, xIndex).doubleValue();
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz}, result);
  }

  @Override
  public AbstractVerticalTransform subset(Range t_range, Range z_range, Range y_range, Range x_range) {
    return new VerticalTransformSubset(this, t_range, z_range, y_range, x_range);
  }

  static String getFormula(AttributeContainer ctv, Formatter errlog) {
    String formula = ctv.findAttributeString("formula_terms", null);
    if (null == formula) {
      errlog.format("CoordTransBuilder %s: needs attribute 'formula_terms' on %s%n", ctv.getName(), ctv.getName());
      return null;
    }
    return formula;
  }

  static List<String> parseFormula(String formula_terms, String termString, Formatter errlog) {
    String[] formulaTerms = formula_terms.split("[\\s:]+"); // split on 1 or more whitespace or ':'
    Iterable<String> terms = StringUtil2.split(termString); // split on 1 or more whitespace
    List<String> values = new ArrayList<>();

    for (String term : terms) {
      for (int j = 0; j < formulaTerms.length; j += 2) { // look at every other formula term
        if (term.equals(formulaTerms[j])) { // if it matches
          values.add(formulaTerms[j + 1]); // next term is the value
          break;
        }
      }
    }
    return values;
  }

}

