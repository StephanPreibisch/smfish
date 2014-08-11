package net.imglib2.scalespace;

import ij.ImageJ;

import java.util.ArrayList;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.smfish.TestCUDA;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import spim.process.cuda.CUDADevice;
import spim.process.cuda.CUDASeparableConvolution;
import spim.process.cuda.CUDASeparableConvolutionFunctions;
import spim.process.cuda.CUDATools;

public class TestScaleSpace
{
	public TestScaleSpace( final CUDASeparableConvolutionFunctions cuda )
	{
		new ImageJ();
		final Random rnd = new Random();
		
		final Img< FloatType > img = ArrayImgs.floats( 512, 512, 256 );
		
		final double[] l = new double[ img.numDimensions() ];
		final double[] s = new double[ img.numDimensions() ];

		for ( int i = 0; i < 100; ++i )
		{
			s[ 0 ] = ( rnd.nextDouble() + 1 ) * 3;
			
			for ( int d = 0; d < img.numDimensions(); ++d )
			{
				l[ d ] = rnd.nextDouble() * img.dimension( d );
				s[ d ] = s[ 0 ];
			}
			addGaussian( img, l, s );
		}

		ImageJFunctions.show( img );
	}
	
	final public static void addGaussian( final Img< FloatType > image, final double[] location, final double[] sigma )
	{
		final int numDimensions = image.numDimensions();
		final int[] size = new int[ numDimensions ];
		
		final long[] min = new long[ numDimensions ];
		final long[] max = new long[ numDimensions ];
		
		final double[] two_sq_sigma = new double[ numDimensions ];
		
		for ( int d = 0; d < numDimensions; ++d )
		{
			size[ d ] = Util.getSuggestedKernelDiameter( sigma[ d ] ) * 2;
			min[ d ] = (int)Math.round( location[ d ] ) - size[ d ]/2;
			max[ d ] = min[ d ] + size[ d ] - 1;
			two_sq_sigma[ d ] = 2 * sigma[ d ] * sigma[ d ];
		}

		final RandomAccessible< FloatType > infinite = Views.extendZero( image );
		final RandomAccessibleInterval< FloatType > interval = Views.interval( infinite, min, max );
		final IterableInterval< FloatType > iterable = Views.iterable( interval );
		final Cursor< FloatType > cursor = iterable.localizingCursor();
		
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			
			double value = 1;
			
			for ( int d = 0; d < numDimensions; ++d )
			{
				final double x = location[ d ] - cursor.getIntPosition( d );
				value *= Math.exp( -(x * x) / two_sq_sigma[ d ] );
			}
			
			cursor.get().set( cursor.get().get() + (float)value );
		}
	}

	public static void main( String[] args )
	{
		final CUDASeparableConvolution cuda = TestCUDA.loadCUDA();
		ArrayList< CUDADevice > dev = CUDATools.queryCUDADetails( cuda, false );

		if ( dev == null )
		{
			dev = new ArrayList< CUDADevice >();
			dev.add( new CUDADevice( -1, "CPU", Runtime.getRuntime().maxMemory(), 0, 0 ) );
		}
		
		new TestScaleSpace( new CUDASeparableConvolutionFunctions( cuda, dev.get( 0 ).getDeviceId() ) );
	}
}
