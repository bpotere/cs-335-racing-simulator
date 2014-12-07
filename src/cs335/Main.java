package cs335;

import java.util.Timer;
import java.util.TimerTask;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;

import com.jogamp.opengl.util.Animator;

public class Main extends JFrame {
	private Timer timer = new Timer();
	
	public Main() {
		super( "Racing Simulator" );
		
		setDefaultCloseOperation( EXIT_ON_CLOSE );
		setSize( 640, 480 );
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
		
		final GLCanvas canvas = new GLCanvas( caps ); 
		add( canvas );
		
		JoglEventListener jgl = new JoglEventListener();
		canvas.addGLEventListener( jgl ); 
		canvas.addKeyListener( jgl ); 
		canvas.addMouseListener( jgl );
		canvas.addMouseMotionListener( jgl );
		
		timer.scheduleAtFixedRate(
			new TimerTask() {
				@Override
				public void run() {
					canvas.display();
				}
			},
			0, // Start immediately.
			17 // 17 ms delay.
		);
	}
}
