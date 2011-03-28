/*
 * Further modifications by TOGoS for use in TMCMG:
 *  - Removed use of templates, auto[un]boxing, and foreach loops
 *    to make source compatible with Java 1.4  
 */

/*
 ** 2011 January 5
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */

/*
 * 2011 February 16
 * 
 * This source code is based on the work of Scaevolus (see notice above).
 * It has been slightly modified by Mojang AB to limit the maximum cache
 * size (relevant to extremely big worlds on Linux systems with limited
 * number of file handles). The region files are postfixed with ".mcr"
 * (Minecraft region file) instead of ".data" to differentiate from the
 * original McRegion files.
 * 
 */

// A simple cache and wrapper for efficiently multiple RegionFiles simultaneously.

package togos.minecraft.mapgen.io;

import java.io.*;
import java.lang.ref.*;
import java.util.*;

public class RegionFileCache {

    private static final int MAX_CACHE_SIZE = 256;


    private static final Map cache = new HashMap();

    private RegionFileCache() {
    }

    public static synchronized RegionFile getRegionFile(File basePath, int chunkX, int chunkZ) {
        File regionDir = new File(basePath, "region");
        File file = new File(regionDir, "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mcr");

        Reference ref = (Reference)cache.get(file);

        if (ref != null && ref.get() != null) {
            return (RegionFile)ref.get();
        }

        if (!regionDir.exists()) {
            regionDir.mkdirs();
        }

        if (cache.size() >= MAX_CACHE_SIZE) {
            RegionFileCache.clear();
        }

        RegionFile reg = new RegionFile(file);
        cache.put(file, new SoftReference(reg));
        return reg;
    }

    public static synchronized void clear() {
        for( Iterator i=cache.values().iterator(); i.hasNext(); ) {
        	Reference ref = (Reference)i.next();
            try {
                if (ref.get() != null) {
                    ((RegionFile)ref.get()).close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cache.clear();
    }

    public static int getSizeDelta(File basePath, int chunkX, int chunkZ) {
        RegionFile r = getRegionFile(basePath, chunkX, chunkZ);
        return r.getSizeDelta();
    }

    public static DataInputStream getChunkDataInputStream(File basePath, int chunkX, int chunkZ) {
        RegionFile r = getRegionFile(basePath, chunkX, chunkZ);
        return r.getChunkDataInputStream(chunkX & 31, chunkZ & 31);
    }

    public static DataOutputStream getChunkDataOutputStream(File basePath, int chunkX, int chunkZ) {
        RegionFile r = getRegionFile(basePath, chunkX, chunkZ);
        return r.getChunkDataOutputStream(chunkX & 31, chunkZ & 31);
    }
}
