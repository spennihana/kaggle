package quora;


import edu.emory.mathcs.nlp.common.util.IOUtils;
import edu.emory.mathcs.nlp.common.util.Language;
import edu.emory.mathcs.nlp.component.template.NLPComponent;
import edu.emory.mathcs.nlp.component.template.feature.Field;
import edu.emory.mathcs.nlp.component.template.lexicon.GlobalLexica;
import edu.emory.mathcs.nlp.component.template.lexicon.GlobalLexicon;
import edu.emory.mathcs.nlp.component.template.node.NLPNode;
import edu.emory.mathcs.nlp.component.tokenizer.Tokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.token.Token;
import edu.emory.mathcs.nlp.decode.NLPDecoder;
import info.debatty.java.stringsimilarity.*;
import info.debatty.java.stringsimilarity.interfaces.StringDistance;
import water.*;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.NewChunk;
import water.fvec.Vec;
import water.parser.BufferedString;
import water.util.ArrayUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ObjectInputStream;
import java.util.*;

import static water.KaggleUtils.importParseFrame;

public class Preprocess extends MRTask<Preprocess> {

  private static String contractionMap(String s) {
    if( s.equals("aren't") ) return "are not";
    if( s.equals("can't") ) return "cannot";
    if( s.equals("couldn't") ) return "could not";
    if( s.equals("didn't") ) return "did not";
    if( s.equals("doesn't") ) return "does not";
    if( s.equals("don't") ) return "do not";
    if( s.equals("hadn't") ) return "had not";
    if( s.equals("hasn't") ) return "has not";
    if( s.equals("haven't") ) return "have not";
    if( s.equals("he'd") ) return "he had";
    if( s.equals("he'll") ) return "he will";
    if( s.equals("he's") ) return "he is";
    if( s.equals("I'd") ) return "I had";
    if( s.equals("I'll") ) return "I will";
    if( s.equals("I'm") ) return "I am";
    if( s.equals("I've") ) return "I have";
    if( s.equals("isn't") ) return "is not";
    if( s.equals("let's") ) return "let us";
    if( s.equals("mightn't") ) return "might not";
    if( s.equals("mustn't") ) return "must not";
    if( s.equals("shan't") ) return "shall not";
    if( s.equals("she'd") ) return "she had";
    if( s.equals("she'll") ) return "she will";
    if( s.equals("she's") ) return "she is";
    if( s.equals("shouldn't") ) return "should not";
    if( s.equals("that's") ) return "that is";
    if( s.equals("there's") ) return "there is";
    if( s.equals("they'd") ) return "they had";
    if( s.equals("they'll") ) return "they will";
    if( s.equals("they're") ) return "they are";
    if( s.equals("they've") ) return "they have";
    if( s.equals("we'd") ) return "we had";
    if( s.equals("we're") ) return "we are";
    if( s.equals("we've") ) return "we have";
    if( s.equals("weren't") ) return "were not";
    if( s.equals("what'll") ) return "what will";
    if( s.equals("what're") ) return "what are";
    if( s.equals("what's") ) return "what is";
    if( s.equals("what've") ) return "what have";
    if( s.equals("where's") ) return "where is";
    if( s.equals("who's") ) return "who had; who would";
    if( s.equals("who'll") ) return "who will";
    if( s.equals("who're") ) return "who are";
    if( s.equals("who's") ) return "who is";
    if( s.equals("who've") ) return "who have";
    if( s.equals("won't") ) return "will not";
    if( s.equals("wouldn't") ) return "would not";
    if( s.equals("you'd") ) return "you had";
    if( s.equals("you'll") ) return "you will";
    if( s.equals("you're") ) return "you are";
    if( s.equals("you've") ) return "you have";
    return s;
  }

  private static final int NDIST_METRICS=11;
  private static final int ID=0;
  private final int Q1;
  private final int Q2;
  private final boolean _test;

  transient Tokenizer _tok;
  transient NLPComponent _pos;
  transient GlobalLexica _lex;

  transient StringDistance[] _simMetrics;

