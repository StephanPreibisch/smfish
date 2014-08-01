package net.imglib2.scalespace;

/**
 * Simple 2d float array that stores all values in one linear array.
 * 
 * <p>License: GPL
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
 * @author Stephan Preibisch (and Stephan Saalfeld the deleter)
 * @version 0.2b
 */
public class FloatArray3D extends FloatArray
{
	final public int width;
	final public int height;
	final public int depth;

	public FloatArray3D( final int width, final int height, final int depth )
	{
		data = new float[ width * height * depth ];
		this.width = width;
		this.height = height;
		this.depth = depth;
	}

	public FloatArray3D( final float[] data, final int width, final int height, final int depth )
	{
		this.data = data;
		this.width = width;
		this.height = height;
		this.depth = depth;
	}

	public FloatArray3D clone()
	{
		FloatArray3D clone = new FloatArray3D( data.clone(), width, height, depth );
		return clone;
	}

	final public float get( final int x, final int y, final int z )
	{
		return data[ z * width * height + y * width + x ];
	}

	final public void set( final float value, final int x, final int y, final int z )
	{
		data[ z * width * height + y * width + x ] = value;
	}
}
