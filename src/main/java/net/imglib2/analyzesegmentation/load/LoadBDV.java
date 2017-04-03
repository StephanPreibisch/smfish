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

	public LoadBDV(  final double scaleZ  )
	{
		this.scaleZ = scaleZ;
	}

	public Cells load()
	{
		if ( defaultXML != null )
			fileChooser.setSelectedFile( new File( defaultXML ) );

		final int returnVal = fileChooser.showDialog( null, "Open" );

		if ( returnVal == JFileChooser.APPROVE_OPTION )
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
					
					cells.getCells().put( cell.getId(), cell );
					if ( cell.getId() >= nextCellId )
						nextCellId = cell.getId() + 1;
				}

				System.out.println( "Loaded " + cells.getCells().keySet().size() + " cells." );
			}
			catch ( final Exception e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return cells;
		}
		else
		{
			return null;
		}
	}

}
