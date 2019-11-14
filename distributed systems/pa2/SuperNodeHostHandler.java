import org.apache.thrift.TException;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TTransport;

public class SuperNodeHostHandler implements SuperNodeHostService.Iface {
  static HashMap<BigInteger,String> idHostMap = new HashMap<>();
  static final String NACK = "NACK";
  public static boolean isRunning = false;
  static int totalNodes = 0;
  static int numNodes;
  static int hostPort;
  int m;
  // static int randSequenceIndex = 0;
  // static int[] randSequence = {8,2,7,5,9,20,6};
  // static int[] randSequence = {8,2,9};

  public SuperNodeHostHandler() {
  }

  public SuperNodeHostHandler(int numBits,int port) {
    m = numBits;
    hostPort = port;
  }

  public static boolean isOperational() {
    return !isRunning;
  }

  public static HashMap<BigInteger,String> getMap() {
    return idHostMap;
  }

  @Override
  public String Join(String ip, String portnum) {
    System.out.println("join called");
    if(isRunning)
      return NACK;
    isRunning = true;
    BigInteger randomNodeKey = BigInteger.ZERO;
    try {
      // System.out.println(idHostMap.size());
      int rand = (int)(Math.random()*idHostMap.size());
      // System.out.println(rand);
      int x = 0;
      String randomNodeInfo;
      for(BigInteger i : idHostMap.keySet()) {
        if(x++ == rand) {
          randomNodeKey = i;
          // System.out.println(randomNodeKey);
          break;
        }
      }
      randomNodeInfo = idHostMap.get(randomNodeKey);
      String info = ip+":"+portnum;
      // System.out.println(info); // remove
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] messageDigest = md.digest((info).getBytes());
      BigInteger key = new BigInteger(1, messageDigest);
      key = key.mod(BigDecimal.valueOf(Math.pow(2,m)).toBigInteger());
      // System.out.println(key); // remove
      if(idHostMap.get(key)!=null) {
        // System.out.println("here?");
        key = BigInteger.ONE;
        do {
          key = key.add(BigInteger.ONE).mod(BigDecimal.valueOf(Math.pow(2,m)).toBigInteger());
          // System.out.println(key);
        } while(idHostMap.get(key)!=null);
      }
      // System.out.println(key); // remove
      // System.out.println(randSequenceIndex+"index");
      // idHostMap.put(new BigInteger(randSequence[randSequenceIndex]+""),info); // remove
      idHostMap.put(key,info);
      System.out.println(key+","+randomNodeInfo+":"+randomNodeKey.toString()+","+m); // remove
      return key+","+randomNodeInfo+":"+randomNodeKey.toString()+","+m;
      // return randSequence[randSequenceIndex++]+","+randomNodeInfo+":"+randomNodeKey.toString()+","+m;
    } catch (Exception e) {
      System.out.println("Something went wrong while node joining: "+ e);
    }
    return randomNodeKey.toString();
  }

  @Override
  public String PostJoin(String ip, String portnum) {
    isRunning = false;
    System.out.println("post join called");
    for(BigInteger i: idHostMap.keySet()) {
      try {
        String val = idHostMap.get(i);
        String[] values = val.split(":");
        TTransport transport;
        if(ip.equals(values[0]))
          continue;
        transport = new TSocket(values[0], Integer.parseInt(values[1]));
        transport.open();
        TProtocol protocol = new  TBinaryProtocol(transport);
        TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
        HostService.Client client = new HostService.Client(mp);
        client.printDHT();
      } catch (Exception e) {
        System.out.println("Something went wrong in calling print DHT" + e);
      }
    }
    return isRunning+"";
  }

  @Override
  public String getHostInfo(String id) {
    return "";
  }

  @Override
  public boolean ping() {
    return true;
  }


}
