package com.example.logprocessor.consumer;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.springframework.stereotype.Service;
import org.xerial.snappy.Snappy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

@Service
public class DecompressionService {

    private final LZ4Factory lz4Factory = LZ4Factory.fastestInstance();

    public String decompress(byte[] data, String algorithm) throws IOException {
        byte[] decompressed = switch (algorithm.toLowerCase()) {
            case "gzip" -> decompressGzip(data);
            case "snappy" -> Snappy.uncompress(data);
            case "lz4" -> decompressLz4(data);
            case "none" -> data;
            default -> throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        };
        
        return new String(decompressed, StandardCharsets.UTF_8);
    }

    private byte[] decompressGzip(byte[] input) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(input);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
        }
        
        return baos.toByteArray();
    }

    private byte[] decompressLz4(byte[] input) {
        int originalSize = ((input[0] & 0xFF) << 24) | ((input[1] & 0xFF) << 16) | 
                          ((input[2] & 0xFF) << 8) | (input[3] & 0xFF);
        
        LZ4FastDecompressor decompressor = lz4Factory.fastDecompressor();
        byte[] restored = new byte[originalSize];
        decompressor.decompress(input, 4, restored, 0, originalSize);
        
        return restored;
    }
}
