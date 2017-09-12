clear all; close all; clc;

numOfJobs = 100;

minVal = -3;
maxVal = 4;
stdev = 0.5;
meanVal = 1;

%%
y = stdev.*randn(numOfJobs, 1) + meanVal;

y = max(y,minVal);
y = min(y,maxVal);

mean(y)
std(y)

betas = '{';
for i=1:numOfJobs  
    betas = [betas  num2str(10^y(i)) ','];    
end
betas = [betas '};'];

fid=fopen('betas.txt','w');

fprintf(fid, betas);

fprintf(fid, '\n');