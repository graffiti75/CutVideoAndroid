package com.example.TwoThumbsSeekBarActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.VideoView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Source: http://stackoverflow.com/questions/22189100/crop-video-like-whatsapp
 */
public class MainActivity extends Activity {

    //--------------------------------------------------
    // Constants
    //--------------------------------------------------

    private static final String[] PERMISSIONS = { Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private static final int PERMISSION_REQUEST = 1212;

    private static final String TAG = MainActivity.class.getSimpleName();

    //--------------------------------------------------
    // Attributes
    //--------------------------------------------------

    /**
     * Context.
     */

    private Activity mActivity = MainActivity.this;

    /**
     * FFMpeg.
     */

    private FFmpeg mFFmpeg;

    /**
     * Layout.
     */

    private TextView mLeftTextView;
    private TextView mRightTextView;
    private View mVideoControlButton;
    private View mVideoSaveButton;

    /**
     * Video.
     */

    private StateObserver mVideoStateObserver = new StateObserver();
    private VideoSliceSeekBar mVideoSliceSeekBar;
    private VideoView mVideoView;
    private String mPath;

    /**
     * Dialog.
     */

    private ProgressDialog mProgressDialog;


    //--------------------------------------------------
    // Activity Life Cycle
    //--------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setLayout();
        checkPermissions();
    }

    //--------------------------------------------------
    // Main Methods
    //--------------------------------------------------

    private void setLayout() {
        mLeftTextView = (TextView) findViewById(R.id.left_pointer);
        mRightTextView = (TextView) findViewById(R.id.right_pointer);

        mVideoSliceSeekBar = (VideoSliceSeekBar) findViewById(R.id.seek_bar);
        mVideoView = (VideoView) findViewById(R.id.video);
        mVideoControlButton = findViewById(R.id.video_control_btn);
        mVideoSaveButton = findViewById(R.id.saveButton);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(null);
    }

