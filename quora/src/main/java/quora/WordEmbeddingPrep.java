package quora;

import water.*;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.NewChunk;
import water.fvec.Vec;
import water.nbhm.NonBlockingHashMap;
import water.parser.BufferedString;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ByteChannel;
import java.util.Arrays;

import static quora.Utils.*;
import static water.KaggleUtils.importParseFrame;


public class WordEmbeddingPrep extends MRTask<WordEmbeddingPrep> {
  private static final String WORDSERVER_IP="192.168.1.145";
  private static final int    WORDSERVER_P = 34534;

  private static final int ID=0;
  private final int Q1;
  private final int Q2;
  private final boolean _test;

  // remote word embedding server bits
  private transient ByteChannel _chan;
  private transient TCPSendThread _sendThread;

  private transient NonBlockingHashMap<String, double[]> _cache;

  WordEmbeddingPrep(boolean test) {
    _test=test;
    Q1=_test?1:3;
    Q2=_test?2:4;
  }

  @Override public void setupLocal() {
    try {
      (_sendThread= TCPSendThread.client(InetAddress.getByName(WORDSERVER_IP),WORDSERVER_P)).start();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }

    try {
      _chan = TCPSendThread.openChan(InetAddress.getByName(WORDSERVER_IP), WORDSERVER_P);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
    _cache = new NonBlockingHashMap<>();
  }
  @Override public void map(Chunk[] cs, NewChunk[] ncs) {
    BufferedString bstr = new BufferedString();
    double[][] min_we = new double[2][300];
    Arrays.fill(min_we[0], Double.MAX_VALUE);
    Arrays.fill(min_we[1], Double.MAX_VALUE);
    double[][] max_we = new double[2][300];
    Arrays.fill(max_we[0], -Double.MAX_VALUE);
    Arrays.fill(max_we[1], -Double.MAX_VALUE);
    for (int r = 0; r < cs[0]._len; ++r) {
      int ncs_idx = 0;
      int id = (int) cs[ID].at8(r);
      ncs[ncs_idx++].addNum(id);

      String q1 = cs[Q1].isNA(r) ? "" : cs[Q1].atStr(bstr, r).toString();
      String q2 = cs[Q2].isNA(r) ? "" : cs[Q2].atStr(bstr, r).toString();
      // drop question marks
      q1 = q1.replace("\\?", "");
      q2 = q2.replace("\\?", "");
      String[] w1 = q1.split(" ");
      String[] w2 = q2.split(" ");

      // undo contractions, remove punctuation, lower
      for (int i = 0; i < w1.length; ++i) w1[i] = contractionMap(w1[i]).replaceAll("\\p{P}", "").toLowerCase();
      for (int i = 0; i < w2.length; ++i) w2[i] = contractionMap(w2[i]).replaceAll("\\p{P}", "").toLowerCase();

      for(String w: w1) {
        double[] c = _cache.get(w);
        min_we[0] = reduceMin(min_we[0], c);
        max_we[0] = reduceMax(max_we[0], c);
      }

      for(String w: w2) {
        double[] c = _cache.get(w);
        min_we[1] = reduceMin(min_we[1], c);
        max_we[1] = reduceMax(max_we[1], c);
      }


      // write out the abs diff in the min/max vecs
      for(int i=0;i<min_we[0].length;i++)
        ncs[ncs_idx++].addNum(Math.abs(min_we[0][i]-min_we[1][i]));
      for(int i=0;i<max_we[0].length;i++)
        ncs[ncs_idx++].addNum(Math.abs(max_we[0][i]-max_we[1][i]));

      if (!_test) ncs[ncs_idx].addNum(cs[cs.length - 1].at8(r));
    }
  }

  public static void main(String[] args) {
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
