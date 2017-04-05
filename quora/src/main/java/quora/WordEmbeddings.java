package quora;


import water.Iced;
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

//    _embeddings_googl = read("./lib/w2vec_models/gw2vec",false);
//    _embeddings_glove = read("./lib/w2vec_models/glove.840B.300d.txt",true);
//    compress();
  }
}
