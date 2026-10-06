package com.yeezi.classlive;

/** Local PCM16/16 kHz level control. The speech heuristic is not speaker identification.
 * Analysis filters never alter the uploaded waveform; no samples are gated or discarded.
 * Quiet speech does not require recognition feedback. Noise is learned outside speech,
 * with a hangover to protect weak endings and a peak limit on every output frame.
 */
final class AudioProcessor {
    static final double MAX_GAIN = 3.9810717055349722; // +12 dB
    static final double CEILING = 31128.0; // approximately -0.45 dBFS
    private static final double TARGET_DB = -25;
    private final double[] envelope = new double[12];
    private final double[] slices = new double[10];
    private int position, count;
    private double noiseDb = -65, gain = 1, inputPrevious, highPass, bandPass;
    private double observedMs, quietMs, zeroMs, clippedMs, weakMs, noisyMs;
    private long lastLocalSpeech = Long.MIN_VALUE;
    private boolean noiseKnown;

    static final class Metrics {
        final double rmsDb, outputDb, peakDb, noiseDb, gainDb;
        final boolean speech, warning;
        final String state;
        Metrics(double r, double o, double p, double n, double g, boolean s, String text, boolean w) {
            rmsDb=r; outputDb=o; peakDb=p; noiseDb=n; gainDb=g; speech=s; state=text; warning=w;
        }
    }
    private static double db(double amplitude) { return 20*Math.log10(Math.max(1,amplitude)/32768.0); }
    private static double duration(double before, boolean condition, double elapsed) {
        return condition ? before+elapsed : Math.max(0,before-2*elapsed);
    }
    Metrics process(byte[] pcm, int length, boolean enhance, long nowMs) {
        if(length<=0 || length>pcm.length || length%2!=0) throw new IllegalArgumentException("PCM16 frame length");
        int samples=length/2, peak=0, clipped=0, crossings=0;
        double power=0, bandPower=0, previousBand=bandPass;
        java.util.Arrays.fill(slices,0);
        for(int i=0;i<samples;i++) {
            int sample=(short)((pcm[i*2]&255)|(pcm[i*2+1]<<8));
            power+=(double)sample*sample;
            peak=Math.max(peak,Math.abs(sample));
            if(Math.abs(sample)>=32600) clipped++;
            // Analysis-only 120 Hz high-pass and ~3.8 kHz low-pass at 16 kHz.
            highPass=0.955*(highPass+sample-inputPrevious); inputPrevious=sample;
            bandPass+=0.775*(highPass-bandPass);
            if((bandPass>=0)!=(previousBand>=0))crossings++;
            previousBand=bandPass;
            double energy=bandPass*bandPass;
            bandPower+=energy; slices[Math.min(9,i*10/samples)]+=energy;
        }
        double elapsed=samples/16.0;
        observedMs+=elapsed;
        double inputDb=db(Math.sqrt(power/samples)), peakDb=db(peak);
        double bandDb=db(Math.sqrt(bandPower/samples));
        double withinLow=Double.POSITIVE_INFINITY, withinHigh=0;
        for(double energy:slices){withinLow=Math.min(withinLow,energy); withinHigh=Math.max(withinHigh,energy);}
        double withinSpan=10*Math.log10(Math.max(1,withinHigh)/Math.max(1,withinLow));
        envelope[position]=bandDb; position=(position+1)%envelope.length;
        if(count<envelope.length)count++;
        double low=bandDb, high=bandDb;
        for(int i=0;i<count;i++){low=Math.min(low,envelope[i]); high=Math.max(high,envelope[i]);}
        double crossingHz=crossings*16000.0/samples;
        boolean inVoiceBand=power>0 && bandPower/power>=0.35 && crossingHz>=70 && crossingHz<=6500;
        boolean modulated=withinSpan>=4 || (count>=3 && high-low>=4.5);
        boolean impulsive=peakDb-inputDb>18;
        boolean localSpeech=observedMs>=300 && inputDb>-58 && bandDb>noiseDb+6
                && inVoiceBand && modulated && !impulsive;
        if(localSpeech)lastLocalSpeech=nowMs;
        boolean recentSpeech=lastLocalSpeech!=Long.MIN_VALUE && nowMs>=lastLocalSpeech && nowMs-lastLocalSpeech<1600;
        // The lower level check stops hangover from treating real silence as speech.
        boolean speech=recentSpeech && inVoiceBand && inputDb>-58 && bandDb>noiseDb+3 && !impulsive;
        quietMs=speech?0:quietMs+elapsed;
        if(!speech && !impulsive && quietMs>=600) {
            double measured=Math.max(-85,bandDb);
            if(!noiseKnown){noiseDb=measured; noiseKnown=true;}
            else {
                // Downward updates are faster; a transient cannot instantly raise the floor.
                double tau=measured<noiseDb?500:8000;
                noiseDb+=(measured-noiseDb)*(1-Math.exp(-elapsed/tau));
            }
        }

        double target=1;
        if(enhance && peak>0) {
            if(peak>CEILING)target=CEILING/peak;
            else if(speech)target=Math.min(MAX_GAIN,Math.max(1,Math.pow(10,(TARGET_DB-inputDb)/20)));
        }
        double before=gain;
        if(!enhance)gain=1;
        else {
            double tau=target<gain?180:700;
            gain+=(target-gain)*(1-Math.exp(-elapsed/tau));
            double cap=peak==0?1:CEILING/peak;
            gain=Math.min(gain,cap);
            // Smooth block boundaries. Both ends obey the current frame's peak limit.
            double start=Math.min(before,cap), outputPower=0;
            for(int i=0;i<samples;i++) {
                int sample=(short)((pcm[i*2]&255)|(pcm[i*2+1]<<8));
                double multiplier=start+(gain-start)*(i+1.0)/samples;
                int out=(int)Math.round(Math.max(-CEILING,Math.min(CEILING,sample*multiplier)));
                pcm[i*2]=(byte)out; pcm[i*2+1]=(byte)(out>>8);
                outputPower+=(double)out*out;
            }
            power=outputPower;
        }
        double outputDb=db(Math.sqrt(power/samples));
        zeroMs=peak==0?zeroMs+elapsed:0;
        clippedMs=duration(clippedMs,clipped>=Math.max(2,samples/200),elapsed);
        weakMs=duration(weakMs,speech && outputDb<-38,elapsed);
        noisyMs=duration(noisyMs,speech && noiseKnown && noiseDb>-36 && bandDb-noiseDb<6,elapsed);
        String state; boolean warning=false;
        if(zeroMs>=4000){state="输入持续为零 · 若老师正在讲话，请检查麦克风";warning=true;}
        else if(clippedMs>=500){state="原始声音持续失真 · 请避开扬声器或移动手机";warning=true;}
        else if(weakMs>=8000){state=enhance?"补偿后仍持续偏弱 · 可尝试另一收音模式":"声音持续偏弱 · 可开启本地音量优化";warning=true;}
        else if(noisyMs>=8000){state="疑似背景声音较强 · 可试系统轻降噪";warning=true;}
        else if(observedMs<600)state="正在本地观察收音…";
        else if(enhance && gain<0.95)state="音量较高 · 正在限制增益";
        else if(speech && enhance && gain>1.12)state="声音偏小 · 正在自动补偿";
        else if(speech)state="检测到疑似讲话 · 收音继续";
        else state="等待讲话 · 收音仍在继续";
        return new Metrics(inputDb,outputDb,peakDb,noiseDb,20*Math.log10(Math.max(.001,gain)),speech,state,warning);
    }
}
