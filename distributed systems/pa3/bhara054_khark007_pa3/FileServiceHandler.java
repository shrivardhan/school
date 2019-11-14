import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.nio.file.*;
import java.io.*;

public class FileServiceHandler implements FileService.Iface {
  static String coordinatorIp; // coordinator ip
  static String coordinatorPort; // coordinator port
  static String port; // port of the server
  static Integer id; // id for the number of requests
  static String ip; // server ip
  static HashMap<Integer,Integer> status; //  map to store the version of each request
  static boolean shouldSync; // check if sync is already in process
  static Lock lock; // lock to generate IDs

  public static class Lock {//lock class
    private boolean isLocked = false;
    public boolean getLock() { // get status of the lock without changing it
      return isLocked;
    }

    public synchronized void lock() { // get the lock
      try {
        while(isLocked) {
          wait();  // wait for unlock
        }
      } catch (Exception e) {
        System.out.println("Could not get lock "+e);
      }
      isLocked = true;
    }

    public synchronized void unlock() { // release the lock
      isLocked = false;
      notify(); // notify unlock
    }
  }

  public FileServiceHandler(String sIp, String sPort , String curPort, String cIP) { // constructor
    coordinatorIp = sIp;
    coordinatorPort = sPort;
    port = curPort;
    id = 0;
    status = new HashMap<Integer,Integer>();
    ip = cIP;
    shouldSync = true;
    lock = new Lock();
    Runnable simple1 = new Runnable() { // start sybcing with the cordinator
      public void run() {
        sync(); // start sync
      }
    };
    new Thread(simple1).start(); // thread starting
  }

  public static void sync() {
    while(true) { // run forever
      try {
        Thread.sleep(15000); // sync every 15 seconds
      } catch (Exception e) {
        System.out.println("Something went wrong in sleeping "+e);
      }
      if(shouldSync) {
        shouldSync = false; // when sync is already running, dont sync again
        contactCoordinator("all","sync"); // contact coordinator
      }
    }
  }

  public static void setPort(String p) {
    port = p; // set the port
  }

  public static String contactCoordinator(String filename, String type) { // contact the coordinator for all operations
    try {
      lock.lock(); // get the lock so that there is no conflict of ids
      id = id + 1; // incrememnt global id
      Integer curId = id; // store id locally
      status.put(curId,-1); // put it in status to track the completion
      lock.unlock(); // remove lock
      TTransport transport = new TSocket(coordinatorIp, Integer.parseInt(coordinatorPort));
      TBinaryProtocol protocol = new TBinaryProtocol(transport);
      transport.open();
      TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "QuorumService");
      QuorumService.Client hostNodeClient = new QuorumService.Client(mp);
      String nodeVals = hostNodeClient.quorum(filename,ip+":"+port,curId+"",type); // contact the coordinator
      while(status.get(curId)==-1 && !type.equals("sync")) {
        Thread.sleep(250); // wait till the version is returned to the server. -1 is the version till no update from coordinator
      }
      transport.close();
      return status.get(curId)+""; // return the version
    } catch (Exception e) {
      System.out.println("Something went wrong "+ e);
      if(type.equals("sync"))
        shouldSync = true; // now sync is complete can run again. Needed in case of failures
    }
    return "end";
  }

  @Override
  public String read(String filename) { // for read operation
    String fileval = "no file present";
    try{ // read the contents of the file
      fileval = (new String(Files.readAllBytes(Paths.get((new File(filename)).getPath()))));
    } catch (Exception e) {
      System.out.println("Could not read file " +e);
      return fileval;
    }
    return fileval+":::"+contactCoordinator(filename,"read"); // contact coordinator
  }

  @Override
  public String write(String filename) { // for write operation
    return contactCoordinator(filename,"write"); // contact coordinator
  }

  @Override
  public String onComplete(String ID, int val) { // on request completion. request ID and its version are parameters
    if(val == -1) { // if sync operation
      VersionServiceHandler v = new VersionServiceHandler(false);
      v.updateVersion(ID); // update all the files returned from the server
      shouldSync = true; // can start with sync again now. sync is complete
      return "end sync";
    }
    int id = Integer.parseInt(ID);
    // System.out.println(ID+" : " +val);
    status.put(id,val); // update the version in status map for the reques
    return "end";
  }

  @Override
  public boolean ping() {
    return true;
  }

}
