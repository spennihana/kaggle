package quora;

import embeddings.WordEmbeddings;
import water.H2O;

public class HelperNode {
  public static void main(String[] args) {
    WordEmbeddings.EMBEDDINGS tapEnumToLoad = WordEmbeddings.EMBEDDINGS.GLOVE;
    H2O.main(args);
  }
}
