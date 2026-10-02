package com.yeezi.classlive;
import java.util.*;
/** Signal-level checks and export completeness checks; no network or Android runtime. */
public final class CoreTests {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static byte[] sine(int peak){byte[] b=new byte[3200];for(int i=0;i<1600;i++){int n=(int)Math.round(peak*Math.sin(2*Math.PI*240*i/16000));b[2*i]=(byte)n;b[2*i+1]=(byte)(n>>8);}return b;}
    static int peak(byte[] b){int p=0;for(int i=0;i<b.length;i+=2)p=Math.max(p,Math.abs((short)((b[i]&255)|(b[i+1]<<8))));return p;}
    static void noEnhancementIsBitExact(){AudioProcessor p=new AudioProcessor();byte[] b=sine(26000),copy=b.clone();p.noteSpeech(0);p.process(b,b.length,false,100);check(Arrays.equals(b,copy),"disabled processing altered input");}
    static void silenceIsNotBoosted(){AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;for(int i=0;i<80;i++){byte[] b=new byte[3200];m=p.process(b,b.length,true,i*100);check(peak(b)==0,"silence generated sound");}check(m.state.contains("输入持续为零"),"zero input did not trigger diagnostic");check(Math.abs(m.gainDb)<.01,"gain rose in silence");}
    static void quietSpeechGetsLimitedBoost(){AudioProcessor p=new AudioProcessor();for(int i=0;i<80;i++){byte[] b=sine(50);p.process(b,b.length,true,i*100);}AudioProcessor.Metrics m=null;for(int i=0;i<35;i++){long t=8000+i*100;p.noteSpeech(t);byte[] b=sine(650);m=p.process(b,b.length,true,t);check(peak(b)<=1300,"boost exceeded 6 dB");}check(m.gainDb>1,"quiet confirmed speech was not boosted");check(m.gainDb<=6.001,"gain exceeded ceiling");}
    static void suddenLoudnessDoesNotClip(){AudioProcessor p=new AudioProcessor();for(int i=0;i<80;i++){byte[] b=sine(40);p.process(b,b.length,true,i*100);}for(int i=0;i<30;i++){long t=8000+i*100;p.noteSpeech(t);byte[] b=sine(700);p.process(b,b.length,true,t);}byte[] loud=sine(32767);AudioProcessor.Metrics m=p.process(loud,loud.length,true,11000);check(peak(loud)<=31128,"loud transition introduced clipping");check(m.gainDb<0,"gain was not immediately bounded");}
    static void isolatedBumpDoesNotWarn(){AudioProcessor p=new AudioProcessor();for(int i=0;i<30;i++){byte[] b=sine(100);p.process(b,b.length,true,i*100);}byte[] bump=new byte[3200];bump[0]=(byte)255;bump[1]=127;AudioProcessor.Metrics m=p.process(bump,bump.length,true,3100);check(!m.warning,"single bump caused warning");}
    static void backgroundWithoutSpeechIsNotBoosted(){AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;for(int i=0;i<100;i++){byte[] b=sine(900);m=p.process(b,b.length,true,i*100);}check(Math.abs(m.gainDb)<.01,"unrecognized background was amplified");}
    static void exportsContainAllRecordsAndHonestMarkers(){Lecture l=new Lecture("abc","Mission / Design",1000);l.recordedMs=61000;for(int i=0;i<200;i++){Lecture.Line r=new Lecture.Line("r"+i,"sentence "+i,"12:00:00",i==199,i==198);if(i<198)r.zh="译文 "+i;l.lines.add(r);}l.lines.get(199).pending=true;l.draft="unfinished end";l.draftTime="12:05:00";String all=l.export(Lecture.ExportMode.BILINGUAL);check(all.contains("sentence 0")&&all.contains("sentence 199"),"export omitted offscreen records");check(all.contains("[翻译尚未完成]")&&all.contains("[未确认片段]")&&all.contains("[识别可能不准]"),"export lost status markers");check(all.contains("unfinished end"),"export lost current partial");check(!l.export(Lecture.ExportMode.ENGLISH).contains("中文:"),"English export contains Chinese translation rows");check(!l.export(Lecture.ExportMode.CHINESE).contains("EN:"),"Chinese export contains English rows");check(!l.filename(Lecture.ExportMode.BILINGUAL).contains("/"),"unsafe export filename");}
    static void longMixedSignalStaysBounded(){
        AudioProcessor p=new AudioProcessor();Random rng=new Random(923);
        for(int frame=0;frame<3000;frame++){
            byte[] b=new byte[3200];int scale=(frame%300<100)?300:(frame%300<200)?12000:32768;
            for(int i=0;i<1600;i++){int n=rng.nextInt(scale*2)-scale;b[2*i]=(byte)n;b[2*i+1]=(byte)(n>>8);}
            if(frame%20<10)p.noteSpeech(frame*100L);
            AudioProcessor.Metrics m=p.process(b,b.length,true,frame*100L);
            check(peak(b)<=31128,"mixed audio clipped at frame "+frame);
            check(!Double.isNaN(m.gainDb)&&m.gainDb<=6.001,"invalid gain at frame "+frame);
        }
    }
    static void sustainedClippingWarns(){
        AudioProcessor p=new AudioProcessor();AudioProcessor.Metrics m=null;
        for(int frame=0;frame<8;frame++){byte[] b=new byte[3200];for(int i=0;i<1600;i++){b[i*2]=(byte)255;b[i*2+1]=127;}m=p.process(b,b.length,true,frame*100L);}
        check(m.warning&&m.state.contains("失真"),"sustained input clipping not reported");
    }
    public static void main(String[] args){noEnhancementIsBitExact();silenceIsNotBoosted();quietSpeechGetsLimitedBoost();suddenLoudnessDoesNotClip();isolatedBumpDoesNotWarn();backgroundWithoutSpeechIsNotBoosted();exportsContainAllRecordsAndHonestMarkers();longMixedSignalStaysBounded();sustainedClippingWarns();System.out.println("PASS: 9 signal/export scenarios, including 5 minutes of mixed PCM");}
}
