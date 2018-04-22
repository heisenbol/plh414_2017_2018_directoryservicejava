package tuc.sk;
 
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*; 
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import org.json.*;

import org.apache.zookeeper.*;


 
public class Directoryservice extends HttpServlet {
	
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			   throws IOException, ServletException {
		
		request.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		if (Zooconf.getInstance().getServiceConfig() == null) { 
			System.err.println("Service not configured");
			response.setContentType("text/html");
			response.sendError(response.SC_SERVICE_UNAVAILABLE , "Service not configured");
			return;
		}
		
		
		String path = request.getPathInfo();
		if (path == null || path.equals("/")) {
			response.setContentType("text/html");
			response.sendError(response.SC_NOT_FOUND, "Missing command 1");
			return;
		}
		String[] parts = path.split("/");
		if (parts.length == 2 && parts[1].equals("status")) {
			response.setContentType("text/plain;charset=UTF-8");
			String statusText = getStatusText();
			out.println(statusText);
		}
		else {
			response.setContentType("text/html");
			response.sendError(response.SC_NOT_FOUND, "Invalid command ");
			return;
		}

	}
	
	private String getStatusText() {
		String result = "";
		String rootPath = "/plh414";
		try {
			ZooKeeper zk = Zooconf.getZooConnection();
			List<String> authChildren = zk.getChildren(rootPath+"/authservices", false);
			List<String> fsChildren = zk.getChildren(rootPath+"/fileservices", false);
			byte[] dsDataBytes = zk.getData(rootPath+"/directoryservice", null, null);
			result = "Directoryservice data: "+ new String(dsDataBytes, "UTF-8")+"\n";
			
			for (String auth : authChildren) {
				byte[] authDataBytes = zk.getData(rootPath+"/authservices/"+auth, null, null);
				result = result + "Auth data for "+auth+": "+ new String(authDataBytes, "UTF-8")+"\n";
			}
			result = result + "\nFS List retrieved now\n";
			for (String fs : fsChildren) {
				byte[] fsDataBytes = zk.getData(rootPath+"/fileservices/"+fs, null, null);
				result = result + "FS data for "+fs+": "+ new String(fsDataBytes, "UTF-8")+"\n";
			}
			result = result + "\nFS List names according to watched list\n ";
			for (String fs : Zooconf.getAvailableFs()) {
				result = result + fs+", ";
			}
			
		}
		catch (KeeperException ex) {
			System.err.println("getStatusText KeeperException "+ex.getMessage());
		}
		catch (InterruptedException ex) {
			System.err.println("getStatusText InterruptedException");
		}
		catch (UnsupportedEncodingException ex) {
			System.err.println("getStatusText UnsupportedEncodingException");
		}
		return result;
	}
	
}
