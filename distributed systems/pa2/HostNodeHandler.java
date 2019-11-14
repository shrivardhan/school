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
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClientManager;

public class HostNodeHandler implements HostService.Iface {
  static String superNodeIp;
  static String superNodePort;
  static int m;
  static BigInteger maxVal = (BigDecimal.valueOf(Math.pow(2,m))).toBigInteger();
  static HashMap<String,String> keysData = new HashMap<String,String>();
  static final String NACK = "NACK";

  public static class Finger {
  	BigInteger start;
  	BigInteger[] interval;
  	BigInteger successorID;
  	String successorIp;
  	String successorPortNumber;

    public Finger() {
    }

  	public Finger(BigInteger id, int i){
      start = BigDecimal.valueOf(Math.pow(2,i-1)).toBigInteger().add(id).mod(maxVal);
  	}

  	public BigInteger getStart() {
  		return start;
  	}

  	public BigInteger[] getInterval() {
  		return interval;
  	}

  	public BigInteger getSuccessor() {
  		return successorID;
  	}

  	public String getIP() {
  		return successorIp;
  	}

  	public String getportNumber() {
  		return successorPortNumber;
  	}

    public void setStart(BigInteger id, int i) {
      start = BigDecimal.valueOf(Math.pow(2,i-1)).toBigInteger().add(id);
    }

    public void setStart(BigInteger id) {
      start = id;
    }

  	public void setInterval(BigInteger[] nodeInterval) {
  		interval = nodeInterval;
  	}

  	public void setSuccessor(BigInteger nodeSuccessor) {
  		successorID = nodeSuccessor;
  	}

  	public void setIP(String nodeIP) {
  		successorIp = nodeIP;
  	}

  	public void setportNumber(String nodePortNumber) {
  		successorPortNumber = nodePortNumber;
  	}
  }

  public static class NodeDetails {
    public BigInteger id;
    public String ip;
    public String hostname;
    public String port;
    public NodeDetails predNode;
    public NodeDetails sucNode;
    public ArrayList<Finger> fingerTable = null;
    public void printFingerTable() {
      int tmp =0;
      for(Finger t: fingerTable) {
        tmp++;
        System.out.println(t.start + " : " + t.successorID + " : " + t.successorIp + " : " + t.successorPortNumber);
      }
      System.out.println("current Node ::: "+curNode.id.toString() + " : " + curNode.ip + " : " + curNode.port);
      System.out.println("Succesor Node ::: "+curNode.sucNode.id.toString() + " : " + curNode.sucNode.ip + " : " + curNode.sucNode.port);
      System.out.println("Predecessor Node ::: "+curNode.predNode.id.toString() + " : " + curNode.predNode.ip + " : " + curNode.predNode.port);
      // System.out.println(tmp);
    }
  }

  static NodeDetails curNode;

  public HostNodeHandler() {}

