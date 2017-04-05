package quora;


import DiffLib.FuzzyCmp;
import water.Iced;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.NewChunk;
import water.parser.BufferedString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import static quora.Utils.STOP_WORDS;
import static quora.Utils.fillEmVecs;

public class PreprocessorTask extends MRTask<PreprocessorTask> {

  private final int Q1;
  private final int Q2;
  private final boolean _test;
  transient Feature[] _features;

  PreprocessorTask(Feature[] features, boolean train) {
    _features=features;
    _test=!train;
    Q1=_test?1:3;
    Q2=_test?2:4;
  }

  String[] getNames() {
    ArrayList<String> names = new ArrayList<>();
    if( _test ) names.add("id");
    for (Feature f : _features) {
      if (f._name.startsWith("DUMMY")) continue;
      names.add(f._name);
    }
    if(!_test) names.add("is_duplicate");
    return names.toArray(new String[names.size()]);
  }

  @Override public void setupLocal() {
    if( _features==null ) {
      _features = FeatureCompute.computeFeatures();
    }
  }
  @Override public void map(Chunk[] cs, NewChunk[] ncs) {
    // some re-usables
    BufferedString bstr = new BufferedString();
    FuzzyCmp fc = new FuzzyCmp();
    double[] raw = new double[300];
    double[] we_s1 = new double[300]; // word embeddings vector
    double[] we_ss1= new double[300]; // sum squared of word embeddings

    double[] we_s2 = new double[300]; // word embeddings vector
    double[] we_ss2= new double[300]; // sum squared of word embeddings
    // loop over each row
    for(int r=0;r<cs[0]._len;++r) {
      String s1=null, s2=null, f1=null, f2=null;
      String[] w1=null,w2=null,fw1=null,fw2=null;
      try {
        int ncs_idx=0;
        if( _test ) ncs[ncs_idx++].addNum(cs[0].at8(r));

        s1 = cs[Q1].isNA(r)?"":cs[Q1].atStr(bstr, r).toString();
        s2 = cs[Q2].isNA(r)?"":cs[Q2].atStr(bstr, r).toString();

        // drop question marks
        s1 = s1.replaceAll("(?!')\\p{P}", ""); // remove punc except single apostrophe
        s2 = s2.replaceAll("(?!')\\p{P}", "");
        w1 = s1.split(" ");
        w2 = s2.split(" ");


        // now remove stop words and tolower
        // remove stop words
        fw1 = filterStopsAndLower(w1,STOP_WORDS);
        fw2 = filterStopsAndLower(w2,STOP_WORDS);
        f1 = Utils.join(fw1);
        f2 = Utils.join(fw2);

        for (Feature f : _features) {
          switch (f._name) {
            case "DUMMY_COMPUTE_FC_W":
              fc._s1=s1; fc._s2=s2;
              fc.comptue(w1, w2);
              continue;
            case "DUMMY_COMPUTE_FC_F":
              fc._s1 =f1; fc._s2=f2;
              fc.comptue(fw1, fw2);
              continue;
            case "DUMMY_COMPUTE_EM_W":
              fillEmVecs(w1, f._em, raw, we_s1, we_ss1);
              fillEmVecs(w2, f._em, raw, we_s2, we_ss2);
              continue;
            case "DUMMY_COMPUTE_EM_F":
              fillEmVecs(fw1, f._em, raw, we_s1, we_ss1);
              fillEmVecs(fw2, f._em, raw, we_s2, we_ss2);
              continue;
          }
          if( f._weop==null )
            ncs[ncs_idx++].addNum(f._op.op(s1, s2, w1, w2, f1, f2, fw1, fw2, fc));
          else
            ncs[ncs_idx++].addNum(f._weop.weop(w1,w2,fw1,fw2,we_s1,we_ss1,we_s2,we_ss2,raw,f._em));
        }
        if (!_test) ncs[ncs_idx].addNum(cs[cs.length - 1].at8(r));
      } catch( Exception e) {
        System.out.println("q1= " + s1);
        System.out.println("q2= " + s2);
        System.out.println("w1= " + Arrays.toString(w1));
        System.out.println("w2= " + Arrays.toString(w2));
        System.out.println("f1= " + Arrays.toString(fw1));
        System.out.println("f2= " + Arrays.toString(fw2));
        System.out.println("sf1= " + f1);
        System.out.println("sf2= " + f2);
        throw new RuntimeException(e);
      }
    }
  }

  // filter out stop words
  static String[] filterStopsAndLower(String[] w, Set<String> stops) {
    ArrayList<String> words = new ArrayList<>();
    for(int i=1;i<w.length-1;++i) {
      String wi = w[i].toLowerCase();
      if( stops.contains(wi) ) continue;
      words.add(wi);
    }
    return words.toArray(new String[words.size()]);
  }


  public static class Feature extends Iced {
    public String _name;
    public feature_op _op;
    public we_op _weop;
    WordEmbeddings.WORD_EM _em;
    public Feature(String name, feature_op op) { _name=name; _op=op; _weop=null; }
    public Feature(String name, WordEmbeddings.WORD_EM em, we_op weop) { _name=name; _op=null; _weop=weop; _em=em; }
    /**
     * lambda for computing different features
     */
    interface feature_op {
      double op(String s1, String s2, String[] w1, String[] w2, String f1, String f2, String[] fw1, String[] fw2,FuzzyCmp fc);
    }

    interface we_op {
      double weop(String[] w1, String[] w2, String[] fw1, String[] fw2, double[] ws1, double[] wss1,
                  double[] ws2, double[] wss2, double[] rawEm, WordEmbeddings.WORD_EM em);
    }
  }
}
