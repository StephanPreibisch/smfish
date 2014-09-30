package net.imglib2.analyzesegmentation;

import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;

import javax.media.j3d.Node;
import javax.media.j3d.PickInfo;
import javax.media.j3d.SceneGraphPath;
import javax.vecmath.Color3f;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.pickfast.PickCanvas;

public class RecolorCell
{
	final PickCanvas pickCanvas;
	final Image3DUniverse univ;
	final Color3f colorActive;

	Sphere oldSphere = null;
	Color3f oldColor = new Color3f();

	public RecolorCell( final Image3DUniverse univ, final Color3f colorActive )
	{
		this.univ = univ;
		this.colorActive = colorActive;
		this.pickCanvas = new PickCanvas( (ImageCanvas3D)univ.getCanvas(), univ.getScene() );

		pickCanvas.setMode( PickInfo.PICK_GEOMETRY );
		pickCanvas.setFlags( PickInfo.SCENEGRAPHPATH | PickInfo.CLOSEST_INTERSECTION_POINT );
		pickCanvas.setTolerance( 3 );
	}

	public void testLocation( final int x, final int y )
	{
		pickCanvas.setShapeLocation( x, y );

		final PickInfo info = pickCanvas.pickClosest();

		if ( info == null )
		{
			if ( oldSphere != null )
			{
				oldSphere.getAppearance().getColoringAttributes().setColor( oldColor );
				oldSphere = null;
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
				
				if ( newSphere == oldSphere )
					return;

				if ( oldSphere != null )
					oldSphere.getAppearance().getColoringAttributes().setColor( oldColor );

				newSphere.getAppearance().getColoringAttributes().getColor( oldColor );
				newSphere.getAppearance().getColoringAttributes().setColor( colorActive );
				oldSphere = newSphere;
			}
		}
	}
}
