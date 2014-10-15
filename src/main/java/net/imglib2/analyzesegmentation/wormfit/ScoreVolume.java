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
	public double score( final InlierCells cells )
	{
		return ( cells.inliers.size() * cells.inliers.size() ) / volume( cells );
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
