package water;

import water.network.SocketChannelFactory;
import water.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

// Sends all messages over TCP (even small ones!)
public class TCPSendThread extends Thread {
  volatile boolean _stopped;
  private InetAddress _ip;
  private int _port;
  private boolean _server;  // are we server?
  private final ByteBuffer _bb; // Reusable output large buffer
  private final LinkedBlockingQueue<BufChan> _msgQ = new LinkedBlockingQueue<>();
  static SocketChannelFactory _scf;  // provides SSL ByteChannel
  static { _scf= H2O.SELF.getSocketFactory(); }

  private TCPSendThread() {
    this(new AutoBuffer.BBPool(100<<20).make());
    _server=true;
  }

  private TCPSendThread(ByteBuffer bb) {
    super("WordEm-TCP-SEND-" + H2O.SELF);
    _bb = bb;
    _server=false;
  }

  public static TCPSendThread server() { return new TCPSendThread(); }
  public static TCPSendThread client(InetAddress serverIP, int serverPort) {
    TCPSendThread t = new TCPSendThread(AutoBuffer.BBP_BIG.make());
    assert !t._server;
    t._ip = serverIP;
    t._port = serverPort;
    return t;
  }

  public void sendMessage(AutoBuffer ab, ByteChannel channel) {
    ab.flipForReading();
    sendMessage(ab._bb, channel);
  }

  public synchronized void sendMessage(ByteBuffer bb, ByteChannel channel) {
    assert bb.position()==0 && bb.limit() > 0;
    _msgQ.add(new BufChan(channel,bb));
  }

  private static class BufChan {
    ByteChannel _chan;
    ByteBuffer _bb;
    BufChan(ByteChannel chan, ByteBuffer buf) { _chan=chan; _bb=buf; }
  }

  @Override public void run(){
    try {
      while( !_stopped ) {            // Forever loop
        BufChan bc = _msgQ.take(); // take never returns null but blocks instead
        ByteBuffer bb = bc._bb;
        assert !bb.isDirect() : "Direct BBs already got recycled";
        assert bb.limit()+1+2 <= _bb.capacity() : "Payload larger than the output buffer: " + (bb.limit()+1+2) + " < " + (_bb.capacity());
        assert bb.remaining() < bb.limit()+1+2;
        _bb.put(bb.array(),0,bb.limit()); // jam all bytes onto _bb
        sendBuffer(bc._chan);                     // send the payload (assumes to be properly formatted)
      }
    } catch(Throwable t) { throw Log.throwErr(t); }
  }
  public Thread halt() { _stopped=true; return this;}

  synchronized void sendBuffer(ByteChannel chan) {
    int retries = 0;
    _bb.flip();                 // limit set to old position; position set to 0
    while( !_stopped && _bb.hasRemaining()) {
      try {
        chan.write(_bb);
      } catch(Exception ioe) {
        ioe.printStackTrace();
        _bb.rewind();           // Position to zero; limit unchanged; retry the operation
        if( !_stopped && !H2O.getShutdownRequested() && (Paxos._cloudLocked || retries++ > 300) ) {
          Log.err("Got IO error when sending TCP bytes: ",ioe);
          _bb.clear();
          break;
        }
        closeChan(chan);
        retries++;
        final int sleep = Math.min(5000,retries << 1);
        try {Thread.sleep(sleep);} catch (InterruptedException e) {/*ignored*/}
      }
    }
    _bb.clear();            // Position set to 0; limit to capacity
    if( _server ) closeChan(chan);
  }

  void closeChan(ByteChannel chan) {
    if(chan != null) {
      try {chan.close();} catch (IOException e) { /*ignored*/}
    }
  }
  public ByteChannel newChan() throws IOException {
    if( _server )
      throw new UnsupportedOperationException("Server never opens new channel");
    assert _ip!=null && _port > 0 : "expected ip and port to be set";
    // Must make a fresh socket
    SocketChannel sock = SocketChannel.open();
    sock.socket().setReuseAddress(true);
    sock.socket().setSendBufferSize(AutoBuffer.BBP_BIG._size);
    InetSocketAddress isa = new InetSocketAddress(_ip, _port);
    boolean res = sock.connect(isa); // Can toss IOEx, esp if other node is still booting up
    assert res;
    sock.configureBlocking(true);
    assert !sock.isConnectionPending() && sock.isBlocking() && sock.isConnected() && sock.isOpen();
    sock.socket().setTcpNoDelay(true);
    return _scf.clientChannel(sock, isa.getHostName(), isa.getPort());
  }

  public static ByteChannel openChan(InetAddress ip, int port) {
    try {
      SocketChannel sock = SocketChannel.open();
      sock.socket().setReuseAddress(true);
      sock.socket().setSendBufferSize(AutoBuffer.BBP_BIG._size);
      InetSocketAddress isa = new InetSocketAddress(ip, port);
      boolean res = sock.connect(isa); // Can toss IOEx, esp if other node is still booting up
      assert res;
      sock.configureBlocking(true);
      assert !sock.isConnectionPending() && sock.isBlocking() && sock.isConnected() && sock.isOpen();
      sock.socket().setTcpNoDelay(true);
      return sock;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}