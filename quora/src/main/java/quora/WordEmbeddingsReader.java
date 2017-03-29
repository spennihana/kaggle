package quora;


import water.Iced;
import water.KaggleUtils;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.Vec;
import water.parser.BufferedString;
import water.util.IcedHashMap;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;

public class WordEmbeddingsReader extends Iced {
  Frame _embeddings;
  int _len;
  IcedHashMap<String, double[]> _cache;  // node local cache
  // path to embeddings file
  // length of embedding vector (not including word...)
  void read(String path, int len) {
    _len=len;
    byte[] types = new byte[len+1];
    Arrays.fill(types, Vec.T_NUM);
    types[0]=Vec.T_STR;
    _embeddings = KaggleUtils.importParseFrame(path, "embeddings", types);
    _cache = new Reduce().doAll(_embeddings).r;
    _embeddings.delete();
  }

  void read2(String path, int len) {
    _cache = new IcedHashMap<>();
    _len=len;
    int print=20000;
    try(BufferedReader in = new BufferedReader(new FileReader(new File(path)))) {
      String line;
      while( (line=in.readLine())!=null ) {
        int x=0;
        int d=0;
        String w=null;
        double[] v = new double[300];
        for(int c=0;c<line.length();++c) {
          if( line.charAt(c)==' ' ) {
            if( w==null ) w=line.substring(x,c);
            else          v[d++] = Double.valueOf(line.substring(x,c));
            x=c+1;
          }
        }
        _cache.put(w,v);
        if(_cache.size() % print==0) {
          System.out.println("Read " + _cache.size() + " word vectors out of 3,000,000");
//          print <<= 1;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void setupLocal() {
    _cache = new Reduce().doAll(_embeddings).r;
    _embeddings.delete();
  }

  static class Reduce extends MRTask<Reduce> {
    IcedHashMap<String, double[]> r;
    @Override public void setupLocal() { r=new IcedHashMap<>(); }
    @Override public void map(Chunk[] cs) {
      BufferedString bstr = new BufferedString();
      double[] v = new double[300];
      for(int i=0;i<cs[0]._len;++i) {
        if( cs[0].isNA(i) ) continue;
        String s = cs[0].atStr(bstr,i).toString();
        for(int c=1;c<cs.length;++c) v[c-1] = cs[c].atd(i);
        r.put(s,v);
      }
    }
    @Override public void reduce(Reduce t) { if( r!=t.r ) r.putAll(t.r); }
  }

  public double[] find(final String w) { return _cache.get(w); }

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

  static double[][] get3(String url) {
    try {
      URL md = openURLConnection(url);
      try (BufferedReader in = new BufferedReader(new InputStreamReader(md.openStream()))) {
        String res = in.readLine();
        double[][] mm = new double[2][];
        mm[0] = parseDoublesHelper(res.substring(res.indexOf("max")));
        mm[1] = parseDoublesHelper(res.substring(res.indexOf("min")));
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
