package net.imglib2.analyzesegmentation.wormfit;

public interface Score
{
	public double score( final InlierCells cells );
}
