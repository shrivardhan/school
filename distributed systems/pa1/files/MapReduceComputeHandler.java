import org.apache.thrift.TException;
import java.util.*;
import java.nio.file.*;
import java.io.*;

public class MapReduceComputeHandler implements MapReduceComputeService.Iface {
	Set<String> positives;
	Set<String> negatives;
	float loadProabability = 0f;
	float sleepTime = 3f;

	public MapReduceComputeHandler(float load, float sleepT) {
		try{
			loadProabability = load;
			sleepTime = sleepT;
			String datap = new String(Files.readAllBytes(Paths.get("positive.txt")));
			String datan = new String(Files.readAllBytes(Paths.get("negative.txt")));
			positives = new HashSet<String>(Arrays.asList(datap.split("\n")));
			negatives = new HashSet<String>(Arrays.asList(datan.split("\n")));
		} catch (Exception e) {
			System.out.println("error while reading files" + e);
		}
	}

	@Override
	public String getSent(String filename) throws TException {
    boolean reject = (Math.random()>loadProabability)?false:true;
		if(reject)
			return "retry";
		float pscore = 0f;
		float nscore = 0f;
		HashSet<String> allwords;
		if(positives == null || negatives == null)
			return "no words";
		try{
			System.out.println("Processing : "+filename.split("/")[filename.split("/").length-1]);
			if(Math.random()<loadProabability)
				Thread.sleep((long)sleepTime*1000);
			File file = new File(filename);
    	Scanner sc = new Scanner(file);
			while (sc.hasNextLine()) {
				allwords = new HashSet<String>(Arrays.asList(sc.nextLine().split(" ")));
				for(String w : allwords)
					if(positives.contains(w))
						pscore += 1f;
					else if (negatives.contains(w))
						nscore += 1f;
			}
		} catch (Exception e) {
			System.out.println("error while reading files" + e);
			return "cannot read";
		}
		float score = (pscore - nscore)/(pscore + nscore);
		String pathString = "./intermediate_dir/";
		try {
			File file = new File(pathString+filename.split("/")[filename.split("/").length-1]);
			if(new File(pathString).exists()) {
				deleteFolder(pathString);
			}
			(new File(pathString)).mkdir();
			file.delete();
			file.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(filename+"###"+score);
			writer.close();
		} catch (Exception e) {
			System.out.println("error while writing files: " + e);
			return "FIRE";
		}
		return "completed";
	}

	@Override
	public boolean ping() throws TException {
		return true;
	}

	@Override
	public String sortSent() throws TException {
		String directory = "./intermediate_dir/";
		System.out.println("Sorting : "+directory);
		HashMap<String,Float> sorted = new HashMap<>();
		try {
			for (final File fileEntry : new File(directory).listFiles()) {
				String[] scoreMap = (new String(Files.readAllBytes(Paths.get(fileEntry.getPath())))).split("###");
				sorted.put(scoreMap[0],Float.valueOf(scoreMap[1]));
			}
		} catch (Exception e) {
			System.out.println("error while output " + e);
		}

		List<Map.Entry<String, Float>> list = new LinkedList<Map.Entry<String, Float>>(sorted.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<String, Float> >() {
        public int compare(Map.Entry<String, Float> o1,
                           Map.Entry<String, Float> o2)
        {
            return (o1.getValue()).compareTo(o2.getValue());
        }
    });
    HashMap<String, Float> sortedMap = new HashMap<String, Float>();
    for (Map.Entry<String, Float> listEntry : list) {
        sortedMap.put(listEntry.getKey(), listEntry.getValue());
    }
		String opDirectory = "./ouput_dir/";
		if(new File(opDirectory).exists()) {
			deleteFolder(opDirectory);
		}
		(new File(opDirectory)).mkdir();
		try {
			File file = new File(opDirectory+"opFile.txt");
			file.delete();
			file.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write("\n");
			for(String keys:sortedMap.keySet())
				writer.write(keys+" : "+sortedMap.get(keys)+"\n");
			writer.close();
		} catch (Exception e) {
			System.out.println("could not write to opFile : "+ e);
		}
		return opDirectory+"opFile.txt";
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
