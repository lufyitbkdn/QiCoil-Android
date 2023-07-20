package com.Meditation.Sounds.frequencies;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;

import com.Meditation.Sounds.frequencies.api.ApiListener;
import com.Meditation.Sounds.frequencies.db.QFDatabase;
import com.Meditation.Sounds.frequencies.db.dao.PlaylistDAO;
import com.Meditation.Sounds.frequencies.feature.base.BaseActivity;
import com.Meditation.Sounds.frequencies.models.Playlist;
import com.Meditation.Sounds.frequencies.tasks.BaseTask;
import com.Meditation.Sounds.frequencies.tasks.UpdateDurationOfAllPlaylistTask;
import com.Meditation.Sounds.frequencies.tasks.UpdatePlaylistInforVer10Task;
import com.Meditation.Sounds.frequencies.utils.Constants;
import com.Meditation.Sounds.frequencies.utils.FilesUtils;
import com.Meditation.Sounds.frequencies.utils.SharedPreferenceHelper;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.appsflyer.AppsFlyerLib;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class QApplication extends MultiDexApplication implements ApiListener, Configuration.Provider {
    private static QApplication INSTANCE;
    private int APP_VERSION = 12;
    private ArrayList<BaseActivity> mStacksActivity;

    public static boolean isActivityDownloadStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        MultiDex.install(this);
        INSTANCE = this;
        AppsFlyerLib.getInstance().init("aNPCN6auSrzidSGCeMrg9R", null, this);
        AppsFlyerLib.getInstance().start(this);
        int version = SharedPreferenceHelper.getInstance().getInt(SharedPreferenceHelper.SHARED_PREF_APP_VERSION);
        if (version < APP_VERSION) {
            onUpgradeVersion(version, APP_VERSION);
            SharedPreferenceHelper.getInstance().setInt(SharedPreferenceHelper.SHARED_PREF_APP_VERSION, APP_VERSION);
        }
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mStacksActivity = new ArrayList<>();


//        new CountDownTimer(30000, 1000) {
//
//            public void onTick(long millisUntilFinished) {
//              //  mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//                //here you can have your logic to set text to edittext
//            }
//
//            public void onFinish() {
//                showAlertDialog(INSTANCE);
//            }
//
//        }.start();
        // showAlertDialog(INSTANCE);
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//               showAlertDialog(INSTANCE);
//            }
//        }, 30000);
    }

    public void addActivityToStack(BaseActivity activity) {
        if (mStacksActivity.size() > 0 && mStacksActivity.get(mStacksActivity.size() - 1).getClass() == activity.getClass()) {
            return;
        }
        mStacksActivity.add(activity);
    }

    public void removeActivityToStack(BaseActivity activity) {
        if (mStacksActivity.size() > 0 && mStacksActivity.get(mStacksActivity.size() - 1).getClass() == activity.getClass()) {
            mStacksActivity.remove(mStacksActivity.size() - 1);
        }
    }

    public BaseActivity getTopActivity() {
        return mStacksActivity.size() > 0 ? mStacksActivity.get(mStacksActivity.size() - 1) : null;
    }

    public static QApplication getInstance() {
        return INSTANCE;
    }

    public void onUpgradeVersion(int oldVersion, int newVersion) {
        if (oldVersion < 9) {
            try {
                FilesUtils.deleteRecursive(new File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_FOLDER));
            } catch (Throwable e) {
            }
            QFDatabase database = QFDatabase.getDatabase(this);
            database.albumDAO().clear();
            database.songDAO().clear();
            database.playlistDAO().clear();
            database.playlistItemDAO().clear();
            database.playlistItemSongDAO().clear();
        }
        if (oldVersion < 10) {
            new UpdatePlaylistInforVer10Task(this, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        if (oldVersion < 11) {
            new UpdateDurationOfAllPlaylistTask(this, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        if (oldVersion < 12) {
            PlaylistDAO playlistDao = QFDatabase.getDatabase(this).playlistDAO();
            Playlist playlist = playlistDao.getFirstModifiedPlaylist();
            if (playlist != null && playlist.getTitle() != null && playlist.getTitle().equalsIgnoreCase("Playlist 1")) {
                playlistDao.updateFromUserFromId(playlist.getId(), 1);
            }
        }
    }

    @Override
    public void onConnectionOpen(BaseTask task) {

    }

    @Override
    public void onConnectionSuccess(BaseTask task, Object data) {

    }

    @Override
    public void onConnectionError(BaseTask task, Exception exception) {

    }

    //Dialoge
    public void showAlertDialog(Context context) {
        /** define onClickListener for dialog */
        DialogInterface.OnClickListener listener
                = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do some stuff eg: context.onCreate(super)
            }
        };

        /** create builder for dialog */
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setMessage("Messag...")
                .setTitle("Title")
                .setPositiveButton("OK", listener);
        /** create dialog & set builder on it */
        Dialog dialog = builder.create();
        /** this required special permission but u can use aplication context */
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        /** show dialog */
        dialog.show();
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        if (BuildConfig.DEBUG) {
            return new Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.DEBUG)
                    .build();
        } else {
            return new Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.ERROR)
                    .build();
        }
    }
}
