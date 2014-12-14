package cs335;

import java.awt.Color;
import java.awt.Font;
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

import com.jogamp.opengl.util.awt.TextRenderer;
import com.owens.oobjloader.builder.Build;
import com.owens.oobjloader.builder.Face;
import com.owens.oobjloader.builder.FaceVertex;
import com.owens.oobjloader.builder.VertexGeometric;
import com.owens.oobjloader.builder.VertexTexture;
import com.owens.oobjloader.parser.Parse;

public class JoglEventListener implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {
	private int windowWidth, windowHeight;
	
	private String skybox_name = "FullMoon";
	private Skybox skybox = null;
	private TextureLoader texture_loader = null;
	private final float skybox_size = 2000.0f;
	
	private final int[] track_textures = new int[32]; // Asphalt etc. goes here
	
	//The major and minor axis of the racetrack ellipse:
	public static final double a = 625; // Production value: 625
	public static final double b = 250; // Production value: 250
	public static final double c = Math.sqrt(Math.pow(a, 2) - Math.pow(b, 2));
	public static final double camber_theta = 9.0/180.0 * Math.PI;
	public static final double t_width = 50;
	
	private long user_track_time;
	
	// Should we animate?
	private boolean should_animate = true;
	
	// Random stuff
	Random random = new Random();
	
	// Multiple cameras. The generic "camera" always points to whatever
	// one is in use.
	private Camera camera_fp = null;
	private Camera camera_free = null;
	private Camera camera = null;
	
	// Render text.
	TextRenderer text_renderer = null;
	
	// Create some AI cars.
	private Car[] ai_cars = new Car[ 4 ];
	
	private Car user_car = null;
	
	// Who is the winner? -1 for none, 0 for user, 1+ for ai car
	private int winner_declared = -1;
	
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
		gl.glAlphaFunc( GL2.GL_GREATER, 0.5f );
		
		// Generate textures.
		gl.glEnable( GL2.GL_TEXTURE_2D );
		texture_loader = new TextureLoader( gl );
		skybox = new Skybox( new TextureLoaderClamping( gl ), skybox_name );

		gl.glMatrixMode( GL2.GL_MODELVIEW );
		gl.glLoadIdentity();
		
		// Initialize the keys.
		for ( int i = 0; i < keys.length; ++i )
			keys[i] = false;
		
		// Allow text drawing.
		text_renderer = new TextRenderer( new Font( Font.SANS_SERIF, Font.BOLD, 12 ) );
		
		// Setup the cameras.
		camera_free = new Camera();
		camera_fp = new Camera();
		camera = camera_fp;
		
		camera_free.moveTo(-10, 0, 6);
		
		// Init our cars.
		user_car = new Car( gl );
		
		for ( int i = 0; i < ai_cars.length; ++i ) {
			ai_cars[ i ] = new Car( gl );
			ai_cars[ i ].setT( ( i + 1 ) * 0.05 );
		}
		
		gl.glGenTextures( track_textures.length, track_textures, 0 );
		
		// Load the hackberry tree for billboard.
		try {
			texture_loader.loadTexture( track_textures[ 0 ], "racetrack_textures/hackberry_tree.png" );
		} catch ( Exception e ) {
		
			e.printStackTrace();
		}
		
		// Load the asphalt track texture.
		try {
			texture_loader.loadTexture( track_textures[ 1 ], "racetrack_textures/Asphalt.jpg" );
		} catch ( Exception e ) {
			
			e.printStackTrace();
		}
		// Load the kerb track texture.
		try {
			texture_loader.loadTexture( track_textures[ 2 ], "racetrack_textures/kerb.jpg" );
		} catch ( Exception e ) {
			
			e.printStackTrace();
		}
		// Load the gravel texture
		try {
			texture_loader.loadTexture( track_textures[ 3 ], "racetrack_textures/gravel_seamless.jpg" );
		} catch ( Exception e ) {
			
			e.printStackTrace();
		}
		// Load the grass texture
		try {
			texture_loader.loadTexture( track_textures[ 4 ], "racetrack_textures/grass_seamless.jpg" );
		} catch ( Exception e ) {
			
			e.printStackTrace();
		}
		// Load the concrete texture
		try {
			texture_loader.loadTexture( track_textures[ 5 ], "racetrack_textures/concrete_seamless.jpg" );
		} catch ( Exception e ) {
			
			e.printStackTrace();
		}
		
