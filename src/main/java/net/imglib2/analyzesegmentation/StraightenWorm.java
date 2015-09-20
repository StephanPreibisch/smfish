package net.imglib2.analyzesegmentation;

import ij3d.Image3DUniverse;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import mpicbg.imglib.util.Util;
import net.imglib2.RealPoint;
import net.imglib2.analyzesegmentation.wormfit.InlierCells;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import spim.fiji.spimdata.interestpoints.InterestPoint;

public class StraightenWorm
{
	public static Cells stretchWormCells( final FindWormOutline fwo, final Cells cells )
	{
		final List< Pair< Integer, float[] > > curved = new ArrayList< Pair<Integer,float[]> >();
		final float[] loc = new float[ 3 ];

		for ( final int id : cells.getCells().keySet() )
		{
			final Cell cell = cells.getCells().get( id );
			cell.getPosition().localize( loc );
			curved.add( new ValuePair< Integer, float[] >( cell.getId(), loc.clone() ) );
		}

		final List< Pair< Integer, float[] > > straight = stretchWorm( fwo, curved );

		final Cells cellsNew = new Cells();

		for ( final Pair< Integer, float[] > p : straight )
		{
			final Cell c = cells.getCells().get( p.getA() );
			final RealPoint position = new RealPoint( 3 );
			for ( int d = 0; d < 3; ++d )
				position.setPosition( p.getB()[ d ], d );
			cellsNew.getCells().put( p.getA(), new Cell( p.getA(), position, c.getRadius() ) );
		}

		return cellsNew;
	}

	public static List< InterestPoint > stretchWormInterestPoints( final FindWormOutline fwo, final List< InterestPoint > list )
	{
		final List< Pair< Integer, float[] > > curved = new ArrayList< Pair<Integer,float[]> >();

		for ( final InterestPoint p : list )
			curved.add( new ValuePair< Integer, float[] >( p.getId(), p.getL() ) );

		final List< Pair< Integer, float[] > > straight = stretchWorm( fwo, curved );

		final ArrayList< InterestPoint > straightInterestPoints = new ArrayList< InterestPoint >();

		for ( final Pair< Integer, float[] > p : straight )
			straightInterestPoints.add(  new InterestPoint( p.getA(), p.getB() ) );

		return straightInterestPoints;
	}

	public static List< Pair< Integer, float[] > > stretchWorm( final FindWormOutline fwo, final List< Pair< Integer, float[] > > list )
	{
		final List< Pair< Integer, float[] > > straight = new ArrayList< Pair< Integer, float[] > >();

		for ( final Pair< Integer, float[] > p : list )
		{
			//System.out.println( Util.printCoordinates( p.getL() ) );

			// find closest line segment and cummulative distance from tail of the worm
			InlierCells closest = null;
			double dist = -1;
			double sumD = -1;
			double l = -1;
	
			double sumDtmp = 0;

			final Point3f q = new Point3f( p.getB() );

			for ( final InlierCells i : fwo.getSegments() )
			{
				final double d = Algebra.shortestDistance( i.getP0(), i.getP1(), q );
				final double point = Algebra.pointOfShortestDistance( i.getP0(), i.getP1(), q );

				final Vector3f ltmp = new Vector3f(
						i.getP1().x - i.getP0().x,
						i.getP1().y - i.getP0().y,
						i.getP1().z - i.getP0().z );

				if ( point >= 0 && point <= 1.2 && ( closest == null || d < dist ) )
				{
					closest = i;
					dist = d;
					sumD = sumDtmp; // the length at the beginning of this segment
					l = ltmp.length(); // the length of this vector
					//System.out.println( d + ": " + i.getP0() + " >>> " + i.getP1() + "@" + point + " ---- CLOSEST" );
				}
				else
				{
					//System.out.println( d + ": " + i.getP0() + " >>> " + i.getP1() + "@" + point );
				}

				sumDtmp += ltmp.length();
			}

			if ( closest == null )
			{
				System.out.println( "Could not assign: " + Util.printCoordinates( p.getB() ) );
				continue;
			}
			//System.exit( 0 );

			// find the point on the line that is closest
			final double x = sumD + Algebra.pointOfShortestDistance( closest.getP0(), closest.getP1(), q ) * l;

			// now rotate the segment vector so it is along the x-axis
			final Point3f pX = new Point3f(
					closest.getP0().x + (float)l,
					closest.getP0().y,
					closest.getP0().z );

			Transform3D t = Algebra.getTransformation( closest.getP0(), closest.getP1(), closest.getP0(), pX, false );
			final Point3f qx = new Point3f( p.getB() );//TODO:here?
			t.transform( qx );

			// now we can simply measure the y
			final double y = qx.y - closest.getP0().y;
			final double z = qx.z - closest.getP0().z;

			straight.add( new ValuePair< Integer, float[] >( p.getA(), new float[]{ (float)x, (float)y, (float)z } ) );
		}

		return straight;
	}

}
