package com.yeezi.classlive;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.*;
import android.media.audiofx.*;
import java.io.IOException;

/** Shared capture setup so free local tests and live transcription use the same path. */
final class MicrophoneInput implements AutoCloseable {
    final AudioRecord recorder;
    final boolean softwareEnhancement;
    final String description;
    private final AutomaticGainControl agc;
    private final NoiseSuppressor ns;
    private MicrophoneInput(AudioRecord r, boolean software, String text, AutomaticGainControl a, NoiseSuppressor n) {
        recorder=r;softwareEnhancement=software;description=text;agc=a;ns=n;
    }
    static MicrophoneInput open(Context context, int mode, boolean enhance, boolean noiseRequested) throws IOException {
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("microphone permission revoked");
        int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0)throw new IOException("unsupported microphone");
        int[] sources=mode==1
            ?new int[]{MediaRecorder.AudioSource.MIC,MediaRecorder.AudioSource.VOICE_RECOGNITION}
            :new int[]{MediaRecorder.AudioSource.VOICE_RECOGNITION,MediaRecorder.AudioSource.MIC};
        AudioRecord mic=null;String note="";
        for(int source:sources){
            try{
                mic=new AudioRecord(source,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,12800));
                if(mic.getState()==AudioRecord.STATE_INITIALIZED){note=source==MediaRecorder.AudioSource.MIC?"标准麦克风":"语音识别输入";break;}
            }catch(IllegalArgumentException ignored){}
            if(mic!=null)mic.release();mic=null;
        }
        if(mic==null)throw new IOException("microphone initialization");
        AutomaticGainControl agc=null;NoiseSuppressor ns=null;boolean software=enhance;
        // Never enable system gain or stack local gain on an enabled system effect.
        try{if(AutomaticGainControl.isAvailable()){
            agc=AutomaticGainControl.create(mic.getAudioSessionId());
            if(agc!=null&&agc.getEnabled()){software=false;note+=" · 系统增益已启用，软件不叠加";}
        }}catch(Exception ignored){}
        if(noiseRequested){
            try{if(NoiseSuppressor.isAvailable()){
                ns=NoiseSuppressor.create(mic.getAudioSessionId());
                if(ns!=null&&ns.setEnabled(true)==AudioEffect.SUCCESS&&ns.getEnabled())note+=" · 轻降噪已开启";
                else note+=" · 轻降噪不可用，保持原声";
            }else note+=" · 本机无系统降噪，保持原声";
            }catch(Exception ignored){note+=" · 轻降噪未生效，保持原声";}
        }
        return new MicrophoneInput(mic,software,note,agc,ns);
    }
    @Override public void close(){
        try{recorder.stop();}catch(Exception ignored){}
        if(ns!=null)try{ns.release();}catch(Exception ignored){}
        if(agc!=null)try{agc.release();}catch(Exception ignored){}
        recorder.release();
    }
}
