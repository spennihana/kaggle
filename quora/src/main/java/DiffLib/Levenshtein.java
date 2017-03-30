package DiffLib;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class Levenshtein {

  public static int lev_dist(CharSequence lhs, CharSequence rhs) {
    int len0 = lhs.length() + 1;
    int len1 = rhs.length() + 1;

    // the array of distances
    int[] cost = new int[len0];
    int[] newcost = new int[len0];

    // initial cost of skipping prefix in String s0
    for (int i = 0; i < len0; i++) cost[i] = i;

    // dynamically computing the array of distances

    // transformation cost for each letter in s1
    for (int j = 1; j < len1; j++) {
      // initial cost of skipping prefix in String s1
      newcost[0] = j;

      // transformation cost for each letter in s0
      for(int i = 1; i < len0; i++) {
        // matching current letters in both strings
        int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;

        // computing cost for each transformation
        int cost_replace = cost[i - 1] + match;
        int cost_insert  = cost[i] + 1;
        int cost_delete  = newcost[i - 1] + 1;

        // keep minimum cost
        newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
      }

      // swap cost/newcost arrays
      int[] swap = cost; cost = newcost; newcost = swap;
    }

    // the distance is the cost for transforming all letters in both strings
    return cost[len0 - 1];
  }

  private static int minimum(int a, int b, int c) {
    return Math.min(Math.min(a, b), c);
  }
  static class LevEdit {
    char _t; // i,s,d (insert sub delete)
    int _s;  // source position
    int _d;  // destination position
    LevEdit(char t, int s, int d) { _t=t;_s=s;_d=d; }
    @Override public String toString() { return ""+_t + ": " + _s + " -> " + _d; }
  }
  public static LevEdit[] edit_ops(String s1, String s2) {
    int[][] distance = new int[s1.length() + 1][s2.length() + 1];

    for (int i = 0; i <= s1.length(); i++)
      distance[i][0] = i;
    for (int j = 1; j <= s2.length(); j++)
      distance[0][j] = j;

    for (int i = 1; i <= s1.length(); i++)
      for (int j = 1; j <= s2.length(); j++)
        distance[i][j] = minimum(
          distance[i - 1][j] + 1,
          distance[i][j - 1] + 1,
          distance[i - 1][j - 1] + ((s1.charAt(i - 1) == s2 .charAt(j - 1)) ? 0 : 1));

    // get the edit ops by looping from the bottom right (len1,len2) to (0,0)
    // get the "ancestor" edit by getting the min cost to the left, up, or diag left
    // a move diagonal left indicates either substitution or the letters were the same
    // a move left indicates a insert (stayed in the same row)
    // a move up   indicates an delete

    int x=s1.length(),y=s2.length();
    Stack<LevEdit> edits = new Stack<>();
    while( x>0 || y>0 ) {
      int dc=Integer.MAX_VALUE; // down cost  m=0
      int lc=Integer.MAX_VALUE; // left cost  m=1
      int uc=Integer.MAX_VALUE; // up cost    m=2
      if( x>0 && y>0 ) dc = distance[x-1][y-1];
      if( y > 0      ) lc = distance[x][y-1];
      if( x > 0      ) uc = distance[x-1][y];

      int m=-1;
      if(      dc<=uc && dc<=lc ) m=0; // min is dc
      else if( lc<=uc && lc<=dc ) m=1; // min is lc
      else if( uc<=lc && uc<=dc ) m=2; // min is uc

      switch(m) {
        case 0: // move diagonal left
          x--;y--;
          char c1 = s1.charAt(x);
          char c2 = s2.charAt(y);
          if( c1!=c2 ) edits.push(new LevEdit('s',x,y));
          break;
        case 1: // move left => insert
          y--;
          edits.push(new LevEdit('i',x,y));
          break;
        case 2: // move up, delete
          x--;
          edits.push(new LevEdit('d',x,y));
          break;
        default:
          throw new RuntimeException("Bad m: " + m);
      }
    }

    LevEdit[] e = new LevEdit[edits.size()];
    int i=0;
    while(!edits.isEmpty())e[i++]=edits.pop();
    return e;
  }

  public static int[][] matching_groups(LevEdit[] editops, String s1, String s2) {
    // matching blocks between edits
    int minlen = Math.min(s1.length(), s2.length());
    ArrayList<int[]> groups = new ArrayList<>();
    int lb=0, ub=0;


    int s=0,d=0;
    int o=0;
    for(int i=editops.length;i!=0;) {
      LevEdit op = editops[o];
      if( s < op._s || d < op._d ) {
        int[] group = new int[]{s,d,op._s -s};
        s=op._s;
        d=op._d;
        groups.add(group);
      }
      char type=op._t;
      switch( type ) {
        case 's':
          do {
            op=editops[o++];
            s++;
            d++;
            i--;
          } while(i!=0 && op._t==type && s==op._s && d==op._d);
          break;
        case 'd':
          do {
            op=editops[o++];
            s++;
            i--;
          } while(i!=0 && op._t==type && s==op._s && d==op._d);
          break;
        case 'i':
          do {
            op=editops[o++];
            d++;
            i--;
          } while(i!=0 && op._t==type && s==op._s && d==op._d);
          break;
      }
    }
    if( s<s1.length() || d<s2.length() ) {
      int[] group = new int[]{s,d,s1.length()-s};
      groups.add(group);
    }

    groups.add(new int[]{s1.length(),s2.length(),0});
    return groups.toArray(new int[groups.size()][]);
  }

  static void mat2out(int[][] mat) {
    for(int r=0;r<mat.length;++r) {
      System.out.println(Arrays.toString(mat[r]));
    }
  }

  public static double ratio(String s1, String s2) { return lev_dist(s1,s2) / (double)(s1.length()+s2.length()); }

  public static void main(String[] args) {

    String s1 = "f9f3ljhf alfjd -- asdkf kssaaa";
    String s2 = "foobar and boo asdffdsa";

    test(s1,s2);
    assert lev_dist("Levenshtein", "Lenvinsten") == 4;
    assert lev_dist("Levenshtein", "Levensthein") == 2;
    assert lev_dist("Levenshtein", "Levenshten") == 1;
    assert lev_dist("Levenshtein", "Levenshtein") == 0;

    s1="sitting"; s2="kitten";
    LevEdit[] e = edit_ops(s1,s2);
    mat2out(matching_groups(e,s1,s2));
    s1="dfaaa"; s2="dgaaa";
    e = edit_ops(s1,s2);
    System.out.println(Arrays.toString(e));
    mat2out(matching_groups(e,s1,s2));

    s1="hello my asdf"; s2="foo asdf eeee";
    e = edit_ops(s1,s2);
    System.out.println(Arrays.toString(e));
    mat2out(matching_groups(e,s1,s2));


    s1 = "javax.servlet.http.HttpServlet.service(HttpServlet.java:848)";
    s2 = "javax.servlet.http.HttpServlet.service(HttpServlet.java:735)";
    e = edit_ops(s1,s2);
    System.out.println(Arrays.toString(e));
    mat2out(matching_groups(e,s1,s2));

      // When the values are the same, the distance is zero.
    TestCase[] testCases = new TestCase[]{
      new TestCase("","",0,"", "000"),
      new TestCase("1","1",0,"", "001110"),
      new TestCase("12","12",0,"", "002220"),
      new TestCase("123","123",0,"", "003330"),
      new TestCase("1234","1234",0,"", "004440"),
      new TestCase("12345","12345",0,"", "005550"),
      new TestCase("passwosd","passwosd",0,"", "008880"),
      new TestCase("","1",1,"i00", "010"),
      new TestCase("","12",2,"i00i01", "020"),
      new TestCase("","123",3,"i00i01i02", "030"),
      new TestCase("","1234",4,"i00i01i02i03", "040"),
      new TestCase("","12345",5,"i00i01i02i03i04", "050"),
      new TestCase("","passwosd",8,"i00i01i02i03i04i05i06i07", "080"),
      new TestCase("1","",1,"d00", "100"),
      new TestCase("12","",2,"d00d10", "200"),
      new TestCase("123","",3,"d00d10d20", "300"),
      new TestCase("1234","",4,"d00d10d20d30", "400"),
      new TestCase("12345","",5,"d00d10d20d30d40", "500"),
      new TestCase("passwosd","",8,"d00d10d20d30d40d50d60d70", "800"),
      new TestCase("passwosd","1passwosd",1,"i00", "018890"),
      new TestCase("passwosd","p1asswosd",1,"i11", "001127890"),
      new TestCase("passwosd","pa1sswosd",1,"i22", "002236890"),
      new TestCase("passwosd","pas1swosd",1,"i33", "003345890"),
      new TestCase("passwosd","pass1wosd",1,"i44", "004454890"),
      new TestCase("passwosd","passw1osd",1,"i55", "005563890"),
      new TestCase("passwosd","passwo1sd",1,"i66", "006672890"),
      new TestCase("passwosd","passwos1d",1,"i77", "007781890"),
      new TestCase("passwosd","passwosd1",1,"i88", "008890"),
      new TestCase("passwosd","asswosd",1,"d00", "107870"),
      new TestCase("passwosd","psswosd",1,"d11", "001216870"),
      new TestCase("passwosd","paswosd",1,"d33", "003434870"),
      new TestCase("passwosd","paswosd",1,"d33", "003434870"),
      new TestCase("passwosd","passosd",1,"d44", "004543870"),
      new TestCase("passwosd","passwsd",1,"d55", "005652870"),
      new TestCase("passwosd","passwod",1,"d66", "006761870"),
      new TestCase("passwosd","passwos",1,"d77", "007870"),
      new TestCase("passwosd","Xasswosd",1,"s00", "117880"),
      new TestCase("passwosd","pXsswosd",1,"s11", "001226880"),
      new TestCase("passwosd","paXswosd",1,"s22", "002335880"),
      new TestCase("passwosd","pasXwosd",1,"s33", "003444880"),
      new TestCase("passwosd","passXosd",1,"s44", "004553880"),
      new TestCase("passwosd","passwXsd",1,"s55", "005662880"),
      new TestCase("passwosd","passwoXd",1,"s66", "006771880"),
      new TestCase("passwosd","passwosX",1,"s77", "007880"),
      new TestCase("12345678","23456781",2,"d00i87", "107880"),
      new TestCase("12345678","34567812",4,"d00d10i86i87", "206880"),
      new TestCase("12345678","45678123",6,"d00d10d20i85i86i87", "305880"),
      new TestCase("12345678","56781234",8,"s00s11s22s33s44s55s66s77", "880"),
      new TestCase("12345678","67812345",6,"i00i01i02d58d68d78", "035880"),
      new TestCase("12345678","78123456",4,"i00i01d68d78", "026880"),
      new TestCase("12345678","81234567",2,"i00d78", "017880"),
      new TestCase("12","21",2,"s00s11", "220"),
      new TestCase("123","321",2,"s00s22", "111330"),
      new TestCase("1234","4321",4,"s00s11s22s33", "440"),
      new TestCase("12345","54321",4,"s00s11s33s44", "221550"),
      new TestCase("123456","654321",6,"s00s11s22s33s44s55", "660"),
      new TestCase("1234567","7654321",6,"s00s11s22s44s55s66", "331770"),
      new TestCase("12345678","87654321",8,"s00s11s22s33s44s55s66s77", "880"),
      new TestCase("Mississippi","ippississiM",6,"i00i01s02d810d910s1010", "13711110"),
      new TestCase("eieio","oieie",2,"s00s44", "113550"),
      new TestCase("bsad+angelina","bsangelina",3,"d33d43d53", "00363713100"),
      new TestCase("?e?uli?ka","e?uli?ka",1,"d00", "108980"),
      new TestCase("","",0,"", "000"),
      new TestCase("a","",1,"d00", "100"),
      new TestCase("","a",1,"i00", "010"),
      new TestCase("abc","",3,"d00d10d20", "300"),
      new TestCase("","abc",3,"i00i01i02", "030"),
      new TestCase("","",0,"", "000"),
      new TestCase("a","a",0,"", "001110"),
      new TestCase("abc","abc",0,"", "003330"),
      new TestCase("","a",1,"i00", "010"),
      new TestCase("a","ab",1,"i11", "001120"),
      new TestCase("b","ab",1,"i00", "011120"),
      new TestCase("ac","abc",1,"i11", "001121230"),
      new TestCase("abcdefg","xabxcdxxefxgx",6,"i00i23i46i47i610i712", "01224248261117130"),
      new TestCase("a","",1,"d00", "100"),
      new TestCase("ab","a",1,"d11", "001210"),
      new TestCase("ab","b",1,"d00", "101210"),
      new TestCase("abc","ac",1,"d11", "001211320"),
      new TestCase("xabxcdxxefxgx","abcdefg",6,"d00d32d64d74d106d127", "10242284211611370"),
      new TestCase("a","b",1,"s00", "110"),
      new TestCase("ab","ac",1,"s11", "001220"),
      new TestCase("ac","bc",1,"s00", "111220"),
      new TestCase("abc","axc",1,"s11", "001221330"),
      new TestCase("xabxcdxxefxgx","1ab2cd34ef5g6",6,"s00s33s66s77s1010s1212", "1124428821111113130"),
      new TestCase("example","samples",3,"d00s10i76", "215770"),
      new TestCase("stusgeon","usgently",6,"d00d10i64i65s66s77", "204880"),
      new TestCase("levenshtein","fsankenstein",6,"i00i01s02s13s24d68", "35378411120"),
      new TestCase("distance","diffesence",5,"i22i23s24s35s46", "0025738100"),
      new TestCase("java was neat","scala is gseat",7,"i00s01s23d56s66i99s910", "1213427721011313140"),
    };


    for(TestCase tc: testCases) {
      assert lev_dist(tc._s1, tc._s2) == tc._d;
    }

    for(TestCase tc: testCases) {
      LevEdit[] ops = edit_ops(tc._s1,tc._s2);
      assert opstr(ops).equals(tc._opstr) : tc + " != " + opstr(ops);
      int[][] groups = matching_groups(ops,tc._s1,tc._s2);
      assert groupstr(groups).equals(tc._groups) : tc + " != " + groupstr(groups);
    }
  }

  static String groupstr(int[][] groups) {
    StringBuilder sb = new StringBuilder();
    for(int i=0;i<groups.length;++i) {
      for(int j=0;j<groups[i].length;++j) {
        sb.append(groups[i][j]);
      }
    }
    return sb.toString();
  }
  static String opstr(LevEdit[] ops) {
    StringBuilder sb = new StringBuilder();
    for(LevEdit o: ops) {
      sb.append(o._t).append(o._s).append(o._d);
    }
    return sb.toString();
  }

  static class TestCase {
    String _s1;
    String _s2;
    int _d;
    String _opstr;
    String _groups;
    TestCase(String s1, String s2, int d, String opStr, String groups) { _s1=s1; _s2=s2; _d=d; _opstr=opStr; _groups=groups; }
    @Override public String toString() { return "s1=" + _s1 +"; s2=" + _s2 + "; d="+ _d + "; opstr=" + _opstr + "; groups=" + _groups; }
  }

  static int test(String s1, String s2) {
    int d = lev_dist(s1,s2);
    System.out.println(d);
    return d;
  }
}