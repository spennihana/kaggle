package water;

import water.fvec.Frame;
import water.fvec.NFSFileVec;
import water.parser.ParseDataset;
import water.parser.ParseSetup;

import java.io.File;

public class KaggleUtils {

  public static Frame importParseFrame(String datasetPath, String name, byte[] types) {
    NFSFileVec nfsFileVec = NFSFileVec.make(new File(datasetPath));
    Key k = nfsFileVec._key;
    String datasetName = name==null?datasetPath.split("\\.(?=[^\\.]+$)")[0]:name;
    ParseSetup ps = ParseSetup.guessSetup(new Key[]{k}, false, 0);
    ps.setColumnTypes(types);
    return ParseDataset.parse(Key.make(datasetName), new Key[]{k}, true, ps);
  }

  public static Frame importParseFrame(String datasetPath, String name) {
    NFSFileVec nfsFileVec = NFSFileVec.make(new File(datasetPath));
    Key k = nfsFileVec._key;
    String datasetName = name==null?datasetPath.split("\\.(?=[^\\.]+$)")[0]:name;
    ParseSetup ps = ParseSetup.guessSetup(new Key[]{k}, false, 0);
    return ParseDataset.parse(Key.make(datasetName), new Key[]{k}, true, ps);
  }
}