  static final String[] NAMES = new String[]{
    "id",  // the row id
    // some basic feature computations on the raw sentences
    "q1_words", "q2_words", "abs_words","common_words","fuzzy_matched","common_caps","q1_chars","q1_fuzzy_chars","q2_chars","q2_fuzzy_chars","abs_chars","abs_fuzzy_chars","q1_punc_count","q2_punc_count","q1_caps","q2_caps","abs_caps",
    // some metrics between the simplified sentences
    "cosine","dameru","jaccard","jwink","leven","lcsub","ngram","leven_norm","optim_align","qgram","sdice",
    // metrics on the POS tags
    "cosine_pos","dameru_pos","jaccard_pos","jwink_pos","leven_pos","lcsub_pos","ngram_pos","leven_norm_pos","optim_align_pos","qgram_pos","sdice_pos",
    // word embedding metrics
    "wes1_sum","wes2_sum",   // word-emedding vectors summed (not including stop words), then the vector is summed
    "wess1_sum","wess2_sum", // word-embedding vectors RMS and summed
    "abs_wes_sum", "abs_wess_sum", // absolute differences of the above
    "wes_cosine", "wess_cosine"    // the cosine distances for each flavor
  };

  String[] getNames() {
    if( _test ) return NAMES;
    else{
      String[] names = Arrays.copyOf(NAMES,NAMES.length+1);
      names[names.length-1] = "is_duplicate";
      return names;
    }
  }

  Preprocess(boolean test) {
    _test=test;
    Q1=_test?1:3;
    Q2=_test?2:4;
  }

  @Override public void setupLocal() {
    org.w3c.dom.Document d=null;
    try {
      d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }

    _tok=edu.emory.mathcs.nlp.common.util.NLPUtils.createTokenizer(Language.ENGLISH);
    System.out.println("Tokenizer loaded");
    // part of speech tagger
    _pos=edu.emory.mathcs.nlp.common.util.NLPUtils.getComponent("edu/emory/mathcs/nlp/models/en-pos.xz");
    System.out.println("POS tagger loaded");
    _lex =new GlobalLexica(d.createElement("mocked"));
    // load lexica
//    _lex.setAmbiguityClasses(getLex("edu/emory/mathcs/nlp/lexica/en-ambiguity-classes-simplified-lowercase.xz", Field.word_form_simplified_lowercase));
//    System.out.println("Ambiguity classes loaded");
//    _lex.setWordClusters(getLex("edu/emory/mathcs/nlp/lexica/en-brown-clusters-simplified-lowercase.xz", Field.word_form_simplified_lowercase));
//    System.out.println("Word clusters loaded");
    _lex.setWordEmbeddings(getLex("edu/emory/mathcs/nlp/lexica/en-word-embeddings-undigitalized.xz", Field.word_form_undigitalized));
    System.out.println("Word embeddings loaded");
//    _lex.setNamedEntityGazetteers(getLex("edu/emory/mathcs/nlp/lexica/en-named-entity-gazetteers-simplified-lowercase.xz", Field.word_form_simplified));
//    System.out.println("named entity gazetteers loaded");
    _lex.setStopWords(getLex("edu/emory/mathcs/nlp/lexica/en-stop-words-simplified-lowercase.xz", Field.word_form_simplified_lowercase));
    System.out.println("stop words loaded");

    // init all the string distance measures one time here
    _simMetrics = new StringDistance[NDIST_METRICS];
    _simMetrics[0] = new Cosine();
    _simMetrics[1] = new Damerau();
    _simMetrics[2] = new Jaccard();
    _simMetrics[3] = new JaroWinkler();
    _simMetrics[4] = new Levenshtein();
    _simMetrics[5] = new LongestCommonSubsequence();
    _simMetrics[6] = new NGram();
    _simMetrics[7] = new NormalizedLevenshtein();
    _simMetrics[8] = new OptimalStringAlignment();
    _simMetrics[9] = new QGram();
    _simMetrics[10]= new SorensenDice();

    System.out.println("Distance metrics initialized");


//    Word2Vec w2v = WordVectorSerializer.readWord2VecModel("");

  }


  static <T> GlobalLexicon<T> getLex(String path, Field f) {
    T lex;
    try(ObjectInputStream oin = IOUtils.createArtifactObjectInputStream(path)) {
      lex=(T)oin.readObject();
    } catch( Exception e) {
      throw new RuntimeException(e);
    }
    return new GlobalLexicon(lex, f,"");
  }

