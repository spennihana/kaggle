package DiffLib;

import quora.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static quora.Utils.*;


public class FuzzyCmp {

  public String _s1;
  public String _s2;

  public double _tokenSetRatio;
  public double _partialTokenSetRatio;
  public double _tokenSortRatio;
  public double _partialTokenSortRatio;
  public double _wratio;
  public double _qratio;
  public double _partialRatio;

  public FuzzyCmp(){}
  public FuzzyCmp(String s1, String s2) {_s1=s1; _s2=s2;}

  public void comptue() {
    String[] w1 = _s1.split(" ");
    String[] w2 = _s2.split(" ");
    double[] bothSetRatios = both_token_set_ratio(w1,w2);
    double[] bothSortRatios= both_token_sort_ratio(w1,w2);
    _partialRatio = partial_ratio(_s1,_s2);
    _partialTokenSetRatio=bothSetRatios[0];
    _tokenSetRatio=bothSetRatios[1];

    _partialTokenSortRatio=bothSortRatios[0];
    _tokenSortRatio=bothSortRatios[1];

    _qratio = Utils.qratio(_s1,_s2);
    _wratio = wratio(_s1,_s2);
  }

  private double wratio(String s1, String s2) {
    boolean try_partial;
    double unbase_scale = .95;
    double partial_scale = .90;
    double base = _qratio;
    double minlen = Math.min(s1.length(), s2.length());
    if( minlen==0 ) return 0;
    double len_ratio = (Math.max(s1.length(), s2.length())) / Math.min(s1.length(), s2.length());

    try_partial = len_ratio > 1.5;
    if( len_ratio > 8 ) partial_scale = 0.6;

    if( try_partial ) {
      double partial = _partialRatio * partial_scale;
      double ptsor   = _partialTokenSortRatio * unbase_scale * partial_scale;
      double ptser   = _partialTokenSetRatio  * unbase_scale * partial_scale;
      return maximum(partial,ptsor,ptser,base);
    }
    double tsor = _tokenSortRatio * unbase_scale;
    double tser = _tokenSetRatio  * unbase_scale;
    return maximum(base,tsor,tser);
  }

  private static double[] both_token_set_ratio(String[] w1, String[] w2) {
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
    double[] rs = new double[2];
    r = Math.max(r, partial_ratio(sorted_sect, combined12));
    r = Math.max(r, partial_ratio(sorted_sect, combined21));
    r = Math.max(r, partial_ratio(combined12,  combined21));
    rs[0]=r;
    r=-Double.MAX_VALUE;

    r = Math.max(r, ratio(sorted_sect, combined12));
    r = Math.max(r, ratio(sorted_sect, combined21));
    rs[1]=r;
    return rs;
  }

  private static double[] both_token_sort_ratio(String[] w1, String[] w2) {
    Arrays.sort(w1); Arrays.sort(w2);
    String s1 = join(w1);
    String s2 = join(w2);
    return new double[]{partial_ratio(s1,s2), ratio(s1,s2)};
  }
}
