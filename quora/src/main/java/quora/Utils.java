package quora;

import embeddings.WordEmbeddings;
import info.debatty.java.stringsimilarity.*;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.util.FastMath;
import water.MRTask;
import water.fvec.Chunk;
import water.util.ArrayUtils;
import water.util.IcedInt;

import java.util.*;

public class Utils {

  public static final transient HashMap<String,String[]> ACRONYMS = new HashMap<>();
  public static final transient HashMap<String,String[]> CONTRACTIONS = new HashMap<>();

  public static final transient HashSet<String> STOP_WORDS = new HashSet<>();
  public static final String[] STOP_WORDS_LIST = new String[]{
    "a","about","above","across","after","afterwards","again","against","all","almost","alone","along","already","also",
    "although","always","am","among","amongst","amoungst","amount","an","and","another","any","anyhow","anyone","anything",
    "anyway","anywhere","are","around","as","at","back","be","became","because","become","becomes","becoming","been",
    "before","beforehand","behind","being","below","beside","besides","between","beyond","bill","both","bottom","but",
    "by","call","can","cannot","cant","co","computer","con","could","couldnt","cry","de","describe","detail","do",
    "done","down","due","during","each","eg","eight","either","eleven","else","elsewhere","empty","enough","etc",
    "even","ever","every","everyone","everything","everywhere","except","few","fifteen","fify","fill","find","fire",
    "first","five","for","former","formerly","forty","found","four","from","front","full","further","get","give","go",
    "had","has","hasnt","have","he","hence","her","here","hereafter","hereby","herein","hereupon","hers","herse",
    "him","himse","his","how","however","hundred","i","ie","if","in","inc","indeed","interest","into","is","it","its","itse",
    "keep","last","latter","latterly","least","less","ltd","made","many","may","me","meanwhile","might","mill","mine","more",
    "moreover","most","mostly","move","much","must","my","myse","name","namely","neither","never","nevertheless","next","nine",
    "no","nobody","none","noone","nor","not","nothing","now","nowhere","of","off","often","on","once","one","only","onto",
    "or","other","others","otherwise","our","ours","ourselves","out","over","own","part","per","perhaps","please","put",
    "rather","re","same","see","seem","seemed","seeming","seems","serious","several","she","should","show","side","since",
    "sincere","six","sixty","so","some","somehow","someone","something","sometime","sometimes","somewhere","still","such",
    "system","take","ten","than","that","the","their","them","themselves","then","thence","there","thereafter","thereby",
    "therefore","therein","thereupon","these","they","thick","thin","third","this","those","though","three","through",
    "throughout","thru","thus","to","together","too","top","toward","towards","twelve","twenty","two","un","under","until",
    "up","upon","us","very","via","was","we","well","were","what","whatever","when","whence","whenever","where","whereafter",
    "whereas","whereby","wherein","whereupon","wherever","whether","which","while","whither","who","whoever","whole","whom",
    "whose","why","will","with","within","without","would","yet","you","your","yours","yourself","yourselves"
  };


  // get the class distributions
  static class Sum extends MRTask<Sum> {
    long _sum0;
    long _sum1;
    @Override public void map(Chunk[] cs) {
      long sum0=0;
      long sum1=0;
      for(int r=0;r<cs[0]._len;++r) {
        long v = cs[3].at8(r);
        if( v!=0 && v!=1 ) {
          System.out.println();
        }
        if( v==0 ) sum0++;
        else sum1++;
      }
      _sum0=sum0;
      _sum1=sum1;
    }
    @Override public void reduce(Sum t) { _sum0 += t._sum0; _sum1 += t._sum1; }
  }

  public static String[] splitSmart(String s) {
    ArrayList<String> strs = new ArrayList<>();
    int x=0;
    int i=0;
    for(;i<s.length();++i) {
      if( s.charAt(i)==' ' && x<i) {
        String sub = s.substring(x,i);
        String[] ac, contraction;
        if( (ac=ACRONYMS.get(sub))!=null )
          Collections.addAll(strs,ac);
        else if( (contraction=CONTRACTIONS.get(sub))!=null )
          Collections.addAll(strs, contraction);
        else
          strs.add(sub);
        x=i+1;
      }
    }
    if( x<i )
      strs.add(s.substring(x,i));
    return strs.toArray(new String[strs.size()]);
  }

