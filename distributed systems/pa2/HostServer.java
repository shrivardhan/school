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

public class HostServer {

  public static void main(String [] args) {
    try {
      HashMap<String,String> confs = new HashMap();
      {
        String[] confVals = (new String(Files.readAllBytes(Paths.get((new File("config")).getPath())))).split("\n");
        for(String cv : confVals) {
          String[] cVals = cv.split(":");
          confs.put(cVals[0],cVals[1]);
        }
      }
      String port = confs.get("HostNodePort"); // get from command line?
      String superNodeIp  = confs.get("SuperNodeIp");//"csel-kh4250-06"; // get from command line
      String superNodePort = confs.get("SuperNodePort");//"8010"; // get from command line
      TMultiplexedProcessor processor = new TMultiplexedProcessor();
      HostNodeHandler h = new HostNodeHandler(superNodeIp,superNodePort,port);
      processor.registerProcessor("HostService", new  HostService.Processor<HostService.Iface>(h));
      processor.registerProcessor("HostClientService", new   HostClientService.Processor<HostClientService.Iface>(new HostClientHandler()));
      Runnable simple = new Runnable() {
        public void run() {
          try{
            TServerTransport t = new TServerSocket(Integer.parseInt(port));
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
