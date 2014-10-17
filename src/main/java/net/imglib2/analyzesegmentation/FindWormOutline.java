package net.imglib2.analyzesegmentation;

import ij3d.Image3DUniverse;

import java.util.ArrayList;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import net.imglib2.analyzesegmentation.wormfit.FirstInlierCells;
import net.imglib2.analyzesegmentation.wormfit.InlierCell;
import net.imglib2.analyzesegmentation.wormfit.InlierCells;
import net.imglib2.analyzesegmentation.wormfit.Score;
import net.imglib2.analyzesegmentation.wormfit.ScoreVolume;
import net.imglib2.multithreading.SimpleMultiThreading;

public class FindWormOutline
{
	final Image3DUniverse univ;
	final Cells cells;
	final Cell cell0, cell1;
	final float initialRadius;

	protected float[] vectorStep = new float[]{ 40f, 30f, 20f, 15f, 10f, 7.5f, 5f, 2f, 1f };

	public FindWormOutline( final Image3DUniverse univ, final Cells cells, final Cell cell0, final Cell cell1, final float initialRadius )
	{
		this.univ = univ;
		this.cells = cells;
		this.cell0 = cell0;
		this.cell1 = cell1;
		this.initialRadius = initialRadius;
	}

	public void findOutline()
	{
		final Color3f inlierColor = new Color3f( 1, 0, 0 );
		final Score score = new ScoreVolume();

		InlierCells i1 = defineFirstCells( initialRadius );
		i1.visualizeInliers( univ, cells, inlierColor );

		InlierCells i = i1;
		
		int c = 0;
		do
		{
			c++;
			i = fitNextSegment( i, score );
		}
		while ( c < 10 && i.getR1() > 0 );
	}

	protected InlierCells fitNextSegment( final InlierCells previousInliers, final Score score )
	{
		final Color3f c = new Color3f( 1, 0, 0 );

		// the initial radius and point are the last radius and point of the previous segment
		final float sr = previousInliers.getR1();

		// the initial search vector
		final Vector3f sv = new Vector3f(
				previousInliers.getP1().x - previousInliers.getP0().x,
				previousInliers.getP1().y - previousInliers.getP0().y,
				previousInliers.getP1().z - previousInliers.getP0().z );
		final float refL = sv.length();

		final Point3f sp = new Point3f( previousInliers.getP1() );

		InlierCells best = null;

		for ( int stepIndex = 0; stepIndex < vectorStep.length; ++stepIndex )
		{
			final float step = vectorStep[ stepIndex ];
			System.out.println( step );

			// the best search vector found so far
			final Vector3f bestSV = new Vector3f( sv );

			for ( int zi = -1; zi <= 1; ++zi )
				for ( int yi = -1; yi <= 1; ++yi )
					for ( int xi = -1; xi <= 1; ++xi )
					{
						// compute the test vector
						final Vector3f v = new Vector3f(
								sv.x + xi * step,
								sv.y + yi * step,
								sv.z + zi * step );

						// normalize it to the same length
						for ( float l = refL * 0.9f; l <= refL * 1.1; l += 1f )
						{
							Algebra.normalizeLength( v, l );
	
							// compute the corresponding point
							final Point3f p = new Point3f( sp.x + v.x, sp.y + v.y, sp.z + v.z );
	
							// compute the quality of the fit
							for ( float r0 = sr; r0 <= sr * 1.25f; r0 += 1f )
								for ( float r1 = 0; r1 <= sr * 1.5f; r1 += 1f )
								{
									final InlierCells inliers = testGuess( sp, p, r0, r1, cells );
			
									if ( best == null || score.score( previousInliers, best ) < score.score( previousInliers, inliers ) )
									{
										if ( best != null )
											best.unvisualizeInliers( univ, cells );
	
										if ( r0 != previousInliers.getR1() )
										{
											previousInliers.unvisualizeInliers( univ, cells );
											previousInliers.setR1( r0 );
											previousInliers.visualizeInliers( univ, cells, c );
										}
	
										bestSV.set( v );
										best = inliers;
										best.visualizeInliers( univ, cells, c );
		
										System.out.println( step + ": l=" + l + " r0=" + r0 + " r1=" + r1 + " score=" + score.score( previousInliers, best ) + " |cells|=" + best.getInlierCells().size() );
										SimpleMultiThreading.threadWait( 250 );
									}
								}
						}
					}

			// update the search vector to the best solution from this scale
			sv.set( bestSV );
		}
		//SimpleMultiThreading.threadHaltUnClean();
		return best;
	}

	protected InlierCells testGuess( final Point3f p0, final Point3f p1, final float r0, final float r1, final Cells cells )
	{
		final ArrayList< InlierCell > inliers = new ArrayList< InlierCell >();
		final Point3f q = new Point3f();

		final double r[] = new double[ 2 ];

		// test which points are along the defined line segment
		for ( final Cell cell : cells.getCells().values() )
		{
			q.x = cell.getPosition().getFloatPosition( 0 );
			q.y = cell.getPosition().getFloatPosition( 1 );
			q.z = cell.getPosition().getFloatPosition( 2 );
			
			Algebra.shortestSquaredDistanceAndPoint( p0, p1, q, r );
			final double dist = Math.sqrt( r[ 0 ] );
			final double t = r[ 1 ];

			if ( t >= 0 && t <= 1 )
			{
				final double distThres = r0 * (1 - t) + r1 * t;
				
				if ( dist + cell.getRadius() <= distThres )
					inliers.add( new InlierCell( cell, dist, t ) );
			}
		}

		return new InlierCells( inliers, r0, r1, p0, p1 );
	}

	protected InlierCells defineFirstCells( final float initialRadius )
	{
		final Point3f p0 = new Point3f( cell0.getPosition().getFloatPosition( 0 ), cell0.getPosition().getFloatPosition( 1 ), cell0.getPosition().getFloatPosition( 2 ) );
		final Point3f p1 = new Point3f( cell1.getPosition().getFloatPosition( 0 ), cell1.getPosition().getFloatPosition( 1 ), cell1.getPosition().getFloatPosition( 2 ) );

		final ArrayList< InlierCell > inliers = new ArrayList< InlierCell >();

		inliers.add( new InlierCell( cell0, 0, 0 ) );
		inliers.add( new InlierCell( cell1, 0, 1 ) );

		return new FirstInlierCells( inliers, 0, initialRadius, p0, p1 );
	}
}
