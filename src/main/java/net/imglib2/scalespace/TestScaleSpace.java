package net.imglib2.scalespace;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.smfish.TestCUDA;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import spim.process.cuda.CUDASeparableConvolution;
import spim.process.cuda.CUDASeparableConvolutionFunctions;
import spim.process.cuda.CUDATools;

public class TestScaleSpace
{
	public TestScaleSpace( final CUDASeparableConvolutionFunctions cuda )
	{
		
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
		ArrayList< Integer > dev = CUDATools.queryCUDADetails( cuda, false );

		if ( dev == null )
		{
			dev = new ArrayList< Integer >();
			dev.add( -1 );
		}
		
		new TestScaleSpace( new CUDASeparableConvolutionFunctions( cuda, dev.get( 0 ) ) );
	}
}
