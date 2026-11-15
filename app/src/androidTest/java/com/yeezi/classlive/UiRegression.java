package com.yeezi.classlive;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Runs in a clean emulator; does not invoke ASR or paid translation APIs. */
public final class UiRegression extends Instrumentation {
    MainActivity activity; int checks; String suffix="";
    interface Task {void run() throws Exception;}
    void ui(Task task){final Throwable[] error={null};runOnMainSync(()->{try{task.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new RuntimeException(error[0]);}
    Object field(Object obj,String name)throws Exception{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    Object call(String name,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,args);}
    void call(String name)throws Exception{call(name,new Class<?>[0]);}
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    void settle(){waitForIdleSync();SystemClock.sleep(350);waitForIdleSync();}
    void shot(String name)throws Exception{settle();Bitmap b=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getExternalFilesDir(null),"qa");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+suffix+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    @Override public void onCreate(Bundle args){super.onCreate(args);suffix=args.getString("suffix","");start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        ui(()->{Lecture l=(Lecture)field(activity,"current");l.title="UI / UX Design";l.lines.clear();for(int i=0;i<14;i++){Lecture.Line r=new Lecture.Line("qa-"+i,"Keep the most important information visible, and make the next action easy to find.","09:"+String.format(Locale.UK,"%02d",i),false,false);r.zh=i==13?"翻译中…":"让重要的信息保持可见，让玩家能够轻松找到下一步操作。";l.lines.add(r);}call("showLecture");});
        settle();ui(()->{Lecture l=(Lecture)field(activity,"current");Lecture.Line last=l.lines.get(13);last.zh="当玩家阅读界面时，最重要的信息应保持可见。最后一句中文翻译会在英文之后返回，即使翻译比原文更长，也应完整地出现在正在收听区域的上方。";call("refresh",new Class<?>[]{Lecture.Line.class},last);});settle();
        ui(()->{ScrollView scroll=(ScrollView)field(activity,"scroll");LinearLayout list=(LinearLayout)field(activity,"list");TextView partial=(TextView)field(activity,"partial");int[] s=new int[2],p=new int[2];scroll.getLocationOnScreen(s);partial.getLocationOnScreen(p);check(s[1]+scroll.getHeight()<=p[1],"list viewport overlaps listening dock");check(list.getHeight()-scroll.getScrollY()<=scroll.getHeight()+2,"late translation did not follow to bottom");check(scroll.getHeight()>80,"transcript viewport too small");});shot("01-live");
        ui(()->{((Switch)field(activity,"follow")).setChecked(false);((ScrollView)field(activity,"scroll")).scrollTo(0,0);Lecture l=(Lecture)field(activity,"current");l.lines.get(13).zh+=" 手动回看时不应强制跳到底部。";call("refresh",new Class<?>[]{Lecture.Line.class},l.lines.get(13));});settle();ui(()->check(((ScrollView)field(activity,"scroll")).getScrollY()==0,"manual reading moved"));
        ui(()->((Switch)field(activity,"follow")).setChecked(true));settle();ui(()->call("history"));shot("02-records");ui(()->call("settings"));shot("03-settings");
        for(int i=0;i<3;i++){final int section=i;ui(()->call("preferences",new Class<?>[]{int.class},section));shot("04-settings-"+i);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();}
        ui(()->call("modelOptions"));shot("05-models-empty");
        // Sparse files exercise management without downloading hundreds of megabytes.
        for(ModelCatalog.Spec spec:new ModelCatalog.Spec[]{ModelCatalog.ALL[0],ModelCatalog.ALL[1]})try(RandomAccessFile file=new RandomAccessFile(OfflineModel.file(activity,spec),"rw")){file.setLength(spec.size);}
        ui(()->{activity.getSharedPreferences("settings",0).edit().putString("offlineModel","Q5_K_M").commit();call("modelOptions");});shot("06-models-installed");
        ui(()->call("deleteModel",new Class<?>[]{ModelCatalog.Spec.class},ModelCatalog.ALL[1]));shot("07-delete-confirm");clickText("删除模型");settle();check(!OfflineModel.file(activity,ModelCatalog.ALL[1]).exists(),"delete removes file");check(OfflineModel.selected(activity).id.equals("Q4_K_M"),"delete selects installed fallback");ui(()->check(((Lecture)field(activity,"current")).lines.size()==14,"delete changed classroom records"));
        ui(()->call("deleteModel",new Class<?>[]{ModelCatalog.Spec.class},ModelCatalog.ALL[0]));settle();clickText("删除模型");settle();check(!OfflineModel.installed(activity),"last deletion blocks ASR until install");
        ui(()->call("about"));shot("08-about");
        Activity audio=startActivitySync(new Intent(getTargetContext(),AudioCheckActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));shot("09-microphone");ui(audio::finish);
        result.putString("stream","CLASSLIVE_UI_PASS "+checks+" checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{shot("failure");}catch(Exception ignored){}result.putString("stream","CLASSLIVE_UI_FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
    void clickText(String text){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(text);boolean clicked=false;for(AccessibilityNodeInfo n:nodes)if(text.contentEquals(n.getText()==null?"":n.getText())){clicked=n.performAction(AccessibilityNodeInfo.ACTION_CLICK);if(clicked)break;}check(clicked,"button not clickable: "+text);}
}
