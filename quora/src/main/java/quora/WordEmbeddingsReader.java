package quora;


import water.Iced;
import water.KaggleUtils;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.Vec;
import water.parser.BufferedString;

import java.util.Arrays;
import java.util.HashMap;

public class WordEmbeddingsReader extends Iced {
  Frame _embeddings;
  int _len;
  transient HashMap<String, double[]> _cache;
  // path to embeddings file
  // length of embedding vector (not including word...)
  void read(String path, int len) {
    _len=len;
    byte[] types = new byte[len+1];
    Arrays.fill(types, Vec.T_NUM);
    types[0]=Vec.T_STR;
    _embeddings = KaggleUtils.importParseFrame(path, "embeddings", types);
  }

  double[] find(final String w) {
    if( _cache==null ) _cache=new HashMap<>();
    double[] v = _cache.get(w);
    if( v!=null ) return v;

    v= new double[_len];
    final double[] _v=v;
    new MRTask() {
      @Override public void map(Chunk[] cs) {
        BufferedString bstr = new BufferedString();
        for(int i=0;i<cs[0]._len;++i) {
          String s = cs[0].isNA(i)?"":cs[0].atStr(bstr,i).toString();
          if( w.equals(s) ) {
            System.out.println("Found word...");
            for(int c=1;c<cs.length;++c)
              _v[c-1] = cs[c].atd(i);
            return;
          }
        }
      }
    }.doAll(_embeddings);
    _cache.put(w,_v);
    return _v;
  }
}
