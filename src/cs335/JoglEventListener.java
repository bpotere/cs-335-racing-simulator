package cs335;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
	
	private String skybox = "ThickCloudsWater";
	private String[] skybox_suffix = {
		"Front2048", "Right2048", "Back2048", "Left2048", "Up2048", "Down2048"
	};
	
	private int[] textures = new int[6];
	
	// Testing
	TempBuilder car;
	
	private final float frame_step = 0.01f;
	
	private float scene_eye_x = 0.0f;
	private float scene_eye_y = 0.0f;
	private float scene_eye_z = 0.0f;
	private float scene_look_x = 1.0f;
	private float scene_look_y = 0.0f;
	private float scene_look_z = 0.0f;
	
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
		gl.glAlphaFunc( GL2.GL_EQUAL, 1.0f );
		
		// Generate textures.
		gl.glEnable( GL2.GL_TEXTURE_2D );
		gl.glGenTextures( textures.length, textures, 0 );
		
		for ( int i = 0; i < 6; ++i )
			generateTexture( gl, i, "skybox/" + skybox + "/" + skybox + skybox_suffix[ i ] + ".png" );
		
		gl.glMatrixMode( GL2.GL_MODELVIEW );
		gl.glLoadIdentity();
		
		// Initialize the keys.
		for ( int i = 0; i < keys.length; ++i )
			keys[i] = false;
		
		// Testing
		car = new TempBuilder( gl );
		try {
			//new Parse( car, "Lamborghini-Aventador/lambonew.obj" );
			new Parse( car, "models/police_car/model.obj" );
		} catch (IOException e) {
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
		
		// Update the camera state.
		if ( keys[KeyEvent.VK_W] || keys[KeyEvent.VK_S] ) {
			float normxy = (float) Math.sqrt( scene_look_x * scene_look_x + scene_look_y * scene_look_y );
			float multiplier = keys[KeyEvent.VK_W] ? 1.0f : -1.0f;
			scene_eye_x += scene_look_x / normxy * 0.1f * multiplier;
			scene_eye_y += scene_look_y / normxy * 0.1f * multiplier;
			//scene_eye_z += scene_look_z * 0.1f;
		}
		
		if ( keys[KeyEvent.VK_R] ) {
			scene_eye_z += 0.1f;
		} else if ( keys[KeyEvent.VK_F] ) {
			scene_eye_z -= 0.1f;
		}
		
		if ( keys[KeyEvent.VK_A] || keys[KeyEvent.VK_D] ) {
			float theta = (float) Math.atan2( scene_look_y, scene_look_x );
			float phi = (float) Math.acos( scene_look_z );
			
			if ( keys[KeyEvent.VK_A] )
				theta += Math.PI / 2.0;
			else if ( keys[KeyEvent.VK_D] )
				theta -= Math.PI / 2.0;
			
			float strafe_x = (float)( Math.cos( theta ) * Math.sin( phi ) );
			float strafe_y = (float)( Math.sin( theta ) * Math.sin( phi ) );
			float normxy = (float) Math.sqrt( strafe_x * strafe_x + strafe_y * strafe_y );
			
			scene_eye_x += strafe_x / normxy * 0.1f;
			scene_eye_y += strafe_y / normxy * 0.1f;
		}
		
		glu.gluLookAt( scene_eye_x, scene_eye_y, scene_eye_z,
				scene_eye_x + scene_look_x, scene_eye_y + scene_look_y, scene_eye_z + scene_look_z,
				0.0f, 0.0f, 1.0f );
		
		drawSkybox( gl );
		//drawCourse( gl );
		drawCar( gl );
		
		gl.glPopMatrix();
	}
	
	private void generateTexture( GL2 gl, int texid, String filename ) {
		try {
			BufferedImage img = ImageIO.read( new File( filename ) );
			byte[] data = ( (DataBufferByte) img.getRaster().getDataBuffer() ).getData();
			
			gl.glBindTexture( GL2.GL_TEXTURE_2D, textures[texid] );
			gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR );
			gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR );
			gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE );
			gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE );
			gl.glTexEnvf( GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE );
			gl.glTexImage2D( GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB, img.getWidth(),
					img.getHeight(), 0, GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, ByteBuffer.wrap( data ) );
		} catch ( IOException e ) {
			System.out.println( e.getMessage() );
		}
	}
	
	private void drawCar( GL2 gl ) {
		//car.draw( gl );
		//gl.glDisable( GL2.GL_TEXTURE_2D );
		//System.out.println( "BEGIN" );
		for ( int i = 0; i < car.faces.size(); ++i ) {
			Face face = car.faces.get( i );
			
			//gl.glEnable( GL2.GL_TEXTURE_2D );
			//gl.glBindTexture( GL2.GL_TEXTURE_2D, 7 );

			if ( -1 != face.material.texid ) {
				gl.glEnable( GL2.GL_TEXTURE_2D );
				gl.glBindTexture( GL2.GL_TEXTURE_2D, face.material.texid );
				//System.out.println( "Changing materials to texture " + face.material.texid );
			} else {
				gl.glDisable( GL2.GL_TEXTURE_2D );
			}
			
			gl.glBegin( GL2.GL_TRIANGLES );
			
			if ( 3 != face.vertices.size() )
				System.out.println( "ERROR: Triangles won't work!" );
			
			for ( int j = 0; j < face.vertices.size(); ++j ) {
				FaceVertex vert = face.vertices.get( j );
				VertexGeometric vert_v = vert.v;
				VertexTexture vert_t = vert.t;
				
				if ( null != vert_t )
					gl.glTexCoord2f( vert_t.u, vert_t.v ); // Use 1 - v if textures are wrong.
				gl.glVertex3f( vert_v.x, vert_v.y, vert_v.z);
			}
			
			gl.glEnd();
		}
		
		gl.glEnable( GL2.GL_TEXTURE_2D );
	}
	
	private void drawCourse( GL2 gl ) {
		float d = 300.0f;
		
		gl.glDisable( GL2.GL_TEXTURE_2D );
		gl.glColor3f( 1.0f, 1.0f, 1.0f );
		gl.glBegin( GL2.GL_QUADS );
		gl.glVertex3f( -d, -d, 0.0f );
		gl.glVertex3f( -d, d, 0.0f );
		gl.glVertex3f( d, d, 0.0f );
		gl.glVertex3f( d, -d, 0.0f );
		gl.glEnd();
		gl.glEnable( GL2.GL_TEXTURE_2D );
	}
	
	private void drawSkybox( GL2 gl ) {
		float d = 1000.0f;
		
		gl.glPushMatrix();
		gl.glTranslatef( scene_eye_x, scene_eye_y, scene_eye_z );
		
		// Front
		gl.glBindTexture( GL2.GL_TEXTURE_2D, textures[ 0 ] );
		gl.glBegin( GL2.GL_QUADS );
		
		gl.glTexCoord2f( 0.0f, 0.0f );
		gl.glVertex3f( d, d, d );
		
		gl.glTexCoord2f( 0.0f, 1.0f );
		gl.glVertex3f( d, d, -d );
		
		gl.glTexCoord2f( 1.0f, 1.0f );
		gl.glVertex3f( d, -d, -d );
		
		gl.glTexCoord2f( 1.0f, 0.0f );
		gl.glVertex3f( d, -d, d );
		
		gl.glEnd();
		
		// Right
		gl.glBindTexture( GL2.GL_TEXTURE_2D, textures[ 3 ] );
		gl.glBegin( GL2.GL_QUADS );
		
		gl.glTexCoord2f( 0.0f, 0.0f );
		gl.glVertex3f( d, -d, d );
		
		gl.glTexCoord2f( 0.0f, 1.0f );
		gl.glVertex3f( d, -d, -d );
		
		gl.glTexCoord2f( 1.0f, 1.0f );
		gl.glVertex3f( -d, -d, -d );
		
		gl.glTexCoord2f( 1.0f, 0.0f );
		gl.glVertex3f( -d, -d, d );
		
		gl.glEnd();
		
		// Back
		gl.glBindTexture( GL2.GL_TEXTURE_2D, textures[ 2 ] );
		gl.glBegin( GL2.GL_QUADS );
		
		gl.glTexCoord2f( 0.0f, 0.0f );
		gl.glVertex3f( -d, -d, d );
		
		gl.glTexCoord2f( 0.0f, 1.0f );
		gl.glVertex3f( -d, -d, -d );
		
		gl.glTexCoord2f( 1.0f, 1.0f );
		gl.glVertex3f( -d, d, -d );
		
		gl.glTexCoord2f( 1.0f, 0.0f );
		gl.glVertex3f( -d, d, d );
		
		gl.glEnd();
		
		// Left
		gl.glBindTexture( GL2.GL_TEXTURE_2D, textures[ 1 ] );
		gl.glBegin( GL2.GL_QUADS );
		
		gl.glTexCoord2f( 0.0f, 0.0f );
		gl.glVertex3f( -d, d, d );
		
		gl.glTexCoord2f( 0.0f, 1.0f );
		gl.glVertex3f( -d, d, -d );
		
		gl.glTexCoord2f( 1.0f, 1.0f );
		gl.glVertex3f( d, d, -d );
		
		gl.glTexCoord2f( 1.0f, 0.0f );
		gl.glVertex3f( d, d, d );
		
		gl.glEnd();
		
		// Up
		gl.glBindTexture( GL2.GL_TEXTURE_2D, textures[ 4 ] );
		gl.glBegin( GL2.GL_QUADS );
		
		gl.glTexCoord2f( 0.0f, 0.0f );
		gl.glVertex3f( -d, d, d );
		
		gl.glTexCoord2f( 0.0f, 1.0f );
		gl.glVertex3f( d, d, d );
		
		gl.glTexCoord2f( 1.0f, 1.0f );
		gl.glVertex3f( d, -d, d );
		
		gl.glTexCoord2f( 1.0f, 0.0f );
		gl.glVertex3f( -d, -d, d );
		
		gl.glEnd();
		
		// Up
		gl.glBindTexture( GL2.GL_TEXTURE_2D, textures[ 5 ] );
		gl.glBegin( GL2.GL_QUADS );
		
		gl.glTexCoord2f( 0.0f, 0.0f );
		gl.glVertex3f( d, d, -d );
		
		gl.glTexCoord2f( 0.0f, 1.0f );
		gl.glVertex3f( -d, d, -d );
		
		gl.glTexCoord2f( 1.0f, 1.0f );
		gl.glVertex3f( -d, -d, -d );
		
		gl.glTexCoord2f( 1.0f, 0.0f );
		gl.glVertex3f( d, -d, -d );
		
		gl.glEnd();
		
		gl.glPopMatrix();
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
		
		final float throttle_rot = 128.0f;
		
		float dx = ( x - mouse_x0 );
		float dy = ( y - mouse_y0 );
		
		if ( MOUSE_MODE_ROTATE == mouse_mode ) {
			float phi = (float) Math.acos( scene_look_z );
			float theta = (float) Math.atan2( scene_look_y, scene_look_x );
			
			theta -= dx / throttle_rot;
			phi += dy / throttle_rot;
			
			if ( theta >= Math.PI * 2.0 )
				theta -= Math.PI * 2.0;
			else if ( theta < 0 )
				theta += Math.PI * 2.0;
			
			if ( phi > Math.PI - 0.1 )
				phi = (float)( Math.PI - 0.1 );
			else if ( phi < 0.1f )
				phi = 0.1f;
			
			scene_look_x = (float)( Math.cos( theta ) * Math.sin( phi ) );
			scene_look_y = (float)( Math.sin( theta ) * Math.sin( phi ) );
			scene_look_z = (float)( Math.cos( phi ) );
		}
		
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