package net.imglib2.analyzesegmentation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Label;

import org.scijava.java3d.Background;
import org.scijava.java3d.LineAttributes;
import org.scijava.vecmath.Color3f;

import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;

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

	public static Image3DUniverse initUniverse()
	{
		final Image3DUniverse uni = new Image3DUniverse( 1200, 600 );
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
