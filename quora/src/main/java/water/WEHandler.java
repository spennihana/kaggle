package water;

import quora.Utils;
import quora.WordEmServer;
import water.api.API;
import water.api.Handler;
import water.api.schemas3.SchemaV3;

import java.util.Arrays;

import static quora.Utils.*;


public class WEHandler extends Handler {
  public WESchema em(int v, WESchema args) {
    args.em= WordEmServer._em.find(args.word);
    return args;
  }

  public WESchema em2(int v, WESchema args) {
    String q1 = args.q1;
    String q2 = args.q2;
    q1 = q1.replace("\\?", "");
    q2 = q2.replace("\\?", "");
    String[] w1 = q1.split(" ");
    String[] w2 = q2.split(" ");
    // undo contractions, remove punctuation, lower
    for (int i = 0; i < w1.length; ++i) w1[i] = contractionMap(w1[i]).replaceAll("\\p{P}", "").toLowerCase();
    for (int i = 0; i < w2.length; ++i) w2[i] = contractionMap(w2[i]).replaceAll("\\p{P}", "").toLowerCase();
    q1 = Utils.join(w1);
    q2 = Utils.join(w2);
    w1 = q1.split(" ");
    w2 = q2.split(" ");

    double[][] min_we = new double[2][300];
    Arrays.fill(min_we[0], Double.MAX_VALUE);
    Arrays.fill(min_we[1], Double.MAX_VALUE);
    double[][] max_we = new double[2][300];
    Arrays.fill(max_we[0], -Double.MAX_VALUE);
    Arrays.fill(max_we[1], -Double.MAX_VALUE);

    for(String w: w1) {
      double[] c = WordEmServer._em.find(w);
      min_we[0] = reduceMin(min_we[0], c);
      max_we[0] = reduceMax(max_we[0], c);
    }
    for(String w: w2) {
      double[] c = WordEmServer._em.find(w);
      min_we[1] = reduceMin(min_we[1], c);
      max_we[1] = reduceMax(max_we[1], c);
    }

    args.min=new double[min_we[0].length];
    args.max=new double[max_we[0].length];
    for(int i=0;i<min_we[0].length;i++) args.min[i]=Math.abs(min_we[0][i]-min_we[1][i]);
    for(int i=0;i<max_we[0].length;i++) args.max[i]=Math.abs(max_we[0][i]-max_we[1][i]);
    return args;
  }

  public static class WESchema extends SchemaV3<Iced, WESchema> {
    @API(help="word", direction=API.Direction.INPUT) public String word;
    @API(help="word", direction=API.Direction.INPUT) public String q1;
    @API(help="word", direction=API.Direction.INPUT) public String q2;
    @API(help="res",  direction=API.Direction.OUTPUT) public double[] em;
    @API(help="res",  direction=API.Direction.OUTPUT) public double[] min;
    @API(help="res",  direction=API.Direction.OUTPUT) public double[] max;
  }
}
