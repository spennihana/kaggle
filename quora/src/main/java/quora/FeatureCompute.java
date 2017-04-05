package quora;


import DiffLib.Levenshtein;
import water.H2O;
import water.H2OApp;
import water.Job;
import water.Key;
import water.fvec.Frame;
import water.fvec.Vec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static quora.PreprocessorTask.Feature;
import static quora.Utils.StrikeAMatch.compareStrings;
import static quora.Utils.*;
import static quora.WordEmbeddings.WORD_EM.GLOVE;
import static quora.WordEmbeddings.WORD_EM.GOOGL;
import static water.KaggleUtils.importParseFrame;
import static water.util.MathUtils.sum;


public class FeatureCompute {

  public static void main(String[] args) {
    WordEmbeddings.WORD_EM tapEnumToLoad = WordEmbeddings.WORD_EM.GLOVE; // read the word embeddings in clinit on each node in cluster
    // boot up h2o for preprocessing
    H2OApp.main(args);
    int id=18;
    boolean sample=false;
    runTrain(id,sample);
    runTest (id,sample);
    H2O.shutdown(0);
  }

  static void runTrain(int id, boolean sample) {
    String outpath= "./data/train_feats"+id+".csv";
    String path = sample?"./data/train_sample.csv":"./data/train_clean.csv";
    String name = "train";
    String key= "train_feats";
    byte[] types= new byte[]{Vec.T_NUM,Vec.T_NUM,Vec.T_NUM, Vec.T_STR, Vec.T_STR, Vec.T_NUM};
    Frame fr = importParseFrame(path,name, types);
    long s = System.currentTimeMillis();
    PreprocessorTask pt = new PreprocessorTask(computeFeatures(),true);
    String[] outnames = pt.getNames();
    Frame out = pt.doAll(outnames.length, Vec.T_NUM,fr).outputFrame(Key.make(key),outnames,null);
    System.out.println("all done: " + (System.currentTimeMillis()-s)/1000. + " seconds");
    System.out.println("Writing frame ");
    Job job = Frame.export(out, outpath, out._key.toString(), false, 1);
    job.get();
    out.delete();
    fr.delete();
  }

  static void runTest(int id,boolean sample) {
    String outpath= "./data/test_feats"+id+".csv";
    String path = sample?"./data/test_sample.csv":"./data/test_clean.csv";
    String name = "test";
    String key= "test_feats";
    byte[] types= new byte[]{Vec.T_NUM,Vec.T_STR,Vec.T_STR};
    Frame fr = importParseFrame(path,name, types);
    long s = System.currentTimeMillis();
    PreprocessorTask pt = new PreprocessorTask(computeFeatures(),false);
    String[] outnames = pt.getNames();
    Frame out = pt.doAll(outnames.length, Vec.T_NUM,fr).outputFrame(Key.make(key),outnames,null);
    System.out.println("all done: " + (System.currentTimeMillis()-s)/1000. + " seconds");
    System.out.println("Writing frame ");
    Job job = Frame.export(out, outpath, out._key.toString(), false, 1);
    job.get();
    fr.delete();
    out.delete();
  }