		// Load a testing up arrow
		try {
			texture_loader.loadTexture( track_textures[ 6 ], "racetrack_textures/scoreboard.jpg" );
		} catch ( Exception e ) {
			
			e.printStackTrace();
		}
		// Garage textures
		// Load a ceramic roof tile
		try {
			texture_loader.loadTexture( track_textures[ 7 ], "racetrack_textures/ceramic_roof_tile.jpg" );
		} catch ( Exception e ) {
			
			e.printStackTrace();
		}
		// Load a garage door
		try {
			texture_loader.loadTexture( track_textures[ 8 ], "racetrack_textures/garage_door.jpg" );
		} catch ( Exception e ) {
					
			e.printStackTrace();
		}
		// Load a white stone concrete
		try {
			texture_loader.loadTexture( track_textures[ 9 ], "racetrack_textures/white_stone.jpg" );
		} catch ( Exception e ) {
					
			e.printStackTrace();
		}
		
		// Load a green up arrow
		try {
			texture_loader.loadTexture( track_textures[ 10 ], "racetrack_textures/UpArrow_Green.jpg" );
		} catch ( Exception e ) {
						
			e.printStackTrace();
		}
		
		// Load a speaker
		try {
			texture_loader.loadTexture( track_textures[ 11 ], "racetrack_textures/speaker.jpg" );
		} catch ( Exception e ) {
						
			e.printStackTrace();
		}
		try {
			texture_loader.loadTexture( track_textures[ 12 ], "racetrack_textures/scoreboard_1.jpg" );
		} catch ( Exception e ) {
						
			e.printStackTrace();
		}
		try {
			texture_loader.loadTexture( track_textures[ 13 ], "racetrack_textures/scoreboard_2.jpg" );
		} catch ( Exception e ) {
						
			e.printStackTrace();
		}
		try {
			texture_loader.loadTexture( track_textures[ 14 ], "racetrack_textures/scoreboard_3.jpg" );
		} catch ( Exception e ) {
						
			e.printStackTrace();
		}
		try {
			texture_loader.loadTexture( track_textures[ 15 ], "racetrack_textures/checkers.jpg" );
		} catch ( Exception e ) {
						
			e.printStackTrace();
		}

		//Start the track time counter.
		user_track_time = System.currentTimeMillis();
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
	
	private void updateEvents( GL2 gl ) {
		final float throttle_pan = 1.0f;
		
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
		
		// Now that we're done with the camera stuff, we should bail if
		// animations are off.
		if ( ! should_animate )
			return;
		
		// Move the car left/right.
		if ( keys[ KeyEvent.VK_LEFT ] )
			user_car.swayLeft();
		else if ( keys[ KeyEvent.VK_RIGHT ] )
			user_car.swayRight();
		
		// Accelerate/brake
		if ( keys[ KeyEvent.VK_UP ] )
			user_car.accelerate();
		else if ( keys[ KeyEvent.VK_DOWN ] )
			user_car.brake();
		
		// Update cars.
		user_car.update();
		for ( int i = 0; i < ai_cars.length; ++i ) {
			// Check collision with ai cars.
			user_car.revertOnCollision( ai_cars[ i ] );
			
			if ( random.nextInt( 100 ) < 1 ) {
				if ( random.nextInt( 100 ) < 50 )
					ai_cars[ i ].swayFluid( -random.nextFloat() * 10 );
				else
					ai_cars[ i ].swayFluid( random.nextFloat() * 10 );
			}
			if ( ai_cars[ i ].getVelocityT() < Car.INITIAL_V_T || random.nextInt( 100 ) < 10 ) {
				if ( random.nextInt( 100 ) < 90 )
					ai_cars[ i ].accelerate();
				else
					ai_cars[ i ].brake();
			}
			ai_cars[ i ].update();
			
			// Check collision with all other cars.
			ai_cars[ i ].revertOnCollision( user_car );
			for ( int j = 0; j < ai_cars.length; ++j ) {
				if ( i == j ) continue;
				ai_cars[ i ].revertOnCollision( ai_cars[ j ] );
			}
		}
		
		// End of loop detection
		if ( winner_declared < 0 ) {
			int highest_t_car = 0;
			double highest_t = user_car.getT();
			
			for ( int i = 0; i < ai_cars.length; ++i ) {
				if ( ai_cars[ i ].getT() > highest_t ) {
					highest_t = ai_cars[ i ].getT();
					highest_t_car = i + 1;
				}
			}
			
			if ( highest_t > 13 * Math.PI / 2.0 )
				winner_declared = highest_t_car;
		}
	}

