package com.yeezi.classlive;

/** Conservative PCM16 processing. Measurements refer to input, before any gain.
 * Boost requires recent recognition feedback plus a margin above the estimated
 * floor. No hard noise gate and no automatic hardware effects are used here. */
final class AudioProcessor {
    static final double MAX_GAIN = 1.9952623149688795; // +6 dB
    static final double CEILING = 31128.0; // approximately -0.45 dBFS
    private final double[] history = new double[80];
    private int position, count, frames, clippedFrames, zeroFrames, weakFrames, noisyFrames;
    private double noiseDb = -70, gain = 1;
    private volatile long speechAt = -100000;

    void noteSpeech(long nowMs) { speechAt = nowMs; }
    static final class Metrics {
        final double rmsDb, peakDb, noiseDb, gainDb;
        final String state;
        final boolean warning;
        Metrics(double r, double p, double n, double g, String s, boolean w) {
            rmsDb=r; peakDb=p; noiseDb=n; gainDb=g; state=s; warning=w;
        }
    }
    private static double db(double amplitude) { return 20*Math.log10(Math.max(1,amplitude)/32768.0); }
    Metrics process(byte[] pcm, int length, boolean enhance, long nowMs) {
        if(length<=0 || length%2!=0) throw new IllegalArgumentException("PCM16 frame length");
        double power=0; int peak=0, clipped=0;
        for(int i=0;i<length;i+=2) {
            int sample=(short)((pcm[i]&255)|(pcm[i+1]<<8));
            power+=(double)sample*sample;
            peak=Math.max(peak,Math.abs(sample));
            if(Math.abs(sample)>=32600) clipped++;
        }
        double rms=Math.sqrt(power/(length/2)), inputDb=db(rms), peakDb=db(peak);
        history[position]=inputDb; position=(position+1)%history.length;
        if(count<history.length) count++;
        frames++;
        if(frames%5==0) {
            double[] sorted=new double[count]; System.arraycopy(history,0,sorted,0,count);
            java.util.Arrays.sort(sorted);
            noiseDb=sorted[Math.max(0,(count-1)/5)];
        }
        boolean speech=nowMs-speechAt>=0 && nowMs-speechAt<2500;
        boolean impulsive=peakDb-inputDb>18;
        double target=1;
        // A loud input can always be attenuated. Quiet input is boosted only
        // after speech has actually been recognized, with enough headroom/SNR.
        if(enhance && peak>0) {
            if(peak>CEILING) target=CEILING/peak;
            else if(frames>=20 && speech && !impulsive && inputDb>-50 && inputDb<-25 && inputDb-noiseDb>=8) {
                target=Math.min(MAX_GAIN,Math.pow(10,(-25-inputDb)/20));
                target=Math.min(target,CEILING/peak);
            }
        }
        if(!enhance) gain=1;
        else {
            double alpha=target<gain?0.45:0.025;
            gain+=alpha*(target-gain);
            // Limit the whole current frame, including sudden loud sounds.
            double applied=peak==0?Math.min(gain,1):Math.min(gain,CEILING/peak);
            for(int i=0;i<length;i+=2) {
                int sample=(short)((pcm[i]&255)|(pcm[i+1]<<8));
                int out=(int)Math.round(Math.max(-CEILING,Math.min(CEILING,sample*applied)));
                pcm[i]=(byte)out; pcm[i+1]=(byte)(out>>8);
            }
            gain=applied;
        }
        boolean inputClipped=clipped>=Math.max(2,length/2000);
        clippedFrames=inputClipped?clippedFrames+1:Math.max(0,clippedFrames-1);
        zeroFrames=peak==0?zeroFrames+1:0;
        weakFrames=speech && inputDb<-38?weakFrames+1:Math.max(0,weakFrames-1);
        noisyFrames=speech && noiseDb>-36 && inputDb-noiseDb<8?noisyFrames+1:Math.max(0,noisyFrames-1);
        String state; boolean warning=false;
        if(zeroFrames>=40) {state="输入持续为零 · 若老师正在讲话，请检查麦克风";warning=true;}
        else if(clippedFrames>=5) {state="输入可能失真 · 请避开扬声器或移动手机";warning=true;}
        else if(noisyFrames>=40) {state="疑似背景声音较强 · 建议调整手机位置";warning=true;}
        else if(weakFrames>=50) {state="收音持续偏弱 · 建议将手机移近老师";warning=true;}
        else if(frames<20) state="正在观察收音环境…";
        else if(!speech && inputDb<-60) state="等待讲话 · 收音仍在继续";
        else if(enhance && gain>1.12) state="声音偏小 · 正在温和补偿";
        else if(enhance && gain<0.95) state="音量较高 · 正在限制增益";
        else if(!speech) state="正在收音 · 等待识别";
        else state="收音正常";
        return new Metrics(inputDb,peakDb,noiseDb,20*Math.log10(Math.max(.001,gain)),state,warning);
    }
}
