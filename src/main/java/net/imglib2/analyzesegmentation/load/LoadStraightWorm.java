package net.imglib2.analyzesegmentation.load;

import java.io.BufferedReader;
import java.io.File;

import javax.swing.JFileChooser;

import mpicbg.spim.io.TextFileAccess;
import net.imglib2.RealPoint;
import net.imglib2.analyzesegmentation.Cell;
import net.imglib2.analyzesegmentation.Cells;

public class LoadStraightWorm extends Load
{
	public static String defaultTXT = "/Users/spreibi/workspace/smfish/cells.em.straight.txt";

	final double lengthX;
	final boolean makeRound;

	double[] scaling, translation;

	public LoadStraightWorm( final double lengthX, final boolean makeRound )
	{
		super( 3 );
		this.lengthX = lengthX;
		this.makeRound = makeRound;

		resetScaling();
		resetTranslation();
	}

	public void setScaling( final double[] s ) { this.scaling = s; }
	public void setTranslation( final double[] t ) { this.translation = t; }

	public void resetScaling() { this.scaling = new double[]{ 1, 1, 1 }; }
	public void resetTranslation() { this.translation = new double[]{ 0, 0, 0 }; }

	public Cells load()
	{
		if ( defaultTXT != null )
			fileChooser.setSelectedFile( new File( defaultTXT ) );

		if ( fileChooser() == JFileChooser.APPROVE_OPTION )
		{
			final File file = fileChooser.getSelectedFile();
			defaultTXT = file.getAbsolutePath();

			System.out.println( "loading annotions from " + file );

			return load( file );
		}
		else
		{
			return null;
		}
	}

	public Cells load( final File file )
	{
		this.numCells = 0;

		final Cells cells = new Cells();

		cells.getCells().clear();
		cells.getSpheres().clear();

		try
		{
			final BufferedReader in = TextFileAccess.openFileRead( file );
	
			while ( in.ready() )
			{
				final String[] entries = in.readLine().split( "\t" );
	
				final int id = Integer.parseInt( entries[ 0 ] );
				final double x = Double.parseDouble( entries[ 1 ] );
				final double y = Double.parseDouble( entries[ 2 ] );
				final double z = Double.parseDouble( entries[ 3 ] );

				final Cell cell = new Cell( id, new RealPoint( x * scaling[ 0 ], y * scaling[ 1 ], z * scaling[ 2 ] ), 2.5f );

				cells.getCells().put( id, cell );

				for ( int d = 0; d < n; ++d )
				{
					min[ d ] = Math.min( min[ d ], cell.getDoublePosition( d ) );
					max[ d ] = Math.max( max[ d ], cell.getDoublePosition( d ) );
				}
			}

			in.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return null;
		}

		this.numCells = cells.getCells().keySet().size();

		final double scale[] = new double[ 3 ];
		scale[ 0 ] = scale[ 1 ] = scale[ 2 ] = lengthX / max[ 0 ];

		if ( makeRound )
		{
			double dimY = max[ 1 ] - min[ 1 ];
			double dimZ = max[ 2 ] - min[ 2 ];

			if ( dimZ > dimY )
				scale[ 1 ] *= dimZ / dimY;
			else
				scale[ 2 ] *= dimY / dimZ;
		}

		resetMinMax();

		for ( final Cell cell : cells.getCells().values() )
		{
			final RealPoint p = cell.getPosition();
			
			for ( int d = 0; d < n; ++d )
			{
				p.setPosition( p.getDoublePosition( d ) * scale[ d ] + translation[ d ], d );

				min[ d ] = Math.min( min[ d ], cell.getDoublePosition( d ) );
				max[ d ] = Math.max( max[ d ], cell.getDoublePosition( d ) );
			}
		}

		return cells;
	}

	@Override
	protected String validExtension() { return "xml"; }
}
