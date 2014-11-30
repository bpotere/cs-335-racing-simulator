package cs335;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.opengl.util.Animator;

public class Main extends Frame {
	static private Animator animator = null;
	
	public Main() {
		super( "Racing Simulator" );
		
		/*
		try {
			new WavefrontParser( "untitled.obj" );
		} catch ( FileNotFoundException e ) {
			
		}
		*/
		
		setLayout( new BorderLayout() );
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				System.exit( 0 );
			}
		});
		
		setSize( 640, 480 );
		setLocation( 0, 0 );
		
		setVisible( true );
		
		setupJOGL();
	}
	
	public static void main( String[] args ) {
		Main m = new Main();
		m.setVisible( true );
	}
	
	private void setupJOGL() {
		GLCapabilities caps = new GLCapabilities( null );
		caps.setDoubleBuffered( true );
		caps.setHardwareAccelerated( true );
		
		GLCanvas canvas = new GLCanvas( caps ); 
		add( canvas );
		
		JoglEventListener jgl = new JoglEventListener();
		canvas.addGLEventListener( jgl ); 
		canvas.addKeyListener( jgl ); 
		canvas.addMouseListener( jgl );
		canvas.addMouseMotionListener( jgl );
		
		animator = new Animator( canvas );
		animator.start();
	}
}
