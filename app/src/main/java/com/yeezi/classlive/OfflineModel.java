package com.yeezi.classlive;

import android.content.Context;
import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;

final class OfflineModel {
    interface Progress { void update(int percent); }

    static ModelCatalog.Spec selected(Context c) { return ModelCatalog.find(c.getSharedPreferences("settings",0).getString("offlineModel","Q4_K_M")); }
    static File file(Context c) { return file(c,selected(c)); }
    static File file(Context c,ModelCatalog.Spec s) { return new File(c.getFilesDir(),s.name); }
    static boolean installed(Context c) { return installed(c,selected(c)); }

    static boolean installed(Context context,ModelCatalog.Spec spec) {
        File f = file(context,spec);
        return f.isFile() && f.length() == spec.size;
    }

    static void install(ModelCatalog.Spec spec, File destination, InputStream input, Progress progress) throws Exception {
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
                if (!Arrays.equals(magic, expectedMagic)) throw new IOException("请选择当前档位对应的 GGUF 模型文件");
                output.write(magic);
                hash.update(magic);
                total = magic.length;
                int n;
                while ((n = source.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                    total += n;
                    if (total > spec.size) throw new IOException("模型版本不匹配");
                    output.write(buffer, 0, n);
                    hash.update(buffer, 0, n);
                    int percent = (int)(100 * total / spec.size);
                    if (percent != last) { progress.update(percent); last = percent; }
                }
                output.getFD().sync();
            }
            byte[] expected = new byte[32];
            for (int i = 0; i < expected.length; i++)
                expected[i] = (byte)Integer.parseInt(spec.sha256.substring(2*i, 2*i+2), 16);
            if (total != spec.size || !MessageDigest.isEqual(expected, hash.digest()))
                throw new IOException("下载不完整，或不是本版所需的模型");
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            if (!temp.renameTo(destination)) throw new IOException("无法保存模型，请检查剩余空间");
        } finally {
            if (temp.exists()) temp.delete();
        }
    }
}
