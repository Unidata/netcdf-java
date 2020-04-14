
/*
This source file is part of the edu.wisc.ssec.mcidas package and is
Copyright (C) 1998 - 2020 by Tom Whittaker, Tommy Jasmin, Tom Rink,
Don Murray, James Kelly, Bill Hibbard, Dave Glowacki, Curtis Rueden
and others.
 
This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package ucar.mcidas;

import java.io.IOException;

/**
 * @author tommyj
 * Adapted from the McIDAS-X module kbxfy2.dlm
 * NOT TESTED YET, USE AT YOUR OWN RISK!
 *
 */

public class CalibratorFY2 extends CalibratorDefault implements Calibrator {

	private boolean isVis = false;
    /** 
     * Current cal type as set by <code>setCalType</code>
     */
    private int curCalType = Calibrator.CAL_RAW;
    private int numFY2Bands = 4;
    private int[] visDetectorId = { 0x6C6C0000, 0xB4B40000, 0xD8D80000, 0xFCFC0000 };
    private int[] prefix;
    private int[] calBlock;
    private static final int VIS_BAND_OFFSET = 192;
    private int[][] albedoFromRaw = new int[numFY2Bands][256];
    private int[][] britFromAlbedo = new int[numFY2Bands][256];
    private int[] tempTable = new int[1024];
    private int[] radTable = new int[1024];
    private int[] britTable = new int[1024];
    private int lastBand = -1;
	
	public CalibratorFY2(int[] prefix, int[] areaDir, int[] calBlock)
			throws IOException {
		super(null, null);
		if(calBlock != null)
			initFY2(prefix, areaDir, calBlock);
		else
			setIsPreCalibrated(true);
	}

	public void initFY2(int[] prefix, int[] areaDir, int[] calBlock){

		this.prefix = prefix;
		this.calBlock = calBlock;
		// initialize tables
		int visOffset = 0;
		float albedo = 0.0f;
		for (int i = 0; i < numFY2Bands; i++) {
			visOffset = VIS_BAND_OFFSET + (i * 64);
			for (int j = 0; j < 256; j+=4) {
				albedo = calBlock[visOffset + ((j + 1) / 4)] / 10000.0f;
				britFromAlbedo[i][j] = (int) Math.round(0.5 + 25.5 * Math.sqrt(albedo));
                albedoFromRaw[i][j] = (int) Math.round(albedo * 100.0f);

                britFromAlbedo[i][j + 1] = (int) Math.round(0.5 + 25.5 * Math.sqrt(albedo));
                albedoFromRaw[i][j + 1] = (int) Math.round(albedo * 100.0f);

                britFromAlbedo[i][j + 2] = (int) Math.round(0.5 + 25.5 * Math.sqrt(albedo));
                albedoFromRaw[i][j + 2] = (int) Math.round(albedo * 100.0f);

                britFromAlbedo[i][j + 3] = (int) Math.round(0.5 + 25.5 * Math.sqrt(albedo));
                albedoFromRaw[i][j + 3] = (int) Math.round(albedo * 100.0f);
			}
		}
	}

	public int[] calibratedList( final int band, final boolean isPreCal ) {
		int[] cList;

		if(isPreCal){
			if (band == 1) {
				// Visible
				cList = new int[]{CAL_RAW, CAL_BRIT};
			} else {
				// IR Channel
				cList = new int[]{CAL_RAW, CAL_TEMP, CAL_BRIT};
			}
		} else {
			if (band == 1) {
				// Visible
				cList = new int[]{CAL_RAW, CAL_ALB, CAL_BRIT};
			} else {
				// IR Channel
				cList = new int[]{CAL_RAW, CAL_TEMP, CAL_RAD, CAL_BRIT};
			}
		}

		return cList;
	}
	/* (non-Javadoc)
	 * @see edu.wisc.ssec.mcidas.CalibratorDefault#calibrate(float, int, int)
	 */
	@Override
	public float calibrate(float inVal, int band, int calTypeOut) {
		
		int detector = 0;
		int irOffset = 0;
		float radiance = 0.0f;
		float temperature = 0.0f;
		float outVal = 0.0f;
		
		// first set the vis-or-ir flag
		if (band == 1) {
			isVis = true;
		} else {
			isVis = false;
		}
		
		if (isVis) {
			// read the detector number from line prefix
			detector = 1;
			for (int i = 0; i < numFY2Bands; i++) {
				if (visDetectorId[i] == prefix[1]) {
					detector = i + 1;
				}
			}
		}
		
		// for IR data, we will generate new tables when the band changes
		if (! isVis) {
			if (band != lastBand) {
				irOffset = calBlock[(band - 2) * 2 + 8] / 4;
				for (int i = 0; i < 1024; i++) {
					temperature = calBlock[irOffset] / 1000.0f;
					radiance = tempToRad(temperature, band);
					tempTable[i] = Math.round(temperature * 100.f);
					radTable[i] = Math.round(radiance * 1000.f);
					if (temperature >= 242.0f) {
						britTable[i] = Math.max(660 - (int) (2 * temperature), 0);
					} else {
						britTable[i] = Math.min(418 - (int) (temperature), 255);
					}
				}
			}
		}
		
		// update last band seen
		lastBand = band;
		
		// finally, do the calibration
		if (calTypeOut == curCalType) {
			outVal = inVal;
		} else {
			if (isVis) {
				if (calTypeOut == Calibrator.CAL_ALB) {
					outVal = albedoFromRaw[detector][(int) inVal];
				}
				if (calTypeOut == Calibrator.CAL_BRIT) {
					outVal = britFromAlbedo[detector][(int) inVal];
				}
			} else {
				if (calTypeOut == Calibrator.CAL_RAD) {
					outVal = radTable[(int) inVal];
				}
				if (calTypeOut == Calibrator.CAL_TEMP) {
					outVal = tempTable[(int) inVal];
				}
				if (calTypeOut == Calibrator.CAL_BRIT) {
					outVal = britTable[(int) inVal];
				}
			}
		}
		
		return outVal;
		
	}
	
	/**
	 * 
	 * calibrate from temperature to radiance
	 * 
	 * @param inVal
	 *            input data value
	 * @param band
	 *            channel/band number
	 * 
	 */

	public float tempToRad(float inVal, int band) {

		float outVal = -1f;
		// derived constants for each band
		float[] fk1 = { 9280.38f, 7136.31f, 37258.20f, 224015.00f };
		float[] fk2 = { 1323.95f, 1212.95f, 2104.22f, 3826.28f };
	    // derived temp constants for each band
		float[] tc1 = { 0.72122f, 1.00668f, 3.76883f, 4.00279f };
		float[] tc2 = { 0.99750f, 0.99621f, 0.99108f, 0.99458f };
	    // temperature adjusted by derived constants
	    float adjustedTemp;

		adjustedTemp = tc1[band - 1] + tc2[band - 1] * inVal;
		outVal = (float) (fk1[band - 1] / 
				 (Math.exp(fk2[band - 1] / adjustedTemp) - 1.0f));

		return (outVal);
	}


	public String calibratedUnit(int calType){
		String unitStr = null;
		switch (calType) {

			case CAL_RAW:
				unitStr = null;
				break;

			case CAL_RAD:
				unitStr = "MW**";
				break;

			case CAL_ALB:
				unitStr = "%";
				break;

			case CAL_TEMP:
				unitStr = "K";
				break;

			case CAL_BRIT:
				unitStr = null;
				break;

		}

		return unitStr;

	}
}
