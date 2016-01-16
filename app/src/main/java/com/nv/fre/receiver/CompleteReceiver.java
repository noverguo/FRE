package com.nv.fre.receiver;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class CompleteReceiver extends BroadcastReceiver {
	private static final String TAG = CompleteReceiver.class.getSimpleName();
	private static final String PER_NAME = "down";
	private static final String KEY_ID = "ID";
	public static void setId(Context context, long id) {
		context.getSharedPreferences(PER_NAME, Context.MODE_PRIVATE).edit().putLong(KEY_ID, id).commit();
	}

	public static long getId(Context context) {
		return context.getSharedPreferences(PER_NAME, Context.MODE_PRIVATE).getLong(KEY_ID, -1);
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if(action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
//			Log.i(TAG, "当前完成应用ID: " + id);
			if(id != getId(context)) {
				return;
			}
			Query query = new Query();
			query.setFilterById(id);
			DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			Cursor cursor = downloadManager.query(query);

			String path = null;
			String uri = null;
			while(cursor.moveToNext()) {
				path = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
				uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
			}
			cursor.close();
			//如果sdcard不可用时下载下来的文件，那么这里将是一个内容提供者的路径，这里打印出来，有什么需求就怎么样处理
			if(TextUtils.isEmpty(path)) {
				path = uri;
			}
			if(path.startsWith("content:")) {
//				cursor = context.getContentResolver().query(Uri.parse(path), null, null, null, null);
//				while(cursor.moveToNext()) {
//				}
//				cursor.close();
			} else {
				Log.i(TAG, "安装应用: " + path);
				installApk(context, path);
			}
		}
	}

	/**
	 * 安装APK文件
	 */
	private void installApk(Context context, String path) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse("file://" + path), "application/vnd.android.package-archive");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
