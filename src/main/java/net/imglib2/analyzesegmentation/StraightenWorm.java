package net.imglib2.analyzesegmentation;

import java.util.ArrayList;
import java.util.List;

import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import net.imglib2.RealPoint;
import net.imglib2.analyzesegmentation.wormfit.InlierCells;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import spim.fiji.spimdata.interestpoints.InterestPoint;

public class StraightenWorm
{
	public static Cells stretchWormCells( final FindWormOutline fwo, final Cells cells, final float amount )
	{
		final List< Pair< Integer, double[] > > curved = new ArrayList< Pair<Integer,double[]> >();
		final double[] loc = new double[ 3 ];

		for ( final int id : cells.getCells().keySet() )
		{
			final Cell cell = cells.getCells().get( id );
			cell.getPosition().localize( loc );
			curved.add( new ValuePair< Integer, double[] >( cell.getId(), loc.clone() ) );
		}

		final List< Pair< Integer, double[] > > straight = stretchWorm( fwo, curved, amount );

		final Cells cellsNew = new Cells();

		for ( final Pair< Integer, double[] > p : straight )
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
		final List< Pair< Integer, double[] > > curved = new ArrayList< Pair<Integer,double[]> >();

		for ( final InterestPoint p : list )
			curved.add( new ValuePair< Integer, double[] >( p.getId(), p.getL() ) );

		final List< Pair< Integer, double[] > > straight = stretchWorm( fwo, curved, amount );

		final ArrayList< InterestPoint > straightInterestPoints = new ArrayList< InterestPoint >();

		for ( final Pair< Integer, double[] > p : straight )
			straightInterestPoints.add(  new InterestPoint( p.getA(), p.getB() ) );

		return straightInterestPoints;
	}

	public static float[] double2float( final double[] d )
	{
		final float[] f = new float[ d.length ];
		for ( int i = 0; i < d.length; ++i )
			f[ i ] = (float)d[ i ];
		return f;
	}

	public static List< Pair< Integer, double[] > > stretchWorm( final FindWormOutline fwo, final List< Pair< Integer, double[] > > list, final float amount )
	{
		final List< Pair< Integer, double[] > > straight = new ArrayList<>();

		for ( final Pair< Integer, double[] > p : list )
		{
			//System.out.println( Util.printCoordinates( p.getL() ) );

			// find closest line segment and cumulative distance from tail of the worm
			InlierCells closest = null;
			double dist = -1;
			double l = -1;
			Point3d sumVec = new Point3d( 0, 0, 0 );
			Point3d sumDVec = new Point3d( 0, 0, 0 );
			Point3d currentVec = null;
			Vector3d lastVec = null;

			final Point3d q = new Point3d( p.getB() );

			for ( final InlierCells i : fwo.getSegments() )
			{
				final double d = Algebra.shortestDistance( i.getP0(), i.getP1(), q );
				final double point = Algebra.pointOfShortestDistance( i.getP0(), i.getP1(), q );

				final Vector3d ltmp = new Vector3d(
						i.getP1().x - i.getP0().x,
						i.getP1().y - i.getP0().y,
						i.getP1().z - i.getP0().z );

				final Vector3d thisVec = new Vector3d( ltmp );
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
					sumDVec = new Point3d( sumVec );
					currentVec = new Point3d( thisVec ); // the new orientation of this segment
					closest = i;
					dist = d;
					l = ltmp.length(); // the length of this vector
					//System.out.println( d + ": " + i.getP0() + " >>> " + i.getP1() + "@" + point + " ---- CLOSEST" );
				}
				else
				{
					//System.out.println( d + ": " + i.getP0() + " >>> " + i.getP1() + "@" + point );
				}

				lastVec = new Vector3d( thisVec );
			}

			if ( closest == null )
			{
				System.out.println( "Could not assign: " + Util.printCoordinates( p.getB() ) );
				continue;
			}

			// now rotate the segment vector so it is along the x-axis
			final Point3d pX = new Point3d(
					closest.getP0().x + (float)l,
					closest.getP0().y,
					closest.getP0().z );

			Transform3D t = Algebra.getTransformation( closest.getP0(), closest.getP1(), closest.getP0(), pX, false );
			final Point3d qx = new Point3d( p.getB() );//TODO:here?
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
			Point3d tmp = new Point3d(
					currentVec.x,
					currentVec.y,
					currentVec.z );

			t = Algebra.getTransformation( new Point3d( 0, 0, 0 ), new Point3d( 1, 0, 0 ), new Point3d( 0, 0, 0 ), tmp, false );
			tmp.x = 0;
			tmp.y = (float)y;
			tmp.z = (float)z;
			t.transform( tmp );

			straight.add( new ValuePair< Integer, double[] >( p.getA(), new double[]{ xn + tmp.x, yn + tmp.y, zn + tmp.z } ) );
		}

		return straight;
	}

}
