package quora;


import DiffLib.FuzzyCmp;
import info.debatty.java.stringsimilarity.*;
import info.debatty.java.stringsimilarity.interfaces.StringDistance;
import water.*;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.NewChunk;
import water.fvec.Vec;
import water.parser.BufferedString;

import java.util.*;

import static quora.Utils.STOP_WORDS;
import static water.KaggleUtils.importParseFrame;

public class Preprocess2 extends MRTask<Preprocess2> {

  private static final int NDIST_METRICS=11;
  private static final int ID=0;
  private final int Q1;
  private final int Q2;
  private final boolean _test;

  transient StringDistance[] _simMetrics;
//  private WordEmbeddingsReader _em;

  static final String[] NAMES = new String[]{
    "id",  // the row id
    // no stop words removed, no tolowercase (just punctuation removed)
    "common_words",
    "common_rat",
    "strike_match",
    "zrat",
    // distance metrics
    "cosine","dameru","jaccard","jwink","leven","lcsub","ngram","leven_norm","optim_align","qgram","sdice",
    "qrat", "wrat", "partial_rat", "partial_set_rat", "partial_sort_rat", "set_rat","sort_rat",

    // now the "fuzzed" versions (stop words removed, tolower'd)
    "common_words2",
    "common_rat2",
    "strike_match2",
//     distance metrics
    "cosine2","dameru2","jaccard2","jwink2","leven2","lcsub2","ngram2","leven_norm2","optim_align2","qgram2","sdice2",
    "qrat2", "wrat2", "partial_rat2", "partial_set_rat2", "partial_sort_rat2", "set_rat2","sort_rat2",
  };

  Preprocess2(boolean test) {
    _test=test;
    Q1=_test?1:3;
    Q2=_test?2:4;
//    _em = new WordEmbeddingsReader();
  }

  @Override public void setupLocal() {
//    _em.read2("./lib/w2vec_models/gw2vec",300);
//    System.out.println(_em._cache.size() + " vecs loaded");

    // init all the string distance measures one time here
    _simMetrics = new StringDistance[NDIST_METRICS];
    _simMetrics[0] = new Cosine();
    _simMetrics[1] = new Damerau();
    _simMetrics[2] = new Jaccard();
    _simMetrics[3] = new JaroWinkler();
    _simMetrics[4] = new Levenshtein();
    _simMetrics[5] = new LongestCommonSubsequence();
    _simMetrics[6] = new NGram();
    _simMetrics[7] = new NormalizedLevenshtein();
    _simMetrics[8] = new OptimalStringAlignment();
    _simMetrics[9] = new QGram();
    _simMetrics[10]= new SorensenDice();
    System.out.println("Distance metrics initialized");
    System.out.print("Node local init done");
  }

