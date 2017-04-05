package net.imglib2.analyzesegmentation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Label;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.Background;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.LineAttributes;
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
import ij3d.ImageCanvas3D;
import spim.fiji.spimdata.interestpoints.InterestPoint;

public class Java3DHelpers
{
	final static public Color3f backgroundColor = new Color3f( 1f, 1f, 1f );
	final static public Color3f foregroundColor = new Color3f( 0.5f, 0.5f, 0.59f );

	final static public Color3f beadColor = new Color3f( 0f, 0f, 0f );
	final static public float beadSize = 3f;

	final static public Color3f boundingBoxColor = new Color3f( 0.7f, 0.7f, 0.85f );
	final static public Color3f imagingBoxColor = new Color3f( 1f, 0.0f, 0.25f );
	final static public LineAttributes boundingBoxLineAttributes = new LineAttributes();
	
	final static public Font statusbarFont = new Font("Cambria", Font.PLAIN, 12);

	public static BranchGroup drawCells(
			final Image3DUniverse univ,
			final Cells cells,
			final Transform3D globalTransform,
			final float transparency )
	{
		return drawCells( univ, cells, globalTransform, null, transparency );
	}

	public static BranchGroup drawCells(
			final Image3DUniverse univ,
			final Cells cells,
			final Transform3D globalTransform,
			final Color3f c,
			final float transparency )
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
			int r = random.nextInt( 192 );
			final Appearance appearance = new Appearance();
			if ( c == null )
			{
				appearance.setColoringAttributes( new ColoringAttributes( new Color3f( r/255f, r/255f, r/255f ), ColoringAttributes.SHADE_GOURAUD ) );
			}
			else
			{
				r += 64;
				appearance.setColoringAttributes( new ColoringAttributes( new Color3f( c.x * r/255f, c.y * r/255f, c.z * r/255f ), ColoringAttributes.SHADE_GOURAUD ) );
			}
			appearance.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.NICEST, transparency ) );

			appearance.getColoringAttributes().setCapability( ColoringAttributes.ALLOW_COLOR_READ );
			appearance.getColoringAttributes().setCapability( ColoringAttributes.ALLOW_COLOR_WRITE );

			final Sphere s = new Sphere( cell.getRadius(), Sphere.BODY, 20, appearance );
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

	public static Image3DUniverse initUniverse()
	{
		final Image3DUniverse uni = new Image3DUniverse( 1800, 1000 );
		uni.show();

		setBackgroundColor( uni, backgroundColor );
		setStatusBarLayout( uni, foregroundColor, statusbarFont );		
		
		boundingBoxLineAttributes.setLineWidth( 2 );
		//boundingBoxLineAttributes.setLinePattern( LineAttributes.PATTERN_DASH_DOT );
		
		return uni;
	}

	public static void setStatusBarLayout( final Image3DUniverse universe, final Color3f color, final Font font )
	{
		final Label status = getStatusBar( universe );
		status.setForeground( new Color((int)(color.x*255), (int)(color.y*255), (int)(color.z*255)) );
		status.setFont( font );
	}

	public static void setStatusBar( final Image3DUniverse universe, final String text )
	{
		final Label status = getStatusBar(universe);
		status.setText( text );

	}

	public static void setBackgroundColor( final Image3DUniverse universe, final Color3f color )
	{
		final Background background = ((ImageCanvas3D)universe.getCanvas()).getBG();
		background.setColor( color );

		final Label status = getStatusBar(universe);
		status.setBackground(new Color((int)(color.x*255), (int)(color.y*255), (int)(color.z*255)));
	}

	public static Label getStatusBar( final Image3DUniverse universe )
	{
		return universe.getWindow().getStatusLabel();
	}

}
