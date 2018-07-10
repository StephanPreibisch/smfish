package net.imglib2.analyzesegmentation.wormfit;

import ij3d.Image3DUniverse;

import java.util.List;

import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import net.imglib2.analyzesegmentation.Cells;

public class FirstInlierCells extends InlierCells
{
	public FirstInlierCells( final List<InlierCell> inliers, final double r0, final double r1, final Point3d p0, final Point3d p1 )
	{
		super( inliers, r0, r1, p0, p1 );
	}

	/**
	 * Draw an elgonated tail for esthetic reasons
	 */
	@Override
	public void visualizeInliers( final Image3DUniverse univ, final Cells cells, final boolean colorInliers, final boolean drawLine, final boolean drawCone )
	{
		final Point3d saveP0 = new Point3d( p0 );
		final Vector3d v0 = new Vector3d( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );
		//p0.sub( v0 );
		//p0.sub( v0 );

		super.visualizeInliers( univ, cells, colorInliers, drawLine, drawCone );

		this.p0.set( saveP0 );
	}
}
