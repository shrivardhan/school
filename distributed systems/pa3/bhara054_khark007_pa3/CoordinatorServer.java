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

public class CoordinatorServer {

  public static void main(String [] args) {
    try {
      HashMap<String,String> confs = new HashMap(); // for the properties in the config file
      {
        String[] confVals = (new String(Files.readAllBytes(Paths.get((new File("config")).getPath())))).split("\n"); // read the config file
        for(String cv : confVals) {
          String[] cVals = cv.split("="); // split it to get key = value pair
          confs.put(cVals[0],cVals[1]); // add it to the map
        }
      }
      int readQ = Integer.parseInt(confs.get("readQuorum")); // read quorum from config
      int writeQ = Integer.parseInt(confs.get("writeQuorum")); // write quorum from config
      String superNodePort = confs.get("CoordinatorPort"); // get the coordinator port from config
      int totalServers = confs.get("servers").split(",").length; // get all the servers
      if(writeQ <= totalServers/2) { // do validation for write quorum
        System.out.println("wrong write quorum \nChanging it to "+totalServers+"/2+1 = "+(totalServers/2+1));
        writeQ = totalServers/2 + 1; // if fails, write quorum = totalserer/2 +1
      }
      if(readQ + writeQ < totalServers) { // validation on read quorum
        System.out.println("wrong read quorum \nChanging it to "+totalServers+"-"+writeQ+"+1 = "+(totalServers-writeQ+1));
        readQ = totalServers-writeQ+1; // if fails read quorum = totatservers - write quorum + 1
      }
      TMultiplexedProcessor processor = new TMultiplexedProcessor();
      QuorumServiceHandler h = new QuorumServiceHandler(readQ,writeQ);
      // processor.registerProcessor("FileService", new FileService.Processor<FileService.Iface>(h));
      processor.registerProcessor("QuorumService", new QuorumService.Processor<QuorumService.Iface>(h));
      Runnable simple = new Runnable() {
        public void run() {
          try{
            TServerTransport t = new TServerSocket(Integer.parseInt(superNodePort));
            TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(t).processor(processor));
            server.serve(); // start the server
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
