package net.imglib2.analyzesegmentation.wormfit;

import javax.vecmath.Vector3f;

/**
 * Maximizes the cells per volume and rewards more cells
 * 
 * @author preibischs
 *
 */
public class ScoreVolume implements Score
{
	@Override
	public double score( final InlierCells previous, final InlierCells cells )
	{
		return ( cells.inliers.size() * cells.inliers.size() * cells.inliers.size() ) / ( volume( cells ) + 2f * vectorDifference( previous, cells ) );
	}

	protected double vectorDifference( final InlierCells previous, final InlierCells cells )
	{
		final Vector3f v0 = new Vector3f(
				previous.getP1().x - previous.getP0().x,
				previous.getP1().y - previous.getP0().y,
				previous.getP1().z - previous.getP0().z );

		final Vector3f v1 = new Vector3f(
				cells.getP1().x - cells.getP0().x,
				cells.getP1().y - cells.getP0().y,
				cells.getP1().z - cells.getP0().z );

		v0.normalize();
		v1.normalize();

		v1.sub( v0 );

		return v1.length();
	}

	protected double volume( final InlierCells cells )
	{
		final Vector3f v = new Vector3f(
				cells.getP1().x - cells.getP0().x,
				cells.getP1().y - cells.getP0().y,
				cells.getP1().z - cells.getP0().z );

		final double h = v.length();
		final double r0 = cells.r0;
		final double r1 = cells.r1;

		return h * Math.PI/3 * ( r0 * r0 + r0 * r1 + r1 * r1 );
	}
}
