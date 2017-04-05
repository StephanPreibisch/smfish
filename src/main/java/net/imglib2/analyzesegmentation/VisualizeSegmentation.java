package net.imglib2.analyzesegmentation;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

import ij3d.Image3DUniverse;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.spim.io.TextFileAccess;
import net.imglib2.analyzesegmentation.load.Load;
import net.imglib2.analyzesegmentation.load.LoadDauer;
import spim.fiji.spimdata.interestpoints.InterestPoint;
import spim.fiji.spimdata.interestpoints.InterestPointList;

public class VisualizeSegmentation
{
	final String dir = "/Users/spreibi/Documents/Transcription Meeting 2014/";
	final double scaleZ = 1.6;

	final Cells cells;
	int nextCellId = 0;

	public VisualizeSegmentation()
	{
		final boolean visualizeStretching = true;

		// Loaded 560 cells, distributed in space: [271.95, 23.16, 70.057] -> [1170.60, 962.07, 186.56], dimensions (898.65, 938.91, 116.50)
		//final Load loader = new LoadBDV( scaleZ );

		// Loaded 672 cells, distributed in space: [755.245, 1561.19, 103.15] -> [2500.66, 14025.16, 1242.0], dimensions (1745.41, 12463.98, 1138.85)
		final Load loader = new LoadDauer( 1.0 / 3, 0.85 );

		//final Load loader = new LoadInterestPoints( 1.0 );

		this.cells = loader.load();

		System.out.println( loader );

		if ( this.cells != null )
		{
			final Image3DUniverse univ = Java3DHelpers.initUniverse();
			final List< InterestPoint > guide = null;loadInterestPoints( dir, "interestpoints/tpId_0_viewSetupId_1.mRNA", scaleZ );
			final List< InterestPoint > alt = null;//loadInterestPoints( dir, "interestpoints/tpId_0_viewSetupId_3.altExon.fused", scaleZ );

			Java3DHelpers.drawInvisibleBoundingBox( univ, cells.getCells() );
			BranchGroup c = Java3DHelpers.drawCells( univ, cells, new Transform3D(), 0.15f );
			BranchGroup i1 = null;//Java3DHelpers.drawInterestPoints( univ, alt, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f, true );
			BranchGroup i2 = null;//Java3DHelpers.drawInterestPoints( univ, guide, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f, false );


			final RecolorCell rcc = new RecolorCell( univ, new Color3f( 1, 0, 0 ) );
			univ.getCanvas().addMouseMotionListener( rcc );

			final DefineInitialCellVector dicv = new DefineInitialCellVector( rcc, cells );
			univ.getCanvas().addMouseListener( dicv );

			// wait until two cells are selected
			do { SimpleMultiThreading.threadWait( 100 ); }
			while ( dicv.getSphere2() == null );

			rcc.setActive( false );
			FindWormOutline.makeScreenshot( 0 );

			if ( visualizeStretching )
			{
				c.detach();
				if ( i1 != null )
					i1.detach();
				if ( i2 != null )
					i2.detach();
			}

			final FindWormOutline fwo = new FindWormOutline( visualizeStretching? null : univ, cells, ((Cell)dicv.getSphere1().getUserData()), ((Cell)dicv.getSphere2().getUserData()), 40 );
			fwo.findOutline();

			System.out.println( "done" );

			if ( !visualizeStretching )
				SimpleMultiThreading.threadHaltUnClean();

			c = Java3DHelpers.drawCells( univ, StraightenWorm.stretchWormCells( fwo, cells, 0 ), new Transform3D(), 0.15f );
			SimpleMultiThreading.threadWait( 5000 );
			c.detach();

			int i = 0;

			for ( float amount = 1f; amount >= 0; amount -= 0.01f )
			{
				System.out.println( amount );

				PrintWriter out;

				if ( guide != null )
				{
					out = TextFileAccess.openFileWrite( new File( dir, "guideRNA.straight.txt" ) );
					final List< InterestPoint > guideStretch = StraightenWorm.stretchWormInterestPoints( fwo, guide, amount );
					for ( final InterestPoint ip : guideStretch )
						out.println( ip.getId() + "\t" + ip.getL()[ 0 ] + "\t" + ip.getL()[ 1 ]  + "\t" + ip.getL()[ 2 ] );
					out.close();

					i2 = Java3DHelpers.drawInterestPoints( univ, guideStretch, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f, false );
				}
	
				if ( alt != null )
				{
					out = TextFileAccess.openFileWrite( new File( dir, "altexonRNA.straight.txt" ) );
					final List< InterestPoint > altStretch = StraightenWorm.stretchWormInterestPoints( fwo, alt, amount );
					for ( final InterestPoint ip : altStretch )
						out.println( ip.getId() + "\t" + ip.getL()[ 0 ] + "\t" + ip.getL()[ 1 ]  + "\t" + ip.getL()[ 2 ] );
					out.close();

					i1 = Java3DHelpers.drawInterestPoints( univ, altStretch, new Transform3D(), new Color3f( 1, 0, 1 ), 0.15f, true );
				}

				out = TextFileAccess.openFileWrite( new File( dir, "cells.straight.txt" ) );
				final Cells cellsNew = StraightenWorm.stretchWormCells( fwo, cells, amount );
				for ( final Cell cell : cellsNew.getCells().values() )
					out.println( cell.getId() + "\t" + cell.getFloatPosition( 0 ) + "\t" + cell.getFloatPosition( 1 )  + "\t" + cell.getFloatPosition( 2 ) );
				out.close();
	
				c = Java3DHelpers.drawCells( univ, cellsNew, new Transform3D(), 0.15f );

				SimpleMultiThreading.threadWait( 250 );
				FindWormOutline.makeScreenshot( i++ );
				SimpleMultiThreading.threadWait( 100 );

				if ( amount < 0.02 )
					SimpleMultiThreading.threadHaltUnClean();
				c.detach();
				if ( i1 != null )
					i1.detach();
				if ( i2 != null )
					i2.detach();
			}
			//System.exit( 0 );
			//VisualizationFunctions.drawArrow( univ, new Vector3f( new float[]{ 100, 100, 100 } ), 45, 10 );
			//drawNuclei( univ, cells, new Transform3D(), 0.95f );
		}
	}

	public static List< InterestPoint > loadInterestPoints( final String dir, final String interestPoints, final double scaleZ )
	{
		return loadInterestPoints( dir, new File( interestPoints ), scaleZ );
	}

	public static List< InterestPoint > loadInterestPoints( final String dir, final File interestPoints, final double scaleZ )
	{
		final InterestPointList list = new InterestPointList( new File( dir ), interestPoints );
		list.loadInterestPoints();

		for ( final InterestPoint p : list.getInterestPoints() )
			p.getL()[ 2 ] *= scaleZ;

		return list.getInterestPoints();
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
