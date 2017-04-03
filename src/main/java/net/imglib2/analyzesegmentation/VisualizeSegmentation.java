package net.imglib2.analyzesegmentation;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

import customnode.CustomPointMesh;
import customnode.Mesh_Maker;
import ij3d.Content;
import ij3d.Image3DUniverse;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.spim.io.TextFileAccess;
import net.imglib2.analyzesegmentation.load.Load;
import net.imglib2.analyzesegmentation.load.LoadBDV;
import spim.fiji.spimdata.interestpoints.InterestPoint;
import spim.fiji.spimdata.interestpoints.InterestPointList;

public class VisualizeSegmentation
{
	final String dir = "/Users/spreibi/Documents/Transcription Meeting 2014/";
	final double scaleZ = 1.6;

	final Cells cells;
	int nextCellId = 0;

	public VisualizeSegmentation()
	{
		final boolean visualizeStretching = false;

		// TODO: remove correction for wrong calibration
		final Load loader = new LoadBDV( scaleZ );

		this.cells = loader.load();

		if ( this.cells != null )
		{
			final Image3DUniverse univ = Java3DHelpers.initUniverse();
			final List< InterestPoint > guide = loadInterestPoints( dir, "interestpoints/tpId_0_viewSetupId_1.mRNA", scaleZ );
			final List< InterestPoint > alt = loadInterestPoints( dir, "interestpoints/tpId_0_viewSetupId_3.altExon.fused", scaleZ );

			drawInvisibleBoundingBox( univ, cells.getCells() );
			BranchGroup c = drawCells( univ, cells, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f );
			BranchGroup i1 = drawInterestPoints( univ, alt, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f, true );
			BranchGroup i2 = drawInterestPoints( univ, guide, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f, false );
			
			final RecolorCell rcc = new RecolorCell( univ, new Color3f( 1, 0, 0 ) );
			univ.getCanvas().addMouseMotionListener( rcc );

			final DefineInitialCellVector dicv = new DefineInitialCellVector( rcc, cells );
			univ.getCanvas().addMouseListener( dicv );

			do
			{
				SimpleMultiThreading.threadWait( 100 );
			}
			while ( dicv.getSphere2() == null );

			rcc.setActive( false );
			FindWormOutline.makeScreenshot( 0 );

			if ( visualizeStretching )
			{
				c.detach();
				i1.detach();
				i2.detach();
			}

			final FindWormOutline fwo = new FindWormOutline( visualizeStretching? null : univ, cells, ((Cell)dicv.getSphere1().getUserData()), ((Cell)dicv.getSphere2().getUserData()), 25 );
			fwo.findOutline();

			System.out.println( "done" );

			if ( !visualizeStretching )
				SimpleMultiThreading.threadHaltUnClean();

			int i = 0;

			for ( float amount = 1f; amount >= 0; amount -= 0.01f )
			{
				System.out.println( amount );

				PrintWriter out = TextFileAccess.openFileWrite( new File( dir, "guideRNA.straight.txt" ) );
				final List< InterestPoint > guideStretch = StraightenWorm.stretchWormInterestPoints( fwo, guide, amount );
				for ( final InterestPoint ip : guideStretch )
					out.println( ip.getId() + "\t" + ip.getL()[ 0 ] + "\t" + ip.getL()[ 1 ]  + "\t" + ip.getL()[ 2 ] );
				out.close();
	
				out = TextFileAccess.openFileWrite( new File( dir, "altexonRNA.straight.txt" ) );
				final List< InterestPoint > altStretch = StraightenWorm.stretchWormInterestPoints( fwo, alt, amount );
				for ( final InterestPoint ip : altStretch )
					out.println( ip.getId() + "\t" + ip.getL()[ 0 ] + "\t" + ip.getL()[ 1 ]  + "\t" + ip.getL()[ 2 ] );
				out.close();
	
				out = TextFileAccess.openFileWrite( new File( dir, "cells.straight.txt" ) );
				final Cells cellsNew = StraightenWorm.stretchWormCells( fwo, cells, amount );
				for ( final Cell cell : cellsNew.getCells().values() )
					out.println( cell.getId() + "\t" + cell.getFloatPosition( 0 ) + "\t" + cell.getFloatPosition( 1 )  + "\t" + cell.getFloatPosition( 2 ) );
				out.close();
	
				c = drawInterestPoints( univ, altStretch, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f, true );
				i1 = drawInterestPoints( univ, guideStretch, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f, false );
				i2 = drawCells( univ, cellsNew, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f );
				
				SimpleMultiThreading.threadWait( 250 );
				//FindWormOutline.makeScreenshot( i++ );
				//SimpleMultiThreading.threadWait( 100 );

				if ( amount < 0.02 )
					SimpleMultiThreading.threadHaltUnClean();
				c.detach();
				i1.detach();
				i2.detach();
			}
			//System.exit( 0 );
			//VisualizationFunctions.drawArrow( univ, new Vector3f( new float[]{ 100, 100, 100 } ), 45, 10 );
			//drawNuclei( univ, cells, new Transform3D(), 0.95f );
		}
	}

