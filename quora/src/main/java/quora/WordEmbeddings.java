package quora;


import water.AutoBuffer;
import water.Iced;
import water.ParallelCsvRead;
import water.parser.BufferedString;

import java.io.*;
import java.util.HashMap;

public class WordEmbeddings extends Iced {

  public enum WORD_EM {
    GLOVE(_embeddings_glove),
    GOOGL(null);
    private final HashMap<BufferedString,double[]> _em;
    WORD_EM(HashMap<BufferedString,double[]> em) { _em=em; }
    public double[] get(String w) { return _em.get(new BufferedString(w)); }
  }
//  public static transient HashMap<BufferedString, double[]> _embeddings_googl;
  public static transient HashMap<BufferedString, double[]> _embeddings_glove;

  static {
    // parse word embeddigns statically
//    _embeddings_googl = read("./lib/w2vec_models/gw2vec",false);
    _embeddings_glove = read("./lib/w2vec_models/glove.840B.300d.txt",true);
    compress();
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


  static void compress() {
    long s = System.currentTimeMillis();
    System.out.println("compressing embeddings...");
    HashMap<BufferedString,double[]> em = _embeddings_glove;
    String outpath = "./lib/w2vec_models/glove.bin";
    try(FileOutputStream out = new FileOutputStream(new File(outpath))) {
      for(BufferedString bs: em.keySet()) {
        AutoBuffer ab = new AutoBuffer();
        byte[] strbits = bs.getBuffer();
        ab.put4(strbits.length);
        ab.putA1(strbits);
        double[] d = em.get(bs);
        for(double dd: d) ab.put8d(dd);
        out.write(ab.buf());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    System.out.println("Finished in " + (System.currentTimeMillis() - s)/1000. + " seconds");
  }

  public static void main(String[] args) {
    System.out.println("yayyy");
  }
}
