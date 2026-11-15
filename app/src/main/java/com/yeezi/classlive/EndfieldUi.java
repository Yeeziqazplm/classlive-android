package com.yeezi.classlive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** ClassLive nightshift: mint / charcoal / 8dp cut / plain / readable / calm.
 * Reading is the primary visual: real transcript, timestamps and model storage.
 */
final class EndfieldUi {
    static final int BG=0xFF141615, PANEL=0xFF202321, INK=0xFFEDEEEB,
            MUTED=0xFFADB4AE, RULE=0xFF363D38, CHIP=0xFF2B312D,
            ACCENT=0xFFB8F5CC, ON_ACCENT=0xFF152019, DANGER=0xFFFF9C91;
    static int dp(Context c,int n){return (int)(c.getResources().getDisplayMetrics().density*n+.5f);}
    static GradientDrawable panel(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(0);return d;}
    static final class Cut extends Drawable {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); final int color,cut;
        Cut(Context c,int color){this.color=color;cut=dp(c,8);}
        @Override public void draw(Canvas canvas){Rect b=getBounds();Path p=new Path();p.moveTo(b.left,b.top);p.lineTo(b.right,b.top);p.lineTo(b.right,b.bottom-cut);p.lineTo(b.right-cut,b.bottom);p.lineTo(b.left,b.bottom);p.close();paint.setColor(color);canvas.drawPath(p,paint);}
        @Override public void setAlpha(int a){paint.setAlpha(a);}
        @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static Button button(Context c,String label){Button b=new Button(c);b.setText(label);b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setPadding(dp(c,14),dp(c,10),dp(c,22),dp(c,10));b.setStateListAnimator(null);style(b,false,false);return b;}
    static void style(Button b,boolean primary,boolean danger){
        Context c=b.getContext();StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},new Cut(c,CHIP));
        states.addState(new int[]{android.R.attr.state_pressed},new Cut(c,primary?0xFF92C4A2:0xFF46564A));
        states.addState(new int[]{android.R.attr.state_focused},new Cut(c,0xFF46564A));
        states.addState(new int[]{},new Cut(c,primary?ACCENT:CHIP));b.setBackground(states);
        b.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled},{}},new int[]{MUTED,danger?DANGER:primary?ON_ACCENT:INK}));
        b.setTypeface(null,Typeface.BOLD);
    }
    static TextView label(Context c,String value){TextView t=new TextView(c);t.setText(value);t.setTextColor(MUTED);t.setTextSize(11);t.setLetterSpacing(.14f);t.setPadding(0,dp(c,14),0,dp(c,6));
        try{t.setTypeface(new Typeface.Builder(c.getAssets(),"fonts/Saira.ttf").setFontVariationSettings("'wght' 600, 'wdth' 125").build());}catch(Exception ignored){}return t;}
    static View rule(Context c){View v=new View(c);v.setBackgroundColor(RULE);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(c,1)));return v;}
    static void toggle(Switch s){Context c=s.getContext();s.setMinHeight(dp(c,48));s.setTextColor(INK);s.setTextSize(14);s.setSwitchPadding(dp(c,12));
        StateListDrawable track=new StateListDrawable();GradientDrawable on=panel(ACCENT),off=panel(RULE);on.setSize(dp(c,40),dp(c,24));off.setSize(dp(c,40),dp(c,24));track.addState(new int[]{android.R.attr.state_checked},on);track.addState(new int[]{},off);s.setTrackDrawable(track);
        GradientDrawable thumb=panel(INK);thumb.setSize(dp(c,20),dp(c,24));thumb.setStroke(dp(c,3),ON_ACCENT);s.setThumbDrawable(thumb);}
    static AlertDialog.Builder dialog(Context c){return new AlertDialog.Builder(c){
        @Override public AlertDialog show(){AlertDialog d=super.show();decorate(d);return d;}
    };}
    static void decorate(AlertDialog d){if(d.getWindow()!=null)d.getWindow().setBackgroundDrawable(panel(PANEL));for(int id:new int[]{-1,-2,-3}){Button b=d.getButton(id);if(b!=null){style(b,id==-1,false);b.setMinHeight(dp(b.getContext(),48));}}}
}
