package com.yeezi.classlive;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.*;
import android.os.*;
import android.view.WindowManager;
import android.widget.*;
import java.io.IOException;
import java.util.Locale;

/** An entirely local, foreground-only A/B microphone check. Never writes or uploads audio. */
public final class AudioCheckActivity extends Activity {
    private static final int TEST_BYTES=16000*2*15;
    private final Handler main=new Handler(Looper.getMainLooper());
    private TextView status,levels,path;
    private Button start,raw,processed,stop;
    private boolean foreground,pendingPermission;
    private byte[] rawAudio,processedAudio;
    private Session session;
    private AudioTrack player;
    private static final class Session {
        volatile boolean active=true;
        volatile MicrophoneInput input;
        final byte[] raw=new byte[TEST_BYTES],processed=new byte[TEST_BYTES];
    }
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(239,244,250));t.setPadding(0,dp(8),0,dp(8));return t;}
    private Button button(String title,LinearLayout parent){Button b=new Button(this);b.setText(title);b.setAllCaps(false);parent.addView(b);return b;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(12,18,32));
        root.setOnApplyWindowInsetsListener((v,i)->{root.setPadding(dp(20)+i.getSystemWindowInsetLeft(),dp(12)+i.getSystemWindowInsetTop(),dp(20)+i.getSystemWindowInsetRight(),dp(12)+i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});
        ScrollView wrapper=new ScrollView(this);wrapper.addView(root);setContentView(wrapper);
        root.addView(text("本地收音测试",23));
        root.addView(text("录制 15 秒英语讲话，再分别回听原声和处理后声音。可让电脑播放同一段英语，在同一位置比较不同输入模式。",15));
        root.addView(text("不调用 API、不上传、不保存音频文件。回听不会额外统一音量，方便直接比较。测试期间请保持此页面；返回或锁屏会停止收音和播放。",13));
        path=text("使用设置页面刚才选择的收音选项",12);root.addView(path);
        status=text("准备就绪 · 点击开始测试",15);root.addView(status);
        levels=text("录制期间显示原始和输出音量",13);root.addView(levels);
        start=button("开始 15 秒测试",root);raw=button("回听原声",root);processed=button("回听处理后声音",root);stop=button("停止录音或回听",root);
        Button back=button("返回设置",root);
        raw.setEnabled(false);processed.setEnabled(false);stop.setEnabled(false);
        start.setOnClickListener(v->begin());raw.setOnClickListener(v->play(rawAudio,"原声"));processed.setOnClickListener(v->play(processedAudio,"处理后声音"));
        stop.setOnClickListener(v->{if(session!=null){cancelRecording();status.setText("测试已中断 · 点击开始重新录制");}else{stopPlayback();status.setText("回听已停止 · 可再次回听或重新测试");}});
        back.setOnClickListener(v->finish());
    }
    @Override protected void onResume(){super.onResume();foreground=true;if(pendingPermission){pendingPermission=false;begin();}}
    @Override protected void onPause(){
        foreground=false;boolean interrupted=session!=null;cancelRecording();stopPlayback();
        if(interrupted)status.setText("测试已中断 · 点击开始重新录制");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);super.onPause();
    }
    @Override protected void onDestroy(){rawAudio=null;processedAudio=null;main.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==71){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED){if(foreground)begin();else pendingPermission=true;}
            else status.setText("需要麦克风权限 · 可在系统设置中允许");}
    }
    private void begin(){
        if(!foreground||session!=null)return;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},71);return;}
        stopPlayback();rawAudio=null;processedAudio=null;
        Session s=new Session();session=s;
        int mode=getIntent().getIntExtra("micMode",0);boolean enhance=getIntent().getBooleanExtra("enhance",true),noise=getIntent().getBooleanExtra("lightNoise",false);
        start.setEnabled(false);raw.setEnabled(false);processed.setEnabled(false);stop.setEnabled(true);
        status.setText("正在初始化麦克风…");getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        new Thread(()->record(s,mode,enhance,noise),"classlive-local-audio-check").start();
    }
    private void record(Session s,int mode,boolean enhance,boolean noise){
        MicrophoneInput input=null;String error="";int kept=0,filled=0;byte[] chunk=new byte[3200];
        try{
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("microphone permission");
            input=MicrophoneInput.open(this,mode,enhance,noise);s.input=input;
            if(!s.active)return;
            final String note=input.description;main.post(()->{if(session==s&&foreground)path.setText(note);});
            input.recorder.startRecording();AudioProcessor processor=new AudioProcessor();long began=SystemClock.elapsedRealtime();
            while(s.active&&kept<TEST_BYTES){
                if(SystemClock.elapsedRealtime()-began>25000)throw new IOException("microphone timeout");
                int n=input.recorder.read(chunk,filled,chunk.length-filled);
                if(n<0){if(!s.active)break;throw new IOException("microphone read");}
                if(n==0)continue;filled+=n;
                if(filled==chunk.length){
                    System.arraycopy(chunk,0,s.raw,kept,chunk.length);
                    AudioProcessor.Metrics m=processor.process(chunk,chunk.length,input.softwareEnhancement,SystemClock.elapsedRealtime());
                    System.arraycopy(chunk,0,s.processed,kept,chunk.length);kept+=chunk.length;filled=0;
                    final int seconds=kept/32000;
                    if(kept%9600==0||kept==TEST_BYTES)main.post(()->{if(session==s&&foreground){
                        status.setText("正在录制 "+seconds+" / 15 秒 · "+m.state);
                        levels.setText(String.format(Locale.UK,"原始 %.0f → 输出 %.0f dBFS\n增益 %+.1f dB",m.rmsDb,m.outputDb,m.gainDb));
                    }});
                }
            }
        }catch(SecurityException e){error="麦克风权限不可用 · 请在系统设置中允许后重试";}
        catch(Exception e){error="收音测试失败 · 请检查麦克风是否被占用，或换一种输入模式";}
        finally{if(input!=null)input.close();s.input=null;}
        final boolean complete=s.active&&kept==TEST_BYTES;final String result=error;
        main.post(()->{if(session!=s||!foreground)return;session=null;start.setEnabled(true);stop.setEnabled(false);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if(complete){rawAudio=s.raw;processedAudio=s.processed;raw.setEnabled(true);processed.setEnabled(true);status.setText("测试完成 · 分别回听，比较清晰度、轻声和句尾");}
            else status.setText(result.isEmpty()?"测试中断 · 点击开始重新录制":result);
        });
    }
    private void cancelRecording(){
        Session s=session;session=null;
        if(s!=null){s.active=false;MicrophoneInput input=s.input;if(input!=null)try{input.recorder.stop();}catch(Exception ignored){}start.setEnabled(true);stop.setEnabled(false);}
    }
    private void play(byte[] data,String name){
        if(!foreground||session!=null||data==null||data.length==0)return;stopPlayback();
        AudioTrack track=null;
        try{
            track=new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(16000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(data.length).build();
            if(track.getState()==AudioTrack.STATE_UNINITIALIZED||track.write(data,0,data.length)!=data.length)throw new IOException("playback setup");
            player=track;final AudioTrack active=track;
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener(){
                @Override public void onMarkerReached(AudioTrack t){if(player==active){stopPlayback();status.setText("回听结束 · 可比较另一段声音");}}
                @Override public void onPeriodicNotification(AudioTrack t){}
            },main);
            track.setNotificationMarkerPosition(data.length/2);track.play();stop.setEnabled(true);status.setText("正在回听"+name+" · 使用手机媒体音量调节");
        }catch(Exception e){if(player==track)stopPlayback();else if(track!=null)track.release();status.setText("回听失败 · 可尝试重新录制");}
    }
    private void stopPlayback(){AudioTrack track=player;player=null;if(track!=null){try{track.stop();}catch(Exception ignored){}track.release();}if(stop!=null&&session==null)stop.setEnabled(false);}
}
