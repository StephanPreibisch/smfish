package net.imglib2.scalespace;

import java.util.Random;

import spim.process.cuda.CUDASeparableConvolution;
import spim.process.cuda.CUDASeparableConvolutionFunctions;
import mpicbg.imagefeatures.FloatArray2D;

/**
 * single octave of a discrete FloatArray2DScaleSpace
 * 
 * This class is optimized for the Difference Of Gaussian detector used in
 * David Lowe's SIFT-algorithm \citep{Loew04}.
 * 
 * The scale space itself consists of an arbitrary number of octaves.  This
 * number is implicitly defined by the minimal image size #IN_SIZE.
 * Octaves contain overlapping scales of the scalespace.  Thus it is possible
 * to execute several operations that depend on adjacent scales within one
 * octave.
 *  
 * 
 * @author Stephan Saalfeld
 * @version 0.1b
 */
public class FloatArray3DScaleOctave
{
	public enum State { EMPTY, STUB, COMPLETE }

	public State state = State.EMPTY;
	
	final CUDASeparableConvolutionFunctions cuda;
	
	public int width = 0;
	public int height = 0;
	public int depth = 0;
	
	private float K = 2.0f; 
	private float K_MIN1_INV = 1.0f / ( K - 1.0f );

	/**
	 * steps per octave
	 * 
	 * an octave consists of STEPS + 3 images to be 
	 */
	public int STEPS = 1;
	
	/**
	 * sigma of gaussian kernels corresponding to the steps of the octave
	 * 
	 * the first member is the sigma of the gaussian kernel that is assumed to
	 * be the generating kernel of the first gaussian image instance of the
	 * octave 
	 */
	public float[] SIGMA;
//	public float[] getSigma()
//	{
//		return SIGMA;
//	}
	
	/**
	 * sigma of gaussian kernels required to create the corresponding gaussian
	 * image instances from the first one
	 */
	private float[] SIGMA_DIFF;
	
	/**
	 * 1D gaussian kernels required to create the corresponding gaussian
	 * image instances from the first one
	 */
	private float[][] KERNEL_DIFF;
	
	/**
	 * gaussian smoothed images
	 */
	private FloatArray3D[] l;
	public FloatArray3D[] getL()
	{
		return l;
	}
	public FloatArray3D getL( int i )
	{
		return l[ i ];
	}
	
	/**
	 * scale normalised difference of gaussian images
	 */
	private FloatArray3D[] d;
	public FloatArray3D[] getD()
	{
		return d;
	}
	public FloatArray3D getD( int i )
	{
		return d[ i ];
	}

	/*
	 * Constructor
	 * 
	 * @param img image being the first gaussian instance of the scale octave
	 *   img must be a 2d-array of float values in range [0.0f, ..., 1.0f]
	 * @param initial_sigma inital gaussian sigma
	 */
	public FloatArray3DScaleOctave(
			FloatArray3D img,
			int steps,
			float initial_sigma,
			final CUDASeparableConvolution cuda,
			final int cudaDeviceId )
	{
		this.cuda = new CUDASeparableConvolutionFunctions( cuda, cudaDeviceId );
		state = State.EMPTY;
		
		width = img.width;
		height = img.height;
		depth = img.depth;
		
		STEPS = steps;
		
		K = ( float )Math.pow( 2.0, 1.0 / ( float )STEPS );
		K_MIN1_INV = 1.0f / ( K - 1.0f );
			
		SIGMA = new float[ STEPS + 3 ];
		SIGMA[ 0 ] = initial_sigma;
		SIGMA_DIFF = new float[ STEPS + 3 ];
		SIGMA_DIFF[ 0 ] = 0.0f;
		KERNEL_DIFF = new float[ STEPS + 3 ][];
		
		//System.out.println( "sigma[0] = " + SIGMA[ 0 ] + "; sigma_diff[0] = " + SIGMA_DIFF[ 0 ] );
			
		for ( int i = 1; i < STEPS + 3; ++i )
		{
			SIGMA[ i ] = initial_sigma * ( float )Math.pow( 2.0f, ( float )i / ( float )STEPS );
			SIGMA_DIFF[ i ] = ( float )Math.sqrt( SIGMA[ i ] * SIGMA[ i ] - initial_sigma * initial_sigma );
			
			//System.out.println( "sigma[" + i + "] = " + SIGMA[ i ] + "; sigma_diff[" + i + "] = " + SIGMA_DIFF[ i ] );

			KERNEL_DIFF[ i ] = Filter.createGaussianKernel(
					SIGMA_DIFF[ i ],
					true );
		}
		l = new FloatArray3D[ 1 ];
		l[ 0 ] = img;
		d = null;
	}
	