  public static String join(String[] words) {
    boolean first=true;
    StringBuilder sb= new StringBuilder();
    for (String word : words) {
      if (first) first = false;
      else sb.append(" ");
      sb.append(word);
    }
    return sb.toString();
  }

  public static double[] reduceMin(double[] a, double[] b) {
    for (int i=0; i<a.length; ++i)
      a[i] = Math.min(a[i], b[i]);
    return a;
  }
  public static double[] reduceMax(double[] a, double[] b) {
    for (int i=0; i<a.length; ++i)
      a[i] = Math.max(a[i], b[i]);
    return a;
  }

  // http://www.catalysoft.com/articles/StrikeAMatch.html
  public static class StrikeAMatch {
    private static HashMap<String, IcedInt> wordLetterPairs(String[] words) {
      HashMap<String, IcedInt> allPairs = new HashMap<>();
      for (String word : words) {
        for(int i=0;i<word.length()-1;i++) {
          String pair = word.substring(i,i+2);
          IcedInt v = allPairs.get(pair);
          if( v==null ) allPairs.put(pair, v=new IcedInt(0));
          v._val++;
        }
      }
      return allPairs;
    }

    public static double compareStrings(String[] w1, String[] w2) {
      HashMap<String,IcedInt> pairs1 = wordLetterPairs(w1);
      HashMap<String,IcedInt> pairs2 = wordLetterPairs(w2);
      int union = pairs1.size() + pairs2.size();

      int intersection = 0;
      for(String p1: pairs1.keySet()) {
        if( pairs2.containsKey(p1) ) {
          intersection++;
          IcedInt v = pairs2.get(p1);
          v._val--;
          if( v._val<= 0 ) pairs2.remove(p1);
        }
      }
      return (2.0 * intersection) / union;
    }
  }

  static int countCommon(String[] q1, String[] q2) {
    HashSet<String> s1 = new HashSet<>();
    Collections.addAll(s1,q1);
    int cnt=0;
    for(String s: q2)
      if( s1.contains(s) ) cnt++;
    return cnt;
  }

  static int countCommonEmbeddings(String[] q1, String[] q2, int n, WordEmbeddings.EMBEDDINGS em) {
    HashSet<String> s1 = new HashSet<>();
    Collections.addAll(s1,q1);
    for(String q: q1) {
      WordEmbeddings.SimilarWord[] simwords = em.mostSimilar(q,n);
      for(WordEmbeddings.SimilarWord sw: simwords) s1.add(sw.word());
    }
    int cnt=0;
    for(String s:q2) {
      if( s1.contains(s) ) cnt++;
      WordEmbeddings.SimilarWord[] simwords = em.mostSimilar(s,n);
      for(WordEmbeddings.SimilarWord sw: simwords) {
        if( s1.contains(sw.word()) ) cnt++;
      }
    }
    return cnt;
  }


  static double countCommonRatio(String[] q1, String[] q2) {
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

  public static double canberra_distance(double[] a, double[] b) {
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      final double num = FastMath.abs(a[i] - b[i]);
      final double denom = FastMath.abs(a[i]) + FastMath.abs(b[i]);
      sum += num == 0.0 && denom == 0.0 ? 0.0 : num / denom;
    }
    return sum;
  }

  public static double earth_movers_distance(double[] a, double[] b) {
    double lastDistance = 0.0D;
    double totalDistance = 0.0D;

    for(int i = 0; i < a.length; ++i) {
      double currentDistance = a[i] + lastDistance - b[i];
      totalDistance += FastMath.abs(currentDistance);
      lastDistance = currentDistance;
    }
    return totalDistance;
  }


  public static float canberra_distance(float[] a, float[] b) {
    float sum = 0;
    for (int i = 0; i < a.length; i++) {
      final float num = FastMath.abs(a[i] - b[i]);
      final float denom = FastMath.abs(a[i]) + FastMath.abs(b[i]);
      sum += num == 0.0 && denom == 0.0 ? 0.0 : num / denom;
    }
    return sum;
  }

  public static float earth_movers_distance(float[] a, float[] b) {
    float lastDistance = 0;
    float totalDistance = 0;

    for(int i = 0; i < a.length; ++i) {
      float currentDistance = a[i] + lastDistance - b[i];
      totalDistance += FastMath.abs(currentDistance);
      lastDistance = currentDistance;
    }
    return totalDistance;
  }