  public HostNodeHandler(String sIp, String sPort , String port) {
    try {
        curNode = new NodeDetails();
        curNode.predNode = new NodeDetails();
        curNode.sucNode = new NodeDetails();
        superNodeIp = sIp;
        superNodePort = sPort;
        String ipHost = InetAddress.getLocalHost().toString();
        String nodeVals = "";
        try {
          do {
          TTransport transport = new TSocket(superNodeIp, Integer.parseInt(sPort));
          TBinaryProtocol protocol = new TBinaryProtocol(transport);
          transport.open();
          TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "SuperNodeHostService");
          SuperNodeHostService.Client hostNodeClient = new SuperNodeHostService.Client(mp);
          nodeVals = hostNodeClient.Join(ipHost.split("/")[1],port);
          transport.close();
        } while(nodeVals.equals(NACK));
        } catch(TException e) {
          System.out.println("Something went wrong in joining: "+e);
        }
        curNode.id = new BigInteger(nodeVals.split(",")[0]);
        curNode.ip = ipHost.split("/")[1];
        curNode.hostname = ipHost.split("/")[0];
        curNode.port = port;
        m = Integer.parseInt(nodeVals.split(",")[2]);
        maxVal = (BigDecimal.valueOf(Math.pow(2,m))).toBigInteger();
        curNode.fingerTable = new ArrayList<>(m);
        startDHT(nodeVals.split(",")[1]);
        } catch (UnknownHostException e) {
          System.out.println("Something went wrong in getting local ip: "+e);
        }
  }

  public static void startDHT(String randNodeVal) {
    if(randNodeVal.split(":")[0].equals("null")) {
      for(int i = 1; i <= m; i++) {
        Finger temp = new Finger(curNode.id,i);
        temp.setSuccessor(curNode.id);
        temp.setIP(curNode.ip);
        temp.setportNumber(curNode.port);
        curNode.fingerTable.add(i-1,temp);
      }
      curNode.sucNode.id = curNode.id;
      curNode.sucNode.ip = curNode.ip;
      curNode.sucNode.port = curNode.port;
      curNode.predNode.id = curNode.id;
      curNode.predNode.ip = curNode.ip;
      curNode.predNode.port = curNode.port;
    } else {
      // System.out.println("ID IS "+ curNode.id.toString());
      init_finger_table(randNodeVal);
      // curNode.printFingerTable();
      updateOthers();
    }
    // curNode.printFingerTable();
    try {
      TTransport transport = new TSocket(superNodeIp, Integer.parseInt(superNodePort));
      TBinaryProtocol protocol = new TBinaryProtocol(transport);
      transport.open();
      TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "SuperNodeHostService");
      SuperNodeHostService.Client hostNodeClient = new SuperNodeHostService.Client(mp);
      String nodeVals = hostNodeClient.PostJoin(curNode.ip,curNode.port);
      transport.close();
      (new HostNodeHandler()).printDHT();
    } catch(TException e) {
      System.out.println("Something went wrong in post join: "+e);
    }
  }

  @Override
  public String findSuccessor(String id) {
    // System.out.println("inside find successor "+id);
    String newID = findPredecessor(new BigInteger(id),0);
    // System.out.println("got from findPredecessor "+newID);
    if(newID.equals(curNode.id.toString()))
      newID = newID+":"+curNode.ip+":"+curNode.port;
    if(newID.split(":")[0].equals(curNode.id.toString())) {
      return curNode.sucNode.id.toString()+":"+curNode.sucNode.ip+":"+curNode.sucNode.port;
    }
    else {
      try {
        String[] newVals = newID.split(":");
        TTransport transport = new TSocket(newVals[1], Integer.parseInt(newVals[2]));
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        transport.open();
        TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
        HostService.Client hostNodeClient = new HostService.Client(mp);
        newID = hostNodeClient.getSuccessor(null);
        // System.out.println("after getting successor "+ newID);
        newVals = newID.split(":");
        newID = newVals[2]+":"+newVals[0]+":"+newVals[1];
        transport.close();
      } catch(Exception e) {
        System.out.println("Something went wrong in getting successor " +e);
      }
    }
    return newID;
  }

  public static String findPredecessor(BigInteger id, int i) {
    String newID = curNode.id.toString()+":"+curNode.ip+":"+curNode.port;
    String newSuccessorID = curNode.sucNode.id.toString()+":"+curNode.sucNode.ip+":"+curNode.sucNode.port;
    int x = m;
    String route = curNode.id.toString();
    int j = (i==2)?2:0;
    while(!bigIntCompare2(new BigInteger(newID.split(":")[0]), new BigInteger(newSuccessorID.split(":")[0]), id)) {
      // System.out.println("out of range ::: " + newID+"###"+newSuccessorID + "###" + id.toString());
      try {
        String[] newVals = newID.split(":");
        // System.out.println(newVals[0]);
        if(newVals[0].equals(curNode.id.toString())) {
          newID = new HostNodeHandler().closestPrecedingFinger(id.toString(),j);
        } else {
          TTransport transport = new TSocket(newVals[1], Integer.parseInt(newVals[2]));
          TBinaryProtocol protocol = new TBinaryProtocol(transport);
          transport.open();
          // System.out.println(newVals[1]);
          TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
          HostService.Client hostNodeClient = new HostService.Client(mp);
          newID = hostNodeClient.closestPrecedingFinger(id.toString(),j);
          // System.out.println(newVals[2]);
          transport.close();
        }
      } catch(TException e) {
        System.out.println("Something went wrong in while getting closest predecessor: "+e);
      }
    // System.out.println("After getting newID from closest preceding finger "+newID.toString());
    try {
      String[] newVals = newID.split(":");
      route = route+";"+newVals[0];
      // System.out.println(newID);
      if(newVals[0].equals(curNode.id.toString())) {
        newSuccessorID = curNode.sucNode.id.toString()+":"+curNode.sucNode.ip+":"+curNode.sucNode.port;
      } else {
        TTransport transport = new TSocket(newVals[1], Integer.parseInt(newVals[2]));
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        transport.open();
        TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
        HostService.Client hostNodeClient = new HostService.Client(mp);
        newSuccessorID = hostNodeClient.getSuccessor(null);
        String[] vals = newSuccessorID.split(":");
        newSuccessorID = vals[2]+":"+vals[0]+":"+vals[1];
        transport.close();
      }
    } catch(TException e) {
      System.out.println("Something went wrong in while get sucessor: "+e);
    }
    // System.out.println("After getting newSuccessorID after closest preceding finger "+newSuccessorID.toString());
      // if(--x == 0)
      //   break;
    }
    if(i==1 && newSuccessorID.split(":")[0].equals(id.toString()))
      return newSuccessorID;
    if(i==2)
      return newSuccessorID+":"+route+";"+newSuccessorID.split(":")[0];
    return newID;
  }

  @Override
  public String closestPrecedingFinger(String id, int j) {
    // System.out.println("inside closest preceding "+id);
    for(int i=m-1;i>=0;i--){
      // System.out.println(i+" :::: "+curNode.id.toString()+" : "+ id.toString()+" : " + curNode.fingerTable.get(i).successorID.toString());
      if ( (id.equals(curNode.fingerTable.get(i).successorID.toString()) || (curNode.id.toString().equals(curNode.fingerTable.get(i).successorID.toString())))) {
        // System.out.println("returning from closestPrecedingFinger"+curNode.sucNode.id.toString()+":"+curNode.sucNode.ip+":"+curNode.sucNode.port);
        return curNode.sucNode.id.toString()+":"+curNode.sucNode.ip+":"+curNode.sucNode.port;
      } else if (bigIntCompare1(curNode.id, new BigInteger(id), curNode.fingerTable.get(i).successorID)) {
        Finger curIndex = curNode.fingerTable.get(i);
        // System.out.println("returning from closestPrecedingFinger inside loop "+ curIndex.getSuccessor()+":"+curIndex.getIP()+":"+curIndex.getportNumber());
        return curIndex.getSuccessor()+":"+curIndex.getIP()+":"+curIndex.getportNumber();
      }
    }
    System.out.println("returning from closestPrecedingFinger "+curNode.id.toString()+":"+curNode.ip+":"+curNode.port);
    return curNode.id.toString()+":"+curNode.ip+":"+curNode.port;
  }

  public static void init_finger_table(String randNodeVal) {
    // System.out.println("Inside init_finger_table");
    BigInteger key0 = BigDecimal.valueOf(Math.pow(2,0)).toBigInteger().add(curNode.id);
    String nodeVals = contactSuccessor(randNodeVal,key0.toString());
    if(nodeVals == null) {
      System.out.println("Could not get successor value");
      return;
    }
    // System.out.println("Succesor will be " + nodeVals);
    Finger temp = new Finger();
    temp.setStart(key0);
    String[] sucNodeVals = nodeVals.split(":");
    temp.setSuccessor(new BigInteger(sucNodeVals[0]));
    temp.setIP(sucNodeVals[1]);
    temp.setportNumber(sucNodeVals[2]);
    curNode.fingerTable.add(0,temp);
    curNode.sucNode = new NodeDetails();
    curNode.sucNode.id = new BigInteger(sucNodeVals[0]);
    curNode.sucNode.ip = sucNodeVals[1];
    curNode.sucNode.port = sucNodeVals[2];
    // transfer();
    try {
      String[] randomNode = nodeVals.split(":");
      TTransport transport = new TSocket(randomNode[1], Integer.parseInt(randomNode[2]));
      TBinaryProtocol protocol = new TBinaryProtocol(transport);
      transport.open();
      TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
      HostService.Client hostNodeClient = new HostService.Client(mp);
      nodeVals = null;
      nodeVals = hostNodeClient.getPredecessor(curNode.ip+":"+curNode.port+":"+curNode.id.toString());
      transport.close();
    } catch(TException e) {
      System.out.println("Something went wrong in get predecessor: "+e);
    }
    if(nodeVals == null) {
      System.out.println("Could not get predecessor value");
      return;
    }
    // System.out.println("Predecessor will be " + nodeVals);
    String[] predNodeVals = nodeVals.split(":");
    curNode.predNode = new NodeDetails();
    curNode.predNode.id = new BigInteger(predNodeVals[2]);
    curNode.predNode.ip = predNodeVals[0];
    curNode.predNode.port = predNodeVals[1];
    for(int i=0;i<m-1;i++) {
      BigInteger curStart = BigDecimal.valueOf(Math.pow(2,i+1)).toBigInteger().add(curNode.id).mod(maxVal);
      BigInteger predStart = curNode.fingerTable.get(i).getStart();
      Finger predTemp = new Finger();
      temp = new Finger();
      if(bigIntCompare3(curNode.id,predStart,curStart)) {
        predTemp = curNode.fingerTable.get(i);
      } else {
        nodeVals = null;
        nodeVals = contactSuccessor(randNodeVal,curStart.toString());
        if(nodeVals == null) {
          System.out.println("Could not get successor value");
          return;
        }
        // System.out.println(i+"th finger successor will be - "+nodeVals);
        sucNodeVals = nodeVals.split(":");
        predTemp.setSuccessor(new BigInteger(sucNodeVals[0]));
        predTemp.setIP(sucNodeVals[1]);
        predTemp.setportNumber(sucNodeVals[2]);
      }
      temp.setStart(curStart);
      temp.setSuccessor(predTemp.getSuccessor());
      temp.setIP(predTemp.getIP());
      temp.setportNumber(predTemp.getportNumber());
      curNode.fingerTable.add(i+1,temp);
    }
    // curNode.printFingerTable();
  }

  public static void transfer() {
    try {
      TTransport transport = new TSocket(curNode.sucNode.ip, Integer.parseInt(curNode.sucNode.port));
      TBinaryProtocol protocol = new TBinaryProtocol(transport);
      transport.open();
      TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
      HostService.Client hostNodeClient = new HostService.Client(mp);
      String value = hostNodeClient.transferKeysData();
      transport.close();
      // System.out.println(value);
      value = value.substring(1, value.length()-1);
      if(!value.trim().equals("")) {
        String[] keyValuePairs = value.split(",");
        for(String pair : keyValuePairs) {
          String[] entry = pair.split("=");
          keysData.put(entry[0].trim(), entry[1].trim());
        }
      }
      printHashMap();
    } catch(TException e) {
      System.out.println("Something went wrong in transfer: "+e);
    }
  }

  @Override
  public String getPredecessor(String val) {
    String res = curNode.predNode.ip+":"+curNode.predNode.port+":"+curNode.predNode.id.toString();
    // System.out.println("Inside getPredecessor "+res); // remove
    if(val == null) {
      return res;
    }
    String[] predVals = val.split(":");
    curNode.predNode = new NodeDetails();
    curNode.predNode.ip = predVals[0];
    curNode.predNode.port = predVals[1];
    curNode.predNode.id = new BigInteger(predVals[2]);
    // curNode.printFingerTable(); // remove
    return res;
  }

  @Override
  public String getSuccessor(String val) {
    String res = curNode.sucNode.ip+":"+curNode.sucNode.port+":"+curNode.sucNode.id.toString();
    // System.out.println("Inside getSuccessor "+res); // remove
    if(val == null) {
      return res;
    }
    String[] sucVals = val.split(":");
    curNode.sucNode = new NodeDetails();
    curNode.sucNode.ip = sucVals[0];
    curNode.sucNode.port = sucVals[1];
    curNode.sucNode.id = new BigInteger(sucVals[2]);
    // curNode.printFingerTable(); // remove
    return res;
  }

  public static void updateOthers(){
    // System.out.println("Inside update others");
    for(int i=0;i<m;i++) {
      // System.out.println(i+" ::: "+curNode.id.subtract( BigDecimal.valueOf(Math.pow(2,i)).toBigInteger()).mod(maxVal).toString());
      String predNodeVals = findPredecessor(curNode.id.subtract( BigDecimal.valueOf(Math.pow(2,i)).toBigInteger()).mod(maxVal),1);
      // System.out.println(i+" :::: After find predecessor "+predNodeVals);
      if(predNodeVals == null || predNodeVals.trim().isEmpty()) {
        System.out.println("Could not get pred vals while updating");
        return;
      }
      do {
      String nextNode = contactPredecessor(predNodeVals,i);
      if(nextNode == null) {
        System.out.println("Something went wrong in contacting the predecessor");
        break;
      } else if(nextNode.trim().equals("")) {
        // System.out.println("No predecessor while updating");
        break;
      } else {
        predNodeVals = nextNode;
      }
    } while(true);
  }
}

  public static String contactSuccessor(String randNodeVal,String key) {
    try {
      // System.out.println("Inside contact successor "+ randNodeVal);
      String[] randomNode = randNodeVal.split(":");
      TTransport transport = new TSocket(randomNode[0], Integer.parseInt(randomNode[1]));
      TBinaryProtocol protocol = new TBinaryProtocol(transport);
      transport.open();
      TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
      HostService.Client hostNodeClient = new HostService.Client(mp);
      String nodeVals = hostNodeClient.findSuccessor(key);
      transport.close();
      return nodeVals;
    } catch(TException e) {
      System.out.println("Something went wrong in contact successor: "+e);
    }
    return null;
  }

  public static String contactPredecessor(String predNodeVals, int i) {
    try {
      // System.out.println("Inside contact predecessor "+ predNodeVals);
      String[] predNode = predNodeVals.split(":");
      if(predNode[0].equals(curNode.id.toString())) {
        return new HostNodeHandler().updateDHT(curNode.id.toString()+":"+curNode.ip+":"+curNode.port,i);
      } else {
        TTransport transport = new TSocket(predNode[1], Integer.parseInt(predNode[2]));
        TBinaryProtocol protocol = new TBinaryProtocol(transport);
        transport.open();
        TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
        HostService.Client hostNodeClient = new HostService.Client(mp);
        String nodeVals = hostNodeClient.updateDHT(curNode.id.toString()+":"+curNode.ip+":"+curNode.port,i);
        transport.close();
        return nodeVals;
      }
    } catch(TException e) {
      System.out.println("Something went wrong in contact predecessor: "+e);
    }
    return null;
  }

  @Override
  public String updateDHT(String n, int i) {
    // System.out.println(i+" ::: Inside Update DHT ::: "+ n);
    String[] nodeVals = n.split(":");
    Finger predRow = curNode.fingerTable.get(i);

    String[] newSuccessor = (curNode.sucNode.id.toString()+":"+curNode.sucNode.ip+":"+curNode.sucNode.port).split(":");
    if(curNode.id.toString().equals(nodeVals[0])) {
      // System.out.println(nodeVals[0]);
      // System.out.println(newSuccessor[0]);
      // System.out.println("new logic starts");
      if(bigIntCompare2(new BigInteger(nodeVals[0]),new BigInteger(newSuccessor[0]),predRow.getStart())) {
         // System.out.println("new logic if");
         predRow.setSuccessor(new BigInteger(newSuccessor[0]));
         predRow.setIP(newSuccessor[1]);
         predRow.setportNumber(newSuccessor[2]);
         if(i == 0) {
           curNode.sucNode = new NodeDetails();
           curNode.sucNode.id = new BigInteger(newSuccessor[0]);
           curNode.sucNode.ip = newSuccessor[1];
           curNode.sucNode.port = newSuccessor[2];
         }
         // return curNode.predNode.id.toString()+":"+curNode.predNode.ip+":"+curNode.predNode.port;
      } else {
        // System.out.println("new logic else");
        String newSuccessorFromNewSuccessor = null;
         try {
           TTransport transport = new TSocket(newSuccessor[1], Integer.parseInt(newSuccessor[2]));
           TBinaryProtocol protocol = new TBinaryProtocol(transport);
           transport.open();
           TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
           HostService.Client hostNodeClient = new HostService.Client(mp);
           // (new BigInteger(nodeVals[0])).add( BigDecimal.valueOf(Math.pow(2,i)).toBigInteger()).mod(maxVal)
           newSuccessorFromNewSuccessor = hostNodeClient.findSuccessor(predRow.getStart().toString());
           transport.close();
         } catch (Exception e) {
           System.out.println("Something went wrong in finding sucessor from nodes successor inside updateDHT");
         }
         if(newSuccessorFromNewSuccessor == null) {
           System.out.println("Returned null from get successor from sucessor inside updateDHT for 2^1 mod");
           return "";
         }
         // System.out.println("returned from getsuccessor from successor inside updateDHT "+ newSuccessorFromNewSuccessor);
         String[] newSuccessorFromNewSuccessorVals = newSuccessorFromNewSuccessor.split(":");
         predRow.setSuccessor(new BigInteger(newSuccessorFromNewSuccessorVals[0]));
         predRow.setIP(newSuccessorFromNewSuccessorVals[1]);
         predRow.setportNumber(newSuccessorFromNewSuccessorVals[2]);
        if(i == 0) {
          curNode.sucNode = new NodeDetails();
          curNode.sucNode.id = new BigInteger(newSuccessorFromNewSuccessorVals[0]);
          curNode.sucNode.ip = newSuccessorFromNewSuccessorVals[1];
          curNode.sucNode.port = newSuccessorFromNewSuccessorVals[2];
        }
        // return curNode.predNode.id.toString()+":"+curNode.predNode.ip+":"+curNode.predNode.port;
      }
    // if(curNode.sucNode.id.compareTo(curNode.fingerTable.get(i).getStart()) == 1 && bigIntCompare3(curNode.id,predRow.getSuccessor(),new BigInteger(nodeVals[0]))) {
    //   // curNode.id.compareTo(new BigInteger(nodeVals[0])) !=0 &&
    //   predRow.setSuccessor(new BigInteger(nodeVals[0]));
    //   predRow.setIP(nodeVals[1]);
    //   predRow.setportNumber(nodeVals[2]);
    //   if(i == 0) {
    //     curNode.sucNode = new NodeDetails();
    //     curNode.sucNode.id = new BigInteger(nodeVals[0]);
    //     curNode.sucNode.ip = nodeVals[1];
    //     curNode.sucNode.port = nodeVals[2];
    //   }
    //   curNode.printFingerTable(); // remove
    //   return curNode.predNode.id.toString()+":"+curNode.predNode.ip+":"+curNode.predNode.port;
    } else if (bigIntCompare2(curNode.id,predRow.getSuccessor(),new BigInteger(nodeVals[0]))) {
      // curNode.fingerTable.get(i).getSuccessor().compareTo(new BigInteger(nodeVals[0])) !=0 && curNode.sucNode.id.compareTo(curNode.fingerTable.get(i).getStart()) != -1 &&
      predRow.setSuccessor(new BigInteger(nodeVals[0]));
      predRow.setIP(nodeVals[1]);
      predRow.setportNumber(nodeVals[2]);
      if(i == 0) {
        curNode.sucNode = new NodeDetails();
        curNode.sucNode.id = new BigInteger(nodeVals[0]);
        curNode.sucNode.ip = nodeVals[1];
        curNode.sucNode.port = nodeVals[2];
      }
      // curNode.printFingerTable(); // remove
      return curNode.predNode.id.toString()+":"+curNode.predNode.ip+":"+curNode.predNode.port;
    }
    // System.out.println("No change in DHT");
    // curNode.printFingerTable(); // remove
    return "";
  }

  public static boolean bigIntCompare1(BigInteger a, BigInteger b, BigInteger c) {
    // System.out.println("Inside bigcompare1");
    if (b.compareTo(a) == 1) {
      // System.out.println("Inside bigcompare1 1");
      if (c.compareTo(a) == 1 && b.compareTo(c) == 1) return true;
      else return false;
    }
    else if(b.compareTo(a) == -1) {
      // System.out.println("Inside bigcompare1 2");
      if (c.compareTo(b) == 1 && a.compareTo(c) == 1) return false;
      else return true;
    }
    return true;
  }

  public static boolean bigIntCompare2(BigInteger a, BigInteger b, BigInteger c) {
    // System.out.println("Inside bigcompare2");
    if (b.compareTo(a) == 1) {
      // System.out.println("Inside bigcompare2 1");
      if (c.compareTo(a) == 1 && b.compareTo(c) != -1) return true;
      else return false;
    }
    else if(b.compareTo(a) == -1) {
      // System.out.println("Inside bigcompare2 2");
      if (c.compareTo(b) == 1 && a.compareTo(c) != -1) return false;
      else return true;
    }
    return true;
  }

  public static boolean bigIntCompare3(BigInteger a, BigInteger b, BigInteger c) {
    // System.out.println("Inside bigcompare3");
    if (b.compareTo(a) == 1) {
      // System.out.println("Inside bigcompare3 1");
        if (c.compareTo(a) != -1 && b.compareTo(c) == 1) return true;
        else return false;
      }
      else if(b.compareTo(a) == -1) {
        // System.out.println("Inside bigcompare3 2");
        if (c.compareTo(b) != -1 && a.compareTo(c) == 1) return false;
        else return true;
      }
      return true;
  }

  @Override
  public boolean ping() {
    return true;
  }

  @Override
  public String getKeysData(String key) {
    try {
    return keysData.get(key);
  } catch (Exception e) {
    System.out.println("Something went wrong in get keys "+e);
  }
  return null;
  }

  @Override
  public String setKeysData(String key, String value) {
    keysData.put(key,value);
    return key +" inserted at "+ curNode.id.toString();
  }

  @Override
  public String printDHT() {
    curNode.printFingerTable();
    printHashMap();
    return "";
  }

  @Override
  public String transferKeysData() {
    String transferKeys="{";
    try{
    List<String> removeAll = new ArrayList<String>();
    for(String s : keysData.keySet()) {
      BigInteger k = new BigInteger(HostClientHandler.getHash(s));
      if(k.compareTo(curNode.predNode.id) != 1) {
        transferKeys = transferKeys +s+"="+keysData.get(s)+",";
        System.out.println(transferKeys);
        removeAll.add(s);
      }
    }
    for(String s : removeAll) {
      keysData.remove(s);
    }
    System.out.println(transferKeys);
    transferKeys = transferKeys.substring(0,transferKeys.length()-1) + "}";
    printHashMap();
    }catch(Exception e) {
    System.out.println("Something went wrong in transfer keys data" + e);
    }
    return transferKeys;
  }

  public static int getm() {
    return m;
  }

  public static void printHashMap() {
    for(String k: keysData.keySet()) {
      System.out.println(k+" = "+keysData.get(k));
    }
  }
}
