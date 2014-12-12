package cs335;

public class Vector3 {
	public double x;
	public double y;
	public double z;
	
	public Vector3( double x, double y, double z ) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3() {
		x = y = z = 0.0;
	}
	
	public void normalize() {
		double norm = getNorm();
		x /= norm;
		y /= norm;
		z /= norm;
	}
	
	public Vector3 getNormalized() {
		double norm = getNorm();
		return new Vector3( x /= norm, y /= norm, z /= norm );
	}
	
	public double getNorm() {
		return Math.sqrt( x * x + y * y + z * z );
	}
	
	// NOTE: This finds an orthogonal 2D vector (using x,y only).
	public Vector3 getOrthogonal() {
		return ( new Vector3( -y, x, 0.0 ) ).getNormalized();
	}
}
