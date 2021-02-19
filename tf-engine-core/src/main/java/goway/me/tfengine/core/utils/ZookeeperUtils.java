package goway.me.tfengine.core.utils;

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

public class ZookeeperUtils {

    @Value("#{ @environment['zookeeper.address']?:'localhost:2181'}")
    private String zkAddress;

    public static CuratorFramework connectZk(String zkAddress){
        CuratorFramework cur=CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .connectionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000,3))
                .build();
        cur.start();//连接
        return cur;
    }

    public static void createListen(CuratorFramework cur,String rootPath){
        //创建监听
        PathChildrenCache cache=new PathChildrenCache(cur,rootPath,true);
        try{
            cache.start();
            cache.rebuild();
            cache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework framwork, PathChildrenCacheEvent event) throws Exception {
                    System.err.println("节点发生变化:"+event.getType());
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void del(CuratorFramework cur,String path){
        //删除节点
        try{
            System.out.println("准备删除"+path);
            Stat stat=cur.checkExists().forPath(path);
            if(stat!=null){
                System.out.println(path+"节点存在，直接删除");
                cur.delete().forPath(path);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void create(CuratorFramework cur,String path,String data){
        //创建节点
        try{
            System.out.println("准备创建"+path);
            cur.create().withMode(CreateMode.PERSISTENT)
                    .forPath(path, data.getBytes());
            System.out.println("节点"+path+"创建成功");
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static String get(CuratorFramework cur,String path){
        //创建节点
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

    public static void main(String[] args) throws Exception {
        String zkAddress="localhost:2181";
        String rootPath="/tfengine";

        CuratorFramework cur = connectZk(zkAddress);
        createListen(cur,rootPath);

        String path=rootPath+"/dubbo_provider";

        create(cur,path,"hello");
        Thread.sleep(1000);

        get(cur,path);
        Thread.sleep(1000);

        del(cur,path);
        Thread.sleep(1000);

    }


    /**
     * 三种watcher来做节点的监听
     * pathcache   监视一个路径下子节点的创建、删除、节点数据更新
     * NodeCache   监视一个节点的创建、更新、删除
     * TreeCache   pathcaceh+nodecache 的合体（监视路径下的创建、更新、删除事件），
     * 缓存路径下的所有子节点的数据
     */

    public static void main1(String[] args) throws Exception {
        String connStr = "192.168.23.24:2181";
        CuratorFramework curatorFramework=CuratorFrameworkFactory.builder()
                .connectString(connStr)
                .connectionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000,3))
                .build();
        curatorFramework.start();

        /**
         * 节点变化NodeCache
         */
       /* NodeCache cache=new NodeCache(curatorFramework,"/curator",false);
        cache.start(true);

        cache.getListenable().addListener(()-> System.out.println("节点数据发生变化,变化后的结果" +
                "："+new String(cache.getCurrentData().getData())));

        curatorFramework.setData().forPath("/curator","菲菲".getBytes());*/


        /**
         * PatchChildrenCache
         */

        PathChildrenCache cache=new PathChildrenCache(curatorFramework,"/event",true);
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        cache.rebuild();
        // Normal / BUILD_INITIAL_CACHE /POST_INITIALIZED_EVENT

        cache.getListenable().addListener((curatorFramework1,pathChildrenCacheEvent)->{
            switch (pathChildrenCacheEvent.getType()){
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
        });

        //  curatorFramework.create().withMode(CreateMode.PERSISTENT).forPath("/event","event".getBytes());
        // TimeUnit.SECONDS.sleep(1);
        // System.out.println("1");
//        curatorFramework.create().withMode(CreateMode.PERSISTENT).forPath("/event/event1","1".getBytes());
//        TimeUnit.SECONDS.sleep(1);
//        System.out.println("2");
//
//        curatorFramework.setData().forPath("/event/event1","222".getBytes());
//        TimeUnit.SECONDS.sleep(1);
//        System.out.println("3");

        curatorFramework.delete().forPath("/event/event1");
        System.out.println("4");




        System.in.read();

    }
}