	/*
	 * Constructor
	 * 
	 * faster initialisation with precomputed gaussian kernels
	 * 
	 * @param img image being the first gaussian instance of the scale octave 
	 * @param initial_sigma inital gaussian sigma
	 * 
	 */
	public FloatArray3DScaleOctave(
			FloatArray3D img,
			float[] sigma,
			float[] sigma_diff,
			float[][] kernel_diff,
			final CUDASeparableConvolution cuda,
			final int cudaDeviceId )
	{
		this.cuda = new CUDASeparableConvolutionFunctions( cuda, cudaDeviceId );
		state = State.EMPTY;
		
		width = img.width;
		height = img.height;
		depth = img.depth;
		
		STEPS = sigma.length - 3;
		
		K = ( float )Math.pow( 2.0, 1.0 / ( float )STEPS );
		K_MIN1_INV = 1.0f / ( K - 1.0f );
			
		SIGMA = sigma;
		SIGMA_DIFF = sigma_diff;
		KERNEL_DIFF = kernel_diff;
		
		l = new FloatArray3D[ 1 ];
		l[ 0 ] = img;
		d = null;
	}
	
	/**
	 * build only the gaussian image with 2 * INITIAL_SIGMA
	 * 
	 * Use this method for the partial creation of an octaved scale space
	 * without creating each scale octave.  Like proposed by Lowe
	 * \citep{Lowe04}, you can use this image to build the next scale octave.
	 * Taking every second pixel of this image, you get a gaussian  image with
	 * INITIAL_SIGMA of the half image size.
	 */
	public void buildStub()
	{
		FloatArray3D img = l[ 0 ];
		l = new FloatArray3D[ 2 ];
		l[ 0 ] = img;
		l[ 1 ] = l[ 0 ].clone();
		cuda.gauss( l[ 1 ].data, new int[]{ l[ 1 ].width, l[ 1 ].height, l[ 1 ].depth }, KERNEL_DIFF[ STEPS ] );
		//l[ 1 ] = Filter.convolveSeparable( l[ 0 ], KERNEL_DIFF[ STEPS ], KERNEL_DIFF[ STEPS ] );

		state = State.STUB;
	}
	
	
	/*
	 * build the scale octave
	 */
	public boolean build()
	{
		FloatArray3D img = l[ 0 ];
		FloatArray3D img2;
		if ( state == State.STUB )
		{
			img2 = l[ 1 ];
			l = new FloatArray3D[ STEPS + 3 ];
			l[ STEPS ] = img2;
		}
		else
		{
			l = new FloatArray3D[ STEPS + 3 ];
		}
		l[ 0 ] = img;
		
		for ( int i = 1; i < SIGMA_DIFF.length; ++i )
		{
			if ( state == State.STUB && i == STEPS )
				continue;
			l[ i ] = l[ 0 ].clone();
			cuda.gauss( l[ i ].data, new int[]{ l[ 1 ].width, l[ 1 ].height, l[ 1 ].depth }, KERNEL_DIFF[ i ] );
			//l[ i ] = Filter.convolveSeparable( l[ 0 ], KERNEL_DIFF[ i ], KERNEL_DIFF[ i ] );
		}
		d = new FloatArray3D[ STEPS + 2 ];
		
		for ( int i = 0; i < d.length; ++i )
		{
			d[ i ] = new FloatArray3D( l[ i ].width, l[ i ].height, l[ i ].depth );
			int j = i + 1;
			for ( int k = 0; k < l[ i ].data.length; ++k )
			{
				d[ i ].data[ k ] = ( l[ j ].data[ k ] - l[ i ].data[ k ] ) * K_MIN1_INV;
			}
		}

		state = State.COMPLETE;

		return true;
	}
	
	/**
	 * clear the scale octave to save memory
	 */
	public void clear()
	{
		this.state = State.EMPTY;
		this.d = null;
		this.l = null;
	}


