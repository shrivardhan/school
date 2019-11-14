import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.TMultiplexedProcessor;
import java.util.Scanner;
import java.nio.file.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.net.InetAddress;

public class FileServer {

  public static void main(String [] args) {
    try {
      HashMap<String,String> confs = new HashMap(); // map to hold the properties of the config

      String[] confVals = (new String(Files.readAllBytes(Paths.get((new File("config")).getPath())))).split("\n"); // read from config file
      for(String cv : confVals) {
        String[] cVals = cv.split("="); // split it to get key value pairs
        confs.put(cVals[0],cVals[1]);
      }

      // String port = confs.get("ServerPort");
      String superNodeIp  = confs.get("CoordinatorIp"); // coordinator ip from config
      String superNodePort = confs.get("CoordinatorPort"); // coordinator port from config
      String curIp = InetAddress.getLocalHost().getHostAddress().toString(); // get local ip
      String curHost = InetAddress.getLocalHost().getHostName(); // get local hostname
      ArrayList<String> ports = new ArrayList<String>();
      TMultiplexedProcessor processor = new TMultiplexedProcessor();
      FileServiceHandler h = new FileServiceHandler(superNodeIp,superNodePort,"8080",curIp);
      processor.registerProcessor("FileService", new  FileService.Processor<FileService.Iface>(h));
      processor.registerProcessor("VersionService", new   VersionService.Processor<VersionService.Iface>(new VersionServiceHandler()));
      for(String cv : confVals) {
        String[] cVals = cv.split("=");
        if(cVals[0].equals("servers")) { // get all the servers
          String[] machineVals = cVals[1].split(","); // split the servers
          for(String m:machineVals) {
            String[] ipPorts = m.split(":"); // get ip:port

            if(ipPorts[0].equals(curIp)||ipPorts[0].equals(curHost)) { // check if the server is the same as this server
              ports.add(ipPorts[1]); // add to the list of ports if above is true
            }
          }
        }
      }
      Runnable simple = new Runnable() {
        public void run() {
          try{
            TServerTransport t = null;
            boolean retry = false;
            int i = 0;
            do {
              try { // check for ports that are still available
                String port = ports.get(i);
                t = new TServerSocket(Integer.parseInt(port));
                retry = true;
                FileServiceHandler.setPort(port);
              } catch(Exception e) {
                ++i; // port is already taken
              }
            } while(!retry && i < ports.size());
            if(!retry) {
              System.out.println("Could not start server"); // all ports were taken or repeated in config
              return;
            }
            TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(t).processor(processor));
            server.serve();
          } catch(Exception e) {
            e.printStackTrace();
          }
        }
      };
      new Thread(simple).start();
    } catch (Exception x) {
      x.printStackTrace();
    }
  }
}
