package quora;

import water.MRTask;
import water.fvec.Chunk;

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
}
