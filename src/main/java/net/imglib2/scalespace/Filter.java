package net.imglib2.scalespace;

import spim.process.cuda.CUDASeparableConvolutionFunctions;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;


/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 */
public class Filter
{
	/**
	 * Create a downsampled {@link FloatProcessor}.
	 * 
	 * @param source the source image
	 * @param scale scaling factor
	 * @param sourceSigma the Gaussian at which the source was sampled (guess 0.5 if you do not know)
	 * @param targetSigma the Gaussian at which the target will be sampled
	 * 
	 * @return a new {@link FloatProcessor}
	 */
	final static public FloatArray3D createDownsampled(
			final FloatArray3D source,
			final float scale,
			final float sourceSigma,
			final float targetSigma,
			final CUDASeparableConvolutionFunctions cuda )
	{
		assert scale <= 1.0f : "Downsampling requires a scale factor < 1.0";
		
		final int ow = source.width;
		final int oh = source.height;
		final int od = source.depth;
		final int w = Math.round( ow * scale );
		final int h = Math.round( oh * scale );
		final int d = Math.round( od * scale );
		final int l = Math.max( Math.max( w, h ), d );
		
		final FloatArray3D temp = source.clone();
		
		smoothForScale( temp, scale, sourceSigma, targetSigma, cuda );
		if ( scale == 1.0f ) return temp;
		
		final float[] tempPixels = temp.data;
		
		final FloatArray3D target = new FloatArray3D( w, h, d );
		final float[] targetPixels = target.data;
		
		/* LUT for scaled pixel locations */
		final int ow1 = ow - 1;
		final int oh1 = oh - 1;
		final int od1 = od - 1;
		
		final int[] lutx = new int[ l ];
		for ( int x = 0; x < w; ++x )
			lutx[ x ] = Math.min( ow1, Math.max( 0, Math.round( x / scale ) ) );
		
		final int[] luty = new int[ l ];
		for ( int y = 0; y < h; ++y )
			luty[ y ] = Math.min( oh1, Math.max( 0, Math.round( y / scale ) ) );
		
		final int[] lutz = new int[ l ];
		for ( int z = 0; z < d; ++z )
			lutz[ z ] = Math.min( od1, Math.max( 0, Math.round( z / scale ) ) );
		
		for ( int z = 0; z < d; ++z )
		{
			final int a = z * w * h;
			final int b = lutz[ z ] * ow * oh;
			
			for ( int y = 0; y < h; ++y )
			{
				final int p = y * w;
				final int q = luty[ y ] * ow;
				for ( int x = 0; x < w; ++x )
					targetPixels[ a + p + x ] = tempPixels[ b + q + lutx[ x ] ];
			}
		}

		return target;
	}

	/**
	 * Smooth with a Gaussian kernel that represents downsampling at a given
	 * scale factor and sourceSigma.
	 */
	final static public void smoothForScale(
			final FloatArray3D source,
			final float scale,
			final float sourceSigma,
			final float targetSigma,
			final CUDASeparableConvolutionFunctions cuda )
	{
		assert scale <= 1.0f : "Downsampling requires a scale factor < 1.0";
		
		final float s = targetSigma / scale;
		final float v = s * s - sourceSigma * sourceSigma;
		if ( v <= 0 )
			return;
		final float sigma = ( float )Math.sqrt( v );
		//final float[] kernel = createGaussianKernel( sigma, true );
		cuda.gauss( source.data, new int[]{ source.width, source.height, source.depth }, sigma );
//		convolveSeparable( source, kernel, kernel );
		//new GaussianBlur().blurFloat( source, sigma, sigma, 0.01 );
	}

	/**
     * Create a 1d-Gaussian kernel of appropriate size.
     *
     * @param sigma Standard deviation of the Gaussian kernel
     * @param normalize Normalize integral of the Gaussian kernel to 1 or not...
     * 
     * @return float[] Gaussian kernel of appropriate size
     */
    final static public float[] createGaussianKernel(
    		final float sigma,
    		final boolean normalize )
	{
		float[] kernel;

		if ( sigma <= 0 )
		{
			kernel = new float[ 3 ];
			kernel[ 1 ] = 1;
		}
		else
		{
			final int size = Math.max( 3, ( int ) ( 2 * ( int )( 3 * sigma + 0.5 ) + 1 ) );

			final float two_sq_sigma = 2 * sigma * sigma;
			kernel = new float[ size ];

			for ( int x = size / 2; x >= 0; --x )
			{
				final float val = ( float ) Math.exp( -( float ) ( x * x ) / two_sq_sigma );

				kernel[ size / 2 - x ] = val;
				kernel[ size / 2 + x ] = val;
			}
		}

		if ( normalize )
		{
			float sum = 0;
			for ( float value : kernel )
				sum += value;

			for ( int i = 0; i < kernel.length; i++ )
				kernel[ i ] /= sum;
		}

		return kernel;
	}
}
