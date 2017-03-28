package water;

import water.fvec.Frame;
import water.parser.ParseDataset;
import water.parser.ParseSetup;

import java.util.ArrayList;

public class KaggleUtils {

  public static Frame importParseFrame(String datasetPath, String name, byte[] types) {
    ArrayList<String> files = new ArrayList<>(); ArrayList<String> keys = new ArrayList<>();
    ArrayList<String> fails = new ArrayList<>(); ArrayList<String> dels = new ArrayList<>();
    H2O.getPM().importFiles(datasetPath,null,files,keys,fails,dels);
    String datasetName = name==null?datasetPath.split("\\.(?=[^\\.]+$)")[0]:name;
    ParseSetup ps = ParseSetup.guessSetup(new Key[]{Key.make(keys.get(0))}, false, 0);
    ps.setColumnTypes(types);
    return ParseDataset.parse(Key.make(datasetName), new Key[]{Key.make(keys.get(0))}, true, ps);
  }

  public static Frame importParseFrame(String datasetPath, String name) {
    ArrayList<String> files = new ArrayList<>(); ArrayList<String> keys = new ArrayList<>();
    ArrayList<String> fails = new ArrayList<>(); ArrayList<String> dels = new ArrayList<>();
    H2O.getPM().importFiles(datasetPath,null,files,keys,fails,dels);
    String datasetName = name==null?datasetPath.split("\\.(?=[^\\.]+$)")[0]:name;
    ParseSetup ps = ParseSetup.guessSetup(new Key[]{Key.make(keys.get(0))}, false, 0);
    return ParseDataset.parse(Key.make(datasetName), new Key[]{Key.make(keys.get(0))}, true, ps);
  }
}
