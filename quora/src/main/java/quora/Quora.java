package quora;

import water.DKV;
import water.H2OApp;
import water.Key;
import water.fvec.Frame;
import water.fvec.Vec;

import java.io.File;

import static water.KaggleUtils.importParseFrame;

public class Quora {
  public static void main(String[] args) {
    H2OApp.main(args);

    String name = "train";
    String path = "./data/train_clean.csv";
    byte[] types= new byte[]{Vec.T_NUM,Vec.T_NUM,Vec.T_NUM, Vec.T_STR, Vec.T_STR, Vec.T_NUM};
    Frame fr = importParseFrame(path,name, types);

    File f= new File("./data/user_labels.csv");
    Frame userLabels;
    if( f.exists() )
      importParseFrame(f.getPath(),"user_labels",new byte[]{Vec.T_NUM});
    else {
      userLabels = new Frame(Key.make("user_labels"), new String[]{"user_labels"}, new Vec[]{fr.anyVec().makeCon(-1)});
      DKV.put(userLabels);
    }
  }
}
