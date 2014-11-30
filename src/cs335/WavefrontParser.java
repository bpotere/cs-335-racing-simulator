package cs335;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Scanner;

public class WavefrontParser {
	//Scanner scanner = null;
	/*
	 * Vertex data: v, vt, vn
	 * Elements: f
	 */
	private ArrayList<ArrayList<Float>> v = new ArrayList<ArrayList<Float>>();
	private ArrayList<ArrayList<Float>> vt = new ArrayList<ArrayList<Float>>();
	private ArrayList<ArrayList<Float>> vn = new ArrayList<ArrayList<Float>>();
	
	public WavefrontParser( String filename ) throws FileNotFoundException {
		Scanner scanner = new Scanner( new File( filename ) );
		
		String[] line;
		while (  scanner.hasNext() ) {
			line = scanner.nextLine().trim().split( "\\p{Space}" );
			
			ArrayList<Float> temp = new ArrayList<Float>( 3 );
			
			switch ( line[ 0 ] ) {
				case "v":					
					temp.add( Float.valueOf( line[ 1 ] ) );
					temp.add( Float.valueOf( line[ 2 ] ) );
					temp.add( Float.valueOf( line[ 3 ] ) );
					v.add( temp );
					break;
					
				case "vt":
					temp.add( Float.valueOf( line[ 1 ] ) );
					temp.add( Float.valueOf( line[ 2 ] ) );
					temp.add( Float.valueOf( line[ 3 ] ) );
					vt.add( temp );
					break;
					
				case "vn":
					temp.add( Float.valueOf( line[ 1 ] ) );
					temp.add( Float.valueOf( line[ 2 ] ) );
					temp.add( Float.valueOf( line[ 3 ] ) );
					vn.add( temp );
					break;
				
				default:
					break;
			}
		}
	}
}
