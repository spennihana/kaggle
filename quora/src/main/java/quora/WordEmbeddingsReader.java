package quora;


import water.Iced;
import water.KaggleUtils;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.Vec;
import water.parser.BufferedString;

import java.util.Arrays;

public class WordEmbeddingsReader extends Iced {

  Frame _embeddings;
  double[] _v;

  // path to embeddings file
  // length of embedding vector (not including word...)
  void read(String path, int len) {
    _v = new double[len];
    byte[] types = new byte[len+1];
    Arrays.fill(types, Vec.T_NUM);
    types[0]=Vec.T_STR;
    _embeddings = KaggleUtils.importParseFrame(path, "embeddings", types);
  }

  double[] find(final String w) {
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
    return _v;
  }
}
