package com.wxtoplink.base.linux;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;

/**
 * Created by 12852 on 2018/7/30.
 */

public class ShellProcess {

    private static final String TAG = ShellProcess.class.getSimpleName();
    public ShellProcess(Process process) {
        this.process = process;
        initEnvironment();
    }

    private int requestCount = 0 ;

    private Process process ;

    private Thread inputThread,outputThread ;

    private OutputStream outputStream ;
    BufferedReader resultBuffer ;

    private Vector<String> linuxCommands ;

    private boolean isClose = false,isInit = false;

    private ExecuteCallback callback ;

    private InputRunnable inputRunnable;
    private OutputRunnable outPutRunnable ;

    //初始化Linux工具，初始化后才可以调用execute方法
    private synchronized void initEnvironment(){
        if(!isInit) {
            try {

                linuxCommands = new Vector<>();
                //输入流（对于Process）
                outputStream = process.getOutputStream();
                //输出流（对于Process）
                resultBuffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
                //输入（对于应用）
                inputRunnable = new InputRunnable();
                //输出（对于应用）
                outPutRunnable = new OutputRunnable();
                //输入线层（对于应用）
                inputThread = new Thread(inputRunnable);
                //输出线层（对于应用）
                outputThread = new Thread(outPutRunnable);
                //开启命令写入线层
                inputThread.start();
                //开启输出线层
                outputThread.start();

                //修改初完成
                isInit = true ;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            Log.e(TAG,"ShellManager is already init");
        }
    }

    public boolean isClose(){
        return isClose ;
    }

    //正常退出
    public void release(){
        //退出进程
        linuxCommands.add("exit\n");
    }

    //执行linux命令
    public synchronized void execute(String linuxCommand,ExecuteCallback callback){
        Log.i(TAG,"-----------------execute start:" + Thread.currentThread().getName());

        if(!isInit){
            //若未初始化，先初始化
            initEnvironment();
        }
        this.callback = callback ;

        requestCount = 0;
        //添加待执行的Linux命令
        linuxCommands.add(linuxCommand + "\n") ;

        //减少回调错位的情况
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class OutputRunnable implements Runnable{

        @Override
        public void run() {
            Log.i(TAG,"----------------outputRunnable is running:"+ Thread.currentThread().getName());
            int count ;
            char[] chars = new char[1024];
            try {
                //resultBuffer会长时间阻塞读取返回结果，直到执行exit后，关闭
                while ((count = resultBuffer.read(chars)) > -1){

                    //添加结果
                    callback.executeResult(String.format("\n^%s^\n%s",requestCount++,new String(chars,0,count)));
                    //延时
                    Thread.sleep(1000);
                    //若进程已经结束，正常结束
                    if(isClose){
                        break;
                    }
                }

                Log.i(TAG,"--------------------outputRunnable is finish");

            }catch (Exception e){
                Log.e(TAG,"-------------------error:" + e.getMessage());
            }finally {
                try {
                    //关闭输出流
                    resultBuffer.close();
                    isClose = true ;
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"--------------close bufferedReader error:" + e.getMessage());
                }
            }
        }
    }

    private class InputRunnable implements Runnable{

        @Override
        public void run() {
            try {
                Log.i(TAG,"---------------inputRunnable is running:" + Thread.currentThread().getName());

                while(true) {
                    //存在待执行的Linux命令，即写入命令
                    while(!linuxCommands.isEmpty()){
                        for(String command : linuxCommands){
                            //如果outputStream已关闭，会抛出IO异常，此时线层正常结束(可以由“exit”命令触发)
                            if (outputStream != null) {
                                Log.i(TAG, "-------------------inputRunnable write command:" + command);
                                outputStream.write(command.getBytes());
                            }
                            Log.i(TAG,linuxCommands.size() + " ");
                        }
                        outputStream.flush();
                        linuxCommands.clear();
                    }
                    //每200毫秒扫描一次
                    Thread.sleep(200);
                    //若进程已经结束，正常结束
                    if(isClose){
                        break;
                    }
                }

            }catch (Exception e){
                Log.e(TAG,"--------------input error:" + e.getMessage());
            }finally {
                try {
                    outputStream.close();
                    linuxCommands.clear();
                    isClose = true ;
                    Log.i(TAG,"--------------close outputStream :" + linuxCommands.capacity());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"--------------close outputStream error:" + e.getMessage());
                }
            }
        }
    }

    //执行命令的回调
    public interface ExecuteCallback{

        void executeResult(String result);
    }

}
