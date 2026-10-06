package com.yeezi.classlive;
import java.util.*;
/** Signal-level checks and export completeness checks; no network or Android runtime. */
public final class CoreTests {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static byte[] sine(int peak){byte[] b=new byte[3200];for(int i=0;i<1600;i++){int n=(int)Math.round(peak*Math.sin(2*Math.PI*240*i/16000));b[2*i]=(byte)n;b[2*i+1]=(byte)(n>>8);}return b;}
    static int peak(byte[] b){int p=0;for(int i=0;i<b.length;i+=2)p=Math.max(p,Math.abs((short)((b[i]&255)|(b[i+1]<<8))));return p;}
    static byte[] speech(int scale,int frame){
        byte[] b=new byte[3200];
        for(int i=0;i<1600;i++){
            double t=(frame*1600L+i)/16000.0;
            double phase=2*Math.PI*(180*t+2*Math.sin(2*Math.PI*1.3*t));
            double voice=.5*Math.sin(phase)+.25*Math.sin(2*phase)+.15*Math.sin(4*phase)+.1*Math.sin(7*phase);
            double envelope=.75+.25*Math.sin(2*Math.PI*2.8*t);
            int n=(int)Math.round(scale*envelope*voice);b[2*i]=(byte)n;b[2*i+1]=(byte)(n>>8);
        }return b;
    }
    static void noEnhancementIsBitExact(){AudioProcessor p=new AudioProcessor();byte[] b=sine(26000),copy=b.clone();p.process(b,b.length,false,100);check(Arrays.equals(b,copy),"disabled processing altered input");}
    static void silenceIsNotBoosted(){AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;for(int i=0;i<80;i++){byte[] b=new byte[3200];m=p.process(b,b.length,true,i*100);check(peak(b)==0,"silence generated sound");}check(m.state.contains("输入持续为零"),"zero input did not trigger diagnostic");check(Math.abs(m.gainDb)<.01,"gain rose in silence");}
    static void quietSpeechGetsLimitedBoost(){AudioProcessor p=new AudioProcessor();for(int i=0;i<20;i++){byte[] b=sine(40);p.process(b,b.length,true,i*100);}AudioProcessor.Metrics m=null;for(int i=0;i<50;i++){byte[] b=speech(900,i);int original=peak(b);m=p.process(b,b.length,true,2000+i*100);check(peak(b)<=Math.ceil(original*AudioProcessor.MAX_GAIN),"boost exceeded 12 dB");}check(m.gainDb>8,"quiet speech was not boosted without ASR feedback");check(m.gainDb<=12.001,"gain exceeded ceiling");}
    static void suddenLoudnessDoesNotClip(){AudioProcessor p=new AudioProcessor();for(int i=0;i<50;i++){byte[] b=speech(900,i);p.process(b,b.length,true,i*100);}byte[] loud=sine(32767);AudioProcessor.Metrics m=p.process(loud,loud.length,true,5000);check(peak(loud)<=31128,"loud transition introduced clipping");check(m.gainDb<0,"gain was not immediately bounded");}
    static void isolatedBumpDoesNotWarn(){AudioProcessor p=new AudioProcessor();for(int i=0;i<30;i++){byte[] b=sine(100);p.process(b,b.length,true,i*100);}byte[] bump=new byte[3200];bump[0]=(byte)255;bump[1]=127;AudioProcessor.Metrics m=p.process(bump,bump.length,true,3100);check(!m.warning,"single bump caused warning");}
    static void stationaryBackgroundSettles(){AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;Random rng=new Random(131);for(int i=0;i<150;i++){byte[] b=new byte[3200];for(int j=0;j<1600;j++){int n=rng.nextInt(1201)-600;b[j*2]=(byte)n;b[j*2+1]=(byte)(n>>8);}m=p.process(b,b.length,true,i*100);}check(m.gainDb<.1,"steady noise remained amplified");check(!m.warning,"steady noise triggered speech warning");}
    static void continuousSpeechDoesNotBecomeNoise(){AudioProcessor p=new AudioProcessor();for(int i=0;i<30;i++){byte[] b=sine(30);p.process(b,b.length,true,i*100);}AudioProcessor.Metrics m=null;double floor=0;for(int i=0;i<300;i++){byte[] b=speech(1200,i);m=p.process(b,b.length,true,3000+i*100);if(i==30)floor=m.noiseDb;}check(m.noiseDb<=floor+1,"continuous speech raised background floor");check(m.gainDb>8,"continuous speech lost gain");}
    static void speechAtStartupGetsBoosted(){AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;for(int i=0;i<40;i++){byte[] b=speech(900,i);m=p.process(b,b.length,true,i*100);}check(m.gainDb>8,"startup speech required a quiet calibration or recognition");}
    static void effectiveBoostClearsWeakWarning(){AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;for(int i=0;i<200;i++){byte[] b=speech(900,i);m=p.process(b,b.length,true,i*100);if(i>30)check(!m.warning,"usefully enhanced speech kept weak warning");}check(m.outputDb-m.rmsDb>8,"output level did not reflect enhancement");}
    static void pauseReleasesGainWithoutDiscardingAudio(){AudioProcessor p=new AudioProcessor();for(int i=0;i<50;i++){byte[] b=speech(900,i);p.process(b,b.length,true,i*100);}AudioProcessor.Metrics m=null;for(int i=0;i<50;i++){byte[] b=sine(20);m=p.process(b,b.length,true,5000+i*100);check(peak(b)>0,"quiet audio was gated");}check(m.gainDb<.1,"pause kept pumping background gain");}
    static void disablingAfterBoostIsBitExact(){AudioProcessor p=new AudioProcessor();for(int i=0;i<50;i++){byte[] b=speech(900,i);p.process(b,b.length,true,i*100);}byte[] b=speech(900,50),copy=b.clone();AudioProcessor.Metrics m=p.process(b,b.length,false,5000);check(Arrays.equals(b,copy),"turning enhancement off altered PCM");check(Math.abs(m.outputDb-m.rmsDb)<.0001,"disabled output level was inaccurate");}
    static void genuinelyWeakOutputStillWarns(){AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;for(int i=0;i<250;i++){byte[] b=speech(230,i);m=p.process(b,b.length,true,i*100);}check(m.warning&&m.state.contains("补偿后仍"),"persistently weak output lost diagnostic");}
    static void weakUnenhancedSpeechWarns(){AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;for(int i=0;i<200;i++){byte[] b=speech(900,i);m=p.process(b,b.length,false,i*100);}check(m.warning&&m.state.contains("可开启"),"disabled enhancement lost weak-input diagnostic");}
    static void negativeFullScaleDoesNotOverflow(){AudioProcessor p=new AudioProcessor();byte[] b=new byte[3200];for(int i=0;i<1600;i++){b[i*2]=0;b[i*2+1]=(byte)128;}AudioProcessor.Metrics m=p.process(b,b.length,true,0);check(peak(b)<=31128,"negative full-scale sample overflowed limiter");check(Double.isFinite(m.outputDb),"negative full-scale measurement was invalid");}
    static void exportsContainAllRecordsAndHonestMarkers(){Lecture l=new Lecture("abc","Mission / Design",1000);l.recordedMs=61000;for(int i=0;i<200;i++){Lecture.Line r=new Lecture.Line("r"+i,"sentence "+i,"12:00:00",i==199,i==198);if(i<198)r.zh="译文 "+i;l.lines.add(r);}l.lines.get(199).pending=true;l.draft="unfinished end";l.draftTime="12:05:00";String all=l.export(Lecture.ExportMode.BILINGUAL);check(all.contains("sentence 0")&&all.contains("sentence 199"),"export omitted offscreen records");check(all.contains("[翻译尚未完成]")&&all.contains("[未确认片段]")&&all.contains("[识别可能不准]"),"export lost status markers");check(all.contains("unfinished end"),"export lost current partial");check(!l.export(Lecture.ExportMode.ENGLISH).contains("中文:"),"English export contains Chinese translation rows");check(!l.export(Lecture.ExportMode.CHINESE).contains("EN:"),"Chinese export contains English rows");check(!l.filename(Lecture.ExportMode.BILINGUAL).contains("/"),"unsafe export filename");}
    static void longMixedSignalStaysBounded(){
        AudioProcessor p=new AudioProcessor();Random rng=new Random(923);
        for(int frame=0;frame<3000;frame++){
            byte[] b=new byte[3200];int scale=(frame%300<100)?300:(frame%300<200)?12000:32768;
            for(int i=0;i<1600;i++){int n=rng.nextInt(scale*2)-scale;b[2*i]=(byte)n;b[2*i+1]=(byte)(n>>8);}
            AudioProcessor.Metrics m=p.process(b,b.length,true,frame*100L);
            check(peak(b)<=31128,"mixed audio clipped at frame "+frame);
            check(Double.isFinite(m.outputDb)&&Double.isFinite(m.noiseDb)&&Double.isFinite(m.gainDb)&&m.gainDb<=12.001,"invalid measurement at frame "+frame);
        }
    }
    static void sustainedClippingWarns(){
        AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;
        for(int frame=0;frame<8;frame++){byte[] b=new byte[3200];for(int i=0;i<1600;i++){b[i*2]=(byte)255;b[i*2+1]=127;}m=p.process(b,b.length,true,frame*100L);}
        check(m.warning&&m.state.contains("失真"),"sustained input clipping not reported");
    }
    public static void main(String[] args){noEnhancementIsBitExact();silenceIsNotBoosted();quietSpeechGetsLimitedBoost();suddenLoudnessDoesNotClip();isolatedBumpDoesNotWarn();stationaryBackgroundSettles();continuousSpeechDoesNotBecomeNoise();speechAtStartupGetsBoosted();effectiveBoostClearsWeakWarning();pauseReleasesGainWithoutDiscardingAudio();disablingAfterBoostIsBitExact();genuinelyWeakOutputStillWarns();weakUnenhancedSpeechWarns();negativeFullScaleDoesNotOverflow();exportsContainAllRecordsAndHonestMarkers();longMixedSignalStaysBounded();sustainedClippingWarns();System.out.println("PASS: 17 signal/export scenarios, including 5 minutes of mixed PCM");}
}
