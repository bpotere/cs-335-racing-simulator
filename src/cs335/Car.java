package cs335;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import com.owens.oobjloader.builder.Face;
import com.owens.oobjloader.builder.FaceVertex;
import com.owens.oobjloader.builder.VertexGeometric;
import com.owens.oobjloader.builder.VertexTexture;
import com.owens.oobjloader.parser.Parse;

public class Car {
	public static final double ACC_NORM = 0.000005;
	public static final double MAX_SPEED = 0.0035;
	public static final double MAX_SPEED_REVERSE = 0.001;
	public static final double SWAY_STEP = 0.1;
	public static final double CAR_RADIUS = 4.0;
	public static final double INITIAL_V_T = 0.003;
	protected TempBuilder model;
	protected double tire_rotation;
	protected Vector3 position = new Vector3();
	protected Vector3 velocity = new Vector3();
	protected double t;
	protected double theta;
	protected double sway;
	protected double acceleration_t;
	protected double velocity_t = INITIAL_V_T;
	protected double sway_amount_fluid = 0.0;
	
	public Car( GL2 gl ) {
		model = new TempBuilder( gl );
		try {
			new Parse( model, "models/police_car/model.obj" );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public double getVelocityT() {
		return velocity_t;
	}
	
	public double getT() {
		return t;
	}
	
	public void setT( double t ) {
		this.t = t;
	}
	
	public void update() {
		// Update sway (fluid).
		if ( sway_amount_fluid > 0.0 ) {
			swayLeft();
			sway_amount_fluid -= SWAY_STEP;
			if ( sway_amount_fluid < 0.0 )
				sway_amount_fluid = 0.0;
		} else if ( sway_amount_fluid < 0.0 ) {
			swayRight();
			sway_amount_fluid += SWAY_STEP;
			if ( sway_amount_fluid > 0.0 )
				sway_amount_fluid = 0.0;
		}
		
		
		velocity_t += acceleration_t;
		if ( velocity_t > MAX_SPEED )
			velocity_t = MAX_SPEED;
		if ( velocity_t < -MAX_SPEED_REVERSE )
			velocity_t = -MAX_SPEED_REVERSE;
		
		acceleration_t = 0.0;
		t += velocity_t;
		velocity.x = -JoglEventListener.a * Math.sin( t );
		velocity.y = JoglEventListener.b * Math.cos( t );
		Vector3 direction = velocity.getOrthogonal().getNormalized();
		
		theta = Math.atan2( velocity.y, velocity.x );
		
		//last_position.x = position.x;
		//last_position.y = position.y;
		position.x = JoglEventListener.a * Math.cos( t ) + direction.x * sway;
		position.y = JoglEventListener.b * Math.sin( t ) + direction.y * sway;
		
		// It's pretty easily to calculate, but let's just pick a constant
		// tire rotation for simplicity.
		tire_rotation += 15.0;
	}
	
	public void revertOnCollision( Car other ) {
		if ( collisionDetected( other ) ) {
			double temp = velocity_t;
			velocity_t = - velocity_t;
			
			do {
				update();
			} while ( collisionDetected( other ) );
			
			// You lose some of your velocity just for colliding.
			velocity_t = temp * 0.85;
		}
	}
	
	public boolean collisionDetected( Car other ) {
		double disp = new Vector3(
			other.getPosition().x - getPosition().x,
			other.getPosition().y - getPosition().y,
			0.0
		).getNorm();
		
		if ( disp < 2 * CAR_RADIUS )
			return true;
		else
			return false;
	}
	
	public void accelerate() {
		acceleration_t = ACC_NORM;
	}
	
	public void brake() {
		acceleration_t = -ACC_NORM;
	}
	
	public void brakeHard() {
		acceleration_t = -Math.sqrt( ACC_NORM );
	}
	
	public void swayLeft() {
		sway += SWAY_STEP;
		
		if ( sway > JoglEventListener.t_width / 2.0 - CAR_RADIUS )
			sway = JoglEventListener.t_width / 2.0 - CAR_RADIUS;
	}
	
	public void swayRight() {
		sway -= SWAY_STEP;
		
		if ( sway < -JoglEventListener.t_width / 2.0 + CAR_RADIUS )
			sway = -JoglEventListener.t_width / 2.0 + CAR_RADIUS;
	}
	
	public void swayFluid( double amount ) {
		if ( 0.0 != sway_amount_fluid )
			return;
		
		sway_amount_fluid = amount;
	}
	
	public Vector3 getPosition() {
		return position;
	}
	
	public Vector3 getVelocity() {
		return velocity;
	}
	
	protected void applyTransforms( GL2 gl ) {
		// Move to where the car should be drawn.
		gl.glTranslated( position.x, position.y, position.z );
				
		// Rotate the car around its derivative, using the track's edge
		// as the rotational origin.
		Vector3 direction = velocity.getOrthogonal().getNormalized();
		double car_offset = JoglEventListener.t_width / 2.0 - sway;
		gl.glTranslated( car_offset * direction.x, car_offset * direction.y, 0.0 );
		gl.glRotated( -JoglEventListener.camber_theta * 180 / Math.PI, velocity.x, velocity.y, 0.0 );
		gl.glTranslated( -car_offset * direction.x, -car_offset * direction.y, 0.0 );
		
		// The car should be pointing in the direction of its velocity.
		gl.glRotated( theta * 180 / Math.PI, 0.0, 0.0, 1.0 );
		
		// Initial rotation to straighten the car to point along the track.
		// If we export the car correctly, we won't have to do this.
		gl.glRotated( 180, 0, 0, 1 );
	}
	
	public double getSway() {
		return sway;
	}
	
	public void draw( GL2 gl ) {
		gl.glPushMatrix();
		applyTransforms( gl );
		
		//int bind_total = 0;
		int current_tex_id = 0;
		for ( Map.Entry<String, ArrayList<Face>> group : model.groups.entrySet() ) {
			ArrayList<Face> faces = group.getValue();
			
			//System.out.println( "Found group with key \"" + group.getKey() + "\"" );
			
			if ( group.getKey().equals( "tires_front" ) || group.getKey().equals( "tires_back" ) ) {
				gl.glPushMatrix();
				
				if ( group.getKey().equals( "tires_front" ) ) {
					gl.glTranslatef( -2.8f, 0.0f, 0.63f );
					gl.glRotated( tire_rotation, 0.0f, -1.0f, 0.0f );
					gl.glTranslatef( 2.8f, 0.0f, -0.63f );
				} else {
					gl.glTranslatef( 2.74f, 0.0f, 0.63f );
					gl.glRotated( tire_rotation, 0.0f, -1.0f, 0.0f );
					gl.glTranslatef( -2.74f, 0.0f, -0.63f );
				}
			}
			
			for ( int j = 0; j < faces.size(); ++j ) {
				Face face = faces.get( j );

				if ( 0 != face.material.texid && face.material.texid != current_tex_id ) {
					current_tex_id = face.material.texid;
					//gl.glEnable( GL2.GL_TEXTURE_2D );
					gl.glBindTexture( GL2.GL_TEXTURE_2D, face.material.texid );
					//bind_total++;
				} else {
					//gl.glDisable( GL2.GL_TEXTURE_2D );
				}
				
				gl.glBegin( GL2.GL_TRIANGLES );
				
				for ( int k = 0; k < face.vertices.size(); ++k ) {
					FaceVertex vert = face.vertices.get( k );
					VertexGeometric vert_v = vert.v;
					VertexTexture vert_t = vert.t;
					
					if ( null != vert_t )
						gl.glTexCoord2f( vert_t.u, vert_t.v ); // Use 1 - v if textures are wrong.
					gl.glVertex3f( vert_v.x, vert_v.y, vert_v.z);
				}
				
				gl.glEnd();
			}
			
			if ( group.getKey().equals( "tires_front" ) || group.getKey().equals( "tires_back" ) ) {
				gl.glPopMatrix();
			}
		}

		//System.out.println( "Needed to bind #" + bind_total );
		gl.glPopMatrix();
	}
}
