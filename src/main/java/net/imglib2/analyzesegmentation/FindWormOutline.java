package net.imglib2.analyzesegmentation;

import ij3d.Image3DUniverse;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import net.imglib2.RealPoint;
import net.imglib2.analyzesegmentation.wormfit.FirstInlierCells;
import net.imglib2.analyzesegmentation.wormfit.InlierCell;
import net.imglib2.analyzesegmentation.wormfit.InlierCells;
import net.imglib2.analyzesegmentation.wormfit.Score;
import net.imglib2.analyzesegmentation.wormfit.ScoreVolume;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;

public class FindWormOutline
{
	final Image3DUniverse univ;
	final Cells cells;
	final Cell cell0, cell1;
	final float initialRadius;

	protected float[] vectorStep = new float[]{ /*20f, */ 20f, 10f, 5f, 2f, 1f };

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
			System.out.print( "segment=" + c );
			i = fitNextSegment( i, score, 3, c );
			System.out.println( ": " + i.getR0() + " " + i.getR1() );
		}
		while ( c < 40 && i.getR1() > 0.1 );
	}

	protected InlierCells fitNextSegment( final InlierCells previousInliers, final Score score, final float cutLength, final int sementCount )
	{
		final Color3f c = new Color3f( 1, 0, 0 );

		// the initial radius and point are the last radius and point of the previous segment
		final float sr = previousInliers.getR1();

		// the initial search vector
		final Vector3f sv = new Vector3f(
				previousInliers.getP1().x - previousInliers.getP0().x,
				previousInliers.getP1().y - previousInliers.getP0().y,
				previousInliers.getP1().z - previousInliers.getP0().z );
		final float refL;

		if ( FirstInlierCells.class.isInstance( previousInliers ) )
			refL = sv.length() * 2f;
		else
			refL = sv.length() * cutLength;

		final Point3f sp = new Point3f( previousInliers.getP1() );

		InlierCells best = null;

		for ( int stepIndex = 0; stepIndex < vectorStep.length; ++stepIndex )
		{
			final float step = vectorStep[ stepIndex ];
			//System.out.println( step );

			// the best search vector found so far
			final Vector3f bestSV = new Vector3f( sv );

			int from, to;

			if ( sementCount < 38 )
			{
				from = -1;
				to = 1;
			}
			else
			{
				from = to = 0;
			}

			for ( int zi = from; zi <= to; ++zi )
				for ( int yi = from; yi <= to; ++yi )
					for ( int xi = from; xi <= to; ++xi )
					{
						// compute the test vector
						final Vector3f v = new Vector3f(
								sv.x + xi * step,
								sv.y + yi * step,
								sv.z + zi * step );

						// normalize it to the same length
						final float l = refL;
						//for ( float l = refL * 0.9f; l <= refL * 1.1; l += 1f )
						{
							Algebra.normalizeLength( v, l );
	
							// compute the corresponding point
							final Point3f p = new Point3f( sp.x + v.x, sp.y + v.y, sp.z + v.z );
	
							// compute the quality of the fit
							//final float r0 = sr;
							for ( float r0 = sr * 0.9f; r0 <= sr * 1.1f; r0 += 0.75f )
								for ( float r1 = 0; r1 <= sr * 1.4f; r1 += 1f )
								{
									final InlierCells inliers = testGuess( sp, p, r0, r1, cells );
			
									if ( best == null || score.score( previousInliers, best ) < score.score( previousInliers, inliers ) )
									{
										if ( best != null )
											best.unvisualizeInliers( univ, cells );
	
										if ( r0 != previousInliers.getR1() )
										{
											//previousInliers.unvisualizeInliers( univ, cells );
											previousInliers.setR1( r0 );
											//previousInliers.visualizeInliers( univ, cells, c );
										}
	
										bestSV.set( v );
										best = inliers;
										//best.visualizeInliers( univ, cells, c );
		
										//System.out.println( step + ": l=" + l + " r0=" + r0 + " r1=" + r1 + " score=" + score.score( previousInliers, best ) + " |cells|=" + best.getInlierCells().size() );
										//SimpleMultiThreading.threadWait( 25 );
									}
								}
						}
					}

			// update the search vector to the best solution from this scale
			sv.set( bestSV );
		}

		//
		// only take 1/3 of the vector
		//
		final Vector3f v = new Vector3f(
				best.getP1().x - best.getP0().x,
				best.getP1().y - best.getP0().y,
				best.getP1().z - best.getP0().z );

		Algebra.normalizeLength( v, v.length() / cutLength );
		
		final Point3f p1 = new Point3f(
				best.getP0().x + v.x,
				best.getP0().y + v.y,
				best.getP0().z + v.z );

		best.unvisualizeInliers( univ, cells );
		best = testGuess( best.getP0(), p1, best.getR0(), best.getR0() * (1.0f - 1.0f/cutLength) + best.getR1() * (1.0f/cutLength), cells );
		best.visualizeInliers( univ, cells, c );

		SimpleMultiThreading.threadWait( 250 );
		//SimpleMultiThreading.threadHaltUnClean();

		return best;
	}

	protected InlierCells testGuess( final Point3f p0, final Point3f p1, final float r0, final float r1, final Cells cells )
	{
		final ArrayList< InlierCell > inliers = new ArrayList< InlierCell >();
		final Point3f q = new Point3f();

		final double r[] = new double[ 2 ];

		// reference point is in the middle of the segmented line
		final RealPoint ref = new RealPoint( new double[]{
				(p1.x - p0.x)/2 + p0.x,
				(p1.y - p0.y)/2 + p0.y,
				(p1.z - p0.z)/2 + p0.z } );

		// search radius is the half the length of the vector + maxR
		final Vector3f v = new Vector3f( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );

		// look for all points in range
		RadiusNeighborSearchOnKDTree< Cell > search = cells.getSearch();
		search.search( ref, v.length() / 2.0 + Math.max( r0, r1 ), false );

		// test which points are along the defined line segment
		for ( int c = 0; c < search.numNeighbors(); ++c )
		{
			final Cell cell = search.getSampler( c ).get();

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

	public static void makeScreenshot( final int index )
	{
		makeScreenshot( new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()), index);
	}

	public static void makeScreenshot( final Rectangle rect, final int index )
	{
		try
		{
			BufferedImage image = new Robot().createScreenCapture( rect );
			ImageIO.write( image, "png", new File( "screenshot_" + index + ".png" ) );
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