  static Feature[] computeFeatures() {
    String[] bestFeatures = new String[]{
      "q2_chars2", "prat2", "emd2", "abs_wess_sum2", "qgram2", "prat_set", "q1_chars2", "q1_chars", "wmd", "abs_wes_sum2",
      "lcsub", "rat_set2", "zratio", "ngram", "q2_chars", "wess_cosine", "qgram", "canberra", "rat_set", "common_ratio",
      "emd", "abs_wess_sum", "leven_norm", "prat_sort", "abs_wes_sum", "wess_cosine2", "jwink2", "common_ratio2",
      "jaccard", "wratio", "wess2_sum2", "cosine", "rat_sort", "prat", "wess1_sum", "wess1_sum2", "wess2_sum",
      "strike_match", "wes_cosine2", "wes1_sum2", "wes2_sum2", "wes2_sum", "wes1_sum", "wes_cosine", "jwink"
    };

    String[] bestFeatures2= new String[]{
      "wes2_skew_glove", "wess2_kurt_glove", "wes1_sum2", "wes_minkowski_glove", "wes1_kurt_glove", "wes1_sum",
      "wess2_kurt", "q1_chars2", "wess_euclidean_glove", "wes2_kurt_glove", "wess_cosine2", "wratio", "wess1_sum_glove",
      "canberra_glove2", "wess2_sum_glove2", "jwink2", "wess1_kurt_glove", "wess2_sum_glove", "prat_sort", "wess2_skew_glove2",
      "wess1_sum", "wess_hamming", "zratio", "wess_hamming_glove2", "rat_sort", "cosine", "wes1_skew_glove", "canberra",
      "wess2_sum", "canberra_glove", "wes_hamming_glove2", "q1_chars", "ngram", "wes2_skew_glove2", "wess1_sum_glove2",
      "jaccard", "rat_set2", "wes1_skew_glove2", "wes_hamming_glove", "wess_hamming_glove", "prat", "wess_cosine_glove",
      "qgram2", "jwink", "rat_set", "common_ratio", "qgram", "lcsub", "leven_norm", "strike_match", "common_ratio2"
    };

    HashSet<String> topFeats = new HashSet<>();
    Collections.addAll(topFeats, bestFeatures);
    Collections.addAll(topFeats,bestFeatures2);
    System.out.println();
    topFeats.add("DUMMY_COMPUTE_FC_W");
    topFeats.add("DUMMY_COMPUTE_EM_W");
    topFeats.add("DUMMY_COMPUTE_FC_F");
    topFeats.add("DUMMY_COMPUTE_EM_F");
//    topFeats.add("wes_hamming");
//    topFeats.add("wes_euclidean");
//    topFeats.add("wes_minkowski");
//    topFeats.add("wess_hamming");
//    topFeats.add("wess_euclidean");
//    topFeats.add("wess_minkowski");
//    topFeats.add("wes_hamming2");
//    topFeats.add("wes_euclidean2");
//    topFeats.add("wes_minkowski2");
//    topFeats.add("wess_hamming2");
//    topFeats.add("wess_euclidean2");
//    topFeats.add("wess_minkowski2");
//    topFeats.add("wes1_skew");
//    topFeats.add("wes2_skew");
//    topFeats.add("wess1_skew");
//    topFeats.add("wess2_skew");
//    topFeats.add("wes1_kurt");
//    topFeats.add("wes2_kurt");
//    topFeats.add("wess1_kurt");
//    topFeats.add("wess2_kurt");
//    topFeats.add("wes1_skew2");
//    topFeats.add("wes2_skew2");
//    topFeats.add("wess1_skew2");
//    topFeats.add("wess2_skew2");
//    topFeats.add("wes1_kurt2");
//    topFeats.add("wes2_kurt2");
//    topFeats.add("wess1_kurt2");
//    topFeats.add("wess2_kurt2");
//
//    topFeats.add("wes1_sum_glove");
//    topFeats.add("wes2_sum_glove");
//    topFeats.add("abs_wes_sum_glove");
//    topFeats.add("wess1_sum_glove");
//    topFeats.add("wess2_sum_glove");
//    topFeats.add("abs_wess_sum_glove");
//    topFeats.add("wes_cosine_glove");
//    topFeats.add("wes_hamming_glove");
//    topFeats.add("wes_euclidean_glove");
//    topFeats.add("wes_minkowski_glove");
//    topFeats.add("wes1_skew_glove");
//    topFeats.add("wes2_skew_glove");
//    topFeats.add("wess1_skew_glove");
//    topFeats.add("wess2_skew_glove");
//    topFeats.add("wes1_kurt_glove");
//    topFeats.add("wes2_kurt_glove");
//    topFeats.add("wess1_kurt_glove");
//    topFeats.add("wess2_kurt_glove");
//    topFeats.add("wess_cosine_glove");
//    topFeats.add("wess_hamming_glove");
//    topFeats.add("wess_euclidean_glove");
//    topFeats.add("wess_minkowski_glove");
//    topFeats.add("emd_glove");
//    topFeats.add("canberra_glove");
//    topFeats.add("wmd_glove");
//
//    topFeats.add("wes1_sum_glove2");
//    topFeats.add("wes2_sum_glove2");
//    topFeats.add("abs_wes_sum_glove2");
//    topFeats.add("wess1_sum_glove2");
//    topFeats.add("wess2_sum_glove2");
//    topFeats.add("abs_wess_sum_glove2");
//    topFeats.add("wes_cosine_glove2");
//    topFeats.add("wes_hamming_glove2");
//    topFeats.add("wes_euclidean_glove2");
//    topFeats.add("wes_minkowski_glove2");
//    topFeats.add("wes1_skew_glove2");
//    topFeats.add("wes2_skew_glove2");
//    topFeats.add("wess1_skew_glove2");
//    topFeats.add("wess2_skew_glove2");
//    topFeats.add("wes1_kurt_glove2");
//    topFeats.add("wes2_kurt_glove2");
//    topFeats.add("wess1_kurt_glove2");
//    topFeats.add("wess2_kurt_glove2");
//    topFeats.add("wess_cosine_glove2");
//    topFeats.add("wess_hamming_glove2");
//    topFeats.add("wess_euclidean_glove2");
//    topFeats.add("wess_minkowski_glove2");
//    topFeats.add("emd_glove2");
//    topFeats.add("canberra_glove2");
//    topFeats.add("wmd_glove2");

    ArrayList<Feature> feats = new ArrayList<>(Arrays.asList(
      // "normal" features
      new Feature("common_words", ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> countCommon(w1,w2))),
      new Feature("common_ratio", ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> countCommonRatio(w1,w2))),
      new Feature("strike_match", ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> compareStrings(w1,w2))),
      new Feature("zratio",       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> Levenshtein.ratio(s1,s2))),
      new Feature("cosine" ,      ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> cosine(s1,s2))),
      new Feature("dameru" ,      ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> demaru(s1,s2))),
      new Feature("jaccard" ,     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> jaccard(s1,s2))),
      new Feature("jwink" ,       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> jaroWinkler(s1,s2))),
      new Feature("leven" ,       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> levenshtein(s1,s2))),
      new Feature("lcsub" ,       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> longestCommonSubsequence(s1,s2))),
      new Feature("ngram" ,       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> ngram(s1,s2))),
      new Feature("leven_norm" ,  ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> normalizedLevenshtein(s1,s2))),
      new Feature("optim_align" , ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> optimalStringAlignment(s1,s2))),
      new Feature("qgram" ,       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> qgram(s1,s2))),
      new Feature("sdice" ,       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> sorensenDice(s1,s2))),
         new Feature("DUMMY_COMPUTE_FC_W", ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> 0)),
      new Feature("qratio",       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._qratio)),
      new Feature("wratio" ,      ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._wratio)),
      new Feature("prat" ,        ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._partialRatio)),
      new Feature("prat_set" ,    ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._partialTokenSetRatio)),
      new Feature("prat_sort" ,   ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._partialTokenSortRatio)),
      new Feature("rat_set" ,     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._tokenSetRatio)),
      new Feature("rat_sort" ,    ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._tokenSortRatio)),
      new Feature("q1_words",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> w1.length)),
      new Feature("q2_words",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> w2.length)),
      new Feature("abs_words",    ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> Math.abs(w1.length-w2.length))),
      new Feature("q1_chars",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> countChars(w1))),
      new Feature("q2_chars",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> countChars(w2))),
      new Feature("abs_chars",    ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> Math.abs(countChars(w1))-countChars(w2))),
         new Feature("DUMMY_COMPUTE_EM_W", GOOGL, ((s1, s2, w1, w2, f1, f2, fw1, fw2, d, em) -> 0)),
      new Feature("wes1_sum", GOOGL,  ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(ws1))),
      new Feature("wes2_sum", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(ws2))),
      new Feature("abs_wes_sum", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> Math.abs(sum(ws1) - sum(ws2)))),
      new Feature("wess1_sum", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(wss1))),
      new Feature("wess2_sum", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(wss2))),
      new Feature("abs_wess_sum", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> Math.abs(sum(wss1) - sum(wss2)))),
      new Feature("wes_cosine", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> cosine_distance(ws1,ws2))),
      new Feature("wes_hamming", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,1))),
      new Feature("wes_euclidean", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,2))),
      new Feature("wes_minkowski", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,3))),
      new Feature("wes1_skew", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(ws1))),
      new Feature("wes2_skew", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(ws2))),
      new Feature("wess1_skew", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(wss1))),
      new Feature("wess2_skew", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(wss2))),
      new Feature("wes1_kurt", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(ws1))),
      new Feature("wes2_kurt", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(ws2))),
      new Feature("wess1_kurt", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(wss1))),
      new Feature("wess2_kurt", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(wss2))),
      new Feature("wess_cosine", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> cosine_distance(wss1,wss2))),
      new Feature("wess_hamming", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,1))),
      new Feature("wess_euclidean", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,2))),
      new Feature("wess_minkowski", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,3))),
      new Feature("emd", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> earth_movers_distance(wss1,wss2))),
      new Feature("canberra", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> canberra_distance(wss1,wss2))),
      new Feature("wmd", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> wmd(w1,w2,d,em))),

         new Feature("DUMMY_COMPUTE_EM_W", GLOVE, ((s1, s2, w1, w2, f1, f2, fw1, fw2, d, em) -> 0)),
      new Feature("wes1_sum_glove", GLOVE,  ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(ws1))),
      new Feature("wes2_sum_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(ws2))),
      new Feature("abs_wes_sum_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> Math.abs(sum(ws1) - sum(ws2)))),
      new Feature("wess1_sum_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(wss1))),
      new Feature("wess2_sum_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(wss2))),
      new Feature("abs_wess_sum_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> Math.abs(sum(wss1) - sum(wss2)))),
      new Feature("wes_cosine_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> cosine_distance(ws1,ws2))),
      new Feature("wes_hamming_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,1))),
      new Feature("wes_euclidean_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,2))),
      new Feature("wes_minkowski_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,3))),
      new Feature("wes1_skew_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(ws1))),
      new Feature("wes2_skew_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(ws2))),
      new Feature("wess1_skew_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(wss1))),
      new Feature("wess2_skew_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(wss2))),
      new Feature("wes1_kurt_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(ws1))),
      new Feature("wes2_kurt_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(ws2))),
      new Feature("wess1_kurt_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(wss1))),
      new Feature("wess2_kurt_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(wss2))),
      new Feature("wess_cosine_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> cosine_distance(wss1,wss2))),
      new Feature("wess_hamming_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,1))),
      new Feature("wess_euclidean_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,2))),
      new Feature("wess_minkowski_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,3))),
      new Feature("emd_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> earth_movers_distance(wss1,wss2))),
      new Feature("canberra_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> canberra_distance(wss1,wss2))),
      new Feature("wmd_glove", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> wmd(w1,w2,d,em))),


      // features with stop words removed and toLower'd
      new Feature("common_words2", ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> countCommon(fw1,fw2))),
      new Feature("common_ratio2", ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> countCommonRatio(fw1,fw2))),
      new Feature("strike_match2", ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> compareStrings(fw1,fw2))),
      new Feature("zratio2",       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> Levenshtein.ratio(f1,f2))),
      new Feature("cosine2",       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> cosine(f1,f2))),
      new Feature("dameru2",       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> demaru(f1,f2))),
      new Feature("jaccard2",      ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> jaccard(f1,f2))),
      new Feature("jwink2",        ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> jaroWinkler(f1,f2))),
      new Feature("leven2",        ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> levenshtein(f1,f2))),
      new Feature("lcsub2",        ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> longestCommonSubsequence(f1,f2))),
      new Feature("ngram2",        ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> ngram(f1,f2))),
      new Feature("leven_norm2",   ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> normalizedLevenshtein(f1,f2))),
      new Feature("optim_align2",  ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> optimalStringAlignment(f1,f2))),
      new Feature("qgram2",        ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> qgram(f1,f2))),
      new Feature("sdice2",        ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> sorensenDice(f1,f2))),
         new Feature("DUMMY_COMPUTE_FC_F", ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> 0)),
      new Feature("qratio2",       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._qratio)),
      new Feature("wratio2",       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._wratio)),
      new Feature("prat2",         ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._partialRatio)),
      new Feature("prat_set2",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._partialTokenSetRatio)),
      new Feature("prat_sort2",    ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._partialTokenSortRatio)),
      new Feature("rat_set2",      ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._tokenSetRatio)),
      new Feature("rat_sort2",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fc._tokenSortRatio)),
      new Feature("q1_words2",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fw1.length)),
      new Feature("q2_words2",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> fw2.length)),
      new Feature("abs_words2",    ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> Math.abs(fw1.length-fw2.length))),
      new Feature("q1_chars2",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> countChars(fw1))),
      new Feature("q2_chars2",     ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> countChars(fw2))),
      new Feature("abs_chars2",    ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> Math.abs(countChars(fw1))-countChars(fw2))),
         new Feature("DUMMY_COMPUTE_EM_F", GOOGL, ((s1, s2, w1, w2, f1, f2, fw1, fw2, d,em) -> 0)),
      new Feature("wes1_sum2",GOOGL,  ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(ws1))),
      new Feature("wes2_sum2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(ws2))),
      new Feature("abs_wes_sum2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> Math.abs(sum(ws1) - sum(ws2)))),
      new Feature("wess1_sum2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(wss1))),
      new Feature("wess2_sum2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(wss2))),
      new Feature("abs_wess_sum2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> Math.abs(sum(wss1) - sum(wss2)))),
      new Feature("wes_cosine2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> cosine_distance(ws1,ws2))),
      new Feature("wes_hamming2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,1))),
      new Feature("wes_euclidean2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,2))),
      new Feature("wes_minkowski2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,3))),
      new Feature("wes1_skew2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(ws1))),
      new Feature("wes2_skew2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(ws2))),
      new Feature("wess1_skew2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(wss1))),
      new Feature("wess2_skew2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(wss2))),
      new Feature("wes1_kurt2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(ws1))),
      new Feature("wes2_kurt2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(ws2))),
      new Feature("wess1_kurt2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(wss1))),
      new Feature("wess2_kurt2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(wss2))),
      new Feature("wess_cosine2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> cosine_distance(wss1,wss2))),
      new Feature("wess_hamming2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,1))),
      new Feature("wess_euclidean2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,2))),
      new Feature("wess_minkowski2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,3))),
      new Feature("emd2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> earth_movers_distance(wss1,wss2))),
      new Feature("canberra2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> canberra_distance(wss1,wss2))),
      new Feature("wmd2", GOOGL, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> wmd(fw1,fw2,d,em))),

         new Feature("DUMMY_COMPUTE_EM_F", GLOVE, ((s1, s2, w1, w2, f1, f2, fw1, fw2, d, em) -> 0)),
      new Feature("wes1_sum_glove2", GLOVE,  ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(ws1))),
      new Feature("wes2_sum_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(ws2))),
      new Feature("abs_wes_sum_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> Math.abs(sum(ws1) - sum(ws2)))),
      new Feature("wess1_sum_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(wss1))),
      new Feature("wess2_sum_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> sum(wss2))),
      new Feature("abs_wess_sum_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> Math.abs(sum(wss1) - sum(wss2)))),
      new Feature("wes_cosine_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> cosine_distance(ws1,ws2))),
      new Feature("wes_hamming_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,1))),
      new Feature("wes_euclidean_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,2))),
      new Feature("wes_minkowski_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(ws1,ws2,3))),
      new Feature("wes1_skew_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(ws1))),
      new Feature("wes2_skew_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(ws2))),
      new Feature("wess1_skew_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(wss1))),
      new Feature("wess2_skew_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> skew(wss2))),
      new Feature("wes1_kurt_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(ws1))),
      new Feature("wes2_kurt_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(ws2))),
      new Feature("wess1_kurt_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(wss1))),
      new Feature("wess2_kurt_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> kurt(wss2))),
      new Feature("wess_cosine_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> cosine_distance(wss1,wss2))),
      new Feature("wess_hamming_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,1))),
      new Feature("wess_euclidean_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,2))),
      new Feature("wess_minkowski_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> minkowski_distance(wss1,wss2,3))),
      new Feature("emd_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> earth_movers_distance(wss1,wss2))),
      new Feature("canberra_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> canberra_distance(wss1,wss2))),
      new Feature("wmd_glove2", GLOVE, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, d, em) -> wmd(w1,w2,d,em)))
    ));
    feats.removeIf(x -> !topFeats.contains(x._name));
    return feats.toArray(new Feature[feats.size()]);
  }
}
