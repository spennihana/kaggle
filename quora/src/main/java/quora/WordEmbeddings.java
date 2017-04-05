package quora;


import water.AutoBuffer;
import water.Iced;
import water.ParallelCsvRead;
import water.parser.BufferedString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class WordEmbeddings extends Iced {

  public enum WORD_EM {
    GLOVE(_embeddings_glove),
    GOOGL(null);
    private final HashMap<BufferedString,double[]> _em;
    WORD_EM(HashMap<BufferedString,double[]> em) { _em=em; }
    public double[] get(String w) { return _em.get(new BufferedString(w)); }
  }
  public static transient HashMap<BufferedString, double[]> _embeddings_googl;
  public static transient HashMap<BufferedString, double[]> _embeddings_glove;

  static {
    // parse word embeddigns statically
    _embeddings_googl = read("./lib/w2vec_models/gw2vec_sample",false);
//    _embeddings_glove = read("./lib/w2vec_models/glove.840B.300d.txt",true);
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
    HashMap<BufferedString,double[]> em = _embeddings_googl;

    int WIDTH = 208; // 208 is maximum string length for googl
//    int width = 1005; // the glove width
    String outpath = "./lib/w2vec_models/googl.bin_sample";
    try(FileOutputStream out = new FileOutputStream(new File(outpath))) {
      byte[] strbits = new byte[WIDTH];
      for(BufferedString bs: em.keySet()) {
        Arrays.fill(strbits, (byte) 0);
        AutoBuffer ab = new AutoBuffer();
        byte[] bsbits = bs.getBuffer();
        System.arraycopy(bsbits,0,strbits,0,bsbits.length);
        ab.putA1(strbits,strbits.length);
        double[] d = em.get(bs);
        for(double dd: d) ab.put4((int)Math.round(dd*1e6));
        assert ab.position() == WIDTH + 4*300; // every line is fixed width
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
