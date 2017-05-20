package water;

import water.api.AbstractRegister;
import java.io.File;

@SuppressWarnings("unused")
public class Register extends AbstractRegister {
  @Override public void register(String relativeResourcePath) throws ClassNotFoundException {
    H2O.registerResourceRoot(new File("web/www"));  // www is in the classpath
    H2O.register("GET /3/Get", GetQuestionHandler.class, "get", "", "Get current quora question pair + label");
    H2O.register("GET /3/Next", GetQuestionHandler.class, "next", "", "Get next quora question pair + label");
    H2O.register("GET /3/Set", GetQuestionHandler.class, "set", "", "Set user label");
    H2O.register("GET /3/Save", GetQuestionHandler.class, "save", "", "Save all user labels");
    H2O.register("GET /3/Scroll", GetQuestionHandler.class, "scroll2", "", "Scroll to next row to label");
  }
}
