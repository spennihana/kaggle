package quora;

import water.H2O;
import water.Job;
import water.fvec.Frame;

import static water.KaggleUtils.importParseFrame;

// stitch test ids on to front of test frame
public class CbindTask {
  public static void main(String[] args) {
    H2O.main(args);
    String outpath = "./data/test_feats13_withIds.csv";
    String testFrame = "./data/test_feats13.csv";
    String testIds   = "./data/test_ids";
    Frame test = importParseFrame(testFrame, "test");
    Frame ids  = importParseFrame(testIds, "testIds");
    Frame out = ids.add(test);
    System.out.println("Writing frame ");
    Job job = Frame.export(out, outpath, out._key.toString(), false, 1);
    job.get();
  }
}
