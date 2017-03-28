import pickle
import xgboost as xgb
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.model_selection import StratifiedShuffleSplit

training = pd.read_csv("../data/train_feats.csv")
print "loaded training frame"

next_sub = 0
with open("../submissions/next", "r") as f:
  next_sub = int(f.readline()) + 1

xlab = list(training.columns)
xlab = [i for i in xlab if i not in ["id", "is_duplicate"]]
ylab = "is_duplicate"

X = training[xlab]
y = training[ylab]

print "creating test/train split"
sss = StratifiedShuffleSplit(n_splits=1, test_size=0.2, random_state=0)
splits = list(sss.split(X,y))
xtrain, xtest = X.loc[splits[0][0]], X.loc[splits[0][1]]
ytrain, ytest = y.loc[splits[0][0]], y.loc[splits[0][1]]

clf = xgb.XGBClassifier(
learning_rate =0.05,
n_estimators=250,
max_depth=4,
min_child_weight=1,
gamma=0.1,
subsample=0.85,
colsample_bytree=0.75,
objective= "binary:logistic",
nthread=4,
scale_pos_weight=1,
reg_alpha=0.01
)

evalset = [(xtest,ytest)]

print "set up xgb: "
print clf

print "training..."
clf.fit(xtrain, ytrain, eval_set=evalset, eval_metric="logloss", early_stopping_rounds=50, verbose=True)

print "saving model to disk"
pickle.dump(clf, open("../models/xgb_{}.model".format(next_sub), "w"))

