/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author: Stephan Preibisch (stephan.preibisch@gmx.de)
 */
package net.imglib2.drift.fit;

import java.util.Collection;

import mpicbg.models.AbstractModel;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * 2d-translation {@link AbstractModel} to be applied to points in 2d-space.
 * 
 * @version 0.2b
 */
public class TranslationModel1D extends AbstractModel< TranslationModel1D > implements InvertibleCoordinateTransform
{
	static final protected int MIN_NUM_MATCHES = 1;
	
	public double tx = 0;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public double[] apply( final double[] l )
	{
		assert l.length == 1 : "1d translation transformations can be applied to 1d points only.";
		
		return new double[]{ l[ 0 ] + tx };
	}
	
	@Override
	final public void applyInPlace( final double[] l )
	{
		assert l.length == 1 : "1d translation transformations can be applied to 1d points only.";
		
		l[ 0 ] += tx;
	}
	
	@Override
	final public double[] applyInverse( final double[] l )
	{
		assert l.length == 1 : "1d translation transformations can be applied to 1d points only.";
		
		return new double[]{ l[ 0 ] - tx };
	}

	@Override
	final public void applyInverseInPlace( final double[] l )
	{
		assert l.length == 1 : "1d translation transformations can be applied to 1d points only.";
		
		l[ 0 ] -= tx;
	}
		
	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 1d translation model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
		double pcx = 0;
		double qcx = 0;
		
		double ws = 0.0f;
		
		for ( final P m : matches )
		{
			final double[] p = m.getP1().getL(); 
			final double[] q = m.getP2().getW(); 
			
			final double w = m.getWeight();
			ws += w;
			
			pcx += w * p[ 0 ];
			qcx += w * q[ 0 ];
		}
		pcx /= ws;
		qcx /= ws;

		tx = ( float )( qcx - pcx );
	}

	@Override
	public TranslationModel1D copy()
	{
		final TranslationModel1D m = new TranslationModel1D();
		m.tx = tx;
		m.cost = cost;
		return m;
	}
	
	@Override
	final public void set( final TranslationModel1D m )
	{
		tx = m.tx;
		cost = m.getCost();
	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * 1 tx
	 * 0 1
	 * 
	 * @param tx
	 */
	final public void set( final float tx )
	{
		this.tx = tx;
	}
	
	@Override
	public TranslationModel1D createInverse()
	{
		final TranslationModel1D ict = new TranslationModel1D();
		
		ict.tx = -tx;	
		ict.cost = cost;
		
		return ict;
	}
}