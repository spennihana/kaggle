package quora;


import water.Iced;
import water.ParallelCsvRead;
import water.parser.BufferedString;

import java.util.Arrays;
import java.util.HashMap;

public class WordEmbeddings extends Iced {

  public transient HashMap<BufferedString, double[]> _embeddings;
  final String _path;

  WordEmbeddings(String path) { _path=path; }
  public  WordEmbeddings read() {
    ParallelCsvRead pcsv = new ParallelCsvRead(_path);
    long start = System.currentTimeMillis();
    long elapsed;
    long elapsed2;
    long elapsed3;
    pcsv.raw_parse(); // suck in the raw bytes
    elapsed = System.currentTimeMillis() - start;
    System.out.println("Disk to RAM read in " + elapsed/1000. + " seconds" );
    start = System.currentTimeMillis();
    pcsv.parse_bytes();
    elapsed2 = System.currentTimeMillis() - start;
    System.out.println("Parsed word embeddings in " + elapsed2/1000. + " seconds");
    _embeddings = new HashMap<>();
    start = System.currentTimeMillis();
    for(ParallelCsvRead.ParseBytesTask pbt: pcsv._pbtasks) {
      _embeddings.putAll(pbt._rows);
      pbt._rows=null;
    }
    elapsed3 = System.currentTimeMillis() - start;
    System.out.println("Reducing word embeddings together in " + elapsed3/1000. + " seconds");
    System.out.println("Total embeddings parse time: " + (elapsed + elapsed2 + elapsed3)/1000. +  " seconds");
    return this;
  }

  public double[] get(String w) {
    return _embeddings.get(new BufferedString(w));
  }

  public static void main(String[] args){
    WordEmbeddings wem = new WordEmbeddings("./lib/w2vec_models/gw2vec_sample").read();
    System.out.println(Arrays.toString(wem.get("hello")));
  }
}
