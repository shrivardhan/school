import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.TMultiplexedProcessor;
import java.util.Scanner;
import java.nio.file.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;

public class VersionServiceHandler implements VersionService.Iface {
  HashMap<String,String> versions = new HashMap<String,String>(); // map of filename to version

  public VersionServiceHandler() { // constructor to initialize the map
    versions = new HashMap<String,String>();
  }

  public VersionServiceHandler(boolean i) { // constructor without initializing map
  }

  public void updateVersion(String value) { // update versions of all the files in the given map
    String[] keyValuePairs = value.substring(1, value.length()-1).split(",");
    for(String pair : keyValuePairs) {
      String[] entry = pair.split("=");
      versions.put(entry[0].trim(), entry[1].trim());
    }
    System.out.println("After syncing \n"+versions.toString());
  }

  @Override
  public String getVersion(String filename) {
    String[] files = filename.substring(1, filename.length()-1).split(",");
    HashMap<String,String> newVersions = new HashMap<String,String>();
    for(String f:files)
      newVersions.put(f.trim(),(versions.get(f.trim())==null)?"0":versions.get(f.trim()));
    return newVersions.toString();
  }

  @Override
  public String getFileVersion(String filename) { // get version of 1 filename
    versions.put(filename,(versions.get(filename)==null)?"0":versions.get(filename)); // check if present else insert 0 into it
    return versions.get(filename); // send the version
  }

  @Override
  public String updateFileVersion(String filename, String version) { // update version of 1 filename
    return versions.put(filename,version); // insert to map
  }

  @Override
  public boolean ping() {
    return true;
  }
}
