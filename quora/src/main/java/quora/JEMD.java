package quora;

import java.util.*;

public class JEMD {


  static class Edge {
    Edge(int to, long cost) {
      _to = to;
      _cost = cost;
    }

    int _to;
    long _cost;
  }

  static class Edge0 {
    Edge0(int to, long cost, long flow) {
      _to = to;
      _cost = cost;
      _flow = flow;
    }

    int _to;
    long _cost;
    long _flow;
  }

  static class Edge1 {
    Edge1(int to, long reduced_cost) {
      _to = to;
      _reduced_cost = reduced_cost;
    }

    int _to;
    long _reduced_cost;
  }

  static class Edge2 {
    Edge2(int to, long reduced_cost, long residual_capacity) {
      _to = to;
      _reduced_cost = reduced_cost;
      _residual_capacity = residual_capacity;
    }

    int _to;
    long _reduced_cost;
    long _residual_capacity;
  }

  static class Edge3 {
    Edge3() {
      _to = 0;
      _dist = 0;
    }

    Edge3(int to, long dist) {
      _to = to;
      _dist = dist;
    }

    int _to;
    long _dist;
  }

  static class MinCostFlow {

    int numNodes;
    Vector<Integer> nodesToQ;

    // e - supply(positive) and demand(negative).
    // c[i] - edges that goes from node i. first is the second nod
    // x - the flow is returned in it
    long compute(long[] e, Vector<List<Edge>> c, Vector<List<Edge0>> x) {
      assert (e.length == c.size());
      assert (x.size() == c.size());

      numNodes = e.length;
      nodesToQ = new Vector<Integer>();
      for (int i = 0; i < numNodes; i++) {
        nodesToQ.add(0);
      }

      // init flow
      for (int from = 0; from < numNodes; ++from) {
        for (Edge it : c.get(from)) {
          x.get(from).add(new Edge0(it._to, it._cost, 0));
          x.get(it._to).add(new Edge0(from, -it._cost, 0));
        }
      }

      // reduced costs for forward edges (c[i,j]-pi[i]+pi[j])
      // Note that for forward edges the residual capacity is infinity
      Vector<List<Edge1>> rCostForward = new Vector<List<Edge1>>();
      for (int i = 0; i < numNodes; i++) {
        rCostForward.add(new LinkedList<Edge1>());
      }
      for (int from = 0; from < numNodes; ++from) {
        for (Edge it : c.get(from)) {
          rCostForward.get(from).add(new Edge1(it._to, it._cost));
        }
      }

      // reduced costs and capacity for backward edges
      // (c[j,i]-pi[j]+pi[i])
      // Since the flow at the beginning is 0, the residual capacity is
      // also zero
      Vector<List<Edge2>> rCostCapBackward = new Vector<List<Edge2>>();
      for (int i = 0; i < numNodes; i++) {
        rCostCapBackward.add(new LinkedList<Edge2>());
      }
      for (int from = 0; from < numNodes; ++from) {
        for (Edge it : c.get(from)) {
          rCostCapBackward.get(it._to).add(
            new Edge2(from, -it._cost, 0));
        }
      }

      // Max supply TODO:demand?, given U?, optimization-> min out of
      // demand,supply
      long U = 0;
      for (int i = 0; i < numNodes; i++) {
        if (e[i] > U)
          U = e[i];
      }
      long delta = (long) (Math.pow(2.0,Math.ceil(Math.log((double) (U)) / Math.log(2.0))));

      Vector<Long> d = new Vector<Long>();
      Vector<Integer> prev = new Vector<Integer>();
      for (int i = 0; i < numNodes; i++) {
        d.add(0l);
        prev.add(0);
      }
      delta = 1;
      while (true) { // until we break when S or T is empty
        long maxSupply = 0;
        int k = 0;
        for (int i = 0; i < numNodes; i++) {
          if (e[i] > 0) {
            if (maxSupply < e[i]) {
              maxSupply = e[i];
              k = i;
            }
          }
        }
        if (maxSupply == 0)
          break;
        delta = maxSupply;

        int[] l = new int[1];
        computeShortestPath(d, prev, k, rCostForward, rCostCapBackward, e, l);

        // find delta (minimum on the path from k to l)
        // delta= e[k];
        // if (-e[l]<delta) delta= e[k];
        int to = l[0];
        do {
          int from = prev.get(to);
          assert (from != to);

          // residual
          int itccb = 0;
          while ((itccb < rCostCapBackward.get(from).size())
            && (rCostCapBackward.get(from).get(itccb)._to != to)) {
            itccb++;
          }
          if (itccb < rCostCapBackward.get(from).size()) {
            if (rCostCapBackward.get(from).get(itccb)._residual_capacity < delta)
              delta = rCostCapBackward.get(from).get(itccb)._residual_capacity;
          }

          to = from;
        } while (to != k);

        // augment delta flow from k to l (backwards actually...)
        to = l[0];
        do {
          int from = prev.get(to);
          assert (from != to);

          // TODO - might do here O(n) can be done in O(1)
          int itx = 0;
          while (x.get(from).get(itx)._to != to) {
            itx++;
          }
          x.get(from).get(itx)._flow += delta;

          // update residual for backward edges
          int itccb = 0;
          while ((itccb < rCostCapBackward.get(to).size())
            && (rCostCapBackward.get(to).get(itccb)._to != from)) {
            itccb++;
          }
          if (itccb < rCostCapBackward.get(to).size()) {
            rCostCapBackward.get(to).get(itccb)._residual_capacity += delta;
          }
          itccb = 0;
          while ((itccb < rCostCapBackward.get(from).size())
            && (rCostCapBackward.get(from).get(itccb)._to != to)) {
            itccb++;
          }
          if (itccb < rCostCapBackward.get(from).size()) {
            rCostCapBackward.get(from).get(itccb)._residual_capacity -= delta;
          }

          // update e
          e[to] += delta;
          e[from] -= delta;

          to = from;
        } while (to != k);
      }

      // compute distance from x
      long dist = 0;
      for (int from = 0; from < numNodes; from++) {
        for (Edge0 it : x.get(from)) {
          dist += (it._cost * it._flow);
        }
      }
      return dist;
    }

