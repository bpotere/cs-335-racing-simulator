package cs335;

import javax.media.opengl.GL2;

public class TextureLoaderClamping extends TextureLoader {
	public TextureLoaderClamping( GL2 gl ) {
		super( gl );
	}
	
	protected void setupTextureParameters() {
		super.setupTextureParameters();
		
		gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE );
		gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE );
	}
}