	public static List< InterestPoint > loadInterestPoints( final String dir, final String interestPoints, final double scaleZ )
	{
		return loadInterestPoints( dir, new File( interestPoints ), scaleZ );
	}

	public static List< InterestPoint > loadInterestPoints( final String dir, final File interestPoints, final double scaleZ )
	{
		final InterestPointList list = new InterestPointList( new File( dir ), interestPoints );
		list.loadInterestPoints();

		for ( final InterestPoint p : list.getInterestPoints() )
			p.getL()[ 2 ] *= scaleZ;

		return list.getInterestPoints();
	}

	public void discoWorm()
	{
		Random random = new Random();
		
		while ( System.currentTimeMillis() > 0 )
		{
			for ( final Sphere s : cells.getSpheres().values() )
			{
				final int r = random.nextInt( 256 );
				final int g = random.nextInt( 256 );
				final int b = random.nextInt( 256 );
				s.getAppearance().getColoringAttributes().setColor( new Color3f( r/255f, g/255f, b/255f ) );
			}
			
			SimpleMultiThreading.threadWait( 100 );
		}
	}

	public static BranchGroup drawCells( final Image3DUniverse univ, final Cells cells, final Transform3D globalTransform, 
			final Color3f color, final float transparency )
	{		
		// get the scene
		final BranchGroup parent = univ.getScene();

		// create a new branch group that contains all transform groups which contain one sphere each
		final BranchGroup viewBranch = new BranchGroup();
		viewBranch.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
		
		// init the structures needed to code the position of the beads
		final Transform3D transform = new Transform3D();
		final Point3f translation = new Point3f();
		final float[] loc = new float[ 3 ];

		final Random random = new Random( 1 );

		// add all beads
		for ( final int id : cells.getCells().keySet() )
		{
			final Cell cell = cells.getCells().get( id );
			
			// set the bead coordinates
			cell.getPosition().localize( loc );
			translation.set( loc );

			// transform the bead coordinates into the position of the view
			globalTransform.transform( translation );

			// create new TransformGroup with the altered coordinates 
			transform.setTranslation( new Vector3f( translation ) );
			final TransformGroup transformGroup = new TransformGroup( transform );

			transformGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transformGroup.setCapability( TransformGroup.ALLOW_CHILDREN_WRITE );

			// add the sphere
			final int r = random.nextInt( 192 );
			final Appearance appearance = new Appearance();
			appearance.setColoringAttributes( new ColoringAttributes( new Color3f( r/255f, r/255f, r/255f ), ColoringAttributes.SHADE_GOURAUD ) );
			appearance.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.NICEST, transparency ) );

			appearance.getColoringAttributes().setCapability( ColoringAttributes.ALLOW_COLOR_READ );
			appearance.getColoringAttributes().setCapability( ColoringAttributes.ALLOW_COLOR_WRITE );

			final Sphere s = new Sphere( cell.getRadius(), Sphere.BODY, 10, appearance );
			s.setCapability( Sphere.ENABLE_APPEARANCE_MODIFY );
			s.getShape().setCapability( Shape3D.ALLOW_APPEARANCE_WRITE );
			transformGroup.addChild( s );
			//s.setName( "nucleus " + cell.getId() );
			s.setUserData( cell );

			// store the link between cell and sphere
			cells.getSpheres().put( id, s );

