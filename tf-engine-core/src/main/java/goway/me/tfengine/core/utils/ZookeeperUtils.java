package goway.me.tfengine.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZookeeperUtils {

    private static Map<String,CuratorFramework> curMap=new HashMap<>();

    public static CuratorFramework connectZk(String zkAddress){
        CuratorFramework cur=curMap.get(zkAddress);
        if(cur==null){
            synchronized (ZookeeperUtils.class){
                cur=CuratorFrameworkFactory.builder()
                        .connectString(zkAddress)
                        .connectionTimeoutMs(5000)
                        .retryPolicy(new ExponentialBackoffRetry(1000,3))
                        .build();
                cur.start();//连接
                curMap.put(zkAddress,cur);
            }
        }
        return cur;
    }

    public static void createListen(String zkAddress,String rootPath){
        //创建监听
        CuratorFramework cur=connectZk(zkAddress);
        PathChildrenCache cache=new PathChildrenCache(cur,rootPath,true);
        try{
            cache.start();
            cache.rebuild();
            cache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework framwork, PathChildrenCacheEvent event) throws Exception {
                    System.err.println("节点发生变化:"+event.getType());
                    switch (event.getType()){
                        case CHILD_ADDED:
                            System.out.println("增加子节点");
                            break;
                        case CHILD_REMOVED:
                            System.out.println("删除子节点");
                            break;
                        case CHILD_UPDATED:
                            System.out.println("更新子节点");
                            break;
                        default:break;
                    }
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void del(String zkAddress,String path){
        //删除节点
        CuratorFramework cur=connectZk(zkAddress);
        try{
            System.out.println("准备删除"+path);
            Stat stat=cur.checkExists().forPath(path);
            if(stat!=null){
                List<String> childrenNodeList = cur.getChildren().forPath(path);
                if(childrenNodeList==null || childrenNodeList.size()==0){
                    //节点为空，直接删除
                    System.out.println(path+"节点存在，直接删除");
                    cur.delete().forPath(path);
                }else{
                    //节点不为空
                    for(String childrenNode:childrenNodeList){
                        del(zkAddress,path+"/"+childrenNode);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void create(String zkAddress,String path,String data){
        //创建节点
        CuratorFramework cur=connectZk(zkAddress);
        try{
            System.out.println("准备创建"+path);
            String[] pathTree = path.split("/");
            StringBuilder pathSb=new StringBuilder();
            if(pathTree.length>0){
                String lastPath=pathTree[pathTree.length-1];
                for(String nowPath:pathTree){
                    if (StringUtils.isBlank(nowPath)){
                        continue;
                    }
                    pathSb.append("/").append(nowPath);
                    if(cur.checkExists().forPath(pathSb.toString())==null){
                        if(nowPath.equals(lastPath)){
                            cur.create().withMode(CreateMode.PERSISTENT)
                                    .forPath(pathSb.toString(), data.getBytes());
                        }else{
                            cur.create().withMode(CreateMode.PERSISTENT).forPath(pathSb.toString());
                        }
                    }
                }

            }
            System.out.println("节点"+path+"创建成功");
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static String get(String zkAddress,String path){
        //创建节点
        CuratorFramework cur=connectZk(zkAddress);
        try{
            //获取节点数据
            System.out.println("准备获取"+path);
            byte[] bs=cur.getData().forPath(path);
            String data = new String(bs);
            System.out.println("数据:"+data);
            return data;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getChildren(String zkAddress,String path){
        //创建节点
        CuratorFramework cur=connectZk(zkAddress);
        try{
            //获取节点数据
            System.out.println("准备获取子节点"+path);
            List<String> fullChildren=new ArrayList<>();
            List<String> childrenList = cur.getChildren().forPath(path);
            for(String childrenNode:childrenList){
                String nowFullPath = path + "/" + childrenNode;
                List<String> list = cur.getChildren().forPath(nowFullPath);
                if(list==null || list.size()==0){
                    //最终节点
                    fullChildren.add(nowFullPath);
                }else{
                    fullChildren.addAll(getChildren(zkAddress,nowFullPath));
                }
            }
            System.out.println("子节点:"+fullChildren);
            return fullChildren;
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static void main(String[] args) throws Exception {
        String zkAddress="localhost:2181";
        String rootPath="/tfengine";

        createListen(zkAddress,rootPath);

        String path=rootPath+"/dubbo_api/";

        create(zkAddress,path+"dubbo_provider_Service1_v1","hello");
        Thread.sleep(1000);
        create(zkAddress,path+"dubbo_provider_Service1_v2","hello");
        Thread.sleep(1000);
        create(zkAddress,path+"dubbo_provider_Service1_v3","hello");
        Thread.sleep(1000);

        try{
            List<String> childrenList = getChildren(zkAddress, rootPath);
            childrenList.forEach(childrenPath->{
                String data = get(zkAddress, childrenPath);
                System.out.println(data);
            });
        }catch (Exception e){
            e.printStackTrace();
        }

        del(zkAddress,rootPath);
        Thread.sleep(1000);

    }


    /**
     * 三种watcher来做节点的监听
     * pathcache   监视一个路径下子节点的创建、删除、节点数据更新
     * NodeCache   监视一个节点的创建、更新、删除
     * TreeCache   pathcaceh+nodecache 的合体（监视路径下的创建、更新、删除事件），
     * 缓存路径下的所有子节点的数据
     */

//    public static void main1(String[] args) throws Exception {
//        String connStr = "192.168.23.24:2181";
//        CuratorFramework curatorFramework=CuratorFrameworkFactory.builder()
//                .connectString(connStr)
//                .connectionTimeoutMs(5000)
//                .retryPolicy(new ExponentialBackoffRetry(1000,3))
//                .build();
//        curatorFramework.start();
//
//        /**
//         * 节点变化NodeCache
//         */
//       /* NodeCache cache=new NodeCache(curatorFramework,"/curator",false);
//        cache.start(true);
//
//        cache.getListenable().addListener(()-> System.out.println("节点数据发生变化,变化后的结果" +
//                "："+new String(cache.getCurrentData().getData())));
//
//        curatorFramework.setData().forPath("/curator","菲菲".getBytes());*/
//
//
//        /**
//         * PatchChildrenCache
//         */
//
//        PathChildrenCache cache=new PathChildrenCache(curatorFramework,"/event",true);
//        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
//        cache.rebuild();
//        // Normal / BUILD_INITIAL_CACHE /POST_INITIALIZED_EVENT
//
//        cache.getListenable().addListener((curatorFramework1,pathChildrenCacheEvent)->{
//            switch (pathChildrenCacheEvent.getType()){
//                case CHILD_ADDED:
//                    System.out.println("增加子节点");
//                    break;
//                case CHILD_REMOVED:
//                    System.out.println("删除子节点");
//                    break;
//                case CHILD_UPDATED:
//                    System.out.println("更新子节点");
//                    break;
//                default:break;
//            }
//        });
//
//        //  curatorFramework.create().withMode(CreateMode.PERSISTENT).forPath("/event","event".getBytes());
//        // TimeUnit.SECONDS.sleep(1);
//        // System.out.println("1");
////        curatorFramework.create().withMode(CreateMode.PERSISTENT).forPath("/event/event1","1".getBytes());
////        TimeUnit.SECONDS.sleep(1);
////        System.out.println("2");
////
////        curatorFramework.setData().forPath("/event/event1","222".getBytes());
////        TimeUnit.SECONDS.sleep(1);
////        System.out.println("3");
//
//        curatorFramework.delete().forPath("/event/event1");
//        System.out.println("4");
//
//
//
//
//        System.in.read();
//
//    }
}
