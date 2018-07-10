package net.imglib2.analyzesegmentation.wormfit;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import customnode.CustomLineMesh;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.analyzesegmentation.Algebra;
import net.imglib2.analyzesegmentation.Cells;
import net.imglib2.analyzesegmentation.Java3DHelpers;

public class InlierCells
{
	final protected static AtomicInteger counter = new AtomicInteger();

	final List< InlierCell > inliers;
	double r0, r1;
	final Point3d p0, p1;

	public Color3f inlierCol = new Color3f( 1, 0, 0 );
	public Color3f vectorCol = new Color3f( 1, 0, 0 );
	public float vectorTransparency = 0.5f;
	public float linewidth = 4f;
	public Color3f truncatedConeColor = new Color3f( 0, 0, 0 );
	public float truncatedConeTransparency = 0.75f;

	HashMap< Integer, Color3f > oldColors;
	Content vector;
	BranchGroup truncatedCone;

	public InlierCells( final List< InlierCell > inliers, final double r0, final double r1, final Point3d p0, final Point3d p1 )
	{
		this.inliers = inliers;
		this.r0 = r0;
		this.r1 = r1;
		this.p0 = p0;
		this.p1 = p1;
	}

	public List< InlierCell > getInlierCells() { return inliers; }
	public double getR0() { return r0; }
	public double getR1() { return r1; }
	public Point3d getP0() { return p0; }
	public Point3d getP1() { return p1; }

	public void setR0( final double r0 ) { this.r0 = r0; }
	public void setR1( final double r1 ) { this.r1 = r1; }

	public void visualizeInliers( final Image3DUniverse univ, final Cells cells, final boolean colorInliers, final boolean drawLine, final boolean drawCone )
	{
		// color inliers
		if ( colorInliers )
		{
			boolean saveColors = false;

			if ( this.oldColors == null )
			{
				this.oldColors = new HashMap< Integer, Color3f >();
				saveColors = true;
			}

			for ( final InlierCell inlier : getInlierCells() )
			{
				final int id = inlier.getCell().getId();
				final Sphere s = cells.getSpheres().get( id );

				if ( saveColors )
				{
					final Color3f c = new Color3f();
					s.getAppearance().getColoringAttributes().getColor( c );
					this.oldColors.put( id, c );
				}
				
				s.getAppearance().getColoringAttributes().setColor( inlierCol );
			}
		}

		// draw the line segment
		if ( this.vector != null )
			univ.removeContent( this.vector.getName() );

		if ( drawLine )
		{
			this.vector = Java3DHelpers.drawLine( univ, p0, p1, "segment " + counter.getAndIncrement() );
			this.vector.setTransparency( vectorTransparency );
			this.vector.setColor( vectorCol );
			((CustomLineMesh)this.vector.getContent().getChild( 0 )).setLineWidth( linewidth );
		}
		
		// draw the truncated cone
		if ( drawCone )
		{
			final Vector3d v0 = new Vector3d( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );
	
			final Point3d pc0 = new Point3d( 0, -v0.length()/2, 0 );
			final Point3d pc1 = new Point3d( 0, v0.length()/2, 0 );
	
			this.truncatedCone = Java3DHelpers.drawTruncatedCone( (float)r0, (float)r1, (float)v0.length(), univ, Algebra.getTransformation( pc0, pc1, p0, p1, false ), truncatedConeColor, truncatedConeTransparency );
		}
	}

	public void unvisualizeInliers( final Image3DUniverse univ, final Cells cells )
	{
		// restore colors
		if ( this.oldColors != null )
		{
			for ( final InlierCell inlier : getInlierCells() )
			{
				final int id = inlier.getCell().getId();
				final Sphere s = cells.getSpheres().get( id );

				s.getAppearance().getColoringAttributes().setColor( this.oldColors.get( id ) );
			}
		}

		// remove line segment
		if ( this.vector != null )
		{
			univ.removeContent( this.vector.getName() );
			this.vector = null;
		}

		// remove the truncated cone
		if ( this.truncatedCone != null )
		{
			univ.getScene().removeChild( this.truncatedCone );
			this.truncatedCone = null;
		}
	}
}