			// add the group to the view branch
			viewBranch.addChild( transformGroup );
		}

		// TO MAKE INVISIBLE
		viewBranch.setCapability( BranchGroup.ALLOW_DETACH );

		// ????
		viewBranch.compile();

		// add the view branch to the scene
		parent.addChild( viewBranch );
		
		return viewBranch;
	}

	public static BranchGroup drawInterestPoints(
			final Image3DUniverse univ,
			final List< InterestPoint > points,
			final Transform3D globalTransform,
			final Color3f color,
			final float transparency,
			final boolean red )
	{		
		// get the scene
		final BranchGroup parent = univ.getScene();

		// create a new branch group that contains all transform groups which contain one sphere each
		final BranchGroup viewBranch = new BranchGroup();
		viewBranch.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
		
		// init the structures needed to code the position of the beads
		final Transform3D transform = new Transform3D();
		final Point3f translation = new Point3f();

		final Random random = new Random( 1 );

		// add all interestpoints
		for ( final InterestPoint p : points )
		{
			// set the coordinates
			translation.set( p.getL() );

			// transform the bead coordinates into the position of the view
			globalTransform.transform( translation );

			// create new TransformGroup with the altered coordinates 
			transform.setTranslation( new Vector3f( translation ) );

			final TransformGroup transformGroup = new TransformGroup( transform );

			transformGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transformGroup.setCapability( TransformGroup.ALLOW_CHILDREN_WRITE );

			// add the sphere
			final int r = 255;//random.nextInt( 192 );
			final Appearance appearance = new Appearance();
			if ( red )
				appearance.setColoringAttributes( new ColoringAttributes( new Color3f( r/255f, 0, 0 ), ColoringAttributes.SHADE_GOURAUD ) );
			else
				appearance.setColoringAttributes( new ColoringAttributes( new Color3f( 0, r/255f, 0 ), ColoringAttributes.SHADE_GOURAUD ) );
			appearance.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.NICEST, transparency ) );

			appearance.getColoringAttributes().setCapability( ColoringAttributes.ALLOW_COLOR_READ );
			appearance.getColoringAttributes().setCapability( ColoringAttributes.ALLOW_COLOR_WRITE );

			final Sphere s = new Sphere( 3, Sphere.BODY, 10, appearance );
			s.setCapability( Sphere.ENABLE_APPEARANCE_MODIFY );
			s.getShape().setCapability( Shape3D.ALLOW_APPEARANCE_WRITE );
			transformGroup.addChild( s );
			//s.setName( "nucleus " + cell.getId() );
			//s.setUserData( p );

			// add the group to the view branch
			viewBranch.addChild( transformGroup );
		}

		// TO MAKE INVISIBLE
		viewBranch.setCapability( BranchGroup.ALLOW_DETACH );

		// ????
		viewBranch.compile();

		// add the view branch to the scene
		parent.addChild( viewBranch );
		
		return viewBranch;
	}

	public static Content drawLine( final Image3DUniverse univ, final Point3f p0, final Point3f p1, final String name )
	{
		final List< Point3f > lineMesh = new ArrayList< Point3f >();

		lineMesh.add( p0 );
		lineMesh.add( p1 );
		
		final Content content = univ.addLineMesh( lineMesh, new Color3f(), name, false );
		content.showCoordinateSystem( false );

		return content;
	}

	public static BranchGroup drawTruncatedCone( final float r1, final float r2, final float l, final Image3DUniverse univ, final Transform3D globalTransform, final Color3f color, final float transparency )
	{
		// get the scene
		final BranchGroup parent = univ.getScene();

		// create a new branch group that contains all transform groups which contain one sphere each
		final BranchGroup viewBranch = new BranchGroup();
		viewBranch.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );

		final TransformGroup transformGroup = new TransformGroup( globalTransform );

		transformGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
		transformGroup.setCapability( TransformGroup.ALLOW_CHILDREN_WRITE );

		// add the truncated cone
		final Appearance appearance = new Appearance();
		appearance.setColoringAttributes( new ColoringAttributes( color, ColoringAttributes.SHADE_GOURAUD ) );
		appearance.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.NICEST, transparency ) );

		appearance.getColoringAttributes().setCapability( ColoringAttributes.ALLOW_COLOR_READ );
		appearance.getColoringAttributes().setCapability( ColoringAttributes.ALLOW_COLOR_WRITE );

		final TruncatedCone cone = new TruncatedCone( r1, r2, l, appearance );
		cone.setCapability( Sphere.ENABLE_APPEARANCE_MODIFY );
		//cone.getShape().setCapability( Shape3D.ALLOW_APPEARANCE_WRITE );
		transformGroup.addChild( cone );

		// add the group to the view branch
		viewBranch.addChild( transformGroup );
		viewBranch.setCapability( BranchGroup.ALLOW_DETACH );

		// ????
		viewBranch.compile();

		// add the view branch to the scene
		parent.addChild( viewBranch );
		
		return viewBranch;
	}

	public static Content drawInvisibleBoundingBox( final Image3DUniverse univ, final Map< Integer, Cell > cells )
	{
		final float[][] positions = new float[ cells.values().size() ][ 3 ];

		int i = 0;

		for ( final Cell cell : cells.values() )
		{
			for ( int d = 0; d < 3; ++d )
				positions[ i ][ d ] = cell.getPosition().getFloatPosition( d );

			++i;
		}

		return drawInvisibleBoundingBox( univ, positions );
	}

	public static Content drawInvisibleBoundingBox( final Image3DUniverse univ, final List< InterestPoint > list )
	{
		final float[][] positions = new float[ list.size() ][ 3 ];

		int i = 0;

		for ( final InterestPoint p : list )
		{
			for ( int d = 0; d < 3; ++d )
				positions[ i ][ d ] = p.getL()[ d ];

			++i;
		}

		return drawInvisibleBoundingBox( univ, positions );
	}

	public static Content drawInvisibleBoundingBox( final Image3DUniverse univ, float[][] p )
	{
		float[] min = null;
		float[] max = null;

		for ( int i = 0; i < p.length; ++i )
		{
			if ( min == null || max == null )
			{
				min = new float[ 3 ];
				max = new float[ 3 ];

				for ( int d = 0; d < min.length; ++d )
				{
					min[ d ] = p[ i ][ d ] - 50;
					max[ d ] = p[ i ][ d ] + 50;
				}
			}

			for ( int d = 0; d < min.length; ++d )
			{
				min[ d ] = Math.min( min[ d ], p[ i ][ d ] - 50 );
				max[ d ] = Math.max( max[ d ], p[ i ][ d ] + 50 );
			}
		}

		final List< Point3f > mesh = new ArrayList< Point3f >();

		mesh.add( new Point3f( min ) );
		mesh.add( new Point3f( max ) );

		final CustomPointMesh cm = new CustomPointMesh( mesh );
		cm.setColor( new Color3f( 0, 1, 0 ) );
		cm.setPointSize( 0 );

		final Content content = univ.addCustomMesh( cm, "invisible bounding box" );
		content.showCoordinateSystem( false );

		return content;
	}

	public static BranchGroup drawNucleiSlow( final Image3DUniverse univ, final Map< Integer, Cell > cells, final Transform3D globalTransform, final float transparency )
	{
		final int meridians = 6;
		final int parallels = 6;

		final List< Point3f > meshes = new ArrayList<Point3f>();

		// add all beads
		for ( final Cell cell : cells.values() )
		{
			final List< Point3f > mesh = Mesh_Maker.createSphere(
					cell.getPosition().getDoublePosition( 0 ),
					cell.getPosition().getDoublePosition( 1 ),
					cell.getPosition().getDoublePosition( 2 ),
					cell.getRadius(),
					meridians,
					parallels );

			meshes.addAll( mesh );
		}

		univ.addTriangleMesh( meshes, new Color3f( 1, 1, 1 ), "nuclei" );

		return null;
	}

	public static void main( String[] args )
	{
		System.out.println( "Java: " + System.getProperty( "java.version" ) );
		final Point3f p0 = new Point3f( 0, 0, 0 );
		final Point3f p1 = new Point3f( 2, 0, 0 );
		final Point3f q = new Point3f( 0.9f, 1, 0 );
		
		System.out.println( Algebra.pointOfShortestDistance( p0, p1, q ) );
		System.out.println( Algebra.shortestDistance( p0, p1, q ) );

		final Vector3f v1 = new Vector3f( 1.5f, 0, 0 );
		final Vector3f v2 = new Vector3f( 324, 123.0f, -1323 );

		Algebra.normalizeLength( v2, v1 );

		System.out.println( v1 + " " + v1.length() );
		System.out.println( v2 + " " + v2.length() );

		/*
		final Point3f p00 = new Point3f( 0, 0, 0 );
		final Point3f p01 = new Point3f( 0, 0, 0 );

		final Point3f p10 = new Point3f( 0, 0, 0 );
		final Point3f p11 = new Point3f( 0, 0, 0 );

		final Transform3D t = Algebra.getTransformation( p00, p01, p10, p11, true );

		t.transform( p00 );
		t.transform( p01 );
		System.out.println( p00 + " == " + p10 );
		System.out.println( p01 + " == " + p11 );
		*/
		
		new VisualizeSegmentation();
	}
}
