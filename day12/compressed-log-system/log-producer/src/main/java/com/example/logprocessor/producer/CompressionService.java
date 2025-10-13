package com.example.logprocessor.producer;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.springframework.stereotype.Service;
import org.xerial.snappy.Snappy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

@Service
public class CompressionService {

    private final LZ4Factory lz4Factory = LZ4Factory.fastestInstance();

    public byte[] compress(String data, CompressionAlgorithm algorithm) throws IOException {
        byte[] input = data.getBytes(StandardCharsets.UTF_8);
        
        return switch (algorithm) {
            case GZIP -> compressGzip(input);
            case SNAPPY -> Snappy.compress(input);
            case LZ4 -> compressLz4(input);
            case NONE -> input;
        };
    }

    private byte[] compressGzip(byte[] input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(input);
        }
        return baos.toByteArray();
    }

    private byte[] compressLz4(byte[] input) {
        LZ4Compressor compressor = lz4Factory.fastCompressor();
        int maxCompressedLength = compressor.maxCompressedLength(input.length);
        byte[] compressed = new byte[maxCompressedLength];
        int compressedLength = compressor.compress(input, 0, input.length, compressed, 0, maxCompressedLength);
        
        byte[] result = new byte[compressedLength + 4];
        result[0] = (byte) (input.length >>> 24);
        result[1] = (byte) (input.length >>> 16);
        result[2] = (byte) (input.length >>> 8);
        result[3] = (byte) input.length;
        System.arraycopy(compressed, 0, result, 4, compressedLength);
        
        return result;
    }

    public CompressionAlgorithm selectAlgorithm(String data) {
        int size = data.getBytes(StandardCharsets.UTF_8).length;
        
        // Skip compression for small payloads
        if (size < 512) {
            return CompressionAlgorithm.NONE;
        }
        
        // Use LZ4 for large batches (fast decompression)
        if (size > 10240) {
            return CompressionAlgorithm.LZ4;
        }
        
        // Default to Snappy for balanced performance
        return CompressionAlgorithm.SNAPPY;
    }
}
