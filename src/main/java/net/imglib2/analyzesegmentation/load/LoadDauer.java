package net.imglib2.analyzesegmentation.load;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;

import mpicbg.spim.io.TextFileAccess;
import net.imglib2.RealPoint;
import net.imglib2.analyzesegmentation.Cell;
import net.imglib2.analyzesegmentation.Cells;

public class LoadDauer extends Load
{
	public static String defaultText = "/Users/spreibi/workspace/smfish/nucs4alignment.txt";

	final double scale, scaleZ;

	public LoadDauer( final double scaleTotal, final double scaleZ )
	{
		super( 3 );
		this.scaleZ = scaleZ;
		this.scale = scaleTotal;
	}

	@Override
	public Cells load()
	{
		if ( defaultText != null )
			fileChooser.setSelectedFile( new File( defaultText ) );

		if ( fileChooser() == JFileChooser.APPROVE_OPTION )
		{
			final File file = fileChooser.getSelectedFile();
			defaultText = file.getAbsolutePath();

			System.out.println( "loading annotions from " + file );

			final Cells cells = new Cells();
			int nextCellId = -1;

			cells.getCells().clear();
			cells.getSpheres().clear();

			//double avgX = 0, avgY = 0, avgZ = 0;
			//int countVerticies = -1;

			final ArrayList< double[] > locations = new ArrayList<>();

			try
			{
				final BufferedReader in = TextFileAccess.openFileRead( file );
	
				while ( in.ready() )
				{
					String line = in.readLine();
					String[] entries = line.substring( 3, line.length() ).split( " " );
	
					final int id = Integer.parseInt( entries[ 0 ] );
					final double x = Double.parseDouble( entries[ 1 ] );
					final double y = Double.parseDouble( entries[ 2 ] );
					final double z = Double.parseDouble( entries[ 3 ] );
	
					// new cell
					if ( id != nextCellId )
					{
						if ( locations.size() > 0 )
						{
							//if ( id - 1 != nextCellId )
							//	System.out.println( nextCellId + " >> " + id );

							// make a new cell from the old data if there is any
							final double[] avg = avg( locations );
							final double[] stdev = stdev( locations, avg );
							final double radius = ( stdev[ 0 ] + stdev[ 1 ] + stdev[ 2 ] ) / 2.0;

							final Cell cell = new Cell( nextCellId, new RealPoint(
									scale * avg[ 1 ], // switch xy
									scale * avg[ 0 ],
									scale * scaleZ * avg[ 2 ] ),
									(float)( scale * radius ) );

							if ( cells.getCells().containsKey( cell.getId() ) )
								System.out.println(  "collision " + cell.getId() );

							cells.getCells().put( cell.getId(), cell );

							for ( int d = 0; d < n; ++d )
							{
								min[ d ] = Math.min( min[ d ], cell.getDoublePosition( d ) );
								max[ d ] = Math.max( max[ d ], cell.getDoublePosition( d ) );
							}

						}

						locations.clear();
						nextCellId = id;
					}

					locations.add( new double[]{ x, y, z } );
				}

				numCells = cells.getCells().keySet().size();

				in.close();
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				numCells = 0;
				return null;
			}
	
			return cells;
		}
		else
		{
			return null;
		}
	}

	@Override
	protected String validExtension() { return "txt"; }

	public static double[] avg( final ArrayList< double[] > values )
	{
		final int n = values.get( 0 ).length;
		final double[] avg = new double[ n ];

		for ( final double[] val : values )
			for ( int d = 0; d < n; ++d )
				avg[ d ] += val[ d ];

		for ( int d = 0; d < n; ++d )
			avg[ d ] /= (double)values.size();

		return avg;
	}

	public static double[] stdev( final ArrayList< double[] > values, final double[] avg )
	{
		final int n = avg.length;
		final double[] stdev = new double[ n ];

		for ( final double[] val : values )
			for ( int d = 0; d < n; ++d )
				stdev[ d ] += ( val[ d ] - avg[ d ] ) * ( val[ d ] - avg[ d ] );

		for ( int d = 0; d < n; ++d )
			stdev[ d ] = Math.sqrt( stdev[ d ] / (double)values.size() );

		return stdev;
	}
}
