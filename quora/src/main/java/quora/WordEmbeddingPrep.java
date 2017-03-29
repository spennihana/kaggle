package quora;

import water.H2O;
import water.Job;
import water.Key;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.NewChunk;
import water.fvec.Vec;
import water.nbhm.NonBlockingHashMap;
import water.parser.BufferedString;

import java.io.IOException;

import static water.KaggleUtils.importParseFrame;


public class WordEmbeddingPrep extends MRTask<WordEmbeddingPrep> {

  private static final int ID=0;
  private final int Q1;
  private final int Q2;
  private final boolean _test;

  // remote word embedding server bits
  private transient NonBlockingHashMap<String, double[]> _cache;

  WordEmbeddingPrep(boolean test) {
    _test=test;
    Q1=_test?1:3;
    Q2=_test?2:4;
  }

  @Override public void setupLocal() {
    _cache = new NonBlockingHashMap<>();
  }
  @Override public void map(Chunk[] cs, NewChunk[] ncs) {
    BufferedString bstr = new BufferedString();

    for (int r = 0; r < cs[0]._len; ++r) {
      int ncs_idx = 0;
      int id = (int) cs[ID].at8(r);
      ncs[ncs_idx++].addNum(id);

      String q1 = cs[Q1].isNA(r) ? "" : cs[Q1].atStr(bstr, r).toString();
      String q2 = cs[Q2].isNA(r) ? "" : cs[Q2].atStr(bstr, r).toString();

      double[][] mm = WordEmbeddingsReader.get3(q1,q2);

      // write out the abs diff in the min/max vecs
      for(int i=0;i<mm[0].length;i++) ncs[ncs_idx++].addNum(mm[0][i]);
      for(int i=0;i<mm[1].length;i++) ncs[ncs_idx++].addNum(mm[1][i]);
      if (!_test) ncs[ncs_idx].addNum(cs[cs.length - 1].at8(r));
    }
  }

  public static void main(String[] args) throws IOException {
    H2O.main(args);

    boolean train=true;
    int id=9999;
    String outpath= train?"./data/train_feats"+id+".csv":"./data/test_feats"+id+".csv";
//    String path = train?"./data/train_clean.csv":"./data/test_clean.csv";
    String path = train?"./data/train_sample.csv":"";
    String name = train?"train":"test";
    String key= train?"train_feats":"test_feats";
    int nouts = 1+600+(train?1:0);

    byte[] types= train?new byte[]{Vec.T_NUM,Vec.T_NUM,Vec.T_NUM, Vec.T_STR, Vec.T_STR, Vec.T_NUM}:new byte[]{Vec.T_NUM,Vec.T_STR,Vec.T_STR};
    Frame fr = importParseFrame(path,name, types);

    WordEmbeddingsReader.get2("hello");

    long s = System.currentTimeMillis();
    WordEmbeddingPrep wep = new WordEmbeddingPrep(!train);

    Frame out = wep.doAll(nouts, Vec.T_NUM, fr).outputFrame(Key.make(key),null,null);
    System.out.println("all done: " + (System.currentTimeMillis()-s)/1000. + " seconds");

    System.out.println("Writing frame ");
    Job job = Frame.export(out, outpath, out._key.toString(), false, 1);
    job.get();

    H2O.shutdown(0);
  }
}
