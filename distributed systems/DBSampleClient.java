import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.util.Scanner;

public class DBSampleClient {
  public static void main(String [] args) {
    try {
      TTransport transport;

      transport = new TSocket("localhost", 9090);
      transport.open();

      TProtocol protocol = new  TBinaryProtocol(transport);
      DBSampleService.Client client = new DBSampleService.Client(protocol);

      perform(client);

      transport.close();
    } catch (TException x) {
      x.printStackTrace();
    }
  }

  private static void perform(DBSampleService.Client client) throws TException {
    int i=0;
    Scanner reader = new Scanner(System.in);
    while(i!=3) {
      System.out.println("Menu:\n1:Add key-value\n2:Retrive value from key\n3.Quit");
      i = reader.nextInt();
      switch(i) {
        case 1: System.out.println("Enter key");
                String key = reader.next();
                System.out.println("Enter value");
                String value = reader.next();
                boolean p1 = client.put(key,value);
                System.out.println(p1);
                break;
        case 2: System.out.println("Enter key");
                String key1 = reader.next();
                String g1 = client.get(key1);
                System.out.println("Value: "+ g1);
                break;
        default: i=3;
      }
    }
  }
}
