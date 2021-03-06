package net.imglib2.analyzesegmentation.wormfit;

import org.scijava.vecmath.Vector3d;

/**
 * Maximizes the cells per volume and rewards more cells
 * 
 * @author preibischs
 *
 */
public class ScoreVolume implements Score
{
	final Vector3d v0 = new Vector3d();
	final Vector3d v1 = new Vector3d();

	@Override
	public double score( final InlierCells previous, final InlierCells cells )
	{
		// good
		final double nc = cells.inliers.size();

		// bad
		final double vd = vectorDifference( previous, cells );
		final double rd = cells.getR1() - cells.getR0();
		final double vol = volume( cells );

		// making smaller is good, bigger not
		final double rf = rd < 0 ? 0.1 : 10;
			
		return ( 4 * nc * nc * nc ) / ( Math.sqrt( vol ) + rf*rd*rd + 1000*vd );
	}

	protected double vectorDifference( final InlierCells previous, final InlierCells cells )
	{
		v0.set(
			previous.getP1().x - previous.getP0().x,
			previous.getP1().y - previous.getP0().y,
			previous.getP1().z - previous.getP0().z );

		v1.set(
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
		final Vector3d v = new Vector3d(
				cells.getP1().x - cells.getP0().x,
				cells.getP1().y - cells.getP0().y,
				cells.getP1().z - cells.getP0().z );

		final double h = v.length();
		final double r0 = cells.r0;
		final double r1 = cells.r1;

		return h * Math.PI/3 * ( r0 * r0 + r0 * r1 + r1 * r1 );
	}
}
