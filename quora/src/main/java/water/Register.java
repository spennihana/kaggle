package water;

import water.api.AbstractRegister;

@SuppressWarnings("unused")
public class Register extends AbstractRegister {
  @Override public void register(String relativeResourcePath) throws ClassNotFoundException {
    H2O.register("GET /3/WordEm", WEHandler.class, "em", "", "Get word embeddings");
  }
}