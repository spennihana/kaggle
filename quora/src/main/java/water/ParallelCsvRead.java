package water;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class ParallelCsvRead {

  public ReadTask[] _rtasks;
  int _nchks;
  long _nbytes;
  String _path;
  public ParallelCsvRead(String path) {
    File f = new File(path);
    long nbytes = f.length();
    int nchks = (int)Math.ceil((double)nbytes/(double)ReadTask.CHK_SIZE);
    _rtasks = new ReadTask[nchks];
    _nchks=nchks;
    _nbytes=nbytes;
    _path=path;
  }

  public void raw_parse() {
    ArrayList<ReadTask> rtasks = new ArrayList<>();
    for(int i=0;i<_rtasks.length;++i) {
      _rtasks[i] = new ReadTask(i,_path, (long)i * (long)ReadTask.CHK_SIZE, i == _rtasks.length - 1 ? (int) (_nbytes - ReadTask.CHK_SIZE * (_nchks - 1)) : ReadTask.CHK_SIZE);
      rtasks.add(_rtasks[i]);
    }
    ForkJoinTask.invokeAll(rtasks);
  }

  public static class ReadTask extends RecursiveAction {
    int _cidx;
    public byte[] _chk;
    int _bytesRead;
    private static final int CHK_SIZE=1<<(20+2); // 4MB chunk sizes
    final long _off;
    final String _path;
    ReadTask(int cidx, String path, long offset, int bytesRead) {_cidx=cidx;_path=path; _off=offset; _bytesRead=bytesRead;}
    @Override protected void compute() {
      try( FileInputStream s = new FileInputStream(new File(_path))) {
        _chk = new byte[_bytesRead];
        FileChannel fc = s.getChannel();
        fc.position(_off);
        fc.read(ByteBuffer.wrap(_chk));
      } catch( Exception e) {
        System.err.println("chunk: " + _cidx + "; bytesToRead: " + _bytesRead +"; offset: " + _off);
        throw new RuntimeException(e);
      }
    }
  }
}
