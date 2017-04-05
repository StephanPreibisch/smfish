package net.imglib2.analyzesegmentation.load;

import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;

import net.imglib2.RealPoint;
import net.imglib2.analyzesegmentation.Cell;
import net.imglib2.analyzesegmentation.Cells;
import spim.fiji.spimdata.interestpoints.InterestPoint;
import spim.fiji.spimdata.interestpoints.InterestPointList;

public class LoadInterestPoints extends Load
{
	public static String defaultTXT = "/Users/spreibi/Documents/Grants and CV/BIMSB/Anstellungen/Ella Bahry/Dauer_Imaging/2017-03-15-1_confocal/segment/interestpoints/tpId_0_viewSetupId_0.beads.ip.txt";

	final double scaleZ;

	public LoadInterestPoints( final double scaleZ )
	{
		super( 3 );

		this.scaleZ = scaleZ;
	}

	public Cells load()
	{
		if ( defaultTXT != null )
			fileChooser.setSelectedFile( new File( defaultTXT ) );

		if ( fileChooser() == JFileChooser.APPROVE_OPTION )
		{
			final File file = fileChooser.getSelectedFile();
			defaultTXT = file.getAbsolutePath();

			final File dir = new File( file.getParent() );
			final File ipFile = new File( file.getName().substring( 0, file.getName().length() - 7 ) );

			System.out.println( "loading annotions from " + dir + " interestpoint: " + ipFile );

			final InterestPointList list = new InterestPointList( dir, ipFile );
			if ( !list.loadInterestPoints() )
			{
				System.out.println( "Could not load interestpoints." );
				return null;
			}

			for ( final InterestPoint p : list.getInterestPoints() )
				p.getL()[ 2 ] *= scaleZ;

			final Cells cells = new Cells();

			cells.getCells().clear();
			cells.getSpheres().clear();

			final List< InterestPoint > ips = list.getInterestPoints();

			for ( int i = 0; i < ips.size(); ++i )
			{
				final InterestPoint ip = ips.get( i );
				final Cell cell = new Cell( i, new RealPoint(
						ip.getL()[ 0 ], // switch xy
						ip.getL()[ 1 ],
						scaleZ * ip.getL()[ 2 ] ),
						15 );

				cells.getCells().put( i, cell );

				for ( int d = 0; d < n; ++d )
				{
					min[ d ] = Math.min( min[ d ], cell.getDoublePosition( d ) );
					max[ d ] = Math.max( max[ d ], cell.getDoublePosition( d ) );
				}
			}

			this.numCells = cells.getCells().keySet().size();

			return cells;
		}
		else
		{
			return null;
		}
	}

	@Override
	protected String validExtension() { return "xml"; }
}
