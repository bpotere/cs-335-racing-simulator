package cs335;

import java.awt.event.KeyEvent;

import javax.media.opengl.glu.GLU;

public class Camera {
	public final double VERTICAL_DELTA = 0.1;
	private double eye_x;
	private double eye_y;
	private double eye_z;
	private double look_x = 1.0;
	private double look_y;
	private double look_z;
	private GLU glu = new GLU();
	
	public Camera() {
	}
	
	public double getEyeX() {
		return eye_x;
	}
	
	public double getEyeY() {
		return eye_y;
	}
	
	public double getEyeZ() {
		return eye_z;
	}
	
	public double getLookX() {
		return look_x;
	}
	
	public double getLookY() {
		return look_y;
	}
	
	public double getLookZ() {
		return look_z;
	}
	
	public void move( double dx, double dy, double dz ) {
		eye_x += dx;
		eye_y += dy;
		eye_z += dz;
	}
	
	public void strafe( double d, double direction ) {
		double theta = Math.atan2( look_y, look_x );
		double phi = Math.acos( look_z );
		
		theta += Math.PI / 2.0 * direction;
		
		double strafe_x = Math.cos( theta ) * Math.sin( phi );
		double strafe_y = Math.sin( theta ) * Math.sin( phi );
		double normxy = Math.sqrt( strafe_x * strafe_x + strafe_y * strafe_y );
		
		eye_x += strafe_x / normxy * d;
		eye_y += strafe_y / normxy * d;
	}
	
	public void strafeLeft( double d ) {
		strafe( d, 1.0 );
	}
	
	public void strafeRight( double d ) {
		strafe( d, -1.0 );
	}
	
	public void moveForward( double d ) {
		double normxy = Math.sqrt( look_x * look_x + look_y * look_y );
		eye_x += look_x / normxy * d;
		eye_y += look_y / normxy * d;
	}
	
	public void moveBackward( double d ) {
		double normxy = Math.sqrt( look_x * look_x + look_y * look_y );
		eye_x -= look_x / normxy * d;
		eye_y -= look_y / normxy * d;
	}
	
	public void moveUp( double d ) {
		move( 0.0, 0.0, d );
	}
	
	public void moveDown( double d ) {
		move( 0.0, 0.0, -d );
	}
	
	public void rotate( double dtheta, double dphi ) {
		double phi = Math.acos( look_z );
		double theta = Math.atan2( look_y, look_x );
		
		theta -= dtheta;
		phi += dphi;
		
		if ( theta >= Math.PI * 2.0 )
			theta -= Math.PI * 2.0;
		else if ( theta < 0.0 )
			theta += Math.PI * 2.0;
		
		if ( phi > Math.PI - VERTICAL_DELTA )
			phi = Math.PI - VERTICAL_DELTA;
		else if ( phi < VERTICAL_DELTA )
			phi = VERTICAL_DELTA;
		
		look_x = Math.cos( theta ) * Math.sin( phi );
		look_y = Math.sin( theta ) * Math.sin( phi );
		look_z = Math.cos( phi );
	}
	
	public void look() {
		glu.gluLookAt( eye_x, eye_y, eye_z, eye_x + look_x, eye_y + look_y, eye_z + look_z, 0.0, 0.0, 1.0 );
	}
}
