service HostService {
	bool ping(),
	string getSuccessor(1: string val),
  string getPredecessor(1: string val),
	string closestPrecedingFinger(1: string id, 2: i32 j),
	string getKeysData(1: string key),
  string setKeysData(1: string key, 2: string value),
	string transferKeysData(),
  string updateDHT(1: string n, 2: i32 i),
  string findSuccessor(1: string id),
	string printDHT()
}
