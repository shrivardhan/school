import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClientManager;


public class HostClientHandler implements HostClientService.Iface {

  public HostClientHandler() {
  }

  @Override
  public String getTitle(String title) {
    HostNodeHandler h = new HostNodeHandler();
    String res = "title not found";
    String key = getHash(title);
    if(key.equals("")) {
      System.out.println("No hash value for " + title);
      return "NH";
    }
    // System.out.println(key.toString());
    String successor = HostNodeHandler.findPredecessor(new BigInteger(key),2);
    // String successor = h.findSuccessor(key);
    try {
      String[] newVals = successor.split(":");
      System.out.println("Route taken for title "+ title +" : "+ newVals[3]);
      TTransport transport;
      // System.out.println(newVals[1]);
      // System.out.println(newVals[2]);
      transport = new TSocket(newVals[1], Integer.parseInt(newVals[2]));
      transport.open();
      TProtocol protocol = new  TBinaryProtocol(transport);
      TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
      HostService.Client client = new HostService.Client(mp);
      String newID = client.getKeysData(title);
      transport.close();
      System.out.println("Value returned " + newID);
      if(newID!=null)
        return newID + "###"+ newVals[3];
    } catch (TException x) {
      System.out.println("Something went wrong in getting title "+x);
    }
    return res;
  }

  @Override
  public String setTitle(String title, String genre) {
    HostNodeHandler h = new HostNodeHandler();
    String key = getHash(title);
    if(key.equals("")) {
      System.out.println("No hash value for " + title);
      return "NH";
    }
    // System.out.println(key.toString());
    String successor = HostNodeHandler.findPredecessor(new BigInteger(key),2);
    // String successor = h.findSuccessor(key);
    String newID = "not inserted";
    String[] newVals = null;
    try {
      newVals = successor.split(":");
      System.out.println("Route taken for "+title+" : "+ newVals[3]);
      TTransport transport;
      transport = new TSocket(newVals[1], Integer.parseInt(newVals[2]));
      transport.open();
      TProtocol protocol = new  TBinaryProtocol(transport);
      TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "HostService");
      HostService.Client client = new HostService.Client(mp);
      newID = client.setKeysData(title,genre);
      // System.out.println(key+" Success info " + newID);
      transport.close();
    } catch (TException x) {
      System.out.println("Something went wrong in setting title "+x);
    }
    return newID+" with key " + key.toString() + "###" + newVals[3];
  }

  @Override
  public String setFromFile(String file) {
    HashMap<String,String> finalVals = new HashMap<>();
    HostClientHandler h = new HostClientHandler();
		try {
				String[] books = (new String(Files.readAllBytes(Paths.get((new File(file)).getPath())))).split("\n");
        int x = 0;
        String res = "";
        for(String s : books) {
          ++x;
          String[] bookGenre = s.split(":");
          // System.out.println(bookGenre[0] + ":::" + bookGenre[1]);
  				// finalVals.put(bookGenre[0],h.setTitle(bookGenre[0],bookGenre[1]));
          res = res + h.setTitle(bookGenre[0],bookGenre[1]) + "@@@";
        }
        if(x == 0) {
          return "no data present";
        }
        res = res.substring(0,res.length()-3);
        // System.out.println(res);
        return res;
        // HostNodeHandler.printHashMap();
		} catch (Exception e) {
			System.out.println("error while set from file " + e);
		}
    return finalVals.toString();
  }

  public static String getHash(String title) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] messageDigest = md.digest((title).getBytes());
      BigInteger key = new BigInteger(1, messageDigest);
      int m = (new HostNodeHandler()).getm();
      return key.mod(BigDecimal.valueOf(Math.pow(2,m)).toBigInteger()).toString();
    } catch (Exception e) {
      System.out.println("Something went wrong in getting Hash value " +e);
    }
    return "";
  }

  @Override
  public boolean ping() {
    return true;
  }
}
