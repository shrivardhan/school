import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.util.Scanner;
import java.nio.file.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QuorumServiceHandler implements QuorumService.Iface {

  public static class Lock { // lock class
    private boolean isLocked = false;
    public boolean getLock() { // get status of the lock without changing it
      return isLocked;
    }

    public synchronized void lock() { // get the lock
      try {
        while(isLocked) {
          wait(); // wait for unlock
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

  public static class Request { // class to hold the details of the request from the servers and corresponding functions
    String reqType;
    String serverIp;
    String ID;
    String filename;
    public Request(String rT, String sIP, String id, String fn) {
      ID = id; // ID generated in server
      serverIp = sIP; // ip:port
      reqType = rT; // read,write,sync
      filename = fn; // file
    }

    public String getRequestType() {
      return reqType;
    }

    public String getServerIp() {
      return serverIp;
    }

    public String getID() {
      return ID;
    }

    public String getFilename() {
      return filename;
    }
  }

  static int readQ; // read quorum
  static int writeQ; // write quorum
  static HashMap<String,Lock> fileLocks; // filename - lock mapping
  static HashMap<String,ConcurrentLinkedQueue<Request>> reqs; // filename - queue mapping
  static ArrayList<String> machines; // list of all the servers to choose for quorum
  static HashMap<String,Integer> newVersions; // temperary hashmap to hold the version details during sync

  public QuorumServiceHandler(int rQ, int wQ) { // constructor to initialize all variables
    readQ = rQ;
    writeQ = wQ;
    fileLocks = new HashMap<String,Lock>();
    reqs = new HashMap<String,ConcurrentLinkedQueue<Request>>();
    machines = new ArrayList<String>();
    String[] confVals = null;
    try{
      confVals = (new String(Files.readAllBytes(Paths.get((new File("config")).getPath())))).split("\n"); // read all details from config
    } catch (Exception e) {
      System.out.println("Could not read config file "+e);
    }
    for(String cv : confVals) { // get the all servers' IP:port from config and store it locally
      String[] cVals = cv.split("=");
      if(cVals[0].equals("servers")) {
        String[] machineVals = cVals[1].split(",");
        for(String m:machineVals)
          machines.add(m);
      }
    }
  }

  @Override
  public String quorum(String filename, String serverIp, String ID, String type) { // is the function called by server for all operations
    if(type.equals("sync")) { // check if the type is a sync operation
      syncQuorum(serverIp);
      return "end sync";
    }
    Request req = new Request(type, serverIp, ID, filename);
    Lock fileLock = fileLocks.get(filename); // 1 lock per file which is used for write and sync
    if(fileLock == null) {
      try {
        fileLock = new Lock(); // create a lock if null
        fileLocks.put(filename,fileLock); // 1 lock per file
      } catch (Exception e) {
        System.out.println("could not get lock " + e);
      }
    }

    ConcurrentLinkedQueue fileQ = reqs.get(filename); // 1 concurrent queue per file
    fileLock.lock(); // lock before creating the queue to avoid multiple creations
    if(fileQ == null) {
      fileQ = new ConcurrentLinkedQueue<Request>(); // create a concurrent queue if null
      reqs.put(filename,fileQ); // 1 concurrent queue per file
    }
    fileLock.unlock(); // unlock

    if(fileLock.getLock()) { // check if the file is in write mode
      fileQ.add(req); // add to file queue. Will be processed in background.
    }
    else if(type.equals("write")) { // check if write operation and not locked
      Runnable simple = new Runnable() {
        public void run() {
          serveRequests(req, 1); // start serving the request
        }
      };
      new Thread(simple).start();
      // fileLock.unlock();
    } else { // will be read request here
      Runnable simple = new Runnable() {
        public void run() {
          if(type.equals("read"))
            readQuorum(filename, serverIp, ID); // start with the request
        }
      };
      new Thread(simple).start(); // start thread
    }
    return "";
  }

  public static void serveRequests(Request req, int i) { // runs in backgorund for all the requests in the queue of the file
    ConcurrentLinkedQueue<Request> fileQ = reqs.get(req.getFilename()); // get the queue of the file
    Lock fileLock = fileLocks.get(req.getFilename()); // get the lock of the file
    do {
      final Request req1 = req;
      if(req1.getRequestType().equals("read")) { // if read request, can run in parallel, so start execution in background
        Runnable simple1 = new Runnable() {
          public void run() {
            readQuorum(req1.getFilename(), req1.getServerIp(), req1.getID());
          }
        };
        new Thread(simple1).start(); // start read request
      }
      else { /// else if write, it has to be sequential
          fileLock.lock(); // wait till the the lock is got to start with the process
          writeQuorum(req1.getFilename(), req1.getServerIp(), req1.getID());
          fileLock.unlock();
      }
      req = fileQ.poll(); // start with the next request in the file queue
    } while(req!=null);
  }

  public static ArrayList<Integer> getServers(int quor) { // get a few random machines from the list for the quorum
    Random rand = new Random();
    ArrayList<Integer> contactedMachines = new ArrayList<Integer>(); // to store the machines details needed for quorum
    for(int i=0;i<quor;i++) {
      int index;
      do {
        index = rand.nextInt(machines.size()); // the index of the random machine chosen
      } while(contactedMachines.contains(index)); // check if the machine is not previosuly selected
      contactedMachines.add(index);
    }
    return contactedMachines;
  }

  public static ArrayList<Integer> contactServer(ArrayList<Integer> machineList, String filename, int version) { // function to contact machines for quorum
    ArrayList<Integer> x = new ArrayList<Integer>(); // list of versions from machines
    for(int m:machineList) { // for all the machines needed for the quotum
      try {
        String[] mach = machines.get(m).split(":");
        TTransport transport = new TSocket(mach[0], Integer.parseInt(mach[1]));
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        transport.open();
        TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "VersionService"); // version service is responsible for all version related activities
        VersionService.Client hostNodeClient = new VersionService.Client(mp);
        String nodeVals;
        if(version == -1) // if sync
          nodeVals = hostNodeClient.getVersion(filename); // get the version of all the files in filename string
        else if(version == 0) // if read
          nodeVals = hostNodeClient.getFileVersion(filename); // get the version for the file
        else // if write
          nodeVals = hostNodeClient.updateFileVersion(filename,version+""); // update the version for that file
        transport.close();
        if(nodeVals == null || version > 0 || nodeVals.equals("null")) // made to handle false flags from client
          nodeVals = "0"; // mimimum version for any file
        if(version >= 0) // not needed for sync
          x.add(Integer.parseInt(nodeVals)); // add versions to a list to find max value
        if(version == -1) // if sync
          findMax(convert(nodeVals)); // convert function converts from string to hashmap and hashmap get the max version for the files
      } catch(Exception e) {
        System.out.println(version +" Something went wrong in getting file versions "+e);
      }
    }
    return x;
  }

  public static HashMap<String,Integer> convert(String value) {// converts from string to hashmap
    String[] keyValuePairs = value.substring(1, value.length()-1).split(","); // remove the brackets and split based on ',' for each element
    HashMap<String,Integer> map = new HashMap<String,Integer>();
    for(String pair : keyValuePairs) {
      String[] entry = pair.split("="); // split key = value pairs
      map.put(entry[0].trim(), Integer.valueOf(entry[1].trim())); // insert to the map
    }
    return map;
  }

  public static void findMax(HashMap<String,Integer> response) { // get the max of the versions. Used for sync
    for(String s: response.keySet()) { // for all the versions of files from the server
      newVersions.put(s,(newVersions.get(s)>=response.get(s))?newVersions.get(s):response.get(s)); // check with current temp version map and keep the max value
    }
  }

  public static int findMax(ArrayList<Integer> fileversions) { // find the max of the versions of a file from different servers from the list. Used for read and write
    int max = 0;
    for(int version:fileversions) { // iterate through all values
      if(version>max) // if version is greater than current max
        max = version; // get the max value
    }
    return max;
  }

  public static void returnInfo(String serverIp, String ID, int val) { // send response back to client
    try {// serverIp is the caller's IP and ID is the ID generated by it, val is the final version to be sent back
      String[] mach =serverIp.split(":");
      TTransport transport = new TSocket(mach[0], Integer.parseInt(mach[1]));
      TBinaryProtocol protocol = new TBinaryProtocol(transport);
      transport.open();
      TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "FileService"); // file service is invloved with all the communication between client to server and coordinator
      FileService.Client hostNodeClient = new FileService.Client(mp);
      String nodeVals;
      nodeVals = hostNodeClient.onComplete(ID, val); // send back the values to the client
      transport.close();
    } catch(Exception e) {
      System.out.println("Something went wrong in returning to server "+e);
    }
  }

  public static void readQuorum(String filename, String serverIp, String ID) { // forms the read quorum and send back the result
    ArrayList<Integer> contactedMachines = getServers(readQ); // choose machines based on the read quorum
    ArrayList<Integer> versions = contactServer(contactedMachines, filename, 0); // contact servers to get the versions
    int max = findMax(versions); // get the max version
    returnInfo(serverIp, ID, max); // return the version to the server
  }

  public static void writeQuorum(String filename, String serverIp, String ID) { // forms the write quorum and send back the result
    ArrayList<Integer> contactedMachines = getServers(writeQ); // choose machines based on the write quorum
    ArrayList<Integer> versions = contactServer(contactedMachines, filename, 0); // contact servers to get the versions
    int max = findMax(versions); // get the max version
    max = max+1;
    contactServer(contactedMachines, filename, max); // contact servers to update the versions
    returnInfo(serverIp, ID, max); // return the version to the server
  }

  public static void syncQuorum(String serverIp) { // forms the read quorum for sync
    newVersions = new HashMap<String,Integer>(); //  form a temp version map : filename = version
    ArrayList<Integer> contactedMachines = getServers(readQ); // choose machines based on the read quorum
    ArrayList<String> files = getFileList(); // get all the files that arent currently writing
    for(String f:files)
      newVersions.put(f,0); // initialize map to 0
    if(files.isEmpty())
      return; // return if no files have been added
    ArrayList<Integer> versions = contactServer(contactedMachines, files.toString(), -1);  // contact servers to get the versions
    unlockFiles(files); // now unlock the files that were used during sync
    returnInfo(serverIp, newVersions.toString(), -1); // return the version to the server
  }

  public static ArrayList<String> getFileList() { // get all the files not in writing mode
    ArrayList<String> fileList = new ArrayList<String>();
    for(String k: fileLocks.keySet()) {
      Lock l = fileLocks.get(k); // get the lock to see if there is an operation running
      if(!l.getLock()) { // try to get the lock
        l.lock(); // get the lock
        fileList.add(k); // add to list
      }
    }
    return fileList;
  }

  public static void unlockFiles(ArrayList<String> fileList) { // unlock the lock for the given files
    for(String k: fileList) {
      fileLocks.get(k).unlock(); // unlock for each file
    }
  }

  @Override
  public boolean ping() {
    return true;
  }
}
