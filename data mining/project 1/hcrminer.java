
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;
import java.io.*;

class hcrminer {

	static ArrayList<ArrayList<Integer>> csr = new ArrayList<ArrayList<Integer>>();
	static HashMap<Integer, Integer> intialDataItemsFreq = new HashMap<Integer,Integer>();

	static HashMap<String,Integer> frequentItemSets = new HashMap<String,Integer>();
	static HashMap<String,String> connection= new HashMap<String,String>();
	static HashMap<String,Float> rules= new HashMap<String,Float>();

	static HashSet<Integer> tidList = new HashSet<Integer>();
	static int maxTid = 0;
	static int maxItem = 0;

	static int minsup;
	static float minconf;
	static String inputFileName;
	static String outputFileName;
	static int options;

	static final String delim = ",";
	static final String ruledelim = "-->";

	public static void main(String[] args) {
		if(args.length != 5) {
			System.out.println("wrong input size");
			return;
		}
		
		minsup = Integer.parseInt(args[0]);
		minconf = Float.parseFloat(args[1]);
		inputFileName = args[2];
		outputFileName = args[3];
		options = Integer.parseInt(args[4]);
		String data = readInput(inputFileName);
		long startTime1 = System.currentTimeMillis();
		createCSR(data);
		startProjection();
		long endTime1 = System.currentTimeMillis();
		System.out.println("Number of Frequent ItemSets: " + frequentItemSets.size());
		// System.out.println("Frequent Item Sets: "+frequentItemSets.toString());
		System.out.println("Time taken for frequentItemSets gen = " + (float)(endTime1-startTime1)/1000);
		long startTime2 = System.currentTimeMillis();
		startRules();
		long endTime2 = System.currentTimeMillis();
		System.out.println("Number of rules: " + rules.size());
		// System.out.println(rules.toString());
		System.out.println("Time taken for rule gen = " + (float)(endTime2-startTime2)/1000);
		writeOutput(outputFileName);
		
	}

