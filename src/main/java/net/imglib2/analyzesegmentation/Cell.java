package net.imglib2.analyzesegmentation;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.util.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Cell implements RealLocalizable
{
	private final int id;

	private float radius;

	private final RealPoint position;

	public Cell( final int id, final RealLocalizable position, final float radius )
	{
		this.id = id;
		this.position = new RealPoint( position );
		this.radius = radius;
	}

	@Override
	public String toString()
	{
		return "cell( " + id + ", " + Util.printCoordinates( position ) + ", " + radius + " )";
	}

	public float getRadius()
	{
		return radius;
	}

	public void setRadius( final int radius )
	{
		this.radius = radius;
	}

	public int getId()
	{
		return id;
	}

	public RealPoint getPosition()
	{
		return position;
	}

	public static Cell fromXml( final Element cell )
	{
		final int id = Integer.parseInt( cell.getElementsByTagName( "id" ).item( 0 ).getTextContent() );
		final String data = ( ( Element ) cell.getElementsByTagName( "position" ).item( 0 ) ).getAttribute( "data" );
		final String[] fields = data.split( "\\s+" );
		final int n = fields.length;
		final RealPoint position = new RealPoint( n );
		for ( int d = 0; d < n; ++d )
			position.setPosition( Double.parseDouble( fields[ d ] ), d );
		final float radius = Float.parseFloat( cell.getElementsByTagName( "radius" ).item( 0 ).getTextContent() );
		return new Cell( id, position, radius );
	}

	public static Element toXml( final Document doc, final Cell cell )
	{
		final Element elem = doc.createElement( "sphere" );

		elem.appendChild( intElement( doc, "id", cell.getId() ) );
		final RealLocalizable pos = cell.getPosition();
		final int n = pos.numDimensions();
		String data = "";
		for ( int d = 0; d < n; ++d )
			data += pos.getDoublePosition( d ) + ( ( d == n - 1 ) ? "" : " " );
		final Element poselem = doc.createElement( "position" );
		poselem.setAttribute( "data", data );
		elem.appendChild( poselem );
		elem.appendChild( floatElement( doc, "radius", cell.getRadius() ) );

		return elem;
	}

	public static Element intElement( final Document doc, final String name, final int value )
	{
		final Element e = doc.createElement( name );
		e.appendChild( doc.createTextNode( Integer.toString( value ) ) );
		return e;
	}

	public static Element floatElement( final Document doc, final String name, final float value )
	{
		final Element e = doc.createElement( name );
		e.appendChild( doc.createTextNode( Float.toString( value ) ) );
		return e;
	}

	@Override
	public int numDimensions() { return 3; }

	@Override
	public void localize( final float[] position ) { this.position.localize( position ); }

	@Override
	public void localize( final double[] position ) { this.position.localize( position ); }

	@Override
	public float getFloatPosition( final int d ) { return this.position.getFloatPosition( d ); }

	@Override
	public double getDoublePosition( final int d ) { return this.position.getDoublePosition( d ); }
}