package cs335;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;

import com.owens.oobjloader.builder.Build;
import com.owens.oobjloader.parser.BuilderInterface;

public class TempBuilder extends Build {
	//ConcurrentSkipListMap<String, Integer> mtmap = new ConcurrentSkipListMap<String, Integer>();
	GL2 gl;
	
	public TempBuilder( GL2 gl ) {
		super();
		this.gl = gl;
	}
	
	public void setMapDecalDispBump(int type, String filename) {
		super.setMapDecalDispBump( type, filename );
		
		// Generate a new texture if none exists.
		if ( -1 == currentMaterialBeingParsed.texid ) {
			int[] textures = new int[ 1 ];
			gl.glGenTextures( 1, textures, 0 );
			generateTexture( textures[ 0 ], filename );
			currentMaterialBeingParsed.texid = textures[ 0 ];
		}
		/*
		if ( ! mtmap.containsKey( filename ) ) {
			int[] textures = new int[ 1 ];
			gl.glGenTextures( 1, textures, 0 );
			generateTexture( textures[ 0 ], filename );
			mtmap.put( filename,  textures[ 0 ] );
		}
		*/
	}
	
	private void generateTexture( int texid, String filename ) {
		Path filename_resolved= Paths.get( objFilename ).toAbsolutePath().resolveSibling( filename );
		
		try {
			BufferedImage img = ImageIO.read( new File( filename_resolved.toString() ) );
			
			int[] pixels = new int[img.getWidth() * img.getHeight()];
			PixelGrabber grabber = new PixelGrabber(img, 0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
			grabber.grabPixels();

			int bufLen = pixels.length * 4;
			ByteBuffer oglPixelBuf = ByteBuffer.wrap( new byte[ bufLen ] );

			for (int y = img.getHeight() - 1; y >= 0; y--) {
				for (int x = 0; x < img.getWidth(); x++) {
					int pixel = pixels[y * img.getWidth() + x];
					oglPixelBuf.put((byte) ((pixel >> 16) & 0xFF));
					oglPixelBuf.put((byte) ((pixel >> 8) & 0xFF));
					oglPixelBuf.put((byte) ((pixel >> 0) & 0xFF));
					oglPixelBuf.put((byte) ((pixel >> 24) & 0xFF));
				}
			}

			oglPixelBuf.flip();

			//byte[] data = ( (DataBufferByte) img.getRaster().getDataBuffer() ).getData();
			
			gl.glBindTexture( GL2.GL_TEXTURE_2D, texid );
			gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR );
			gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR );
			gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE );
			gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE );
			gl.glTexEnvf( GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE );
			gl.glTexImage2D( GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, img.getWidth(),
					img.getHeight(), 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, oglPixelBuf );
		} catch ( Exception e ) {
			System.out.println( "Failed to load texture: " + e.getMessage() );
		}
	}
}
