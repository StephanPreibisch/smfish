package net.imglib2.drift;

import mpicbg.models.AbstractModel;
import mpicbg.models.RigidModel2D;
import plugin.DescriptorParameters;

public class CorrectDrift
{

	protected DescriptorParameters getParametersForProjection( final AbstractModel< ? > model )
	{
		final DescriptorParameters params = new DescriptorParameters();
		
		params.dimensionality = 2;
		params.sigma1 = 2.99f; // before 2.099f
		params.sigma2 = 3.55f; // before 2.4961457f;
		params.threshold = 0.010566484f;
		params.lookForMaxima = true;
		params.lookForMinima = true;
		
		if ( model == null )
			params.model = new RigidModel2D();
		else
			params.model = model;
			
		params.similarOrientation = true;
		params.numNeighbors = 3;
		params.redundancy = 1;
		params.significance = 3;
		params.ransacThreshold = 5;
		params.channel1 = 0;
		params.channel2 = 0;
		
		// for stack-registration
		params.globalOpt = 0; // 0=all-to-all; 1=all-to-all-withrange; 2=all-to-1; 3=Consecutive
		params.range = 5;	
		params.directory = "";
		
		params.reApply = false;
		params.roi1 = null;
		params.roi2 = null;
		
		params.setPointsRois = true;
		
		params.silent = false;
		
		// 0 == fuse in memory, 1 == write to disk, 2 == nothing
		params.fuse = 2;
		
		return params;
	}

}
