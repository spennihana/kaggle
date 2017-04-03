package quora;

import water.H2O;

public class Quora {

  public static void main(String[] args) {
    WordEmbeddings em = new WordEmbeddings(); // load word embeddings statically now
    H2O.main(args);
  }

}
