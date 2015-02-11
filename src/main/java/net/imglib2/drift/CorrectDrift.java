package net.imglib2.drift;

import mpicbg.models.AbstractModel;
import mpicbg.models.RigidModel2D;
import mpicbg.models.TranslationModel2D;
import plugin.DescriptorParameters;

public class CorrectDrift
{

	protected DescriptorParameters getParametersForProjection()
	{
		final DescriptorParameters params = new DescriptorParameters();
		
		params.dimensionality = 2;
		params.sigma1 = 6.9450;
		params.sigma2 = 8.270341f;
		params.threshold = 0.014f;
		params.lookForMaxima = true;
		params.lookForMinima = true;

		params.model = new TranslationModel2D();

		params.similarOrientation = true;
		params.numNeighbors = 3;
		params.redundancy = 1;
		params.significance = 2;
		params.ransacThreshold = 5;
		params.channel1 = 1;
		params.channel2 = 1;
		
		// for stack-registration
		params.globalOpt = 1; // 0=all-to-all; 1=all-to-all-withrange; 2=all-to-1; 3=Consecutive
		params.range = 3;
		params.directory = "";
		
		params.reApply = false;
		params.roi1 = null;
		params.roi2 = null;
		
		params.setPointsRois = false;
		
		params.silent = false;
		
		// 0 == fuse in memory, 1 == write to disk, 2 == nothing
		params.fuse = 2;
		
		return params;
	}

}
