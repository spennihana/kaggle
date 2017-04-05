package water;

import water.api.API;
import water.api.Handler;
import water.api.schemas3.SchemaV3;
import water.fvec.Frame;
import water.parser.BufferedString;

public class GetQuestionHandler extends Handler {
  static long ROW=0;
  static final int Q1 = 3;
  static final int Q2 = 4;

  public GetQuestionSchema next(int v, GetQuestionSchema args) {
    if( args.row_id==-1 )args.row_id=++ROW;
    else ROW=args.row_id;
    return fill(args);
  }

  public GetQuestionSchema get(int v, GetQuestionSchema args) {
    return fill(args);
  }

  public GetQuestionSchema set(int v, GetQuestionSchema args) {
    Frame userLabels = DKV.get("user_labels").get();
    userLabels.anyVec().set(ROW,args.label);
    DKV.put(userLabels);
    return fill(args);
  }

  public GetQuestionSchema save(int v, GetQuestionSchema args) {
    String outpath = "./data/user_labels.csv";
    Frame out = DKV.get("user_labels").get();
    Job job = Frame.export(out, outpath, out._key.toString(), false, 1);
    job.get();
    return fill(args);
  }

  public static class GetQuestionSchema extends SchemaV3<Iced, GetQuestionSchema> {
    @API(help="row number of question to get", direction=API.Direction.INPUT) public long row_id;
    @API(help="question 1", direction=API.Direction.OUTPUT) public String question1;
    @API(help="question 2", direction=API.Direction.OUTPUT) public String question2;
    @API(help="label", direction=API.Direction.OUTPUT) public int label;
    @API(help="user label", direction=API.Direction.INPUT) public int user_label;
  }

  static GetQuestionSchema fill(GetQuestionSchema g) {
    Frame fr = DKV.get("train").get();
    Frame userLabels = DKV.get("user_labels").get();
    BufferedString bstr = new BufferedString();
    g.row_id=ROW;
    long r = g.row_id;
    g.question1 = fr.vec(Q1).isNA(r)?"":fr.vec(Q1).atStr(bstr, r).toString();
    g.question2 = fr.vec(Q2).isNA(r)?"":fr.vec(Q2).atStr(bstr, r).toString();
    g.label = (int)fr.vec(5).at8(r);
    g.user_label = (int)userLabels.vec(0).at8(r);
    return g;
  }
}
