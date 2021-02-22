package goway.me.tfengine.core.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JarFileUtils {

    /**
     *
     * @param apiReference DubboService的全路径
     * @param modelName 模块名称
     * @param version 版本号
     * @return jar文件的base64
     */
    public static String getJarBase64(String apiReference,String modelName,String version) {
        String dirRoot="copyClass"+ UUID.randomUUID().toString().replaceAll("-","");
        try {
            Class<?> aClass = Class.forName(apiReference);
            //获取要导出的文件列表
            List<Class> outClassList=new ArrayList<>();
            outClassList.add(aClass);
            Method[] methods = aClass.getMethods();
            for(Method method:methods){
                Class<?> returnType = method.getReturnType();
                Class<?>[] parameterTypes = method.getParameterTypes();
                for (Class paramType:parameterTypes){
                    String typeName=paramType.getName();
                    System.out.println(typeName);
                    if(typeName.contains(modelName)){
                        outClassList.add(paramType);
                    }
                }
                String typeName=returnType.getName();
                System.out.println(typeName);
                if(typeName.contains(modelName)){
                    outClassList.add(returnType);
                }
            }
            outClassList.forEach(outClass -> {
                String className=outClass.getSimpleName();
                InputStream is = outClass.getResourceAsStream(className+".class");
                String path = dirRoot+"/"+outClass.getName().replaceAll("\\.","/")+".class";
                File destFile=new File(path);
                if (!destFile.getParentFile().exists()) {
                    boolean mkdirsSuccess = destFile.getParentFile().mkdirs();
                    if(!mkdirsSuccess){
                        throw new RuntimeException(String.format("无法创建目录【%s】",destFile.getParentFile().getAbsolutePath()));
                    }
                }
                FileOutputStream fos=null;
                try{
                    fos=new FileOutputStream(path);
                    byte[] bytes=new byte[1];
                    while (is.read(bytes)!=-1){
                        fos.write(bytes);
                    }
                    fos.flush();
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(null!=fos){
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            String savePath=dirRoot+"/"+apiReference.substring(0,apiReference.indexOf("."));
            String jarName=String.format("%s_%s_%s.jar",modelName,aClass.getSimpleName(),version);
            String zipFilePath=dirRoot+"/"+ jarName;
            ZipUtils.toZip(savePath,new FileOutputStream(zipFilePath),true);
            return Base64FileUtil.getFileStr(zipFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            //删除class文件夹和jar包
            delete(new File(dirRoot));
        }
        return null;
    }

    //删除文件或文件夹
    private static boolean delete(File file){
        if(!file.exists()){
            return true;
        }
        File[] files =null;
        if(file.isDirectory()){
            //文件夹
            files=file.listFiles();
        }
        if(files!=null && files.length>0){
            for(File f: files){
                boolean deleteSuccess = delete(f);
                if(!deleteSuccess){
                    return false;
                }
            }
        }
        return file.delete();
    }


}
