package quora;


import water.Iced;
import water.KaggleUtils;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.Vec;
import water.nbhm.NonBlockingHashMap;
import water.parser.BufferedString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;

public class WordEmbeddingsReader extends Iced {
  Frame _embeddings;
  int _len;
  transient NonBlockingHashMap<String, double[]> _cache;  // node local cache
  // path to embeddings file
  // length of embedding vector (not including word...)
  void read(String path, int len) {
    _len=len;
    byte[] types = new byte[len+1];
    Arrays.fill(types, Vec.T_NUM);
    types[0]=Vec.T_STR;
    _embeddings = KaggleUtils.importParseFrame(path, "embeddings", types);
  }

  void setupLocal() {_cache =new NonBlockingHashMap<>();}

  public double[] find(final String w) {
    double[] v = _cache.get(w);
    if( v!=null ) return v;

    v= new double[_len];
    final double[] _v=v;
    new MRTask() {
      boolean _found=false;
      @Override public void map(Chunk[] cs) {
        if( _found ) return;
        BufferedString bstr = new BufferedString();
        for(int i=0;i<cs[0]._len;++i) {
          String s = cs[0].isNA(i)?"":cs[0].atStr(bstr,i).toString();
          if( w.equals(s) ) {
            _found=true;
            for(int c=1;c<cs.length;++c)
              _v[c-1] = cs[c].atd(i);
            return;
          }
        }
      }
    }.doAll(_embeddings);
    _cache.putIfAbsent(w,_v);
    return _v;
  }

  static double[] get2(String word) {
    try {
      URL md = openURLConnection("http://192.168.1.145:54321/3/WordEm?word="+ URLEncoder.encode(word, "UTF-8"));
      try (BufferedReader in = new BufferedReader(new InputStreamReader(md.openStream()))) {
        String foo = in.readLine();
        return parseDoubles(foo.substring(foo.indexOf("[")+1, foo.indexOf("]")));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch(MalformedURLException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  static double[][] get3(String s1, String s2) {
    try {
      String url = "http://192.168.1.145:54321/3/WordEm?q1="+ URLEncoder.encode(s1, "UTF-8")+"&q2="+URLEncoder.encode(s2, "UTF-8");
      URL md = openURLConnection(url);
      try (BufferedReader in = new BufferedReader(new InputStreamReader(md.openStream()))) {
        String res = in.readLine();
        double[][] mm = new double[2][];
        mm[0] = parseDoublesHelper(res.substring(res.indexOf("max"), res.indexOf("]")+1));
        mm[1] = parseDoublesHelper(res.substring(res.indexOf("min"), res.indexOf("]")+1));
        return mm;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch(MalformedURLException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  static double[] parseDoublesHelper(String s) {
    return parseDoubles(s.substring(s.indexOf("[")+1, s.indexOf("]")));
  }

  static double[] parseDoubles(String da) {
    double[] res = new double[300];
    int x=0;
    int c=0;
    for(int i=0;i<da.length();++i) {
      if(da.charAt(i)==',') {
        res[c++] = (double)Float.valueOf(da.substring(x,i));
        x=i+1;
      }
    }
    return res;
  }

  static URL openURLConnection(String url) throws MalformedURLException, UnsupportedEncodingException {
    return new URL(url);
  }
}
