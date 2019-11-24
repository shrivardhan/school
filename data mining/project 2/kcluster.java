import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;
import java.io.*;


class kcluster {
	static HashMap<String, HashMap<String, Float>> inputData = new HashMap<String,HashMap<String, Float>>();
	static HashMap<Integer,String> keyInt = new HashMap<Integer,String>();
	static HashMap<String, ArrayList<String>> classID = new HashMap<String, ArrayList<String>>();

	static String inputFileName;
	static String classFileName;
	static String outputFileName;
	static String criteriaFunction;
	static int clusterSize;
	static int trialSize;

	static int centroidFunction = 0;
	static int[] seed = {1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,37,39};
	static float globalCriteriaVal = 0.0f;
	static int trialVal = -1;
	static HashMap<String,Integer> globalAssignment = new HashMap<String,Integer>();
	static HashMap<String, HashMap<String, Float>> epTable = new HashMap<String,HashMap<String, Float>>();

	final static String CLUSTERTOTAL = "clusterTotal";
	final static String TOPICTOTAL = "topictotal";
	final static String PURITY = "purity";
	final static String ENTROPY = "entropy";


	static ArrayList<HashMap<String,Float>> initialCentroids = null;
	static HashMap<String, Integer> clusterAssignment = new HashMap<String,Integer>();

	static HashMap<String, Integer> newAssignment = new HashMap<String,Integer>();
	static ArrayList<HashMap<String,Float>> newInitialCentroids = null;

	static HashMap<String,Float> globalMean = new HashMap<String,Float>();
	static boolean first = true;

	public static void main(String[] args) {
		if(args.length != 6) {
			System.out.println("wrong input size");
			return;
		}
		
		inputFileName = args[0];
		criteriaFunction = args[1];
		classFileName = args[2];
		clusterSize = Integer.parseInt(args[3]);
		trialSize = Integer.parseInt(args[4]);
		outputFileName = args[5];
		long startTime = System.currentTimeMillis();
		processInput(readInput(inputFileName));
		if(criteriaFunction.equals("SSE"))
			centroidFunction = 1;
		else if(criteriaFunction.equals("I2"))
			centroidFunction = 0;
		else  {
			centroidFunction = 2;
			calculateGlobalMean();
		}
		// System.out.println(""+centroidFunction);
		cluster(true);
		processClassFile(readInput(classFileName));
		computeEPTable();
		writeOutput(outputFileName);
		// printOutput();
		long endTime = System.currentTimeMillis();
		System.out.println("Best trial = " + trialVal + ", best criteria Function value = " + globalCriteriaVal);
		System.out.println("Entropy = " + (-1f)*epTable.get(TOPICTOTAL).get(ENTROPY) + " and Purity = "+ epTable.get(TOPICTOTAL).get(PURITY));
		System.out.println("Time taken = " + (float)(endTime-startTime)/1000+" seconds");
	}

	static void printOutput() {
		// System.out.println("\nTABLE:\n\t");
		ArrayList<String> temp = new ArrayList<String>();
		for(String topicKey : classID.keySet()) {
			temp.add(topicKey);
			System.out.print(topicKey+"\t");
		}
		System.out.println();
		// temp.add(PURITY);
		// System.out.println(PURITY+"\t");
		// temp.add(ENTROPY);
		// System.out.println(ENTROPY+"\t");
		for(int clusterKey=0;clusterKey<clusterSize;clusterKey++) {
			HashMap<String,Float> clusterVals = epTable.get(clusterKey+"");
			System.out.print(clusterKey+"    \t");
			for(String topicKey : temp) {
				Float val = clusterVals.get(topicKey);
				if(val == null)
					val = 0.0f;
				if(!(topicKey.equals(PURITY)||topicKey.equals(ENTROPY)))
					System.out.print("  "+Math.round(val)+"  \t");
				// else if (topicKey.equals(PURITY))
				// 	System.out.print("  "+(float)Math.round(val*100)/100+"  \t");
				// else
				// 	System.out.print("  "+(float)(-1)*Math.round(val*100)/100+"  \t");				
			}
			System.out.println();
		}
		// HashMap<String,Float> totalVals = epTable.get(TOPICTOTAL);
		// System.out.print("TOTAL\t");
		// for(String topicKey : temp) {
		// 	if(!(topicKey.equals(PURITY)||topicKey.equals(ENTROPY)))
		// 			System.out.print("  "+Math.round(totalVals.get(topicKey))+"  \t");
		// 	else if (topicKey.equals(PURITY))
		// 		System.out.print("  "+(float)Math.round(totalVals.get(topicKey)*100)/100+"  \t");
		// 	else
		// 		System.out.print("  "+(float)(-1)*Math.round(totalVals.get(topicKey)*100)/100+"  \t");
		// }
		// System.out.println();
		System.out.println("Best trial = " + trialVal + ", best criteria Function value = " + globalCriteriaVal);
		System.out.println("Entropy = " + (-1f)*epTable.get(TOPICTOTAL).get(ENTROPY) + " and Purity = "+ epTable.get(TOPICTOTAL).get(PURITY));
	}

