package quora;

import water.H2O;

public class Quora {

  public static void main(String[] args) {
    WordEmbeddings.WORD_EM tapEnumToLoad = WordEmbeddings.WORD_EM.GLOVE;
    H2O.main(args);
  }

}
