package net.imglib2.postprocessing;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.omg.PortableServer.POA;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import ij.IJ;
import ij.ImageJ;
import ij3d.Content;
import ij3d.Image3DUniverse;
import mpicbg.models.AffineModel3D;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import net.imglib2.RealPoint;
import net.imglib2.analyzesegmentation.Cell;
import net.imglib2.analyzesegmentation.Cells;
import net.imglib2.analyzesegmentation.Java3DHelpers;
import net.imglib2.analyzesegmentation.load.LoadStraightWorm;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import spim.fiji.spimdata.interestpoints.InterestPoint;
import spim.process.interestpointregistration.Detection;
import spim.process.interestpointregistration.MatchPointList;
import spim.process.interestpointregistration.PairwiseMatch;
import spim.process.interestpointregistration.RANSACParameters;
import spim.process.interestpointregistration.TransformationModel;
import spim.process.interestpointregistration.geometricdescriptor.RGLDMPairwise;
import spim.process.interestpointregistration.geometricdescriptor.RGLDMParameters;
import spim.process.interestpointregistration.icp.IterativeClosestPointPairwise;
import spim.process.interestpointregistration.icp.IterativeClosestPointParameters;

public class AlignWorms
{
	public static void align( final Cells cellsA, final Cells cellsB, final String name, final ArrayList< Pair< Point3f, Point3f > > connections, final ArrayList< Pair< Cell, Cell > > corresponding )
	{
		final List< InterestPoint > interestpointListA = new ArrayList<>();
		final List< InterestPoint > interestpointListB = new ArrayList<>();

		for ( final Cell c : cellsA.getCells().values() )
			interestpointListA.add( new InterestPoint( c.getId(), new float[]{ c.getFloatPosition( 0 ), c.getFloatPosition( 1 ), c.getFloatPosition( 2 ) } ) );

		for ( final Cell c : cellsB.getCells().values() )
			interestpointListB.add( new InterestPoint( c.getId(), new float[]{ c.getFloatPosition( 0 ), c.getFloatPosition( 1 ), c.getFloatPosition( 2 ) } ) );

		final MatchPointList listA = new MatchPointList( interestpointListA, null );
		final MatchPointList listB = new MatchPointList( interestpointListB, null );

		final PairwiseMatch pairs = new PairwiseMatch( new ViewId( 0, 0 ), new ViewId( 0, 1 ), listA, listB );
		final TransformationModel t = new TransformationModel( 2, 1, 0.5, true );

		final RANSACParameters rp = new RANSACParameters( 10, 0.01f, 2, 10000 );
		final RGLDMParameters dp = new RGLDMParameters( 5000, 2, 2, 2 );
		//final RGLDMPairwise match = new RGLDMPairwise( pairs, t, name, rp, dp );

		final IterativeClosestPointParameters ipp = new IterativeClosestPointParameters( 5, 1 );
		final IterativeClosestPointPairwise match = new IterativeClosestPointPairwise( pairs, t, name, ipp );

		connections.clear();
		corresponding.clear();

		try
		{
			match.call();

			for ( final PointMatchGeneric< Detection > pair : pairs.getInliers() )
			{
				final int idA = pair.getPoint1().getId();
				final int idB = pair.getPoint2().getId();

				final Cell cellA = cellsA.getCells().get( idA );
				final Cell cellB = cellsB.getCells().get( idB );

				corresponding.add( new ValuePair< Cell, Cell >( cellA, cellB ) );
				connections.add(  new ValuePair< Point3f, Point3f >(
						new Point3f( cellA.getFloatPosition( 0 ), cellA.getFloatPosition( 1 ), cellA.getFloatPosition( 2 ) ),
						new Point3f( cellB.getFloatPosition( 0 ), cellB.getFloatPosition( 1 ), cellB.getFloatPosition( 2 ) ) ) );
			}
		}
		catch ( Exception e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void rotateX( final Cells cells, final double degrees )
	{
		final Transform3D t = new Transform3D();
		t.rotX( Math.toRadians( degrees ) );

		final double[] tmp = new double[ 16 ];
		t.get( tmp );

		final AffineTransform3D model = new AffineTransform3D();

		model.set(
			tmp[ 0 ], tmp[ 1 ], tmp[ 2 ], tmp[ 3 ],
			tmp[ 4 ], tmp[ 5 ], tmp[ 6 ], tmp[ 7 ],
			tmp[ 8 ], tmp[ 9 ], tmp[ 10 ], tmp[ 11 ] );

		for ( final Cell cell : cells.getCells().values() )
			model.apply( cell.getPosition(), cell.getPosition() );

		cells.buildKDTree();
	}

	static HashMap< Integer, Color3f > oldColorsA;
	static HashMap< Integer, Color3f > oldColorsB;
	
	public static void reColor( final Image3DUniverse univ, final ArrayList< Pair< Cell, Cell > > corresponding, final Cells cellsA, final Cells cellsB )
	{
		oldColorsA = new HashMap<>();
		oldColorsB = new HashMap<>();

		final Color3f in = new Color3f( 0.3f, 0.8f, 0.3f );
		
		for ( int i = 0; i < corresponding.size(); ++i )
		{
			final Pair< Cell, Cell > pair = corresponding.get( i );

			final int idA = pair.getA().getId();
			final Sphere sA = cellsA.getSpheres().get( idA );

			final int idB = pair.getB().getId();
			final Sphere sB = cellsB.getSpheres().get( idB );

			final Color3f cA = new Color3f();
			final Color3f cB = new Color3f();
			sA.getAppearance().getColoringAttributes().getColor( cA );
			sB.getAppearance().getColoringAttributes().getColor( cB );
			oldColorsA.put( idA, cA );
			oldColorsB.put( idB, cB );

			sA.getAppearance().getColoringAttributes().setColor( in );
			sB.getAppearance().getColoringAttributes().setColor( in );
			
		}
	}

	public static void unColor( final Image3DUniverse univ, final ArrayList< Pair< Cell, Cell > > corresponding, final Cells cellsA, final Cells cellsB )
	{
		for ( int i = 0; i < corresponding.size(); ++i )
		{
			final Pair< Cell, Cell > pair = corresponding.get( i );

			final int idA = pair.getA().getId();
			final Sphere sA = cellsA.getSpheres().get( idA );

			final int idB = pair.getB().getId();
			final Sphere sB = cellsB.getSpheres().get( idB );

			sA.getAppearance().getColoringAttributes().setColor( oldColorsA.get( idA ) );
			sB.getAppearance().getColoringAttributes().setColor( oldColorsB.get( idB ) );
		}
	}

	public static void drawLines( final Image3DUniverse univ, final ArrayList< Pair< Point3f, Point3f > > connections )
	{
		final List< Point3f > lineMesh = new ArrayList< Point3f >();

		for ( int i = 0; i < connections.size(); ++i )
		{
			final Pair< Point3f, Point3f > pair = connections.get( i );
			lineMesh.add( pair.getA() );
			lineMesh.add( pair.getB() );
			//Java3DHelpers.drawLine( univ, pair.getA(), pair.getB(), "Line " + i );
		}

		final Content content = univ.addLineMesh( lineMesh, new Color3f(), "Lines", false );
		content.showCoordinateSystem( false );

	}

	public static void main( String[] args )
	{
		final LoadStraightWorm loader = new LoadStraightWorm( 1000.0, true );

		//loader.setScaling( new double[]{ 1.0, 1.114999693006692, 1.0 } );
		final Cells em = loader.load( new File( "/Users/spreibi/workspace/smfish/cells.em.straight.txt" ) );
		System.out.println( loader );

		// wrong annotation
		em.getCells().remove( 83 );

		loader.resetScaling();
		final Cells confocal = loader.load( new File( "/Users/spreibi/workspace/smfish/cells.confocal.straight.txt" ) );
		System.out.println( loader );

		final Image3DUniverse univ = Java3DHelpers.initUniverse();

		Java3DHelpers.drawInvisibleBoundingBox( univ, confocal.getCells() );

		final float transparency = 0.3f;
		final Color3f colConfocal = new Color3f( 0.5f, 0, 0 );
		final Color3f colEM = new Color3f( 0, 0, 0.5f );

		BranchGroup groupConfocal = Java3DHelpers.drawCells( univ, confocal, new Transform3D(), transparency );
		BranchGroup groupEM = Java3DHelpers.drawCells( univ, em, new Transform3D(), transparency );

		int degrees = 0;
		int inc = 5;

		do
		{
			degrees += inc;
			rotateX( confocal, inc );

			groupConfocal.detach();
			groupConfocal = Java3DHelpers.drawCells( univ, confocal, new Transform3D(), transparency );

			final ArrayList< Pair< Point3f, Point3f > > connections = new ArrayList<>();
			final ArrayList< Pair< Cell, Cell > > corresponding = new ArrayList<>();

			align( em, confocal, "Angle " + (degrees % 360), connections, corresponding );
			reColor( univ, corresponding, em, confocal );

			SimpleMultiThreading.threadWait( 250 );
			unColor( univ, corresponding, em, confocal );

			//drawLines( univ, connections );
		}
		while ( univ != null );
	}
}