	static void writeOutput(String fileName) {
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
			for(String key : globalAssignment.keySet()) {
				bw.write(key+","+globalAssignment.get(key)+"\n");
			}
		} catch(Exception e) {
			System.out.println("Something went wrong while writing output");
		}
	}

	static String readInput(String fileName) {
		String data = "";
		try{
			data = new String(Files.readAllBytes(Paths.get(fileName))); 
		} catch(Exception e) {
			System.out.println("something went wrong while reading input" + e);
		}
		return data;
	}

	static void processInput(String data) {
		String[] inputValues = data.split("\n");
		HashMap<String, Float> innerMap = null;
		int temp=0;
		for (String inputVal: inputValues) {
			String[] vals = inputVal.split(",");
			keyInt.put(temp++,vals[0]);
			innerMap =  inputData.get(vals[0]);
			if(innerMap == null)
				innerMap = new HashMap<String, Float>();
			Float val = innerMap.get(vals[1]);
			if(val == null)
				innerMap.put(vals[1],Float.valueOf(vals[2]));
			inputData.put(vals[0],innerMap);
		}
	}

	static void processClassFile(String data) {
		String[] inputValues = data.split("\n");
		for (String inputVal: inputValues) {
			String[] vals = inputVal.split(",");
			ArrayList<String> ids = classID.get(vals[1]);
			if(ids == null || ids.size() == 0)
				ids = new ArrayList<String>();
			ids.add(vals[0]);
			classID.put(vals[1],ids);
		}
	}

	static void initializeCentroids(int trial) {
		Random rand;
		if(trial<20)
		 rand = new Random(seed[trial]);
		else
			rand = new Random(trial);
		initialCentroids = new ArrayList<>(clusterSize);
		newInitialCentroids = new ArrayList<>(clusterSize);
		ArrayList<Integer> randomPoints = new ArrayList<Integer>();
		for (int i=0;i<clusterSize;i++) {
			int randPoint = rand.nextInt(inputData.size());
			if(randomPoints.contains(randPoint)) {
				--i;
				continue;
			}
			String key = keyInt.get(randPoint);
			HashMap<String,Float> inputPoint = inputData.get(key);
			HashMap<String,Float> newPoint = new HashMap<String,Float>();
			for(String dataKeys:inputPoint.keySet()) {
				newPoint.put(dataKeys,inputPoint.get(dataKeys));
			}
			initialCentroids.add(i,newPoint);
			newInitialCentroids.add(i,newPoint);
			randomPoints.add(randPoint);
		}
	}

	static void calculateGlobalMean() {
		int totPoints = 0;
		for(String key:inputData.keySet()) {
			++totPoints;
			HashMap<String, Float> dataPoint = inputData.get(key);
			for(String dataKey: dataPoint.keySet()) {
				Float val = globalMean.get(dataKey);
				if(val == null) 
					val = 0.0f;
				globalMean.put(dataKey,val+dataPoint.get(dataKey));
			}
		}
		for(String key: globalMean.keySet()) {
			globalMean.put(key,globalMean.get(key)/totPoints);
		}

	}

	static float globalE1Criteria(HashMap<String, Integer> newClusterAssignment,ArrayList<HashMap<String,Float>> newCentroids) {
		float res = 0.0f;
		for(int i = 0;i<clusterSize;i++) {
			int tot = 0;
			for(String dataKey:inputData.keySet()) {
				if(newClusterAssignment.get(dataKey)!=i)
					continue;
				++tot;
			}
			res += tot*calculateCriteria(newCentroids.get(i),globalMean);
		}
		return res;
	}

	static float calculateE1Criteria(HashMap<String, Integer> newClusterAssignment, int centerVal) {
		HashMap<String, Float> centroid = null;
		if(!first)
			centroid= calculateNewCentroid(centerVal,newClusterAssignment);
		else 
			centroid = newInitialCentroids.get(centerVal);
		float result = calculateCriteria(centroid,globalMean);
		int tot = 0;
		for(String key:newClusterAssignment.keySet()) {
			if(newClusterAssignment.get(key) == centerVal)
				++tot;
		}
		// System.out.println("::"+result*tot);
		return result*tot;
	}

	static boolean clusterUpdateForE1() {
		boolean repeat = false;
		for(String key:inputData.keySet()) {
			HashMap<String, Float> dataPoint = inputData.get(key);
			Integer clusterVal = newAssignment.get(key);
			if(clusterVal == null)
				clusterVal = -1;
			int cent = -1;
			Float res = 0.0f;
			for(int i =0;i<clusterSize;i++) {
				newAssignment.put(key,i);
				float temp = calculateE1Criteria(newAssignment,i);
				if(temp<res  || cent == -1) {
					res = temp;
					cent = i;
				}
			}
			newAssignment.put(key,clusterVal);
			if(clusterVal != cent) {
				newAssignment.put(key,cent);
				repeat = true;
				calculateNewCentroid(clusterVal,cent,newAssignment);
			}
		}
		first = false;
		return repeat;
	}

	static void cluster(boolean run) {
		int trials = trialSize;
		while(trials>0) {
			newAssignment = new HashMap<String,Integer>();
			// System.out.println("::"+trials);
			initializeCentroids(20-trials);
			// int z = 0;
			boolean repeat = true;
			do {
				repeat =  clusterUpdate();
				// System.out.println(globalCriteria(newAssignment,newInitialCentroids)+"::"+z+++"::"+repeat);
			} while(repeat);
			float res = globalCriteria(newAssignment,newInitialCentroids);
			if(res<globalCriteriaVal || trialVal == -1) {
				globalCriteriaVal = res;
				trialVal = trialSize-trials+1;
				for(String key : newAssignment.keySet()) {
					globalAssignment.put(key,newAssignment.get(key));
				}
			}
			--trials;
		}
	}

	static boolean clusterUpdate() {
		if(centroidFunction == 2)
			return clusterUpdateForE1();
		boolean repeat = false;
		for(String key:inputData.keySet()) {
			HashMap<String, Float> dataPoint = inputData.get(key);
			int cent = -1;
			Float res = 0.0f;
			for(int i =0;i<clusterSize;i++) {
				HashMap<String, Float> centroid = newInitialCentroids.get(i);
				float temp = calculateCriteria(dataPoint,centroid);
				if((temp<res && centroidFunction == 1) || (temp>res && centroidFunction == 0) || cent == -1) {
					res = temp;
					cent = i;
				}
			}
			Integer clusterVal = newAssignment.get(key);
			if(clusterVal == null)
				clusterVal = -1;
			if(clusterVal != cent) {
				newAssignment.put(key,cent);
				repeat = true;
				calculateNewCentroid(clusterVal,cent,newAssignment);
			}
		}
		return repeat;
	}

	static ArrayList<HashMap<String,Float>> updateCentroids(int oldClusterVal, int newClusterVal,HashMap<String, Float> oldCenter,HashMap<String, Float> newCenter) {
		ArrayList<HashMap<String,Float>> newCentroids = new ArrayList<>(clusterSize);
		for(int i=0;i<newInitialCentroids.size();i++) {
			if(oldCenter != null && i == oldClusterVal) {
				newCentroids.add(i,oldCenter);
			}
			else if(i == newClusterVal) {
				newCentroids.add(i,newCenter);
			}
			else {
				newCentroids.add(i,newInitialCentroids.get(i));
			}
		}
		return newCentroids;
	}

	static void calculateNewCentroid(int oldClusterVal, int newClusterVal,HashMap<String, Integer> newClusterAssignment) {
		HashMap<String, Float> oldCenter = null;
		if(oldClusterVal>=0) {
			oldCenter = calculateNewCentroid(oldClusterVal,newClusterAssignment);
		}
		HashMap<String, Float> newCenter = calculateNewCentroid(newClusterVal,newClusterAssignment);
		newInitialCentroids = updateCentroids(oldClusterVal,newClusterVal,oldCenter,newCenter);
	}

	static HashMap<String, Float> calculateNewCentroid(int centerVal, HashMap<String, Integer> newClusterAssignment) {
		HashMap<String, Float> centroid = new HashMap<String,Float>();
		int totalPoints = 0;
		for(String dataKey:inputData.keySet()) {
			if(newClusterAssignment.get(dataKey) == null || newClusterAssignment.get(dataKey)!=centerVal) {
				continue;
			}
			++totalPoints;
			HashMap<String, Float> dataPoint = inputData.get(dataKey);
			for(String key:dataPoint.keySet()) {
				Float f = centroid.get(key);
				if(f == null) {
					f = 0.0f;
				}
				centroid.put(key,dataPoint.get(key)+f);
			}
		}
		for(String key: centroid.keySet()) {
			centroid.put(key,centroid.get(key)/totalPoints);
		}
		return centroid;
	}

	static void cluster() {
		int trials = trialSize;
		while(trials>0) {
			// System.out.println("::"+trials);
			initializeCentroids(20-trials);
			clusterAssignment = new HashMap<String,Integer>();
			HashMap<String, Integer> newClusterAssignment = null;
			ArrayList<HashMap<String,Float>> newCentroids = null;
			// int z = 0;
			do {
				newClusterAssignment = assignCluster(initialCentroids);
				newCentroids = calculateNewCentroid(newClusterAssignment);
				// System.out.println(globalCriteria(newClusterAssignment,initialCentroids)+"::"+z++);
			} while(!compare(newClusterAssignment,newCentroids));
			float res = globalCriteria(clusterAssignment,initialCentroids);
			if(res<globalCriteriaVal || trialVal == -1) {
				globalCriteriaVal = res;
				trialVal = trialSize-trials+1;
				for(String key : clusterAssignment.keySet()) {
					globalAssignment.put(key,clusterAssignment.get(key));
				}
			}
			--trials;
		}
	}

	static HashMap<String, Integer> assignCluster(ArrayList<HashMap<String,Float>> currentCentroids) {
		HashMap<String, Integer> newClusterAssignment = new HashMap<String,Integer>();
		for(String key:inputData.keySet()) {
			HashMap<String, Float> dataPoint = inputData.get(key);
			int cent = -1;
			Float res = 0.0f;
			for(int i =0;i<clusterSize;i++) {
				HashMap<String, Float> centroid = currentCentroids.get(i);
				float temp = calculateCriteria(dataPoint,centroid);
				if((temp<res && centroidFunction == 1) || (temp>res && centroidFunction == 0) || cent == -1) {
					res = temp;
					cent = i;
				}
			}
			newClusterAssignment.put(key,cent);
		}
		return newClusterAssignment;
	}

	static ArrayList<HashMap<String,Float>> calculateNewCentroid(HashMap<String, Integer> newClusterAssignment) {
		ArrayList<HashMap<String,Float>> newCentroids = new ArrayList<>(clusterSize);
		for(int i = 0;i<clusterSize;i++) {
			HashMap<String, Float> centroid = new HashMap<String,Float>();
			int totalPoints = 0;
			for(String dataKey:inputData.keySet()) {
				if(newClusterAssignment.get(dataKey)!=i) {
					continue;
				}
				++totalPoints;
				HashMap<String, Float> dataPoint = inputData.get(dataKey);
				for(String key:dataPoint.keySet()) {
					Float f = centroid.get(key);
					if(f == null) {
						f = 0.0f;
					}
					centroid.put(key,dataPoint.get(key)+f);
				}
			}
			for(String key: centroid.keySet()) {
				centroid.put(key,centroid.get(key)/totalPoints);
			}
			newCentroids.add(i,centroid);
		}
		return newCentroids;
	}

	static float calculateCriteria(HashMap<String, Float> dataPoint, HashMap<String, Float> centroid) {
		float result = 0.0f;
		if(centroidFunction == 1) {
			for(String key:dataPoint.keySet()) {
				Float f = centroid.get(key);
				if(f == null)
					f = 0.0f;
				result += (float)Math.pow(dataPoint.get(key)-f,2);
			}
			for(String key:centroid.keySet()) 
				if(dataPoint.get(key) == null) 
					result += (float)Math.pow(centroid.get(key),2);
		} else {
			for(String key:dataPoint.keySet()) {
				Float f = centroid.get(key);
				if(f == null)
					continue;
				result += dataPoint.get(key)*f;
			}
			float val = 0.0f;
			for(String key:dataPoint.keySet()) 
				val += (float)Math.pow(dataPoint.get(key),2);
			result = result/(float)Math.sqrt(val); 
			val = 0.0f;
			for(String key:centroid.keySet()) 
				val += (float)Math.pow(centroid.get(key),2);
			result = result/(float)Math.sqrt(val);
		}
		return result;
	}

	static boolean compare(HashMap<String, Integer> newClusters, ArrayList<HashMap<String,Float>> newCentroids) {
		boolean result = true;
		for(String key : newClusters.keySet()) {
			if(!newClusters.get(key).equals(clusterAssignment.get(key))) {
				result = false;
				break;
			}
		}
		clusterAssignment = newClusters;
		newClusters = new HashMap<String,Integer>();
		initialCentroids = newCentroids;
		newCentroids = new ArrayList<HashMap<String,Float>>();
		return result;
	}

	static float globalCriteria(HashMap<String, Integer> newClusterAssignment,ArrayList<HashMap<String,Float>> newCentroids) {
		if(centroidFunction == 2)
			return globalE1Criteria(newClusterAssignment,newCentroids);
		float res = 0.0f;
		for(int i = 0;i<clusterSize;i++) {
			for(String dataKey:inputData.keySet()) {
				if(newClusterAssignment.get(dataKey)!=i)
					continue;
				res += calculateCriteria(inputData.get(dataKey),newCentroids.get(i));
			}
		}
		return res;
	}

	static void computeEPTable() {
		for(int i = 0; i < clusterSize; i++) 
			epTable.put(i+"",new HashMap<String,Float>());
		epTable.put(TOPICTOTAL,new HashMap<String,Float>());
		HashMap<String,Float> total = epTable.get(TOPICTOTAL);
		Float tot = 0f;
		for(String key:classID.keySet()) {
			for(String id:classID.get(key)) {
				String cluster = globalAssignment.get(id)+"";
				HashMap<String,Float> clusterTemp = epTable.get(cluster);
				Float topicVal = clusterTemp.get(key);
				if(topicVal == null)
					topicVal = 0f;
				clusterTemp.put(key,topicVal+1f);
				Float clusterTot = clusterTemp.get(CLUSTERTOTAL);
				if(clusterTot == null)
					clusterTot = 0f;
				clusterTemp.put(CLUSTERTOTAL,clusterTot+1f);
				Float topicTot = total.get(key);
				if(topicTot == null)
					topicTot = 0f;
				total.put(key,topicTot+1f);
				tot += 1f;
			}
		}
		total.put(CLUSTERTOTAL,tot);
		// System.out.println("::\n"+epTable.toString());
		Float totE = 0.0f;
		Float totP = 0.0f;
		for(String clusterKey:epTable.keySet()) {
			if(clusterKey.equals(TOPICTOTAL))
				continue;
			HashMap<String,Float> clusterVals = epTable.get(clusterKey);
			Float clusterP = 0.0f;
			Float clusterE = 0.0f;
			Float clustertot = clusterVals.get(CLUSTERTOTAL);
			for(String topicKey: clusterVals.keySet()) {
				if(topicKey.equals(CLUSTERTOTAL))
					continue;
				Float topicP = clusterVals.get(topicKey)/clustertot;
				if(topicP > clusterP)
					clusterP = topicP;
				clusterE += topicP * (float)(Math.log(topicP)/Math.log(2));
			}
			clusterVals.put(PURITY,clusterP);
			clusterVals.put(ENTROPY,clusterE);
			if(clusterVals.get(CLUSTERTOTAL) == null)
				clusterVals.put(CLUSTERTOTAL,0.0f);
			totE += clusterVals.get(CLUSTERTOTAL) * clusterE / epTable.get(TOPICTOTAL).get(CLUSTERTOTAL);
			totP += clusterVals.get(CLUSTERTOTAL) * clusterP / epTable.get(TOPICTOTAL).get(CLUSTERTOTAL);
		}
		epTable.get(TOPICTOTAL).put(PURITY,totP);
		epTable.get(TOPICTOTAL).put(ENTROPY,totE);
	}
}