  @Override public void map(Chunk[] cs, NewChunk[] ncs) {
    BufferedString bstr = new BufferedString();
    FuzzyCmp fc = new FuzzyCmp();
    double[] dists = new double[NDIST_METRICS];  // on fuzzed words (no punc, lower case, no stops)
//    float[] we_s1 = new float[300]; // word embeddings vector
//    float[] we_ss1= new float[300]; // sum squared of word embeddings
//
//    float[] we_s2 = new float[300]; // word embeddings vector
//    float[] we_ss2= new float[300]; // sum squared of word embeddings
    for(int r=0;r<cs[0]._len;++r) {
      String q1=null;
      String q2=null;
      String[] w1=null;
      String[] w2=null;
      String[] f1=null;
      String[] f2=null;
      String sf1=null;
      String sf2=null;
      try {
        int ncs_idx=0;
        int id = (int) cs[ID].at8(r);
        ncs[ncs_idx++].addNum(id);

        q1 = cs[Q1].isNA(r)?"":cs[Q1].atStr(bstr, r).toString();
        q2 = cs[Q2].isNA(r)?"":cs[Q2].atStr(bstr, r).toString();

        // drop question marks
        q1 = q1.replaceAll("(?!')\\p{P}", ""); // remove punc except single apostrophe
        q2 = q2.replaceAll("(?!')\\p{P}", "");
        w1 = q1.split(" ");
        w2 = q2.split(" ");

        // non-fuzzy features
        int common_words = countCommon(w1,w2);
        double common_rat = countCommonRatio(w1,w2);
        double strike_match = Utils.StrikeAMatch.compareStrings(w1,w2);
        double zrat = DiffLib.Levenshtein.ratio(q1,q2);
        getDistances(q1,q2,dists);
        fc._s1=q1; fc._s2=q2;
        fc.comptue(w1,w2);
        ncs[ncs_idx++].addNum(common_words);
        ncs[ncs_idx++].addNum(common_rat);
        ncs[ncs_idx++].addNum(strike_match);
        for( double dist : dists ) ncs[ncs_idx++].addNum(dist);
        ncs[ncs_idx++].addNum(fc._qratio);
        ncs[ncs_idx++].addNum(fc._wratio);
        ncs[ncs_idx++].addNum(fc._partialRatio);
        ncs[ncs_idx++].addNum(fc._partialTokenSetRatio);
        ncs[ncs_idx++].addNum(fc._partialTokenSortRatio);
        ncs[ncs_idx++].addNum(fc._tokenSetRatio);
        ncs[ncs_idx++].addNum(fc._tokenSortRatio);


        // now remove stop words and tolower
        // remove stop words
        f1 = toStringA(w1,STOP_WORDS);
        f2 = toStringA(w2,STOP_WORDS);
        sf1 = Utils.join(f1);
        sf2 = Utils.join(f2);

        common_words = countCommon(w1,w2);
        common_rat = countCommonRatio(w1,w2);
        strike_match = Utils.StrikeAMatch.compareStrings(w1,w2);
        getDistances(sf1,sf2,dists);
        fc._s1=sf1; fc._s2=sf2;
        fc.comptue(f1,f2);
        ncs[ncs_idx++].addNum(common_words);
        ncs[ncs_idx++].addNum(common_rat);
        ncs[ncs_idx++].addNum(strike_match);
        ncs[ncs_idx++].addNum(zrat);
        for( double dist : dists ) ncs[ncs_idx++].addNum(dist);
        ncs[ncs_idx++].addNum(fc._qratio);
        ncs[ncs_idx++].addNum(fc._wratio);
        ncs[ncs_idx++].addNum(fc._partialRatio);
        ncs[ncs_idx++].addNum(fc._partialTokenSetRatio);
        ncs[ncs_idx++].addNum(fc._partialTokenSortRatio);
        ncs[ncs_idx++].addNum(fc._tokenSetRatio);
        ncs[ncs_idx++].addNum(fc._tokenSortRatio);
        if (!_test) ncs[ncs_idx].addNum(cs[cs.length - 1].at8(r));
      } catch (Exception e) {
        System.out.println("q1= " + q1);
        System.out.println("q2= " + q2);
        System.out.println("w1= " + Arrays.toString(w1));
        System.out.println("w2= " + Arrays.toString(w2));
        System.out.println("f1= " + Arrays.toString(f1));
        System.out.println("f2= " + Arrays.toString(f2));
        System.out.println("sf1= " + sf1);
        System.out.println("sf2= " + sf2);
        throw new RuntimeException(e);
      }
    }
  }

  static float cosine_distance(float[] a, float[] b) {
    float sum_ab = 0;
    float sum_a2 = 0;
    float sum_b2 = 0;
    for(int i=0;i<a.length;++i) {
      sum_ab += a[i]*b[i];
      sum_a2 += a[i]*a[i];
      sum_b2 += b[i]*b[i];
    }
    float sim = sum_ab / (float)(Math.sqrt(sum_a2) * Math.sqrt(sum_b2) );
    return 1-sim;
  }

  // single vec becomes a normalized sum...
//  void wordEmVecs(String[] words, float[] ws, float[] wss) {
//    for(int i=0;i<ws.length;++i) ws[i]=wss[i]=0;
//    for (String s: words) {
//      double[] f = _em._cache.get(s);
//      if( f==null ) continue;
//      add(ws, f);
//      addSq(wss, f);
//    }
//    norm(ws);
//    norm(sqrt(wss));
//  }

  double[] getDistances(String s1, String s2, double[] dists) {
    dists[0 /*cosine*/]      = _simMetrics[0].distance(s1,s2);
    dists[1 /*dameru*/]      = _simMetrics[1].distance(s1,s2);
    dists[2 /*jaccard*/]     = _simMetrics[2].distance(s1,s2);
    dists[3 /*jwink*/]       = _simMetrics[3].distance(s1,s2);
    dists[4 /*leven*/]       = DiffLib.Levenshtein.lev_dist(s1,s2);//_simMetrics[4].distance(s1,s2);
    dists[5 /*lcsub*/]       = _simMetrics[5].distance(s1,s2);
    dists[6 /*ngram*/]       = _simMetrics[6].distance(s1,s2);
    dists[7 /*leven_norm*/]  = _simMetrics[7].distance(s1,s2);
    dists[8 /*optim_align*/] = _simMetrics[8].distance(s1,s2);
    dists[9 /*qgram*/]       = _simMetrics[9].distance(s1,s2);
    dists[10 /*sdice*/]      = _simMetrics[10].distance(s1,s2);
    return dists;
  }

