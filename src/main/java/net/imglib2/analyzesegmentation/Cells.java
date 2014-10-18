package net.imglib2.analyzesegmentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.collection.KDTree;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;

import com.sun.j3d.utils.geometry.Sphere;

public class Cells
{
	Map< Integer, Cell > cells;
	Map< Integer, Sphere > spheres;
	
	KDTree< Cell > tree;
	RadiusNeighborSearchOnKDTree< Cell > search;

	public Cells()
	{
		this.cells = new HashMap< Integer, Cell >();
		this.spheres = new HashMap< Integer, Sphere >();
	}

	public void buildKDTree()
	{
		// kdtree to only check points that are close to the segmented line
		final ArrayList< Cell > cellList = new ArrayList< Cell >();
		cellList.addAll( getCells().values() );
		tree = new KDTree< Cell >( cellList, cellList );
		search = new RadiusNeighborSearchOnKDTree< Cell >( tree );
	}

	public KDTree< Cell > getKDTree()
	{
		if ( tree == null )
			buildKDTree();

		return tree;
	}

	public RadiusNeighborSearchOnKDTree< Cell > getSearch()
	{
		getKDTree();
		return search;
	}

	public Map< Integer, Cell > getCells() { return cells; }
	public Map< Integer, Sphere > getSpheres() { return spheres; }

	public void setCells( final Map< Integer, Cell > cells ) { this.cells = cells; }
	public void setSpheres( final Map< Integer, Sphere > spheres ) { this.spheres = spheres; }
}
