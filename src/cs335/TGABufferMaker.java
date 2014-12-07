   package cs335;
   
   import com.jogamp.common.nio.Buffers;
   import java.io.File;
   import java.io.FileInputStream;
   import java.nio.ByteBuffer;
   import java.nio.MappedByteBuffer;
   import java.nio.channels.FileChannel;
   
   
   public class TGABufferMaker {
   
       public static TGABuffer make(String filename) {
           ByteBuffer imageBuffer;
           File f = new File(filename);
   
           try {
               // Open the file and then get a channel from the stream
               FileInputStream fis = new FileInputStream(f);
               FileChannel fc = fis.getChannel();
               int sz = (int) fc.size();
               MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
               byte b = bb.get(2);
               if (b != 2) {
                   System.out.println("Only handles unmapped RGB format");
                   throw new Exception();
               }
   
               int width = getInt(bb, 12);
               int height = getInt(bb, 14);
               byte bpp = bb.get(16);
               if (bpp != 32) {
                   System.out.println("Only handles 32 bit RGBA format");
                   throw new Exception();
               }
               // here we have a 32bit RGB width X height sequence of pixels 
               bb.position(18); //go past header
               imageBuffer = Buffers.newDirectByteBuffer(width * height * 4);
               for (int i = 0; i < height; i++) {
                   for (int j = 0; j < width; j++) {
                       byte blue = bb.get(); // in TGA, in BGRA order!!
                       byte green = bb.get();
                       byte red = bb.get();
                       byte alpha = bb.get();
                       imageBuffer.put(red);
                       imageBuffer.put(green);
                       imageBuffer.put(blue);
                       imageBuffer.put(alpha);
                   }
               }
               imageBuffer.rewind();
               return new TGABuffer(imageBuffer, width, height);
   
           } catch (Exception x) {
               System.err.println( x);
               System.exit(0);
           }
           return null;
       }
   
       private static int getInt(MappedByteBuffer bb, int offset) {
           // TGA stores 16bit ints as unsigned, LoHi order, but
           // getShort would expect to read signed 2's complement in HiLo order
           // so we need this..
           int bLo = bb.get(offset);
           if (bLo < 0) {
               bLo = 256 + bLo;
           }
           int bHi = (int) bb.get(offset + 1);
           if (bHi < 0) {
               bHi = 256 + bHi;
           }
           int result = (bHi << 8) | bLo;
           return result;
       }
   
}