  static boolean allzero(float[] d) {
    for(double dd: d) if(dd!=0) return false;
    return true;
  }

  public static double wmd(String[] w1, String[] w2, float[] d, WordEmbeddings.EMBEDDINGS em) {
    Arrays.fill(d,0);
    HashMap<String, float[]> embeddings = new HashMap<>();
    int len_t1=0, len_t2=0;
    for(String w: w1) {
      em.get(w,d);
      if( allzero(d) ) continue;
      embeddings.put(w,d);
      len_t1++;
    }
    for(String w: w2) {
      if( embeddings.get(w)!=null ) len_t2++;
      else {
        em.get(w,d);
        if( allzero(d) ) continue;
        embeddings.put(w,d);
        len_t2++;
      }
    }
    if( len_t1==0 || len_t2==0 ) return Double.POSITIVE_INFINITY;

    HashSet<String> docset1 = new HashSet<>();
    HashSet<String> docset2 = new HashSet<>();
    HashMap<String,IcedInt> dict = new HashMap<>();
    String[] doc1 = new String[len_t1];
    String[] doc2 = new String[len_t2];
    int c=0;
    for(String w: w1) {
      if( embeddings.get(w)!=null ) {
        docset1.add(w);
        doc1[c++]=w;
        IcedInt v = dict.get(w);
        if( v==null ) dict.put(w, v=new IcedInt(0));
        v._val++;
      }
    }
    c=0;
    for(String w: w2) {
      if( embeddings.get(w)!=null ) {
        docset2.add(w);
        doc2[c++]=w;
        IcedInt v = dict.get(w);
        if( v==null ) dict.put(w, v=new IcedInt(0));
        v._val++;
      }
    }

    if( dict.size()==1 ) return 0;

    double[][] distMat = new double[dict.size()][dict.size()];
    int i=-1,j;
    double sum=0;
    for(String t1: dict.keySet()) {
      i++;
      j=-1;
      for(String t2: dict.keySet()) {
        j++;
        if( !docset1.contains(t1) || !docset2.contains(t2) ) continue;
        distMat[i][j] = l2norm2(embeddings.get(t1),embeddings.get(t2));
        sum += distMat[i][j];
      }
    }
    if( sum==0 ) return Double.POSITIVE_INFINITY;

    return JEMD.emdHat(doc2bow(doc1,dict), doc2bow(doc2,dict),distMat,-1);
  }

  public static double l2norm2(float[] x, float[] y) {  // Computes \sum_{i=1}^n (x_i - y_i)^2
    assert x.length == y.length;
    double sse = 0;
    for(int i = 0; i < x.length; i++) {
      double diff = x[i] - y[i];
      sse += diff * diff;
    }
    return sse;
  }

  public static double[] doc2bow(String[] w, HashMap<String,IcedInt> dict) {
    HashMap<String,IcedInt> counter = new HashMap<>();
    for(String s: w) {
      IcedInt v = counter.get(s);
      if( v==null ) counter.put(s, v=new IcedInt(0));
      v._val++;
    }

    double[] res = new double[dict.size()];
    int i=0;
    for(String v: dict.keySet()) {
      IcedInt ii = counter.get(v);
      if( ii==null ) continue;
      res[i++] = (double)ii._val / (double)w.length;
    }
    return res;
  }

  public static double qratio(String a, String b) {return ratio(a,b);}

  // https://github.com/seatgeek/fuzzywuzzy/blob/master/fuzzywuzzy/fuzz.py#L238-L308
  public static double wratio(String a, String b) {
    boolean try_partial;
    double unbase_scale = .95;
    double partial_scale = .90;
    double base = qratio(a,b);
    double len_ratio = (Math.max(a.length(), b.length())) / Math.min(a.length(), b.length());

    try_partial = len_ratio > 1.5;
    if( len_ratio > 8 ) partial_scale = 0.6;

    String[] toks1 = a.split(" ");
    String[] toks2 = a.split(" ");

    if( try_partial ) {
      double partial = partial_ratio(a,b) * partial_scale;
      double ptsor   = token_sort_ratio(toks1,toks2,true) * unbase_scale * partial_scale;
      double ptser   = token_set_ratio (toks1,toks2,true) * unbase_scale * partial_scale;
      return maximum(partial,ptsor,ptser,base);
    }
    double tsor = token_sort_ratio(toks1,toks2,false) * unbase_scale;
    double tser = token_set_ratio (toks1,toks2,false) * unbase_scale;
    return maximum(base,tsor,tser);
  }

