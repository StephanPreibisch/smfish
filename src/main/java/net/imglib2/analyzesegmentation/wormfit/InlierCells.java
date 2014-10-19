package net.imglib2.analyzesegmentation.wormfit;

import ij3d.Content;
import ij3d.Image3DUniverse;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.media.j3d.BranchGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import net.imglib2.analyzesegmentation.Algebra;
import net.imglib2.analyzesegmentation.Cells;
import net.imglib2.analyzesegmentation.VisualizeSegmentation;

import com.sun.j3d.utils.geometry.Sphere;

import customnode.CustomLineMesh;

public class InlierCells
{
	final protected static AtomicInteger counter = new AtomicInteger();

	final List< InlierCell > inliers;
	float r0, r1;
	final Point3f p0, p1;

	public Color3f inlierCol = new Color3f( 1, 0, 0 );
	public Color3f vectorCol = new Color3f( 1, 0, 0 );
	public float vectorTransparency = 0.5f;
	public float linewidth = 4f;
	public Color3f truncatedConeColor = new Color3f( 0, 0, 0 );
	public float truncatedConeTransparency = 0.75f;

	HashMap< Integer, Color3f > oldColors;
	Content vector;
	BranchGroup truncatedCone;

	public InlierCells( final List< InlierCell > inliers, final float r0, final float r1, final Point3f p0, final Point3f p1 )
	{
		this.inliers = inliers;
		this.r0 = r0;
		this.r1 = r1;
		this.p0 = p0;
		this.p1 = p1;
	}

	public List< InlierCell > getInlierCells() { return inliers; }
	public float getR0() { return r0; }
	public float getR1() { return r1; }
	public Point3f getP0() { return p0; }
	public Point3f getP1() { return p1; }

	public void setR0( final float r0 ) { this.r0 = r0; }
	public void setR1( final float r1 ) { this.r1 = r1; }

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
			this.vector = VisualizeSegmentation.drawLine( univ, p0, p1, "segment " + counter.getAndIncrement() );
			this.vector.setTransparency( vectorTransparency );
			this.vector.setColor( vectorCol );
			((CustomLineMesh)this.vector.getContent().getChild( 0 )).setLineWidth( linewidth );
		}
		
		// draw the truncated cone
		if ( drawCone )
		{
			final Vector3f v0 = new Vector3f( p1.x - p0.x, p1.y - p0.y, p1.z - p0.z );
	
			final Point3f pc0 = new Point3f( 0, -v0.length()/2, 0 );
			final Point3f pc1 = new Point3f( 0, v0.length()/2, 0 );
	
			this.truncatedCone = VisualizeSegmentation.drawTruncatedCone( r0, r1, v0.length(), univ, Algebra.getTransformation( pc0, pc1, p0, p1, false ), truncatedConeColor, truncatedConeTransparency );
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
