package com.yeezi.classlive;

import android.content.Context;
import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;

final class OfflineModel {
    static final String NAME = "parakeet-unified-en-0.6b-Q4_K_M.gguf";
    static final String URL = "https://huggingface.co/handy-computer/parakeet-unified-en-0.6b-gguf/resolve/"
            + "843b1b175ace04f5a89d379c79e2518c0d065b25/" + NAME;
    static final String SHA256 = "a8bf3de2b393bd14ead5a858c3748d5e3b07a20fdeabdd3b498fba4f463fa929";
    static final long SIZE = 477274496L;

    interface Progress { void update(int percent); }

    static File file(Context context) { return new File(context.getFilesDir(), NAME); }

    static boolean installed(Context context) {
        File f = file(context);
        return f.isFile() && f.length() == SIZE;
    }

    static void install(File destination, InputStream input, Progress progress) throws Exception {
        File temp = new File(destination.getPath() + ".part");
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        long total = 0;
        int last = -1;
        try {
            try (FileOutputStream output = new FileOutputStream(temp)) {
                byte[] buffer = new byte[64 * 1024];
                byte[] expectedMagic = {'G','G','U','F'};
                byte[] magic = new byte[4];
                DataInputStream source = new DataInputStream(input);
                source.readFully(magic);
                if (!Arrays.equals(magic, expectedMagic)) throw new IOException("请选择实验版所需的 GGUF 模型文件");
                output.write(magic);
                hash.update(magic);
                total = magic.length;
                int n;
                while ((n = source.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                    total += n;
                    if (total > SIZE) throw new IOException("模型版本不匹配");
                    output.write(buffer, 0, n);
                    hash.update(buffer, 0, n);
                    int percent = (int)(100 * total / SIZE);
                    if (percent != last) { progress.update(percent); last = percent; }
                }
                output.getFD().sync();
            }
            byte[] expected = new byte[32];
            for (int i = 0; i < expected.length; i++)
                expected[i] = (byte)Integer.parseInt(SHA256.substring(2*i, 2*i+2), 16);
            if (total != SIZE || !MessageDigest.isEqual(expected, hash.digest()))
                throw new IOException("下载不完整，或不是本版所需的模型");
            if (!temp.renameTo(destination)) throw new IOException("无法保存模型，请检查剩余空间");
        } finally {
            if (temp.exists()) temp.delete();
        }
    }
}