	/**
	 * downsample src by simply using every second pixel into
	 * dst
	 * 
	 * For efficiency reasons, the dimensions of dst are not checked,
	 * that is, you have to take care, that
	 * dst.width == src.width / 2 + src.width mod 2 and
	 * dst.height == src.height / 2 + src.height mod 2 and
	 * dst.depth == src.depth / 2 + src.depth mod 2.
	 * 
	 * @param src the source image
	 * @param dst destination image
	 */
	public static void downsample( FloatArray3D src, FloatArray3D dst )
	{
		int ws = 2 * src.width;

		int zs = 0;
		for ( int z = 0; z < dst.width * dst.height * dst.depth; z += dst.width * dst.height )
		{
			int ys = 0;
			for ( int y = 0; y < dst.width * dst.height; y += dst.width )
			{
				int xs = 0;
				for ( int x = 0; x < dst.width; ++x )
				{
					dst.data[ z + y + x ] = src.data[ zs + ys + xs ];
					xs += 2;
				}
				ys += ws;
			}
			zs += ws * src.height;
		}
	}

	public static void upsample( final FloatArray3D src, final FloatArray3D dst )
	{
		// init first pixel
		dst.data[ 0 ] = src.data[ 0 ];
		
		// init first line 
		final int xd = 2;
		int xd1 = 2;
		int xd2 = 1;
		
		for ( int xs1 = 1; xs1 < src.width; ++xs1 )
		{
			int xs2 = xs1 - 1;
			dst.data[ xd1 ] = src.data[ xs1 ];
			dst.data[ xd2 ] = ( src.data[ xs1 ] + src.data[ xs2 ] ) / 2.0f;
			xd1 += xd;
			xd2 += xd;
		}

		// init first plane
		final int ydw = 2 * dst.width;
		int yd1 = ydw;
		int yd2 = dst.width;

		for ( int ys1 = src.width; ys1 < src.width * src.height; ys1 += src.width )
		{
			int ys2 = ys1 - src.width;
			xd1 = 2;
			xd2 = 1;
			dst.data[ yd1 ] = src.data[ ys1 ];
			dst.data[ yd2 ] = ( src.data[ ys1 ] + src.data[ ys2 ] ) / 2;
			
			for ( int xs1 = 1; xs1 < src.width; ++xs1 )
			{
				int xs2 = xs1 - 1;

				dst.data[ yd1 + xd1 ] = src.data[ ys1 + xs1 ];
				dst.data[ yd1 + xd2 ] = ( src.data[ ys1 + xs1 ] + src.data[ ys1 + xs2 ] ) / 2.0f;
				dst.data[ yd2 + xd1 ] = ( src.data[ ys1 + xs1 ] + src.data[ ys2 + xs1 ] ) / 2.0f;
				dst.data[ yd2 + xd2 ] = ( src.data[ ys1 + xs1 ] + src.data[ ys2 + xs2 ] + src.data[ ys1 + xs2 ] + src.data[ ys2 + xs1 ] ) / 4.0f;
				xd1 += 2;
				xd2 += 2;
			}
			
			yd1 += ydw;
			yd2 += ydw;
		}
		
		// for all remaining planes do
		final int zdwh = 2 * dst.width * dst.height;
		
		int zd1 = zdwh;
		int zd2 = dst.width * dst.height;

		for ( int zs1 = src.width * src.height; zs1 < src.width * src.height * src.depth; zs1 += src.width * src.height )
		{
			int zs2 = zs1 - src.width * src.height;
			dst.data[ zd1 ] = src.data[ zs1 ];
			dst.data[ zd2 ] = ( src.data[ zs1 ] + src.data[ zs2 ] ) / 2;

			// fill up first x lines
			xd1 = 2;
			xd2 = 1;
			for ( int xs1 = 1; xs1 < src.width; ++xs1 )
			{
				int xs2 = xs1 - 1;
				dst.data[ zd1 + xd1 ] = src.data[ zs1 + xs1 ];
				dst.data[ zd1 + xd2 ] = ( src.data[ zs1 + xs1 ] + src.data[ zs1 + xs2 ] ) / 2.0f;
				dst.data[ zd2 + xd1 ] = ( src.data[ zs1 + xs1 ] + src.data[ zs2 + xs1 ] ) / 2.0f;
				dst.data[ zd2 + xd2 ] = ( src.data[ zs1 + xs1 ] + src.data[ zs2 + xs2 ] ) / 2.0f;
				xd1 += 2;
				xd2 += 2;
			}

			// fill up first y lines and then extend through all x
			yd1 = ydw;
			yd2 = dst.width;
			for ( int ys1 = src.width; ys1 < src.width * src.height; ys1 += src.width )
			{
				int ys2 = ys1 - src.width;
				xd1 = 2;
				xd2 = 1;
				dst.data[ zd1 + yd1 ] = src.data[ zs1 + ys1 ];
				dst.data[ zd1 + yd2 ] = ( src.data[ zs1 + ys1 ] + src.data[ zs1 + ys2 ] ) / 2.0f;
				dst.data[ zd2 + yd1 ] = ( src.data[ zs1 + ys1 ] + src.data[ zs2 + ys1 ] ) / 2.0f;
				dst.data[ zd2 + yd2 ] = ( src.data[ zs1 + ys1 ] + src.data[ zs2 + ys2 ] ) / 2;

				
				for ( int xs1 = 1; xs1 < src.width; ++xs1 )
				{
					int xs2 = xs1 - 1;
					dst.data[ zd1 + yd1 + xd1 ] = src.data[ zs1 + ys1 + xs1 ];
					dst.data[ zd1 + yd1 + xd2 ] = ( src.data[ zs1 + ys1 + xs1 ] + src.data[ zs1 + ys1 + xs2 ] ) / 2.0f;
					dst.data[ zd1 + yd2 + xd1 ] = ( src.data[ zs1 + ys1 + xs1 ] + src.data[ zs1 + ys2 + xs1 ] ) / 2.0f;
					dst.data[ zd1 + yd2 + xd2 ] = ( src.data[ zs1 + ys1 + xs1 ] + src.data[ zs1 + ys2 + xs2 ] + src.data[ zs1 + ys2 + xs1 ] + src.data[ zs1 + ys1 + xs2 ] ) / 4.0f;
					
					dst.data[ zd2 + yd1 + xd1 ] = ( src.data[ zs1 + ys1 + xs1 ] + src.data[ zs2 + ys1 + xs1 ] ) / 2.0f;
					dst.data[ zd2 + yd1 + xd2 ] = ( src.data[ zs1 + ys1 + xs1 ] + src.data[ zs2 + ys1 + xs1 ] + src.data[ zs1 + ys1 + xs2 ] + src.data[ zs2 + ys1 + xs2 ] ) / 4.0f;
					dst.data[ zd2 + yd2 + xd1 ] = ( src.data[ zs1 + ys1 + xs1 ] + src.data[ zs2 + ys1 + xs1 ] + src.data[ zs1 + ys2 + xs1 ] + src.data[ zs2 + ys2 + xs1 ] ) / 4.0f;
					
					dst.data[ zd2 + yd2 + xd2 ] = ( src.data[ zs1 + ys1 + xs1 ] + src.data[ zs2 + ys1 + xs1 ] + src.data[ zs1 + ys2 + xs1 ] + src.data[ zs2 + ys2 + xs1 ] +
													src.data[ zs1 + ys2 + xs2 ] + src.data[ zs2 + ys2 + xs2 ] + src.data[ zs1 + ys1 + xs2 ] + src.data[ zs2 + ys1 + xs2 ] ) / 8.0f;
					
					xd1 += 2;
					xd2 += 2;
				}
				
				yd1 += ydw;
				yd2 += ydw;
			}
			zd1 += zdwh;
			zd2 += zdwh;
		}

		if ( dst.width % 2 == 0 )
		{
			for ( zd1 = dst.width * dst.height; zd1 < dst.width * dst.height * dst.depth; zd1 += dst.width * dst.height )
			{
				yd1 = dst.width * dst.height - dst.width;
				yd2 = yd1 - dst.width;
				for ( xd1 = 0; xd1 < dst.width; ++xd1 )
				{
					dst.data[ zd1 + yd1 + xd1 ] = dst.data[ zd1 + yd2 + xd1 ];
				}
			}
		}
	}
	
