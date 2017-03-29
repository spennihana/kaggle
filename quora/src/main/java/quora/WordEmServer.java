package quora;

import water.*;
import water.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WordEmServer {

  public static final int FAIL=0;
  public static final int FETCH=1; // get word embedding [word]
  public static final int CONN=2;
  public static final int SENTINEL=0xef;

  public static final int PORT=34534;  // todo: scan for a port on bootup?
  public static InetSocketAddress _key;
  public static TCPSendThread _sendThread;

  static WordEmbeddingsReader _em;

  public static void main(String[] args) throws IOException {
    H2OApp.main(args);
    _key = new InetSocketAddress(H2O.SELF_ADDRESS, PORT);
    (new TCPThread()).start();  // receiver thread
    (_sendThread = TCPSendThread.server()).start(); // sender thread
    _em = new WordEmbeddingsReader();
    _em.read("./lib/w2vec_models/gw2vec_sample",300);
    Log.info("Server ready...");
  }

  public static AutoBuffer get(String word) {
    AutoBuffer ab = new AutoBuffer();
    double[] v = _em.find(word);
    ab.put1(FETCH).put4(4/*ndoubles*/ + 8*v.length).put1(SENTINEL);
    ab.putInt(v.length);
    for( double aV : v ) ab.put8d(aV);
    return ab;
  }
}