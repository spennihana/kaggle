package quora;

import org.apache.commons.math3.util.FastMath;
import water.MRTask;
import water.fvec.Chunk;
import water.util.IcedInt;

import java.util.HashMap;

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
}
