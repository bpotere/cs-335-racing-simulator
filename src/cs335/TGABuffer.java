	 package cs335;
	 
	 import java.nio.ByteBuffer;
	 
	 public class TGABuffer {
	 
	     private ByteBuffer imageBuffer;
	     private int width;
	     private int height;
	 
	     TGABuffer(ByteBuffer b, int width, int height) {
	         imageBuffer = b;
	         this.width = width;
	         this.height = height;
	     }
	 
	     public int getHeight() {
	         return height;
	     }
	 
	     public int getWidth() {
	         return width;
	     }
	 
	     public ByteBuffer getBuffer() {
	         return imageBuffer;
	     }
}