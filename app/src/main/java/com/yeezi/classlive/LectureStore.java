package com.yeezi.classlive;
import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Files are app-private JSON records. TXT is generated only by an explicit export. */
final class LectureStore {
    private final File directory;
    private final ExecutorService writer=Executors.newSingleThreadExecutor();
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    interface Failure { void failed(); }
    LectureStore(Context context) throws IOException {
        directory=new File(context.getFilesDir(),"lectures");
        if(!directory.isDirectory() && !directory.mkdirs())throw new IOException("directory");
    }
    static JSONObject encode(Lecture l) throws JSONException {
        JSONObject o=new JSONObject().put("schema",2).put("id",l.id).put("title",l.title).put("createdAt",l.createdAt)
            .put("recordedMs",l.recordedMs).put("draft",l.draft).put("draftTime",l.draftTime);
        JSONArray a=new JSONArray();
        for(Lecture.Line r:l.lines) a.put(new JSONObject().put("id",r.id).put("en",r.en).put("zh",r.zh).put("time",r.time)
            .put("unconfirmed",r.unconfirmed).put("lowConfidence",r.lowConfidence).put("error",r.error));
        return o.put("lines",a);
    }
    static Lecture decode(JSONObject o) throws JSONException {
        String id=o.getString("id"); if(!id.matches("[a-zA-Z0-9-]+"))throw new JSONException("id");
        Lecture l=new Lecture(id,o.getString("title"),o.getLong("createdAt"));
        l.recordedMs=o.optLong("recordedMs");l.draft=o.optString("draft");l.draftTime=o.optString("draftTime");
        JSONArray a=o.getJSONArray("lines");
        for(int i=0;i<a.length();i++) {
            JSONObject r=a.getJSONObject(i);
            Lecture.Line line=new Lecture.Line(r.optString("id",UUID.randomUUID().toString()),r.getString("en"),r.optString("time"),r.optBoolean("unconfirmed"),r.optBoolean("lowConfidence"));
            line.zh=r.optString("zh");line.error=r.optString("error");l.lines.add(line);
        }
        return l;
    }
    void save(Lecture lecture,Failure failure) {
        try {
            final byte[] bytes=encode(lecture).toString().getBytes("UTF-8");final String id=lecture.id;
            writer.execute(()->{
                AtomicFile f=new AtomicFile(new File(directory,id+".json"));FileOutputStream out=null;
                try {out=f.startWrite();out.write(bytes);f.finishWrite(out);}
                catch(Exception e){if(out!=null)f.failWrite(out);main.post(failure::failed);}
            });
        }catch(Exception e){main.post(failure::failed);}
    }
    Lecture read(String id) throws Exception {
        if(id==null||!id.matches("[a-zA-Z0-9-]+"))return null;
        File f=new File(directory,id+".json");if(!f.exists()&&!new File(f+".bak").exists())return null;
        return decode(new JSONObject(readText(new AtomicFile(f))));
    }
    List<Lecture> all() {
        List<Lecture> list=new ArrayList<>();File[] files=directory.listFiles();if(files==null)return list;
        Set<String> ids=new HashSet<>();
        for(File f:files) {
            String name=f.getName();if(name.endsWith(".json.bak"))name=name.substring(0,name.length()-4);
            if(!name.endsWith(".json"))continue;
            String id=name.substring(0,name.length()-5);if(!ids.add(id))continue;
            try {Lecture l=read(id);if(l!=null)list.add(l);}catch(Exception ignored){}
        }
        list.sort((a,b)->Long.compare(b.createdAt,a.createdAt));return list;
    }
    private static String readText(AtomicFile f) throws Exception {
        try(InputStream in=f.openRead();ByteArrayOutputStream b=new ByteArrayOutputStream()) {
            byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);return b.toString("UTF-8");
        }
    }
    Lecture legacy(Context c) throws Exception {
        File old=new File(c.getFilesDir(),"class.json");
        if(!old.exists()&&!new File(old+".bak").exists())return null;
        JSONArray a=new JSONArray(readText(new AtomicFile(old)));if(a.length()==0)return null;
        return importLegacy(a,old.lastModified()>0?old.lastModified():System.currentTimeMillis());
    }
    static Lecture importLegacy(JSONArray a,long createdAt) throws JSONException {
        Lecture l=new Lecture("legacy-v01","之前的课堂",createdAt);
        for(int i=0;i<a.length();i++) {
            JSONObject r=a.getJSONObject(i);Lecture.Line line=new Lecture.Line(UUID.randomUUID().toString(),r.getString("en"),r.optString("time"),false,false);
            line.zh=r.optString("zh");l.lines.add(line);
        }
        return l;
    }
    void shutdown(){writer.shutdown();}
}
