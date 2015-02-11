package net.imglib2.drift;
import java.io.BufferedReader;
import java.io.IOException;

import mpicbg.spim.io.TextFileAccess;


public class Parse
{

	public static void main( String[] args ) throws IOException
	{
		BufferedReader in = TextFileAccess.openFileRead( "/Users/preibischs/Downloads/drift_worm4.txt" );
		
		while ( in.ready() )
		{
			String l = in.readLine();
			
			if ( l.startsWith( "Tile " ) && l.contains( "], [" ) )
			{
				int z = Integer.parseInt( l.substring( 4, l.indexOf( '(' ) ).trim() );
				double x = Double.parseDouble( l.substring( l.indexOf( "[[" ) + 11, l.indexOf( "], [" ) ) );
				double y = Double.parseDouble( l.substring( l.indexOf( "], [" ) + 14, l.indexOf( "]])" ) ) );
				
				System.out.println( z + "\t" + x + "\t" + y );
			}
		}
	}
}
