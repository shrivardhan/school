import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.util.*;
import java.nio.file.*;
import java.io.*;

public class MapReduceHandler implements MapReduceService.Iface {
  private static final String RETRY = "retry";
  private String[] hostnames;
  //check if synchrnoized/concurrent queue is needed
  private Queue<String> allFiles = new LinkedList<>(); ;

  public MapReduceHandler() {

  }

	@Override
	public String getSentinment(String filenames) throws TException {
    try {
      String hosts = new String(Files.readAllBytes(Paths.get("hostnames.txt")));
      hostnames = hosts.split("\n");
    } catch (Exception e) {
      System.out.println("Could not load hosts " + e);
      return "CANNOT RUN";
    }
    long startTime = System.currentTimeMillis();
    for (final File fileEntry : new File(filenames).listFiles()) {
        allFiles.add(fileEntry.getName());
    }

    try {
      while(allFiles.peek()!=null) {
        String s = allFiles.element();
        Random rand = new Random();
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            String s3 = "-";
            try {
              TTransport transport = new TSocket(hostnames[rand.nextInt(hostnames.length)], 9091);
              transport.open();
              TProtocol protocol = new  TBinaryProtocol(transport);
              MapReduceComputeService.Client client = new MapReduceComputeService.Client(protocol);
              s3 = perform(client,filenames+s);
              // if(s3.equals(RETRY))
              //   allFiles.add(s);
              transport.close();
            } catch (TException x) {
              System.out.println("scores went wrong "+x);
            }
          }
        };
        (new Thread(runnable)).start();
      }
    } catch (Exception e) {
      System.out.println ("Exception in threads is caught " + e);
    }//remove
    try{
      Thread.sleep(5000);
    } catch (Exception e) {
      // need to finish all threads before sort
    }
    String opFileName = "noFile";
    try {
      Random rand = new Random();
      TTransport transport;
      transport = new TSocket(hostnames[rand.nextInt(hostnames.length)], 9091);
      transport.open();
      TProtocol protocol = new  TBinaryProtocol(transport);
      MapReduceComputeService.Client client = new MapReduceComputeService.Client(protocol);
      opFileName = performSort(client);
      transport.close();
    } catch(TException e) {
      System.out.println("sorting went wrong " + e);
    }
    long endTime = System.currentTimeMillis();
    return (endTime-startTime)+"+++"+opFileName;
	}

  @Override
  public boolean ping() throws TException {
    return true;
  }

  private static String perform(MapReduceComputeService.Client client, String filename) throws TException {
    System.out.println("mapping - "+ filename);
    String s2 = client.getSent(filename);
    return s2;
  }

  private static String performSort(MapReduceComputeService.Client client) throws TException {
    System.out.println("sorting");
    String s2 = client.sortSent();
    return s2;
  }

}
