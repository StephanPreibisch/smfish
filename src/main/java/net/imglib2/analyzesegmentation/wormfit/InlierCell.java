package net.imglib2.analyzesegmentation.wormfit;

import net.imglib2.analyzesegmentation.Cell;

public class InlierCell
{
	final double dist, t;
	final Cell cell;
	
	public InlierCell( final Cell cell, final double dist, final double t )
	{
		this.cell = cell;
		this.dist = dist;
		this.t = t;
	}

	public Cell getCell() { return cell; }
	public double getDistance() { return dist; }
	public double getT() { return t; }
}
