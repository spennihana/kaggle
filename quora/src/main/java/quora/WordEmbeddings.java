package quora;


import water.Iced;
import water.ParallelCsvRead;
import water.parser.BufferedString;

import java.util.HashMap;

public class WordEmbeddings extends Iced {

  public enum WORD_EM {
    GLOVE(_embeddings_glove),
    GOOGL(_embeddings_googl);
    private final HashMap<BufferedString,double[]> _em;
    WORD_EM(HashMap<BufferedString,double[]> em) { _em=em; }
    public double[] get(String w) { return _em.get(new BufferedString(w)); }
  }
  public static transient HashMap<BufferedString, double[]> _embeddings_googl;
  public static transient HashMap<BufferedString, double[]> _embeddings_glove;

  static {
    // parse word embeddigns statically
    _embeddings_googl = read("./lib/w2vec_models/gw2vec",false);
    _embeddings_glove = read("./lib/w2vec_models/glove.840B.300d.txt",true);
  }

  public static HashMap<BufferedString, double[]> read(String path, boolean glove) {
    ParallelCsvRead pcsv = new ParallelCsvRead(path);
    long start = System.currentTimeMillis();
    long elapsed;
    long elapsed2;
    long elapsed3;
    pcsv.raw_parse(); // suck in the raw bytes
    elapsed = System.currentTimeMillis() - start;
    System.out.println("Disk to RAM read in " + elapsed/1000. + " seconds" );
    start = System.currentTimeMillis();
    if( glove ) pcsv.parse_glove();
    else        pcsv.parse_bytes();
    elapsed2 = System.currentTimeMillis() - start;
    System.out.println("Parsed word embeddings in " + elapsed2/1000. + " seconds");
    HashMap<BufferedString, double[]> embeddings = new HashMap<>();
    start = System.currentTimeMillis();
    for(ParallelCsvRead.ParseBytesTask pbt: pcsv._pbtasks) {
      embeddings.putAll(pbt._rows);
      pbt._rows=null;
    }
    elapsed3 = System.currentTimeMillis() - start;
    System.out.println("Reducing word embeddings together in " + elapsed3/1000. + " seconds");
    System.out.println("Total embeddings parse time: " + (elapsed + elapsed2 + elapsed3)/1000. +  " seconds");
    return embeddings;
  }
}
