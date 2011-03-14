package BSimLogic;

import java.util.*;
import javax.vecmath.Vector3d;

import bsim.*;
import bsim.particle.*;

/**
 * NOT bacterium logic gate 
 */
class BSimNOTBacterium extends BSimLogicBacterium {
	// Redefine the constructor as only single input and no output into the chemical fields
	public BSimNOTBacterium(BSim sim, Vector3d position, BSimChemicalField chemIn, double threshold,
									double reporterDelay) {
		super(sim, position, chemIn, threshold, null, 0.0, null, 0.0, 0.0, reporterDelay);
	}
	
	@Override
	public void action() {				
		if(chemIn1.getConc(position) < threshold1) {
			activated = true;
			lastActivated = sim.getTime();
			if(lastInActivated == -1 || (sim.getTime() - lastInActivated) > productionDelay) {
				reporter = true;				
			}
		}
		else {
			activated = false;
			lastInActivated = sim.getTime();
			if(lastActivated == -1 || (sim.getTime() - lastActivated) > reporterDelay) reporter = false;
		}
	}
}
