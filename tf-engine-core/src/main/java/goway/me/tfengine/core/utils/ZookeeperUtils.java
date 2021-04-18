package goway.me.tfengine.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
                    log.info("ZookeeperUtils.Listen 节点发生变化:"+event.getType());
                    switch (event.getType()){
                        case CHILD_ADDED:
                            log.info("ZookeeperUtils.Listen 增加子节点");
                            break;
                        case CHILD_REMOVED:
                            log.info("ZookeeperUtils.Listen 删除子节点");
                            break;
                        case CHILD_UPDATED:
                            log.info("ZookeeperUtils.Listen 更新子节点");
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
            log.info("ZookeeperUtils.del 准备删除节点{}",path);
            Stat stat=cur.checkExists().forPath(path);
            if(stat!=null){
                List<String> childrenNodeList = cur.getChildren().forPath(path);
                if(childrenNodeList==null || childrenNodeList.size()==0){
                    //节点为空，直接删除
                    log.info("ZookeeperUtils.del {}节点存在，直接删除",path);
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

    public static void create(String zkAddress,String path,String data,CreateMode createMode){
        //创建节点
        CuratorFramework cur=connectZk(zkAddress);
        try{
            log.info("ZookeeperUtils.create 准备创建节点{}",path);
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
                            cur.create().withMode(createMode)
                                    .forPath(pathSb.toString(), data.getBytes());
                        }else{
                            cur.create().withMode(createMode).forPath(pathSb.toString());
                        }
                    }
                }

            }
            log.info("ZookeeperUtils.create 节点{}创建成功",path);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static String get(String zkAddress,String path){
        //创建节点
        CuratorFramework cur=connectZk(zkAddress);
        try{
            //获取节点数据
            log.info("ZookeeperUtils.get 准备获取节点{}",path);
            byte[] bs=cur.getData().forPath(path);
            String data = new String(bs);
            log.info("ZookeeperUtils.get 数据：{}",data);
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
            log.info("ZookeeperUtils.getChildren 准备获取子节点{}",path);
            if(cur.checkExists().forPath(path)==null){
                return new ArrayList<>();
            }
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
            log.info("ZookeeperUtils.getChildren 子节点：{}",fullChildren);
            System.out.println("子节点:"+fullChildren);
            return fullChildren;
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
