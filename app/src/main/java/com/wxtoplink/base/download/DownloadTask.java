package com.wxtoplink.base.download;

import com.wxtoplink.base.download.listener.DownloadListener;

/**
 * Created by 12852 on 2018/7/26.
 */

public class DownloadTask {

    //下载路径
    private String download_url ;
    //下载的文件名
    private String file_name ;
    //文件的保存路径，全路径
    private String file_path ;
    //下载文件的监听器
    private DownloadListener downloadListener ;

    public DownloadTask(String download_url, String file_name, String file_path, DownloadListener downloadListener) {
        this.download_url = download_url;
        this.file_name = file_name;
        this.file_path = file_path;
        this.downloadListener = downloadListener;
    }

    public String getDownload_url() {
        return download_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }
}
