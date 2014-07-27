package net.imglib2.smfish;

import java.io.File;
import java.util.ArrayList;

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
		
		final File f = new File( "img_1280x1024x128.tif" );
		System.out.println( f.getAbsolutePath() );
		
		final Img< FloatType > img = OpenImg.open( f.getAbsolutePath(), new ArrayImgFactory<FloatType>() );

		final float[] imgF = ((FloatArray)((ArrayImg< FloatType, ? > )img).update( null ) ).getCurrentStorageArray();
		final float[] kernel = getFloatKernelPadded( Util.createGaussianKernel1DDouble( 1, true ), 31 );
		
		if ( kernel == null )
			return;

		System.out.println( cuda.multipleOfX_31() );
		System.out.println( cuda.multipleOfY_31() );
		System.out.println( cuda.multipleOfZ_31() );

		boolean success = cuda.convolve_31( imgF, kernel, kernel, kernel, (int)img.dimension( 0 ), (int)img.dimension( 1 ), (int)img.dimension( 2 ), true, true, true, devId );

		System.out.println( success );

		new DisplayImage().exportImage(img, f.getAbsolutePath() );
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
		System.out.println( Util.createGaussianKernel1DDouble( 1, true ).length );

		for ( final float f : getFloatKernelPadded( Util.createGaussianKernel1DDouble( 1, true ), 9 ) )
			System.out.println( f );
		
		CUDASeparableConvolution cuda = NativeLibraryTools.loadNativeLibrary( null, CUDASeparableConvolution.class );

		if ( cuda == null )
			return;

		final ArrayList< Integer > dev = CUDATools.queryCUDADetails( cuda, false );
		
		new TestCUDA( cuda, dev.get( 0 ) );
	}
}
