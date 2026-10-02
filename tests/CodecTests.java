package com.yeezi.classlive;
import org.json.*;
public final class CodecTests {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Lecture l=new Lecture("id-123","游戏设计课",1710000000000L);l.recordedMs=75000;l.draft="unfinished sentence";l.draftTime="10:01:00";
        for(int i=0;i<250;i++){Lecture.Line r=new Lecture.Line("line-"+i,"English "+i,"10:00:00",i==0,i==1);r.zh=i<249?"中文 "+i:"";r.error=i==249?"network":"";r.pending=i==249;l.lines.add(r);}
        String saved=LectureStore.encode(l).toString();Lecture restored=LectureStore.decode(new JSONObject(saved));
        check(restored.lines.size()==250,"persistence lost offscreen lines");check(restored.title.equals(l.title)&&restored.recordedMs==75000,"lecture metadata changed");check(restored.draft.equals(l.draft),"crash recovery lost partial");
        check(restored.lines.get(0).unconfirmed&&restored.lines.get(1).lowConfidence,"quality flags lost");check(restored.lines.get(248).zh.equals("中文 248"),"Unicode translation lost");check(!restored.lines.get(249).pending&&restored.lines.get(249).zh.isEmpty(),"restored pending request cannot be retried");
        JSONArray old=new JSONArray("[{\"en\":\"Old lesson\",\"zh\":\"旧课堂\",\"time\":\"09:00:00\"},{\"en\":\"pending\",\"zh\":\"\",\"time\":\"09:00:10\"}]");Lecture migrated=LectureStore.importLegacy(old,12345);
        check(migrated.lines.size()==2&&migrated.lines.get(0).zh.equals("旧课堂")&&migrated.lines.get(1).zh.isEmpty(),"v0.1 migration lost data");
        try{LectureStore.decode(new JSONObject(saved).put("id","../other"));throw new AssertionError("unsafe id accepted");}catch(JSONException expected){}
        System.out.println("PASS: persistence roundtrip, interrupted translation recovery, v0.1 migration, record ID validation");
    }
}
