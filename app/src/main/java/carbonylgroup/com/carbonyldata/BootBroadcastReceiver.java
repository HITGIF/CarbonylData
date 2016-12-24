package carbonylgroup.com.carbonyldata;

/**
 * Created by carbo on 10/07/2016.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent startIntent = new Intent(context, WatchingService.class);
        context.startService(startIntent);
    }

}