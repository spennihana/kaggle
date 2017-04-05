package quora;


import water.Iced;
import water.ParallelCsvRead;
import water.parser.BufferedString;

import java.util.Arrays;
import java.util.HashMap;

public class WordEmbeddings extends Iced {

  public enum WORD_EM {
    GLOVE(new ParallelCsvRead("./lib/w2vec_models/glove.bin",1005,2).parse_bin()),
    GOOGL(new ParallelCsvRead("./lib/w2vec_models/googl.bin",208,1).parse_bin());
    private final WordEmbeddings _em;
    WORD_EM(HashMap<BufferedString,BufferedString>[] maps) { _em=new WordEmbeddings(maps); }
    public void get(String w,double[] res) {_em.get(new BufferedString(w),res); }
  }
  private transient HashMap<BufferedString,BufferedString> _map;
  private WordEmbeddings(HashMap<BufferedString,BufferedString>[] maps) {
    _map=new HashMap<>();
    for (HashMap<BufferedString, BufferedString> map : maps) _map.putAll(map);
  }

  void get(BufferedString s,double[] res) {
    Arrays.fill(res,0);
    BufferedString bs= _map.get(s);
    if( bs==null )
      return;
    int i=bs.getOffset();
    byte[] buf = bs.getBuffer();
    int idx=0;
    for(;i<4*300;i+=4)
      res[idx++]=(double)readInt(i,buf)/1e6;
  }

  int readInt(int pos, byte[] buf) {
    return ( buf[pos++] & 0xFF ) | (buf[pos++] & 0xFF) <<8 | (buf[pos++] & 0xFF) << 16 | (buf[pos] & 0xFF) << 24;
  }
}
