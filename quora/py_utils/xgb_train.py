import numpy as np
from sklearn.cross_validation import train_test_split
import pickle
import xgboost as xgb
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.model_selection import StratifiedShuffleSplit

training = pd.read_csv("../data/train_feats13.csv")
print "loaded training frame"

pos_train = training[training['is_duplicate']==1]
neg_train = training[training['is_duplicate']==0]
p=0.175
scale = ((1.*len(pos_train) / (1.*len(pos_train) + 1.*len(neg_train))) / (1.*p)) - 1.
while scale > 1:
    neg_train = pd.concat([neg_train, neg_train])
    scale -=1
neg_train = pd.concat([neg_train, neg_train[:int(scale * len(neg_train))]])
train = pd.concat([pos_train, neg_train])


next_sub = 0
with open("../submissions/next", "r") as f:
  next_sub = int(f.readline()) + 1

xlab = list(train.columns)
xlab = [i for i in xlab if i not in ["id", "is_duplicate"]]
ylab = "is_duplicate"
X = train[xlab]
y = train[ylab]



print "creating test/train split"
#sss = StratifiedShuffleSplit(n_splits=1, test_size=0.2)
#splits = list(sss.split(X,y))
#xtrain, xtest = X.loc[splits[0][0]], X.loc[splits[0][1]]
#ytrain, ytest = y.loc[splits[0][0]], y.loc[splits[0][1]]
xtrain, xtest, ytrain, ytest = train_test_split(train[xlab], train['is_duplicate'], test_size=0.2, random_state=0)

clf = xgb.XGBClassifier(
learning_rate =0.02,
n_estimators=1000,
max_depth=4,
min_child_weight=1,
gamma=0.1,
subsample=.7,
colsample_bytree=0.7,
objective= "binary:logistic",
scale_pos_weight=1,
reg_alpha=0.01
)

evalset = [(xtrain.as_matrix(), ytrain.as_matrix()),(xtest.as_matrix(),ytest.as_matrix())]

print "set up xgb: "
print clf

print "training..."
clf.fit(xtrain.as_matrix(), ytrain.as_matrix(), eval_set=evalset, eval_metric="logloss", early_stopping_rounds=50, verbose=True)

print "saving model to disk"
pickle.dump(clf, open("../models/xgb_{}.model".format(next_sub), "w"))

