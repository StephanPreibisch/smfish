package net.imglib2.analyzesegmentation;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;

import javax.media.j3d.Node;
import javax.media.j3d.PickInfo;
import javax.media.j3d.SceneGraphPath;
import javax.vecmath.Color3f;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.pickfast.PickCanvas;

public class RecolorCell implements MouseMotionListener
{
	final PickCanvas pickCanvas;
	final Image3DUniverse univ;
	final Color3f colorActive;

	Sphere currentSphere = null;
	Color3f currentColor = new Color3f();

	public RecolorCell( final Image3DUniverse univ, final Color3f colorActive )
	{
		this.univ = univ;
		this.colorActive = colorActive;
		this.pickCanvas = new PickCanvas( (ImageCanvas3D)univ.getCanvas(), univ.getScene() );

		pickCanvas.setMode( PickInfo.PICK_GEOMETRY );
		pickCanvas.setFlags( PickInfo.SCENEGRAPHPATH | PickInfo.CLOSEST_INTERSECTION_POINT );
		pickCanvas.setTolerance( 0 );
	}

	public Sphere getCurrentSphere() { return currentSphere; }
	public Color3f getCurrentColor() { return currentColor; }

	@Override
	public void mouseMoved( final MouseEvent arg0 ) { testLocation( arg0.getPoint().x, arg0.getPoint().y ); }

	@Override
	public void mouseDragged( final MouseEvent arg0 ) {}

	public void testLocation( final int x, final int y )
	{
		pickCanvas.setShapeLocation( x, y );

		final PickInfo info = pickCanvas.pickClosest();

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

				System.out.println( "Cell id: "  + ((Cell)currentSphere.getUserData()).getId() );
			}
		}
	}
}
