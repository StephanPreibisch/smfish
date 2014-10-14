package net.imglib2.analyzesegmentation;

import ij3d.Image3DUniverse;

import java.util.ArrayList;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import mpicbg.imglib.multithreading.SimpleMultiThreading;
import net.imglib2.analyzesegmentation.wormfit.FirstInlierCells;
import net.imglib2.analyzesegmentation.wormfit.InlierCell;
import net.imglib2.analyzesegmentation.wormfit.InlierCells;

public class FindWormOutline
{
	final Image3DUniverse univ;
	final Cells cells;
	final Cell cell0, cell1;
	final float initialRadius;

	protected float[] vectorStep = new float[]{ 20f, 10f, 5f, 2f, 1f, 0.5f };

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

		InlierCells i1 = defineFirstCells( initialRadius );
		i1.visualizeInliers( univ, cells, inlierColor );

		InlierCells i2 = fitNextSegment( i1 );
		i2.visualizeInliers( univ, cells, inlierColor );
	}

	protected InlierCells fitNextSegment( final InlierCells previousInliers )
	{
		// the initial radius and point are the last radius and point of the previous segment
		final float sr = previousInliers.getR1();
		final Point3f sp = previousInliers.getP1();
		final Vector3f d = new Vector3f( sp.x - previousInliers.getP0().x, sp.y - previousInliers.getP0().y, sp.z - previousInliers.getP0().z );
		final float l = d.length();

		for ( int stepIndex = 0; stepIndex < vectorStep.length; ++stepIndex )
		{
			final float step = vectorStep[ stepIndex ];
			
			for ( int zi = -1; zi <= 1; ++zi )
				for ( int yi = -1; yi <= 1; ++yi )
					for ( int xi = -1; xi <= 1; ++xi )
					{
						xi = yi = zi = 0;
						
						// compute the test vector
						final Vector3f v = new Vector3f( d.x + xi * step, d.y + yi * step, d.z + zi * step );

						// normalize it to the same length
						Algebra.normalizeLength( v, l );

						// compute the corresponding point
						final Point3f p = new Point3f( sp.x + v.x, sp.y + v.y, sp.z + v.z );

						// compute the quality of the fit
						final InlierCells inliers = smallestRadius( sp, p, sr, cells );
						inliers.visualizeInliers( univ, cells, new Color3f( 1, 0, 0 ) );
						SimpleMultiThreading.threadHaltUnClean();
					}
		}
		
		return null;
	}

	protected InlierCells smallestRadius( final Point3f p0, final Point3f p1, final float r0, final Cells cells )
	{
		Color3f col = new Color3f( 1, 0, 0 );
		InlierCells inliers = null;
		InlierCells previous = null;

		for ( float r1 = 0; r1 <= r0 * 2; r1 += 1.0f )
		{
			InlierCells current = testGuess( p0, p1, r0, r1, cells );
			if ( previous != null )
				previous.unvisualizeInliers( univ, cells );
			current.visualizeInliers( univ, cells, col );
			previous = current;
			SimpleMultiThreading.threadWait( 500 );

			if ( inliers == null || inliers.getInlierCells().size() < current.getInlierCells().size() )
				inliers = current;

			System.out.println( r1 + ": " + inliers.getInlierCells().size() );
		}

		previous.unvisualizeInliers( univ, cells );

		return inliers;
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

			if ( cell.getId() == 5 )
			{
				System.out.println( "t:" + t + " d:" + dist + " d+r:" + (dist + cell.getRadius()) + " t:" + (r0 * ( 1- t) + r1 * t ) + " r0:" + r0 + " r1:" + r1 );
			}

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
