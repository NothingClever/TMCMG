package togos.minecraft.mapgen.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.jnbt.CompoundTag;
import org.jnbt.NBTOutputStream;

import togos.minecraft.mapgen.ui.NoiseCanvas;
import togos.minecraft.mapgen.world.ChunkUtil;
import togos.minecraft.mapgen.world.gen.ChunkFunction;
import togos.minecraft.mapgen.world.structure.ChunkData;

public class MapWriter
{
	public int tmod( int num, int modby ) {
		if( num < 0 ) {
			num = -num;
			num %= modby;
			num = modby - num;
			return num;
		} else {
			return num % modby;
		}
	}
	
	public String chunkPath( int x, int z ) {
		return Integer.toString( tmod(x,64), 36 ) + "/" +
			Integer.toString( tmod(z,64), 36 ) + "/" +
			"c." + Integer.toString(x,36) + "." + Integer.toString(z,36) + ".dat";
	}
	
	public void writeChunkToFile( ChunkData cd, String baseDir ) throws IOException {
		String fullPath = baseDir + "/" + chunkPath( cd.x, cd.z );
		File f = new File(fullPath);
		File dir = f.getParentFile();
		if( dir != null && !dir.exists() ) dir.mkdirs();
		FileOutputStream os = new FileOutputStream(f);
		try {
			NBTOutputStream nbtos = new NBTOutputStream(os);
			
			HashMap levelRootTags = new HashMap();
			levelRootTags.put("Level",cd.toTag());
			CompoundTag fileRootTag = new CompoundTag("",levelRootTags);
			
			// System.out.println(fileRootTag);
			
			nbtos.writeTag(fileRootTag);
			nbtos.close();
		} finally {
			os.close();
		}
		// System.err.println("Wrote "+fullPath);
	}
	
	public static void main(String[] args) {
		int boundsX = 0;
		int boundsZ = 0;
		int boundsWidth = 1;
		int boundsDepth = 1;
		String mapDir = ".";
		for( int i=0; i<args.length; ++i ) {
			if( "-map-dir".equals(args[i]) ) {
				mapDir = args[++i];
			} else if( "-x".equals(args[i]) ) {
				boundsX = Integer.parseInt(args[++i]);
			} else if( "-z".equals(args[i]) ) {
				boundsZ = Integer.parseInt(args[++i]);
			} else if( "-width".equals(args[i]) ) {
				boundsWidth = Integer.parseInt(args[++i]);
			} else if( "-depth".equals(args[i]) ) {
				boundsDepth = Integer.parseInt(args[++i]);
			} else {
				System.err.println("Unrecognised argument: "+args[i]);
				System.exit(1);
			}
		}
		
		try {
			/*
			ChunkData cd = new ChunkData();
			for( int z=0; z<16; ++z ) {
				for( int x=0; x<16; ++x ) {
					for( int y=0; y<128; ++y ) {
						if( y < 64+x+z ) {
							cd.setBlock(x, y, z, (byte)17, (byte)0 );
						} else {
							cd.setBlock(x, y, z, (byte)0, (byte)0 );
						}
					}
				}
			}
			cd.x = 23; 
			cd.z = -6;
			ChestData chest = new ChestData();
			chest.x = cd.x*cd.width+0;
			chest.y = 64;
			chest.z = cd.z*cd.depth+0;
			chest.inventoryItems.add( new InventoryItemData( BlockIDs.DIAMOND_AXE, 10, 1) );
			chest.inventoryItems.add( new InventoryItemData( BlockIDs.DIAMOND_PICKAXE, 10, 2) );
			chest.inventoryItems.add( new InventoryItemData( BlockIDs.DIAMOND_SPADE, 10, 3) );
			chest.inventoryItems.add( new InventoryItemData( BlockIDs.DIAMOND_SWORD, 10, 4) );
			//cd.tileEntityData.add( chest );
			//cd.setBlock(0,64,0, (byte)54);
			ChunkUtil.addTileEntity(chest, cd);
			ChunkUtil.calculateLighting(cd, 15);
			new MapWriter().writeChunkToFile(cd, mapDir);
			*/
			
			MapWriter mapWriter = new MapWriter();
			ChunkFunction cfunc = NoiseCanvas.getDefaultLayerMapper().getLayerChunkFunction();
			for( int z=0; z<boundsDepth; ++z ) {
				for( int x=0; x<boundsWidth; ++x ) {
					ChunkData cd = cfunc.getChunk(boundsX+x, boundsZ+z);
					ChunkUtil.calculateLighting(cd, 15);
					mapWriter.writeChunkToFile(cd, mapDir);
				}
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}
