package com.wxtoplink.base.linux;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 管理Shell进程，固定时间未被使用的进程将被关闭回收
 * Created by 12852 on 2018/8/1.
 */

public class ShellManager {

    private static final String TAG = ShellManager.class.getSimpleName();

    //进程构造器
    private ProcessBuilder processBuilder ;
    //设置进程工作的初始目录
    private String rootFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() ;
    //定时器
    private Subscription timeDisposable ;
    //当前未使用时长
    private int existTime = 0 ;
    //被管理的进程
    private ShellProcess shellProcess;

    //初始化
    private ShellManager(){
        processBuilder = new ProcessBuilder("sh");
        //设置Shell进程初始路径
        processBuilder.directory(new File(rootFilePath));
        //合并结果输出流和错误结果输出流
        processBuilder.redirectErrorStream(true);
    }

    private static final ShellManager instance = new ShellManager();

    public static ShellManager getInstance(){
        return instance ;
    }

    public synchronized ShellProcess getConnected() throws IOException {
        existTime = 0 ;
        if(timeDisposable == null || timeDisposable.isUnsubscribed()){
            timeDisposable = Observable.
                    interval(1, TimeUnit.MINUTES)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            existTime ++ ;
                            Log.i(TAG,"==============================existTime =" + existTime);
                            //进程为null,关闭定时器
                            if(shellProcess == null){
                                timeDisposable.unsubscribe();
                                timeDisposable = null ;
                            }else if(shellProcess.isClose()){
                                //进程已经退出，释放内存,关闭定时器
                                shellProcess = null ;
                                timeDisposable.unsubscribe();
                                timeDisposable = null ;
                            }else if(existTime >= 5){
                                //超过十分钟未被调用，正常退出进程
                                shellProcess.release();
                            }
                        }
                    });
        }

        if(shellProcess == null || shellProcess.isClose()){
            //创建一个新的Linux窗口进程
            shellProcess = new ShellProcess(processBuilder.start());
            Log.i(TAG,"创建进程");
        }

        return shellProcess ;
    }

}
