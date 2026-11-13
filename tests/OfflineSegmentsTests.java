package com.yeezi.classlive;

import java.util.Arrays;
import java.util.Collections;

final class OfflineSegmentsTests {
    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
    private static OfflineSegments.Word word(long start, long end, String text) {
        return new OfflineSegments.Word(start, end, text);
    }
    public static void main(String[] args) {
        OfflineSegments split = new OfflineSegments();
        byte[] frame = new byte[3200];
        for (int i = 0; i < 79; ++i) equal(true, split.add(frame, frame.length, false) == null);
        OfflineSegments.Block first = split.add(frame, frame.length, false);
        equal(1200, first.rightMs);
        equal(256000, first.pcm.length);
        for (int i = 0; i < 10; ++i) split.add(frame, frame.length, false);
        OfflineSegments.Block second = split.flush();
        equal(6800L, second.startMs);
        equal(1200, second.leftMs);
        equal(0, second.rightMs);
        equal(70400, second.pcm.length);

        // The second decode moves the same word across the nominal seam.
        OfflineSegments.Stitcher stitch = new OfflineSegments.Stitcher();
        equal("do for", stitch.accept(first, Arrays.asList(
            word(6960,7040,"do"),word(7200,7280,"for"),word(7360,7970,"you."))));
        equal("you, again", stitch.accept(second, Arrays.asList(
            word(640,720,"For"),word(800,1240,"you,"),word(1400,1480,"again"))));
        equal("", stitch.finish());

        stitch = new OfflineSegments.Stitcher();
        equal("", stitch.accept(first, Collections.singletonList(word(7470,7680,"tail"))));
        equal("tail", stitch.accept(second, Collections.emptyList()));
        equal("", stitch.finish());

        // Pause cuts are contiguous and don't duplicate audio.
        split = new OfflineSegments();
        OfflineSegments.Block natural = null;
        for (int i = 0; i < 24; ++i) natural = split.add(frame, frame.length, i >= 19);
        equal(0, natural.rightMs);
        equal(76800, natural.pcm.length);
        split.add(frame, frame.length, false);
        equal(2400L, split.flush().startMs);
        System.out.println("PASS: quiet seams, hard overlap, shifted word, and tail");
    }
}
