service HostClientService {
	bool ping(),
	string setTitle(1: string title, 2: string genre),
  string getTitle(1: string title),
	string setFromFile(1: string file)
}
