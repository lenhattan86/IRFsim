addpath('matlab_func');
common_settings;

workloads = {'BB', 'TPC-DS','TPC-H'};
worloadFolders = {'BB', 'TPCDS', 'TPCH'};

fig_path = ['../EuroSys17/fig/'];


% workloads = {'BB'};
% worloadFolders = {''};

%%


%%
% result_folder= '.';
% result_folder= ['result/20170105/' ];
% result_folder= ['result/20170113/' ];
result_folder= ['result/20170116_avg/' ];

baseline = 27;

if false
    scaleUps = [0.125, 0.25, 0.5, 1, 2, 4, 8];
    speedfair_compl_files = {
                      'SpeedFair-output_t0.125x.csv';
                      'SpeedFair-output_t0.25x.csv';
                      'SpeedFair-output_t0.5x.csv';
                      'SpeedFair-output_t1.0x.csv';
                      'SpeedFair-output_t2.0x.csv';
                      'SpeedFair-output_t4.0x.csv';
                      'SpeedFair-output_t8.0x.csv'};  
    xVals = scaleUps;
elseif true
%   avgDuration = [2.0, 4.0, 6.0, 8.0, 10.0, 15.0, 20.0, 30.0, 40.0, 60.0, 80.0, 100.0];
    avgDuration = [2.0, 4.0, 6.0, 8.0, 10.0, 15.0, 20.0, 30.0, 40.0, 60.0, 80.0];
    speedfair_compl_files = {
                      'SpeedFair-output_avg2.0.csv';
                      'SpeedFair-output_avg4.0.csv';
                      'SpeedFair-output_avg6.0.csv';
                      'SpeedFair-output_avg8.0.csv';
                      'SpeedFair-output_avg10.0.csv';
                      'SpeedFair-output_avg15.0.csv';
                      'SpeedFair-output_avg20.0.csv';
                      'SpeedFair-output_avg30.0.csv';
                      'SpeedFair-output_avg40.0.csv';
                      'SpeedFair-output_avg60.0.csv';
                      'SpeedFair-output_avg80.0.csv';
                      'SpeedFair-output_avg100.0.csv'};
  xVals = avgDuration;

elseif false
  avgDuration = [2.0, 6.0, 10.0, 15.0, 100.0];
    speedfair_compl_files = {
                      'SpeedFair-output_avg2.0.csv';
                      'SpeedFair-output_avg6.0.csv';
                      'SpeedFair-output_avg10.0.csv';
                      'SpeedFair-output_avg15.0.csv';
                      'SpeedFair-output_avg100.0.csv'
                      };
  xVals = avgDuration;
end


%%


figIdx = 0;

% fig_path = 'figs\';
%%


plots  = [true, false];
% figureSize = figSizeTwothirdCol;
figureSize = figSizeFourFifthCol;
if plots(1) 
   INTERACTIVE_QUEUE = 'bursty';
   
   figure;
   scrsz = get(groot,'ScreenSize');   
   
  for i=1:length(workloads)
   output_folder = [result_folder worloadFolders{i} '/output/'];
   
   [ speedfair_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, INTERACTIVE_QUEUE);  
   
   plot(xVals, speedfair_avg_compl_time(1:length(xVals)),  workloadLineStyles{i}, 'LineWidth',LineWidth);
   hold on;
  end
  xLabel='avg. task duration of TQ jobs (secs)';
  ylim([0 130]);
  yLabel=strAvgComplTime;
  legendStr=workloads;

  legend(legendStr,'Location','south','FontSize',fontLegend,'Orientation','horizontal');
  set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
  xlabel(xLabel,'FontSize',fontAxis);
  ylabel(yLabel,'FontSize',fontAxis);
  set(gca,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'sen_analysis_task_duration';
      epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
        print ('-depsc', epsFile);
   end
   
end

%%
fileNames
return;
%%

for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end