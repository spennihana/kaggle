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
import java.net.HttpURLConnection;
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
      URL md = openURLConnection(word);
      try (BufferedReader in = new BufferedReader(new InputStreamReader(md.openStream()))) {
        String foo = in.readLine();
        return null;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch(MalformedURLException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  static URL openURLConnection(String word) throws MalformedURLException, UnsupportedEncodingException {
    URL md = new URL("192.168.1.145:54321/3/WordEm?word="+ URLEncoder.encode(word, "UTF-8"));
    HttpURLConnection.setFollowRedirects(false);
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) md.openConnection();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      System.out.println("Response code = " + connection.getResponseCode());
    } catch (IOException e) {
      e.printStackTrace();
    }
    String header = connection.getHeaderField("location");
    if (header != null)
      System.out.println("Redirected to " + header);
    return md;
  }
}
