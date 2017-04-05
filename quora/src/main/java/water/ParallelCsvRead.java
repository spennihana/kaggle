package water;

import water.parser.BufferedString;

import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

import static water.Value.NFS;

public class ParallelCsvRead {
  private static final ForkJoinPool FJPOOL = ForkJoinPool.commonPool();
  public ReadTask[] _rtasks;
  public ParseBytesTask[] _pbtasks;
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

  // do something with the byte arrays
  public void parse_bytes() {
    _pbtasks = new ParseBytesTask[_rtasks.length];
    ArrayList<ParseBytesTask> pbtasks = new ArrayList<>();
    for(int i=0;i<_pbtasks.length;++i) {
      _pbtasks[i] = new ParseBytesTask(_rtasks[i],i<_rtasks.length-1?_rtasks[i+1]._chk:null,i,(byte)' ');
      pbtasks.add(_pbtasks[i]);
    }
    ForkJoinTask.invokeAll(pbtasks);
  }

  public void parse_glove() {
    _pbtasks = new GloveBytesTask[_rtasks.length];
    ArrayList<ParseBytesTask> pbtasks = new ArrayList<>();
    for(int i=0;i<_pbtasks.length;++i) {
      _pbtasks[i] = new GloveBytesTask(_rtasks[i],i<_rtasks.length-1?_rtasks[i+1]._chk:null,i,(byte)' ');
      pbtasks.add(_pbtasks[i]);
    }
    ForkJoinTask.invokeAll(pbtasks);
  }

  public void raw_parse() {
    for(int i=0;i<_rtasks.length;++i)
      _rtasks[i] = new ReadTask(i,_path, (long)i * (long)ReadTask.CHK_SIZE, i == _rtasks.length - 1 ? (int) (_nbytes - ReadTask.CHK_SIZE * (_nchks - 1)) : ReadTask.CHK_SIZE);
    FJPOOL.invoke(new ThrottledForkTask(_rtasks,0,_rtasks.length,null));
  }

  public static class ParseBytesTask extends RecursiveAction {

    protected static final int[] c2i = new int[128];
    static {
      c2i['0']=0;
      c2i['1']=1;
      c2i['2']=2;
      c2i['3']=3;
      c2i['4']=4;
      c2i['5']=5;
      c2i['6']=6;
      c2i['7']=7;
      c2i['8']=8;
      c2i['9']=9;
    }

    static final byte CHAR_CR = 13;  // \r
    static final byte CHAR_LF = 10;  // \n
    static final byte CHAR_DASH = '-';
    static final byte CHAR_SPACE = ' ';
    static final byte CHAR_DOT = '.';
    static final byte CHAR_ZERO = '0';
    protected final byte CHAR_SEP;

    byte[] _in;
    byte[] _nextBits; // the next byte array over (null if final chunk)

    int _cidx;
    public HashMap<BufferedString, double[]> _rows;
    int _max;
    ParseBytesTask(ReadTask byteArray, byte[] next_bytes, int cidx, byte sep) {
      _in=byteArray._chk;
      byteArray._chk=null; // free the pointer from byteArray...
      CHAR_SEP=sep;
      _nextBits=next_bytes;
      _cidx=cidx;
    }

    // read a string followed by 300 doubles
    @Override protected void compute() {
      _rows = new HashMap<>();
      _max = Integer.MIN_VALUE;
      int pos=0;
      byte b;
      if( _cidx!=0 ) { // if not the first chk, means another thread already parsed these bits
        while ((b = _in[pos++]) != CHAR_CR && b != CHAR_LF) ; // loop up to CR or LF, and go one byte past
        b = _in[pos];
        if (b == CHAR_LF) pos++; // could be that last byte read was a CR, so skip this byte
      }
      // now the fun begins!
      while(pos < _in.length) {
        int start = pos;
        while( pos < _in.length && (b = _in[pos]) != CHAR_CR && b!= CHAR_LF ) pos++;
        if(pos == _in.length && _nextBits!=null) { // out of bytes, always read the boundary line
          // read _nextBits until newline into a special byte[] here
          int npos=0;
          while( (b=_nextBits[npos++])!=CHAR_CR && b!=CHAR_LF );
          b= _nextBits[npos];
          if( b==CHAR_LF ) npos++;
          byte[] boundaryLine = new byte[(pos-start) + npos];
          System.arraycopy(_in,start,boundaryLine,0,pos-start);
          System.arraycopy(_nextBits,0,boundaryLine,pos-start,npos);
          captureLine(0,boundaryLine.length-1,boundaryLine);
          break;
        }
        captureLine(start,pos++,_in);
      }
      _in=null; // free the last pointer
      _nextBits=null;
    }