	@Override
	public void display( GLAutoDrawable gLDrawable ) {
		final GL2 gl = gLDrawable.getGL().getGL2();
		
		gl.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );
		
		gl.glMatrixMode( GL2.GL_MODELVIEW );
		gl.glPushMatrix();

		updateEvents( gl );
		
		// Hacky camera adjustment for z-coord/up to avoid having to mess with modelview.
		Vector3 dir_to_track = user_car.getVelocity().getOrthogonal().getNormalized();
		camera_fp.setUp( dir_to_track.x, dir_to_track.y, dir_to_track.getNorm() * Math.tan( Math.PI / 2.0 - camber_theta ) );
		camera_fp.moveTo( user_car.getPosition().x + dir_to_track.x * 1.5, user_car.getPosition().y + dir_to_track.y * 1.5, 2.2 + ( t_width / 2.0 - user_car.getSway() ) * Math.sin(camber_theta) );
		camera_fp.lookTowards( user_car.getVelocity().x, user_car.getVelocity().y, 0.0 );
		camera.look();
		
		gl.glPushMatrix();
		gl.glTranslatef( (float) camera.getEyeX(), (float) camera.getEyeY(), (float) camera.getEyeZ() );
		skybox.draw( gl, skybox_size );
		gl.glPopMatrix();
		
		// Draw cars.
		//gl.glPushMatrix();
		user_car.draw( gl );
		for ( int i = 0; i < ai_cars.length; ++i )
			ai_cars[ i ].draw( gl );
		//gl.glPopMatrix();
		
		// Draw some billboard trees.
		for ( int r = 20; r < 50; r += 10 )
			for ( int theta = 0; theta < 360; theta += 30 )
				drawBillboard( gl, r * (float) Math.cos( theta * Math.PI / 180 ), r * (float) Math.sin( theta * Math.PI / 180 ) );
		
		gl.glPushMatrix();
		drawCourse( gl );
		gl.glPopMatrix();
		
		//Draw two scoring pylons, put them at the focal points of the ellipse
		gl.glPushMatrix();
		gl.glTranslated(c, 0, 0);
		drawScorePylon(gl);
		gl.glPopMatrix();
		
		
		gl.glPushMatrix();
		gl.glTranslated(-c, 0, 0);
		drawScorePylon(gl);
		gl.glPopMatrix();
		
		gl.glPushMatrix();
		gl.glTranslated(0, c/4.0, 0);
		drawGarage(gl);
		gl.glPopMatrix();
		
