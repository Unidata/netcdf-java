/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.gempak;


import com.google.common.base.MoreObjects;

/**
 * Class to hold the Grid Analysis Block
 *
 * @author IDV Development Team
 */
public class AnalysisBlock {

    /** raw values */
    float[] vals = null;

    /**
     * Create a new analysis block
     */
    public AnalysisBlock() {}

    /**
     * Create a new analysis block with the values
     *
     * @param words   analysis block values
     */
    public AnalysisBlock(float[] words) {
        setValues(words);
    }

    /**
     * Set the analysis block values
     *
     * @param values   the raw values
     */
    public void setValues(float[] values) {
        vals = values;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("vals", vals)
            .toString();
    }
}

