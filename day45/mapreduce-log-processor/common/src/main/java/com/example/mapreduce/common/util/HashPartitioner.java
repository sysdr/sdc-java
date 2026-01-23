package com.example.mapreduce.common.util;

public class HashPartitioner {
    
    public static int getPartition(String key, int numPartitions) {
        // Use murmur3 hash for better distribution
        int hash = murmur3Hash(key);
        return Math.abs(hash % numPartitions);
    }
    
    private static int murmur3Hash(String key) {
        byte[] data = key.getBytes();
        int seed = 0x9747b28c;
        
        int h = seed;
        int len = data.length;
        int i = 0;
        
        while (len >= 4) {
            int k = data[i] & 0xff;
            k |= (data[i + 1] & 0xff) << 8;
            k |= (data[i + 2] & 0xff) << 16;
            k |= (data[i + 3] & 0xff) << 24;
            
            k *= 0xcc9e2d51;
            k = (k << 15) | (k >>> 17);
            k *= 0x1b873593;
            
            h ^= k;
            h = (h << 13) | (h >>> 19);
            h = h * 5 + 0xe6546b64;
            
            i += 4;
            len -= 4;
        }
        
        int k = 0;
        switch (len) {
            case 3:
                k ^= (data[i + 2] & 0xff) << 16;
            case 2:
                k ^= (data[i + 1] & 0xff) << 8;
            case 1:
                k ^= (data[i] & 0xff);
                k *= 0xcc9e2d51;
                k = (k << 15) | (k >>> 17);
                k *= 0x1b873593;
                h ^= k;
        }
        
        h ^= data.length;
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        
        return h;
    }
}
