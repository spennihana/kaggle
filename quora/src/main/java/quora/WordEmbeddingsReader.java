package quora;


import water.*;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.Vec;
import water.nbhm.NonBlockingHashMap;
import water.parser.BufferedString;
import water.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.util.Arrays;

import static quora.WordEmServer.*;

public class WordEmbeddingsReader extends Iced {
  Frame _embeddings;
  int _len;
  transient NonBlockingHashMap<String, double[]> _cache;  // node local cache
  // path to embeddings file
  // length of embedding vector (not including word...)
  void read(String path, int len) {
    _len=len;
    byte[] types = new byte[len+1];
    Arrays.fill(types, Vec.T_NUM);
    types[0]=Vec.T_STR;
    _embeddings = KaggleUtils.importParseFrame(path, "embeddings", types);
  }

  void setupLocal() {_cache =new NonBlockingHashMap<>();}

  double[] find(final String w) {
    double[] v = _cache.get(w);
    if( v!=null ) return v;

    v= new double[_len];
    final double[] _v=v;
    new MRTask() {
      boolean _found=false;
      @Override public void map(Chunk[] cs) {
        if( _found ) return;
        BufferedString bstr = new BufferedString();
        for(int i=0;i<cs[0]._len;++i) {
          String s = cs[0].isNA(i)?"":cs[0].atStr(bstr,i).toString();
          if( w.equals(s) ) {
            _found=true;
            for(int c=1;c<cs.length;++c)
              _v[c-1] = cs[c].atd(i);
            return;
          }
        }
      }
    }.doAll(_embeddings);
    _cache.putIfAbsent(w,_v);
    return _v;
  }

  static double[] get(String word, TCPSendThread st, ByteChannel chan) {
    AutoBuffer ab = new AutoBuffer();
    ab.put1((byte)FETCH).put4(1+word.getBytes().length).put1((byte)0xef);
    ab.putStr(word);
    st.sendMessage(ab, chan);
    // now block on chan to read

    try {
      ByteBuffer bb = ByteBuffer.allocate(1 + 4 + 1).order(ByteOrder.nativeOrder());  // makes a new HeapByteBuffer
      bb.position(0);
      while (bb.hasRemaining()) // read first INIT_BUF bytes into the buffer
        chan.read(bb);
      Log.info("received data...");
      bb.flip();  // flip buffer for reading
      int method = bb.get();
      int sz = bb.getInt();
      int sentinel = (0xFF) & bb.get();
      if (sentinel != SENTINEL)
        throw new IOException("Missing EOM sentinel when opening new tcp channel");
      // read in the rest of the payload from the remote process
      ByteBuffer buf = ByteBuffer.allocate(sz).order(ByteOrder.nativeOrder());
      while (buf.hasRemaining())
        chan.read(buf);
      buf.flip();
      switch(method) {
        case FAIL: FAIL(buf);
        case FETCH: return parseDoubles(buf);
        default: throw new RuntimeException("unknown method: " + method);
      }
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  static double[] parseDoubles(ByteBuffer bb) {
    int ndoubles = bb.getInt();
    double[] d = new double[ndoubles];
    for(int i=0;i<d.length;i++) d[i]=bb.getDouble();
    return d;
  }

  static void FAIL(ByteBuffer bb) {
    int sz = bb.get();
    byte[] s;
    if( sz<=0 ) s= "Failed to retrieve market data.".getBytes();
    else {
      s = new byte[sz - 1];
      bb.get(s);
    }
    throw new RuntimeException(new String(s));
  }
}
