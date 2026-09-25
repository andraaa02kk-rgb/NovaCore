package com.novacore.performance

import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.content.ContextCompat
import com.novacore.performance.audio.NovaAudioService
import com.novacore.performance.overlay.NovaOverlayService
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

class NovaBridge(private val a: Activity, private val web: WebView) {
    private val p = a.getSharedPreferences("nova", Context.MODE_PRIVATE)
    private val picker = 42
    private var pending: String? = null
    private fun js(s: String) = a.runOnUiThread { web.evaluateJavascript("window.novaReceive && window.novaReceive($s)", null) }
    private fun json(block: JSONObject.() -> Unit) = JSONObject().apply(block).toString()
    private fun mem(): JSONObject { val m=ActivityManager.MemoryInfo(); (a.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(m); return JSONObject().apply { put("total",m.totalMem); put("available",m.availMem); put("used",m.totalMem-m.availMem); put("percent",((m.totalMem-m.availMem)*100/m.totalMem).toInt()) } }
    @JavascriptInterface fun getMemoryInfo()=mem().toString()
    @JavascriptInterface fun getBatteryInfo():String { val i=a.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED)); return json { put("percent",i?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)); put("charging",i?.getIntExtra(BatteryManager.EXTRA_STATUS,-1)==BatteryManager.BATTERY_STATUS_CHARGING || i?.getIntExtra(BatteryManager.EXTRA_STATUS,-1)==BatteryManager.BATTERY_STATUS_FULL); put("status",i?.getIntExtra(BatteryManager.EXTRA_STATUS,-1)) } }
    @JavascriptInterface fun getStorageInfo():String { val s=StatFs(a.filesDir.path); val t=s.totalBytes; val f=s.availableBytes; return json { put("total",t); put("available",f); put("used",t-f); put("percent",((t-f)*100/t).toInt()) } }
    @JavascriptInterface fun getThermalInfo():String { val pm=a.getSystemService(PowerManager::class.java); return json { put("status",if(Build.VERSION.SDK_INT>=29) pm.currentThermalStatus else -1); put("headroom",if(Build.VERSION.SDK_INT>=30) pm.getThermalHeadroom(0) else JSONObject.NULL); put("temperature",JSONObject.NULL) } }
    @JavascriptInterface fun getDisplayInfo():String { val d=a.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager; val x=d.getDisplay(android.view.Display.DEFAULT_DISPLAY); val m=android.util.DisplayMetrics(); x?.getRealMetrics(m); return json { put("width",m.widthPixels); put("height",m.heightPixels); put("refresh",x?.refreshRate ?: 0); put("rates",x?.supportedModes?.map{it.refreshRate}?.distinct() ?: emptyList<Float>()) } }
    @JavascriptInterface fun getDeviceInfo()=json { put("manufacturer",Build.MANUFACTURER); put("model",Build.MODEL); put("codename",Build.DEVICE); put("android",Build.VERSION.RELEASE); put("sdk",Build.VERSION.SDK_INT); put("abi",Build.SUPPORTED_ABIS.firstOrNull()?:("N/A")); put("cores",Runtime.getRuntime().availableProcessors()); put("cpu",File("/proc/cpuinfo").takeIf{it.canRead()}?.readLines()?.firstOrNull{it.contains("Hardware")||it.contains("model name")}?.substringAfter(":")?.trim() ?: "N/A"); put("gpu","N/A") }
    @JavascriptInterface fun getCpuInfo()=getDeviceInfo()
    @JavascriptInterface fun getGpuInfo()="{\"gpu\":\"N/A\"}"
    @JavascriptInterface fun openBackgroundPicker(type:String){ pending=type; a.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE); type=if(type=="photo")"image/*" else "video/*"; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)},picker) }
    @JavascriptInterface fun resetBackground(){p.edit().remove("background").apply(); js("{type:'backgroundReset'}")}
    @JavascriptInterface fun openAudioPicker(){pending="audio"; a.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/*";addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)},picker)}
    @JavascriptInterface fun playAudio(uri:String){ContextCompat.startForegroundService(a,Intent(a,NovaAudioService::class.java).setAction(NovaAudioService.PLAY).setData(Uri.parse(uri)))}
    @JavascriptInterface fun pauseAudio(){a.startService(Intent(a,NovaAudioService::class.java).setAction(NovaAudioService.PAUSE))}
    @JavascriptInterface fun resumeAudio(){a.startService(Intent(a,NovaAudioService::class.java).setAction(NovaAudioService.PLAY))}
    @JavascriptInterface fun stopAudio(){a.startService(Intent(a,NovaAudioService::class.java).setAction(NovaAudioService.STOP))}
    @JavascriptInterface fun seekAudio(ms:Long){a.startService(Intent(a,NovaAudioService::class.java).setAction(NovaAudioService.SEEK).putExtra("ms",ms))}
    @JavascriptInterface fun enableOverlay(){if(!Settings.canDrawOverlays(a)){js("{type:'error',message:'Overlay permission required'}");a.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:${a.packageName}")))}else{ContextCompat.startForegroundService(a,Intent(a,NovaOverlayService::class.java));js("{type:'overlay',enabled:true}")}}
    @JavascriptInterface fun disableOverlay(){a.stopService(Intent(a,NovaOverlayService::class.java))}
    @JavascriptInterface fun isOverlayPermissionGranted()=Settings.canDrawOverlays(a)
    @JavascriptInterface fun getCacheSize()=(cacheBytes(a.cacheDir)+cacheBytes(a.externalCacheDir)).toString()
    private fun cacheBytes(f:File?):Long= f?.walkTopDown()?.filter{it.isFile}.sumOf{it.length()}
    @JavascriptInterface fun clearNovaCache(){a.cacheDir.deleteRecursively();a.externalCacheDir?.deleteRecursively();js("{type:'cacheCleared',bytes:0}")}
    @JavascriptInterface fun setBoostEnabled(v:Boolean){p.edit().putBoolean("boost",v).apply()}
    @JavascriptInterface fun getBoostStatus()=p.getBoolean("boost",false)
    @JavascriptInterface fun exportDiagnosticReport(){pending="report";a.startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="application/json";putExtra(Intent.EXTRA_TITLE,"nova-diagnostic.json")},picker)}
    fun onResult(code:Int,result:Int,data:Intent?){if(code!=picker||result!=Activity.RESULT_OK||data?.data==null)return; val u=data.data!!;try{a.contentResolver.takePersistableUriPermission(u,data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(_:Exception){};if(pending=="report"){a.contentResolver.openOutputStream(u)?.use{it.write(json{put("device",JSONObject(getDeviceInfo()));put("memory",mem());put("battery",JSONObject(getBatteryInfo()));put("storage",JSONObject(getStorageInfo()));put("thermal",JSONObject(getThermalInfo()));put("display",JSONObject(getDisplayInfo()));put("version","1.0")}.toByteArray())}}else if(pending=="audio")playAudio(u.toString())else{p.edit().putString("background",u.toString()).putString("backgroundType",pending).apply();js("{type:'background',kind:'$pending',uri:${JSONObject.quote(u.toString())}}")}}
    fun resume(){}; fun pause(){}
}
