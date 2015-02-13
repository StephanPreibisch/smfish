package net.imglib2.drift;

import ij.CompositeImage;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import mpicbg.models.InvertibleBoundable;
import mpicbg.models.Point;
import mpicbg.models.TranslationModel2D;
import mpicbg.spim.io.TextFileAccess;
import net.imglib2.drift.fit.Line;
import net.imglib2.drift.fit.PointFunctionMatch;
import plugin.DescriptorParameters;
import process.Matching;

public class CorrectDrift
{
	public String driftExt = ".drift.txt";

	public CorrectDrift( final String dir, final String file, final int numChannels )
	{
		System.out.println( new File ( dir, file ).getAbsolutePath() );

		final File driftFile = new File ( dir, file + driftExt );

		if ( !driftFile.exists() )
			computeIndividualDrifts( dir, file, numChannels );

		final float[] driftsX = loadDrifts( driftFile, 1 );
		final float[] driftsY = loadDrifts( driftFile, 2 );

		final Line lx = removeOutliers( driftsX, 10, 0.5 );
		final Line ly = removeOutliers( driftsY, 10, 0.5 );

		final ArrayList< InvertibleBoundable > models = createModels( lx, ly, 0, driftsX.length - 1 );

		System.out.println( "z" + "\t" + "ransacdriftX" + "\t" + "ransacdriftY" + "\t" + "fitX" + "\t" + "fitY" );

		final float[] t = new float[ 2 ];
		for ( int z = 0; z < driftsX.length; ++z )
		{
			t[ 0 ] = t[ 1 ] = 0;
			models.get( z ).applyInPlace( t );

			System.out.println( z + "\t" + driftsX[ z ] + "\t" + driftsY[ z ] + "\t" + t[ 0 ] + "\t" + t[ 1 ] );
		}

		// fuse
		
	}

	public static ArrayList< InvertibleBoundable > createModels( final Line lx, final Line ly, final int from, final int to )
	{
		final ArrayList< InvertibleBoundable > list = new ArrayList< InvertibleBoundable >();

		for ( int z = from; z <= to; ++z )
		{
			final double x = lx.getM() * z + lx.getN();
			final double y = ly.getM() * z + ly.getN();

			final TranslationModel2D m = new TranslationModel2D();
			m.set( (float)x, (float)y );
			list.add( m );
		}

		return list;
	}

	public static Line removeOutliers( final float[] drift, final double epsilon, final double minInlierRatio )
	{
		final ArrayList< PointFunctionMatch > candidates = new ArrayList<PointFunctionMatch>();
		final ArrayList< PointFunctionMatch > inliers = new ArrayList<PointFunctionMatch>();

		for ( int z = 0; z < drift.length; ++ z )
			if ( drift[ z ] != 0 )
				candidates.add( new PointFunctionMatch( new Point( new float[]{ z, drift[ z ] } ) ) );

		int numRemoved = 0;

		try
		{
			final Line l = new Line();

			l.ransac( candidates, inliers, 1000, epsilon, minInlierRatio );
			numRemoved = candidates.size() - inliers.size();

			l.fit( inliers );

			// reset so that only ransac points are saved
			for ( int i = 0; i < drift.length; ++i )
				drift[ i ] = 0;

			for ( final PointFunctionMatch i : inliers )
				drift[ Math.round( i.getP1().getL()[ 0 ] ) ] = i.getP1().getL()[ 1 ];
				
			System.out.println( "y = " + l.getM() + " x + " + l.getN() + ", " + numRemoved + " points removed." );

			return l;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			numRemoved = drift.length;
			return null;
		}
	}

	protected float[] loadDrifts( final File driftFile, final int column )
	{
		final ArrayList< Float > drifts = new ArrayList< Float >();
		final BufferedReader in = TextFileAccess.openFileRead( driftFile );

		try
		{
			while ( in.ready() )
			{
				String[] values = in.readLine().trim().split( "\t" );
				drifts.add( Float.parseFloat( values[ column ] ) );
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}

		final float[] d = new float[ drifts.size() ];
		for ( int i = 0; i < d.length; ++i )
			d[ i ] = drifts.get( i );

		return d;
	}

	protected void computeIndividualDrifts( final String dir, final String file, final int numChannels )
	{
		ImagePlus imp = new ImagePlus( new File( dir, file ).getAbsolutePath() );
		
		final int numTimePoints = imp.getStackSize() / numChannels;
		imp.setDimensions( numChannels, 1, numTimePoints );
		imp = makeComposite( imp, CompositeImage.COMPOSITE );

		final ArrayList<InvertibleBoundable> models = Matching.descriptorBasedStackRegistration( imp, getParameters( 1 ) );
		final float[] t = new float[ 2 ];

		final PrintWriter out = TextFileAccess.openFileWrite( new File ( dir, file + driftExt ) );
		int z = 0;

		for ( final InvertibleBoundable model : models )
		{
			final TranslationModel2D m = (TranslationModel2D)model;
			t[ 0 ] = t[ 1 ] = 0;
			m.applyInPlace( t );
			
			out.println( ++z + "\t" + t[ 0 ] + "\t" + t[ 1 ] );
		}

		out.close();
		imp.close();
	}

	protected DescriptorParameters getParameters( final int dapichannel )
	{
		// same min/max for all timepoints
		DescriptorParameters.minMaxType = 1;

		// at least 20 corresponding features for a model
		DescriptorParameters.minInlierFactor = 10 / new TranslationModel2D().getMinNumMatches();

		final DescriptorParameters params = new DescriptorParameters();
		
		params.dimensionality = 2;
		params.sigma1 = 6.9450;
		params.sigma2 = 8.270341f;
		params.threshold = 0.01f;
		params.lookForMaxima = true;
		params.lookForMinima = false;

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

		final int numChannels = 2;
		//String dir = "/media/preibisch/data/Microscopy/confocal/TestAcquisitions";
		String  dir = "/Volumes/My Passport/confocal";

		for ( int i = 1; i <= 4; ++i )
			new CorrectDrift( dir, "worm" + i + ".zip", numChannels );
	}
}
