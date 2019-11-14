import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

public class MapReduceComputeServer {

  public static MapReduceComputeHandler handler;

  public static MapReduceComputeService.Processor processor;

  public static void main(String [] args) {
    try {
      float load = 0f;
      float sleepTime = 0f;
      if(args.length > 0) {
        load = Float.parseFloat(args[0]);
        sleepTime = 3f;
      }
      if(args.length > 1)
        sleepTime = Float.parseFloat(args[1]);
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
