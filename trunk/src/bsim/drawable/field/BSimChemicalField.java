/**
 * BSimChemicalField.java
 *
 * Class to hold a chemical field. Two type are available, fixed and difuse. Fixed fields
 * will merely remain static and have a use for the goal chemoattractant. Difuse fields
 * will alter over time with concentrtions spreading out over the field. Boundary
 * conditions are specified by the user and allow for removal of chemical (by difusion out
 * the area) or conservation (keeping all chemicals within the field area).
 *
 * Authors: Thomas Gorochowski
 * 			Mattia Fazzini(Update)
 * 			Antoni Matyjaszkiewicz (Update)
 * Created: 20/07/2008
 * Updated: 26/08/2009
 */
package bsim.drawable.field;

import java.awt.Color;
import java.awt.Graphics;

import bsim.BSimParameters;
import bsim.drawable.BSimDrawable;


public class BSimChemicalField implements BSimDrawable {

	// The type of field
	// TYPE_FIXED  = fixed field (no diffusion)
	// TYPE_DIFUSE = diffuses
	public static int TYPE_FIXED   = 1;
	public static int TYPE_DIFFUSE = 2;
	protected int fieldType        = TYPE_FIXED;

	// The boundary type
	public static int BOUNDARY_LEAK     = 1;
	public static int BOUNDARY_CONSERVE = 2;
	protected int boundaryType          = BOUNDARY_LEAK;

	// The discrete 2D field holding concentration values
	protected double[][][] field;

	// The diffusion rate between field elements (this is per unit squared area)
	// IMPORTANT: Field blocks may not be square in shape so rate must be altered 
	//            for each side
	protected double rate = 0.0;

	// Position of the field in the simulation space
	protected double[] startPos;
	protected double width, height, depth;

	// Number of discrete divisions along each axis
	protected int xBoxes, yBoxes, zBoxes;
	protected double boxWidth, boxHeight , boxDepth;

	// Time step length
	protected double dt;

	// Colour of the field when it is displayed
	protected Color colour;

	// Maximum concentration (should be less than 1 as this will be the alpha value)
	protected float maxCon = 0.3f;

	// Flag stating whether the field is displayed
	protected boolean isDisplayed = true;

	// The direction of a linear field (constants)
	public static int LINEAR_X = 1;
	public static int LINEAR_Y = 2;
	public static int LINEAR_Z = 3;

	// The type of diffusion used (if required)
	public static int DIFFUSE_X2  = 1;
	public static int DIFFUSE_EXP = 2;
	protected int diffuseType     = DIFFUSE_X2;
	
	// The minimum change in concentration that bacteria can detect 
	protected double threshold;
	
	// Parameters for the simulation
	protected BSimParameters params;
	
	// Max number of threads to create
	protected int MAX_WORKER_THREADS = 2;


	/**
	 * General constructor.
	 */
	public BSimChemicalField (int newFieldType, int newBoundaryType, double newRate, 
			double[] newStartPos, double newWidth, double newHeight, double newDepth, int newXBoxes,
			int newYBoxes, int newZBoxes, double newDt, double newThreshold, Color newColour,
			BSimParameters p){

		// Set all internal variables
		
		params = p;
		MAX_WORKER_THREADS = params.getNumOfThreads();

		fieldType = newFieldType;
		boundaryType = newBoundaryType;
		rate = newRate;

		startPos = newStartPos;
		width = newWidth;
		height = newHeight;
		depth = newDepth;

		xBoxes = newXBoxes;
		yBoxes = newYBoxes;
		zBoxes = newZBoxes;

		boxWidth  = width/xBoxes;
		boxHeight = height/yBoxes;
		boxDepth = height/zBoxes;

		dt = newDt;
		colour = newColour;
		
		threshold = newThreshold;

		// Create the field of the required size (this is fixed for the duration of the object)
		field = new double[xBoxes][yBoxes][zBoxes];
	}


