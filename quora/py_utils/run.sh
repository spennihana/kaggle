#!/bin/sh

python stream_predict.py 0 200000 &
python stream_predict.py 200000 400000 &
python stream_predict.py 400000 600000 &
python stream_predict.py 600000 800000 &
python stream_predict.py 800000 1000000 &
python stream_predict.py 1000000 1200000 &
python stream_predict.py 1200000 1400000 &
python stream_predict.py 1400000 1600000 &
python stream_predict.py 1600000 1800000 &
python stream_predict.py 