  @Override public void map(Chunk[] cs, NewChunk[] ncs) {
    NLPDecoder nlpd = new NLPDecoder();
    BufferedString bstr = new BufferedString();
    double[] dists = new double[NDIST_METRICS];  // on fuzzed words (no punc, lower case, no stops)
    double[] dists2= new double[NDIST_METRICS];  // on POS tags
    float[] we_s1 = new float[50]; // word embeddings vector is length 50
    float[] we_ss1= new float[50]; // sum squared of word embeddings

    float[] we_s2 = new float[50]; // word embeddings vector is length 50
    float[] we_ss2= new float[50]; // sum squared of word embeddings
    for(int r=0;r<cs[0]._len;++r) {
      String q1=null;
      String q2=null;
      String[] w1=null;
      String[] w2=null;
      String[] f1=null;
      String[] f2=null;
      String sf1=null;
      String sf2=null;
      try {
        int ncs_idx=0;
        int id = (int) cs[ID].at8(r);
        ncs[ncs_idx++].addNum(id);

        q1 = cs[Q1].isNA(r)?"":cs[Q1].atStr(bstr, r).toString();
        q2 = cs[Q2].isNA(r)?"":cs[Q2].atStr(bstr, r).toString();
        // drop question marks
        q1 = q1.replace("\\?","");
        q2 = q2.replace("\\?","");
        w1 = q1.split(" ");
        w2 = q2.split(" ");

        // undo contractions
        for(int i=0;i<w1.length;++i) w1[i] = contractionMap(w1[i]);
        for(int i=0;i<w2.length;++i) w2[i] = contractionMap(w2[i]);

        q1 = join(w1);
        q2 = join(w2);
        q1 = q1.replaceAll("\\p{P}", "");
        q2 = q2.replaceAll("\\p{P}", "");

        List<Token> t1 = _tok.tokenize(q1);
        List<Token> t2 = _tok.tokenize(q2);
        NLPNode[] nodes1 = nlpd.toNodeArray(t1);
        NLPNode[] nodes2 = nlpd.toNodeArray(t2);

        _lex.process(nodes1);
        _pos.process(nodes1);
        _lex.process(nodes2);
        _pos.process(nodes2);

        f1 = toStringA(nodes1);
        f2 = toStringA(nodes2);
        sf1 = join(f1);
        sf2 = join(f2);

        int q1_words = w1.length;
        int q2_words = w2.length;
        int common_words = countCommon(w1, w2);
        int fuzzy_matched = countCommon(f1, f2);
        int common_caps = countCapsCommon(w1, w2);
        int q1_chars = q1.length();
        int q1_fuzzy_chars = fuzzyChars(f1);
        int q2_chars = q2.length();
        int q2_fuzzy_chars = fuzzyChars(f2);
        int abs_chars = Math.abs(q1_chars - q2_chars);
        int abs_fuzzy_chars = Math.abs(q1_fuzzy_chars - q2_fuzzy_chars);
        int q1_punc_count = countPunc(q1);
        int q2_punc_count = countPunc(q2);
        int q1_caps = countCaps(q1);
        int q2_caps = countCaps(q2);
        int abs_caps = Math.abs(q1_caps - q2_caps);
        ncs[ncs_idx++].addNum(q1_words);
        ncs[ncs_idx++].addNum(q2_words);
        ncs[ncs_idx++].addNum(Math.abs(q1_words - q2_words));
        ncs[ncs_idx++].addNum(common_words);
        ncs[ncs_idx++].addNum(fuzzy_matched);
        ncs[ncs_idx++].addNum(common_caps);
        ncs[ncs_idx++].addNum(q1_chars);
        ncs[ncs_idx++].addNum(q1_fuzzy_chars);
        ncs[ncs_idx++].addNum(q2_chars);
        ncs[ncs_idx++].addNum(q2_fuzzy_chars);
        ncs[ncs_idx++].addNum(abs_chars);
        ncs[ncs_idx++].addNum(abs_fuzzy_chars);
        ncs[ncs_idx++].addNum(q1_punc_count);
        ncs[ncs_idx++].addNum(q2_punc_count);
        ncs[ncs_idx++].addNum(q1_caps);
        ncs[ncs_idx++].addNum(q2_caps);
        ncs[ncs_idx++].addNum(abs_caps);

        // similarity features on fuzzed words
        getDistances(q1, q2, dists);
//        ncs_idx = 15;
        for (double dist : dists) ncs[ncs_idx++].addNum(dist);
//        assert ncs_idx == 26;

        // string similarity on POS tags
        // only get pos tags for non-stop words
        String pos1 = posTags(nodes1);
        String pos2 = posTags(nodes2);
        getDistances(pos1, pos2, dists2);
        for (double dist : dists2) ncs[ncs_idx++].addNum(dist);
//        assert ncs_idx == 37;

        // word-embeddings features
        wordEmVecs(nodes1, we_s1, we_ss1);
        wordEmVecs(nodes2, we_s2, we_ss2);

        float wes1_sum = ArrayUtils.sum(we_s1);
        float wes2_sum = ArrayUtils.sum(we_s2);
        float abs_wes_sum = Math.abs(wes1_sum - wes2_sum);
        float wess1_sum = ArrayUtils.sum(we_ss1);
        float wess2_sum = ArrayUtils.sum(we_ss2);
        float abs_wess_sum = Math.abs(wess1_sum - wess2_sum);
        float wes_cosine = cosine_distance(we_s1, we_s2);
        float wess_cosine = cosine_distance(we_ss1, we_ss2);
        ncs[ncs_idx++].addNum(wes1_sum);
        ncs[ncs_idx++].addNum(wes2_sum);
        ncs[ncs_idx++].addNum(wess1_sum);
        ncs[ncs_idx++].addNum(wess2_sum);
        ncs[ncs_idx++].addNum(abs_wes_sum);
        ncs[ncs_idx++].addNum(abs_wess_sum);
        ncs[ncs_idx++].addNum(wes_cosine);
        ncs[ncs_idx++].addNum(wess_cosine);
        if (!_test) ncs[ncs_idx].addNum(cs[cs.length - 1].at8(r));
      } catch (Exception e) {
        System.out.println("q1= " + q1);
        System.out.println("q2= " + q2);
        System.out.println("w1= " + Arrays.toString(w1));
        System.out.println("w2= " + Arrays.toString(w2));
        System.out.println("f1= " + Arrays.toString(f1));
        System.out.println("f2= " + Arrays.toString(f2));
        System.out.println("sf1= " + sf1);
        System.out.println("sf2= " + sf2);
        throw new RuntimeException(e);
      }
    }
  }