    private void initVideoView() {
        mPath = "android.resource://com.example.TwoThumbsSeekBarActivity/" + R.raw.make_your_song;
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mediaPlayer) {
                mVideoSliceSeekBar.setSeekBarChangeListener(new VideoSliceSeekBar.SeekBarChangeListener() {
                    @Override
                    public void seekBarValueChanged(int leftThumb, int rightThumb) {
                        mLeftTextView.setText(getTimeForTrackFormat(leftThumb, true));
                        mRightTextView.setText(getTimeForTrackFormat(rightThumb, true));
                    }
                });

                mVideoSliceSeekBar.setMaxValue(mediaPlayer.getDuration());
                mVideoSliceSeekBar.setLeftProgress(0);
//                mVideoSliceSeekBar.setRightProgress(mediaPlayer.getDuration());

                // 5 seconds is the maximum entry.
                mVideoSliceSeekBar.setRightProgress(5000);

                // Minimum difference of 5 seconds.
                Integer minimum = (5000 * 100) / mediaPlayer.getDuration();
                Log.d(TAG, "Minimum: " + minimum);
                mVideoSliceSeekBar.setProgressMinDiff(minimum);

                // Maximum difference of 20 seconds.
//                Integer maximum = (20000 * 100) / mediaPlayer.getDuration();
                Integer maximum = 72;
                Log.d(TAG, "Maximum: " + maximum);
                mVideoSliceSeekBar.setProgressMaxDiff(maximum);

                mVideoControlButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        performVideoViewClick();
                    }
                });

                mVideoSaveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Left progress: " + mVideoSliceSeekBar.getLeftProgress() / 1000);
                        Log.d(TAG, "Right progress: " + mVideoSliceSeekBar.getRightProgress() / 1000);
                        Log.d(TAG, "Total duration: " + mediaPlayer.getDuration() / 1000);
                        executeTrimCommand(mVideoSliceSeekBar.getLeftProgress(), mVideoSliceSeekBar.getRightProgress());
                    }
                });
            }
        });
        mVideoView.setVideoURI(Uri.parse(mPath));
    }

    private void loadFFMpegBinary() {
        try {
            if (mFFmpeg == null) {
                Log.d(TAG, "FFmpeg was null.");
                mFFmpeg = FFmpeg.getInstance(this);
            }
            mFFmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "FFmpeg correct loaded.");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        } catch (Exception e) {
            Log.d(TAG, "Exception: " + e);
        }
    }

    //--------------------------------------------------
    // Other Methods
    //--------------------------------------------------

    private void execFFmpegBinary(final String command) {
        try {
            mFFmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output " + s);
                }

                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "SUCCESS with output " + s);
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command (ffmpeg): " + command);
                    Log.d(TAG, "progress : " + s);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "Started command (ffmpeg): " + command);
                    mProgressDialog.setMessage("Processing...");
                    mProgressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command (ffmpeg): " + command);
                    mProgressDialog.dismiss();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Do nothing.
        }
    }

    private void performVideoViewClick() {
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            mVideoSliceSeekBar.setSliceBlocked(false);
            mVideoSliceSeekBar.removeVideoStatusThumb();
        } else {
            mVideoView.seekTo(mVideoSliceSeekBar.getLeftProgress());
            mVideoView.start();
            mVideoSliceSeekBar.setSliceBlocked(true);
            mVideoSliceSeekBar.videoPlayingProgress(mVideoSliceSeekBar.getLeftProgress());
            mVideoStateObserver.startVideoProgressObserving();
        }
    }

    public static String getTimeForTrackFormat(int timeInMillis, boolean display2DigitsInMinsSection) {
        int minutes = (timeInMillis / (60 * 1000));
        int seconds = (timeInMillis - minutes * 60 * 1000) / 1000;
        String result = display2DigitsInMinsSection && minutes < 10 ? "0" : "";
        result += minutes + ":";
        if (seconds < 10) {
            result += "0" + seconds;
        } else {
            result += seconds;
        }
        return result;
    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(MainActivity.this).setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.device_not_supported))
            .setMessage(getString(R.string.device_not_supported_message))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.finish();
                }
            }).create().show();
    }

    private void executeTrimCommand(int startMilli, int endMilli) {
        File moviesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        String filePrefix = "make_your_song";
        String fileFormat = ".mp4";
        String fileName = filePrefix + fileFormat;

        try {
            InputStream inputStream = getAssets().open(fileName);
            File src = new File(moviesDirectory, fileName);
            storeFile(inputStream, src);

            File dest = new File(moviesDirectory, filePrefix + "_1" + fileFormat);
            if (dest.exists()) {
                dest.delete();
            }
            Log.d(TAG, "Start Trim (source): " + src.getAbsolutePath());
            Log.d(TAG, "Start Trim (destination): " + dest.getAbsolutePath());
            Log.d(TAG, "Start Trim (startMilli): " + startMilli);
            Log.d(TAG, "Start Trim (endMilli): " + endMilli);

            execFFmpegBinary("-i " + src.getAbsolutePath() + " -ss " + startMilli / 1000 + " -to " + endMilli / 1000
                + " -strict -2 -async 1 " + dest.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void storeFile(InputStream input, File file) {
        try {
            final OutputStream output = new FileOutputStream(file);
            try {
                try {
                    final byte[] buffer = new byte[1024];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                    output.flush();
                } finally {
                    output.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //--------------------------------------------------
    // Permissions
    //--------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted.
                    initVideoView();
                    loadFFMpegBinary();
                } else {
                    // Permission denied, boo! Disable the functionality that depends on this permission.
                    PermissionUtils.alertAndFinish(mActivity);
                }
                return;
            }
        }
    }

    private void checkPermissions() {
        // Checks the Android version of the device.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Boolean writeExternalStorage = PermissionUtils.canAccessWriteExternalStorage(mActivity);
            if (!writeExternalStorage) {
                requestPermissions(PERMISSIONS, PERMISSION_REQUEST);
            } else {
                // Permission was granted.
                initVideoView();
                loadFFMpegBinary();
            }
        } else {
            // Version is below Marshmallow.
            initVideoView();
            loadFFMpegBinary();
        }
    }

    //--------------------------------------------------
    // StateObserver
    //--------------------------------------------------

    private class StateObserver extends Handler {

        private boolean mAlreadyStarted = false;

        private Runnable mObserverWork = new Runnable() {
            @Override
            public void run() {
                startVideoProgressObserving();
            }
        };

        private void startVideoProgressObserving() {
            if (!mAlreadyStarted) {
                mAlreadyStarted = true;
                sendEmptyMessage(0);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            mAlreadyStarted = false;
            mVideoSliceSeekBar.videoPlayingProgress(mVideoView.getCurrentPosition());
            if (mVideoView.isPlaying() && mVideoView.getCurrentPosition() < mVideoSliceSeekBar.getRightProgress()) {
                postDelayed(mObserverWork, 50);
            } else {
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                }
                mVideoSliceSeekBar.setSliceBlocked(false);
                mVideoSliceSeekBar.removeVideoStatusThumb();
            }
        }
    }
}