import org.apache.thrift.TException;
import java.util.HashMap;

public class DBSampleHandler implements DBSampleService.Iface {

  private HashMap<String,String> tempStorage;

  public DBSampleHandler() {
    tempStorage = new HashMap<String,String>();
  }

	@Override
	public String get(String key) throws TException {
    return tempStorage.get(key);
	}

  @Override
  public boolean ping() throws TException {
    return true;
  }

  @Override
	public boolean put(String key, String val) throws TException {
    try {
      tempStorage.put(key,val);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
	}

}