  // filter out stop words
  static String[] toStringA(String[] w, Set<String> stops) {
    ArrayList<String> words = new ArrayList<>();
    for(int i=1;i<w.length-1;++i) {
      String wi = w[i].toLowerCase();
      if( stops.contains(wi) ) continue;
      words.add(wi);
    }
    return words.toArray(new String[words.size()]);
  }

  int countCaps(String s) {
    int cnt=0;
    for(int i=0;i<s.length();++i ) {
      char c = s.charAt(i);
      if( 'A' <= c && c <= 'Z' ) cnt++;
    }
    return cnt;
  }

  int countPunc(String s) {
    int cnt=0;
    for(int i=0;i<s.length();++i) {
      char c = s.charAt(i);
      if( ('a' <= c && c <= 'z')          ||  // a lowercase ascii letter
        ('A' <= c && c <= 'Z')          ||  // a uppercase ascii letter
        ('0' <= c && c <= '9')          ||  // a digit
        (c==' ' || c=='\t' || c=='\n')      // a space, tab, or newline
        ) continue;
      cnt++;
    }
    return cnt;
  }

  int fuzzyChars(String[] f) {
    int c=0;
    for( String ff: f) c += ff.length();
    return c;
  }

  int countCommon(String[] q1, String[] q2) {
    HashSet<String> s1 = new HashSet<>();
    Collections.addAll(s1,q1);
    int cnt=0;
    for(String s: q2)
      if( s1.contains(s) ) cnt++;
    return cnt;
  }

  double countCommonRatio(String[] q1, String[] q2) {
    HashSet<String> s1 = new HashSet<>();
    Collections.addAll(s1,q1);
    HashSet<String> s2 = new HashSet<>();
    Collections.addAll(s2,q2);

    if( s1.size()==0 || s2.size()==0 ) return 0;
    int c1=0,c2=0;
    for(String s: s1) if( s2.contains(s) ) c1++;
    for(String s: s2) if( s1.contains(s) ) c2++;
    return (double)(c1+c2) / (double) (s1.size() + s2.size());
  }

  // count common capitalized words
  int countCapsCommon(String w1, String w2) {
    HashSet<String> caps1 = new HashSet<>();
    for(int i=0;i<w1.length();++i) {
      char c = w1.charAt(i);
      if( 'A' <= c && c <= 'Z' ) caps1.add(""+c);
    }
    int cnt=0;
    for(int i=0;i<w2.length();++i) {
      if( caps1.contains(""+w2.charAt(i)) ) cnt++;
    }
    return cnt;
  }

  public static void main(String[] args) {
    H2OApp.main(args);

    boolean train=true;
    int id=12;
    String outpath= train?"./data/train_feats"+id+".csv":"./data/test_feats"+id+".csv";
    String path = train?"./data/train_clean.csv":"./data/test_clean.csv";
    String name = train?"train":"test";
    String key= train?"train_feats":"test_feats";

    int nembeddings=0;//300;

    String[] names = Arrays.copyOf(Preprocess2.NAMES, Preprocess2.NAMES.length+nembeddings + ((train?1:0)));
    int n=Preprocess2.NAMES.length;
    for(int i=1;i<=nembeddings;++i) names[n++] = "em_"+i;
    if(train) names[names.length-1] = "is_duplicate";

    int nouts = names.length;
    byte[] types= train?new byte[]{Vec.T_NUM,Vec.T_NUM,Vec.T_NUM, Vec.T_STR, Vec.T_STR, Vec.T_NUM}:new byte[]{Vec.T_NUM,Vec.T_STR,Vec.T_STR};

    Frame fr = importParseFrame(path,name, types);
    long s = System.currentTimeMillis();
    Preprocess2 p = new Preprocess2(!train);
    Frame out = p.doAll(nouts, Vec.T_NUM, fr).outputFrame(Key.make(key),names,null);
    System.out.println("all done: " + (System.currentTimeMillis()-s)/1000. + " seconds");

    System.out.println("Writing frame ");
    Job job = Frame.export(out, outpath, out._key.toString(), false, 1);
    job.get();

    H2O.shutdown(0);
  }
}
