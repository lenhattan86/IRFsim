clear; close all; clc;

is_printed = true;
fig_path = 'C:\Users\lenha\Documents\GitHub\NSDI17\fig\';
interactive_time  = [2.0 4.0 4.0 5.0; 1.9 3.5 3.5 4.2; 2.0 2.0 2.0 2.0;]
figure(1);
bar(interactive_time', 'group');
ylabel('time (seconds)');
xlabel('number of batch queues');
legend('DRF','SpeedFair','DRF weight','Location','northwest');
ylim([0 6]);
set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
if is_printed
   print ('-depsc', [fig_path 'sim_interactive_compl_time.eps']);
end

%%
folder = 'result_20160926/log/';
num_queues = 5;
max_time_step = 50;
linewidth=2;
barwidth = 1.0;


logFile = [ folder 'DRF-output49_1_50.csv'];3
figure(2);
[queueNames, res1, res2] = import_res_usage(logFile);
resCutOff = res1(1:max_time_step*num_queues);
shapeRes1 = reshape(resCutOff,num_queues,max_time_step);
bar(shapeRes1',barwidth,'stacked');
ylim([0 200]);
legend('interactive','batch01','batch02','batch03','batch04');
set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
if is_printed
   print ('-depsc', [fig_path 'res_usage_drf.eps']);
end

logFile = [ folder 'SpeedFair-output49_1_50.csv'];
figure(3);
[queueNames, res1, res2] = import_res_usage(logFile);
resCutOff = res1(1:max_time_step*num_queues);
shapeRes1 = reshape(resCutOff,num_queues,max_time_step);
bar(shapeRes1',barwidth,'stacked');
ylim([0 200]);
legend('interactive','batch01','batch02','batch03','batch04');
set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
if is_printed
   print ('-depsc', [fig_path 'res_usage_speedfair.eps']);
end

logFile = [ folder 'DRF-w-output49_1_50.csv'];
figure(4);
[queueNames, res1, res2] = import_res_usage(logFile);
resCutOff = res1(1:max_time_step*num_queues);
shapeRes1 = reshape(resCutOff,num_queues,max_time_step);
bar(shapeRes1',barwidth,'stacked');
ylim([0 200]);
legend('interactive','batch01','batch02','batch03','batch04');
set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
if is_printed
   print ('-depsc', [fig_path 'res_usage_drf-w.eps']);
end

