package quora;

import water.H2OApp;
import water.util.Log;

import java.io.IOException;

public class WordEmServer {

  public static WordEmbeddingsReader _em;

  public static void main(String[] args) throws IOException {
    H2OApp.main(args);
    _em = new WordEmbeddingsReader();
    _em.read("./lib/w2vec_models/gw2vec_sample",300);
    _em.setupLocal();
    Log.info("Server ready...");
  }
}