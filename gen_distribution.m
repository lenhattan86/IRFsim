clear all; close all; clc;

numOfQueues = 10;
lambda = 20;
queueSize = 1000;
toWrite = zeros(numOfQueues,queueSize);
for i=1:numOfQueues
   interTimeArrival = poissrnd(lambda,1,queueSize);
%    toWrite(i,:) = sort(interTimeArrival);
   toWrite(i,:) = interTimeArrival;
end
csvwrite('dist_gen/poissrnd.csv',toWrite);