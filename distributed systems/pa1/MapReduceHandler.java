import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.*;
import java.nio.file.*;
import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MapReduceHandler implements MapReduceService.Iface {
  private static final String RETRY = "retry";
  private static final String INTERMEDIATE = "./intermediate_dir/";
  private static final String STATSDIR = "./output_dir/";
  private static final String STATSFILE = "stats.txt";
  private String[] hostnames;
  //private Queue<String> allFiles = new LinkedList<>();
  private ConcurrentLinkedQueue<String> allFiles = new ConcurrentLinkedQueue<>();
  private int totalFilesSent = 0;
  private Set<Thread> threads = new HashSet<>();
  private HashMap<String,Stats> stats = new HashMap<>();

  class Stats {
    int numJobs;
    int jobsCompleted;
    float totalTime;
    float avgTime;
    Stats() {
      numJobs = 0;
      jobsCompleted = 0;
      totalTime = 0.0f;
      avgTime = 0.0f;
    }

    void incrementJobs() {
      ++numJobs;
    }

    void incrementCompleted() {
      ++jobsCompleted;
    }

    void addTotTime(float t){
      totalTime += t;
    }

    int getNumJobs() {
      return numJobs;
    }

    int getJobsCompleted() {
      return jobsCompleted;
    }

    float getTotTime() {
      return totalTime;
    }

    float getAvgTime() {
      return totalTime/jobsCompleted;
    }
  }

  public MapReduceHandler() {

  }

	@Override
	public String getSentinment(String filenames) throws TException {
    long startTime = System.currentTimeMillis();
    try {
      String hosts = new String(Files.readAllBytes(Paths.get("hostnames.txt")));
      hostnames = hosts.split("\n");
      if(hostnames.length == 0)
        return "No Hosts. Edit hostnames.txt";
      for(String s : hostnames)
        stats.put(s,new Stats());
    } catch (Exception e) {
      System.out.println("Could not load hosts " + e);
      return "CANNOT RUN";
    }
    System.out.println("Start Mapping");
    int x = 0;
    for (File fileEntry : new File(filenames).listFiles()) {
        allFiles.add(fileEntry.getName());
        x++;
    }
    if(x == 0) {
        return "No files present. Add files to input folder";
    }
    int y = 0;
    // ExecutorService executorService = null;
    try {
      if(new File(INTERMEDIATE).exists() != true) {
  			(new File(INTERMEDIATE)).mkdir();
      }
  		// else
      //   deleteFolder("./intermediate_dir/");
      Thread t = null;
      // ExecutorService executorService = Executors.newFixedThreadPool(100);
      while(allFiles.peek()!=null || totalFilesSent < x) {
        if(allFiles.peek()==null) {
          Thread.sleep(1000);
          // continue;
        }
        y++;
        String s = allFiles.remove();
        Random rand = new Random();
        Runnable runnable = new Runnable() {
        // executorService.execute(new Runnable() {
          @Override
          public void run() {
            String s3 = "-";
            try {
              String host = hostnames[rand.nextInt(hostnames.length)];
              TTransport transport = new TSocket(host, 9091);
              transport.open();
              TProtocol protocol = new  TBinaryProtocol(transport);
              MapReduceComputeService.Client client = new MapReduceComputeService.Client(protocol);
              s3 = perform(client,filenames+s);
              stats.get(host).incrementJobs();
              if(s3.equals(RETRY)) {
                allFiles.add(s);
                // System.out.println("retrying");
              } else {
                stats.get(host).incrementCompleted();
                ++totalFilesSent;
                stats.get(host).addTotTime(Float.parseFloat(s3));
              }
              transport.close();
            } catch (TException x) {
              System.out.println("scores went wrong "+x);
            }
          }
        };
        t = (new Thread(runnable));
        t.start();
      }
      for(Thread t1: threads)
        t1.join();
      // executorService.shutdown();
      // executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      // Thread.sleep(3000);
    } catch (Exception e) {
      // System.out.println ("Exception in threads is caught " + e);
    }
    // System.out.println("files sent"+y);
    try{
     Thread.sleep(4000);
    } catch (Exception e) {
      // need to finish all threads before sort
    }
    System.out.println("stats for mapping");
    for(String host : hostnames) {
      System.out.println("hosntame name = "+ host + "\ntotal jobs sent = "+stats.get(host).getNumJobs() +
      "\ntotal jobs completed = "+stats.get(host).getJobsCompleted()+"\ntotal time for execution = "+
      stats.get(host).getTotTime()+"ms\naverage time taken = "+stats.get(host).getAvgTime()+"ms");
    }
    String opFileName = "noFile";
    float sortTime = 0.0f;
    String sortHost = "";
    try {
      Random rand = new Random();
      TTransport transport;
      sortHost = hostnames[rand.nextInt(hostnames.length)];
      transport = new TSocket(sortHost, 9091);
      transport.open();
      TProtocol protocol = new  TBinaryProtocol(transport);
      MapReduceComputeService.Client client = new MapReduceComputeService.Client(protocol);
      opFileName = performSort(client);
      if(opFileName.split("###").length > 1) {
        sortTime = Float.parseFloat(opFileName.split("###")[0]);
        opFileName = opFileName.split("###")[1];
        System.out.println("Sorting on host = "+sortHost +", time taken = " + sortTime+"ms");
      }
      transport.close();
    } catch(TException e) {
      System.out.println("sorting went wrong " + e);
    }
    long endTime = 0l;
    try{
      File file = new File(STATSDIR+STATSFILE);
			file.delete();
			file.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
      for(String host : hostnames) {
        writer.write("hosntame name = "+ host + "\ntotal jobs sent = "+stats.get(host).getNumJobs() +
        "\ntotal jobs completed = "+stats.get(host).getJobsCompleted()+"\ntotal time for execution = "+
        stats.get(host).getTotTime()+"ms\naverage time taken = "+stats.get(host).getAvgTime()+"ms\n");
      }
      writer.write("Sorting on host = "+sortHost +", time taken = " + sortTime+"ms\n");
      endTime = System.currentTimeMillis();
      writer.write("Total time taken for the entire job = "+(endTime-startTime)+"ms\n");
      writer.close();
    } catch(Exception e) {
      System.out.println("writing stats went wrong " + e);
    }

    // System.out.println("Total files - " + x);
    return (endTime-startTime)+"###"+opFileName+"###"+STATSFILE;
	}

  @Override
  public boolean ping() throws TException {
    return true;
  }

  private static String perform(MapReduceComputeService.Client client, String filename) throws TException {
    // System.out.println("mapping - "+ filename);
    String s2 = client.getSent(filename);
    return s2;
  }

  private static String performSort(MapReduceComputeService.Client client) throws TException {
    System.out.println("sorting");
    String s2 = client.sortSent();
    return s2;
  }

  private static void deleteFolder(String path) {
		File index = new File(path);
		String[]entries = index.list();
		for(String s: entries){
	    File currentFile = new File(index.getPath(),s);
	    currentFile.delete();
		}
  }

}
