import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import java.util.Scanner;

public class MapReduceComputeServer {

  public static MapReduceComputeHandler handler;

  public static MapReduceComputeService.Processor processor;

  public static void main(String [] args) {
    try {
      Scanner reader = new Scanner(System.in);
      int i = 0;
      float load = 0f;
      float sleepTime = 3f;
      while(i!=3) {
        System.out.println("Menu:\n1:Enter Load Probability (default = 0 {random policy}) \n2:Enter Sleep Time (default = 3s)\n3.To start server");
        i = reader.nextInt();
        switch(i) {
          case 1: System.out.println("Enter Load Probability");
                  String d1 = reader.next();
                  load = Float.parseFloat(d1);
                  break;
          case 2: System.out.println("Enter sleep time in seconds");
                  String d2 = reader.next();
                  sleepTime = Float.parseFloat(d2);
                  break;
          default: i=3;
        }
      }
        System.out.println("load Probability = "+ load + " , sleepTime = "+sleepTime);
      // if(args.length > 0) {
      //   load = Float.parseFloat(args[0]);
      //   sleepTime = 3f;
      // }
      // if(args.length > 1)
      //   sleepTime = Float.parseFloat(args[1]);
      handler = new MapReduceComputeHandler(load,sleepTime);
      processor = new MapReduceComputeService.Processor(handler);

      Runnable simple = new Runnable() {
        public void run() {
          simple(processor);
        }
      };

      new Thread(simple).start();
    } catch (Exception x) {
      x.printStackTrace();
    }
  }

  public static void simple(MapReduceComputeService.Processor processor) {
    try {
      TServerTransport serverTransport = new TServerSocket(9091);
      TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

      System.out.println("Starting the simple server...");
      server.serve();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