  public static double maximum(double... d) {
    return ArrayUtils.maxValue(d);
  }

  public static double ratio(String s1, String s2) {
    return 100*DiffLib.Levenshtein.ratio(s1,s2);
  }

  // https://github.com/seatgeek/fuzzywuzzy/blob/master/fuzzywuzzy/fuzz.py#L56-L90
  public static double partial_ratio(String s1, String s2) {
    try {
      String shorter,longer;
      if (s1.length() <= s2.length()) {
        shorter = s1;
        longer = s2;
      } else {
        shorter=s2;
        longer=s1;
      }
      int[][] blocks = DiffLib.Levenshtein.matching_groups(shorter, longer);
      double maxScore = -Double.MAX_VALUE;
      int longer_len=longer.length();
      for (int[] block : blocks) {
        int diff = block[1] - block[0];
        int long_s = diff > 0 ? diff : 0;
        int long_e = long_s + shorter.length();
        String long_sub = longer.substring(long_s, long_e>longer_len?longer_len:long_e);
        double r = DiffLib.Levenshtein.ratio(shorter, long_sub);
        if (r > 0.995) return 100;
        maxScore = Math.max(maxScore, r);
      }
      return 100 * maxScore;
    } catch (Exception e) {
      System.err.println("s1= " + s1 + "; s2=" + s2);
      throw new RuntimeException(e);
    }
  }

  public static double token_sort_ratio(String[] w1, String[] w2, boolean partial) {
    Arrays.sort(w1); Arrays.sort(w2);
    String s1 = join(w1);
    String s2 = join(w2);
    return partial?partial_ratio(s1,s2):ratio(s1,s2);
  }

  public static double token_set_ratio(String[] w1, String[] w2, boolean partial) {
    HashSet<String> t1 = new HashSet<>(); Collections.addAll(t1,w1);
    HashSet<String> t2 = new HashSet<>(); Collections.addAll(t2,w2);

    HashSet<String> sect         = new HashSet<>();
    HashSet<String> diff1to2     = new HashSet<>(); // t1 - t2
    HashSet<String> diff2to1     = new HashSet<>(); // t2 - t1
    for(String s: t1) if( t2.contains(s) ) sect    .add(s);
    for(String s: t1) if( !t2.contains(s)) diff1to2.add(s);
    for(String s: t2) if( !t1.contains(s)) diff2to1.add(s);

    String sorted_sect  = sortJoin(sect);
    String sorted_diff12= sortJoin(diff1to2);
    String sorted_diff21= sortJoin(diff2to1);

    String combined12 = (sorted_sect + " " + sorted_diff12).trim();
    String combined21 = (sorted_sect + " " + sorted_diff21).trim();
    sorted_sect = sorted_sect.trim();

    double r = -Double.MAX_VALUE;
    if( partial ) {
      r = Math.max(r, partial_ratio(sorted_sect, combined12));
      r = Math.max(r, partial_ratio(sorted_sect, combined21));
      r = Math.max(r, partial_ratio(combined12,  combined21));
      return r;
    }

    r = Math.max(r, ratio(sorted_sect, combined12));
    r = Math.max(r, ratio(sorted_sect, combined21));
    return Math.max(r, ratio(combined12,  combined21));
  }

  public static String sortJoin(HashSet<String> strs) {
    String[] toks = strs.toArray(new String[strs.size()]);
    Arrays.sort(toks);
    return join(toks);
  }

  public static int countChars(String[] f) {
    int c=0;
    for( String ff: f) c += ff.length();
    return c;
  }

  public static void fillEmVecs(String[] words, WordEmbeddings.EMBEDDINGS em, float[] f, double[] ws, double[] wss) {
    for(int i=0;i<ws.length;++i) ws[i]=wss[i]=0;
    for(String s: words ) {
      em.get(s,f);
      if( f==null ) continue;
      add(ws, f);
      addSq(wss, f);
    }
    norm(ws);
    norm(sqrt(wss));
  }

  static void norm(double[] a) {
    float ss=0;
    for (double aa : a) ss += aa * aa;
    for(int i=0;i<a.length;++i)
      a[i]=ss==0?0f:a[i]/(double)Math.sqrt(ss);
  }

