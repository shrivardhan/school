import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.TMultiplexedProcessor;
import java.util.Scanner;
import java.nio.file.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;

public class SuperNodeServer {

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

      int m = Integer.parseInt(confs.get("DHTEntries"));
      int hostPort = Integer.parseInt(confs.get("HostNodePort"));
      TMultiplexedProcessor processor = new TMultiplexedProcessor();
      SuperNodeHostHandler h = new SuperNodeHostHandler(m,hostPort);
      processor.registerProcessor("SuperNodeHostService", new SuperNodeHostService.Processor<SuperNodeHostService.Iface>(h));
      processor.registerProcessor("SuperNodeClientService", new SuperNodeClientService.Processor<SuperNodeClientService.Iface>(new SuperNodeClientHandler()));
      System.out.println("Starting the server...");
      Runnable simple = new Runnable() {
        public void run() {
          try{
            TServerTransport t = new TServerSocket(Integer.parseInt(confs.get("SuperNodePort")));
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
