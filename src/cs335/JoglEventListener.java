package cs335;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;

import com.owens.oobjloader.builder.Build;
import com.owens.oobjloader.builder.Face;
import com.owens.oobjloader.builder.FaceVertex;
import com.owens.oobjloader.builder.VertexGeometric;
import com.owens.oobjloader.builder.VertexTexture;
import com.owens.oobjloader.parser.Parse;

public class JoglEventListener implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {
	private int windowWidth, windowHeight;
	
	private String skybox_name = "ThickCloudsWater";
	private Skybox skybox = null;
	private TextureLoader texture_loader = null;
	private final float skybox_size = 2000.0f;
	
	private final int[] track_textures = new int[3]; // Asphalt etc. goes here
	
	//The major and minor axis of the racetrack ellipse:
	private double a = 625; // Production value: 625
	private double b = 250; // Production value: 250
	
	// Multiple cameras. The generic "camera" always points to whatever
	// one is in use.
	private Camera camera_fp = null;
	private Camera camera_free = null;
	private Camera camera = null;
	
	// Testing
	private TempBuilder car = null;
	
	// Testing basic AI
	private float ai_car_x = 0.0f;
	private float ai_car_y = 250.0f;
	private float ai_car_t = 0.0f;
	private float ai_car_theta = 0.0f;
	
	private double[] control_points = {
		127.29361,186.37989, 414.66858,7.0116203, 576.67863,280.88575,
		738.68868,554.75989, 187.08303,539.33036, 437.81287,869.13653,
		688.54271,1198.9427, 55.932041,955.92763, 94.505863,869.13653,
		133.07968,782.34543, 316.30534,548.97381, 200.58387,365.74816,
		84.862407,182.52251, 127.29361,186.37989, 127.29361,186.37989 // z
	};
	
	private final float frame_step = 0.01f;
	private float tire_rotation = 0.0f;
	
	private int mouse_x0 = 0;
	private int mouse_y0 = 0;
	
	private int mouse_mode = 0;
	
	private final int MOUSE_MODE_NONE = 0;
	private final int MOUSE_MODE_ROTATE = 1;
	
	private boolean[] keys = new boolean[256];
	
	private GLU glu = new GLU();
	
	public void displayChanged(
			GLAutoDrawable gLDrawable,
			boolean modeChanged,
			boolean deviceChanged) {
	}

	public void init( GLAutoDrawable gLDrawable ) {
		GL2 gl = gLDrawable.getGL().getGL2();
		//gl.glShadeModel(GL.GL_LINE_SMOOTH);              // Enable Smooth Shading
		gl.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );    // Black Background
		gl.glClearDepth( 1.0f );                      // Depth Buffer Setup
		gl.glEnable( GL.GL_DEPTH_TEST );              // Enables Depth Testing
		gl.glDepthFunc( GL.GL_LEQUAL );               // The Type Of Depth Testing To Do
		// Really Nice Perspective Calculations
		//gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		
		gl.glEnable( GL2.GL_BLEND );
		gl.glBlendFunc( GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA );
		
		gl.glEnable( GL2.GL_ALPHA_TEST );
		gl.glAlphaFunc( GL2.GL_GREATER, 0.0f );
		
		// Generate textures.
		gl.glEnable( GL2.GL_TEXTURE_2D );
		texture_loader = new TextureLoader( gl );
		skybox = new Skybox( texture_loader, skybox_name );

		gl.glMatrixMode( GL2.GL_MODELVIEW );
		gl.glLoadIdentity();
		
		// Initialize the keys.
		for ( int i = 0; i < keys.length; ++i )
			keys[i] = false;
		
		// Setup the cameras.
		camera_free = new Camera();
		camera_fp = new Camera();
		camera = camera_free;
		
