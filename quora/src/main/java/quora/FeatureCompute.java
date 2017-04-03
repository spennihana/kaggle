package quora;


import static quora.PreprocessorTask.Feature;
import static quora.Utils.*;
import static quora.Utils.StrikeAMatch.*;


public class FeatureCompute {


  public static void main(String[] args) {

    Feature[] features = new PreprocessorTask.Feature[] {

      // "normal" features
      new Feature("common_words", ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> countCommon(w1,w2))),
      new Feature("common_ratio", ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> countCommonRatio(w1,w2))),
      new Feature("strike_match", ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> compareStrings(w1,w2))),
      new Feature("zratio",       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> DiffLib.Levenshtein.ratio(s1,s2))),
      new Feature("cosine" ,      ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> cosine(s1,s2))),
      new Feature("dameru" ,      ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> demaru(s1,s2))),
      new Feature("jaccard" ,     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> jaccard(s1,s2))),
      new Feature("jwink" ,       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> jaroWinkler(s1,s2))),
      new Feature("leven" ,       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> levenshtein(s1,s2))),
      new Feature("lcsub" ,       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> longestCommonSubsequence(s1,s2))),
      new Feature("ngram" ,       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> ngram(s1,s2))),
      new Feature("leven_norm" ,  ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> normalizedLevenshtein(s1,s2))),
      new Feature("optim_align" , ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> optimalStringAlignment(s1,s2))),
      new Feature("qgram" ,       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> qgram(s1,s2))),
      new Feature("sdice" ,       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> sorensenDice(s1,s2))),
      new Feature("qratio",       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> {
        fc._s1=s1; fc._s2=s2;
        fc.comptue(w1,w2);
        return fc._qratio;
      })),
      new Feature("wratio" ,      ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._wratio)),
      new Feature("prat" ,        ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._partialRatio)),
      new Feature("prat_set" ,    ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._partialTokenSetRatio)),
      new Feature("prat_sort" ,   ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._partialTokenSortRatio)),
      new Feature("rat_set" ,     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._tokenSetRatio)),
      new Feature("rat_sort" ,    ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._tokenSortRatio)),
      new Feature("q1_words",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> w1.length)),
      new Feature("q2_words",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> w2.length)),
      new Feature("abs_words",    ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> Math.abs(w1.length-w2.length))),
      new Feature("q1_chars",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> countChars(w1))),
      new Feature("q2_chars",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> countChars(w2))),
      new Feature("abs_chars",    ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> Math.abs(countChars(w1))-countChars(w2))),




      // features with stop words removed and toLower'd
      new Feature("common_words2", ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> countCommon(fw1,fw2))),
      new Feature("common_ratio2", ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> countCommonRatio(fw1,fw2))),
      new Feature("strike_match2", ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> compareStrings(fw1,fw2))),
      new Feature("zratio2",       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> DiffLib.Levenshtein.ratio(f1,f2))),
      new Feature("cosine2",       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> cosine(f1,f2))),
      new Feature("dameru2",       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> demaru(f1,f2))),
      new Feature("jaccard2",      ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> jaccard(f1,f2))),
      new Feature("jwink2",        ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> jaroWinkler(f1,f2))),
      new Feature("leven2",        ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> levenshtein(f1,f2))),
      new Feature("lcsub2",        ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> longestCommonSubsequence(f1,f2))),
      new Feature("ngram2",        ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> ngram(f1,f2))),
      new Feature("leven_norm2",   ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> normalizedLevenshtein(f1,f2))),
      new Feature("optim_align2",  ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> optimalStringAlignment(f1,f2))),
      new Feature("qgram2",        ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> qgram(f1,f2))),
      new Feature("sdice2",        ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> sorensenDice(f1,f2))),
      new Feature("qratio2",       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> {
        fc._s1=f1; fc._s2=f2;
        fc.comptue(fw1,fw2);
        return fc._qratio;
      })),
      new Feature("wratio2",       ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._wratio)),
      new Feature("prat2",         ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._partialRatio)),
      new Feature("prat_set2",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._partialTokenSetRatio)),
      new Feature("prat_sort2",    ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._partialTokenSortRatio)),
      new Feature("rat_set2",      ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._tokenSetRatio)),
      new Feature("rat_sort2",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fc._tokenSortRatio)),
      new Feature("q1_words2",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fw1.length)),
      new Feature("q2_words2",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> fw2.length)),
      new Feature("abs_words2",    ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> Math.abs(fw1.length-fw2.length))),
      new Feature("q1_chars2",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> countChars(fw1))),
      new Feature("q2_chars2",     ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> countChars(fw2))),
      new Feature("abs_chars2",    ((s1, s2, w1, w2, ws1,wss1, f1, f2, fw1, fw2, ws2, wss2, fc) -> Math.abs(countChars(fw1))-countChars(fw2))),
    };
  }
}
