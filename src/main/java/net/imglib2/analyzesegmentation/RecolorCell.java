package net.imglib2.analyzesegmentation;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import org.scijava.java3d.Node;
import org.scijava.java3d.SceneGraphPath;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.java3d.utils.picking.PickCanvas;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.java3d.utils.picking.PickTool;
import org.scijava.vecmath.Color3f;

import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;

public class RecolorCell implements MouseMotionListener
{
	final PickCanvas pickCanvas;
	final Image3DUniverse univ;
	final Color3f colorActive;

	Sphere currentSphere = null;
	Color3f currentColor = new Color3f();

	boolean active;

	public RecolorCell( final Image3DUniverse univ, final Color3f colorActive )
	{
		this.univ = univ;
		this.colorActive = colorActive;
		this.pickCanvas = new PickCanvas( (ImageCanvas3D)univ.getCanvas(), univ.getScene() );
		this.active = true;

		pickCanvas.setMode( PickTool.GEOMETRY );
		//pickCanvas.setFlags( PickInfo.SCENEGRAPHPATH | PickInfo.CLOSEST_INTERSECTION_POINT );
		pickCanvas.setTolerance( 0 );
	}

	public Sphere getCurrentSphere() { return currentSphere; }
	public Color3f getCurrentColor() { return currentColor; }
	public void setActive( final boolean active )
	{ 
		this.active = active;

		if ( currentSphere != null )
		{
			currentSphere.getAppearance().getColoringAttributes().setColor( currentColor );
			currentSphere = null;
		}
	}

	@Override
	public void mouseMoved( final MouseEvent arg0 ) { testLocation( arg0.getPoint().x, arg0.getPoint().y ); }

	@Override
	public void mouseDragged( final MouseEvent arg0 ) {}

	public void testLocation( final int x, final int y )
	{
		pickCanvas.setShapeLocation( x, y );

		final PickResult info = pickCanvas.pickClosest();

		if ( info == null )
		{
			if ( currentSphere != null )
			{
				currentSphere.getAppearance().getColoringAttributes().setColor( currentColor );
				currentSphere = null;
			}

			return;
		}

		SceneGraphPath path = info.getSceneGraphPath();
		
		for ( int j = path.nodeCount() - 1; j >= 0; --j )
		{
			final Node node = path.getNode( j );

			if ( Sphere.class.isInstance( node ) )
			{
				final Sphere newSphere = (Sphere)node;
				
				if ( newSphere == currentSphere )
					return;

				if ( currentSphere != null )
					currentSphere.getAppearance().getColoringAttributes().setColor( currentColor );

				newSphere.getAppearance().getColoringAttributes().getColor( currentColor );
				newSphere.getAppearance().getColoringAttributes().setColor( colorActive );
				currentSphere = newSphere;

				if ( currentSphere.getUserData() != null )
					System.out.println( "Cell id: "  + ((Cell)currentSphere.getUserData()).getId() );
			}
		}
	}
}