    void computeShortestPath(Vector<Long> d, Vector<Integer> prev, int from, Vector<List<Edge1>> costForward, Vector<List<Edge2>> costBackward, long[] e, int[] l) {
      // Making heap (all inf except 0, so we are saving comparisons...)
      Vector<Edge3> Q = new Vector<Edge3>();
      for (int i = 0; i < numNodes; i++) {
        Q.add(new Edge3());
      }

      Q.get(0)._to = from;
      nodesToQ.set(from, 0);
      Q.get(0)._dist = 0;

      int j = 1;
      // TODO: both of these into a function?
      for (int i = 0; i < from; ++i) {
        Q.get(j)._to = i;
        nodesToQ.set(i, j);
        Q.get(j)._dist = Long.MAX_VALUE;
        j++;
      }

      for (int i = from + 1; i < numNodes; i++) {
        Q.get(j)._to = i;
        nodesToQ.set(i, j);
        Q.get(j)._dist = Long.MAX_VALUE;
        j++;
      }

      Vector<Boolean> finalNodesFlg = new Vector<Boolean>();
      for (int i = 0; i < numNodes; i++) {
        finalNodesFlg.add(false);
      }
      do {
        int u = Q.get(0)._to;

        d.set(u, Q.get(0)._dist); // final distance
        finalNodesFlg.set(u, true);
        if (e[u] < 0) {
          l[0] = u;
          break;
        }

        heapRemoveFirst(Q, nodesToQ);

        // neighbors of u
        for (Edge1 it : costForward.get(u)) {
          assert (it._reduced_cost >= 0);
          long alt = d.get(u) + it._reduced_cost;
          int v = it._to;
          if ((nodesToQ.get(v) < Q.size())
            && (alt < Q.get(nodesToQ.get(v))._dist)) {
            heapDecreaseKey(Q, nodesToQ, v, alt);
            prev.set(v, u);
          }
        }
        for (Edge2 it : costBackward.get(u)) {
          if (it._residual_capacity > 0) {
            assert (it._reduced_cost >= 0);
            long alt = d.get(u) + it._reduced_cost;
            int v = it._to;
            if ((nodesToQ.get(v) < Q.size())
              && (alt < Q.get(nodesToQ.get(v))._dist)) {
              heapDecreaseKey(Q, nodesToQ, v, alt);
              prev.set(v, u);
            }
          }
        }

      } while (Q.size() > 0);

      for (int _from = 0; _from < numNodes; ++_from) {
        for (Edge1 it : costForward.get(_from)) {
          if (finalNodesFlg.get(_from)) {
            it._reduced_cost += d.get(_from) - d.get(l[0]);
          }
          if (finalNodesFlg.get(it._to)) {
            it._reduced_cost -= d.get(it._to) - d.get(l[0]);
          }
        }
      }

      // reduced costs and capacity for backward edges
      // (c[j,i]-pi[j]+pi[i])
      for (int _from = 0; _from < numNodes; ++_from) {
        for (Edge2 it : costBackward.get(_from)) {
          if (finalNodesFlg.get(_from)) {
            it._reduced_cost += d.get(_from) - d.get(l[0]);
          }
          if (finalNodesFlg.get(it._to)) {
            it._reduced_cost -= d.get(it._to) - d.get(l[0]);
          }
        }
      }
    }