		gl.glPushMatrix();
		gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[ 15 ]);
		gl.glBegin(GL2.GL_QUADS);
		double x1 = (a + t_width/2.0) * Math.cos(Math.PI/2.0);
		double y1 = (b + t_width/2.0) * Math.sin(Math.PI/2.0);
		double z = t_width * Math.sin(camber_theta);
		gl.glTexCoord2d(0, 0);
		gl.glVertex3d(x1, y1, z);
		
		x1 = (a + t_width/2.0) * Math.cos((1)/180.0 * Math.PI + Math.PI/2.0);
		y1 = (b + t_width/2.0) * Math.sin((1)/180.0 * Math.PI + Math.PI/2.0);
		gl.glTexCoord2d(1, 0);
		gl.glVertex3d(x1, y1, z);
		
		x1 = (a - t_width/2.0) * Math.cos((1)/180.0 * Math.PI + Math.PI/2.0);
		y1 = (b - t_width/2.0) * Math.sin((1)/180.0 * Math.PI + Math.PI/2.0);
		gl.glTexCoord2d(1, 1);
		gl.glVertex3d(x1, y1, 0);
		
		x1 = (a - t_width/2.0) * Math.cos(Math.PI/2.0);
		y1 = (b - t_width/2.0) * Math.sin(Math.PI/2.0);
		gl.glTexCoord2d(0, 1);
		gl.glVertex3d(x1, y1, 0);
		
		//gl.glTexCoord2d(0.0f, 1.0f);		 gl.glVertex3d(0, 0, 1); 
		//gl.glTexCoord2d(1.0f, 1.0f);		 gl.glVertex3d(1, 0, 1);
		//gl.glTexCoord2d(1.0f, 0.0f);		 gl.glVertex3d(1, 1, 1); 
		//gl.glTexCoord2d(0.0f, 0.0f);		 gl.glVertex3d(0, 1, 1);
		
		gl.glEnd();
		
		gl.glPopMatrix();
		
		gl.glPopMatrix();
		
		
		// LASTLY (VERY IMPORTANT): draw the lap times text. Do this earlier, and
		// it will be obscured by things drawn after it.
		text_renderer.beginRendering( windowWidth, windowHeight );
		text_renderer.setColor( Color.WHITE );
		text_renderer.draw( "Time: " + ( ( System.currentTimeMillis() - user_track_time ) / 1000.0f ) + " s", 32, windowHeight - 32 );
		
		// Print the winner message if one has been declared.
		if ( 0 == winner_declared ) {
			text_renderer.setColor( Color.GREEN );
			text_renderer.draw( "YOU ARE THE WINNER! ROCK ON!", 32, windowHeight - 48 );
		} else if ( winner_declared > 0 ) {
			text_renderer.setColor( Color.RED );
			text_renderer.draw( "YOU FAILED! YOUR LIFE IS MEANINGLESS!", 32, windowHeight - 48 );
		}
		
		text_renderer.endRendering();
		
		gl.glPopMatrix();
	}
	
	private void drawGarage( GL2 gl){
		
		
		int block_size = 12;
		
		for (int i = -10; i < 10; i = i + 2){
			gl.glBindTexture( GL.GL_TEXTURE_2D, track_textures[ 7 ] ); //roof
			gl.glPushMatrix();
			gl.glTranslated(i * block_size, block_size, block_size);
			gl.glScaled(4 * block_size, 4 * block_size, 1);
			drawCube(gl);
			gl.glPopMatrix();
			
			gl.glBindTexture( GL.GL_TEXTURE_2D, track_textures[ 8 ] ); //garage door
			gl.glPushMatrix();
			gl.glTranslated(i * block_size, 0, 0);
			gl.glScaled(block_size, block_size, block_size);
			drawCube(gl);
			gl.glPopMatrix();
			
			gl.glPushMatrix();
			gl.glTranslated(i * block_size, block_size, 0);
			gl.glScaled(block_size, block_size, block_size);
			drawCube(gl);
			gl.glPopMatrix();
			
			gl.glBindTexture( GL.GL_TEXTURE_2D, track_textures[ 9 ] ); //white stone
			gl.glPushMatrix();
			gl.glTranslated((i + 1) * block_size, 0, 0);
			gl.glScaled(block_size, block_size, block_size);
			drawCube(gl);
			gl.glPopMatrix();
			
			gl.glPushMatrix();
			gl.glTranslated((i + 1) * block_size, block_size, 0);
			gl.glScaled(block_size, block_size, block_size);
			drawCube(gl);
			gl.glPopMatrix();
		}
		
		
		gl.glBindTexture( GL2.GL_TEXTURE_2D, 0 );
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
		
		gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[4]);
		//Draw an inner grass area
		gl.glBegin( GL2.GL_TRIANGLE_FAN);
		gl.glTexCoord2d(0, 0);
		gl.glVertex3d(0.0, 0.0, 0.0);
		for (int i = 0; i <= 360; i = i + 30){
			double x = 128 * Math.cos(i/180.0 * Math.PI);
			double y = 128 * Math.sin(i/180.0 * Math.PI);
			gl.glTexCoord2d(1, 0);
			gl.glVertex3d(x, y, 0.0);
			
			x = 128 * Math.cos(i/180.0 * Math.PI);
			y = 128 * Math.sin(i/180.0 * Math.PI);
			gl.glTexCoord2d(0, 1);
			gl.glVertex3d(x, y, 0.0);
		}
		gl.glEnd();
		
		//Draws the inner gravel
		gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[3]);
		gl.glBegin( GL2.GL_TRIANGLE_FAN);
		gl.glTexCoord2d(0, 0);
		gl.glVertex3d(0.0, 0.0, -0.05);
		for (int i = 0; i <= 360; i = i + 30){
			double x = 1024 * Math.cos(i/180.0 * Math.PI);
			double y = 1024 * Math.sin(i/180.0 * Math.PI);
			gl.glTexCoord2d(64, 0);
			gl.glVertex3d(x, y, -0.05);
			
			x = 1024 * Math.cos(i/180.0 * Math.PI);
			y = 1024 * Math.sin(i/180.0 * Math.PI);
			gl.glTexCoord2d(0, 64);
			gl.glVertex3d(x, y, -0.05);
		}
		gl.glEnd();
		
		
		//Draws the inner kerb
		gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[2]);
		gl.glBegin( GL2.GL_QUAD_STRIP);
		for (int i = 0; i < 360; i++){
			double x = (a - 30) * Math.cos(i/180.0 * Math.PI);
			double y = (b - 30) * Math.sin(i/180.0 * Math.PI);
			gl.glTexCoord2d(0, 0);
			gl.glVertex3d(x, y, 0.0);
			
			x = (a - 25) * Math.cos(i/180.0 * Math.PI);
			y = (b - 25) * Math.sin(i/180.0 * Math.PI);
			gl.glTexCoord2d(1, 0);
			gl.glVertex3d(x, y, 0.0);
			
			x = (a - 30) * Math.cos((i+1)/180.0 * Math.PI);
			y = (b - 30) * Math.sin((i+1)/180.0 * Math.PI);
			gl.glTexCoord2d(0, 1);
			gl.glVertex3d(x, y, 0.0);
			
			x = (a - 25) * Math.cos((i+1)/180.0 * Math.PI);
			y = (b - 25) * Math.sin((i+1)/180.0 * Math.PI);
			gl.glTexCoord2d(1, 1);
			gl.glVertex3d(x, y, 0.0);
		}
		gl.glEnd();
		
		//Draws the main course
		gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[1]);
		gl.glBegin( GL2.GL_QUAD_STRIP);
		for (int i = 0; i < 360; i++){
			//double theta = 9/180.0 * Math.PI;	//The angle of the course section
			//double t_width = 50;				//50 feet wide for the cars
			double x = (a - t_width/2.0) * Math.cos(i/180.0 * Math.PI);
			double y = (b - t_width/2.0) * Math.sin(i/180.0 * Math.PI);
			double z = t_width * Math.sin(camber_theta);
			gl.glTexCoord2d(0, 0);
			gl.glVertex3d(x, y, 0);
			
			x = (a + t_width/2.0) * Math.cos(i/180.0 * Math.PI);
			y = (b + t_width/2.0) * Math.sin(i/180.0 * Math.PI);
			gl.glTexCoord2d(1, 0);
			gl.glVertex3d(x, y, z);
			
			x = (a - t_width/2.0) * Math.cos((i+1)/180.0 * Math.PI);
			y = (b - t_width/2.0) * Math.sin((i+1)/180.0 * Math.PI);
			gl.glTexCoord2d(0, 1);
			gl.glVertex3d(x, y, 0);
			
			x = (a + t_width/2.0) * Math.cos((i+1)/180.0 * Math.PI);
			y = (b + t_width/2.0) * Math.sin((i+1)/180.0 * Math.PI);
			gl.glTexCoord2d(1, 1);
			gl.glVertex3d(x, y, z);
		}
		gl.glEnd();
		
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	}
	
	public void drawCube( final GL2 gl ){
		 
		 gl.glTranslated(-0.5, -0.5, 0);
		 gl.glBegin(GL2.GL_QUADS);
		
		 // on the XY plane
		 // front plane -- top
		 gl.glNormal3d(0,  0, 1);
		 //gl.glColor3d(1, 0, 0);
		 gl.glTexCoord2d(0.0f, 1.0f);		 gl.glVertex3d(0, 0, 1); 
		 gl.glTexCoord2d(1.0f, 1.0f);		 gl.glVertex3d(1, 0, 1);
		 gl.glTexCoord2d(1.0f, 0.0f);		 gl.glVertex3d(1, 1, 1); 
		 gl.glTexCoord2d(0.0f, 0.0f);		 gl.glVertex3d(0, 1, 1);
		
		 // back plane -- bottom
		 gl.glNormal3d(0,  0, -1);
		 //gl.glColor3d(1, 0, 0);
		 gl.glTexCoord2d(0f, 1f);gl.glVertex3d(0, 0, 0); 
		 gl.glTexCoord2d(1f, 1f);gl.glVertex3d(1, 0, 0);
		 gl.glTexCoord2d(1f, 0f);gl.glVertex3d(1, 1, 0); 
		 gl.glTexCoord2d(0f, 0f);gl.glVertex3d(0, 1, 0);
		 
		 // on the YZ plane
		 // left plane 
		 gl.glNormal3f(-1,  0, 0);
		 //gl.glColor3f(0, 1, 0);
		 gl.glTexCoord2f(1f, 0f);gl.glVertex3d(0, 0, 0); 
		 gl.glTexCoord2f(0f, 0f);gl.glVertex3d(0, 1, 0);
		 gl.glTexCoord2f(0f, 1f);gl.glVertex3d(0, 1, 1); 
		 gl.glTexCoord2f(1f, 1f);gl.glVertex3d(0, 0, 1);
		 
		 
		 // right plane
		 gl.glNormal3f(1,  0, 0);
		 //gl.glColor3f(0, 1, 0);
		 gl.glTexCoord2f(0f, 0f);gl.glVertex3d(1, 0, 0); 
		 gl.glTexCoord2f(1f, 0f);gl.glVertex3d(1, 1, 0);
		 gl.glTexCoord2f(1f, 1f);gl.glVertex3d(1, 1, 1); 
		 gl.glTexCoord2f(0f, 1f);gl.glVertex3d(1, 0, 1);
		 
		 
		 // on the XZ plane,  
		 // up plane;  -- front
		 gl.glNormal3f(0,  1, 0);
		 //gl.glColor3f(0, 0, 1);
		 gl.glTexCoord2f(1f, 0f);gl.glVertex3d(0, 1, 0); 
		 gl.glTexCoord2f(0f, 0f);gl.glVertex3d(1, 1, 0);
		 gl.glTexCoord2f(0f, 1f);gl.glVertex3d(1, 1, 1); 
		 gl.glTexCoord2f(1f, 1f);gl.glVertex3d(0, 1, 1);
		 
		 // down plane; -- back
		 gl.glNormal3f(0,  -1, 0);
		 //gl.glColor3f(0, 0, 1);
		 gl.glTexCoord2f(0f, 0f);gl.glVertex3d(0, 0, 0); 
		 gl.glTexCoord2f(1f, 0f);gl.glVertex3d(1, 0, 0);
		 gl.glTexCoord2f(1f, 1f);gl.glVertex3d(1, 0, 1); 
		 gl.glTexCoord2f(0f, 1f);gl.glVertex3d(0, 0, 1);
		
		 gl.glEnd(); 
	}
	
	public void drawScorePylon( GL2 gl){
		
		// Lower Pole
		gl.glBindTexture( GL.GL_TEXTURE_2D, track_textures[ 5 ] );
		gl.glPushMatrix();
		gl.glScaled(1, 1, 64);
		drawCube(gl);
		gl.glPopMatrix();
		
		// This part of the pole has writing on the real one
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		gl.glPushMatrix();
		gl.glColor4d(0, 0, 0, 1);
		gl.glTranslated(0, 0, 64);
		gl.glScaled(4, 4, 64);
		drawCube(gl);
		gl.glPopMatrix();
		
		//Brace
		gl.glPushMatrix();
		gl.glTranslated(0, 0, 126);
		gl.glScaled(24, 4, 4);
		drawCube(gl);
		gl.glPopMatrix();
		
		//The main screen -- needs a texture
		if (user_car.getT() < Math.PI/2.0){
			gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[ 6 ]);	//Blank lap time
		}
		else if (Math.PI/2.0 <= user_car.getT() && user_car.getT() < 5*Math.PI/2.0){
			gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[ 12 ]);	//Lap 1
		}
		else if (5*Math.PI/2.0 <= user_car.getT() && user_car.getT() < 9*Math.PI/2.0){
			gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[ 13 ]);	//Lap 2
		}
		else if (9*Math.PI/2.0 <= user_car.getT()){
			gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[ 14 ]);	//Lap 3
		}
		gl.glPushMatrix();
		gl.glColor4d(0, 0, 0, 1);
		gl.glTranslated(0, 0, 128);
		gl.glScaled(32, 32, 32);
		drawCube(gl);
		gl.glPopMatrix();
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		
		//Speakers up top -- needs a texture

		gl.glBindTexture(GL.GL_TEXTURE_2D, track_textures[ 11 ]); //speaker
		for ( int theta = 0; theta < 360; theta += 30 ){
			gl.glPushMatrix();
			double x = 16 * Math.cos(theta);
			double y = 16 * Math.sin(theta);
			gl.glTranslated(x, y, 160);
			gl.glScaled(4, 4, 4);
			drawCube(gl);
			gl.glPopMatrix();
		}
		
	}
	
	@Override
	public void dispose( GLAutoDrawable arg0 ) {
	}

	@Override
	public void keyTyped( KeyEvent e ) {
		switch ( e.getKeyChar() ) {
			// Toggle animations.
			case KeyEvent.VK_0:
				should_animate = ! should_animate;
				break;
				
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