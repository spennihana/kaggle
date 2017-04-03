package quora;


import water.ParallelCsvRead;
import water.nbhm.NonBlockingHashMap;
import water.parser.BufferedString;
import water.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

import static water.ParallelCsvRead.ReadTask;

public class WordEmbeddings {

  private ParseBytesTask[] _pbTasks;
  NonBlockingHashMap<BufferedString, double[]> _embeddings;

  public static WordEmbeddings read(String path) {
    WordEmbeddings em = new WordEmbeddings();
    ParallelCsvRead pcsv = new ParallelCsvRead(path);
    long start = System.currentTimeMillis();
    long elapsed;
    long elapsed2;
    pcsv.raw_parse(); // suck in the raw bytes
    elapsed = System.currentTimeMillis() - start;
    Log.info("Read raw bytes in to RAM in " + elapsed/1000. + " seconds" );
    start = System.currentTimeMillis();
    em.parse_bytes(pcsv._rtasks);
    elapsed2 = System.currentTimeMillis() - start;
    Log.info("Parsed word embeddings in " + elapsed2/1000. + " seconds");
    Log.info("Total embeddings parse time: " + (elapsed + elapsed2)/1000. +  " seconds");
    em._embeddings = new NonBlockingHashMap<>();
    for(ParseBytesTask pbt: em._pbTasks) {
      em._embeddings.putAll(pbt._rows);
      pbt._rows=null;
    }
    em._pbTasks=null;
    return em;
  }

  public double[] get(String w) {
    return _embeddings.get(new BufferedString(w));
  }

  private static class FindWordTask extends RecursiveAction {
    final ParseBytesTask[] _pbtasks;
    final BufferedString _bstr;
    final int _lo,_hi;
    double[] _result;
    FindWordTask _next; // rhs tasks
    FindWordTask(BufferedString bstr, ParseBytesTask[] pbtasks, int lo, int hi, FindWordTask next) {
      _lo=lo; _hi=hi;
      _pbtasks=pbtasks;
      _bstr=bstr;
      _next=next;
    }

    @Override protected void compute() {
      int l=_lo;
      int h=_hi;
      FindWordTask right = null;
      while( h-l > 1 && getSurplusQueuedTaskCount() <= 3 ) {
        int mid = (l+h)>>>1;
        right = new FindWordTask(_bstr,_pbtasks,mid,h,right);
        right.fork();
        h=mid;
      }
      double[] res = search(l,h);
      if( res!=null ) {
        _result=res;
        return;
      }
      while( right!=null ) {
        if( right.tryUnfork() ) {
          res = right.search(right._lo, right._hi);
          if( res!=null ) {
            _result=res;
            return;
          }
        } else {
          right.join();
          res = right._result;
          if( res!=null ) {
            _result=res;
            return;
          }
        }
        right=right._next;
      }
      _result= null;
    }

    double[] search(int l, int h) {
      for(int i=l;i<h;++i) {
        double[] r = _pbtasks[i]._rows.get(_bstr);
        if( r!=null ) return r;
      }
      return null;
    }
  }


  private WordEmbeddings parse_bytes(ReadTask[] rtasks) {
    _pbTasks = new ParseBytesTask[rtasks.length];
    ArrayList<ParseBytesTask> pbtasks = new ArrayList<>();
    for(int i=0;i<_pbTasks.length;++i) {  //_pbtasks.length
      _pbTasks[i] = new ParseBytesTask(rtasks[i],i<rtasks.length-1?rtasks[i+1]._chk:null,i,(byte)' ');
      pbtasks.add(_pbTasks[i]);
    }
    ForkJoinTask.invokeAll(pbtasks);
    return this;
  }

  // internal parallel parsing bits
  private static class ParseBytesTask extends RecursiveAction {
    private static final int[] c2i = new int[58]; // need 48-57 inclusive
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
    static final byte CHAR_SPACE = ' ';
    static final byte CHAR_DASH = '-';
    static final byte CHAR_ZERO = '0';
    static final byte CHAR_DECIMAL = '.';
    final byte CHAR_SEP;

    byte[] _in;
    byte[] _nextBits; // the next byte array over (null if final chunk)

    int _cidx;
    public HashMap<BufferedString, double[]> _rows;
    ParseBytesTask(ParallelCsvRead.ReadTask byteArray, byte[] next_bytes, int cidx, byte sep) {
      _in=byteArray._chk;
      byteArray._chk=null; // free the pointer from byteArray...
      CHAR_SEP=sep;
      _nextBits=next_bytes;
      _cidx=cidx;
    }

    // read a string followed by 300 doubles
    @Override protected void compute() {
      _rows = new HashMap<>();
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
          b= _in[npos];
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
      int x;
      while( --i>=start ) { // skip the white space by decrementing i
        x=i;
        while( i>= start && bits[i]!=CHAR_SPACE ) i--;
        int fidx=0;
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
          assert bits[ii]==CHAR_ZERO;
          ii++;
          assert bits[ii]==CHAR_DECIMAL;
          ii++;
          for(;ii<=x;++ii) {
            d += c2i[bits[ii]] / di;
            di *= 10.;
          }
          if( !pos ) d*=-1;
          em[em_idx--]=d;
        } else {
          byte[] field = new  byte[x-i];
          for (int ii = i + 1; ii <= x; ++ii)
            field[fidx++] = bits[ii];
          _rows.put(new BufferedString(field,0,field.length),em);
          break;
        }
      }
      assert em_idx <= 0;
      assert i < start;
    }
  }

  public static void main(String[] args) {
    WordEmbeddings we = WordEmbeddings.read("./lib/w2vec_models/gw2vec");
    double[] res = we.get("</s>");
    System.out.println(Arrays.toString(res));
    System.out.println("foo");
  }
}
