package water;

import quora.WordEmServer;
import water.util.Log;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;

import static quora.WordEmServer.CONN;
import static quora.WordEmServer.SENTINEL;

class ResponderThread extends Thread {

  final InetAddress _client;
  final ByteChannel _chan;
  ResponderThread(String name, InetAddress client, ByteChannel channel) {
    super("ResponderThread-" + name + "-" + H2O.SELF);
    _client = client;
    _chan = channel;
  }

  // flip buffer and send...
  private static void send(AutoBuffer ab, ByteChannel channel) {
    ab.flipForReading();
    ByteBuffer bb = ab._bb;
    assert bb.position()==0 && bb.limit() > 0;
    assert !bb.isDirect() : "Direct BBs already got recycled";
    assert bb.remaining() < bb.limit()+1+2;
    int retries=0;
    try {
      channel.write(bb);
    } catch (Exception ioe) {
      ioe.printStackTrace();
      bb.rewind();
      if( retries++ > 300) {
        Log.err("Got IO error when sending TCP bytes: ", ioe);
        bb.clear();
      }
      retries++;
      final int sleep = Math.min(5000, retries<<1);
      try{Thread.sleep(sleep);} catch( InterruptedException e) {/* ignored */}
    }
  }

  static class Fetch extends ResponderThread {
    private final ByteBuffer _bb;
    Fetch(InetAddress client, ByteChannel channel, int sz) {
      super("Fetch",client, channel);
      _bb= ByteBuffer.allocate(sz).order(ByteOrder.nativeOrder());  // makes a new HeapByteBuffer
    }
    @Override public void run() {
      String word;
      AutoBuffer res;
      try {
        while( _bb.hasRemaining() )
          _chan.read(_bb);
        _bb.flip();
        byte[] s = new byte[_bb.get()-1];  // _bb.get()-1 is the length of the symbol, extra 1 added by AutoBuffer, subtract it here...
        _bb.get(s);
        word = new String(s, "UTF-8");
        res   = WordEmServer.get(word);
      } catch (Throwable e1) {
        throw new RuntimeException(e1);
      }
      send(res,_chan);
    }
  }

  static class Conn extends ResponderThread {
    Conn(InetAddress client, ByteChannel channel) { super("Conn", client, channel); }
    @Override public void run() {
      Log.info("Received connection from: " + _client);
      AutoBuffer ab = new AutoBuffer(2);
      ab.put1(CONN).put4(0).put1(SENTINEL);
      send(ab,_chan);
    }
  }
}