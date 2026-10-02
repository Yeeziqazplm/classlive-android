package com.yeezi.classlive;
import java.util.*;

final class Lecture {
    final String id;
    String title;
    final long createdAt;
    long recordedMs;
    final List<Line> lines=new ArrayList<>();
    String draft="";
    String draftTime="";
    Lecture(String id,String title,long createdAt) {this.id=id;this.title=title;this.createdAt=createdAt;}
    static final class Line {
        final String id,en,time;
        String zh="",error="";
        final boolean unconfirmed,lowConfidence;
        boolean pending;
        Line(String id,String en,String time,boolean unconfirmed,boolean lowConfidence) {
            this.id=id;this.en=en;this.time=time;this.unconfirmed=unconfirmed;this.lowConfidence=lowConfidence;
        }
    }
    enum ExportMode { BILINGUAL, ENGLISH, CHINESE }
    String export(ExportMode mode) {
        StringBuilder b=new StringBuilder("ClassLive 课堂记录\n课程：").append(title)
            .append("\n开始时间：").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.UK).format(new Date(createdAt)))
            .append("\n收音时长：").append(recordedMs/60000).append(" 分 ").append(recordedMs/1000%60)
            .append(" 秒\n条目数：").append(lines.size()).append("\n\n");
        for(Line r:lines) {
            b.append('[').append(r.time).append(']');
            if(r.unconfirmed) b.append(" [未确认片段]");
            if(r.lowConfidence) b.append(" [识别可能不准]");
            b.append('\n');
            if(mode!=ExportMode.CHINESE) b.append("EN: ").append(r.en).append('\n');
            if(mode!=ExportMode.ENGLISH) b.append("中文: ").append(r.zh.isEmpty()?(r.pending?"[翻译尚未完成]":"[未翻译]"):r.zh).append('\n');
            if(!r.error.isEmpty()) b.append("备注: ").append(r.error).append('\n');
            b.append('\n');
        }
        if(!draft.isEmpty()) {
            b.append('[').append(draftTime).append("] [正在识别，尚未确认]\n");
            if(mode!=ExportMode.CHINESE) b.append("EN: ").append(draft).append('\n');
            if(mode!=ExportMode.ENGLISH) b.append("中文: [未翻译]\n");
        }
        return b.toString();
    }
    String filename(ExportMode mode) {
        String safe=title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_").trim();
        if(safe.isEmpty())safe="课堂";
        if(safe.length()>60)safe=safe.substring(0,60);
        String suffix=mode==ExportMode.ENGLISH?"英文":mode==ExportMode.CHINESE?"中文":"双语";
        return safe+"_"+new java.text.SimpleDateFormat("yyyyMMdd-HHmm",Locale.UK).format(new Date(createdAt))+"_"+suffix+".txt";
    }
}
