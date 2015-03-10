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
    //=================================�����Ǳ�����ʼ��=====================================
	//��ȡ���ͻ������ʵ��
	private static PackageManager packageManager = null;
	private static ActivityManager activityManager = null;
	
	//�������еĽ����б����Խ���ProgramUtil��
	private static List<RunningAppProcessInfo> runningProcessList = null;
	private static List<ProgramUtil> infoList = null;
	
	//��ѡ�еĽ��̵�����
	private static RunningAppProcessInfo processSelected = null;
		
	//��ȡӦ�ó���Ļ�����Ϣ���Խ���PackageUtil��
	private static PackageUtil packageUtil = null;
	
	//ˢ�ºͽ������̰�ť
	private static Button refresh = null;
	private static Button killAll = null;
	
	//��̨ˢ���б�����ʾˢ����ʾ���Խ���RefreshHandler��
	private static RefreshHandler handler = null;
	
	//ProgressDialog����ˢ�½�����
	private static ProgressDialog progressDialog =null;
	
	//ListView���������Խ���procListAdapter��
	private static ProcListAdapter procListAdapter = null;
	
	//****************************************�¼ӹ��ܣ��ڴ���ʾ********************************************
	private TextView canUseMemory;
	//****************************************�¼ӹ��ܣ��ڴ���ʾ********************************************
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
      //****************************************�¼ӹ��ܣ��ڴ���ʾ********************************************
        canUseMemory = (TextView)findViewById(R.id.showMemory); 
        
