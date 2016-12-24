package carbonylgroup.com.carbonyldata;

import android.app.Activity;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.List;

public class WatchingService extends Service {

    private boolean watching = false;
    private Double preData = .0;
    private Double nowData = .0;
    private Double totalData = .0;
    private Double remainData = .0;
    private String msgReceived;
    private String ns = Context.NOTIFICATION_SERVICE;
    private String TAG = "WorkingService_TAG";
    private String Process_Name = "carbonylgroup.com.carbonyldata:protectService";
    private DecimalFormat twoDigitFormat = new DecimalFormat("#.00");
    private StrongService startS2 = new StrongService.Stub() {
        @Override
        public void stopService() throws RemoteException {
            Intent i = new Intent(getBaseContext(), protectService.class);
            getBaseContext().stopService(i);
        }

        @Override
        public void startService() throws RemoteException {
            Intent i = new Intent(getBaseContext(), protectService.class);
            getBaseContext().startService(i);
        }


    };
    private ContentObserver mObserver;

    public Runnable watchingRunnable = new Runnable() {
        @Override
        public void run() {

            String persentage = "NULL";
            String specific = "NULL";
            while (watching) {
                try {
                    Thread.sleep(1000);//每1000ms进行流量监控

                    preData = nowData;
                    nowData = Double.valueOf(TrafficStats.getMobileRxBytes());
                    remainData -= (nowData - preData);
                    persentage = twoDigitFormat.format(remainData / totalData * 100) + "% Remain";
                    specific = "Remain " + twoDigitFormat.format(getDataInMB(remainData)) + "MB | " + "Total " + twoDigitFormat.format(getDataInMB(totalData)) + "MB";
                    setNotification(persentage, specific);
                } catch (Exception e) {
                    Log.e(TAG, "run: watching thread error");
                }
            }
        }
    };

    public Runnable autoCheckRunnable = new Runnable() {
        @Override
        public void run() {

            while (watching) {
                try {
                    Thread.sleep(getSetting.readIntSetting("ac_period_list", WatchingService.this));//根据设置的时间发短信
                    sendSMS();
                    Log.d(TAG, "run: Checking Message Sent");
                } catch (Exception e) {
                    Log.e(TAG, "run: checking thread error");
                }
            }
        }
    };


    @Override
    public void onCreate() {

        super.onCreate();
        keepprotectService();

        initValue();
        sendSMS();

        toast(getResources().getString(R.string.service_started), 1);
        setNotification("Checking for data usage...", "Waiting for service provider.");
    }

    private void initValue() {

        nowData = Double.valueOf(TrafficStats.getMobileRxBytes());
        mObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                ContentResolver resolver = getContentResolver();
                Cursor cursor = resolver.query(Uri.parse("content://sms/inbox"), new String[]{"_id", "address", "body"}, null, null, "_id desc");
                String address = "";
                String body = "";
                long id = -1;

                if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                    id = cursor.getLong(0);
                    address = cursor.getString(1);
                    body = cursor.getString(2);

                }
                cursor.close();

