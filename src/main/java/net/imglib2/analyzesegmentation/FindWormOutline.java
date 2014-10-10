package net.imglib2.analyzesegmentation;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ij3d.Content;
import ij3d.Image3DUniverse;

public class FindWormOutline
{
	final Image3DUniverse univ;
	final Cells cells;
	final Cell cell1, cell2;

	public FindWormOutline( final Image3DUniverse univ, final Cells cells, final Cell cell1, final Cell cell2 )
	{
		this.univ = univ;
		this.cells = cells;
		this.cell1 = cell1;
		this.cell2 = cell2;
	}

	public void findOutline()
	{
		final List< Point3f > lineMesh = new ArrayList< Point3f >();

		final Point3f p10 = new Point3f( cell1.getPosition().getFloatPosition( 0 ), cell1.getPosition().getFloatPosition( 1 ), cell1.getPosition().getFloatPosition( 2 ) );
		final Point3f p11 = new Point3f( cell2.getPosition().getFloatPosition( 0 ), cell2.getPosition().getFloatPosition( 1 ), cell2.getPosition().getFloatPosition( 2 ) );

		final Vector3f v = new Vector3f( p11.x - p10.x, p11.y - p10.y, p11.z - p10.z );
		p10.sub( v );
		p10.sub( v );

		lineMesh.add( p10 );
		lineMesh.add( p11 );
		
		final Content content = univ.addLineMesh( lineMesh, new Color3f(), "InitialVector", false );
		content.showCoordinateSystem( false );

		v.set( p11.x - p10.x, p11.y - p10.y, p11.z - p10.z );

		final Point3f p00 = new Point3f( 0, -v.length()/2, 0 );
		final Point3f p01 = new Point3f( 0, v.length()/2, 0 );

		VisualizeSegmentation.drawTruncatedCone( 0, 20, v.length(), univ, Algebra.getTransformation( p00, p01, p10, p11, false ), new Color3f( 1, 0, 1 ), 0.75f );
		//drawTruncatedCone( 10, 20, 100, univ, new Transform3D(), new Color3f( 1, 0, 1 ), 0.5f );
	}

	protected void fitSegment( final Point3f start, final float startRadius, final Vector3f direction )
	{
		
	}
}
