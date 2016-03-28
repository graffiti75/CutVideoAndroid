package com.example.TwoThumbsSeekBarActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;

/**
 * PermissionUtils.java.
 *
 * @author Rodrigo Cericatto
 * @since Mar 19, 2016
 */
public class PermissionUtils {

    //--------------------------------------------------
    // Permissions Methods
    //--------------------------------------------------

    public static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(permission));
        }
        return false;
    }

    public static void alertAndFinish(final Activity context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_name).setMessage(context.getString(R.string.permissions_denial));
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                context.finish();
            }
        });

        // Add the buttons.
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static Boolean canAccessWriteExternalStorage(Activity context) {
        return (PermissionUtils.hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }
}