    void heapDecreaseKey(Vector<Edge3> Q, Vector<Integer> nodes_to_Q,
                         int v, long alt) {
      int i = nodes_to_Q.get(v);
      Q.get(i)._dist = alt;
      while (i > 0 && Q.get(PARENT(i))._dist > Q.get(i)._dist) {
        swapHeap(Q, nodes_to_Q, i, PARENT(i));
        i = PARENT(i);
      }
    }

    void heapRemoveFirst(Vector<Edge3> Q, Vector<Integer> nodes_to_Q) {
      swapHeap(Q, nodes_to_Q, 0, Q.size() - 1);
      Q.remove(Q.size() - 1);
      heapify(Q, nodes_to_Q, 0);
    }

    void heapify(Vector<Edge3> Q, Vector<Integer> nodes_to_Q, int i) {
      do {
        // TODO: change to loop
        int l = LEFT(i);
        int r = RIGHT(i);
        int smallest;
        if ((l < Q.size()) && (Q.get(l)._dist < Q.get(i)._dist)) {
          smallest = l;
        } else {
          smallest = i;
        }
        if ((r < Q.size()) && (Q.get(r)._dist < Q.get(smallest)._dist)) {
          smallest = r;
        }

        if (smallest == i)
          return;

        swapHeap(Q, nodes_to_Q, i, smallest);
        i = smallest;

      } while (true);
    }

    void swapHeap(Vector<Edge3> Q, Vector<Integer> nodesToQ, int i, int j) {
      Edge3 tmp = Q.get(i);
      Q.set(i, Q.get(j));
      Q.set(j, tmp);
      nodesToQ.set(Q.get(j)._to, j);
      nodesToQ.set(Q.get(i)._to, i);
    }

    int LEFT(int i) {
      return 2 * (i + 1) - 1;
    }

    int RIGHT(int i) {
      return 2 * (i + 1); // 2 * (i + 1) + 1 - 1
    }

    int PARENT(int i) {
      return (i - 1) / 2;
    }
  }


  static double dist(double[]p1,double[]p2) {
    double dx = p1[0]-p2[0];
    double dy = p1[1]-p2[1];
    return Math.sqrt(dx*dx + dy*dy);
  }

  public static class Signature {
    int _nfeat;
    double[][] _features;
    double[] _weights;
  }