		// Testing
		car = new TempBuilder( gl );
		try {
			new Parse( car, "models/police_car/model.obj" );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		gl.glGenTextures( 3, track_textures, 0 );
		
		// Load the hackberry tree for billboard.
		try {
			texture_loader.loadTexture( track_textures[ 0 ], "racetrack_textures/hackberry_tree.png" );
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Load the asphalt track texture.
		try {
			texture_loader.loadTexture( track_textures[ 1 ], "racetrack_textures/Asphalt.jpg" );
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void reshape(
						GLAutoDrawable gLDrawable,
						int x,
						int y,
						int width,
						int height ) {
		windowWidth = width;
		windowHeight = height > 0 ? height : 1;
		
		final GL2 gl = gLDrawable.getGL().getGL2();
		
		gl.glViewport( 0, 0, width, height );
		gl.glMatrixMode( GL2.GL_PROJECTION );
		gl.glLoadIdentity();
		glu.gluPerspective( 60.0f, (float) windowWidth / windowHeight, 0.1f, skybox_size / 2.0 * Math.sqrt( 3 ) );
	}

	@Override
	public void display( GLAutoDrawable gLDrawable ) {
		final GL2 gl = gLDrawable.getGL().getGL2();
		
		gl.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );
		
		gl.glMatrixMode( GL2.GL_MODELVIEW );
		gl.glPushMatrix();
		
		final float throttle_pan = 0.25f;
		
		// Move forward/backward.
		if ( keys[ KeyEvent.VK_W ] )
			camera_free.moveForward( throttle_pan );
		else if ( keys[ KeyEvent.VK_S ] )
			camera_free.moveBackward( throttle_pan );
		
		// Move up/down.
		if ( keys[ KeyEvent.VK_R ] )
			camera_free.moveUp( throttle_pan );
		else if ( keys[KeyEvent.VK_F] )
			camera_free.moveDown( throttle_pan );
		
		// Strafe left/right.
		if ( keys[ KeyEvent.VK_A ] )
			camera_free.strafeLeft( throttle_pan );
		else if ( keys[ KeyEvent.VK_D ] )
			camera_free.strafeRight( throttle_pan );
		
		// Update AI position.
		ai_car_t += 0.001f;
		ai_car_x = (float) ( a * Math.cos( ai_car_t ) );
		ai_car_y = (float) ( b * Math.sin( ai_car_t ) );
		float ai_car_dx = (float) ( -a * Math.sin( ai_car_t ) );
		float ai_car_dy = (float) ( b * Math.cos( ai_car_t ) );
		ai_car_theta = (float) Math.atan2( ai_car_dy, ai_car_dx );
		camera_fp.moveTo( ai_car_x, ai_car_y, 3.0 );
		camera_fp.lookTowards( ai_car_dx, ai_car_dy, 0.0 );
		
		camera.look();
		
		gl.glPushMatrix();
		gl.glTranslatef( (float) camera.getEyeX(), (float) camera.getEyeY(), (float) camera.getEyeZ() );
		skybox.draw( gl, skybox_size );
		gl.glPopMatrix();
		
		// Draw a car.
		drawCar( gl, car );
		
		// Draw an AI car.
		gl.glPushMatrix();
		gl.glTranslated( ai_car_x, ai_car_y, 0.0 );
		gl.glRotated( ai_car_theta * 180 / Math.PI, 0.0, 0.0, 1.0 );
		// Initial rotation to straighten the car to point along the track.
		// If we export the car correctly, we won't have to do this.
		gl.glRotated( 180, 0, 0, 1 );
		drawCar( gl, car );
		gl.glPopMatrix();
		
		// Draw some billboard trees.
		for ( int r = 20; r < 50; r += 10 )
			for ( int theta = 0; theta < 360; theta += 30 )
				drawBillboard( gl, r * (float) Math.cos( theta * Math.PI / 180 ), r * (float) Math.sin( theta * Math.PI / 180 ) );
			
		drawCourse( gl );
		
		gl.glPopMatrix();
		
		tire_rotation += 3.0f;
	}
	
	private void drawCar( GL2 gl, TempBuilder car ) {
		int bind_total = 0;
		int current_tex_id = -1;
		for ( Map.Entry<String, ArrayList<Face>> group : car.groups.entrySet() ) {
			ArrayList<Face> faces = group.getValue();
			
			//System.out.println( "Found group with key \"" + group.getKey() + "\"" );
			
			if ( group.getKey().equals( "tires_front" ) || group.getKey().equals( "tires_back" ) ) {
				gl.glPushMatrix();
				
				if ( group.getKey().equals( "tires_front" ) ) {
					gl.glTranslatef( -2.8f, 0.0f, 0.63f );
					gl.glRotatef( tire_rotation, 0.0f, -1.0f, 0.0f );
					gl.glTranslatef( 2.8f, 0.0f, -0.63f );
				} else {
					gl.glTranslatef( 2.74f, 0.0f, 0.63f );
					gl.glRotatef( tire_rotation, 0.0f, -1.0f, 0.0f );
					gl.glTranslatef( -2.74f, 0.0f, -0.63f );
				}
			}
			
			for ( int j = 0; j < faces.size(); ++j ) {
				Face face = faces.get( j );

				if ( -1 != face.material.texid && face.material.texid != current_tex_id ) {
					current_tex_id = face.material.texid;
					//gl.glEnable( GL2.GL_TEXTURE_2D );
					gl.glBindTexture( GL2.GL_TEXTURE_2D, face.material.texid );
					bind_total++;
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
	}
	
	private void drawBillboard( GL2 gl, float x, float y ) {
		gl.glBindTexture( GL.GL_TEXTURE_2D, track_textures[ 0 ] );
		
		gl.glPushMatrix();
		gl.glTranslatef( x, y, 0.0f );
		
		// Get the current modelview matrix.
		float modelview[] = new float[ 16 ];
		gl.glGetFloatv( GL2.GL_MODELVIEW_MATRIX, modelview, 0 );

		// Undo all rotations (scaling is also lost).
		for( int i = 0; i < 3; ++i ) {
			for( int j = 0; j < 3; ++j ) {
				if ( i == j )
					modelview[ i * 4 + j ] = 1.0f;
				else
					modelview[ i * 4 + j ] = 0.0f;
			}
		}

		// Set the modelview matrix with no rotations.
		gl.glLoadMatrixf( modelview, 0 );
		
		// Draw the billboard tree.
		gl.glBegin( GL2.GL_QUADS );
		
		gl.glTexCoord2f( 0, 1 - ( 620.0f / 1024 ) );
		gl.glVertex3f( -3.0f, 0.0f, 0.0f );
		
		gl.glTexCoord2f( 1, 1 - ( 620.0f / 1024 ) );
		gl.glVertex3f( 3.0f, 0.0f, 0.0f );
		
		gl.glTexCoord2f( 1, 1 );
		gl.glVertex3f( 3.0f, 7.265625f,  0.0f );
		
		gl.glTexCoord2f( 0, 1 );
		gl.glVertex3f( -3.0f, 7.265625f,  0.0f );
		
		gl.glEnd();
		
		gl.glPopMatrix();

		gl.glBindTexture( GL2.GL_TEXTURE_2D, 0 );
	}
	
	private void drawCourse( GL2 gl ) {
		float d = 300.0f;
		
		/*
		gl.glDisable( GL2.GL_TEXTURE_2D );
		gl.glColor3f( 1.0f, 1.0f, 1.0f );
		gl.glBegin( GL2.GL_QUADS );
		gl.glVertex3f( -d, -d, 0.0f );
		gl.glVertex3f( -d, d, 0.0f );
		gl.glVertex3f( d, d, 0.0f );
		gl.glVertex3f( d, -d, 0.0f );
		gl.glEnd();
		gl.glEnable( GL2.GL_TEXTURE_2D );
		*/
		gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[1]);
		gl.glBegin( GL2.GL_TRIANGLE_STRIP);
		//gl.glColor4f(0, 0, 0, 1);
		
		
		for (int i = 0; i < 360; i++){
			double x = (a - 25) * Math.cos(i/180.0 * Math.PI);
			double y = (b - 25) * Math.sin(i/180.0 * Math.PI);
			gl.glTexCoord2d(0, 0);
			gl.glVertex3d(x, y, 0.0);
			
			x = (a + 25) * Math.cos(i/180.0 * Math.PI);
			y = (b + 25) * Math.sin(i/180.0 * Math.PI);
			gl.glTexCoord2d(1, 0);
			gl.glVertex3d(x, y, 0.0);
			
			x = (a - 25) * Math.cos((i+1)/180.0 * Math.PI);
			y = (b - 25) * Math.sin((i+1)/180.0 * Math.PI);
			gl.glTexCoord2d(1, 1);
			gl.glVertex3d(x, y, 0.0);
			
			x = (a + 25) * Math.cos((i+1)/180.0 * Math.PI);
			y = (b + 25) * Math.sin((i+1)/180.0 * Math.PI);
			gl.glTexCoord2d(0, 1);
			gl.glVertex3d(x, y, 0.0);
		}
		gl.glEnd();
		
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	}
	
	@Override
	public void dispose( GLAutoDrawable arg0 ) {
	}

	@Override
	public void keyTyped( KeyEvent e ) {
		switch ( e.getKeyChar() ) {
			// Switch to free view camera.
			case KeyEvent.VK_1:
				if ( camera != camera_free ) {
					camera_free.moveTo( camera_fp.getEyeX(), camera_fp.getEyeY(), camera_fp.getEyeZ() );
					camera_free.lookTowards( camera_fp.getLookX(), camera_fp.getLookY(), camera_fp.getLookZ() );
					camera = camera_free;
				}
				break;
			
			// Switch to fps camera.
			case KeyEvent.VK_2:
				if ( camera != camera_fp ) {
					camera = camera_fp;
				}
				break;
		}
	}

	@Override
	public void keyPressed( KeyEvent e ) {
		keys[e.getKeyCode()] = true;
	}

	@Override
	public void keyReleased( KeyEvent e ) {
		keys[e.getKeyCode()] = false;
	}

	@Override
	public void mouseDragged( MouseEvent e ) {
		int x = e.getX();
		int y = e.getY();
		
		final float throttle_rot = 0.01f;
		
		float dx = ( x - mouse_x0 );
		float dy = ( y - mouse_y0 );
		
		if ( MOUSE_MODE_ROTATE == mouse_mode )
			camera_free.rotate( dx * throttle_rot, dy * throttle_rot );
		
		mouse_x0 = x;
		mouse_y0 = y;
	}
	
	@Override
	public void mouseMoved( MouseEvent e ) {
	}

	@Override
	public void mouseClicked( MouseEvent e ) {
	}

	@Override
	public void mousePressed( MouseEvent e ) {
		mouse_x0 = e.getX();
		mouse_y0 = e.getY();
		
		if ( MouseEvent.BUTTON1 == e.getButton() ) {
			mouse_mode = MOUSE_MODE_ROTATE;
		} else {
			mouse_mode = MOUSE_MODE_NONE;
		}
	}

	@Override
	public void mouseReleased( MouseEvent e ) {
	}

	@Override
	public void mouseEntered( MouseEvent e ) {
	}

	@Override
	public void mouseExited( MouseEvent e ) {
	}
}