  static float cosine_distance(float[] a, float[] b) {
    float sum_ab = 0;
    float sum_a2 = 0;
    float sum_b2 = 0;
    for(int i=0;i<a.length;++i) {
      sum_ab += a[i]*b[i];
      sum_a2 += a[i]*a[i];
      sum_b2 += b[i]*b[i];
    }
    float sim = sum_ab / (float) (Math.sqrt(sum_a2) * Math.sqrt(sum_b2) );
    return 1-sim;
  }

  void wordEmVecs(NLPNode[] nodes, float[] ws, float[] wss) {
    for(int i=0;i<ws.length;++i) ws[i]=wss[i]=0;

    int cnt=0;
    for (NLPNode n1 : nodes) {
      if( n1.isStopWord() ) continue;
      cnt++;
      float[] f = n1.getWordEmbedding();
      add(ws, f);
      addSq(wss, f);
    }

    ArrayUtils.div(ws,cnt);
    ArrayUtils.div(wss,cnt*cnt);
    sqrt(wss);
  }

  static void sqrt(float[] a) {
    for(int i=0;i<a.length;++i) a[i] = (float)Math.sqrt(a[i]);
  }

  static void add(float[] a, float[] f) {
    if( f==null ) return;
    for(int i=0;i<f.length;++i) a[i] += f[i];
  }

  static void addSq(float[] a, float[] f) {
    if( f==null ) return;
    for(int i=0;i<f.length;++i) a[i] += f[i]*f[i];
  }

  double[] getDistances(String s1, String s2, double[] dists) {
    dists[0 /*cosine*/]      = _simMetrics[0].distance(s1,s2);
    dists[1 /*dameru*/]      = _simMetrics[1].distance(s1,s2);
    dists[2 /*jaccard*/]     = _simMetrics[2].distance(s1,s2);
    dists[3 /*jwink*/]       = _simMetrics[3].distance(s1,s2);
    dists[4 /*leven*/]       = _simMetrics[4].distance(s1,s2);
    dists[5 /*lcsub*/]       = _simMetrics[5].distance(s1,s2);
    dists[6 /*ngram*/]       = _simMetrics[6].distance(s1,s2);
    dists[7 /*leven_norm*/]  = _simMetrics[7].distance(s1,s2);
    dists[8 /*optim_align*/] = _simMetrics[8].distance(s1,s2);
    dists[9 /*qgram*/]       = _simMetrics[9].distance(s1,s2);
    dists[10 /*sdice*/]      = _simMetrics[10].distance(s1,s2);
    return dists;
  }

