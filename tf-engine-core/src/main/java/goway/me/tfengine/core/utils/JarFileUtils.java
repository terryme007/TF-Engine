package goway.me.tfengine.core.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
            String[] pkgSplit = apiReference.split("\\.");
            String org=pkgSplit[0]+"."+pkgSplit[1];
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
                    if(typeName.contains(org)){
                        outClassList.add(paramType);
                    }
                }
                String typeName=returnType.getName();
                System.out.println(typeName);
                if(typeName.contains(org)){
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

    //从jar包中加载指定package下的类
    public static Set<Class<?>> getPackageService(URL url, String pack){
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();// 第一个class类的集合
        boolean recursive = true;// 是否循环迭代
        String packageName = pack;// 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');

        JarFile jar;
        try {
            // 获取jar
            jar = new JarFile(url.getFile());
            // 从此jar包 得到一个枚举类
            Enumeration<JarEntry> entries = jar.entries();
            // 同样的进行循环迭代
            while (entries.hasMoreElements()) {
                // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                // 如果是以/开头的
                if (name.charAt(0) == '/') {
                    // 获取后面的字符串
                    name = name.substring(1);
                }
                // 如果前半部分和定义的包名相同
                if (name.startsWith(packageDirName)) {
                    int idx = name.lastIndexOf('/');
                    // 如果以"/"结尾 是一个包
                    if (idx != -1) {
                        // 获取包名 把"/"替换成"."
                        packageName = name.substring(0, idx).replace('/', '.');
                    }
                    // 如果可以迭代下去 并且是一个包
                    if ((idx != -1) || recursive) {
                        // 如果是一个.class文件 而且不是目录
                        if (name.endsWith(".class") && !entry.isDirectory()) {
                            // 去掉后面的".class" 获取真正的类名
                            String className = name.substring(packageName.length() + 1, name.length() - 6);
                            try {
                                // 添加到classes
                                Class<?> extClass = Class.forName(packageName + '.' + className);
                                classes.add(extClass);
                            } catch (Exception e) {
                                // log.error("添加用户自定义视图类错误 找不到此类的.class文件");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // log.error("在扫描用户定义视图时从jar包获取文件出错");
            e.printStackTrace();
        }
        return classes;
    }

    /**
     * 获取目录下的文件
     * @param obj
     * @return
     */
    public static ArrayList<File> getListFiles(Object obj,String fileType) {
        File directory = null;
        if (obj instanceof File) {
            directory = (File) obj;
        } else {
            directory = new File(obj.toString());
        }
        ArrayList<File> files = new ArrayList<File>();
        if (directory.isFile()) {
            if(StringUtils.isNotBlank(fileType)){
                int lastDot = directory.getName().lastIndexOf(".");
                if(lastDot>0 && fileType.equals(directory.getName().substring(lastDot+1))){
                    files.add(directory);
                }
            }

            return files;
        } else if (directory.isDirectory()) {
            if(!directory.getName().equals(".git") && !directory.getName().equals("targer") && !directory.getName().equals("logs")){
                File[] fileArr = directory.listFiles();
                for (int i = 0; i < fileArr.length; i++) {
                    File fileOne = fileArr[i];
                    files.addAll(getListFiles(fileOne,fileType));
                }
            }
        }
        return files;
    }


}
