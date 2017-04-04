package net.imglib2.analyzesegmentation.load;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import net.imglib2.RealInterval;
import net.imglib2.RealPositionable;
import net.imglib2.analyzesegmentation.Cells;

public abstract class Load implements RealInterval
{
	final JFileChooser fileChooser;

	int numCells = 0;

	final int n;
	final double[] min, max;

	public Load( final int numDimensions )
	{
		this.min = new double[ numDimensions ];
		this.max = new double[ numDimensions ];
		this.n = numDimensions;

		for ( int d = 0; d < n; ++d )
		{
			min[ d ] = Double.MAX_VALUE;
			max[ d ] = -Double.MAX_VALUE;
		}

		this.fileChooser = createFileChooser();
	}

	/**
	 * Load the cells and populate min, max, numCells
	 * @return
	 */
	public abstract Cells load();
	protected abstract String validExtension();

	public int numCells() { return numCells; }

	protected JFileChooser createFileChooser()
	{
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter( new FileFilter()
		{
			@Override
			public String getDescription() { return validExtension() + " files"; }

			@Override
			public boolean accept( final File f )
			{
				if ( f.isDirectory() )
					return true;
				if ( f.isFile() )
				{
					final String s = f.getName();
					if ( validExtension() != null )
					{
						final int i = s.lastIndexOf('.');
						if (i > 0 &&  i < s.length() - 1) {
							final String ext = s.substring(i+1).toLowerCase();
							return ext.equals( validExtension() );
						}
					}
					else
					{
						return true;
					}
				}
				return false;
			}
		} );

		return fileChooser;
	}

	@Override
	public double realMin( final int d ) { return min[ d ]; }

	@Override
	public void realMin( final double[] min )
	{
		for ( int d = 0; d < n; ++d )
			min[ d ] = this.min[ d ];
	}

	@Override
	public void realMin( final RealPositionable min ) { min.setPosition( this.min ); }

	@Override
	public double realMax( final int d ) { return max[ d ]; }

	@Override
	public void realMax( final double[] max )
	{
		for ( int d = 0; d < n; ++d )
			max[ d ] = this.max[ d ];
	}

	@Override
	public void realMax( final RealPositionable max ) { max.setPosition( this.max ); }

	@Override
	public int numDimensions() { return n; }

	@Override
	public String toString() { return "Loaded " + numCells() + " cells, distributed in space: " + printRealInterval(); }

	public String printRealInterval() { return printRealInterval( this ); }

	public static String printRealInterval( final RealInterval interval )
	{
		String out = "(Interval empty)";

		if ( interval == null || interval.numDimensions() == 0 )
			return out;

		out = "[" + interval.realMin( 0 );

		for ( int i = 1; i < interval.numDimensions(); i++ )
			out += ", " + interval.realMin( i );

		out += "] -> [" + interval.realMax( 0 );

		for ( int i = 1; i < interval.numDimensions(); i++ )
			out += ", " + interval.realMax( i );

		out += "], dimensions (" + ( interval.realMax( 0 ) - interval.realMin( 0 ) );

		for ( int i = 1; i < interval.numDimensions(); i++ )
			out += ", " + ( interval.realMax( i ) - interval.realMin( i ) );

		out += ")";

		return out;
	}

}
