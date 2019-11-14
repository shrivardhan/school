import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public class MapReduceClient {
  private static final String OUTPUTDIR = "./output_dir/";
  private static final String STATSFILE = "stats.txt";
	private static final String OUTPUTFILE = "output.txt";
	private static final String INTERMEDIATE = "./intermediate_dir/";
  public static void main(String [] args) {
    try {
      TTransport transport;
      transport = new TSocket("localhost", 9090);
      transport.open();
      TProtocol protocol = new  TBinaryProtocol(transport);
      MapReduceService.Client client = new MapReduceService.Client(protocol);
      perform(client);
      transport.close();
    } catch (TException x) {
      x.printStackTrace();
    }
  }

  private static void perform(MapReduceService.Client client) throws TException {
    // String s1 = client.getSentinment("./input_dir/");
    // System.out.println("returned from server: "+s1);
    // if(s1.split("+++").length>1)
    //   writeToFile(s1.split("+++")[0]);
    int i=0;
    Scanner reader = new Scanner(System.in);

    while(i!=3) {
      System.out.println("Menu:\n1:Sort Directory\n2.Quit");
      i = reader.nextInt();
      switch(i) {
        case 1: System.out.println("Enter Directory (Eg: ./input_dir/)");
                String d1 = reader.next();
                String op = client.getSentinment(d1);
                if(op.split("###").length>1) {
                  float time = Float.parseFloat(op.split("###")[0]);
                  // writeToFile(time);
                  readFile(time);
                } else
                  System.out.println(op);

                break;
        default: i=3;
      }
    }
  }

  private static void readFile(float time) {
    try{
      String output = new String(Files.readAllBytes(Paths.get(OUTPUTDIR+OUTPUTFILE)));
      System.out.println(output);
      System.out.println("Time taken for the entire job = "+ time +"ms\n\n");
    } catch (Exception e) {
      System.out.println("Error while reading output file : "+ e);
    }
  }

  // private static void writeToFile(float time) {
  //   try{
  //     File file = new File(OUTPUTDIR+STATSFILE);
  //     file.delete();
  //     file.createNewFile();
  //     // BufferedWriter writer = new BufferedWriter(new FileWriter(file,true));
  //     FileWriter writer = new FileWriter(file,true);
  //     writer.write("Total time taken for the job = "+time+"ms\n");
  //     writer.close();
  //   } catch(Exception e) {
  //     System.out.println("Error while writing stats : "+ e);
  //   }
  // }
}
