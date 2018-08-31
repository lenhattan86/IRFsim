clear; close all;
addpath('matlab_func');
common_settings;
is_printed = false;

%workload='BB';
workload='SIMPLE';

num_batch_queues = 3;
num_interactive_queue = 0;
num_queues = num_batch_queues + num_interactive_queue;
START_TIME = 0; END_TIME = 1000;  STEP_TIME = 1;
cluster_size = 3;

CPUCap = cluster_size * 64;
GPUCap = cluster_size;
MemCap = cluster_size* 96; 

% figureSize = [1 1 2/3 2/3].* figSizeOneCol;
figureSize = [1 1 4/5 6/5].* figSizeOneCol;
legendSize = [1 1 4/5 1] .* legendSize;

[jobIds_allox, compltimes_allox] = importJobComplTimes('log/AlloX_compltimes.csv');
[jobIds_ES,    compltimes_ES] = importJobComplTimes('log/ES_compltimes.csv');


[~, alloxIds]=sort(jobIds_allox);
[~, ESIds]=sort(jobIds_ES);

slowDownJobs = compltimes_ES(ESIds) .- compltimes_allox(alloxIds);