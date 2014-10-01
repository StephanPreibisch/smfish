package net.imglib2.analyzesegmentation;

import java.util.HashMap;
import java.util.Map;

import com.sun.j3d.utils.geometry.Sphere;

public class Cells
{
	Map< Integer, Cell > cells;
	Map< Integer, Sphere > spheres;
	
	public Cells()
	{
		this.cells = new HashMap< Integer, Cell >();
		this.spheres = new HashMap< Integer, Sphere >();
	}

	public Map< Integer, Cell > getCells() { return cells; }
	public Map< Integer, Sphere > getSpheres() { return spheres; }

	public void setCells( final Map< Integer, Cell > cells ) { this.cells = cells; }
	public void setSpheres( final Map< Integer, Sphere > spheres ) { this.spheres = spheres; }
}