  static String posTags(NLPNode[] nodes) {
    StringBuilder sb = new StringBuilder();
    for(NLPNode n: nodes) {
      if( n.isStopWord() ) continue;
      sb.append(n.getPartOfSpeechTag());
    }
    return sb.toString();
  }

  static String join(NLPNode[] nodes) {
    boolean first=true;
    StringBuilder sb= new StringBuilder();
    for(int i=1;i<nodes.length-1;++i) {
      if( first ) first=false;
      else sb.append(" ");
      sb.append(nodes[i].getWordFormSimplifiedLowercase());
    }
    return sb.toString();
  }

  static String join(String[] words) {
    boolean first=true;
    StringBuilder sb= new StringBuilder();
    for (String word : words) {
      if (first) first = false;
      else sb.append(" ");
      sb.append(word);
    }
    return sb.toString();
  }

  // filter out stop words
  static String[] toStringA(NLPNode[] nodes) {
    ArrayList<String> words = new ArrayList<>();
    for(int i=1;i<nodes.length-1;++i) {
      if( nodes[i].isStopWord() ) continue;
      words.add(nodes[i].getWordFormSimplifiedLowercase());
    }
    return words.toArray(new String[words.size()]);
  }

  int countCaps(String s) {
    int cnt=0;
    for(int i=0;i<s.length();++i ) {
      char c = s.charAt(i);
      if( 'A' <= c && c <= 'Z' ) cnt++;
    }
    return cnt;
  }

  int countPunc(String s) {
    int cnt=0;
    for(int i=0;i<s.length();++i) {
      char c = s.charAt(i);
      if( ('a' <= c && c <= 'z')          ||  // a lowercase ascii letter
          ('A' <= c && c <= 'Z')          ||  // a uppercase ascii letter
          ('0' <= c && c <= '9')          ||  // a digit
          (c==' ' || c=='\t' || c=='\n')      // a space, tab, or newline
        ) continue;
      cnt++;
    }
    return cnt;
  }

  int fuzzyChars(String[] f) {
    int c=0;
    for( String ff: f) c += ff.length();
    return c;
  }

  int countCommon(String[] q1, String[] q2) {
    HashSet<String> s1 = new HashSet<>();
    Collections.addAll(s1,q1);
    int cnt=0;
    for(String s: q2)
      if( s1.contains(s) ) cnt++;
    return cnt;
  }

  // count common capitalized words
  int countCapsCommon(String[] w1, String[] w2) {
    HashSet<String> caps1 = new HashSet<>();
    for(String w: w1) {
      if( w.length()<1 ) continue;
      char c = w.charAt(0);
      if( 'A' <= c && c <= 'Z' ) caps1.add(w);
    }
    int cnt=0;
    for(String w: w2) {
      if( caps1.contains(w) ) cnt++;
    }
    return cnt;
  }

  public static void main(String[] args) {
    H2OApp.main(args);
    boolean train=true;
    int id=3;
    String outpath= train?"./data/train_feats"+id+".csv":"./data/test_feats"+id+".csv";
    String path = train?"./data/train_clean.csv":"./data/test_clean.csv";
    String name = train?"train":"test";
    String key= train?"train_feats":"test_feats";
    int nouts = Preprocess.NAMES.length+(train?1:0);
    byte[] types= train?new byte[]{Vec.T_NUM,Vec.T_NUM,Vec.T_NUM, Vec.T_STR, Vec.T_STR, Vec.T_NUM}:new byte[]{Vec.T_NUM,Vec.T_STR,Vec.T_STR};

    Frame fr = importParseFrame(path,name, types);
    long s = System.currentTimeMillis();
    Preprocess p = new Preprocess(!train);
    Frame out = p.doAll(nouts, Vec.T_NUM, fr).outputFrame(Key.make(key),p.getNames(),null);
    System.out.println("all done: " + (System.currentTimeMillis()-s)/1000. + " seconds");

    System.out.println("Writing frame ");
    Job job = Frame.export(out, outpath, out._key.toString(), false, 1);
    job.get();

    H2O.shutdown(0);
  }
}