                if (checkFormat(address, body, id)) {
                    msgReceived = body;
                    resolver.delete(Telephony.Sms.CONTENT_URI, "_id=" + id, null);
                    handleMsg();
                }
            }
        };
        getContentResolver().registerContentObserver(Uri.parse("content://sms/"), true, mObserver);
    }

    private boolean checkFormat(String address, String body, long id) {

        int formatValue = getSetting.readIntSetting("message_format_list", WatchingService.this);
        switch (formatValue) {
            case 0:
                if (address.equals("10001") && body.contains("您的具体上网流量使用情况如下") && id != -1)
                    return true;
                break;

            case 1:
                if (address.equals("10001") && body.contains("上月不清零流量/手机上网国内流量") && id != -1)
                    return true;
                break;

            default:
                break;
        }
        return false;
    }

    private void checkFormat() {

        try {
            int formatValue = getSetting.readIntSetting("message_format_list", WatchingService.this);

            switch (formatValue) {

                case 0:
                    totalData = getDataInB(Double.parseDouble(msgReceived.substring(msgReceived.indexOf("总流量") + 3, msgReceived.indexOf("MB，其中"))));

                    remainData = getDataInB(Double.parseDouble(msgReceived.substring(msgReceived.indexOf("还剩余") + 3, msgReceived.indexOf("MB\n仅供参考"))));
                    break;

                case 1:
                    totalData = getDataInB(Double.parseDouble(msgReceived.substring(msgReceived.indexOf("上月不清零流量") + 19, msgReceived.indexOf("MB；剩余量"))) +
                            Double.parseDouble(msgReceived.substring(msgReceived.indexOf("3、手机上网国内流量") + 13, msgReceived.lastIndexOf("MB；剩余量"))));

                    remainData = getDataInB(Double.parseDouble(msgReceived.substring(msgReceived.indexOf("MB；剩余量") + 6, msgReceived.indexOf("MB\n3、"))) +
                            Double.parseDouble(msgReceived.substring(msgReceived.lastIndexOf("MB；剩余量") + 6, msgReceived.lastIndexOf("MB\n"))));
                    break;

                default:
                    Log.e(TAG, "checkFormatForData: Wrong Value");
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "checkFormatForData: Error");
        }
    }

    private Double getDataInMB(Double BData) {
        return BData / 1024 / 1024;
    }

    private Double getDataInB(Double MBData) {
        return MBData * 1024 * 1024;
    }

    public void sendSMS() {

        try {
            String SENT_SMS_ACTION = "SENT_SMS_ACTION";
            Intent sentIntent = new Intent(SENT_SMS_ACTION);
            PendingIntent sentPI = PendingIntent.getBroadcast(WatchingService.this, 0, sentIntent, 0);
            WatchingService.this.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context _context, Intent _intent) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            break;
                    }
                }
            }, new IntentFilter(SENT_SMS_ACTION));

            String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
            Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
            PendingIntent deliverPI = PendingIntent.getBroadcast(WatchingService.this, 0,
                    deliverIntent, 0);
            WatchingService.this.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context _context, Intent _intent) {
                }
            }, new IntentFilter(DELIVERED_SMS_ACTION));

            String phoneNumber = "";
            String message = "";
            if (getSetting.readStringSetting("service_provider_list", WatchingService.this).equals("0")) {
                phoneNumber = "10001";
            }
            message = getSetting.readStringSetting("message_code_text", WatchingService.this);

            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            List<String> divideContents = smsManager.divideMessage(message);
            for (String text : divideContents) {
                smsManager.sendTextMessage(phoneNumber, null, text, sentPI, deliverPI);
            }
        } catch (Exception e) {
            toast(getResources().getString(R.string.need_setting) + "\n" + getResources().getString(R.string.need_setting_explain), 2);
        }
    }

    private void handleMsg() {

        checkFormat();

        if (!watching) {
            watching = true;
            Thread watchingThread = new Thread(watchingRunnable);
            Thread autoCheckThread = new Thread(autoCheckRunnable);
            watchingThread.start();
            if (getSetting.readBooleanSetting("auto_check_switch", WatchingService.this))
                autoCheckThread.start();
        }
    }

    private void toast(String string, int period) {

        Toast.makeText(WatchingService.this,
                string, period == 1 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG)
                .show();
    }

    private void setNotification(String title, String text) {

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

        int icon = R.drawable.ic_notifications_black_24dp;
        CharSequence tickerText = "Carbonyl Data is watching";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        notification.flags |= Notification.FLAG_NO_CLEAR;
        Context context = getApplicationContext();
        CharSequence contentTitle = title;
        CharSequence contentText = text;
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        mNotificationManager.notify(0, notification);
    }

    private void keepprotectService() {

        boolean isRun = Utils.isProcessRunning(WatchingService.this, Process_Name);
        if (!isRun) {
            try {
                startS2.startService();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {

        keepprotectService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        keepprotectService();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {

        return (IBinder) startS2;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        keepprotectService();
    }
}