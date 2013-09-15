package togos.noise.function;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import togos.minecraft.mapgen.ui.Icons;
import togos.noise.v3.vector.function.LFunctionDaDaDa_Da;


/**
 * Copy and paste of terragen import, which was a gutted copy and paste of simplex noise
 * Because there are currently no script functions that take normal strings
 * and I'm too lazy to add some, I just hacked the simplex noise function
 * so x,y,z correspond to x,z, and file number. Thus can only load *.png files
 * of the format #.png where # is an integer <= 255 (arbitrary). Can use "constants"
 *  for them in the script.
 */
public final class PngNoise implements LFunctionDaDaDa_Da
{
	// To Do:
	// Add some way to offset image.
	// -- Can be accomplished in script with axis + displacement
	// Decide between tiling or not
	// -- No tiling seems like the best option, more intuitive and flexible
	// Need some way to detect when to reload map because it's not refreshing it
	// when it needs too.
	// -- Possibly working via time stamp checking, a little hacky
	// Change behavior so that it looks in script's directory not in jar file's.
    static double[][] map= new double[256][];
    static int[][] mapdata = new int[256][5];
	static int iMaxX = 0;
	static int iMaxY = 1;
	static int iHeightScale = 2;
	static int iBaseHeight = 3;
	static int iTimeStamp = 4;

    public final float apply(float x, float y, float _z) {
    	int z = (int)_z;
    	//Arbitrary magic number
    	if (z > 255 || z < 0) {
    		return 0;
    	}
    	loadMap(x,y,z); 
    	if ( x > mapdata[z][iMaxX] - 1 ) { return 0; }
    	if ( x < 0 ) {  return 0;  }
    	if ( y > mapdata[z][iMaxY] - 1) {   return 0;  }
    	if ( y < 0 ) {   return 0;  }
    	//Should return values 0-1
    	return 	(float)((map[z][ (int)x  + ((int)y * mapdata[z][iMaxX])  ]) / 65536.0d);
    }

	public void apply( int vectorSize, double[] x, double[] y, double[] z, double[] dest ) {
		for( int j=vectorSize-1; j>=0; --j ) {
			dest[j] = apply( (float)x[j], (float)y[j], (float)z[j] );
		}
    }
	
	private void loadMap(float x, float y, float _z) {
		int z = (int)_z;

		Path path = Paths.get(z + ".png");
		BasicFileAttributes attributes;
		
		try {
			attributes = Files.readAttributes(path, BasicFileAttributes.class);
			FileTime creationTime = attributes.lastModifiedTime();
			if ( map[z] != null && creationTime.hashCode() == mapdata[z][iTimeStamp]) {
	    		return; 
	    	}
			//System.out.printf( "Opened");
			BufferedImage img = null;
		    img = ImageIO.read(new File(z + ".png"));
		    //System.out.printf( "Read");
		    //Only read 16bit grey scale for now, could add other formats later
		    if (img.getType() == BufferedImage.TYPE_USHORT_GRAY ){
		    	//System.out.printf( "Right type");
		    	mapdata[z][iMaxX] = img.getWidth();
		    	mapdata[z][iMaxY] = img.getHeight();
		    	int imagesize = img.getWidth() * img.getHeight();
		    	map[z] = new double[imagesize];
		    	img.getData().getPixels(0, 0, img.getWidth(), img.getHeight(), map[z]);
    			//Works but getRGB converts into a 8 bit value so lose 8 bits of precision
//			    	for(int i = 0; i < img.getWidth(); i++) {
//			    		for(int j = 0; j < img.getHeight(); j++) {
//			    			//map[z][i+ j * img.getWidth()] = img.getRGB(i, j);
//			    			//System.out.printf( "v: %d\n", map[z][i+ j * img.getWidth()]);
//			    		}
//					}
			    }
			mapdata[z][iTimeStamp] = creationTime.hashCode();
		} catch (IOException e3) {
			
			//Actual message is I have no idea of what to do with an exception
			System.out.printf( "File not found or could not be opened");
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		}

}