package net.imglib2.smfish;

import java.util.ArrayList;

import spim.process.cuda.CUDASeparableConvolution;
import spim.process.cuda.CUDATools;
import spim.process.cuda.NativeLibraryTools;

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
