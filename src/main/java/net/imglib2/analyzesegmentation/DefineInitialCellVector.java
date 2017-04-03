package net.imglib2.analyzesegmentation;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.vecmath.Color3f;


public class DefineInitialCellVector implements MouseListener
{
	final Color3f col1 = new Color3f( new float[]{ 12f/255f, 139f/255f, 0f/255f } );
	final Color3f col2 = new Color3f( new float[]{ 23f/255f, 97f/255f, 255f/255f } );
	final RecolorCell rcc;
	final Cells cells;

	Sphere sphere1, sphere2;

	public DefineInitialCellVector( final RecolorCell rcc, final Cells cells )
	{
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
