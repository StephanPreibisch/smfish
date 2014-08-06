package net.imglib2.scalespace;

import java.util.ArrayList;

import net.imglib2.smfish.TestCUDA;
import spim.process.cuda.CUDASeparableConvolution;
import spim.process.cuda.CUDATools;

public class TestScaleSpace
{
	public static void main( String[] args )
	{
		final CUDASeparableConvolution cuda = TestCUDA.loadCUDA();
		ArrayList< Integer > dev = CUDATools.queryCUDADetails( cuda, false );

		if ( dev == null )
		{
			dev = new ArrayList< Integer >();
			dev.add( - 1 );
		}

	}
}
