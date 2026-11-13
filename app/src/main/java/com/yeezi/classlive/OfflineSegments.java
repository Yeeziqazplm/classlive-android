package com.yeezi.classlive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Fixed-rate PCM16. Cut at quiet gaps when possible; overlap the unavoidable cuts. */
final class OfflineSegments {
    private static final int BYTES_PER_MS = 32;
    private static final int TARGET_MS = 2400;
    private static final int MAX_MS = 8000;
    private static final int PAUSE_MS = 500;
    private static final int OVERLAP_MS = 1200;
    private final byte[] buffer = new byte[(MAX_MS + OVERLAP_MS) * BYTES_PER_MS];
    private int size, prefix, quietMs;
    private long startBytes;

    static final class Block {
        final byte[] pcm;
        final long startMs;
        final int leftMs, rightMs;
        Block(byte[] pcm, long startMs, int leftMs, int rightMs) {
            this.pcm = pcm;
            this.startMs = startMs;
            this.leftMs = leftMs;
            this.rightMs = rightMs;
        }
    }

    static final class Word {
        final long start, end;
        final String text;
        Word(long start, long end, String text) { this.start = start; this.end = end; this.text = text; }
        long middle() { return start + (end - start) / 2; }
    }

    Block add(byte[] pcm, int length, boolean quiet) {
        if (length < 0 || length > pcm.length || length % 2 != 0 || size + length > buffer.length)
            throw new IllegalArgumentException("Invalid audio frame");
        System.arraycopy(pcm, 0, buffer, size, length);
        size += length;
        quietMs = quiet ? quietMs + length / BYTES_PER_MS : 0;
        int fresh = (size - prefix) / BYTES_PER_MS;
        if (fresh >= TARGET_MS && quietMs >= PAUSE_MS) return cut(false);
        return fresh >= MAX_MS ? cut(true) : null;
    }

    Block flush() { return size == 0 ? null : cut(false); }

    private Block cut(boolean hard) {
        int retain = hard ? OVERLAP_MS * BYTES_PER_MS : 0;
        Block result = new Block(Arrays.copyOf(buffer, size), startBytes / BYTES_PER_MS, prefix / BYTES_PER_MS, retain / BYTES_PER_MS);
        if (retain > 0) System.arraycopy(buffer, size - retain, buffer, 0, retain);
        startBytes += size - retain;
        size = prefix = retain;
        quietMs = 0;
        return result;
    }

    /**
     * Publish each side of the overlap once. Keep the uncommitted tail as a
     * fallback if the second decode hears nothing. Use exact word + timing
     * to avoid deduplicating intentional repetitions in the lecture.
     */
    static final class Stitcher {
        private List<Word> pending = new ArrayList<>();
        private List<Word> priorOverlap = new ArrayList<>();
        private Word last;

        String accept(Block block, List<Word> relative) {
            List<Word> words = new ArrayList<>();
            for (Word w : relative) {
                if (w.start < 0 || w.end <= w.start) {
                    String tail = finish();
                    priorOverlap.clear();
                    last = null;
                    return tail.isEmpty() ? w.text : tail + " " + w.text;
                }
                if (w.middle() < block.pcm.length / BYTES_PER_MS)
                    words.add(new Word(block.startMs + w.start, block.startMs + w.end, w.text));
            }
            long left = block.startMs + block.leftMs / 2;
            long right = block.startMs + block.pcm.length / BYTES_PER_MS - block.rightMs / 2;
            long priorBoundary = block.startMs + block.leftMs;
            long nextOverlap = block.startMs + block.pcm.length / BYTES_PER_MS - block.rightMs;
            List<Word> settled = new ArrayList<>(), next = new ArrayList<>(), overlap = new ArrayList<>();
            Word oldAnchor = null, newAnchor = null;
            long best = Long.MAX_VALUE;
            for (Word w : words) {
                if (w.start >= priorBoundary + 180) break;
                String key = normalized(w.text);
                if (key.isEmpty()) continue;
                for (Word prior : priorOverlap) {
                    long delta = Math.abs(prior.start - w.start);
                    if (delta > 400 || !key.equals(normalized(prior.text))) continue;
                    long score = Math.abs(prior.start-left) + delta;
                    if (score < best) { best=score; oldAnchor=prior; newAnchor=w; }
                }
            }
            boolean heardTail = false;
            for (Word w : words) if (w.middle() >= left && w.start < priorBoundary) heardTail = true;
            if (oldAnchor == null && !heardTail) settled.addAll(pending);
            if (oldAnchor != null) {
                for (Word w : pending) if (w.start < oldAnchor.start) settled.add(w);
            }
            for (Word w : words) {
                if (newAnchor == null ? w.middle() < left
                        : w.start < newAnchor.start || (w == newAnchor && !pending.contains(oldAnchor))) continue;
                if (w.middle() < right || block.rightMs == 0) settled.add(w);
                else next.add(w);
                if (block.rightMs > 0 && w.end > nextOverlap) overlap.add(w);
            }
            pending = next;
            priorOverlap = overlap;
            return textOf(settled);
        }

        String finish() {
            String tail = textOf(pending);
            pending.clear();
            return tail;
        }

        private String textOf(List<Word> words) {
            StringBuilder out = new StringBuilder();
            Word previousBlock = last;
            for (Word w : words) {
                if (previousBlock != null && Math.abs(w.middle() - previousBlock.middle()) < 260
                        && w.start < previousBlock.end && previousBlock.start < w.end
                        && normalized(w.text).equals(normalized(previousBlock.text))) continue;
                if (out.length() > 0) out.append(' ');
                out.append(w.text);
                last = w;
            }
            return out.toString();
        }
        private static String normalized(String word) {
            return word.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9']", "");
        }
    }
}
