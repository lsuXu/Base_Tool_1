package com.wxtoplink.base.download;

import android.util.Log;

import com.wxtoplink.base.download.listener.DownloadListener;
import com.wxtoplink.base.download.listener.DownloadListenerImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 文件下载管理器（以下载队列方式，仅允许单个文件同时下载）
 * Created by 12852 on 2018/7/24.
 */

public class DownloadManager {

    private static final String TAG = DownloadManager.class.getSimpleName();

    private static final DownloadManager instance = new DownloadManager();
    //下载队列
    private List<DownloadTask> downloadQueue ;

    public static DownloadManager getInstance (){
        return instance;
    };

    private DownloadManager(){
        downloadQueue = new ArrayList<>();
    }

    //添加下载任务
    public boolean downloadFile(String url ,String fileName,String filePath){
        return downloadFile(url,fileName,filePath,null);
    }

    /**
     * 添加下载任务
     * @param filePath 文件保存全路径
     * @param url   网络下载路径
     * @param downloadListener 网络回调
     * @return
     */
    public boolean downloadFile(String url,String fileName,String filePath, DownloadListener downloadListener){
        Log.i(TAG,"新建下载任务：filePath =" + filePath + ";url =" + url + " ;downloadListener =" + downloadListener);
        DownloadTask downloadObject = new DownloadTask(url,fileName,filePath,downloadListener);
        return addDownloadTask(downloadObject);
    }

    public boolean downloadFile(DownloadTask downloadTask){
        Log.i(TAG,"新建下载任务：downloadTask =" + downloadTask.toString());
        return addDownloadTask(downloadTask);
    }

    public List<DownloadTask> getDownloadQueue(){
        return downloadQueue;
    }

    //当下载队列发生改变时执行，判断当前下载状况，决定是否进行文件下载
    private void download(){
        if(!downloadQueue.isEmpty() && !DownloadListenerImpl.getInstance().isDownloading()){
            download(downloadQueue.get(0));
        }else if(DownloadListenerImpl.getInstance().isDownloading()){
            Log.i(TAG,"任务下载中");
        }else{
            Log.i(TAG,"任务已经全部下载完成");
        }
    }

    //添加下载任务
    private boolean addDownloadTask(DownloadTask downloadTask){
        //下载队列为空或者下载队列中不包含当前任务,添加下载任务
        if(downloadQueue.isEmpty()){
            Log.i(TAG,"添加下载任务：hashCode =" + downloadTask.hashCode() + "  ;地址：" + downloadTask);
            downloadQueue.add(downloadTask);
            download();
            return true ;
        }else if(!isContains(downloadTask)){
            Log.i(TAG,"添加下载任务：hashCode =" + downloadTask.hashCode() + "  ;地址：" + downloadTask);
            downloadQueue.add(downloadTask);
            download();
            return true ;
        }
        Log.i(TAG,"任务已存在：" + downloadTask);
        return false;
    }

    private boolean isContains(DownloadTask downloadTask){
        for(DownloadTask task:downloadQueue){
            if(task.getFile_path().equals(downloadTask.getFile_path())){
                return true ;
            }
        }

        return false;
    }

    //移除下载任务
    private boolean removeDownloadTask(DownloadTask downloadTask){
        if(downloadQueue.contains(downloadTask)){
            Log.i(TAG,"删除下载任务：" + downloadTask);
            downloadQueue.remove(downloadTask);
            download();
            return true ;
        }
        return false;
    }

    //下载
    private void download(final DownloadTask downloadObject){
        Log.i(TAG,"准备下载：filePath =" + downloadObject.getFile_path());
        if(downloadObject.getDownloadListener() != null){
            DownloadListenerImpl.getInstance().setDownloadListener(downloadObject.getDownloadListener());
        }
        DownloadListenerImpl.getInstance().onStartDownLoad(downloadObject);
        RetrofitHelper.getInstance()
                .getRetrofit()
                .create(DownloadService.class)
                .download(downloadObject.getDownload_url())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(new Func1<ResponseBody, InputStream>() {
                    @Override
                    public InputStream call(ResponseBody responseBody) {
                        return responseBody.byteStream();
                    }
                })
                .subscribe(new Action1<InputStream>() {
                    @Override
                    public void call(InputStream inputStream) {
                        Log.i(TAG,"开始下载文件");
                        try {
                            writeFile(inputStream, downloadObject.getFile_path());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG,"下载完成");
                        //下载完成，执行下载完成回调
                        DownloadListenerImpl.getInstance().onFinishDownload();
                        removeDownloadTask(downloadObject);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG,"下载出错");
                        removeDownloadTask(downloadObject);
                        DownloadListenerImpl.getInstance().onError(throwable.getMessage());
                    }
                });
    }

    //写文件
    private void writeFile(InputStream inputStream,String filePath) throws IOException {
        File file = new File(filePath);
        if(file.exists()){
            file.delete();
            Log.i(TAG,"文件已存在，删除文件");
        }

        FileOutputStream fileOutputStream = null ;

        try {
            fileOutputStream = new FileOutputStream(file);

            byte[] bytes = new byte[1024];
            int length ;
            while((length = inputStream.read(bytes)) != -1 ){
                fileOutputStream.write(bytes,0,length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            DownloadListenerImpl.getInstance().onError(e.getMessage());
        }finally {
            if(fileOutputStream!= null){
                fileOutputStream.close();
            }
            if(inputStream != null){
                inputStream.close();
            }
        }

    }
}
