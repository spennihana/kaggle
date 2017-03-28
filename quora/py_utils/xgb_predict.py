import pickle
import xgboost as xgb
import pandas as pd

testing = pd.read_csv("../data/test_feats.csv")
print "testing data read in"

next_sub = 0
with open("../submissions/next", "r") as f:
  next_sub = int(f.readline()) + 1

xlab = list(testing.columns)
xlab = [i for i in xlab if i not in ["id", "is_duplicate"]]

clf = loaded_model = pickle.load(open("../models/xgb_{}.model".format(next_sub), "rb"))
print "loaded pickled model"

print "making predictions..."
preds = clf.predict_proba(testing[xlab].as_matrix())
print "probability predictions made"

print "writing to disk..."
with open("../submissions/{}.csv".format(next_sub), "w") as f:
  f.write("{},{}\n".format("test_id","is_duplicate"))
  for i in xrange(len(testing.id)):
    f.write("{},{}\n".format(testing.id[i], preds[i][1]))
    
with open("../submissions/next", "w") as f:
  f.write("{}".format(next_sub))
