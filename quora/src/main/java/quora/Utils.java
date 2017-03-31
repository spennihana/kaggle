package quora;

import org.apache.commons.math3.util.FastMath;
import water.MRTask;
import water.fvec.Chunk;
import water.util.ArrayUtils;
import water.util.IcedInt;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Utils {

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

  public static final String[] STOP_WORDS = new String[]{
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


  public static String contractionMap(String s) {
    if( s.equals("aren't") ) return "are not";
    if( s.equals("can't") ) return "cannot";
    if( s.equals("couldn't") ) return "could not";
    if( s.equals("didn't") ) return "did not";
    if( s.equals("doesn't") ) return "does not";
    if( s.equals("don't") ) return "do not";
    if( s.equals("hadn't") ) return "had not";
    if( s.equals("hasn't") ) return "has not";
    if( s.equals("haven't") ) return "have not";
    if( s.equals("he'd") ) return "he had";
    if( s.equals("he'll") ) return "he will";
    if( s.equals("he's") ) return "he is";
    if( s.equals("I'd") ) return "I had";
    if( s.equals("I'll") ) return "I will";
    if( s.equals("I'm") ) return "I am";
    if( s.equals("I've") ) return "I have";
    if( s.equals("isn't") ) return "is not";
    if( s.equals("let's") ) return "let us";
    if( s.equals("mightn't") ) return "might not";
    if( s.equals("mustn't") ) return "must not";
    if( s.equals("shan't") ) return "shall not";
    if( s.equals("she'd") ) return "she had";
    if( s.equals("she'll") ) return "she will";
    if( s.equals("she's") ) return "she is";
    if( s.equals("shouldn't") ) return "should not";
    if( s.equals("that's") ) return "that is";
    if( s.equals("there's") ) return "there is";
    if( s.equals("they'd") ) return "they had";
    if( s.equals("they'll") ) return "they will";
    if( s.equals("they're") ) return "they are";
    if( s.equals("they've") ) return "they have";
    if( s.equals("we'd") ) return "we had";
    if( s.equals("we're") ) return "we are";
    if( s.equals("we've") ) return "we have";
    if( s.equals("weren't") ) return "were not";
    if( s.equals("what'll") ) return "what will";
    if( s.equals("what're") ) return "what are";
    if( s.equals("what's") ) return "what is";
    if( s.equals("what've") ) return "what have";
    if( s.equals("where's") ) return "where is";
    if( s.equals("who's") ) return "who had; who would";
    if( s.equals("who'll") ) return "who will";
    if( s.equals("who're") ) return "who are";
    if( s.equals("who's") ) return "who is";
    if( s.equals("who've") ) return "who have";
    if( s.equals("won't") ) return "will not";
    if( s.equals("wouldn't") ) return "would not";
    if( s.equals("you'd") ) return "you had";
    if( s.equals("you'll") ) return "you will";
    if( s.equals("you're") ) return "you are";
    if( s.equals("you've") ) return "you have";
    return s;
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

  public static double wmd(String[] w1, String[] w2, WordEmbeddingsReader em) {

    HashMap<String, double[]> embeddings = new HashMap<>();
    int len_t1=0, len_t2=0;
    for(String w: w1) {
      double[] d = em._cache.get(w);
      if( d==null ) continue;
      embeddings.put(w,d);
      len_t1++;
    }
    for(String w: w2) {
      if( embeddings.get(w)!=null ) len_t2++;
      else {
        double[] d = em._cache.get(w);
        if( d==null ) continue;
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
        distMat[i][j] = ArrayUtils.l2norm2(embeddings.get(t1),embeddings.get(t2));
        sum += distMat[i][j];
      }
    }
    if( sum==0 ) return Double.POSITIVE_INFINITY;

    return JEMD.emdHat(doc2bow(doc1,dict), doc2bow(doc2,dict),distMat,-1);
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


  public static void main(String[] args) {
    String s1 = "air force 20 2017 aboard jan obamas plan sworn trump will";
    String s2 = "air force courtesy flight inauguration newlyformer president";
    partial_ratio(s1,s2);
  }
}
