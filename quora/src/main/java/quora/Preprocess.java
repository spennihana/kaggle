package quora;


import water.H2OApp;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.Vec;
import water.parser.BufferedString;

import static water.KaggleUtils.importParseFrame;

public class Preprocess extends MRTask<Preprocess> {

  private static final int ID=0;
  private final int Q1;
  private final int Q2;
  private final boolean _test;

  Preprocess(boolean test) {
    _test=test;
    Q1=_test?1:3;
    Q2=_test?2:4;
  }
  @Override public void map(Chunk[] cs) {

    BufferedString bstr = new BufferedString();
    for(int r=0;r<cs[0]._len;++r) {
      int id = (int)cs[ID].at8(r);
      String q1 = cs[Q1].atStr(bstr,r).toString();
      String q2 = cs[Q2].atStr(bstr,r).toString();


//      System.out.println();
    }
  }


  public static void main(String[] args) {
    H2OApp.main(args);
    Frame fr = importParseFrame("./data/train.csv","train", new byte[]{Vec.T_NUM,Vec.T_NUM,Vec.T_NUM, Vec.T_STR, Vec.T_STR, Vec.T_NUM});
    Chunk[] cs = new Chunk[fr.numCols()];
    for(int i=0;i<cs.length;++i) cs[i] = fr.vec(i).chunkForChunkIdx(0);
    long s = System.currentTimeMillis();
//    new Predict("./models/0001.model").map(cs,new NewChunk[]{});
    new Preprocess(false).doAll(fr);
    System.out.println("all done: " + (System.currentTimeMillis()-s)/1000. + " seconds");
  }
}
