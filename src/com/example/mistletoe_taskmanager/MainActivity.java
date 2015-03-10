package com.example.mistletoe_taskmanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Debug.MemoryInfo;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;  
import android.view.View;
import android.view.View.OnClickListener;  
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;  
import android.widget.Toast;

public class MainActivity extends ListActivity {
    //=================================以下是变量初始化=====================================
	//获取包和活动管理器实例
	private static PackageManager packageManager = null;
	private static ActivityManager activityManager = null;
	
	//正在运行的进程列表（自建类ProgramUtil）
	private static List<RunningAppProcessInfo> runningProcessList = null;
	private static List<ProgramUtil> infoList = null;
	
	//被选中的进程的名称
	private static RunningAppProcessInfo processSelected = null;
		
	//获取应用程序的基本信息（自建类PackageUtil）
	private static PackageUtil packageUtil = null;
	
	//刷新和结束进程按钮
	private static Button refresh = null;
	private static Button killAll = null;
	
	//后台刷新列表和显示刷新提示（自建类RefreshHandler）
	private static RefreshHandler handler = null;
	
	//ProgressDialog用来刷新进度条
	private static ProgressDialog progressDialog =null;
	
	//ListView适配器（自建类procListAdapter）
	private static ProcListAdapter procListAdapter = null;
	
	//****************************************新加功能：内存显示********************************************
	private TextView canUseMemory;
	//****************************************新加功能：内存显示********************************************
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
      //****************************************新加功能：内存显示********************************************
        canUseMemory = (TextView)findViewById(R.id.showMemory); 
        
//        refresh = (Button)findViewById(R.id.refresh);  
//               //获取系统服务信息  
//                myActivityManager =(ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);          
//                upDateMemInfo();  
//                refresh.setOnClickListener(new OnClickListener(){  
//                    public void onClick(View source){  
//                       upDateMemInfo();  
//                    }  

    	//****************************************新加功能：内存显示********************************************
    //==============================以下包含函数实例化==================================================
        refresh = (Button)findViewById(R.id.myButton_refresh);
        //自建刷新按钮监听函数RefreshButtonListener()
        refresh.setOnClickListener(new refreshButtonListener());
        killAll = (Button)findViewById(R.id.myButton_killAll);
        //自建结束全部进程按钮监听函数KillAllButtonButtonListener()
        killAll.setOnClickListener(new killAllButtonListener());
        
