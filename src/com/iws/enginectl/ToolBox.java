package com.iws.enginectl;

import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ToolBox {
    public static JSONObject jsonFileReader(String filePath){
        StringBuilder json= new StringBuilder();
        File file = new File(filePath);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            byte[] buf = new byte[1024];
            int length = 0;

            while((length = fileInputStream.read(buf)) != -1){
                json.append(new String(buf,0,length));
            }
            fileInputStream.close();
            return JSONObject.parseObject(json.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteFile(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }
}
