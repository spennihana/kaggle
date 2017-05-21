package quora;

import water.DKV;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.NewChunk;
import water.fvec.Vec;
import water.parser.BufferedString;
import water.util.IcedHashMap;
import water.util.IcedInt;
import water.util.Log;

/**
 * Compute some "magic features" as discussed here:
 *   https://www.kaggle.com/jturkewitz/magic-features-0-03-gain
 *
 * The idea is to compute the frequency of each question across both train & test sets.
 * This gives two new features q1_freq and q2_freq.
 */
public class MagicFeatures {

  // attaches the magic features onto both train and test frames
  public static void magicFeatures(Frame train, Frame test) {
    // first step is to collect up hashmap of <question,count> pairs
    final IcedHashMap<BufferedString, IcedInt> trainDict = new CollectQuestionsTask(3,4).doAll(train)._dict;
    IcedHashMap<BufferedString, IcedInt> testDict  = new CollectQuestionsTask(1,2).doAll(test)._dict;
    // reduce train/test dicts
    CollectQuestionsTask.reduce(trainDict,testDict);

    // now place the frequencies down for each question in the train set
    Log.info("computing q1_freq");
    train.add(new MRTask() {
      int _q1=3, _q2=4;
      @Override public void map(Chunk[] cs, NewChunk[] ncs) {
        BufferedString bstr = new BufferedString();
        for(int i=0;i<cs[0]._len; i++) {
          // q1_freq
          cs[_q1].atStr(bstr,i);
          ncs[0].addNum(trainDict.get(bstr)._val);

          // q2_freq
          cs[_q2].atStr(bstr,i);
          ncs[1].addNum(trainDict.get(bstr)._val);

        }
      }
    }.doAll(2, Vec.T_NUM, train).outputFrame(null, new String[]{"q1_freq", "q2_freq"},null));
    DKV.put(train);

    Log.info("computing q2_freq");
    // now place the frequencies down for each question in the test set
    test.add(new MRTask() {
      int _q1=1, _q2=2;
      @Override public void map(Chunk[] cs, NewChunk[] ncs) {
        BufferedString bstr = new BufferedString();
        for(int i=0;i<cs[0]._len; i++) {
          // q1_freq
          cs[_q1].atStr(bstr,i);
          ncs[0].addNum(trainDict.get(bstr)._val);

          // q2_freq
          cs[_q2].atStr(bstr,i);
          ncs[1].addNum(trainDict.get(bstr)._val);

        }
      }
    }.doAll(2, Vec.T_NUM, test).outputFrame(null, new String[]{"q1_freq", "q2_freq"},null));
    DKV.put(test);
  }

  private static class CollectQuestionsTask extends MRTask<CollectQuestionsTask> {
    IcedHashMap<BufferedString, IcedInt> _dict;
    int _q1, _q2;

    CollectQuestionsTask(int q1, int q2) { _q1=q1; _q2=q2; }
    @Override public void map(Chunk[] cs) {
      _dict = new IcedHashMap<>();
      for(int i=0;i<cs[0]._len;++i) {

        // question 1
        if( !cs[_q1].isNA(i) ) {
          BufferedString bstr = new BufferedString();
          cs[_q1].atStr(bstr, i);
          IcedInt v = _dict.get(bstr);
          if (v == null) _dict.put(bstr, v = new IcedInt(0));
          v._val++;
        }

        // question 2
        if( !cs[_q2].isNA(i) ) {
          BufferedString bstr = new BufferedString();
          cs[_q2].atStr(bstr, i);
          IcedInt v = _dict.get(bstr);
          if (v == null) _dict.put(bstr, v = new IcedInt(0));
          v._val++;
        }
      }
    }

    @Override public void reduce(CollectQuestionsTask cqt) {
      reduce(_dict, cqt._dict);
    }

    static IcedHashMap<BufferedString, IcedInt> reduce(IcedHashMap<BufferedString, IcedInt> left, IcedHashMap<BufferedString, IcedInt> rite) {
      for(BufferedString bs: rite.keySet()) {
        IcedInt v = left.get(bs);
        if( v==null ) left.put(bs, v=new IcedInt(0));
        v._val += rite.get(bs)._val;
      }
      return left;
    }
  }
}
