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
	public TestCUDA( final CUDASeparableConvolution cuda )
	{
		
	}
	
	public static void main( String[] args )
	{
		CUDASeparableConvolution cuda = NativeLibraryTools.loadNativeLibrary( null, CUDASeparableConvolution.class );

		if ( cuda == null )
			return;

		final ArrayList< Integer > dev = CUDATools.queryCUDADetails( cuda, false );
	}
}