	/**
	 * Setup the field as linear. Direction should use one of the constants
	 * defined in this class. All concentrations should be positive or will be rounded
	 * to zero. The gradient is linear between start and end concentrations.
	 */
	public void setAsLinear (int dir, double startCon, double endCon){
		int i, j, k;

		// Variable to hold the value of each box along the gradient
		double[] linVals;

		// Check that the concentration values are positive (if not set or zero)
		if(startCon < 0){ startCon = 0;}
		if(endCon < 0){ endCon = 0;}


		// If an X directional field
		if(dir == LINEAR_X){

			// We will have x boxes worth of values
			linVals = new double[xBoxes];

			// Calculate the difference in concentration between boxes
			double conDelta = (endCon - startCon) / (double)xBoxes;

			// Find the concentration for each box
			for(i=0; i<xBoxes; i++){
				// Calculate the concentration for the current box
				linVals[i] = startCon + (i * conDelta);
			}

			// Update the field
			for(i=0; i<xBoxes; i++){
				for(j=0; j<yBoxes; j++){
					for(k=0; k<zBoxes; k++)
					{
						// Update the field with the calculated value
						field[i][j][k] = linVals[i];
					}
				}
			}
		}
		// If a Y directional field
		else if(dir == LINEAR_Y){

			// We will have y boxes worth of values
			linVals = new double[yBoxes];

			// Calculate the difference in concentration between boxes
			double conDelta = (endCon - startCon) / (double)yBoxes;

			// Find the concentration for each box
			for(i=0; i<yBoxes; i++){
				// Calculate the concentration for the current box
				linVals[i] = startCon + (i * conDelta);
			}

			// Update the field
			for(i=0; i<xBoxes; i++){
				for(j=0; j<yBoxes; j++){
					for(k=0; k<zBoxes; k++)
					{
						// Update the field with the calculated value
						field[i][j][k] = linVals[j];
					}
				}
			}
		}
		// If a Z directional field
		else if(dir == LINEAR_Z){
	
				// We will have z boxes worth of values
				linVals = new double[zBoxes];
	
				// Calculate the difference in concentration between boxes
				double conDelta = (endCon - startCon) / (double)zBoxes;
	
				// Find the concentration for each box
				for(i=0; i<zBoxes; i++){
					// Calculate the concentration for the current box
					linVals[i] = startCon + (i * conDelta);
				}
	
				// Update the field
				for(i=0; i<xBoxes; i++){
					for(j=0; j<yBoxes; j++){
						for(k=0; k<zBoxes; k++)
						{
							// Update the field with the calculated value
							field[i][j][k] = linVals[k];
						}
					}
				}
			}
		// An invalid direction must have been given
		else{
			System.err.println("Invalid direction for static linear field.");
		}
	}


	/**
	 * Set if the field should be displayed.
	 */
	public void setDisplayed (boolean newIsDisplayed){ 
		isDisplayed = newIsDisplayed; 
	}


	/**
	 * Set the diffusion scheme used.
	 */
	public void setDiffuseType (int newDiffuseType){
		diffuseType = newDiffuseType;
	}


	/**
	 * Draw the field to a given graphics context.
	 */
	public void redraw (Graphics g) {
	}


	/**
	 * Update the field for a single time step.
	 */
	public void updateField (){
		int i;
		int xStart, xEnd;

		// Update rules for diffuse fields
		if(fieldType == TYPE_DIFFUSE){

			// Array to hold the updated field
			double[][][] newField = new double[xBoxes][yBoxes][zBoxes];

			// Ratios of each face (for weighting, used later but only calculated once)
			// Still not sure about this
			double xRat = (width  * 3) / (width + height + depth);
			double yRat = (height * 3) / (width + height + depth);
			double zRat = (depth * 3) / (width + height + depth);
			
			// Create array of worker threads
			Thread[] workerThreads = new Thread[MAX_WORKER_THREADS];
			
			// Create each of the worker threads and set them runing.
			for(i=0; i<MAX_WORKER_THREADS; i++) {
				
				// Calculate the start and end indexes for the partition
				xStart = (int)(xBoxes / MAX_WORKER_THREADS) * i;
				if (i == MAX_WORKER_THREADS - 1) {
					xEnd = xBoxes;
				}
				else {
					xEnd = (xBoxes / MAX_WORKER_THREADS) * (i + 1);
				}
				
				// Create and start the actual threads with the required parameters
				workerThreads[i] = new BSimChemicalFieldThread(newField, xRat, yRat, zRat, xStart, xEnd);
				workerThreads[i].start();
			}
			
			// Wait for all threads to finish execution before continuing
			for(i=0; i<MAX_WORKER_THREADS; i++) {
				try{
					workerThreads[i].join();
				} catch (InterruptedException ignore) { }
			}

			// Now that the new field has been claculated swap for old one
			field = newField;
		}
	}
	
	
	/**
	 * Worker thread used to calculate the diffusion over a partition of the full
	 * space.
	 */
	protected class BSimChemicalFieldThread extends Thread {
		
		
		// The field to work on (this will be a reference because arrays are objects)
		// This means a single instance of the array will by all the threads.
		double[][][] newField;
		
		// Ratios of the faces
		double xRat, yRat, zRat;
		
