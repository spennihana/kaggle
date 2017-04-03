package quora;


import water.nbhm.NonBlockingHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class WordEmbeddings {
  int _len;
  transient NonBlockingHashMap<String, double[]> _cache; // cached vecs
  void read(String path, int len) {
    _cache = new NonBlockingHashMap<>();
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
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
