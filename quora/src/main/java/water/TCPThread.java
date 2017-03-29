package water;

import quora.WordEmServer;
import water.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;

import static quora.WordEmServer.CONN;
import static quora.WordEmServer.FETCH;
import static quora.WordEmServer.SENTINEL;
import static water.TCPSendThread._scf;

public class TCPThread extends Thread {
  private static final int INIT_BUF=1+4+1;
  private ServerSocketChannel _ssc;   // this is the ServerSocketChannel bound during bootup
  private ByteBuffer _bb;
  public TCPThread() { _bb = ByteBuffer.allocate(INIT_BUF).order(ByteOrder.nativeOrder()); }

  @SuppressWarnings("resource")
  public void run() {
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    ServerSocketChannel errsock = null;
    boolean saw_error = false;
    ExecutorService es = Executors.newSingleThreadExecutor();
    Log.info("Started receiver thread.");
    while( true ) {
      SocketChannel socketChannel=null;
      try {
        // Cleanup from any prior socket failures.  Rare unless we're really sick.
        if( errsock != null ) { // One time attempt a socket close
          final ServerSocketChannel tmp2 = errsock; errsock = null;
          tmp2.close();       // Could throw, but errsock cleared for next pass
        }
        if( saw_error ) Thread.sleep(100); // prevent deny-of-service endless socket-creates
        saw_error = false;

        // More common-case setup of a ServerSocket
        if( _ssc == null ) {
          _ssc = ServerSocketChannel.open();
          _ssc.socket().setReceiveBufferSize(AutoBuffer.BBP_BIG._size);
          _ssc.socket().bind(WordEmServer._key);
        }
        // Block for TCP connection and setup to read from it.
        socketChannel = _ssc.accept();  // client connect
        final SocketChannel sock=socketChannel;
        es.submit(() -> {
          try {
            ByteChannel chan = _scf.serverChannel(sock);  // this must be an ssl wrapped socket
            Log.info("Handling remote tcp connection from: " + sock.getRemoteAddress());
            _bb.position(0);  // reset buffer
            while(_bb.hasRemaining())  // read first INIT_BUF bytes into the buffer
              chan.read(_bb);
            _bb.flip();  // flip buffer for reading
            int method = _bb.get();
            int sz = _bb.getInt(); // get the size of the remaining bytes in channel
            int sentinel = (0xFF) & _bb.get();
            if (sentinel != SENTINEL)
              throw new IOException("Missing EOM sentinel when opening new tcp channel");

            InetAddress client = sock.socket().getInetAddress();
            switch (method) {
              case FETCH:
                new ResponderThread.Fetch(client, chan, sz).start();
                break;
              case CONN:
                new ResponderThread.Conn(client, chan).start();
                break;
              default:
                throw new IllegalArgumentException("Unknown method type: " + method);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }).get(2,TimeUnit.SECONDS); // should be extremely fast to handle a connect a client and fork a handling thread
      } catch( AsynchronousCloseException ex ) {
        break;                  // Socket closed for shutdown
      } catch ( InterruptedException | IOException | ExecutionException e) {
        e.printStackTrace();
        // On any error from anybody, close all sockets & re-open
        Log.err("IO error on TCP port "+WordEmServer.PORT+": ",e);
        saw_error = true;
        errsock = _ssc;  _ssc = null; // Signal error recovery on the next loop
      } catch (TimeoutException e) {
        if(socketChannel != null) {
          try {socketChannel.close();} catch (IOException ee) { /*ignored*/}
        }
      }
    }
  }
}