		// The thread number and total number of threads
		int xStart, xEnd;
		
		
		/**
		 * General constructor.
		 * 
		 */
		public BSimChemicalFieldThread(double[][][] newNewField, double newXRat,
		                               double newYRat, double newZRat, int newXStart, int newXEnd){
			
			// Update local values for the worker thread to work over
			newField = newNewField;
			xRat = newXRat;
			yRat = newYRat;
			zRat = newZRat;
			xStart = newXStart;
			xEnd = newXEnd;
		}
		
		
		/**
		 * Function run when thread starts. Updates the progress monitor window.
		 */
		public void run(){
			
			// Variables used in calculating the amount of diffusion
			double curVal, curDelta, 
			valN, valE, valS, valW, valU, valD, 
			dN, dE, dS, dW, dU, dD;
					
			// Flags to handle boundaries
			boolean noN, noE, noS, noW, noU, noD;

			// Loop through all x y and z boxes
			for(int x=xStart; x<xEnd; x++) {
				for(int y=0; y<yBoxes; y++) {
					for(int z=0;z<zBoxes; z++){
					
					// Reset the edge constraints
					noN = false;
					noE = false;
					noS = false;
					noW = false;
					//u means up
					noU = false;
					//D means down
					noD = false;

					// Reset other variables
					valN = 0.0;
					valE = 0.0;
					valS = 0.0;
					valW = 0.0;
					valU = 0.0;
					valD = 0.0;
					
					dN = 0.0;
					dE = 0.0;
					dS = 0.0;
					dW = 0.0;
					dU = 0.0;
					dD = 0.0;

					// Get the current value of the field at the given box
					curVal = field[x][y][z];

					// Calculate the diffusion coefficients for each face and
					// update the boolean flags if adjacent boxes are not present
					
					if(z == 0){ 
						noU = true;
						dU = diffuseCoeff(curVal, 0);
					}
					else{ 
						valU = field[x][y][z-1];
						dU = diffuseCoeff(curVal, valU); 
					}

					if(z == zBoxes-1){ 
						noD = true;
						dD = diffuseCoeff(curVal, 0);
					}
					else{
						valD = field[x][y][z+1];
						dD = diffuseCoeff(curVal, valD); 
					}
					
					if(y == 0){ 
						noN = true;
						dN = diffuseCoeff(curVal, 0);
					}
					else{ 
						valN = field[x][y-1][z];
						dN = diffuseCoeff(curVal, valN); 
					}

					if(y == yBoxes-1){ 
						noS = true;
						dS = diffuseCoeff(curVal, 0);
					}
					else{
						valS = field[x][y+1][z];
						dS = diffuseCoeff(curVal, valS); 
					}

					if(x == 0){ 
						noW = true;
						dW = diffuseCoeff(curVal, 0);
					}
					else{ 
						valW = field[x-1][y][z];
						dW = diffuseCoeff(curVal, valW); 
					}

					if(x == xBoxes-1){ 
						noE = true;
						dE = diffuseCoeff(curVal, 0);
					}
					else{ 
						valE = field[x+1][y][z];
						dE = diffuseCoeff(curVal, valE); 
					}

					// Reset the current delta
					curDelta = 0.0;

					// Weight the face deltas based on their area	
					// Required because fields may not be uniform in structure
					dN = dN * xRat;
					dS = dS * xRat;
					dE = dE * yRat;
					dW = dW * yRat;
					dU = dU * zRat;
					dD = dD * zRat;

					// Add up the face contributions
					if(boundaryType == BOUNDARY_LEAK){
						curDelta = dN * (curVal - valN) + 
								   dE * (curVal - valE) +
								   dS * (curVal - valS) +
								   dW * (curVal - valW) +
								   dU * (curVal - valU) +
								   dD * (curVal - valD);
					}
					else{
						if(!noN){
							curDelta += dN * (curVal - valN);
						}
						if(!noE){
							curDelta += dE * (curVal - valE);
						}
						if(!noS){
							curDelta += dS * (curVal - valS);
						}
						if(!noW){
							curDelta += dW * (curVal - valW);
						}
						if(!noU){
							curDelta += dU * (curVal - valU);
						}
						if(!noD){
							curDelta += dD * (curVal - valD);
						}
					}

					// Update the new field
					newField[x][y][z] = curVal - (dt * curDelta);
				}
				}
			}
		}
		
