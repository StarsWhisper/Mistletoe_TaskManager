package com.example.mistletoe_taskmanager;
import java.lang.reflect.Method;
import java.util.Iterator;

import android.app.ProgressDialog;
import com.example.mistletoe_taskmanager.MainActivity.RefreshHandler;
import com.example.mistletoe_taskmanager.MainActivity.RefreshThread;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;           
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
	
//程序详细信息界面
public class ProcDetailActivity extends Activity {
	private static final String ATTR_PACKAGE_STATS="PackageStats";	
	private DetailProgramUtil processInfo = null;
	private static TextView textProcessName = null;
	private static TextView textProcessVersion = null;
	private static TextView textInstallDir = null;
	private static TextView textDataDir = null;
	private static TextView textPkgSize = null;
	private static TextView textPermission = null;
	private static TextView textService = null;
	private static TextView textActivity = null;
	private static Button btnKillProcess = null;
	
	//******************************功能更新**********************************************
	//添加功能：强制结束后自动刷新
	//后台刷新列表和显示刷新提示（自建类RefreshHandler）
//		private static RefreshHandler handler = null;
	//***********************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detail_layout);
		//获得界面元素
		//程序名称
		textProcessName = (TextView)findViewById(R.id.detail_processName);
		//程序版本
		textProcessVersion = (TextView)findViewById(R.id.detail_copyright);
		//安装目录
		textInstallDir = (TextView)findViewById(R.id.detail_installLocation);
		//数据目录
		textDataDir = (TextView)findViewById(R.id.detail_dataLocation);
		//包的小信息
		textPkgSize = (TextView)findViewById(R.id.detail_size);
		//权限信息
		textPermission = (TextView)findViewById(R.id.detail_permission);
		//服务信息
		textService = (TextView)findViewById(R.id.detail_service);
		//Activity信息
		textActivity = (TextView)findViewById(R.id.detail_activity);
		//强制结束按键
		btnKillProcess = (Button)findViewById(R.id.detail_killProcessButton);
		//绑定监听器
		btnKillProcess.setOnClickListener(new KillButtonListener());
		//获得传递过来的数据
		Intent intent = getIntent();
		Bundle bundle= intent.getExtras();
		processInfo = (DetailProgramUtil) bundle.getSerializable("process_info");
		//将获得的数据显示到界面
		showAppInfo();
	}
	//显示程序详细信息
	public void showAppInfo(){
		//设置程序名称
		textProcessName.setText(processInfo.getProcessName());
		//设置程序安装目录
		textInstallDir.setText(processInfo.getSourceDir());
		//设置程序版本
		textProcessVersion.setText(
				"公司名称：" + processInfo.getCompanyName()
				+ "  " + "版本号：" + processInfo.getVersionName()
				+ "(" + processInfo.getVersionCode() + ")");
		//设置数据目录
		textDataDir.setText(processInfo.getDataDir());
		//设置权限
		textPermission.setText(processInfo.getUserPermissions());
		//设置服务信息
		textService.setText(processInfo.getServices());
		//设置Activity信息
		textActivity.setText(processInfo.getActivities());
		//设置包大小信息
		getpkginfo(processInfo.getPackageName());
	}
	//结束进程按键监听器
	private class KillButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			//获得活动管理器
			ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			PackageUtil packageUtil = new PackageUtil(ProcDetailActivity.this);
			//如果是本程序，则不结束
			if (processInfo.getProcessName().equals("com.example.mistletoe_taskmanager")) {
				Toast.makeText(ProcDetailActivity.this, "无法结束本程序自身！", Toast.LENGTH_LONG).show();
				return;
			}
			//获得程序的信息类
			ApplicationInfo tempAppInfo = packageUtil.getApplicationInfo(processInfo.getProcessName());
			activityManager.killBackgroundProcesses(tempAppInfo.packageName);
			//******************************功能更新**********************************************
//			updateProcessList();
			//********************************************************************************
			Toast.makeText(ProcDetailActivity.this, "进程已强制结束！", Toast.LENGTH_LONG).show();
		}
	}
	
	//******************************功能更新**********************************************
//	private void updateProcessList(){
//    //开启新线程，执行更新操作（自建RefreshThread类）
//    RefreshThread thread = new RefreshThread();
//    handler = new RefreshHandler(); 
//    thread.start();
//    }
    //********************************************************************************
    
		//用于更新界面的报数据带下信息
	 private Handler mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 1:
					String infoString="";
					PackageStats newPs = msg.getData().getParcelable(ATTR_PACKAGE_STATS);
					if (newPs!=null) {
						infoString+="应用程序大小: "+formatFileSize(newPs.codeSize);
						infoString+="\n数据大小: "+formatFileSize(newPs.dataSize);
						infoString+="\n缓存大小: "+formatFileSize(newPs.cacheSize);
					}
					textPkgSize.setText(infoString);
					break;
				default:
					break;
				}
			}
			};
	//利用反射机制获得程序的包大小信息
			//使用普通方式将得不到包的大小信息
	public void getpkginfo(String pkg){
		PackageManager pm = getPackageManager();
		try {
			Method getPackageSizeInfo = pm.getClass()
			.getMethod("getPackageSizeInfo", String.class,
					IPackageStatsObserver.class);
			getPackageSizeInfo.invoke(pm, pkg,
			new PkgSizeObserver());
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	//使用AIDL实现进程间通信，将包的信息发送给mHandler
	class PkgSizeObserver extends IPackageStatsObserver.Stub {
		public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
     Message msg = mHandler.obtainMessage(1);
     Bundle data = new Bundle();
     //将包信息存放到data中
     data.putParcelable(ATTR_PACKAGE_STATS, pStats);
     msg.setData(data);
     mHandler.sendMessage(msg);
    
		}
	}
	 //格式化文件大小信息
	public static String formatFileSize(long length) {
		String result = null;
		int sub_string = 0;
		//文件是GB级别的情况
		if (length >= 1073741824) {
			sub_string = String.valueOf((float) length / 1073741824).indexOf(
					".");
			result = ((float) length / 1073741824 + "000").substring(0,
					sub_string + 3)
					+ "GB";
			//文件是MB级别的情况
		} else if (length >= 1048576) {
			sub_string = String.valueOf((float) length / 1048576).indexOf(".");
			result = ((float) length / 1048576 + "000").substring(0,
					sub_string + 3)
					+ "MB";
			//文件是KB级别的情况
		} else if (length >= 1024) {
			sub_string = String.valueOf((float) length / 1024).indexOf(".");
			result = ((float) length / 1024 + "000").substring(0,
					sub_string + 3)
					+ "KB";
			//文件是B级别的情况
		} else if (length < 1024)
			result = Long.toString(length) + "B";
		return result;
	}
}
