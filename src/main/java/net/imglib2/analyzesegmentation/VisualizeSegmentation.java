package net.imglib2.analyzesegmentation;

import ij3d.Content;
import ij3d.Image3DUniverse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.util.Util;
import mpicbg.spim.vis3d.VisualizeBeads;
import net.imglib2.RealPoint;
import net.imglib2.analyzesegmentation.wormfit.InlierCells;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import spim.fiji.spimdata.interestpoints.InterestPoint;
import spim.fiji.spimdata.interestpoints.InterestPointList;

import com.sun.j3d.utils.geometry.Sphere;

import customnode.CustomPointMesh;
import customnode.Mesh_Maker;

public class VisualizeSegmentation
{
	public static String defaultXML = "/Users/spreibi/Documents/Microscopy/smFISH/samidouble_43_reconstructed.cells.xml";

	final double scaleZ = 1.6;

	final JFileChooser fileChooser;
	final Cells cells = new Cells();
	int nextCellId = 0;

	public VisualizeSegmentation()
	{
		this.fileChooser = createFileChooser();

		// TODO: remove correction for wrong calibration
		if ( this.loadAnnotations( scaleZ ) )
		{
			final Image3DUniverse univ = VisualizeBeads.initUniverse();

			drawInvisibleBoundingBox( univ, cells.getCells() );
			drawCells( univ, cells, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f );

			//normalizeDetections( univ, null );

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

			final FindWormOutline fwo = new FindWormOutline( univ, cells, ((Cell)dicv.getSphere1().getUserData()), ((Cell)dicv.getSphere2().getUserData()), 25 );
			fwo.findOutline();

			System.out.println( "done" );

			//normalizeDetections( univ, fwo );

			//System.exit( 0 );
			//VisualizationFunctions.drawArrow( univ, new Vector3f( new float[]{ 100, 100, 100 } ), 45, 10 );
			//drawNuclei( univ, cells, new Transform3D(), 0.95f );
		}
	}

	public void normalizeDetections( final Image3DUniverse univ, final FindWormOutline fwo )
	{
		final String dir = "/Users/spreibi/Documents/Transcription Meeting 2014/";

		final InterestPointList guideList = new InterestPointList( new File( dir ), new File( "interestpoints/tpId_0_viewSetupId_1.mRNA" ) );
		guideList.loadInterestPoints();

		final InterestPointList altList = new InterestPointList( new File( dir ), new File( "interestpoints/tpId_0_viewSetupId_3.altExon" ) );
		altList.loadInterestPoints();

		for ( final InterestPoint p : guideList.getInterestPoints() )
			p.getL()[ 2 ] *= scaleZ;

		for ( final InterestPoint p : altList.getInterestPoints() )
			p.getL()[ 2 ] *= scaleZ;

		if ( fwo == null )
		{
			drawInterestPoints( univ, guideList.getInterestPoints(), new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f );
		}
		else
		{
			//final List< InterestPoint > straightGuide = stretchWorm( fwo, guideList.getInterestPoints() );
			//drawInterestPoints( univ, straightGuide, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f );
			
			final Cells cellsNew = stretchWormCells( fwo, cells );
			drawCells( univ, cellsNew, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f );
			
			/*
			System.out.println( "guide" );
			for ( final InterestPoint p : stretchWorm( fwo, guideList.getInterestPoints() ) )
				System.out.println( p.getId() + "\t" + p.getL()[ 0 ] + "\t" + p.getL()[ 1 ] + "\t" + p.getL()[ 2 ] );

			System.out.println( "\nalt" );
			for ( final InterestPoint p : stretchWorm( fwo, altList.getInterestPoints() ) )
				System.out.println( p.getId() + "\t" + p.getL()[ 0 ] + "\t" + p.getL()[ 1 ] + "\t" + p.getL()[ 2 ] );
			*/
		}
	}

	public Cells stretchWormCells( final FindWormOutline fwo, final Cells cells )
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

	public List< InterestPoint > stretchWormInterestPoints( final FindWormOutline fwo, final List< InterestPoint > list )
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

	public List< Pair< Integer, float[] > > stretchWorm( final FindWormOutline fwo, final List< Pair< Integer, float[] > > list )
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
			final Point3f qx = new Point3f( p.getB() );
			t.transform( qx );

			// now we can simply measure the y
			final double y = qx.y - closest.getP0().y;
			final double z = qx.z - closest.getP0().z;

			straight.add( new ValuePair< Integer, float[] >( p.getA(), new float[]{ (float)x, (float)y, (float)z } ) );
		}

		return straight;
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
			final int r = random.nextInt( 192 );
			final Appearance appearance = new Appearance();
			appearance.setColoringAttributes( new ColoringAttributes( new Color3f( r/255f, 0, r/255f ), ColoringAttributes.SHADE_GOURAUD ) );
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

	protected synchronized boolean loadAnnotations( final double scaleZ )
	{
		if ( defaultXML != null )
		{
			fileChooser.setSelectedFile( new File( defaultXML ) );
		}

		final int returnVal = fileChooser.showDialog( null, "Open" );

		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			final File file = fileChooser.getSelectedFile();
			defaultXML = file.getAbsolutePath();

			System.out.println( "loading annotions from " + file );

			cells.getCells().clear();
			cells.getSpheres().clear();

			nextCellId = 0;

			try
			{
				final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				final DocumentBuilder db = dbf.newDocumentBuilder();
				final Document dom = db.parse( file );
				final Element root = dom.getDocumentElement();
				final NodeList nodes = root.getElementsByTagName( "sphere" );
				for ( int i = 0; i < nodes.getLength(); ++i )
				{
					final Cell cell = Cell.fromXml( ( Element ) nodes.item( i ) );
					
					cell.getPosition().setPosition( cell.getDoublePosition( 2 ) * scaleZ, 2 );
					
					cells.getCells().put( cell.getId(), cell );
					if ( cell.getId() >= nextCellId )
						nextCellId = cell.getId() + 1;
				}

				System.out.println( "Loaded " + cells.getCells().keySet().size() + " cells." );
			}
			catch ( final Exception e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return true;
		}
		else
		{
			return false;
		}
	}

	protected JFileChooser createFileChooser()
	{
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter( new FileFilter()
		{
			@Override
			public String getDescription() { return "xml files"; }

			@Override
			public boolean accept( final File f )
			{
				if ( f.isDirectory() )
					return true;
				if ( f.isFile() )
				{
					final String s = f.getName();
					final int i = s.lastIndexOf('.');
					if (i > 0 &&  i < s.length() - 1) {
						final String ext = s.substring(i+1).toLowerCase();
						return ext.equals( "xml" );
					}
				}
				return false;
			}
		} );

		return fileChooser;
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
