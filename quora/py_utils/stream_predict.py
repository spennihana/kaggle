import pickle
import xgboost as xgb 
import pandas as pd

def write_preds(f, preds, ids):
  for pid in xrange(len(preds)):
    pred = "{},{}\n".format(ids[pid], preds[pid][1])
    f.write(pred)

test_fr = "../data/test_feats18.csv"
next_sub = 0 
with open("../submissions/next", "r") as f:
  next_sub = int(f.readline()) + 1 

clf = loaded_model = pickle.load(open("../models/xgb_{}.model".format(next_sub), "rb"))

sub = open("../submissions/{}.csv".format(next_sub), "w")
sub.write("{},{}\n".format("test_id","is_duplicate"))
with open(test_fr, "r") as testing:
  testing.readline() # skip header
  i=0
  lines=[]
  ids=[]
  for line in testing:
    line = line.strip()
    line = line.split(",")
    test_id = line[0]
    row = []
    for z in line[1:]:
      if   z=='':         d=float("nan")
      elif z=='Infinity': d=float("inf")
      elif z=='-Infinity':d=float("-inf")
      else:               d=float(z)
      row += [d]
    lines += [row]
    ids  += [test_id]
    if len(lines) == 50000:
      preds = clf.predict_proba(lines)
      write_preds(sub,preds,ids)
      i+= len(lines)
      print "made {} predictions".format(i)
      lines=[]
      ids=[]
  preds = clf.predict_proba(lines)
  write_preds(sub,preds,ids)
sub.close()

