package net.imglib2.drift;

import ij.CompositeImage;
import ij.ImageJ;
import ij.ImagePlus;
import mpicbg.models.TranslationModel2D;
import plugin.DescriptorParameters;
import process.Matching;

public class CorrectDrift
{
	public CorrectDrift()
	{
		ImagePlus imp = new ImagePlus( "/Volumes/My Passport/confocal/worm3.zip" );

		final int numChannels = 2;
		final int numTimePoints = imp.getStackSize() / numChannels;
		imp.setDimensions( numChannels, 1, numTimePoints );
		imp = makeComposite( imp, CompositeImage.COMPOSITE );

		imp.show();
		Matching.descriptorBasedStackRegistration( imp, getParameters( 1 ) );
	}
	
	protected DescriptorParameters getParameters( final int dapichannel )
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
		params.significance = 3;
		params.ransacThreshold = 3;
		params.channel1 = dapichannel + 1;
		params.channel2 = dapichannel + 1;
		
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

	public static CompositeImage makeComposite( final ImagePlus imp, final int mode )
	{
		// cache the (correct) channel, frame and slice counts
		final int channels = imp.getNChannels();
		final int frames = imp.getNFrames();
		final int slices = imp.getNSlices();

		// construct the composite image
		final CompositeImage cmp = new CompositeImage( imp, mode );

		// reset the correct dimension counts
		cmp.setDimensions( channels, slices, frames );

		return cmp;
	}

	public static void main( String[] args )
	{
		new ImageJ();
		new CorrectDrift();
	}
}
