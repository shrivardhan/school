
service DBSampleService {
	bool ping(),
	string get(1: string key),
	bool put(1: string key, 2: string val)
}
