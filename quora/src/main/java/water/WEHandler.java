package water;

import quora.WordEmServer;
import water.api.API;
import water.api.Handler;
import water.api.schemas3.SchemaV3;


public class WEHandler extends Handler {
  public WESchema em(int v, WESchema args) {
    args.em= WordEmServer._em.find(args.word);
    return args;
  }

  public static class WESchema extends SchemaV3<Iced, WESchema> {
    @API(help="word", direction=API.Direction.INPUT) public String word;
    @API(help="res",  direction=API.Direction.OUTPUT) public double[] em;
  }
}
