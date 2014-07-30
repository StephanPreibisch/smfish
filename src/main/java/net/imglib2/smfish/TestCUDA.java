package net.imglib2.smfish;

import ij.ImageJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import com.sun.jna.Native;

import net.imglib2.img.basictypeaccess.array.FloatArray;
import mpicbg.imglib.util.Util;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import spim.process.cuda.CUDASeparableConvolution;
import spim.process.cuda.CUDATools;
import spim.process.cuda.NativeLibraryTools;
import spim.process.fusion.export.DisplayImage;

/**
 * 
 * @author preibischs
 *
 */
public class TestCUDA
{
	public TestCUDA( final CUDASeparableConvolution cuda, final int devId )
	{
		
		final File f = new File( "img_1160x961x81.tif" );
		//final File f = new File( "img_1280x1024x128.tif" );
		System.out.println( f.getAbsolutePath() );
		
		final Img< FloatType > input = OpenImg.open( f.getAbsolutePath(), new ArrayImgFactory<FloatType>() );
		final Random rnd = new Random( 435656 );
		for ( final FloatType t : input )
			t.set( t.get() + rnd.nextFloat() );
		
		final Img< FloatType > imgCPU = input.copy();
		final Img< FloatType > imgGPU = imgCPU.copy();

		final int w = (int)imgCPU.dimension( 0 );
		final int h = (int)imgCPU.dimension( 1 );
		final int d = (int)imgCPU.dimension( 2 );
		
		final double sigma = 21;
		
		final float[] imgFCPU = ((FloatArray)((ArrayImg< FloatType, ? > )imgCPU).update( null ) ).getCurrentStorageArray();
		final float[] imgFGPU = ((FloatArray)((ArrayImg< FloatType, ? > )imgGPU).update( null ) ).getCurrentStorageArray();
		
		final double[] kdouble = Util.createGaussianKernel1DDouble( sigma, true );
		final float[] kernelCPU = getFloatKernelPadded( kdouble, kdouble.length );
		final float[] kernelGPU = getFloatKernelPadded( kdouble, 255 );
		
		System.out.println( "kernelsize CPU: " + kernelCPU.length );
		System.out.println( "kernelsize GPU: " + kernelGPU.length );
		
		if ( kernelGPU == null || kernelCPU == null )
			return;

		new ImageJ();
		
		long t = 0;
		
		//for ( int i = 0; i < 50; ++i )
		{
			long time = System.currentTimeMillis();
			boolean success = cuda.convolve_255( imgFGPU, kernelGPU.clone(), kernelGPU.clone(), kernelGPU.clone(), w, h, d, true, true, true, 2, 0, devId );
			time = (System.currentTimeMillis() - time);
			t += time;
			System.out.println( success + ": " + time + " ms." );
		}
		
		System.out.println( t / 50 );

		new DisplayImage().exportImage( imgGPU, "GPU " + f.getAbsolutePath() );

		long time = System.currentTimeMillis();
		cuda.convolutionCPU( imgFCPU, kernelCPU.clone(), kernelCPU.clone(), kernelCPU.clone(), kernelCPU.length, kernelCPU.length, kernelCPU.length, w, h, d, 2, 0 );
		System.out.println( "CPU: " + (System.currentTimeMillis() - time) + " ms." );
		
		
		//new DisplayImage().exportImage( input, "Input " );
		new DisplayImage().exportImage( imgCPU, "CPU " + f.getAbsolutePath() );
	}

	public static float[] getFloatKernelPadded( final double[] kernel, final int size )
	{
		if ( kernel.length > size )
			return null;

		final float[] k = new float[ size ];

		final int s = ( size - kernel.length )/2;

		for ( int i = 0; i < kernel.length; ++i )
			k[ s + i ] = (float)kernel[ i ];

		return k;
	}
	
	public static void main( String[] args )
	{
		final CUDASeparableConvolution cuda;
		final ArrayList< Integer > dev;
		
		final File f = new File( "/home/preibisch/workspace/smfish/libSeparableConvolutionCUDALib.so" );
		
		if ( f.exists() )
		{
			cuda = (CUDASeparableConvolution)Native.loadLibrary( f.getAbsolutePath(), CUDASeparableConvolution.class );
			dev = new ArrayList<Integer>();
			dev.add( 0 );
		}
		else
		{
			cuda = NativeLibraryTools.loadNativeLibrary( null, CUDASeparableConvolution.class );
	
			if ( cuda == null )
				return;

			dev = CUDATools.queryCUDADetails( cuda, false );
		}
		
		new TestCUDA( cuda, dev.get( 0 ) );
	}
}
