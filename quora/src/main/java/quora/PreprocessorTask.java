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

public class PreprocessorTask extends MRTask<PreprocessorTask> {

  private final int Q1;
  private final int Q2;
  private final boolean _test;
  transient Feature[] _features;
  transient WordEmbeddings _em;

  PreprocessorTask(Feature[] features, boolean test) {
    _features=features;
    _test=test;
    Q1=_test?1:3;
    Q2=_test?2:4;
  }

  String[] getNames() {
    String[] names = new String[_test? _features.length : _features.length+1];
    for(int i=0;i<_features.length;++i) names[i] = _features[i]._name;
    if( _test ) return names;
    names[names.length-1] = "is_duplicate";
    return names;
  }

  @Override public void setupLocal() {
    _em = WordEmbeddings._em;
    if( _features==null ) {
      _features = FeatureCompute.computeFeatures();
    }
  }
  @Override public void map(Chunk[] cs, NewChunk[] ncs) {
    // some re-usables
    BufferedString bstr = new BufferedString();
    FuzzyCmp fc = new FuzzyCmp();
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
          if( f._weop==null )
            ncs[ncs_idx++].addNum(f._op.op(s1, s2, w1, w2, f1, f2, fw1, fw2, fc));
          else
            ncs[ncs_idx++].addNum(f._weop.weop(w1,w2,fw1,fw2,we_s1,we_ss1,we_s2,we_ss2,_em));
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
    public Feature(String name, feature_op op) { _name=name; _op=op; _weop=null; }
    public Feature(String name, float f, we_op weop) { _name=name; _op=null; _weop=weop; }
    /**
     * lambda for computing different features
     */
    interface feature_op {
      double op(String s1, String s2, String[] w1, String[] w2, String f1, String f2, String[] fw1, String[] fw2,FuzzyCmp fc);
    }

    interface we_op {
      double weop(String[] w1, String[] w2, String[] fw1, String[] fw2, double[] ws1, double[] wss1,
                  double[] ws2, double[] wss2, WordEmbeddings em);
    }
  }
}
