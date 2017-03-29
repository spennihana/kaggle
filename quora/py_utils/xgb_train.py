import pickle
import xgboost as xgb
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.model_selection import StratifiedShuffleSplit

training = pd.read_csv("../data/train_feats6.csv")
print "loaded training frame"

next_sub = 0
with open("../submissions/next", "r") as f:
  next_sub = int(f.readline()) + 1

xlab = list(training.columns)
xlab = [i for i in xlab if i not in ["id", "is_duplicate"]]
#xlab = ["abs_words","fuzzy_matched","abs_chars","abs_fuzzy_chars","hash_equals","match_strike","fuzz_strike","wes_cosine","wes_earth", "wes_canberra"]
#xlab = ["fuzzy_matched","abs_fuzzy_chars","hash_equals","match_strike","fuzz_strike","wes_cosine","wes_earth", "wes_canberra"]
#xlab = ["abs_words","abs_chars","hash_equals","match_strike","wes_cosine","wes_earth", "wes_canberra"]
#xlab = ["cosine","dameru","jaccard","jwink","leven","lcsub","ngram","leven_norm","optim_align","qgram","sdice"]
ylab = "is_duplicate"

X = training[xlab]
y = training[ylab]

print "creating test/train split"
sss = StratifiedShuffleSplit(n_splits=1, test_size=0.2)
splits = list(sss.split(X,y))
xtrain, xtest = X.loc[splits[0][0]], X.loc[splits[0][1]]
ytrain, ytest = y.loc[splits[0][0]], y.loc[splits[0][1]]

clf = xgb.XGBClassifier(
learning_rate =0.02,
n_estimators=1000,
max_depth=9,
min_child_weight=1,
gamma=0.1,
#subsample=.75,
#colsample_bytree=0.8,
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

