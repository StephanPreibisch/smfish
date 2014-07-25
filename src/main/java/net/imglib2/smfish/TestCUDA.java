package net.imglib2.smfish;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
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
		System.out.println( cuda.multipleOfX_31() );
		System.out.println( cuda.multipleOfY_31() );
		System.out.println( cuda.multipleOfZ_31() );
		
		final File f = new File( "img_1280x1024x128.tif" );
		System.out.println( f.getAbsolutePath() );
		
		final Img< FloatType > img = OpenImg.open( f.getAbsolutePath(), new ArrayImgFactory<FloatType>() );
		//ImageJFunctions.show( img );
		new DisplayImage().exportImage(img, f.getAbsolutePath() );
	}
	
	public static void main( String[] args )
	{
		CUDASeparableConvolution cuda = NativeLibraryTools.loadNativeLibrary( null, CUDASeparableConvolution.class );

		if ( cuda == null )
			return;

		final ArrayList< Integer > dev = CUDATools.queryCUDADetails( cuda, false );
		
		new TestCUDA( cuda, dev.get( 0 ) );
	}
}
