import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.util.Scanner;

public class MapReduceClient {
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
    String s1 = client.getSentinment("./input_dir/");
    System.out.println("returned from server: "+s1);
    // int i=0;
    // Scanner reader = new Scanner(System.in);

    // while(i!=3) {
    //   System.out.println("Menu:\n1:Sort Directory\n2.Quit");
    //   i = reader.nextInt();
    //   switch(i) {
    //     case 1: System.out.println("Enter Directory");
    //             String d1 = reader.next();
    //             String op = client.getSentinment(d1);
    //             //Need to go through File and op the file, and print time
    //             System.out.println("Value: "+ op);
    //             break;
    //     default: i=3;
    //   }

  }
}
