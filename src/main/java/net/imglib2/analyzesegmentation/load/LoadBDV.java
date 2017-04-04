package net.imglib2.analyzesegmentation.load;

import java.io.File;

import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.imglib2.analyzesegmentation.Cell;
import net.imglib2.analyzesegmentation.Cells;

public class LoadBDV extends Load
{
	public static String defaultXML = "/Users/spreibi/Documents/Microscopy/smFISH/samidouble_43_reconstructed.cells.xml";

	final double scaleZ;

	public LoadBDV( final double scaleZ )
	{
		super( 3 );

		this.scaleZ = scaleZ;
	}

	public Cells load()
	{
		if ( defaultXML != null )
			fileChooser.setSelectedFile( new File( defaultXML ) );

		if ( fileChooser() == JFileChooser.APPROVE_OPTION )
		{
			final File file = fileChooser.getSelectedFile();
			defaultXML = file.getAbsolutePath();

			System.out.println( "loading annotions from " + file );

			final Cells cells = new Cells();
			int nextCellId = 0;

			cells.getCells().clear();
			cells.getSpheres().clear();

			try
			{
				final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				final DocumentBuilder db = dbf.newDocumentBuilder();
				final Document dom = db.parse( file );
				final Element root = dom.getDocumentElement();
				final NodeList nodes = root.getElementsByTagName( "sphere" );
				for ( int i = 0; i < nodes.getLength(); ++i )
				{
					final Cell cell = Cell.fromXml( ( Element ) nodes.item( i ) );

					cell.getPosition().setPosition( cell.getDoublePosition( 2 ) * scaleZ, 2 );

					for ( int d = 0; d < n; ++d )
					{
						min[ d ] = Math.min( min[ d ], cell.getDoublePosition( d ) );
						max[ d ] = Math.max( max[ d ], cell.getDoublePosition( d ) );
					}

					cells.getCells().put( cell.getId(), cell );
					if ( cell.getId() >= nextCellId )
						nextCellId = cell.getId() + 1;
				}

				this.numCells = cells.getCells().keySet().size();
			}
			catch ( final Exception e )
			{
				// TODO Auto-generated catch block
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
	protected String validExtension() { return "xml"; }
}
