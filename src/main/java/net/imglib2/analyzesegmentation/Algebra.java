package net.imglib2.analyzesegmentation;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 *  Copyright (c) 2014 Stephan Preibisch
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @author Stephan Preibisch
 */
public class Algebra
{
	/**
	 * Given a line segment from p0 >> p1, compute the relative location 
	 * (p0 = 0 > p1 = 1) on this line where the distance to point q is minimal.
	 * 
	 * See: http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
	 * 
	 * @param p0 - line segment start
	 * @param p1 - line segment end
	 * @param q - query point
	 * @return
	 */
	public static double pointOfShortestDistance( final Point3f p0, final Point3f p1, final Point3f q )
	{
		final Vector3f v0 = new Vector3f( p0.x - q.x, p0.y - q.y, p0.z - q.z );
		final Vector3f v1 = new Vector3f( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );

		return -v0.dot( v1 ) / v1.lengthSquared();
	}

	/**
	 * Given a vector defined by a line segment from p0 >> p1, compute the shortest 
	 * squared distance between point q and the vector.
	 * 
	 * See: http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
	 * 
	 * @param p0 - line segment start
	 * @param p1 - line segment end
	 * @param q - query point
	 * @return
	 */
	public static double shortestSquaredDistance( final Point3f p0, final Point3f p1, final Point3f q )
	{
		final Vector3f v0 = new Vector3f( p0.x - q.x, p0.y - q.y, p0.z - q.z );
		final Vector3f v1 = new Vector3f( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );

		final float v0ls = v0.lengthSquared();
		final float v1ls = v1.lengthSquared();
		final float dotp = v0.dot( v1 );

		return ( v0ls * v1ls - dotp * dotp ) / v1ls;
	}

	/**
	 * Given a vector defined by a line segment from p0 >> p1, compute the shortest distance 
	 * between point q and the vector.
	 * 
	 * See: http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
	 * 
	 * @param p0 - line segment start
	 * @param p1 - line segment end
	 * @param q - query point
	 * @return
	 */
	public static double shortestDistance( final Point3f p0, final Point3f p1, final Point3f q )
	{
		return Math.sqrt( shortestSquaredDistance( p0, p1, q ) );
	}

	/**
	 * Computes a Transform3D that will rotate vector v0 into the direction of vector v1.
	 * Note: vectors MUST be normalized for this to work!
	 *
	 * @param v0
	 * @param v1
	 * @return
	 */
	public static Transform3D getRotation( final Vector3f v0, final Vector3f v1 )
	{
		// the rotation axis is defined by the cross product
		final Vector3f rotAxis = new Vector3f();
		rotAxis.cross( v0, v1 );
		rotAxis.normalize();

		// if the cross product returns NaN, the vectors already point in the same direction,
		// so we return the identity transform
		if ( Float.isNaN( rotAxis.x ) )
			return new Transform3D();

		// the rotation angle is defined by the dot product (if normalized)
		final float angle = v0.dot( v1 );

		// Do an axis/angle 3d transformation
		final Transform3D t = new Transform3D();
		t.set( new AxisAngle4f( rotAxis, (float)Math.acos( angle ) ) );

		return t;
	}

	/**
	 * Computes a Transform3D that will transform one line segment to the other line segment.
	 * 
	 * @param p00 - first line segment start
	 * @param p01 - first line segment end
	 * @param p10 - second line segment start
	 * @param p11 - second line segment end
	 * @param scale - if the Transform3D should also scale the line segment
	 * @return
	 */
	public static Transform3D getTransformation(
			final Point3f p00,
			final Point3f p01,
			final Point3f p10,
			final Point3f p11,
			final boolean scale )
	{
		// compute the vectors
		final Vector3f v0 = new Vector3f( p01.x - p00.x, p01.y - p00.y, p01.z - p00.z );
		final Vector3f v1 = new Vector3f( p11.x - p10.x, p11.y - p10.y, p11.z - p10.z );

		// the final transformation
		Transform3D transform = new Transform3D();

		// first translate to origin
		transform.setTranslation( new Vector3f( -p00.x, -p00.y, -p00.z ) );

		// second, scale if wanted
		if ( scale )
		{
			final float scaling = v1.length() / v0.length();

			// if the scaling turns out to be NaN, v0 must be of length zero,
			// in this case we just do nothing
			if ( !Float.isNaN( scaling ) )
				transform.setScale( scaling );
		}

		// third, preconcatenate the rotation
		v0.normalize();
		v1.normalize();

		transform.mul( getRotation( v0, v1 ), transform );

		// fourth, preconcatenate the translation to the origin of the second line segment
		final Transform3D t = new Transform3D();
		t.setTranslation( new Vector3f( p10.x, p10.y, p10.z ) );
		transform.mul( t, transform );

		return transform;
	}
}
