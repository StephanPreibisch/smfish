package net.imglib2.postprocessing;

import java.io.File;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class CenterWorm
{
	public static void main( String[] args )
	{
		new ImageJ();

		final ImagePlus imp = new ImagePlus( new File( "rotationworm.tif" ).getAbsolutePath() );
		imp.show();

		for ( int z = 1; z <= imp.getStackSize(); ++z )
		{
			imp.setSlice( z );
			final ImageProcessor ip = imp.getStack().getProcessor( z );

			//int xs = -1;

			long centerX = 0;
			long count = 0;

			for ( int x = 0; x < ip.getWidth(); ++x )
			{
				for ( int y = 0; y < ip.getHeight(); ++y )
				{
					if ( ip.getf( x, y ) < 255 )
					{
						centerX += x;
						++count;
					}
				}
			}

			double center = (double)centerX/(double)count;
			System.out.println( z + ": " + center );
			IJ.run(imp, "Translate...", "x=" + (1150-center) + " y=0 interpolation=None slice");

			for ( int x = 0; x < ip.getWidth(); ++x )
			{
				if ( ip.getf( x, 0 ) == 0 )
				for ( int y = 0; y < ip.getHeight(); ++y )
					ip.setf( x, y, 255 );
			}

		}
	}
}
