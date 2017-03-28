package quora;


import hex.Model;
import hex.ScoreKeeper;
import hex.tree.gbm.GBM;
import hex.tree.gbm.GBMModel;
import water.AutoBuffer;
import water.H2O;
import water.H2OApp;
import water.Job;
import water.fvec.Frame;

import java.io.*;

import static water.KaggleUtils.importParseFrame;

public class Modeling {


  public static void main(String[] args) {
    H2OApp.main(args);

    Frame training = importParseFrame("./data/train_feats2.csv", "training_features");
    GBMModel.GBMParameters parms = new GBMModel.GBMParameters();

    int subnum;
    try(BufferedReader in = new BufferedReader(new FileReader(new File("./submissions/next")) )) {
      subnum=Integer.valueOf(in.readLine()) + 1;
    } catch( IOException e) {
      throw new RuntimeException(e);
    }

    // make a training/test split
    // break into 3 parts
    // use parts 1-2 for training
    // use part 3 for overall validation
    // do xval on parts 1&2, tho
//    Vec fiveParts = AstKFold.stratifiedKFoldColumn(training.vec("is_duplicate"),3,-1L);

//    Vec testPred = new MRTask() {
//      @Override public void map(Chunk c, NewChunk nc) { for(int i = 0; i<c._len; ++i) nc.addNum(c.at8(i)==2?1:0); }
//    }.doAll(Vec.T_NUM,fiveParts).outputFrame().anyVec();
//    Vec trainPred = new MRTask() {
//      @Override public void map(Chunk c, NewChunk nc) { for(int i = 0; i<c._len; ++i) nc.addNum(c.at8(i)==2?0:1); }
//    }.doAll(Vec.T_NUM,fiveParts).outputFrame().anyVec();

//    Frame train = training.deepSlice(new Frame(trainPred),null);
//    train._key = Key.make("training_frame");
//    DKV.put(train);
//    Frame test = training.deepSlice(new Frame(testPred),null);
//    test._key = Key.make("test_frame");
//    DKV.put(test);

    parms._ntrees = 300;
    parms._learn_rate = 0.005;
    parms._max_depth = 9;
    parms._stopping_metric = ScoreKeeper.StoppingMetric.logloss;
//    parms._valid = test._key;
    parms._train = training._key;
    parms._response_column = "is_duplicate";
    parms._ignored_columns = new String[]{"id"};

    GBM m = new GBM(parms);
    m.trainModel();
    GBMModel model = m.get();

    // save model to disk
    try( FileOutputStream fos = new FileOutputStream(new File("./models/000"+subnum+".h2o_gbm")); FileOutputStream bin = new FileOutputStream(new File("./models/000"+subnum+".h2o_gbm.bin")) ) {
      model.toJava(fos,false,false);
      model.writeAll(new AutoBuffer(bin, true)).close();
    } catch( IOException e) {
      throw new RuntimeException(e);
    }

    // predict and save predictions
    predict(model,"./data/test_feats2.csv",subnum);

    H2O.shutdown(0);
    System.exit(0);
  }


  static void predict(Model m, String test_path, int subnum) {

    Frame test = importParseFrame(test_path, "test_features");
    Frame preds = m.score(test);

    Frame res = new Frame();
    res.add("test_id", test.vec("id"));
    res.add("is_duplicate", preds.vec(0));
    Job job = Frame.export(res, "./submissions/"+subnum+".csv", "submission_"+subnum, false, 1);
    job.get();

    try(BufferedWriter out = new BufferedWriter(new FileWriter(new File("./submissions/next")) )) {
      out.write(""+subnum);
    } catch( IOException e) {
      throw new RuntimeException(e);
    }
  }
}
