
/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.gtask.remote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import net.micode.notes.R;
import net.micode.notes.ui.NotesListActivity;
import net.micode.notes.ui.NotesPreferenceActivity;

/*
 private void showNotification(int tickerId, String content) 向用户提示当前同步的状态，是一个用于交互的方法
 protected Integer doInBackground(Void... unused) 此方法在后台线程执行，完成任务的主要工作，通常需要较长的时间
 protected void onProgressUpdate(String... progress)  可以使用进度条增加用户体验度。 此方法在主线程执行，用于显示任务执行的进度。
 protected void onPostExecute(Integer result)  相当于Handler 处理UI的方式，在这里面可以使用在doInBackground 得到的结果处理操作UI
*/

public class GTaskASyncTask extends AsyncTask<Void, String, Integer> {//异步操作类，用于实现Gtask的异步操作

    private static int GTASK_SYNC_NOTIFICATION_ID = 5234235;

    public interface OnCompleteListener {
        void onComplete();
    }

    private Context mContext;

    private NotificationManager mNotifiManager;

    private GTaskManager mTaskManager;

    private OnCompleteListener mOnCompleteListener;

    public GTaskASyncTask(Context context, OnCompleteListener listener) {
        mContext = context;
        mOnCompleteListener = listener;
        mNotifiManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mTaskManager = GTaskManager.getInstance();
    }

    public void cancelSync() {
        mTaskManager.cancelSync();
    }

    public void publishProgess(String message) {
        publishProgress(new String[] {
            message
        });
    }

    private void showNotification(int tickerId, String content) {
        Notification notification = new Notification(R.drawable.notification, mContext
                .getString(tickerId), System.currentTimeMillis());
        notification.defaults = Notification.DEFAULT_LIGHTS;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        PendingIntent pendingIntent;
        if (tickerId != R.string.ticker_success) {
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesPreferenceActivity.class), 0);

        } else {
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesListActivity.class), 0);
        }
//        notification.setLatestEventInfo(mContext, mContext.getString(R.string.app_name), content,
//                pendingIntent);
        notification.contentIntent = pendingIntent;
        mNotifiManager.notify(GTASK_SYNC_NOTIFICATION_ID, notification);
    }

    @Override
    protected Integer doInBackground(Void... unused) {
        publishProgess(mContext.getString(R.string.sync_progress_login, NotesPreferenceActivity
                .getSyncAccountName(mContext)));
        return mTaskManager.sync(mContext, this);
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        showNotification(R.string.ticker_syncing, progress[0]);
        if (mContext instanceof GTaskSyncService) {
            ((GTaskSyncService) mContext).sendBroadcast(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result == GTaskManager.STATE_SUCCESS) {
            showNotification(R.string.ticker_success, mContext.getString(
                    R.string.success_sync_account, mTaskManager.getSyncAccount()));
            NotesPreferenceActivity.setLastSyncTime(mContext, System.currentTimeMillis());
        } else if (result == GTaskManager.STATE_NETWORK_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_network));
        } else if (result == GTaskManager.STATE_INTERNAL_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_internal));
        } else if (result == GTaskManager.STATE_SYNC_CANCELLED) {
            showNotification(R.string.ticker_cancel, mContext
                    .getString(R.string.error_sync_cancelled));
        }
        if (mOnCompleteListener != null) {
            new Thread(new Runnable() {

                public void run() {
                    mOnCompleteListener.onComplete();
                }
            }).start();
        }
    }
}
