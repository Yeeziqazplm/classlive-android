package com.yeezi.classlive;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Used on a single inference worker. The mic reader never runs this engine. */
final class OfflineAsr implements AutoCloseable {
    private long handle;

    OfflineAsr(File model) throws IOException {
        System.loadLibrary("classlive_asr");
        handle = openNative(model.getAbsolutePath(), 4);
        if (handle == 0) throw new IOException("Parakeet model did not load");
    }

    List<OfflineSegments.Word> decode(byte[] pcm) throws IOException {
        if (handle == 0) throw new IOException("Local recognizer is closed");
        String output = new String(decodeNative(handle, pcm), StandardCharsets.UTF_8);
        List<OfflineSegments.Word> words = new ArrayList<>();
        for (String line : output.split("\n")) {
            String[] parts = line.split("\t", 3);
            if (parts.length != 3 || parts[2].trim().isEmpty()) continue;
            words.add(new OfflineSegments.Word(Long.parseLong(parts[0]), Long.parseLong(parts[1]), parts[2].trim()));
        }
        return words;
    }

    @Override public void close() {
        if (handle != 0) {
            closeNative(handle);
            handle = 0;
        }
    }

    private static native long openNative(String modelPath, int threads) throws IOException;
    private static native byte[] decodeNative(long handle, byte[] pcm) throws IOException;
    private static native void closeNative(long handle);
}
