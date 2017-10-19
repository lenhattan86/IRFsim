
%% Test SpeedUp

%an example with randomized 100 jobs
%cpugpu = [0.7 0.4 0.3]'; memory= [0.03 0.1 0.1]';
cpugpu = [0.9 0.7 0.3]'; memory= [0.1 0.05 0.1]';
beta = [10, 0.1, 1]';
report = [cpugpu beta memory];
[k,alloc,envy]= speedup(report);

