package quora;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Stack;

public class Parser {

  static final byte CHAR_TAB = '\t';
  static final byte CHAR_CR = 13;  // \r
  static final byte CHAR_LF = 10;  // \n
  static final byte CHAR_SPACE = ' ';
  static final byte CHAR_DOUBLE_QUOTE = '"';
  static final byte CHAR_SINGLE_QUOTE = '\'';

  protected final byte CHAR_DECIMAL_SEP = '.';
  protected final byte CHAR_SEP;

  Parser(String path, String outpath, char sep) { _path=path; _outpath=outpath; CHAR_SEP=(byte)sep; }

  String _path;
  String _outpath;
  long _nlines;
  void parse() {
    _nlines=1;
    ByteBuffer bb = ByteBuffer.allocate(1<<22).order(ByteOrder.nativeOrder()); // read 4MB at a time
    ByteReader br = new ByteReader(bb);
    try (InputStream is = new FileInputStream(new File(_path)); BufferedWriter bw = new BufferedWriter(new FileWriter(new File(_outpath)))) {
      while( is.read(bb.array(),bb.position(),bb.limit()-bb.position())!=-1 ) {
        br.readLines(bw);
      }
    } catch (IOException e) {
      throw new RuntimeException("failure... lines parsed: " + _nlines);
    }
  }

  // take the stance that CRLF or LF in quotes is not the end of the line!
  // assume that all quotes are matched, otherwise will end up at the EOF
  // with quotes
  class ByteReader {
    ByteBuffer _bb;
    byte[] _lineBytes; // bytes making up a single line
    ByteReader(ByteBuffer bb) {_bb=bb;}
    int readOneLine() {
      int start=_bb.position();
      Stack<Character> quotes = new Stack<>();
      while(_bb.hasRemaining()) {
        int pos = _bb.position();
        byte b = _bb.get();
        switch( b ) {

          // "
          // check if next char is also a quote
          //  => yes then delete both quotes
          // if have a quote, then either match it against the top of the quote stack
          // or push this quote onto the stack
          case CHAR_DOUBLE_QUOTE:
            // two quotes in a row, remove them both!
            if( b==_bb.get(pos+1) ) {
              _bb.array()[pos]   = '\0';
              _bb.array()[pos+1] = '\0';
              _bb.get(); // skip to next byte
            } else {
              if (quotes.isEmpty()) quotes.push((char) b);
              else {
                char c = quotes.peek();
                if ((byte) c == b) quotes.pop();
                else quotes.push((char) b);
              }
            }
            break;
          case CHAR_CR:
            _bb.array()[pos] = '\0';
            break;
          case CHAR_LF:
            _bb.array()[pos] = '\0';
            // not inside of a quoted string and got a \n
            // we are at the end of a line
            if( quotes.isEmpty() ) {
              _lineBytes = new byte[pos - start];
              int lb=0;
              for(int i=start;i<pos;++i) {
                if( _bb.get(i)=='\0' ) continue;
                _lineBytes[lb++] = _bb.get(i);
              }
              _lineBytes = Arrays.copyOf(_lineBytes,lb);
              _nlines++;
              return _bb.position();
            }
        }
      }
      return start;
    }

    void readLines(BufferedWriter bw) throws IOException {
      int start=0;
      _bb.position(0);
      while(_bb.hasRemaining()) {
        start=readOneLine();
        bytesToString(bw);
      }
      // we're here and we're out of bytes for this line
      // compact what we have in bb and go get more from
      // the InputStream
      _bb.position(start);
      _bb.compact();
    }
    void bytesToString(BufferedWriter bw) throws IOException {
      if( _lineBytes==null ) return;
      if( _nlines ==2300 )
        System.out.println();
      String s = new String(_lineBytes, Charset.defaultCharset());
      bw.write(s);
      bw.write("\n");
      _lineBytes=null;
    }
  }

  public static void main(String[] args) {
    String path = "./data/train.csv";
    String outpath = "./data/train_clean.csv";
    Parser ps = new Parser(path, outpath, ',');
    ps.parse();
  }
}
