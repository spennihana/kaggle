import numpy as np
from sklearn.cross_validation import train_test_split
import pickle
import xgboost as xgb
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.model_selection import StratifiedShuffleSplit

training = pd.read_csv("../data/train_feats19.csv")
print "loaded training frame"

pos_train = training[training['is_duplicate']==1]
neg_train = training[training['is_duplicate']==0]
training=None
p=0.165
scale = ((1.*len(pos_train) / (1.*len(pos_train) + 1.*len(neg_train))) / (1.*p)) - 1.
#while scale > 1:
neg_train = pd.concat([neg_train, neg_train])
scale -=1
neg_train = pd.concat([neg_train, neg_train.sample(n=int(scale*len(neg_train)))])  #neg_train[:int(scale * len(neg_train))]])
train = pd.concat([pos_train, neg_train])


next_sub = 0
with open("../submissions/next", "r") as f:
  next_sub = int(f.readline()) + 1

xlab = list(train.columns)
xlab = [i for i in xlab if i not in ["id", "is_duplicate"]]
ylab = "is_duplicate"
pos_train=None
neg_train=None


print "creating test/train split"
xtrain, xtest, ytrain, ytest =  train_test_split(train[xlab], train['is_duplicate'], test_size=0.2, random_state=0)
#xtrain = train[xlab]
#xtest = xtrain
#ytrain = train['is_duplicate']
#ytest = train
train = None

clf = xgb.XGBClassifier(
learning_rate =0.02,
n_estimators=500,
max_depth=4,
min_child_weight=1,
gamma=0.1,
subsample=.7,
colsample_bytree=0.7,
objective= "binary:logistic",
scale_pos_weight=1,
reg_alpha=0.01
)

evalset = [(xtrain.as_matrix(), ytrain.as_matrix()), (xtest.as_matrix(),ytest.as_matrix())]
xtrain = None
ytrain = None
xtest = None
ytest = None

print "set up xgb: "
print clf

print "training..."
clf.fit(evalset[0][0], evalset[0][1], eval_set=evalset, eval_metric="logloss", early_stopping_rounds=50, verbose=True)


print "saving model to disk"
pickle.dump(clf, open("../models/xgb_{}.model".format(next_sub), "w"))