	/**
	 * upsample src by linearly interpolating into dst
	 * 
	 * For efficiency reasons, the dimensions of dst are not checked,
	 * that is, you have to take care, that
	 * src.width == dst.width / 2 + dst.width mod 2 and
	 * src.height == dst.height / 2 + dst.height mod 2 and
	 * src.depth == dst.depth / 2 + dst.depth mod 2.
	 * 
	 * @param src the source image
	 * @param dst destination image
	 */
	public static void upsample( FloatArray2D src, FloatArray2D dst )
	{
		int rdw = 2 * dst.width;
		int rd1 = rdw;
		int rd2 = dst.width;
		int xd1 = 2;
		int xd2 = 1;
		dst.data[ 0 ] = src.data[ 0 ];
		for ( int xs1 = 1; xs1 < src.width; ++xs1 )
		{
			int xs2 = xs1 - 1;
			dst.data[ xd1 ] = src.data[ xs1 ];
			dst.data[ xd2 ] = ( src.data[ xs1 ] + src.data[ xs2 ] ) / 2.0f;
			xd1 += 2;
			xd2 += 2;
		}
		for ( int rs1 = src.width; rs1 < src.data.length; rs1 += src.width )
		{
			int rs2 = rs1 - src.width;
			xd1 = 2;
			xd2 = 1;
			dst.data[ rd1 ] = src.data[ rs1 ];
			dst.data[ rd2 ] = ( src.data[ rs1 ] + src.data[ rs2 ] ) / 2;
			
			for ( int xs1 = 1; xs1 < src.width; ++xs1 )
			{
				int xs2 = xs1 - 1;
				dst.data[ rd1 + xd1 ] = src.data[ rs1 + xs1 ];
				dst.data[ rd1 + xd2 ] = ( src.data[ rs1 + xs1 ] + src.data[ rs1 + xs2 ] ) / 2.0f;
				dst.data[ rd2 + xd1 ] = ( src.data[ rs1 + xs1 ] + src.data[ rs2 + xs1 ] ) / 2.0f;
				dst.data[ rd2 + xd2 ] = ( src.data[ rs1 + xs1 ] + src.data[ rs2 + xs2 ] ) / 2.0f;
				xd1 += 2;
				xd2 += 2;
			}
			
			rd1 += rdw;
			rd2 += rdw;
			
		}/*
		if ( dst.height % 2 == 0 )
		{
			rd1 = dst.data.length - dst.width;
			rd2 = rd1 - dst.width;
			for ( xd1 = 0; xd1 < dst.width; ++xd1 )
			{
				dst.data[ rd1 + xd1 ] = dst.data[ rd2 + xd1 ];
			}
		}
		if ( dst.width % 2 == 0 )
		{
			xd1 = dst.width - 1;
			xd2 = dst.width - 2;
			for ( rd1 = 0; rd1 < dst.data.length; rd1 += dst.width )
			{
				dst.data[ rd1 + xd1 ] = dst.data[ rd1 + xd2 ];
			}
		}*/
	}
	
