clear all; close all; clc;

numOfQueues = 1;
% lambda = 20;
lambda = 17;
queueSize = 2000;
toWrite = zeros(numOfQueues,queueSize);
for i=1:numOfQueues
   interTimeArrival = poissrnd(lambda,1,queueSize);
%    toWrite(i,:) = sort(interTimeArrival);
   toWrite(i,:) = interTimeArrival;
end
csvwrite('bin/poissrnd.csv',toWrite);