  static double[] sqrt(double[] a) {
    for(int i=0;i<a.length;++i) {
      double aa = Double.isNaN(a[i])?0:a[i];
      a[i] = Math.sqrt(aa);
    }
    return a;
  }

  static void add(double[] a, float[] f) {
    if( f==null ) return;
    for(int i=0;i<f.length;++i) {
      double aa = Double.isNaN(a[i])?0:a[i];
      double ff = Double.isNaN(f[i])?0:f[i];
      a[i] = aa + ff;
    }
  }

  static void addSq(double[] a, float[] f) {
    if( f==null ) return;
    for(int i=0;i<f.length;++i) {
      double aa = Double.isNaN(a[i])?0:a[i];
      double ff = Double.isNaN(f[i])?0:f[i];
      a[i] = aa + ff*ff;
    }
  }

  static double cosine_distance(double[] a, double[] b) {
    double sum_ab = 0;
    double sum_a2 = 0;
    double sum_b2 = 0;
    for(int i=0;i<a.length;++i) {
      double aa = Double.isNaN(a[i])?0:a[i];
      double bb = Double.isNaN(b[i])?0:b[i];
      sum_ab += aa*bb;
      sum_a2 += aa*aa;
      sum_b2 += bb*bb;
    }
    double sim = sum_ab / (Math.sqrt(sum_a2) * Math.sqrt(sum_b2) );
    return 1-sim;
  }

  static double minkowski_distance(double[] a, double[] b, int p) {
    double sum=0;
    for(int i=0;i<a.length;++i) {
      double aa = Double.isNaN(a[i])?0:a[i];
      double bb = Double.isNaN(b[i])?0:b[i];
      sum += Math.pow(Math.abs(aa-bb), p);
    }
    return Math.pow(sum, 1./p);
  }

  static double skew(double[] a) {return new Skewness().evaluate(a,0,a.length);}
  static double kurt(double[] a) {return new Kurtosis().evaluate(a,0,a.length);}
  public static double cosine(String s1, String s2) {return _cos.distance(s1,s2);}
  public static double demaru(String s1, String s2) {return _demaru.distance(s1,s2);}
  public static double jaccard(String s1, String s2) {return _jac.distance(s1,s2);}
  public static double jaroWinkler(String s1, String s2) {return _jwink.distance(s1,s2);}
  public static double levenshtein(String s1, String s2) {return DiffLib.Levenshtein.lev_dist(s1,s2);}
  public static double longestCommonSubsequence(String s1, String s2) {return _lcsub.distance(s1,s2);}
  public static double ngram(String s1, String s2) {return _ngram.distance(s1,s2);}
  public static double normalizedLevenshtein(String s1, String s2) {return _normLeven.distance(s1,s2);}
  public static double optimalStringAlignment(String s1, String s2) {return _optimStringAlign.distance(s1,s2);}
  public static double qgram(String s1, String s2) {return _qgram.distance(s1,s2);}
  public static double sorensenDice(String s1, String s2) {return _sdice.distance(s1,s2);}

  private static final transient Cosine _cos;
  private static final transient Damerau _demaru;
  private static final transient Jaccard _jac;
  private static final transient JaroWinkler _jwink;
  private static final transient Levenshtein _leven;
  private static final transient LongestCommonSubsequence _lcsub;
  private static final transient NGram _ngram;
  private static final transient NormalizedLevenshtein _normLeven;
  private static final transient OptimalStringAlignment _optimStringAlign;
  private static final transient QGram _qgram;
  private static final transient SorensenDice _sdice;