	public static void main( String[] args )
	{
		
		Random rnd = new Random( 5335 );

		FloatArray3D s = new FloatArray3D( 4, 4, 4 );
		FloatArray3D d = new FloatArray3D( 8, 8, 8 );

		int i = 0;
		
		for ( int z = 0; z < s.depth; ++z )
			for ( int y = 0; y < s.height; ++y )
				for ( int x = 0; x < s.width; ++x )
					s.set( i++/*rnd.nextInt( 10 ) + 1*/, x, y, z );

		upsample( s, d );
		print( s );
		print( d );
		
		System.exit( 0 );
		
		FloatArray2D src = new FloatArray2D( 4, 4 );
		FloatArray2D dst = new FloatArray2D( 8, 8 );

		int j = 0;
		
		for ( int y = 0; y < src.height; ++y )
			for ( int x = 0; x < src.width; ++x )
				src.set( rnd.nextInt( 10 ), x, y );
		
		
		upsample(src, dst);
		print( src );
		System.out.println();
		print( dst );
		
	}

	public static void print( final FloatArray3D f )
	{
		for ( int z = 0; z < f.depth; ++z )
		{
			for ( int y = 0; y < f.height; ++y )
			{
				for ( int x = 0; x < f.width; ++x )
				{
					System.out.print( f.get( x, y, z ) + " " );
				}
				System.out.println();
			}
			System.out.println();
		}
	}

	public static void print( final FloatArray2D f )
	{
		for ( int y = 0; y < f.height; ++y )
		{
			for ( int x = 0; x < f.width; ++x )
			{
				System.out.print( f.get( x, y ) + " " );
			}
			System.out.println();
		}
	}
}
