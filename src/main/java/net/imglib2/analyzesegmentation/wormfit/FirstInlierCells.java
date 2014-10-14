package net.imglib2.analyzesegmentation.wormfit;

import ij3d.Image3DUniverse;

import java.util.List;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import net.imglib2.analyzesegmentation.Cells;

public class FirstInlierCells extends InlierCells
{
	public FirstInlierCells( final List<InlierCell> inliers, final float r0, final float r1, final Point3f p0, final Point3f p1 )
	{
		super( inliers, r0, r1, p0, p1 );
	}

	/**
	 * Draw an elgonated tail for esthetic reasons
	 */
	@Override
	public void visualizeInliers( final Image3DUniverse univ, final Cells cells, final Color3f inlierColor )
	{
		final Point3f saveP0 = new Point3f( p0 );
		final Vector3f v0 = new Vector3f( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );
		p0.sub( v0 );
		p0.sub( v0 );

		super.visualizeInliers( univ, cells, inlierColor );

		this.p0.set( saveP0 );
	}
}
