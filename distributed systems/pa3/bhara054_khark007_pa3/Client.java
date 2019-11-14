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
  static ArrayList<String> machines = new ArrayList<String>(); // list of all the servers that can be connected to

  public static int getServers() { // get a random server to connect
    Random rand = new Random();
    return rand.nextInt(machines.size());
  }

  private static HashMap<String,Integer> readCaseFiles(String filename) { // read from the test case files nad put the data into a map
    HashMap<String,Integer> testCases = new HashMap<>(); // map to hold the test case data
    try{
      String[] caseVals = (new String(Files.readAllBytes(Paths.get((new File(filename)).getPath())))).split("\n"); // read from test case file
      for(int i=0;i<caseVals.length;i++){
        String[] tc = caseVals[i].split(","); // split to get key value pairs
        testCases.put(tc[0], Integer.valueOf(tc[1])); // insert to map
      }
    } catch (Exception e) {
      System.out.println("Could not read case file "+e);
    }
    return testCases;
  }

  public static void testcases() {
    do {
      System.out.println("Menu:\n1:Write one file\n2:Read one file\n3:Write multiple files\n4:Read multiple files\n"
    + "5:Read/Write multiple files\n6:Write heavy one file\n7:Read heavy one file\n8:Write heavy multiple files\n"
    + "9:Read heavy multiple files\n10:Multiple Read and Write one file\n11:Multiple Read and Write multiple files");
      Scanner reader = new Scanner(System.in);
      int i = reader.nextInt();
      if(i>0 && i<12){
        String d1 = "TestCases/case"+i+""; // file case name
        HashMap<String,Integer> testCases = readCaseFiles(d1); // read from test case file
        // long overalltime = 0l;
        // long readtime = 0l;
        // long writetime = 0l;
        for (String k : testCases.keySet()) {
          String[] key = k.split(":");
          String filename = key[0]; // file name
          String operation = key[1]; // kind of opeation -  w,r
          Integer times = testCases.get(k); //  number of times to run
          int h = 0;
          ArrayList<Thread> allThreads = new ArrayList<Thread>(); // list to hold all the threads
          long startTimems = System.currentTimeMillis(); // start time
          for(int j=0;j<times;j++) {
              final int h1 = ++h;
              Runnable simple = new Runnable() {
                public void run() {
                  try {
                    String[] connectMachine = machines.get(getServers()).split(":");
                    TTransport transport;
                    transport = new TSocket(connectMachine[0], Integer.parseInt(connectMachine[1]));
                    transport.open();
                    TProtocol protocol = new  TBinaryProtocol(transport);
                    TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "FileService");
                    FileService.Client client = new FileService.Client(mp);
                    String op1 = "";
                    // System.out.println(operation);
                    try {
                      if(operation.equals("r"))
                        op1 = client.read(filename);
                      else
                        op1 = client.write(filename);
                    } catch (TException e) {
                      System.out.println("Something went wrong in client requests "+ e);
                    }
                    System.out.println(h1+" Output for "+filename+" - "+ op1);
                    transport.close();
                  } catch (Exception e) {
                    System.out.println("Something went wrong in test cases "+e);
                  }
                }
              };
              Thread t = new Thread(simple);
              allThreads.add(h1-1,t); // add all threads to a list
              t.start();

          }
          for(Thread t:allThreads) // wait for the threads to finish
            try {
              t.join();
            }
            catch(Exception e) {
              System.out.println("join failed "+e);
            }
          long endTimems = System.currentTimeMillis(); // end time
          long totalTimems = endTimems - startTimems; // total time
          System.out.println("Time taken = " +totalTimems+"ms");
          // overalltime += totalTimems;
          // if(operation.equals("r"))
          //   readtime += totalTimems;
          // else
          //   writetime += totalTimems;
        }
        // System.out.println("Cumulative execution time");
        // System.out.println("total read time = " +readtime+"ms");
        // System.out.println("total write time = " +writetime+"ms");
        // System.out.println("total time = " +overalltime+"ms");
      }
      System.out.println("Do you want to execute 1 more test case? (y/n)");
      if(!reader.next().equals("y"))
        break;
    } while(true);
  }

  public static void main(String [] args) {
    HashMap<String,String> confs = new HashMap();
    try {
      String[] confVals = (new String(Files.readAllBytes(Paths.get((new File("config")).getPath())))).split("\n"); // read from config
      for(String cv : confVals) {
        String[] cVals = cv.split("=");
        if(cVals[0].equals("servers")) { // get all the servers
          String[] machineVals = cVals[1].split(","); // split the servers
          for(String m:machineVals) // add them to a list
            machines.add(m);
        }
      }
      String[] connectMachine = machines.get(getServers()).split(":"); // get a random machine
      TTransport transport;
      transport = new TSocket(connectMachine[0], Integer.parseInt(connectMachine[1]));
      transport.open();
      TProtocol protocol = new  TBinaryProtocol(transport);
      TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "FileService");
      FileService.Client client = new FileService.Client(mp);
      Scanner reader = new Scanner(System.in);
      // String newID = client.getNode();
      // System.out.println("Node to connect " + newID);
      // transport.close();
      int i = 15;
      while(i!=5) {
        System.out.println("Menu:\n1: Read a file \n2: Write a file \n3: Execute Test Cases \n4: Quit");
        i = reader.nextInt();
        switch(i) {
          case 1: System.out.println("Enter file (Eg: abc.txt)");
                  String d1 = reader.next();
                  long startTimems1 = System.currentTimeMillis();
                  String op1 = client.read(d1);
                  long endTimems1 = System.currentTimeMillis();
                  long totalTimems1 = endTimems1 - startTimems1;
                  String[] op = op1.split(":::");
                  System.out.println("File contents = \n" + op[0]);
                  System.out.println("Version = "+ op[1]);
                  System.out.println("Time taken: "+totalTimems1+"ms");
                  break;
          case 2: System.out.println("Enter file (Eg: abc.txt)");
                  String d2 = reader.next();
                  long startTimems2 = System.currentTimeMillis();
                  String op2 = client.write(d2);
                  long endTimems2 = System.currentTimeMillis();
                  long totalTimems2 = endTimems2 - startTimems2;
                  System.out.println("Version = "+op2);
                  System.out.println("Time taken: "+totalTimems2+"ms");
                  break;
          case 3:
                  testcases();
                  break;
          default: i=5;
        }
      }
      transport.close();

    } catch (TException x) {
      x.printStackTrace();
      System.out.println("Something went wrong " + x);
    } catch (Exception x) {
      x.printStackTrace();
      System.out.println("Something went wrong " + x);
    }
  }

}
