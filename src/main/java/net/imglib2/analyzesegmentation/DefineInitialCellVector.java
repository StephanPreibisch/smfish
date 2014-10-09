package net.imglib2.analyzesegmentation;

import ij3d.Content;
import ij3d.Image3DUniverse;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import com.sun.j3d.utils.geometry.Sphere;

public class DefineInitialCellVector implements MouseListener
{
	final Color3f col1 = new Color3f( new float[]{ 12f/255f, 139f/255f, 0f/255f } );
	final Color3f col2 = new Color3f( new float[]{ 23f/255f, 97f/255f, 255f/255f } );
	final RecolorCell rcc;
	final Image3DUniverse univ;
	final Cells cells;

	Sphere sphere1, sphere2;

	public DefineInitialCellVector( final Image3DUniverse univ, final RecolorCell rcc, final Cells cells )
	{
		this.univ = univ;
		this.rcc = rcc;
		this.cells = cells;
	}

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		if ( sphere2 == null && e.getButton() == MouseEvent.BUTTON1 && rcc.getCurrentSphere() != null )
		{
			final Color3f col;
			if ( sphere1 == null )
			{
				col = col1;
				sphere1 = rcc.getCurrentSphere();
			}
			else if ( sphere2 == null && sphere1 != rcc.getCurrentSphere() )
			{
				col = col2;
				sphere2 = rcc.getCurrentSphere();
				
				final List< Point3f > lineMesh = new ArrayList< Point3f >();
				
				final Cell cell1 = (Cell)sphere1.getUserData();
				final Cell cell2 = (Cell)sphere2.getUserData();
				
				lineMesh.add( new Point3f( cell1.getPosition().getFloatPosition( 0 ), cell1.getPosition().getFloatPosition( 1 ), cell1.getPosition().getFloatPosition( 2 ) ) );
				lineMesh.add( new Point3f( cell2.getPosition().getFloatPosition( 0 ), cell2.getPosition().getFloatPosition( 1 ), cell2.getPosition().getFloatPosition( 2 ) ) );
				
				final Content content = univ.addLineMesh( lineMesh, new Color3f(), "InitialVector", false );
				content.showCoordinateSystem( false );
			}
			else
			{
				return;
			}

			rcc.getCurrentColor().set( col );

			final Color3f tmpColor = new Color3f();
			rcc.getCurrentSphere().getAppearance().getColoringAttributes().getColor( tmpColor );
			tmpColor.set( ( tmpColor.x + col.x )/2, ( tmpColor.y + col.y )/2, ( tmpColor.z + col.z )/2 );
			
			rcc.getCurrentSphere().getAppearance().getColoringAttributes().setColor( tmpColor );
		}
	}

	@Override
	public void mouseEntered( final MouseEvent e ) {}

	@Override
	public void mouseExited( final MouseEvent e ) {}

	@Override
	public void mousePressed( final MouseEvent e ) {}

	@Override
	public void mouseReleased( final MouseEvent e ) {}

	public Sphere getSphere1() { return sphere1; }
	public Sphere getSphere2() { return sphere2; }
}
