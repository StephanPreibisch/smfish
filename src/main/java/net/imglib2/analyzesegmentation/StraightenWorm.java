package net.imglib2.analyzesegmentation;

import java.util.ArrayList;
import java.util.List;

import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

import mpicbg.imglib.util.Util;
import net.imglib2.RealPoint;
import net.imglib2.analyzesegmentation.wormfit.InlierCells;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import spim.fiji.spimdata.interestpoints.InterestPoint;

public class StraightenWorm
{
	public static Cells stretchWormCells( final FindWormOutline fwo, final Cells cells, final float amount )
	{
		final List< Pair< Integer, float[] > > curved = new ArrayList< Pair<Integer,float[]> >();
		final float[] loc = new float[ 3 ];

		for ( final int id : cells.getCells().keySet() )
		{
			final Cell cell = cells.getCells().get( id );
			cell.getPosition().localize( loc );
			curved.add( new ValuePair< Integer, float[] >( cell.getId(), loc.clone() ) );
		}

		final List< Pair< Integer, float[] > > straight = stretchWorm( fwo, curved, amount );

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

	public static List< InterestPoint > stretchWormInterestPoints( final FindWormOutline fwo, final List< InterestPoint > list, final float amount )
	{
		final List< Pair< Integer, float[] > > curved = new ArrayList< Pair<Integer,float[]> >();

		for ( final InterestPoint p : list )
			curved.add( new ValuePair< Integer, float[] >( p.getId(), p.getL() ) );

		final List< Pair< Integer, float[] > > straight = stretchWorm( fwo, curved, amount );

		final ArrayList< InterestPoint > straightInterestPoints = new ArrayList< InterestPoint >();

		for ( final Pair< Integer, float[] > p : straight )
			straightInterestPoints.add(  new InterestPoint( p.getA(), p.getB() ) );

		return straightInterestPoints;
	}

	public static List< Pair< Integer, float[] > > stretchWorm( final FindWormOutline fwo, final List< Pair< Integer, float[] > > list, final float amount )
	{
		final List< Pair< Integer, float[] > > straight = new ArrayList< Pair< Integer, float[] > >();

		for ( final Pair< Integer, float[] > p : list )
		{
			//System.out.println( Util.printCoordinates( p.getL() ) );

			// find closest line segment and cumulative distance from tail of the worm
			InlierCells closest = null;
			double dist = -1;
			double l = -1;
			Point3f sumVec = new Point3f( 0, 0, 0 );
			Point3f sumDVec = new Point3f( 0, 0, 0 );
			Point3f currentVec = null;
			Vector3f lastVec = null;

			final Point3f q = new Point3f( p.getB() );

			for ( final InlierCells i : fwo.getSegments() )
			{
				final double d = Algebra.shortestDistance( i.getP0(), i.getP1(), q );
				final double point = Algebra.pointOfShortestDistance( i.getP0(), i.getP1(), q );

				final Vector3f ltmp = new Vector3f(
						i.getP1().x - i.getP0().x,
						i.getP1().y - i.getP0().y,
						i.getP1().z - i.getP0().z );

				final Vector3f thisVec = new Vector3f( ltmp );
				thisVec.normalize();
				thisVec.x = 1 * ( 1.0f - amount ) + thisVec.x * amount;
				thisVec.y = 0 * ( 1.0f - amount ) + thisVec.y * amount;
				thisVec.z = 0 * ( 1.0f - amount ) + thisVec.z * amount;
				thisVec.normalize();
				thisVec.x *= ltmp.length();
				thisVec.y *= ltmp.length();
				thisVec.z *= ltmp.length();

				// sum of vectors until the beginning of this segment
				if ( lastVec != null )
				{
					sumVec.x += lastVec.x;
					sumVec.y += lastVec.y;
					sumVec.z += lastVec.z;
				}

				if ( point >= 0 && point <= 1.2 && ( closest == null || d < dist ) )
				{
					sumDVec = new Point3f( sumVec );
					currentVec = new Point3f( thisVec ); // the new orientation of this segment
					closest = i;
					dist = d;
					l = ltmp.length(); // the length of this vector
					//System.out.println( d + ": " + i.getP0() + " >>> " + i.getP1() + "@" + point + " ---- CLOSEST" );
				}
				else
				{
					//System.out.println( d + ": " + i.getP0() + " >>> " + i.getP1() + "@" + point );
				}

				lastVec = new Vector3f( thisVec );
			}

			if ( closest == null )
			{
				System.out.println( "Could not assign: " + Util.printCoordinates( p.getB() ) );
				continue;
			}

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

			// find the point on the line that is closest
			// final double x = sumD + Algebra.pointOfShortestDistance( closest.getP0(), closest.getP1(), q ) * l;

			// compute new location on the vector
			final double relVecLength = Algebra.pointOfShortestDistance( closest.getP0(), closest.getP1(), q );
			final double xn = sumDVec.x + currentVec.x * relVecLength;
			final double yn = sumDVec.y + currentVec.y * relVecLength;
			final double zn = sumDVec.z + currentVec.z * relVecLength;

			// transform y, z into the orientation of this segment
			Point3f tmp = new Point3f(
					currentVec.x,
					currentVec.y,
					currentVec.z );

			t = Algebra.getTransformation( new Point3f( 0, 0, 0 ), new Point3f( 1, 0, 0 ), new Point3f( 0, 0, 0 ), tmp, false );
			tmp.x = 0;
			tmp.y = (float)y;
			tmp.z = (float)z;
			t.transform( tmp );

			straight.add( new ValuePair< Integer, float[] >( p.getA(), new float[]{ (float)xn + tmp.x, (float)yn + tmp.y, (float)zn + tmp.z } ) );
		}

		return straight;
	}

}
