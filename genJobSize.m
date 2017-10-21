
probabilities = 0.1*ones(10,1);
xf = 1:10;
numOfSamples = 100;
[values XF DF] = genValFromProb(xf, probabilities, numOfSamples, valMean, minVal, maxVal, valStd);