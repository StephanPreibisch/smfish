package net.imglib2.analyzesegmentation;

import ij3d.Image3DUniverse;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

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

	final ArrayList< InlierCells > segments = new ArrayList< InlierCells >();

	protected float[] vectorStep = new float[]{ /*20f, */ 40f, 20f, 15f, 10f, 5f, 2f, 1f };

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
		final Score score = new ScoreVolume();

		InlierCells i1 = defineFirstCells( initialRadius );
		if ( univ != null )
			i1.visualizeInliers( univ, cells, false, true, false );

		segments.add( i1 );

		InlierCells i = i1;
		
		int c = 0;
		do
		{
			c++;
			System.out.print( "segment=" + c );
			i = fitNextSegment( i, score, 5, c );
			System.out.println( ": " + i.getR0() + " " + i.getR1() );

			segments.add( i );
		}
		while ( c < 95 && i.getR1() > 0.1 );

		// worm43:c=39
		// worm41:c=27
		
		// dauer em:c=47
		// dauer confocal: 59
		SimpleMultiThreading.threadWait( 250 );
		makeScreenshot( c + 1 );
	}

	public ArrayList< InlierCells > getSegments() { return segments; }

	protected InlierCells fitNextSegment( final InlierCells previousInliers, final Score score, final double cutLength, final int sementCount )
	{
		// the initial radius and point are the last radius and point of the previous segment
		final double sr = previousInliers.getR1();

		// the initial search vector
		final Vector3d sv = new Vector3d(
				previousInliers.getP1().x - previousInliers.getP0().x,
				previousInliers.getP1().y - previousInliers.getP0().y,
				previousInliers.getP1().z - previousInliers.getP0().z );
		final double refL;

		if ( FirstInlierCells.class.isInstance( previousInliers ) )
			refL = sv.length() * 3f;
		else
			refL = sv.length() * cutLength;

		final Point3d sp = new Point3d( previousInliers.getP1() );

		InlierCells best = null;

		for ( int stepIndex = 0; stepIndex < vectorStep.length; ++stepIndex )
		{
			final float step = vectorStep[ stepIndex ];
			//System.out.println( step );

			// the best search vector found so far
			final Vector3d bestSV = new Vector3d( sv );

			// TODO: Remove manual stopping
			int from, to;

			if ( sementCount < 94 )
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
						final Vector3d v = new Vector3d(
								sv.x + xi * step,
								sv.y + yi * step,
								sv.z + zi * step );

						// normalize it to the same length
						final double l = refL;
						//for ( double l = refL * 0.9f; l <= refL * 1.1; l += 1f )
						{
							Algebra.normalizeLength( v, l );
	
							// compute the corresponding point
							final Point3d p = new Point3d( sp.x + v.x, sp.y + v.y, sp.z + v.z );
	
							// compute the quality of the fit
							//final float r0 = sr;
							for ( double r0 = sr * 0.9; r0 <= sr * 1.1f; r0 += 0.75 )
								for ( double r1 = 0; r1 <= sr * 1.4; r1 += 1 )
								{
									final InlierCells inliers = testGuess( sp, p, r0, r1, cells );
			
									if ( best == null || score.score( previousInliers, best ) < score.score( previousInliers, inliers ) )
									{
										//if ( best != null &&  univ != null )
										//	best.unvisualizeInliers( univ, cells );
	
										if ( r0 != previousInliers.getR1() )
										{
											// if ( univ != null )
											//	previousInliers.unvisualizeInliers( univ, cells );
											previousInliers.setR1( r0 );
											// if ( univ != null )
											//	previousInliers.visualizeInliers( univ, cells, true, true, true );
										}
	
										bestSV.set( v );
										best = inliers;
										// if ( univ != null )
										//	best.visualizeInliers( univ, cells, true, true, true );
		
										//System.out.println( step + ": l=" + l + " r0=" + r0 + " r1=" + r1 + " score=" + score.score( previousInliers, best ) + " |cells|=" + best.getInlierCells().size() );
										// if ( univ != null )
										//	SimpleMultiThreading.threadWait( 25 );
									}
								}
						}
					}

			// update the search vector to the best solution from this scale
			sv.set( bestSV );
		}

		// if ( univ != null )
		//	preCut.visualizeInliers( univ, cells, true, false, false );

		//
		// only take 1/3 of the vector
		//
		final Vector3d v = new Vector3d(
				best.getP1().x - best.getP0().x,
				best.getP1().y - best.getP0().y,
				best.getP1().z - best.getP0().z );

		Algebra.normalizeLength( v, v.length() / cutLength );
		
		final Point3d p1 = new Point3d(
				best.getP0().x + v.x,
				best.getP0().y + v.y,
				best.getP0().z + v.z );

		// if ( univ != null )
		//	best.unvisualizeInliers( univ, cells );
		best = testGuess( best.getP0(), p1, best.getR0(), best.getR0() * (1.0f - 1.0f/cutLength) + best.getR1() * (1.0f/cutLength), cells );
		if ( univ != null )
			best.visualizeInliers( univ, cells, true, true, true );

		SimpleMultiThreading.threadWait( 250 );
		//makeScreenshot( sementCount );
		//preCut.unvisualizeInliers( univ, cells );

		return best;
	}

	protected InlierCells testGuess( final Point3d p0, final Point3d p1, final double r0, final double r1, final Cells cells )
	{
		final ArrayList< InlierCell > inliers = new ArrayList< InlierCell >();
		final Point3d q = new Point3d();

		final double r[] = new double[ 2 ];

		// reference point is in the middle of the segmented line
		final RealPoint ref = new RealPoint( new double[]{
				(p1.x - p0.x)/2 + p0.x,
				(p1.y - p0.y)/2 + p0.y,
				(p1.z - p0.z)/2 + p0.z } );

		// search radius is the half the length of the vector + maxR
		final Vector3d v = new Vector3d( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );

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
		final Point3d p0 = new Point3d( cell0.getPosition().getFloatPosition( 0 ), cell0.getPosition().getFloatPosition( 1 ), cell0.getPosition().getFloatPosition( 2 ) );
		final Point3d p1 = new Point3d( cell1.getPosition().getFloatPosition( 0 ), cell1.getPosition().getFloatPosition( 1 ), cell1.getPosition().getFloatPosition( 2 ) );

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
