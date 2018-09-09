package com.skierkowski.WifiManager;

import com.facebook.react.uimanager.*;
import com.facebook.react.bridge.*;
import com.facebook.systrace.Systrace;
import com.facebook.systrace.SystraceMessage;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import android.os.Bundle;
import android.content.Context;
import java.util.List;
import com.facebook.systrace.Systrace;
import com.facebook.systrace.SystraceMessage;

import com.facebook.react.LifecycleState;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.widget.Toast;

import voice.encoder.VoicePlayer;
import voice.encoder.DataEncoder;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;
import java.util.List;

public class WifiManagerModule extends ReactContextBaseJavaModule {
  private VoicePlayer player = new VoicePlayer();

  private String sendMac = null;
  private String wifiName;
  private String currentBssid;


  public WifiManagerModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return "WifiManager";
  }

  @ReactMethod
  public void list(Callback successCallback, Callback errorCallback) {
    try {
      WifiManager mWifiManager = (WifiManager) getReactApplicationContext().getSystemService(Context.WIFI_SERVICE);
      if (mWifiManager.getWifiState() == mWifiManager.WIFI_STATE_ENABLED) {
          mWifiManager.disconnect();
          mWifiManager.reconnect();
      } else {
          mWifiManager.setWifiEnabled(true);
      }
      List < ScanResult > results = mWifiManager.getScanResults();
      WritableArray wifiArray =  Arguments.createArray();
      for (ScanResult result: results) {
        if(!result.SSID.equals("")){
          wifiArray.pushString(result.SSID);
        }
      }
      successCallback.invoke(wifiArray);
    } catch (IllegalViewOperationException e) {
      errorCallback.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void connect(String ssid, String password) {
    WifiManager mWifiManager = (WifiManager) getReactApplicationContext().getSystemService(Context.WIFI_SERVICE);
    List < ScanResult > results = mWifiManager.getScanResults();
    for (ScanResult result: results) {
      if (ssid.equals(result.SSID)) {
        connectTo(result, password, ssid);
      }
    }
  }

  public void connectTo(ScanResult result, String password, String ssid) {
    //Make new configuration
    WifiConfiguration conf = new WifiConfiguration();
    conf.SSID = "\"" + ssid + "\"";
    String Capabilities = result.capabilities;
    if (Capabilities.contains("WPA2")) {
      conf.preSharedKey = "\"" + password + "\"";
    } else if (Capabilities.contains("WPA")) {
      conf.preSharedKey = "\"" + password + "\"";
    } else if (Capabilities.contains("WEP")) {
      conf.wepKeys[0] = "\"" + password + "\"";
      conf.wepTxKeyIndex = 0;
      conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
      conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
    } else {
      conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    }
    //Remove the existing configuration for this netwrok
    WifiManager mWifiManager = (WifiManager) getReactApplicationContext().getSystemService(Context.WIFI_SERVICE);
    List<WifiConfiguration> mWifiConfigList = mWifiManager.getConfiguredNetworks();
    String comparableSSID = ('"' + ssid + '"'); //Add quotes because wifiConfig.SSID has them
    for(WifiConfiguration wifiConfig : mWifiConfigList){
      if(wifiConfig.SSID.equals(comparableSSID)){
        int networkId = wifiConfig.networkId;
        mWifiManager.removeNetwork(networkId);
        mWifiManager.saveConfiguration();
      }
    }
    //Add configuration to Android wifi manager settings...
     WifiManager wifiManager = (WifiManager) getReactApplicationContext().getSystemService(Context.WIFI_SERVICE);
     mWifiManager.addNetwork(conf);
    //Enable it so that android can connect
    List < WifiConfiguration > list = mWifiManager.getConfiguredNetworks();
    for (WifiConfiguration i: list) {
      if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
        wifiManager.disconnect();
        wifiManager.enableNetwork(i.networkId, true);
        wifiManager.reconnect();
        break;
      }
    }
  }

  @ReactMethod
  public void status(Callback statusResult) {
    ConnectivityManager connManager = (ConnectivityManager) getReactApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    statusResult.invoke(mWifi.getState().toString());
  }

  private static Integer findNetworkInExistingConfig(WifiManager wifiManager, String ssid) {
   List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
   for (WifiConfiguration existingConfig : existingConfigs) {
     if (existingConfig.SSID.equals(ssid)) {
       return existingConfig.networkId;
     }
   }
   return null;
 }

 @ReactMethod
 public void getWifi(String wifi, Callback result)
 {
     WifiManager wifiMan = (WifiManager) getReactApplicationContext().getSystemService(Context.WIFI_SERVICE);

     WifiInfo wifiInfo = wifiMan.getConnectionInfo();

     wifiName = wifi.toString();
     if (wifiName.length() > 2 && wifiName.charAt(0) == '"'
             && wifiName.charAt(wifiName.length() - 1) == '"') {
         wifiName = wifiName.substring(1, wifiName.length() - 1);
     }

     List<ScanResult> wifiList = wifiMan.getScanResults();
     ArrayList<String> mList = new ArrayList<String>();
     mList.clear();

     for (int i = 0; i < wifiList.size(); i++)
     {
         mList.add((wifiList.get(i).BSSID).toString());

     }


     if (currentBssid == null)
     {
         for (int i = 0; i < wifiList.size(); i++) {
             if ((wifiList.get(i).SSID).toString().equals(wifiName))
             {
                 currentBssid = (wifiList.get(i).BSSID).toString();
                 break;
             }
         }
     }
     else {
         if (currentBssid.equals("00:00:00:00:00:00")
                 || currentBssid.equals("")) {
             for (int i = 0; i < wifiList.size(); i++)
             {
                 if ((wifiList.get(i).SSID).toString().equals(wifiName)) {
                     currentBssid = (wifiList.get(i).BSSID).toString();
                     break;
                 }
             }
         }
     }
     if (currentBssid == null)
     {
         return;
     }

     String tomacaddress[] = currentBssid.split(":");
     int currentLen = currentBssid.split(":").length;

     for (int m = currentLen - 1; m > -1; m--)
     {
         for (int j = mList.size() - 1; j > -1; j--)
         {
             if (!currentBssid.equals(mList.get(j)))
             {
                 String array[] = mList.get(j).split(":");
                 if (!tomacaddress[m].equals(array[m])) {
                     mList.remove(j);//
                 }
             }
         }
         if (mList.size() == 1 || mList.size() == 0) {
             if (m == 5) {
                 sendMac = tomacaddress[m].toString();
             } else if (m == 4) {
                 sendMac = tomacaddress[m].toString()
                         + tomacaddress[m + 1].toString();
             } else {
                 sendMac = tomacaddress[5].toString()
                         + tomacaddress[4].toString()
                         + tomacaddress[3].toString();
             }
             break;
         }
     }


      result.invoke(sendMac);
 }

 @ReactMethod
 public  void  sendSonic(String mac, final String wifi)
 {
     byte[] midbytes = null;

     try {
         midbytes = HexString2Bytes(mac);
         printHexString(midbytes);
     } catch (Exception e) {
         e.printStackTrace();
     }
     if (midbytes.length > 6)
     {
         Toast.makeText(getReactApplicationContext(), "no support",
                 Toast.LENGTH_SHORT).show();
         return;
     }

     byte[] b = null;
     int num = 0;
     if (midbytes.length == 2) {
         b = new byte[] { midbytes[0], midbytes[1] };
         num = 2;
     } else if (midbytes.length == 3) {
         b = new byte[] { midbytes[0], midbytes[1], midbytes[2] };
         num = 3;
     } else if (midbytes.length == 4) {
         b = new byte[] { midbytes[0], midbytes[1], midbytes[2], midbytes[3] };
         num = 4;
     } else if (midbytes.length == 5) {
         b = new byte[] { midbytes[0], midbytes[1], midbytes[2],
                 midbytes[3], midbytes[4] };
         num = 5;
     } else if (midbytes.length == 6) {
         b = new byte[] { midbytes[0], midbytes[1], midbytes[2],
                 midbytes[3], midbytes[4], midbytes[5] };
         num = 6;
     } else if (midbytes.length == 1) {
         b = new byte[] { midbytes[0] };
         num = 1;
     }

     int a[] = new int[19];
     a[0] = 6500;
     int i, j;
     for (i = 0; i < 18; i++)
     {
         a[i + 1] = a[i] + 200;
     }

     player.setFreqs(a);

     player.play(DataEncoder.encodeMacWiFi(b, wifi.trim()), 5, 1000);

 }

 private static byte uniteBytes(byte src0, byte src1)
 {
     byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 })).byteValue();
     _b0 = (byte) (_b0 << 4);
     byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 })).byteValue();
     byte ret = (byte) (_b0 ^ _b1);
     return ret;
 }

 private static byte[] HexString2Bytes(String src)
 {
     byte[] ret = new byte[src.length() / 2];
     byte[] tmp = src.getBytes();
     for (int i = 0; i < src.length() / 2; i++)
     {
         ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
     }
     return ret;
 }

 private static void printHexString(byte[] b) {
     // System.out.print(hint);
     for (int i = 0; i < b.length; i++)
     {
         String hex = Integer.toHexString(b[i] & 0xFF);
         if (hex.length() == 1) {
             hex = '0' + hex;
         }
         System.out.print("aaa" + hex.toUpperCase() + " ");
     }
     System.out.println("");
 }
}