	static void writeOutput(String fileName) {
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
			if(minsup == 15 || minsup == 20) {
				for(String items: frequentItemSets.keySet()) {
					bw.write(items.replace(',',' ') + " | " + "{}" + " | " + frequentItemSets.get(connection.get(items)) 
					+ " | " + "-1" + "\n");
				}
				return;
			}
			for(String s : rules.keySet()) {
				String[] t1 = s.split(ruledelim);
				String lhs = t1[0];
				String rhs = t1[1];
				bw.write(lhs.replace(',',' ') + " | " + rhs.replace(',',' ') + " | " + frequentItemSets.get(connection.get(s)) 
					+ " | " + rules.get(s) + "\n");
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

	static void createCSR(String data){
		String[] transactions = data.split("\n");
		int oldTransaction = -1;
		ArrayList<Integer> temp = null;
		for(String transaction : transactions ) {
			String[] t = transaction.split(" ");
			int curTid = Integer.parseInt(t[0]);
			int curItem = Integer.parseInt(t[1]);
			tidList.add(curTid);
			if(curTid!=oldTransaction || oldTransaction == -1) {
				csr.add(curTid,new ArrayList<Integer>());
				oldTransaction = curTid;
			}
			Integer tempCount = intialDataItemsFreq.get(curItem);
			if(tempCount == null)
				tempCount = 0 ;

			intialDataItemsFreq.put(curItem, tempCount + 1);

			temp = csr.get(curTid);
			temp.add(curItem);
		}
		Iterator<Map.Entry<Integer, Integer>> iterator = intialDataItemsFreq.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Integer> entry = iterator.next();
			if (entry.getValue() < minsup) {
				iterator.remove();
			}
		}

	}

	static HashMap<Integer, Integer> calcFreq(HashSet<Integer> allowedTransactions, Set<Integer> allowedItems) {
		HashMap<Integer, Integer> dataitemsfreq = new HashMap<Integer,Integer>();
		for(int item: allowedItems) {
			int freqItem = 0;
			for(int transaction: allowedTransactions) {
				freqItem += (csr.get(transaction)).contains(item)?1:0; 
			}
			if(freqItem >= minsup)
				dataitemsfreq.put(item,freqItem);
		}
		return dataitemsfreq;
	}

	static HashMap<Integer, Integer> sortItems(HashMap<Integer, Integer> dataItemsFrequency) {

		List<Map.Entry<Integer, Integer>> list = 
		new LinkedList<Map.Entry<Integer, Integer>>(dataItemsFrequency.entrySet()); 
		if(options == 1) {
			Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() { 
				@Override
				public int compare(Map.Entry<Integer, Integer> e1,  
					Map.Entry<Integer, Integer> e2) { 
					return (e1.getKey()).compareTo(e2.getKey()); 
				} 
			}); 
			
		}
		else if(options == 2) {
			Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() { 
				@Override
				public int compare(Map.Entry<Integer, Integer> e1,  
					Map.Entry<Integer, Integer> e2) { 
					return (e1.getValue()).compareTo(e2.getValue()); 
				} 
			}); 
		}
		else if(options == 3) {
			Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() { 
				@Override
				public int compare(Map.Entry<Integer, Integer> e1,  
					Map.Entry<Integer, Integer> e2) { 
					return (e2.getValue()).compareTo(e1.getValue()); 
				} 
			}); 
		}
		HashMap<Integer, Integer> sortedKeys = new LinkedHashMap<Integer, Integer>(); 
		for (Map.Entry<Integer, Integer> e : list) { 
			sortedKeys.put(e.getKey(), e.getValue()); 
		} 
		return sortedKeys;
	}

	static void startProjection() {
		createProjection("",sortItems(intialDataItemsFreq),tidList);
	}

	static void createProjection(String lastItem, HashMap<Integer, Integer> allowedItems,HashSet<Integer> allowedTransactions) {
		for(int s : allowedItems.keySet()) {
			String newLastItem = lastItem+s+delim;
			frequentItemSets.put(newLastItem,allowedItems.get(s));
			HashSet<Integer> transactionList = new HashSet<Integer>();
			HashMap<Integer,ArrayList<Integer>> projectionDB = new HashMap<Integer,ArrayList<Integer>>();
			for(int i : allowedTransactions) {
				ArrayList<Integer> temp = csr.get(i);
				int itemIndex = temp.indexOf(s);
				if(itemIndex<0)
					continue;
				transactionList.add(i);
				temp.remove(itemIndex);
				projectionDB.put(i, new ArrayList<Integer>(temp));
			}
			HashMap<Integer, Integer> newDataItemsFreq = calcFreq(transactionList,allowedItems.keySet());
			createProjection(newLastItem,sortItems(newDataItemsFreq),transactionList);

			for(int i : projectionDB.keySet()) {
				csr.remove(i);
				csr.add(i,new ArrayList<Integer>(projectionDB.get(i)));
			}
		}
	}

	static String sortItemsForRules(String itemset) {
		String temp = "";
		temp = permuteHelper(itemset.split(","),0);
		return temp;
	}



	private static String permuteHelper(String[] arr, int index){
		if(index >= arr.length - 1){ 
			String temp = "";
			for(int i = 0; i < arr.length - 1; i++){
				temp = temp+arr[i]+delim;
			}
			if(arr.length > 0)  {
				temp = temp+arr[arr.length - 1]+delim;
			}
			if(frequentItemSets.get(temp)!=null) {
				return temp;
			}
			return null;
		}

		for(int i = index; i < arr.length; i++){ 
			String t = arr[index];
			arr[index] = arr[i];
			arr[i] = t;
			String temp = permuteHelper(arr, index+1);
			if(temp != null)
				return temp;
			t = arr[index];
			arr[index] = arr[i];
			arr[i] = t;
		}
		return null;
	}

	static void startRules() {
		for(String items: frequentItemSets.keySet()) {
			ArrayList<String> item1set = new ArrayList<String>(Arrays.asList(items.split(",")));
			int len = item1set.size();
			generateRules(items,new ArrayList<String>(item1set),1,len);
		}
	}

	static void generateRules(String fItem, ArrayList<String> items, int consequentlen, int itemlen) {
		if(itemlen>consequentlen) {
			String[] tempSplit =  fItem.split(",");
			Iterator<String> it = items.iterator();
			while(it.hasNext()) {
				String s = it.next();
				String temp = "";
				for(String t: tempSplit) {
					ArrayList<String> s1 = new ArrayList<String>(Arrays.asList(s.split(",")));
					if(!s1.contains(t))
						temp = temp+t+delim;
				}
				float conf = 0.0f;
				try {
					conf = (float)frequentItemSets.get(fItem)/frequentItemSets.get(temp);
				} catch (Exception e) {
					temp = sortItemsForRules(temp);
					conf = (float)frequentItemSets.get(fItem)/frequentItemSets.get(temp);
				}

				if(conf >= minconf) {
					connection.put(temp+ruledelim+s,fItem);
					rules.put(temp+ruledelim+s,conf);
				}
				else {
					it.remove();
				}
			}
			ArrayList<String> newConsequents = candidategen(items,consequentlen); 
			generateRules(fItem, new ArrayList<String>(newConsequents),consequentlen+1,itemlen);
		}
	}

	static ArrayList<String> candidategen(ArrayList<String> items, int len) {
		ArrayList<String> newset = new ArrayList<String>();
		for(int i =0 ; i < items.size() ; i++) {
			String[] tempSplit1 = items.get(i).split(",");
			for(int j = i+1 ; j < items.size(); j++) {
				String[] tempSplit2 = items.get(j).split(",");
				boolean merge = true;
				for(int k=0;k<len-1;k++) {
					if(!tempSplit1[k].equals(tempSplit2[k])) {
						merge = false;
						break;
					}
				}
				if(merge) {
					newset.add(items.get(i)+delim+tempSplit2[len-1]);
				}
			}
		}
		return newset;
	}
}
