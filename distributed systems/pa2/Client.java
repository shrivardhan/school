import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import org.apache.thrift.protocol.TMultiplexedProtocol;

public class Client {
  static final String NACK = "NACK";
  public static void main(String [] args) {
    HashMap<String,String> confs = new HashMap();
    try {
      String[] confVals = (new String(Files.readAllBytes(Paths.get((new File("config")).getPath())))).split("\n");
      for(String cv : confVals) {
        String[] cVals = cv.split(":");
        confs.put(cVals[0],cVals[1]);
      }
    } catch(Exception e) {
      System.out.println("Something went wrong in reading config " + e);
    }

    try {
      String superNodeIp  = confs.get("SuperNodeIp");
      String superNodePort = confs.get("SuperNodePort");
      String newID = "NA";
      do {
        TTransport transport;
        transport = new TSocket(superNodeIp, Integer.parseInt(superNodePort));
        transport.open();
        TProtocol protocol = new  TBinaryProtocol(transport);
        TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "SuperNodeClientService");
        SuperNodeClientService.Client client = new SuperNodeClientService.Client(mp);
        newID = client.getNode();
        System.out.println("Node to connect " + newID);
        transport.close();
      } while (newID.equals(NACK));

      String[] vals = newID.split(":");
      int i=5;
      Scanner reader = new Scanner(System.in);
      TTransport transport1;
      transport1 = new TSocket(vals[0], Integer.parseInt(vals[1]));
      transport1.open();
      TProtocol protocol1 = new  TBinaryProtocol(transport1);
      TMultiplexedProtocol mp1 = new TMultiplexedProtocol(protocol1, "HostClientService");
      HostClientService.Client client1 = new HostClientService.Client(mp1);

      // try {
  		// 		String[] books = (new String(Files.readAllBytes(Paths.get((new File("shakespeares.txt")).getPath())))).split("\n");
      //     for(String s : books) {
      //       String[] bookGenre = s.split(":");
      //       // System.out.println(bookGenre[0] + ":::" + bookGenre[1]);
      //       String rm = client1.setTitle(bookGenre[0],bookGenre[1]);
      //       System.out.println(rm);
    	// 			// finalVals.put(bookGenre[0],h.setTitle(bookGenre[0],bookGenre[1]));
      //     }
  		// } catch (Exception e) {
  		// 	System.out.println("error while set from file " + e);
  		// }

      while(i!=4) {
        System.out.println("Menu:\n1: Put Keys from File\n2: Put keys individually\n3: get keys\n4. Quit");
        i = reader.nextInt();
        switch(i) {
          case 1: System.out.println("Enter file (Eg: abc.txt)");
                  String d1 = reader.next();
                  String op = client1.setFromFile(d1);
                  // System.out.println(op);
                  System.out.println("type 1 for printing routing table and 2 for no print");
                  int r1 = reader.nextInt();
                  String[] results0 = op.split("@@@");
                  for(String rr : results0) {
                    // System.out.println(rr);
                    String[] results1 = rr.split("###");
                    if(r1 == 1)
                      System.out.println(results1[0] + " ::: routing info "+ results1[1]);
                    else
                      System.out.println(results1[0]);
                  }
                  break;
          case 2: System.out.println("Enter title");
                  String k = reader.next();
                  System.out.println("Enter value");
                  String v = reader.next();
                  String op1 = client1.setTitle(k,v);
                  System.out.println("type 1 for printing routing table and 2 for no print");
                  int r3 = reader.nextInt();
                  String[] results3 = op1.split("###");
                  if(r3 == 1)
                    System.out.println(results3[0] + " ::: routing info "+ results3[1]);
                  else
                    System.out.println(results3[0]);
                  break;
          case 3: System.out.println("Enter title");
                  reader.nextLine(); // eat the carriages left
                  String k1 = reader.nextLine();
                  String op2 = client1.getTitle(k1);
                  // System.out.println(op2);
                  if(op2.equals("title not found"))
                    System.out.println("Book Title is not present in the network");
                  else {
                    String[] results2 = op2.split("###");
                    System.out.println(results2[0]);
                    System.out.println("type 1 for printing routing table and 2 for no print");
                    int r2 = reader.nextInt();
                    if(r2 == 1)
                      System.out.println( " ::: routing info "+ results2[1]);

                  }
                  break;
          default: i=5;
        }
      }
      transport1.close();

    } catch (TException x) {
      x.printStackTrace();
      System.out.println("Something went wrong " + x);
    }
  }


}