  static {
    Collections.addAll(STOP_WORDS,STOP_WORDS_LIST);
    _cos = new Cosine();
    _demaru = new Damerau();
    _jac = new Jaccard();
    _jwink = new JaroWinkler();
    _leven = new Levenshtein();
    _lcsub =  new LongestCommonSubsequence();
    _ngram = new NGram();
    _normLeven = new NormalizedLevenshtein();
    _optimStringAlign = new OptimalStringAlignment();
    _qgram = new QGram();
    _sdice = new SorensenDice();

    ACRONYMS.put("UPSC",new String[]{"Union","Public","Service","Commission"});
    ACRONYMS.put("AOA", new String[]{"Angle","of","Attack"});
    ACRONYMS.put("CBSE",new String[]{"Central","Board","of","Secondary","Education"});
    ACRONYMS.put("OITNB", new String[]{"Orange","is","the","New","Black"});
    ACRONYMS.put("CTC", new String[]{"Cost","to","Company"});
    ACRONYMS.put("dms", new String[]{"private","messages"});

    CONTRACTIONS.put("aren't", new String[]{"are", "not"});
    CONTRACTIONS.put("can't", new String[]{"cannot"});
    CONTRACTIONS.put("couldn't", new String[]{"could", "not"});
    CONTRACTIONS.put("didn't", new String[]{"did", "not"});
    CONTRACTIONS.put("doesn't", new String[]{"does", "not"});
    CONTRACTIONS.put("don't", new String[]{"do", "not"});
    CONTRACTIONS.put("hadn't", new String[]{"had", "not"});
    CONTRACTIONS.put("hasn't", new String[]{"has", "not"});
    CONTRACTIONS.put("haven't", new String[]{"have", "not"});
    CONTRACTIONS.put("he'd", new String[]{"he", "had"});
    CONTRACTIONS.put("he'll", new String[]{"he", "will"});
    CONTRACTIONS.put("he's", new String[]{"he", "is"});
    CONTRACTIONS.put("I'd", new String[]{"I", "had"});
    CONTRACTIONS.put("I'll", new String[]{"I", "will"});
    CONTRACTIONS.put("I'm", new String[]{"I", "am"});
    CONTRACTIONS.put("I've", new String[]{"I", "have"});
    CONTRACTIONS.put("isn't", new String[]{"is", "not"});
    CONTRACTIONS.put("let's", new String[]{"let", "us"});
    CONTRACTIONS.put("mightn't", new String[]{"might", "not"});
    CONTRACTIONS.put("mustn't", new String[]{"must", "not"});
    CONTRACTIONS.put("shan't", new String[]{"shall", "not"});
    CONTRACTIONS.put("she'd", new String[]{"she", "had"});
    CONTRACTIONS.put("she'll", new String[]{"she", "will"});
    CONTRACTIONS.put("she's", new String[]{"she", "is"});
    CONTRACTIONS.put("shouldn't", new String[]{"should", "not"});
    CONTRACTIONS.put("that's", new String[]{"that", "is"});
    CONTRACTIONS.put("there's", new String[]{"there", "is"});
    CONTRACTIONS.put("they'd", new String[]{"they", "had"});
    CONTRACTIONS.put("they'll", new String[]{"they", "will"});
    CONTRACTIONS.put("they're", new String[]{"they", "are"});
    CONTRACTIONS.put("they've", new String[]{"they", "have"});
    CONTRACTIONS.put("we'd", new String[]{"we", "had"});
    CONTRACTIONS.put("we're", new String[]{"we", "are"});
    CONTRACTIONS.put("we've", new String[]{"we", "have"});
    CONTRACTIONS.put("weren't", new String[]{"were", "not"});
    CONTRACTIONS.put("what'll", new String[]{"what", "will"});
    CONTRACTIONS.put("what're", new String[]{"what", "are"});
    CONTRACTIONS.put("what's", new String[]{"what", "is"});
    CONTRACTIONS.put("what've", new String[]{"what", "have"});
    CONTRACTIONS.put("where's", new String[]{"where", "is"});
    CONTRACTIONS.put("who's", new String[]{"who", "would"});
    CONTRACTIONS.put("who'll", new String[]{"who", "will"});
    CONTRACTIONS.put("who're", new String[]{"who", "are"});
    CONTRACTIONS.put("who's", new String[]{"who", "is"});
    CONTRACTIONS.put("who've", new String[]{"who", "have"});
    CONTRACTIONS.put("won't", new String[]{"will", "not"});
    CONTRACTIONS.put("wouldn't", new String[]{"would", "not"});
    CONTRACTIONS.put("you'd", new String[]{"you", "had"});
    CONTRACTIONS.put("you'll", new String[]{"you", "will"});
    CONTRACTIONS.put("you're", new String[]{"you", "are"});
    CONTRACTIONS.put("you've", new String[]{"you", "have"});
  }

  public static void main(String[] args) {
    String s1 = "air force 20 2017 aboard jan obamas plan sworn trump will";
    String s2 = "air force courtesy flight inauguration newlyformer president";
    partial_ratio(s1,s2);
  }
}