//        refresh = (Button)findViewById(R.id.refresh);  
//               //��ȡϵͳ������Ϣ  
//                myActivityManager =(ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);          
//                upDateMemInfo();  
//                refresh.setOnClickListener(new OnClickListener(){  
//                    public void onClick(View source){  
//                       upDateMemInfo();  
//                    }  

    	//****************************************�¼ӹ��ܣ��ڴ���ʾ********************************************
    //==============================���°�������ʵ����==================================================
        refresh = (Button)findViewById(R.id.myButton_refresh);
        //�Խ�ˢ�°�ť��������RefreshButtonListener()
        refresh.setOnClickListener(new refreshButtonListener());
        killAll = (Button)findViewById(R.id.myButton_killAll);
        //�Խ�����ȫ�����̰�ť��������KillAllButtonButtonListener()
        killAll.setOnClickListener(new killAllButtonListener());
        
        //��ȡ�����������Ա��ȡ����ͼ�������
        packageManager = this.getPackageManager();
        activityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);  //??????????
        //��ȡ������Ϣ
        packageUtil = new PackageUtil(this);
        
        //��ȡ�������еĽ����б�
        runningProcessList = new ArrayList<RunningAppProcessInfo>();
        infoList = new ArrayList<ProgramUtil>();
        
        //���ø����б�����
        updateProcessList();
        upDateMemInfo();
        
    }
    
    //�����б������ı�д
    private void updateProcessList(){
    	progressDialog = new ProgressDialog(MainActivity.this);
    	progressDialog.setProgressStyle(progressDialog.STYLE_SPINNER);
    	progressDialog.setTitle("��ʾ");
    	progressDialog.setMessage("����ˢ��...");
    	
    //�������̣߳�ִ�и��²������Խ�RefreshThread�ࣩ
    RefreshThread thread = new RefreshThread();
    handler = new RefreshHandler(); 
    thread.start();
    //��ʾ���ȶԻ���
    progressDialog.show();
    }

    //���ڸ��½���Ľ���******************** public��ǰʲô��û��****Ϊ�˼ӹ��ܶ�����********
    public class RefreshThread extends Thread {
		@Override
		public void run() {
			procListAdapter = buildProcListAdapter();
			Message msg = handler.obtainMessage();
			handler.sendMessage(msg);
		}
	}
    
    //��̨ˢ���б�����ʾˢ����ʾ�����ı�д ******************** public��ǰ��private****Ϊ�˼ӹ��ܶ�����********
    public class RefreshHandler extends Handler{
    	public void handleMessage(Message msg){
    		//���½���-ListView���������Խ���procListAdapter��
    		getListView().setAdapter(procListAdapter);
    		//ȡ����ʾ���ȶԻ���
    		progressDialog.dismiss();
    	}
    }
    
    //����ListAdapter
    public ProcListAdapter buildProcListAdapter() {
		//����������еĳ���                            ����������������������������������������������������������������������������
    	if(!runningProcessList.isEmpty()){
    		runningProcessList.clear();
    	}
    	//��մ�����г��������
    	if(!infoList.isEmpty()){
    		infoList.clear();
    	}
    	//��ȡ�������еĽ���
    	runningProcessList = activityManager.getRunningAppProcesses();
    	RunningAppProcessInfo procInfo = null;
    	for (Iterator<RunningAppProcessInfo> iterator = runningProcessList.iterator(); iterator.hasNext();) {
    		procInfo = iterator.next();                                   //????????????????????
    		//���������Ϣ�洢�����У��Խ�buildProgramUtilSimpleInfo��ȡӦ�ó��������Ϣ������
    		ProgramUtil programUtil = buildProgramUtilSimpleInfo(procInfo.pid, procInfo.processName);
    		//��������Ϣ���ӵ�������
    		infoList.add(programUtil);
    	}
    	ProcListAdapter adapter = new ProcListAdapter(infoList, this);
    	return adapter;
	}
    
    //��ȡ�ڴ���Ϣ����getUsedMemory
    public String getUsedMemory(int pid)
	{
		//��û������ʵ��
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		//����int����
		int[] pids = {pid};
		MemoryInfo[] memoryInfos =  am.getProcessMemoryInfo(pids);
		//��ý���ռ���ڴ�����������kBֵ
		int memorysize = memoryInfos[0].getTotalPrivateDirty();
		return "�ڴ�ռ��Ϊ "+ memorysize +" KB";
	}
    
    //��ȡӦ�ó��������Ϣ�ĺ���buildProgramUtilSimpleInfo
    public ProgramUtil buildProgramUtilSimpleInfo(int procId, String procNameString) {

		ProgramUtil programUtil = new ProgramUtil();
		programUtil.setProcessName(procNameString);
		
		//���ݽ���������ȡӦ�ó����ApplicationInfo����
		ApplicationInfo tempAppInfo = packageUtil.getApplicationInfo(procNameString);

		if (tempAppInfo != null) {
			//Ϊ���̼���ͼ��ͳ�������
			programUtil.setIcon(tempAppInfo.loadIcon(packageManager));
    		programUtil.setProgramName(tempAppInfo.loadLabel(packageManager).toString());
		} 
		else {
			//�����ȡʧ�ܣ���ʹ��Ĭ�ϵ�ͼ��ͳ�����
			programUtil.setIcon(getApplicationContext().getResources().getDrawable(R.drawable.ic_launcher));
			programUtil.setProgramName(procNameString);
		}
		//���ý����ڴ�ʹ�������Խ���ȡ�ڴ���Ϣ������
		String str = getUsedMemory(procId);
		programUtil.setMemString(str);
		return programUtil;
    }


    //ˢ�°�ť���������ı�д
    private class refreshButtonListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			//�Խ������б�����updateProcessList()
			updateProcessList();
		}
    }
    
    //����ȫ�����̰�ť���������ı�д
    private class killAllButtonListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			int count = infoList.size();
			ProgramUtil pu = null;
			//�������н��̣�����ر�
			for (int i = 0; i < count; i++) {
				pu = infoList.get(i);
				//�Խ��ر�ָ�����̺���closeOneProcess
				closeOneProcess(pu.getProcessName());
			}
			//�����б�
			updateProcessList();
		}
    }
    
  //�б�����������
  	@Override
  	protected void onListItemClick(ListView l, View v, int position, long id) {
  		//��õ�ǰѡ�еĽ���
      	processSelected = runningProcessList.get(position);
      	//�½��Ի���
      	AlertButtonListener listener = new AlertButtonListener();
      	Dialog alertDialog = new AlertDialog.Builder(this)
      		.setIcon(android.R.drawable.ic_dialog_info)
      		.setTitle("��ѡ��")
      		.setNegativeButton("ǿ�ƽ���", listener)
      		.setNeutralButton("�鿴����", listener).create();
      	alertDialog.show();
      	super.onListItemClick(l, v, position, id);
  	}
  	
  	private class AlertButtonListener implements 
	android.content.DialogInterface.OnClickListener {
//��������
@Override
public void onClick(DialogInterface dialog, int which) {
	switch (which) {
	case Dialog.BUTTON_NEUTRAL:
		Intent intent = new Intent();
		intent.setClass(MainActivity.this, ProcDetailActivity.class);
		// Ϊѡ�еĽ��̻�ȡ��װ������ϸ��Ϣ
		DetailProgramUtil programUtil = buildProgramUtilComplexInfo(processSelected.processName);
		if (programUtil == null) {
			break;
		}
		Bundle bundle = new Bundle();
		// ʹ��Serializable��Activity֮�䴫�ݶ���
		bundle.putSerializable("process_info", (Serializable)programUtil);
		intent.putExtras(bundle);
		//�򿪽�����ϸ��Ϣ����
		startActivity(intent);				
		break;
	case Dialog.BUTTON_NEGATIVE:
		//��������
		closeOneProcess(processSelected.processName);
		//���½���
		updateProcessList();
		break;
	default:
		break;
	}
}
}
  	
  //�ر�ָ�����̺���closeOneProcess
    private void closeOneProcess(String processName) {
    	//��ֹ�û�����������
		if (processName.equals(R.string.class)) {		
			Toast.makeText(MainActivity.this, "Canot Terminate Myself!", Toast.LENGTH_LONG).show();
			return;
		}
		//ͨ��һ�����������ظó����һ��ApplicationInfo����
		ApplicationInfo tempAppInfo = packageUtil.getApplicationInfo(processName);
		if (tempAppInfo == null) {
			return;
		}
		//���ݰ����رս���
		activityManager.killBackgroundProcesses(tempAppInfo.packageName);
    }
    
    /*
	 * Ϊ���̻�ȡ��װ��������
	 */
    public DetailProgramUtil buildProgramUtilComplexInfo(String procNameString) {

    	DetailProgramUtil complexProgramUtil = new DetailProgramUtil();
		// ���ݽ���������ȡӦ�ó����ApplicationInfo����
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
		complexProgramUtil.setCompanyName("����");
		complexProgramUtil.setVersionName(tempPkgInfo.versionName);
		complexProgramUtil.setVersionCode(tempPkgInfo.versionCode);
		complexProgramUtil.setDataDir(tempAppInfo.dataDir);
		complexProgramUtil.setSourceDir(tempAppInfo.sourceDir);
		complexProgramUtil.setPackageName(tempPkgInfo.packageName);
		// ��ȡ����������Ϣ����ҪΪPackageManager������Ȩ(packageManager.getPackageInfo()����)
		complexProgramUtil.setUserPermissions(tempPkgInfo.requestedPermissions);
		complexProgramUtil.setServices(tempPkgInfo.services);
		complexProgramUtil.setActivities(tempPkgInfo.activities);
		
		return complexProgramUtil;
    }
    //****************************************�¼ӹ��ܣ��ڴ���ʾ********************************************
    public void upDateMemInfo(){              
    	        //���MemoryInfo����    
    	        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();  
    	        //���ϵͳ�����ڴ棬������MemoryInfo������    
    	        activityManager.getMemoryInfo(memoryInfo) ;    
    	        long memSize = memoryInfo.availMem ;              
    	        //�ַ�����ת��   
    	        String leftMemSize = Formatter.formatFileSize(getBaseContext(), memSize);  
    	        canUseMemory.setText(leftMemSize);  
    	    }  

}