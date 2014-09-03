package net.imglib2.smfish;

import ij.ImageJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import spim.fiji.plugin.util.OpenImg;
import spim.process.cuda.CUDADevice;
import spim.process.cuda.CUDASeparableConvolution;
import spim.process.cuda.CUDASeparableConvolutionFunctions;
import spim.process.cuda.CUDATools;
import spim.process.cuda.NativeLibraryTools;
import spim.process.fusion.export.DisplayImage;

import com.sun.jna.Native;

/**
 * 
 * @author preibischs
 *
 */
public class TestJNAMemoryLeak
{
	public TestJNAMemoryLeak( final CUDASeparableConvolution cuda, final CUDADevice devId )
	{
		
		final File f = new File( "img_1388x1040x81.tif" );
		//final File f = new File( "img_694x520x81.tif" );
		System.out.println( f.getAbsolutePath() );
		
		final Img< FloatType > input = OpenImg.open( f.getAbsolutePath(), new ArrayImgFactory<FloatType>() );

		final Img< FloatType > imgCPU = input.copy();

		final int w = (int)imgCPU.dimension( 0 );
		final int h = (int)imgCPU.dimension( 1 );
		final int d = (int)imgCPU.dimension( 2 );
		
		final double sigma = 0.51;
		
		final float[] imgFCPU = ((FloatArray)((ArrayImg< FloatType, ? > )imgCPU).update( null ) ).getCurrentStorageArray();

		new ImageJ();
		
		final CUDASeparableConvolutionFunctions cudaFunctions = new CUDASeparableConvolutionFunctions( cuda, devId.getDeviceId() );

		final double[] kdouble = Util.createGaussianKernel1DDouble( sigma, true );
		final float[] kernelCPU = CUDASeparableConvolutionFunctions.getFloatKernelPadded( kdouble, kdouble.length );
		
		System.out.println( "kernelsize CPU: " + kernelCPU.length );

		if ( kernelCPU == null )
			return;

		while ( true )
		{
			long time = System.currentTimeMillis();
			
			
			
			cuda.convolutionCPU( imgFCPU.clone(), kernelCPU.clone(), kernelCPU.clone(), kernelCPU.clone(), kernelCPU.length, kernelCPU.length, kernelCPU.length, w, h, d, 2, 0 );
			System.out.println( "CPU: " + (System.currentTimeMillis() - time) + " ms." );
			System.out.println( "Free: " + Runtime.getRuntime().freeMemory()/(1024*1024) + " TotaL: " + Runtime.getRuntime().totalMemory()/(1024*1024) + " Max: " + Runtime.getRuntime().maxMemory()/(1024*1024) );
		}
		
		//new DisplayImage().exportImage( input, "Input " );
		//new DisplayImage().exportImage( imgCPU, "CPU " + f.getAbsolutePath() );
	}

	public static CUDASeparableConvolution loadCUDA()
	{
		final CUDASeparableConvolution cuda;
		
		final File f = new File( "/home/preibisch/workspace/smfish/libSeparableConvolutionCUDALib.so" );
		
		if ( f.exists() )
		{
			cuda = (CUDASeparableConvolution)Native.loadLibrary( f.getAbsolutePath(), CUDASeparableConvolution.class );
		}
		else
		{
			cuda = NativeLibraryTools.loadNativeLibrary( "separable", CUDASeparableConvolution.class );	
		}

		return cuda;
	}
	public static void main( String[] args )
	{
		final CUDASeparableConvolution cuda = loadCUDA();
		ArrayList< CUDADevice > dev = CUDATools.queryCUDADetails( cuda, false );

		if ( dev == null )
		{
			dev = new ArrayList< CUDADevice >();
			dev.add( new CUDADevice( -1, "CPU", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().maxMemory(), 0, 0 ) );
		}

		new TestJNAMemoryLeak( cuda, dev.get( 0 ) );
	}
}
