service QuorumService {
  bool ping(),
  string quorum(1:string filename, 2:string serverIp, 3:string ID, 4:string type)
}
