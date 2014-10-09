package net.imglib2.analyzesegmentation;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * 
 * @author Stephan Preibisch
 *
 */
public class Algebra
{
	/**
	 * Computes a Transform3D that will rotate the oldDirection into the newDirection.
	 * Note: vectors MUST be normalized for this to work!
	 * 
	 * @param oldDirection
	 * @param newDirection
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