    void captureLine(int start, int end, byte[] bits) {
      int em_idx=299;
      double[] em = new double[300];
      int i=end;
      i--; // skip the LF
      i--; // skip a \b
      i--; // skip another \b
      int x=-1;
      while( --i>=start ) { // skip the white space by decrementing i
        x=i;
        if( em_idx< 0 ) break;
        while( i>= start && bits[i]!=CHAR_SPACE ) i--;
        if( em_idx>=0 ) {
          double d=0;
          // parse each byte into a digit...
          double di=10; // divide by powers of 10 according to digit index
          boolean pos=true; // positve or negative
          int ii=i+1;
          if( bits[ii]==CHAR_DASH ) {
            pos=false;
            ii++;
          }
          assert bits[ii]==CHAR_ZERO : new String(Arrays.copyOfRange(bits,i+1,x)) + "; full bits: " + new String(Arrays.copyOfRange(bits,start,end-1));
          ii++;
          assert bits[ii]==CHAR_DOT : new String(Arrays.copyOfRange(bits,i+1,x)) + "; full bits: " + new String(Arrays.copyOfRange(bits,start,end-1));
          ii++;
          for(;ii<=x;++ii) {
            d += c2i[bits[ii]] / di;
            di *= 10.;
          }
          if( !pos ) d=-d;
          em[em_idx--]=d;
        }
      }
      i=start-1;
      int fidx=0;
      assert x > 0 && x > i;
      byte[] field = new  byte[x-i];
      for (int ii = i + 1; ii <= x; ++ii)
        field[fidx++] = bits[ii];
      _rows.put(new BufferedString(field,0,field.length),em);
      _max = Math.max(field.length,_max);
      assert em_idx <= 0;
      assert i < start;
    }
  }

  public static class GloveBytesTask extends ParseBytesTask {
    GloveBytesTask(ReadTask byteArray, byte[] next_bytes, int cidx, byte sep) {
      super(byteArray, next_bytes, cidx, sep);
    }

    @Override protected void compute() {super.compute();}
    @Override void captureLine(int start, int end, byte[] bits) {
      int em_idx=299;
      double[] em = new double[300];
      int i=end;
      i--; // skip the LF
      if( bits[i]=='\b') i--; // skip a \b
      if( bits[i]=='\b') i--; // skip another \b
      int x=-1;
      while( i>=start ) { // skip the white space by decrementing i
        x=i;
        if( em_idx <0 ) break;
        boolean sci=false;
        while( i>= start && bits[i]!=CHAR_SPACE ) {
          sci |= bits[i]=='e';
          i--;
        }
        int fidx=0;
        if( em_idx>=0 ) {
          if (sci) {
            em[em_idx--] = Double.parseDouble(new String(Arrays.copyOfRange(bits,i+1,x+1)));
          } else {
            double d = 0;
            // parse each byte into a digit...
            double di = 10; // divide by powers of 10 according to digit index
            boolean pos = true; // positve or negative
            int ii = i + 1;
            if (bits[ii] == CHAR_DASH) {
              pos = false;
              ii++;
            }
            d = c2i[bits[ii]];
            ii++;
            if (ii < x) {
              assert bits[ii] == CHAR_DOT : new String(Arrays.copyOfRange(bits, i + 1, x)) + "; em_idx=" + em_idx + ";  full bits: " + new String(Arrays.copyOfRange(bits, start, end - 1));
              ii++;
              for (; ii <= x; ++ii) {
                d += c2i[bits[ii]] / di;
                di *= 10.;
              }
            }
            if (!pos) d = -d;
            em[em_idx--] = d;
          }
        }
        i--;
      }
      int fidx=0;
      i=start-1;
      assert x > 0 && x > i;
      byte[] field = new  byte[x-i];
      for (int ii = i + 1; ii <= x; ++ii)
        field[fidx++] = bits[ii];
      _rows.put(new BufferedString(field,0,field.length),em);
      _max = Math.max(field.length,_max);
      assert em_idx <= 0;
      assert i < start;
    }
  }

