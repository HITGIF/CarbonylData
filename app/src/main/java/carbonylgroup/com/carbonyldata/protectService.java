package carbonylgroup.com.carbonyldata;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class protectService extends Service {


    private StrongService startS1 = new StrongService.Stub() {

        @Override
        public void stopService() throws RemoteException {
            Intent i = new Intent(getBaseContext(), WatchingService.class);
            getBaseContext().stopService(i);
        }

        @Override
        public void startService() throws RemoteException {
            Intent i = new Intent(getBaseContext(), WatchingService.class);
            getBaseContext().startService(i);

        }
    };

    public void onCreate() {
        keepService1();
    }

    private void keepService1() {

        boolean isRun = Utils.isProcessRunning(protectService.this, "carbonylgroup.com.carbonyldata:WatchingService");
        if (!isRun) {
            try {
                startS1.startService();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {
        keepService1();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        keepService1();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        keepService1();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return (IBinder) startS1;
    }
}
