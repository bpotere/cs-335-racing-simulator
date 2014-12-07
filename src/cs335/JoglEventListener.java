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
	private final float skybox_size = 1000.0f;
	
	private final int[] track_textures = new int[3]; // Asphalt etc. goes here
	
	//The major and minor axis of the racetrack ellipse:
	private double a = 625;
	private double b = 250;
	
	private Camera camera = null;
	
	// Testing
	private TempBuilder car = null;
	
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
		
		camera = new Camera();
		
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
		glu.gluPerspective( 60.0f, (float) windowWidth / windowHeight, 0.1f, 2000.0f );
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
			camera.moveForward( throttle_pan );
		else if ( keys[ KeyEvent.VK_S ] )
			camera.moveBackward( throttle_pan );
		
		// Move up/down.
		if ( keys[ KeyEvent.VK_R ] )
			camera.moveUp( throttle_pan );
		else if ( keys[KeyEvent.VK_F] )
			camera.moveDown( throttle_pan );
		
		// Strafe left/right.
		if ( keys[ KeyEvent.VK_A ] )
			camera.strafeLeft( throttle_pan );
		else if ( keys[ KeyEvent.VK_D ] )
			camera.strafeRight( throttle_pan );
		
		camera.look();
		
		gl.glPushMatrix();
		gl.glTranslatef( (float) camera.getEyeX(), (float) camera.getEyeY(), (float) camera.getEyeZ() );
		skybox.draw( gl, skybox_size );
		gl.glPopMatrix();
		
		for ( int i = 0; i < 5; ++i ) {
			gl.glTranslatef( 0.0f, 5.0f, 0.0f );
			drawCar( gl, car );
		}
		
		// Draw a single billboard tree
		drawBillboard(gl, 0, -1);
		drawBillboard(gl, -1, 0);
		drawBillboard(gl, -5, 0);
		drawCourse(gl);
		
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
	
	private void drawBillboard( GL2 gl, int i, int j ) {
		float pos[]=new float[3],right[]=new float[3],up[]=new float[3];

		gl.glBindTexture( GL.GL_TEXTURE_2D, track_textures[0] );
		
		gl.glPushMatrix();
		gl.glTranslatef((float)(5+i*10.0f),(float)(5+j * 10.0f), 0.f);
		
		pos[0] = (float)(5+i*10.0); pos[1] = 0; pos[2] = (float)(5+j * 10.0);
		
		float modelview[]=new float[16];
		int i1,j1;

		// save the current modelview matrix
		gl.glPushMatrix();

		// get the current modelview matrix
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX , modelview,0);

		// undo all rotations
		// beware all scaling is lost as well 
		for( i1=0; i1<3; i1++ ) 
			for( j1=0; j1<3; j1++ ) {
				if ( i1==j1 )
					modelview[i1*4+j1] = 1.0f;
				else
					modelview[i1*4+j1] = 0.0f;
			}

		// set the modelview with no rotations
		gl.glLoadMatrixf(modelview,0);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0,1-(620.0f/1024));gl.glVertex3f(-3.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(1,1-(620.0f/1024));gl.glVertex3f(3.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(1,1);gl.glVertex3f(3.0f, 7.265625f,  0.0f);
		gl.glTexCoord2f(0,1);gl.glVertex3f(-3.0f, 7.265625f,  0.0f);
		gl.glEnd();
		gl.glPopMatrix();
		
		gl.glPopMatrix();

		gl.glBindTexture(GL.GL_TEXTURE_2D,0);


		gl.glColor3f(0.0f,1.0f,1.0f);


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
			camera.rotate( dx * throttle_rot, dy * throttle_rot );
		
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