package com.projecttango.examples.java.pointcloud;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.projecttango.examples.java.pointcloud.rajawali.PointCloud;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by nara007 on 17/5/3.
 */

public class ClientSocket extends Thread {

    Socket socket;
    InputStream is;
    public Handler mHandler;
    private static final byte AZIMUTHMSG = 0x1;
    private static final byte BLUETOOTHMSG = 0x2;
    private static final byte COMMANDMSG = 0x3;

    ClientSocket(){

    }

    @Override
    public void run() {
        super.run();

        try{

            System.out.println("client start...");
//            socket  = new Socket("192.168.1.107",4200);
//            socket  = new Socket("141.76.21.205",4200);
            socket  = new Socket("172.26.144.63",4200);
            System.out.println("client started...");
            socket.setSoTimeout ( 5000 );//设置超时时间

            if (socket!=null)
            {
                System.out.println("网络连接成功");
            }else {

                System.out.println("网络连接失败");
            }

            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String info = null;

            byte[]tt=new byte[8];
            int b;
//            while((info=br.readLine())!=null){
//                System.out.println("Hello,我是客户端，服务器说："+info);
//                float f = Float.parseFloat(info);
//
////                System.out.println(f);
//                PointCloudActivity.azimuth = f;
//
//            }
            byte[] type = new byte[4];
            byte[] value = new byte[4];

            int msgType;
                while ((b = is.read(tt)) != -1) {
//                    System.out.println("length: "+b);

                    System.arraycopy(tt, 0, type, 0, 4);
                    System.arraycopy(tt, 4, value, 0, 4);

                    msgType =ClientSocket.getIntFromBytes(type);

//                    System.out.println("msgtype "+msgType+" length "+b);

                    if(msgType==AZIMUTHMSG){

                        PointCloudActivity.azimuth = (float)(getIntFromBytes(value)/100000.0f);
//                        System.out.println("azimuth "+ PointCloudActivity.azimuth);
                    }
                    else if(msgType==BLUETOOTHMSG){
                        int key = getIntFromBytes(value);
                        System.out.println("key from server "+key);

                        Message msg = new Message();
                        msg.what = COMMANDMSG;
                        msg.obj = key;

                        PointCloudActivity.myHandler.sendMessage(msg);

                    }

                    else{

                    }

                }


        }catch (Exception e){
            e.printStackTrace();
        }
    }

//    @Override
//    public void run() {
//        super.run();
//
//        try{
//
//            System.out.println("client start...");
//            socket  = new Socket("192.168.1.107",4200);
////            socket  = new Socket("141.76.22.38",4200);
//            System.out.println("client started...");
//            socket.setSoTimeout ( 5000 );//设置超时时间
//
//            if (socket!=null)
//            {
//                System.out.println("网络连接成功");
//            }else {
//
//                System.out.println("网络连接失败");
//            }
//
//            is = socket.getInputStream();
////            BufferedReader br = new BufferedReader(new InputStreamReader(is));
////            String info = null;
//
////            while((info=br.readLine())!=null){
////                System.out.println("Hello,我是客户端，服务器说："+info);
////                float f = Float.parseFloat(info);
////                PointCloudActivity.azimuth = f;
////
////            }
//
//
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        Looper.prepare();
//
//        mHandler = new Handler(){
//            @Override
//            public void handleMessage(Message msg) {
//                super.handleMessage(msg);
//
//                byte[] type = MainActivity.getByteArray(msg.what);
//                byte[] value;
//                if(msg.what == BLUETOOTHMSG){
//                    value = MainActivity.getByteArray((int)(msg.obj));
//                }
//                else if(msg.what == AZIMUTHMSG){
//                    value = MainActivity.getByteArray((float)(msg.obj));
//                }
//
//                else{
//                    System.out.println("no defined msg type");
//                    return;
//                }
////                    System.out.println("subthread key"+msg.obj);
//
//                MainActivity.this.OutputToClient(byteMerger(type, value));
//            }
//        };
//
//        Looper.loop();
//    }


//    static int getIntFromBytes(byte[] data){
//
//        int u = (int)(data [3] | data [2] << 8 |
//                data [1] << 16 | data [0] << 24);
//        return u;
//    }

    public static int getIntFromBytes(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }
}