        //获取包管理器，以便获取程序图标和名称
        packageManager = this.getPackageManager();
        activityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);  //??????????
        //获取基本信息
        packageUtil = new PackageUtil(this);
        
        //获取正在运行的进程列表
        runningProcessList = new ArrayList<RunningAppProcessInfo>();
        infoList = new ArrayList<ProgramUtil>();
        
        //调用更新列表方法
        updateProcessList();
        upDateMemInfo();
        
    }
    
    //更新列表函数的编写
    private void updateProcessList(){
    	progressDialog = new ProgressDialog(MainActivity.this);
    	progressDialog.setProgressStyle(progressDialog.STYLE_SPINNER);
    	progressDialog.setTitle("提示");
    	progressDialog.setMessage("正在刷新...");
    	
    //开启新线程，执行更新操作（自建RefreshThread类）
    RefreshThread thread = new RefreshThread();
    handler = new RefreshHandler(); 
    thread.start();
    //显示进度对话框
    progressDialog.show();
    }

    //用于更新界面的进程******************** public以前什么都没有****为了加功能而更改********
    public class RefreshThread extends Thread {
		@Override
		public void run() {
			procListAdapter = buildProcListAdapter();
			Message msg = handler.obtainMessage();
			handler.sendMessage(msg);
		}
	}
    
    //后台刷新列表和显示刷新提示函数的编写 ******************** public以前是private****为了加功能而更改********
    public class RefreshHandler extends Handler{
    	public void handleMessage(Message msg){
    		//更新界面-ListView适配器（自建类procListAdapter）
    		getListView().setAdapter(procListAdapter);
    		//取消显示进度对话框
    		progressDialog.dismiss();
    	}
    }
    
    //构建ListAdapter
    public ProcListAdapter buildProcListAdapter() {
		//清空正在运行的程序                            ？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？
    	if(!runningProcessList.isEmpty()){
    		runningProcessList.clear();
    	}
    	//清空存放运行程序的数组
    	if(!infoList.isEmpty()){
    		infoList.clear();
    	}
    	//获取正在运行的进程
    	runningProcessList = activityManager.getRunningAppProcesses();
    	RunningAppProcessInfo procInfo = null;
    	for (Iterator<RunningAppProcessInfo> iterator = runningProcessList.iterator(); iterator.hasNext();) {
    		procInfo = iterator.next();                                   //????????????????????
    		//将程序的信息存储到类中（自建buildProgramUtilSimpleInfo获取应用程序基本信息函数）
    		ProgramUtil programUtil = buildProgramUtilSimpleInfo(procInfo.pid, procInfo.processName);
    		//将程序信息添加到数组中
    		infoList.add(programUtil);
    	}
    	ProcListAdapter adapter = new ProcListAdapter(infoList, this);
    	return adapter;
	}
    
    //获取内存信息函数getUsedMemory
    public String getUsedMemory(int pid)
	{
		//获得活动管理器实例
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		//构建int数组
		int[] pids = {pid};
		MemoryInfo[] memoryInfos =  am.getProcessMemoryInfo(pids);
		//获得进程占用内存总量，返回kB值
		int memorysize = memoryInfos[0].getTotalPrivateDirty();
		return "内存占用为 "+ memorysize +" KB";
	}
    
    //获取应用程序基本信息的函数buildProgramUtilSimpleInfo
    public ProgramUtil buildProgramUtilSimpleInfo(int procId, String procNameString) {

		ProgramUtil programUtil = new ProgramUtil();
		programUtil.setProcessName(procNameString);
		
		//根据进程名，获取应用程序的ApplicationInfo对象
		ApplicationInfo tempAppInfo = packageUtil.getApplicationInfo(procNameString);

		if (tempAppInfo != null) {
			//为进程加载图标和程序名称
			programUtil.setIcon(tempAppInfo.loadIcon(packageManager));
    		programUtil.setProgramName(tempAppInfo.loadLabel(packageManager).toString());
		} 
		else {
			//如果获取失败，则使用默认的图标和程序名
			programUtil.setIcon(getApplicationContext().getResources().getDrawable(R.drawable.ic_launcher));
			programUtil.setProgramName(procNameString);
		}
		//设置进程内存使用量（自建获取内存信息函数）
		String str = getUsedMemory(procId);
		programUtil.setMemString(str);
		return programUtil;
    }


    //刷新按钮监听函数的编写
    private class refreshButtonListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			//自建更新列表函数updateProcessList()
			updateProcessList();
		}
    }
    
    //结束全部进程按钮监听函数的编写
    private class killAllButtonListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			int count = infoList.size();
			ProgramUtil pu = null;
			//遍历所有进程，逐个关闭
			for (int i = 0; i < count; i++) {
				pu = infoList.get(i);
				//自建关闭指定进程函数closeOneProcess
				closeOneProcess(pu.getProcessName());
			}
			//更新列表
			updateProcessList();
		}
    }
    
  //列表按键监听器
  	@Override
  	protected void onListItemClick(ListView l, View v, int position, long id) {
  		//获得当前选中的进程
      	processSelected = runningProcessList.get(position);
      	//新建对话框
      	AlertButtonListener listener = new AlertButtonListener();
      	Dialog alertDialog = new AlertDialog.Builder(this)
      		.setIcon(android.R.drawable.ic_dialog_info)
      		.setTitle("请选择")
      		.setNegativeButton("强制结束", listener)
      		.setNeutralButton("查看详情", listener).create();
      	alertDialog.show();
      	super.onListItemClick(l, v, position, id);
  	}
  	
  	private class AlertButtonListener implements 
	android.content.DialogInterface.OnClickListener {
//按键处理
@Override
public void onClick(DialogInterface dialog, int which) {
	switch (which) {
	case Dialog.BUTTON_NEUTRAL:
		Intent intent = new Intent();
		intent.setClass(MainActivity.this, ProcDetailActivity.class);
		// 为选中的进程获取安装包的详细信息
		DetailProgramUtil programUtil = buildProgramUtilComplexInfo(processSelected.processName);
		if (programUtil == null) {
			break;
		}
		Bundle bundle = new Bundle();
		// 使用Serializable在Activity之间传递对象
		bundle.putSerializable("process_info", (Serializable)programUtil);
		intent.putExtras(bundle);
		//打开进程详细信息界面
		startActivity(intent);				
		break;
	case Dialog.BUTTON_NEGATIVE:
		//结束进程
		closeOneProcess(processSelected.processName);
		//更新界面
		updateProcessList();
		break;
	default:
		break;
	}
}
}
  	
  //关闭指定进程函数closeOneProcess
    private void closeOneProcess(String processName) {
    	//阻止用户结束本程序
		if (processName.equals(R.string.class)) {		
			Toast.makeText(MainActivity.this, "Canot Terminate Myself!", Toast.LENGTH_LONG).show();
			return;
		}
		//通过一个程序名返回该程序的一个ApplicationInfo对象
		ApplicationInfo tempAppInfo = packageUtil.getApplicationInfo(processName);
		if (tempAppInfo == null) {
			return;
		}
		//根据包名关闭进程
		activityManager.killBackgroundProcesses(tempAppInfo.packageName);
    }
    
    /*
	 * 为进程获取安装包的详情
	 */
    public DetailProgramUtil buildProgramUtilComplexInfo(String procNameString) {

    	DetailProgramUtil complexProgramUtil = new DetailProgramUtil();
		// 根据进程名，获取应用程序的ApplicationInfo对象
		ApplicationInfo tempAppInfo = packageUtil.getApplicationInfo(procNameString);
		if (tempAppInfo == null) {
			return null;
		}
		
		PackageInfo tempPkgInfo = null;
		try {
			tempPkgInfo = packageManager.getPackageInfo(
					tempAppInfo.packageName, 
					PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_ACTIVITIES
					| PackageManager.GET_SERVICES | PackageManager.GET_PERMISSIONS);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		if (tempPkgInfo == null) {
			return null;
		}
		
		complexProgramUtil.setProcessName(procNameString);
		complexProgramUtil.setCompanyName("暂无");
		complexProgramUtil.setVersionName(tempPkgInfo.versionName);
		complexProgramUtil.setVersionCode(tempPkgInfo.versionCode);
		complexProgramUtil.setDataDir(tempAppInfo.dataDir);
		complexProgramUtil.setSourceDir(tempAppInfo.sourceDir);
		complexProgramUtil.setPackageName(tempPkgInfo.packageName);
		// 获取以下三个信息，需要为PackageManager进行授权(packageManager.getPackageInfo()方法)
		complexProgramUtil.setUserPermissions(tempPkgInfo.requestedPermissions);
		complexProgramUtil.setServices(tempPkgInfo.services);
		complexProgramUtil.setActivities(tempPkgInfo.activities);
		
		return complexProgramUtil;
    }
    //****************************************新加功能：内存显示********************************************
    public void upDateMemInfo(){              
    	        //获得MemoryInfo对象    
    	        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();  
    	        //获得系统可用内存，保存在MemoryInfo对象上    
    	        activityManager.getMemoryInfo(memoryInfo) ;    
    	        long memSize = memoryInfo.availMem ;              
    	        //字符类型转换   
    	        String leftMemSize = Formatter.formatFileSize(getBaseContext(), memSize);  
    	        canUseMemory.setText(leftMemSize);  
    	    }  

}
