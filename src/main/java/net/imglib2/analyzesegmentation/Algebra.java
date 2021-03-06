package net.imglib2.analyzesegmentation;

import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.AxisAngle4d;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

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
	 * Normalize the length of Vector3d v1 so that it has the same length as Vector3d v2
	 * @param v1 - to be normalized
	 * @param v2 - the reference vector
	 */
	public static void normalizeLength( final Vector3d v1, final Vector3d v2 )
	{
		normalizeLength( v1, v2.length() );
	}

	/**
	 * Normalize the length of Vector3d v1 so that it has length l
	 * @param v - to be normalized
	 * @param l - new length
	 */
	public static void normalizeLength( final Vector3d v, final double l )
	{
		final double c = l / v.length();

		v.x *= c;
		v.y *= c;
		v.z *= c;
	}

	/**
	 * Given a line segment from p0 to p1, compute the relative location 
	 * (p0 = 0 ... p1 = 1) on this line where the distance to point q is minimal.
	 * 
	 * See: http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
	 * 
	 * @param p0 - line segment start
	 * @param p1 - line segment end
	 * @param q - query point
	 * @return the relative location
	 */
	public static double pointOfShortestDistance( final Point3d p0, final Point3d p1, final Point3d q )
	{
		final Vector3d v0 = new Vector3d( p0.x - q.x, p0.y - q.y, p0.z - q.z );
		final Vector3d v1 = new Vector3d( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );

		return -v0.dot( v1 ) / v1.lengthSquared();
	}

	/**
	 * Given a vector defined by a line segment from p0 to p1, compute the shortest 
	 * squared distance between point q and the vector.
	 * 
	 * See: http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
	 * 
	 * @param p0 - line segment start
	 * @param p1 - line segment end
	 * @param q - query point
	 * @return the square distance
	 */
	public static double shortestSquaredDistance( final Point3d p0, final Point3d p1, final Point3d q )
	{
		final Vector3d v0 = new Vector3d( p0.x - q.x, p0.y - q.y, p0.z - q.z );
		final Vector3d v1 = new Vector3d( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );

		final double v0ls = v0.lengthSquared();
		final double v1ls = v1.lengthSquared();
		final double dotp = v0.dot( v1 );

		return ( v0ls * v1ls - dotp * dotp ) / v1ls;
	}

	/**
	 * Given a vector defined by a line segment from p0 to p1, compute the shortest distance 
	 * between point q and the vector.
	 * 
	 * See: http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
	 * 
	 * @param p0 - line segment start
	 * @param p1 - line segment end
	 * @param q - query point
	 * @return the distance
	 */
	public static double shortestDistance( final Point3d p0, final Point3d p1, final Point3d q )
	{
		return Math.sqrt( shortestSquaredDistance( p0, p1, q ) );
	}

	/**
	 * Given a line segment from p0 to p1, compute the relative location (p0 = 0 ... p1 = 1) on
	 * this line where the distance to point q is minimal as well as the shortest squared distance
	 * to the point.
	 * 
	 * See: http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
	 * 
	 * @param p0 - line segment start
	 * @param p1 - line segment end
	 * @param q - query point
	 * @param result - result[ 0 ] will contain the squared distance, result[ 1 ] the relative point on the segmented line
	 */
	public static void shortestSquaredDistanceAndPoint( final Point3d p0, final Point3d p1, final Point3d q, final double[] result )
	{
		final Vector3d v0 = new Vector3d( p0.x - q.x, p0.y - q.y, p0.z - q.z );
		final Vector3d v1 = new Vector3d( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );

		final double v0ls = v0.lengthSquared();
		final double v1ls = v1.lengthSquared();
		final double dotp = v0.dot( v1 );

		result[ 0 ] = ( v0ls * v1ls - dotp * dotp ) / v1ls;
		result[ 1 ] = -dotp / v1ls;
	}

	/**
	 * Computes a Transform3D that will rotate vector v0 into the direction of vector v1.
	 * Note: vectors MUST be normalized for this to work!
	 *
	 * @param v0 - vector 0
	 * @param v1 - vector 1
	 * @return the transformation
	 */
	public static Transform3D getRotation( final Vector3d v0, final Vector3d v1 )
	{
		// the rotation axis is defined by the cross product
		final Vector3d rotAxis = new Vector3d();
		rotAxis.cross( v0, v1 );
		rotAxis.normalize();

		// if the cross product returns NaN, the vectors already point in the same direction,
		// so we return the identity transform
		if ( Double.isNaN( rotAxis.x ) )
			return new Transform3D();

		// the rotation angle is defined by the dot product (if normalized)
		// make sure it is really between [-1,1], numerical instabilities can lead to e.g. 1.0000001
		final double angle = Math.min( 1.0f, Math.max( -1.0f, v0.dot( v1 ) ) );

		// Do an axis/angle 3d transformation
		final Transform3D t = new Transform3D();
		t.set( new AxisAngle4d( rotAxis, Math.acos( angle ) ) );

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
	 * @return the transformation
	 */
	public static Transform3D getTransformation(
			final Point3d p00,
			final Point3d p01,
			final Point3d p10,
			final Point3d p11,
			final boolean scale )
	{
		// compute the vectors
		final Vector3d v0 = new Vector3d( p01.x - p00.x, p01.y - p00.y, p01.z - p00.z );
		final Vector3d v1 = new Vector3d( p11.x - p10.x, p11.y - p10.y, p11.z - p10.z );

		// the final transformation
		Transform3D transform = new Transform3D();

		// first translate to origin
		transform.setTranslation( new Vector3d( -p00.x, -p00.y, -p00.z ) );

		// second, scale if wanted
		if ( scale )
		{
			final double scaling = v1.length() / v0.length();

			// if the scaling turns out to be NaN, v0 must be of length zero,
			// in this case we just do nothing
			if ( !Double.isNaN( scaling ) )
				transform.setScale( scaling );
		}

		// third, preconcatenate the rotation
		v0.normalize();
		v1.normalize();

		transform.mul( getRotation( v0, v1 ), transform );

		// fourth, preconcatenate the translation to the origin of the second line segment
		final Transform3D t = new Transform3D();
		t.setTranslation( new Vector3d( p10.x, p10.y, p10.z ) );
		transform.mul( t, transform );

		return transform;
	}
}
