package water;

import water.fvec.Chunk;
import water.fvec.Frame;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;

import static water.KaggleUtils.importParseFrame;

public class Predict extends MRTask<Predict> {

  String _modelPath;
  String _testPath;
  int _subNumber;
  transient Socket _sock;

  private static final int[] CHANS = new int[]{34534, 34536, 34538, 35012, 35014, 35016, 35018, 35020};

  Predict(String modelPath) {
    _modelPath=modelPath;
  }

  public ByteChannel newChan(int cidx) throws IOException {
    // Must make a fresh socket
    SocketChannel sock = SocketChannel.open();
    sock.socket().setReuseAddress(true);
    InetSocketAddress isa = new InetSocketAddress("127.0.0.1", 34534); //CHANS[cidx%CHANS.length]);
    boolean res = sock.connect(isa); // Can toss IOEx, esp if other node is still booting up
    assert res;
    sock.configureBlocking(true);
    assert !sock.isConnectionPending() && sock.isBlocking() && sock.isConnected() && sock.isOpen();
    sock.socket().setTcpNoDelay(true);
    _sock=sock.socket();
    return sock;
  }

  @Override public void map(Chunk[] cs) {
    ByteChannel chan=null;
    AutoBuffer ab = new AutoBuffer();
    int batch=10000;
    long nbytes_read=0;
    try {
      chan = newChan(cs[0].cidx());
    } catch (IOException e) {
      e.printStackTrace();
    }
    int nrow=0;
    for(int r=0;r<cs[0]._len;++r) {
      for(int c=1;c<cs.length;++c) {
//        if( _fr.name(c).equals("id") || _fr.name(c).equals("label") ) continue; // skip labels and ids
        ab.put8d(cs[c].atd(r));
      }
      nrow++;
      if( nrow%batch==0 ) {
        double nr = ab.position() / (17*8.);
        System.out.println("Sending " + nr + " rows to " + _sock.getInetAddress() + ":" + _sock.getPort());
        ab.flipForReading();
        predict2(ab,chan);
        ByteBuffer bb = ByteBuffer.allocate(nrow*8).order(ByteOrder.nativeOrder());
        while( bb.hasRemaining() ) {
          try {
            chan.read(bb);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        nbytes_read+= bb.limit();
        ab._bb.clear();
        nrow=0;
      }
    }
    System.out.println("Sending " + nrow + " rows to " + _sock.getInetAddress() +  ":" + _sock.getPort());
    ab.flipForReading();
    predict2(ab, chan);
    ByteBuffer bb = ByteBuffer.allocate(nrow*8).order(ByteOrder.nativeOrder());
    while( bb.hasRemaining() ) {
      try {
        chan.read(bb);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    nbytes_read+= bb.limit();
    System.out.println("Bytes read from remote: " + nbytes_read + "; nrows=" + nbytes_read / (17*8.));
    try {
      assert chan != null;
      chan.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  void predict2(AutoBuffer ab, ByteChannel chan) {
    int ntries=0;
    while(true) {
      try {
        chan.write(ab._bb);
        return;
      } catch (IOException e) {
        if( ntries>=1000 )
          throw new RuntimeException(e);
        ntries++;
      }
    }
  }

  public static void main(String[] args) {
    H2OApp.main(args);
    Frame fr = importParseFrame("./data/exp1_test.csv","test",null);
    Chunk[] cs = new Chunk[fr.numCols()];
    for(int i=0;i<cs.length;++i) cs[i] = fr.vec(i).chunkForChunkIdx(0);
    long s = System.currentTimeMillis();
//    new Predict("./models/0001.model").map(cs,new NewChunk[]{});
    new Predict("./models/0001.model").doAll(fr);
    System.out.println("all done: " + (System.currentTimeMillis()-s)/1000. + " seconds");
  }
}
