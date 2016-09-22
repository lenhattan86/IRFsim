clear; close all; clc;

%% plot CPU
fig_path = 'C:\Users\lenha\Documents\GitHub\NSDI17\fig\';
interactive_time  = [2  3  4  5   6.2  7.17 8.43;  2 2 3.33 4.25 4.4 6.17 7.14]
batch_time        = [10 11 12 13  15   16.0  17;     10 10 12 13.0 14.0 15 18]
figure(1);
bar(interactive_time', 'group');
ylabel('time (seconds)');
xlabel('number of interactive jobs (not include 1 batch)');
legend('DRF','SpeedFair');
set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
print ('-depsc', [fig_path 'sim_interactive_compl_time.eps']);

figure(2);
bar(batch_time', 'group');
ylabel('time (seconds)');
xlabel('number of interactive jobs (not include 1 batch)');
legend('DRF','SpeedFair');
set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
print ('-depsc', [fig_path 'sim_batch_compl_time.eps']);


%%
avgAvailRes = zeros(2,7);
[name,resource] = importAvailResource('log/DRF-output1_1_2.csv');
avgAvailRes(1,1) = mean(resource);
[name,resource] = importAvailResource('log/DRF-output2_1_3.csv');
avgAvailRes(1,2) = mean(resource);
[name,resource] = importAvailResource('log/DRF-output3_1_4.csv');
avgAvailRes(1,3) = mean(resource);
[name,resource] = importAvailResource('log/DRF-output4_1_5.csv');
avgAvailRes(1,4) = mean(resource);
[name,resource] = importAvailResource('log/DRF-output5_1_6.csv');
avgAvailRes(1,5) = mean(resource);
[name,resource] = importAvailResource('log/DRF-output6_1_7.csv');
avgAvailRes(1,6) = mean(resource);
[name,resource] = importAvailResource('log/DRF-output7_1_8.csv');
avgAvailRes(1,7) = mean(resource);

[name,resource] = importAvailResource('log/SpeedFair-output1_1_2.csv');
avgAvailRes(2,1) = mean(resource);
[name,resource] = importAvailResource('log/SpeedFair-output2_1_3.csv');
avgAvailRes(2,2) = mean(resource);
[name,resource] = importAvailResource('log/SpeedFair-output3_1_4.csv');
avgAvailRes(2,3) = mean(resource);
[name,resource] = importAvailResource('log/SpeedFair-output4_1_5.csv');
avgAvailRes(2,4) = mean(resource);
[name,resource] = importAvailResource('log/SpeedFair-output5_1_6.csv');
avgAvailRes(2,5) = mean(resource);
[name,resource] = importAvailResource('log/SpeedFair-output6_1_7.csv');
avgAvailRes(2,6) = mean(resource);
[name,resource] = importAvailResource('log/SpeedFair-output7_1_8.csv');
avgAvailRes(2,7) = mean(resource);

utilReource = 100 - avgAvailRes;

figure(3);
bar(utilReource', 'group');
ylabel('Resource Utilization (%)');
xlabel('number of interactive jobs (not include 1 batch)');
ylim([0 110]);
legend('DRF','SpeedFair');
set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
print ('-depsc', [fig_path 'sim_res_util.eps']);

%% service curves
figure(4);
x = [0:20]
y1 = [0:10:50 70:20:350];
y2 = [0:45:225 230:5:300];

plot(x,y1, 'linewidth',2);
hold on;
plot(x,y2, 'linewidth',2);
ylabel('cpus');
xlabel('time');
ylim([0 600]);
legend('batch','interactive');
set (gcf, 'PaperUnits', 'inches', 'PaperPosition', [0.0 0 4.0 3.0]);
print ('-depsc', [fig_path 'service_curves.eps']);