		// Calculate diffusion coefficients
		private double diffuseCoeff(double val1, double val2) {

			// Variable to hold the calculated diffusion coefficient
			double diffCoeff = 0.0;

			// Check to see if the values are 0 (minimise computation)
			if(val1 == 0.0 && val2 == 0.0){
				return diffCoeff;
			}

			// The different diffusion methods (DIFFUSE_X2 is the default)
			if (diffuseType == DIFFUSE_X2) {
				diffCoeff = (rate * rate) /
				((rate * rate) + Math.pow(Math.abs(val1 - val2),2));
			}
			else if (diffuseType == DIFFUSE_EXP) {
				diffCoeff = Math.exp(-1 * Math.pow((Math.abs(val1 - val2) / rate),2));
			}
			else {
				diffCoeff = 0.0;
			}

			// Return the diffusion coefficient
			return diffCoeff;
		}
	}


	/**
	 * Add an amount of chemical to a given point in simulation space.
	 * The amount is the increased concentration, this can be greater than 1
	 * if your field boxes are greater than a unit square.
	 */
	public void addChemical (double amount, double[] position){

		// Variable to hold the found concentration
		double con, newCon;

		// Check to see if the position falls in the field
		if(position[0]<startPos[0] || position[1]<startPos[1] || position[2]<startPos[2] ||
				position[0]>(startPos[0] + width) || position[1]>(startPos[1] + height)  || position[2]>(startPos[2] + depth)) {
			// Outside the bound of the field so do nothing
		}
		else{

			// Find the box that the position falls in and get the concentration
			int xNum = (int)((position[0] - startPos[0])/boxWidth);
			int yNum = (int)((position[1] - startPos[1])/boxHeight);
			int zNum = (int)((position[2] - startPos[2])/boxDepth);
			con = field[xNum][yNum][zNum];

			// Weight the new concentration by the volume of the box
			//not sure about this
			newCon = con + amount/(width/height/depth);

			// Ensure that concentration does not exceed 1
			if(newCon > 1.0) {
				newCon = 1.0;
			}

			// Update the field
			field[xNum][yNum][zNum] = newCon;
		}
	}

	
	/**
	 * Take an amount of chemical from a given point in simulation space.
	 * Useful if, for example, the chemical has diffused into a cell.
	 * 
	 * NOTE: perhaps this could be incorporated into addChemical instead?
	 */
	public void removeChemical (double amount, double[] position){

		// Variable to hold the found concentration
		double con, newCon;

		// Check to see if the position falls in the field
		if(position[0]<startPos[0] || position[1]<startPos[1] || position[2]<startPos[2] ||
				position[0]>(startPos[0] + width) || position[1]>(startPos[1] + height)  || position[2]>(startPos[2] + depth)) {
			// Outside the bound of the field so do nothing
		}
		else{

			// Find the box that the position falls in and get the concentration
			int xNum = (int)((position[0] - startPos[0])/boxWidth);
			int yNum = (int)((position[1] - startPos[1])/boxHeight);
			int zNum = (int)((position[2] - startPos[2])/boxDepth);
			con = field[xNum][yNum][zNum];

			// Weight the new concentration by the volume of the box
			//not sure about this
			newCon = con - amount/(width/height/depth);

			// Ensure that concentration does not exceed 1
			if(newCon < 0.0) {
				newCon = 0.0;
			}

			// Update the field
			field[xNum][yNum][zNum] = newCon;
		}
	}

	/**
	 * Get the concentration at a given point. The co-ordinates are of the simulation
	 * space and therefore if they fall outside the range of the field zero will be
	 * returned.
	 */
	public double getConcentration (double[] position) {

		// Variable to hold the found concentration
		double con;

		// Check to see if the position falls in the field
		if(position[0]<startPos[0] || position[1]<startPos[1] || position[2]<startPos[2] ||
				position[0]>(startPos[0] + width) || position[1]>(startPos[1] + height) || position[2]>(startPos[2] + depth)) {

			// Outside the bound of the field so return 0
			con = 0.0;
		}
		else{

			// Find the square that the position falls in and return concentration
			int xNum = (int)((position[0] - startPos[0])/boxWidth);
			int yNum = (int)((position[1] - startPos[1])/boxHeight);
			int zNum = (int)((position[2] - startPos[2])/boxDepth);
			con = field[xNum][yNum][zNum];
		}

		// Return the concentration
		return con;
	}


	/**
	 * Standard get methods for the class.
	 */
	public int getFieldType (){ return fieldType; }
	public int getBoundaryType (){ return boundaryType; }
	public double getRate (){ return rate; }
	public double[][][] getField (){ return field; }
	public double getThreshold() {return threshold;}
	public boolean getDisplayed() {return isDisplayed;}
}