  static public double distance(Signature signature1, Signature signature2, double extraMassPenalty) {

    double[] P = new double[signature1._nfeat + signature2._nfeat];
    double[] Q = new double[P.length];
    System.arraycopy(signature1._weights, 0, P, 0, signature1._nfeat);
    System.arraycopy(signature2._weights, 0, Q, signature1._nfeat, signature2._nfeat);

    double[][] C = new double[P.length][P.length];
    for (int i = 0; i < signature1._nfeat; i++) {
      for (int j = 0; j < signature2._nfeat; j++) {
        double dist = dist(signature1._features[i], signature2._features[j]);
        assert (dist >= 0);
        C[i][j + signature1._nfeat] = dist;
        C[j + signature1._nfeat][i] = dist;
      }
    }
    return emdHat(P, Q, C, extraMassPenalty);
  }


  static private long emdHatImplLongLongInt(long[] Pc, long[] Qc, long[][] C, long extraMassPenalty) {

    int N = Pc.length;
    assert (Qc.length == N);

    // Ensuring that the supplier - P, have more mass.
    // Note that we assume here that C is symmetric
    long[] P;
    long[] Q;
    long absDiffSumPSumQ;
    long sumP = 0;
    long sumQ = 0;
    for (long aPc : Pc) sumP += aPc;
    for (int i = 0; i < N; i++) sumQ += Qc[i];
    if (sumQ > sumP) {
      P = Qc;
      Q = Pc;
      absDiffSumPSumQ = sumQ - sumP;
    } else {
      P = Pc;
      Q = Qc;
      absDiffSumPSumQ = sumP - sumQ;
    }

    // creating the b vector that contains all vertexes
    long[] b = new long[2*N+2];
    int THRESHOLD_NODE = 2 * N;
    int ARTIFICIAL_NODE = 2 * N + 1; // need to be last !
    System.arraycopy(P, 0, b, 0, N);
    System.arraycopy(Q, 0, b, N, 2 * N - N);

    // remark*) I put here a deficit of the extra mass, as mass that flows
    // to the threshold node
    // can be absorbed from all sources with cost zero (this is in reverse
    // order from the paper,
    // where incoming edges to the threshold node had the cost of the
    // threshold and outgoing
    // edges had the cost of zero)
    // This also makes sum of b zero.
    b[THRESHOLD_NODE]  = -absDiffSumPSumQ;
    b[ARTIFICIAL_NODE] = 0;

    long maxC = 0;
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++) {
        assert (C[i][j] >= 0);
        if (C[i][j] > maxC)
          maxC = C[i][j];
      }
    }
    if (extraMassPenalty == -1) extraMassPenalty = maxC;

    Set<Integer> sourcesThatFlowNotOnlyToThresh = new HashSet<Integer>();
    Set<Integer> sinksThatGetFlowNotOnlyFromThresh = new HashSet<Integer>();
    long preFlowCost = 0;

    // regular edges between sinks and sources without threshold edges
    Vector<List<Edge>> c = new Vector<List<Edge>>();
    for (int i = 0; i < b.length; i++) {
      c.add(new LinkedList<Edge>());
    }
    for (int i = 0; i < N; i++) {
      if (b[i] == 0) continue;
      for (int j = 0; j < N; j++) {
        if (b[j + N] == 0) continue;
        if (C[i][j] == maxC) continue;
        c.get(i).add(new Edge(j + N, C[i][j]));
        sourcesThatFlowNotOnlyToThresh.add(i);
        sinksThatGetFlowNotOnlyFromThresh.add(j + N);
      }
    }

    // converting all sinks to negative
    for (int i = N; i < 2 * N; i++) { b[i] = -b[i]; }

    // add edges from/to threshold node,
    // note that costs are reversed to the paper (see also remark* above)
    // It is important that it will be this way because of remark* above.
    for (int i = 0; i < N; ++i) {
      c.get(i).add(new Edge(THRESHOLD_NODE, 0));
    }
    for (int j = 0; j < N; ++j) {
      c.get(THRESHOLD_NODE).add(new Edge(j + N, maxC));
    }

    // artificial arcs - Note the restriction that only one edge i,j is
    // artificial so I ignore it...
    for (int i = 0; i < ARTIFICIAL_NODE; i++) {
      c.get(i).add(new Edge(ARTIFICIAL_NODE, maxC + 1));
      c.get(ARTIFICIAL_NODE).add(new Edge(i, maxC + 1));
    }

    // remove nodes with supply demand of 0
    // and vertexes that are connected only to the
    // threshold vertex
    int currentNodeName = 0;
    // Note here it should be vector<int> and not vector<int>
    // as I'm using -1 as a special flag !!!
    int REMOVE_NODE_FLAG = -1;
    Vector<Integer> nodesNewNames = new Vector<Integer>();
    Vector<Integer> nodesOldNames = new Vector<Integer>();
    for (int i = 0; i < b.length; i++) {
      nodesNewNames.add(REMOVE_NODE_FLAG);
      nodesOldNames.add(0);
    }
    for (int i = 0; i < N * 2; i++) {
      if (b[i] != 0) {
        if (sourcesThatFlowNotOnlyToThresh.contains(i)
          || sinksThatGetFlowNotOnlyFromThresh.contains(i)) {
          nodesNewNames.set(i, currentNodeName);
          nodesOldNames.add(i);
          currentNodeName++;
        } else {
          if (i >= N) {
            preFlowCost -= (b[i] * maxC);
          }
          b[THRESHOLD_NODE] =  b[THRESHOLD_NODE] + b[i]; // add mass(i<N) or deficit (i>=N)
        }
      }
    }
    nodesNewNames.set(THRESHOLD_NODE, currentNodeName);
    nodesOldNames.add(THRESHOLD_NODE);
    currentNodeName++;
    nodesNewNames.set(ARTIFICIAL_NODE, currentNodeName);
    nodesOldNames.add(ARTIFICIAL_NODE);
    currentNodeName++;

    long[] bb = new long[b.length];
    int j = 0;
    for (int i = 0; i < b.length; i++) {
      if (nodesNewNames.get(i) != REMOVE_NODE_FLAG) {
        bb[j] = b[i];
        j++;
      }
    }

    Vector<List<Edge>> cc = new Vector<List<Edge>>();
    for (int i = 0; i < bb.length; i++) {
      cc.add(new LinkedList<Edge>());
    }
    for (int i = 0; i < c.size(); i++) {
      if (nodesNewNames.get(i) == REMOVE_NODE_FLAG)
        continue;
      for (Edge it : c.get(i)) {
        if (nodesNewNames.get(it._to) != REMOVE_NODE_FLAG) {
          cc.get(nodesNewNames.get(i)).add(
            new Edge(nodesNewNames.get(it._to), it._cost));
        }
      }
    }

    MinCostFlow mcf = new MinCostFlow();

    long myDist;

    Vector<List<Edge0>> flows = new Vector<List<Edge0>>(bb.length);
    for (int i = 0; i < bb.length; i++) {
      flows.add(new LinkedList<Edge0>());
    }

    long mcfDist = mcf.compute(bb, cc, flows);

    myDist = preFlowCost + // pre-flowing on cases where it was possible
      mcfDist + // solution of the transportation problem
      (absDiffSumPSumQ * extraMassPenalty); // emd-hat extra mass penalty

    return myDist;
  }

  public static double emdHat(double[] P, double[] Q, double[][] C, double extraMassPenalty) {

    // This condition should hold:
    // ( 2^(sizeof(CONVERT_TO_T*8)) >= ( MULT_FACTOR^2 )
    // Note that it can be problematic to check it because
    // of overflow problems. I simply checked it with Linux calc
    // which has arbitrary precision.
    double MULT_FACTOR = 1000000;

    // Constructing the input
    int N = P.length;
    long[] iP = new long[N];
    long[] iQ = new long[N];
    long[][] iC = new long[N][N];

    // Converting to CONVERT_TO_T
    double sumP = 0.0;
    double sumQ = 0.0;
    double maxC = C[0][0];
    for (int i = 0; i < N; i++) {
      sumP += P[i];
      sumQ += Q[i];
      for (int j = 0; j < N; j++) {
        if (C[i][j] > maxC)
          maxC = C[i][j];
      }
    }
    double minSum = Math.min(sumP, sumQ);
    double maxSum = Math.max(sumP, sumQ);
    double PQnormFactor = MULT_FACTOR / maxSum;
    double CnormFactor = MULT_FACTOR / maxC;
    for (int i = 0; i < N; i++) {
      iP[i] = (long) (Math.floor(P[i] * PQnormFactor + 0.5));
      iQ[i] = (long) (Math.floor(Q[i] * PQnormFactor + 0.5));
      for (int j = 0; j < N; j++)
        iC[i][j] = (long) (Math.floor(C[i][j] * CnormFactor + 0.5));
    }

    // computing distance without extra mass penalty
    double dist = emdHatImplLongLongInt(iP, iQ, iC, 0);
    // unnormalize
    dist = dist / PQnormFactor;
    dist = dist / CnormFactor;

    // adding extra mass penalty
    if (extraMassPenalty == -1)
      extraMassPenalty = maxC;
    dist += (maxSum - minSum) * extraMassPenalty;

    return dist;
  }

  public static class Test {
    static double[] a0 = {1.0, 0.0, 0.0, 0.0};
    static double[] a1 = {0.0, 1.0, 0.0, 0.0};
    static double[] a2 = {0.0, 1.0, 1.0, 0.0};
    static double[] b0 = {1.0, 0.31350830458876927, 0.475451529763324, 0.710099174235318, 0.8180547959863713, 0.8501705482451378, 0.7091117393023645, 0.3421407576224318, 0.0, 0.0, 0.8648715755286225, 0.0, 0.0, 0.2296406823738588, 0.32854154105225764, 0.41240916326716803, 0.2556550109727834, 0.0, 0.0, 0.0, 0.9688367289608703, 0.0, 0.15675415229438464, 0.2556550109727834, 0.4473841617389759, 0.5493536070363795, 0.4274423997306564, 0.0, 0.0, 0.0, 0.9897035895139639, 0.0, 0.0, 0.3659766656118061, 0.49450429339199403, 0.6613006436919866, 0.5263432584090552, 0.0, 0.0, 0.0, 0.9501732176368471, 0.0989008586783988, 0.0989008586783988, 0.3956034347135952, 0.5865550152810103, 0.710099174235318, 0.6355631023347673, 0.2556550109727834, 0.0, 0.0, 0.8570091629463844, 0.0, 0.0, 0.4042535848157063, 0.5841965520250411, 0.7250339741455023, 0.6900589756736945, 0.3421407576224318, 0.0, 0.0, 0.6999031392570128, 0.0, 0.15675415229438464, 0.2776498124065264, 0.5399424749792293, 0.6234493289150748, 0.6160355170421022, 0.31350830458876927, 0.0, 0.0, 0.434403964700911, 0.0, 0.0, 0.0, 0.3863948346682434, 0.434403964700911, 0.2776498124065264, 0.0989008586783988, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    static double[] b1 = {0.7690558279672058, 0.5431277903401206, 0.7400048870218827, 0.9431329651179853, 1.0, 0.8608371807886335, 0.6014104886149799, 0.0, 0.0, 0.0, 0.3620851935600804, 0.0, 0.0, 0.0, 0.0, 0.1810425967800402, 0.1810425967800402, 0.0, 0.0, 0.0, 0.8073470804011144, 0.2869457269295445, 0.3620851935600804, 0.3620851935600804, 0.6014104886149799, 0.5082508251725361, 0.0, 0.0, 0.0, 0.0, 0.8407357836698793, 0.1810425967800402, 0.5082508251725361, 0.6892934219525761, 0.7824530853950201, 0.626304483621074, 0.42036789183493967, 0.0, 0.0, 0.0, 0.8189574032199598, 0.42036789183493967, 0.649030920489625, 0.7241703871201608, 0.7241703871201608, 0.8300735172696652, 0.2869457269295445, 0.0, 0.0, 0.0, 0.8703360187326165, 0.42036789183493967, 0.5082508251725361, 0.7073136187644842, 0.921047483801923, 0.7549340506391291, 0.626304483621074, 0.0, 0.0, 0.0, 0.573891453859089, 0.3620851935600804, 0.46798832370958465, 0.5082508251725361, 0.3620851935600804, 0.5431277903401206, 0.1810425967800402, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    static double[] b2 = {0.8430654934475055, 0.6239426073894736, 0.7730847346217581, 0.9313448972142149, 1.0, 0.8783820744571478, 0.5344908771065096, 0.1686130986895011, 0.0, 0.0, 0.6419699112188565, 0.0, 0.5058392960685033, 0.604471635932257, 0.5833054849228712, 0.5058392960685033, 0.1686130986895011, 0.0, 0.0, 0.0, 0.3372261973790022, 0.1686130986895011, 0.0, 0.5058392960685033, 0.43585853724275586, 0.43585853724275586, 0.2672454385532548, 0.0, 0.0, 0.0, 0.7287336883921703, 0.43585853724275586, 0.1686130986895011, 0.5833054849228712, 0.5601205897026693, 0.47335681252935546, 0.3372261973790022, 0.0, 0.0, 0.0, 0.7730847346217581, 0.43585853724275586, 0.5058392960685033, 0.5344908771065096, 0.6587529295664228, 0.6239426073894736, 0.1686130986895011, 0.0, 0.0, 0.0, 0.7830149820263362, 0.3915074910131681, 0.43585853724275586, 0.7162562210501102, 0.6587529295664228, 0.6891997754414121, 0.2672454385532548, 0.1686130986895011, 0.0, 0.0, 0.5344908771065096, 0.0, 0.3372261973790022, 0.2672454385532548, 0.3915074910131681, 0.43585853724275586, 0.1686130986895011, 0.0, 0.0, 0.0, 0.1686130986895011, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

    static double getValue(double[] map, int x, int y, int bins) { return map[(y * bins) + x]; }
    static Signature getSignature(double[] map, int bins) {
      // find number of entries in the sparse matrix
      int n = 0;
      for (int x = 0; x < bins; x++) {
        for (int y = 0; y < bins; y++) {
          if (getValue(map, x, y, bins) > 0) {
            n++;
          }
        }
      }

      // compute features and weights
      double[][] features = new double[n][];
      double[] weights = new double[n];
      int i = 0;
      for (int x = 0; x < bins; x++) {
        for (int y = 0; y < bins; y++) {
          double val = getValue(map, x, y, bins);
          if (val > 0) {
            features[i] = new double[]{x,y};
            weights[i] = val;
            i++;
          }
        }
      }

      Signature signature = new Signature();
      signature._nfeat=n;
      signature._features = features;
      signature._weights = weights;
      return signature;
    }

    static double emdDist(double[] map1, double[] map2, int bins) {
      Signature sig1 = getSignature(map1, bins);
      Signature sig2 = getSignature(map2, bins);
      return JEMD.distance(sig1, sig2, -1);
    }

    public static void main(String[] args) {
      System.out.println("test 1: " + emdDist(a0, a0, 2) + " [expected: 0.0]");
      System.out.println("test 1: " + emdDist(a0, a1, 2) + " [expected: 1.0]");
      System.out.println("test 2: " + emdDist(a0, a2, 2) + " [expected: 2.0]");
      System.out.println("test 3: " + emdDist(b0, b1, 10) + " [expected: 19.1921]");
      System.out.println("test 4: " + emdDist(b0, b2, 10) + " [expected: 25.7637]");
    }
  }
}