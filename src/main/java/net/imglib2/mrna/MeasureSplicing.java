package net.imglib2.mrna;

import ij.ImageJ;

import java.io.File;
import java.util.ArrayList;

import mpicbg.models.Point;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import spim.fiji.plugin.util.OpenImg;
import spim.fiji.spimdata.interestpoints.InterestPoint;
import spim.fiji.spimdata.interestpoints.InterestPointList;
import spim.process.fusion.export.DisplayImage;

public class MeasureSplicing
{
	final InterestPointList guideList, altList;
	final Img< FloatType > guide, alt;
	
	public MeasureSplicing( final InterestPointList guideList, final InterestPointList altList, final Img< FloatType > guide, final Img< FloatType > alt )
	{
		this.guideList = guideList;
		this.altList = altList;
		this.guide = guide;
		this.alt = alt;
	}

	public void intensityCorrectAlt()
	{
		// 35 == 4000
		// 105 == 3500
		
		final Cursor< FloatType > c = alt.localizingCursor();
		
		while ( c.hasNext() )
		{
			final FloatType t = c.next();
			
			final int z = c.getIntPosition( 2 );
			
			if ( z <= 35 )
				continue;
			
			double rel = Math.min( 1, (z - 35.0) / ( 105.0 - 35.0 ) );
			
			t.set( t.get() + 500.0f * (float)rel );
		}
	}

	public ArrayList< InterestPoint > filterAlternativeList()
	{
		final ArrayList< InterestPoint > newList = new ArrayList<InterestPoint>();

		for ( final InterestPoint alt : altList.getInterestPoints() )
		{
			boolean keep = false;
			
			for ( final InterestPoint guide : guideList.getInterestPoints() )
				if ( distXY( alt, guide ) < 1.25 && distZ( alt, guide ) < 3 )
					keep = true;
			
			if ( keep )
				newList.add( alt );
		}
		
		System.out.println( newList.size() );
		
		return newList;
	}

	private static final double distZ( final Point p1, final Point p2 )
	{
		return Math.abs( p1.getL()[ 2 ] - p2.getL()[ 2 ] );
	}

	private static final double distXY( final Point p1, final Point p2 )
	{
		double sum = 0.0;
		
		double d = p1.getL()[ 0 ] - p2.getL()[ 0 ];
		sum += d * d;
		
		d = p1.getL()[ 1 ] - p2.getL()[ 1 ];
		sum += d * d;
		
		return Math.sqrt( sum );
	}

	public void measureIntensities()
	{
		final RandomAccess< FloatType > r1 = guide.randomAccess();
		final RandomAccess< FloatType > r2 = alt.randomAccess();
		
		final int n = r1.numDimensions();
		final long[] l = new long[ n ];
		
		double min1 = Double.MAX_VALUE;
		double min2 = Double.MAX_VALUE;
		double max1 = 0;
		double max2 = 0;
		
		for ( final InterestPoint p : guideList.getInterestPoints() )
		{
			for ( int d = 0; d < n; ++d )
				l[ d ] = Math.round( p.getL()[ d ] );
			
			r1.setPosition( l );
			r2.setPosition( l );
			
			double v1 = getValue( r1 );
			double v2 = getValue( r2 );
			
			System.out.println( v1 + "\t" + v2 );
			
			min1 = Math.min( min1, v1 );
			min2 = Math.min( min2, v2 );

			max1 = Math.max( max1, v1 );
			max2 = Math.max( max2, v2 );
		}
		
		System.out.println();
		System.out.println( "min1=" + min1 + " max1=" + max1 );
		System.out.println( "min2=" + min2 + " max2=" + max2 );
	}

	protected double getValue( final RandomAccess< FloatType > r )
	{
		final int xp = r.getIntPosition( 0 );
		final int yp = r.getIntPosition( 1 );
		final int zp = r.getIntPosition( 2 );
		
		final int[] l = new int[ 3 ];
		
		double v = 0;
		
		int c = 0;
		
		for ( l[ 2 ] = zp - 1; l[ 2 ] <= zp + 1; ++l[ 2 ] )
			for ( l[ 1 ] = yp - 1; l[ 1 ] <= yp + 1; ++l[ 1 ] )
				for ( l[ 0 ] = xp - 1; l[ 0 ] <= xp + 1; ++l[ 0 ] )
				{
					r.setPosition( l );
					v += r.get().get() / 27.0f;
					++c;
					
				}
		
		//System.out.println( c + " " + v );
		//System.exit( 0 );
		
		return v;
	}
	
	public static void main( String[] args )
	{
		final String dir = "/Users/spreibi/Documents/Transcription Meeting 2014/";

		final InterestPointList guideList = new InterestPointList( new File( dir ), new File( "interestpoints/tpId_0_viewSetupId_0.mRNA" ) );
		guideList.loadInterestPoints();

		final InterestPointList altList = new InterestPointList( new File( dir ), new File( "interestpoints/tpId_0_viewSetupId_2.altExon" ) );
		altList.loadInterestPoints();

		final Img< FloatType > guide = null;//OpenImg.open( new File( dir, "41_guide.tif" ).getAbsolutePath(), new ArrayImgFactory<FloatType>() );
		final Img< FloatType > alt = null;//OpenImg.open( new File( dir, "41_alt_norm_div.tif" ).getAbsolutePath(), new ArrayImgFactory<FloatType>() );

		System.out.println( guideList.getInterestPoints().size() );
		System.out.println( altList.getInterestPoints().size() );
		
		//MeasureSplicing m = new MeasureSplicing( guideList, altList, guide, alt );
		//m.intensityCorrectAlt();
		//m.measureIntensities();
		//ArrayList< InterestPoint > newList = m.filterAlternativeList();
		//altList.setInterestPoints( newList );
		//altList.saveInterestPoints();

		System.out.println( "done" );
		//new ImageJ();
		//new DisplayImage().exportImage( alt, "alt" );
	}
}
