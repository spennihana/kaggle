package quora;


import DiffLib.Levenshtein;
import water.H2O;
import water.H2OApp;
import water.Job;
import water.Key;
import water.fvec.Frame;
import water.fvec.Vec;

import static quora.PreprocessorTask.Feature;
import static quora.Utils.StrikeAMatch.compareStrings;
import static quora.Utils.*;
import static water.KaggleUtils.importParseFrame;
import static water.util.MathUtils.sum;


public class FeatureCompute {


  public static void main(String[] args) {
    // boot up h2o for preprocessing
    H2OApp.main(args);
    boolean train=true;
    int id=15;
    String outpath= train?"./data/train_feats"+id+".csv":"./data/test_feats"+id+".csv";
    String path = train?"./data/train_sample.csv":"./data/test_clean.csv";
    String name = train?"train":"test";
    String key= train?"train_feats":"test_feats";
    byte[] types= train?new byte[]{Vec.T_NUM,Vec.T_NUM,Vec.T_NUM, Vec.T_STR, Vec.T_STR, Vec.T_NUM}:new byte[]{Vec.T_NUM,Vec.T_STR,Vec.T_STR};
    Frame fr = importParseFrame(path,name, types);
    long s = System.currentTimeMillis();
    PreprocessorTask pt = new PreprocessorTask(computeFeatures(),"./lib/w2vec_models/gw2vec_sample",false);
    String[] outnames = pt.getNames();
    Frame out = pt.doAll(outnames.length, Vec.T_NUM,fr).outputFrame(Key.make(key),outnames,null);
    System.out.println("all done: " + (System.currentTimeMillis()-s)/1000. + " seconds");
    System.out.println("Writing frame ");
    Job job = Frame.export(out, outpath, out._key.toString(), false, 1);
    job.get();
    H2O.shutdown(0);
  }

  static Feature[] computeFeatures() {
    return new Feature[] {
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
      new Feature("qratio",       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> {
        fc._s1=s1; fc._s2=s2;
        fc.comptue(w1,w2);
        return fc._qratio;
      })),
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
      new Feature("wes1_sum",-1,  ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> {
        fillEmVecs(w1, em, ws1, wss1);
        fillEmVecs(w2, em, ws2, wss2);
        return sum(ws1);
      })),
      new Feature("wes2_sum", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> sum(ws2))),
      new Feature("abs_wes_sum", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> Math.abs(sum(ws1) - sum(ws2)))),
      new Feature("wess1_sum", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> sum(wss1))),
      new Feature("wess2_sum", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> sum(wss2))),
      new Feature("abs_wess_sum", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> Math.abs(sum(wss1) - sum(wss2)))),
      new Feature("wes_cosine", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> cosine_distance(ws1,ws2))),
      new Feature("wess_cosine", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> cosine_distance(wss1,wss2))),
      new Feature("emd", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> earth_movers_distance(wss1,wss2))),
      new Feature("canberra", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> canberra_distance(wss1,wss2))),
      new Feature("wmd", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> wmd(w1,w2,em))),


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
      new Feature("qratio2",       ((s1, s2, w1, w2, f1, f2, fw1, fw2, fc) -> {
        fc._s1=f1; fc._s2=f2;
        fc.comptue(fw1,fw2);
        return fc._qratio;
      })),
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
      new Feature("wes1_sum2",-1,  ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> {
        fillEmVecs(fw1, em, ws1, wss1);
        fillEmVecs(fw2, em, ws2, wss2);
        return sum(ws1);
      })),
      new Feature("wes2_sum2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> sum(ws2))),
      new Feature("abs_wes_sum2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> Math.abs(sum(ws1) - sum(ws2)))),
      new Feature("wess1_sum2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> sum(wss1))),
      new Feature("wess2_sum2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> sum(wss2))),
      new Feature("abs_wess_sum2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> Math.abs(sum(wss1) - sum(wss2)))),
      new Feature("wes_cosine2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> cosine_distance(ws1,ws2))),
      new Feature("wess_cosine2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> cosine_distance(wss1,wss2))),
      new Feature("emd2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> earth_movers_distance(wss1,wss2))),
      new Feature("canberra2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> canberra_distance(wss1,wss2))),
      new Feature("wmd2", -1, ((w1, w2, fw1, fw2, ws1, wss1, ws2, wss2, em) -> wmd(fw1,fw2,em))),
    };
  }
}
