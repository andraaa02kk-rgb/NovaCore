package com.novacore.performance.overlay

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*

class NovaOverlayService: Service(){ private var wm:WindowManager?=null; private var view:View?=null; private val tag="NovaOverlay"
 override fun onCreate(){super.onCreate();Log.d(tag,"service created"); if(Build.VERSION.SDK_INT>=26)startForeground(9,Notification.Builder(this,"nova_overlay").setContentTitle("NOVA//CORE Mini Monitor").setSmallIcon(android.R.drawable.ic_menu_info_details).build()); create()}
 private fun create(){if(!Settings.canDrawOverlays(this)){Log.e(tag,"permission denied");stopSelf();return}; val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(22,14,22,14);setBackgroundColor(Color.argb(220,10,14,12));addView(TextView(context).apply{text="NOVA//CORE\nFPS    N/A\nTEMP   N/A\nPING   N/A";setTextColor(Color.WHITE);textSize=12f})};val lp=WindowManager.LayoutParams(260,WindowManager.LayoutParams.WRAP_CONTENT,if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP or Gravity.START;lp.x=40;lp.y=180;var dx=0f;var dy=0f;box.setOnTouchListener{_,e->when(e.action){MotionEvent.ACTION_DOWN->{dx=e.rawX-lp.x;dy=e.rawY-lp.y};MotionEvent.ACTION_MOVE->{lp.x=(e.rawX-dx).toInt();lp.y=(e.rawY-dy).toInt();wm?.updateViewLayout(box,lp)}};true};wm=getSystemService(WINDOW_SERVICE) as WindowManager;view=box;try{wm!!.addView(box,lp);Log.d(tag,"WindowManager.addView success")}catch(t:Throwable){Log.e(tag,"exception",t);stopSelf()}}
 override fun onDestroy(){try{view?.let{wm?.removeView(it)}}catch(_:Throwable){};Log.d(tag,"overlay removed");super.onDestroy()};override fun onBind(i:Intent?)=null
}
