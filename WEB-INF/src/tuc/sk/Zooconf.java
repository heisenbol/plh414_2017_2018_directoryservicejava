package tuc.sk;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;

import java.io.*;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.*;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import java.util.concurrent.CountDownLatch;
import java.util.*;
import org.json.*;
import java.security.*;

@WebListener
public class Zooconf implements ServletContextListener {
	private Properties serviceConfig = null;
	private ZooKeeper zk = null;
	private List<String> fsList = null;
	final CountDownLatch connectedSignal = new CountDownLatch(1);
	private static Zooconf zooConfInstance = null;
	
    class FsWatcher implements Watcher {
        
        public void process(WatchedEvent event) {
            System.err.println("Watcher triggered");
            
            // watches are one time events. So rewatch for changes. This will just reload all available FS's
            // could be smarter to check which specific FS was removed/added using the event object
			Watcher watcher = new FsWatcher();
			watchForFsChanges(watcher);
        }
    }

	private void initFsWatches() {
		fsList = Collections.synchronizedList(new ArrayList<String>());
		Watcher watcher = new FsWatcher();
		watchForFsChanges(watcher);
	}
	private void watchForFsChanges(Watcher watcher) {
		// we want to get the list of available FS, and watch for changes
		String rootPath = "/plh414";
		try {
			fsList.clear();
			List<String> fsChildren = zk.getChildren(rootPath+"/fileservices", watcher);
			for (String fs : fsChildren) {
				// TODO: need probably also it's associated data
				fsList.add(fs);
			}
		}
		catch (KeeperException ex) {
			System.err.println("getStatusText KeeperException "+ex.getMessage());
		}
		catch (InterruptedException ex) {
			System.err.println("getStatusText InterruptedException");
		}
	}
    public void contextInitialized(ServletContextEvent sce) {
        System.err.println("Directoryservice Context start initialization");
        initConfProperties(sce.getServletContext());
		Zooconf instance = getInstance();
		try {
			instance.zk = instance.zooConnect();
			instance.publishService();
			instance.initFsWatches();
		}
		catch (InterruptedException ex) {
			System.err.println("init InterruptedException");
		}
		catch (IOException ex) {
			System.err.println("init IOException");
		}
    }

    public void contextDestroyed(ServletContextEvent sce) {
        System.err.println("Directoryservice Context destroyed");
        Zooconf instance = getInstance();
		try {
			if (instance.zk != null) {
				instance.zk.close();
			}
		}
		catch ( InterruptedException ex) {
			System.err.println("destroy InterruptedException");
		}
    }
    
	public static Zooconf getInstance() {
		if (zooConfInstance == null) {
			zooConfInstance = new Zooconf();
		}
		return zooConfInstance;
	}
	
	public static ZooKeeper getZooConnection() {
		Zooconf instance = getInstance();
		return instance.zk;
	}
	public static Properties getServiceConfig() {
		Zooconf instance = getInstance();
		return instance.serviceConfig;
	}
	public static List<String> getAvailableFs() {
		Zooconf instance = getInstance();
		return instance.fsList;
	}
   	private void publishService() {
		// znode should already exist. Set data to this node
		Zooconf instance = getInstance();
		ACL acl = null;
		try {
			String base64EncodedSHA1Digest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA1").digest((instance.serviceConfig.getProperty("ZOOKEEPER_USER")+":"+instance.serviceConfig.getProperty("ZOOKEEPER_PASSWORD")).getBytes()));
			acl = new ACL(ZooDefs.Perms.ALL, new Id("digest",instance.serviceConfig.getProperty("ZOOKEEPER_USER")+":" + base64EncodedSHA1Digest));
		}
		catch (NoSuchAlgorithmException ex) {
			System.err.println("destroy NoSuchAlgorithmException");
		}
		try {
			JSONObject data = new JSONObject();
			data.put("SERVERHOSTNAME", instance.serviceConfig.getProperty("SERVERHOSTNAME"));
			data.put("SERVER_PORT", instance.serviceConfig.getProperty("SERVER_PORT"));
			data.put("SERVER_SCHEME", instance.serviceConfig.getProperty("SERVER_SCHEME"));
			data.put("CONTEXT", instance.serviceConfig.getProperty("CONTEXT"));
			instance.zk.setData("/plh414/directoryservice", data.toString().getBytes("UTF-8"), -1);
		}	
		catch (KeeperException ex) {
			System.err.println("create destroy KeeperException");
		}
		catch (InterruptedException ex) {
			System.err.println("create destroy InterruptedException");
		}
		catch (UnsupportedEncodingException ex) {
			System.err.println("create destroy UnsupportedEncodingException");
		}
	}
	private void initConfProperties(ServletContext servletContext) {
		Zooconf instance = getInstance();
		InputStream input = null;
		try {
			input = servletContext.getResourceAsStream("/WEB-INF/config.properties");
			if (input != null) {
				instance.serviceConfig = new Properties();
				instance.serviceConfig.load(input);
				System.err.println("Loaded configuration");
			}
			else {
		        System.err.println("Directoryservice configuration unavailable Error");
			}
			
			// TODO: check that all necessary parameters have been defined
		}
		catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private ZooKeeper zooConnect() throws IOException,InterruptedException {
		System.err.println("start zooConnect");
		
		Properties config = getServiceConfig();
		ZooKeeper zk = new ZooKeeper(config.getProperty("ZOOKEEPER_HOST"), 3000, new Watcher() {
			@Override
			public void process(WatchedEvent we) {
				if (we.getState() == KeeperState.SyncConnected) {
					connectedSignal.countDown();
				}
			}
		});
		connectedSignal.await();
		
		zk.addAuthInfo("digest", new String(config.getProperty("ZOOKEEPER_USER")+":"+config.getProperty("ZOOKEEPER_PASSWORD")).getBytes()); 
		
		System.err.println("finished zooConnect");

		return zk;
	}
}
