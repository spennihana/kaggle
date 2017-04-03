package quora;


import water.ParallelCsvRead;
import water.nbhm.NonBlockingHashMap;
import water.parser.BufferedString;
import water.util.Log;

import java.util.Arrays;

public class WordEmbeddings {

  private ParallelCsvRead.ParseBytesTask[] _pbTasks;
  NonBlockingHashMap<BufferedString, double[]> _embeddings;

  public static WordEmbeddings read(String path) {
    WordEmbeddings em = new WordEmbeddings();
    ParallelCsvRead pcsv = new ParallelCsvRead(path);
    long start = System.currentTimeMillis();
    long elapsed;
    long elapsed2;
    pcsv.raw_parse(); // suck in the raw bytes
    elapsed = System.currentTimeMillis() - start;
    Log.info("Read raw bytes in to RAM in " + elapsed/1000. + " seconds" );
    start = System.currentTimeMillis();
    pcsv.parse_bytes();
    elapsed2 = System.currentTimeMillis() - start;
    Log.info("Parsed word embeddings in " + elapsed2/1000. + " seconds");
    Log.info("Total embeddings parse time: " + (elapsed + elapsed2)/1000. +  " seconds");
    em._embeddings = new NonBlockingHashMap<>();
    for(ParallelCsvRead.ParseBytesTask pbt: pcsv._pbtasks) {
      em._embeddings.putAll(pbt._rows);
      pbt._rows=null;
    }
    em._pbTasks=null;
    return em;
  }

  public double[] get(String w) {
    return _embeddings.get(new BufferedString(w));
  }

  public static void main(String[] args){
    WordEmbeddings wem = WordEmbeddings.read("./lib/w2vec_models/gw2vec");
    System.out.println(Arrays.toString(wem.get("helo")));
  }
}
