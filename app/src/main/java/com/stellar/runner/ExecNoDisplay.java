package com.stellar.runner;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import java.io.OutputStream;

import roro.stellar.Stellar;

public class ExecNoDisplay extends Activity {

    Process p;
    Thread h1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        h1 = new Thread(new Runnable() {
            @Override
            public void run() {
                StellarExec(getIntent().getStringExtra("content"));
            }
        });
        h1.start();
        super.onCreate(savedInstanceState);
    }


    public void StellarExec(String cmd) {
        try {
            //使用Stellar执行命令
            p = Stellar.INSTANCE.newProcess(new String[]{"sh"}, null, null);
            OutputStream out = p.getOutputStream();
            out.write((cmd + "\n").getBytes());
            out.flush();
            out.close();
            //等待命令运行完毕
            p.waitFor();

        } catch (Exception ignored) {
        }
    }


    @Override
    public void onDestroy() {
        //关闭所有输入输出流，销毁进程，防止内存泄漏等问题
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= 26) {
                        p.destroyForcibly();
                    } else {
                        p.destroy();
                    }
                    h1.interrupt();
                } catch (Exception ignored) {
                }
            }
        });
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        finish();
        super.onResume();
    }
}