  static class ReadTask extends RecursiveAction {
    int _cidx;
    byte[] _chk;
    int _bytesRead;
    private static final int CHK_SIZE=1<<(20+2); // 4MB chunk sizes
    final long _off;
    final String _path;
    ReadTask(int cidx, String path, long offset, int bytesRead) {_cidx=cidx;_path=path; _off=offset; _bytesRead=bytesRead;}
    @Override protected void compute() {
      try( FileInputStream s = new FileInputStream(new File(_path))) {
        FileChannel fc = s.getChannel();
        fc.position(_off);
        AutoBuffer ab = new AutoBuffer(fc,true, NFS);
        _chk = ab.getA1(_bytesRead);
        ab.close();
      } catch( Exception e) {
        System.err.println("chunk: " + _cidx + "; bytesToRead: " + _bytesRead +"; offset: " + _off);
        throw new RuntimeException(e);
      }
    }
  }

  static class ThrottledForkTask extends RecursiveAction {
    final RecursiveAction[] _tasks;
    final int _lo, _hi;
    ThrottledForkTask _next;
    ThrottledForkTask(RecursiveAction[] tasks, int lo, int hi, ThrottledForkTask next) {
      _tasks=tasks;
      _lo=lo;
      _hi=hi;
      _next=next;
    }

    void leaf(int l, int h) {
      ArrayList<RecursiveAction> tasks = new ArrayList<>();
      tasks.addAll(Arrays.asList(_tasks).subList(l, h));
      ForkJoinTask.invokeAll(tasks);
    }

    @Override protected void compute() {
      int l=_lo;
      int h=_hi;
      ThrottledForkTask right = null;
      while( h-l > 1 && getSurplusQueuedTaskCount() <= 3 ) {
        int mid = (l+h) >>> 1;
        right = new ThrottledForkTask(_tasks,mid,h,right);
        right.fork();
        h=mid;
      }
      leaf(_lo,_hi);
      while( right !=null ) {
        if( right.tryUnfork() ) {
          right.leaf(right._lo,right._hi);
        } else {
          right.join();
        }
        right=right._next;
      }
    }
  }

  public static void main(String[] args) {
//    ParallelCsvRead r = new ParallelCsvRead("./lib/w2vec_models/gw2vec");
    ParallelCsvRead r = new ParallelCsvRead("./lib/w2vec_models/glove.840B.300d.txt");
    long s = System.currentTimeMillis();
    r.raw_parse();
    System.out.println("Raw disk read in " + (System.currentTimeMillis() - s)/1000. + " seconds");
    s=System.currentTimeMillis();
//    r.parse_bytes();
    r.parse_glove();
    System.out.println("Parse bytes in " + (System.currentTimeMillis() - s)/1000. + " seconds");
    int max = r._pbtasks[0]._max;
    for(int i=1;i<r._pbtasks.length;++i) {
      max = Math.max(max, r._pbtasks[i]._max);
    }
    System.out.println("MAX STR LEN: " + max);
    int nrows=0;
    for(int i=0;i<r._pbtasks.length;++i) {
      nrows += r._pbtasks[i]._rows.size();
    }
    System.out.println(nrows + " rows read");
    System.out.println();
  }
}
