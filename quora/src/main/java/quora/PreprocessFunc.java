package quora;

import water.fvec.Chunk;
import water.fvec.NewChunk;

public interface PreprocessFunc {
  void f(Chunk[] cs, NewChunk[] ncs);
}


