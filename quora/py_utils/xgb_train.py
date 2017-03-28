import pickle
import xgboost as xgb
import pandas as pd

training = pd.read_csv("../data/train_feats.csv")
print "loaded training frame"

next_sub = 0
with open("../submissions/next", "r") as f:
  next_sub = int(f.readline()) + 1

xlab = list(training.columns)
xlab = [i for i in xlab if i not in ["id", "is_duplicate"]]
ylab = "is_duplicate"

clf = xgb.XGBClassifier(
learning_rate =0.045,
n_estimators=500,
max_depth=9,
min_child_weight=1,
gamma=0.1,
subsample=0.85,
colsample_bytree=0.75,
objective= "binary:logistic",
nthread=4,
scale_pos_weight=1,
reg_alpha=0.01)

print "set up xgb: "
print clf

print "training..."
clf.fit(training[xlab].as_matrix(), training[ylab].as_matrix())

print "saving model to disk"
pickle.dump(clf, open("../models/xgb_{}.model".format(next_sub), "w"))

