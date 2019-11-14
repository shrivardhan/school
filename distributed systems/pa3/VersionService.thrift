service VersionService {
  bool ping(),
  string getFileVersion(1:string filename),
  string getVersion(1:string filename),
  string updateFileVersion(1:string filename, 2:string version)
}
