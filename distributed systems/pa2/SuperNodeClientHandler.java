import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.math.BigInteger;

public class SuperNodeClientHandler implements SuperNodeClientService.Iface {

  public SuperNodeClientHandler() {
  }

  @Override
  public String getNode() {
    SuperNodeHostHandler h = new SuperNodeHostHandler();
    HashMap<BigInteger,String> idHostMap;
    if(h.isOperational())
      idHostMap = h.getMap();
    else
      return "";
      BigInteger randomNodeKey = BigInteger.ZERO;

      int rand = (int)(Math.random()*idHostMap.size());
      System.out.println(rand);
      int x = 0;
      String randomNodeInfo;
      for(BigInteger i : idHostMap.keySet()) {
        if(x++ == rand) {
          randomNodeKey = i;
          System.out.println(randomNodeKey);
          break;
        }
      }
      randomNodeInfo = idHostMap.get(randomNodeKey);

      return randomNodeInfo;
  }

  @Override
  public boolean ping() {
    return true;
  }

}
