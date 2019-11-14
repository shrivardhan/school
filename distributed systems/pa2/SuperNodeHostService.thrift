service SuperNodeHostService {
	bool ping(),
	string Join(1: string ip, 2: string portnum),
  string PostJoin(1: string ip, 2: string portnum),
  string getHostInfo(1: string id)
}
