service FileService {
  bool ping(),
  string read(1:string filename),
  string write(1:string filename),
  string onComplete(1:string id, 2:i32 val)
}
