addpath('matlab_func');
common_settings;

workloads = {'BB', 'TPC-DS','TPC-H'};
worloadFolders = {'BB', 'TPCDS', 'TPCH'};
% workloads = {'BB'};
% worloadFolders = {''};

%%


%%
% result_folder= '.';
result_folder= ['result/20170117_err/' ];

if true
  avgDuration = -0.9:0.1:0.9;
    speedfair_compl_files = {
      'SpeedFair-output_err-0.9.csv';
      'SpeedFair-output_err-0.8.csv';
      'SpeedFair-output_err-0.7.csv';
      'SpeedFair-output_err-0.6.csv';
      'SpeedFair-output_err-0.5.csv';
      'SpeedFair-output_err-0.4.csv';
      'SpeedFair-output_err-0.3.csv';
      'SpeedFair-output_err-0.2.csv';
      'SpeedFair-output_err-0.1.csv';
      'SpeedFair-output_err0.0.csv';
      'SpeedFair-output_err0.1.csv';
      'SpeedFair-output_err0.2.csv';
      'SpeedFair-output_err0.3.csv';
      'SpeedFair-output_err0.4.csv';
      'SpeedFair-output_err0.5.csv';
      'SpeedFair-output_err0.6.csv';
      'SpeedFair-output_err0.7.csv';
      'SpeedFair-output_err0.8.csv';
      'SpeedFair-output_err0.9.csv';
      'SpeedFair-output_err_base.csv';
      };
  xVals = avgDuration;
elseif false
  avgDuration = [-0.5, -0.2, 0.0, 0.2, 0.5];
    speedfair_compl_files = {
      'SpeedFair-output_err-0.5.csv';
      'SpeedFair-output_err-0.2.csv';
      'SpeedFair-output_err0.0.csv';
      'SpeedFair-output_err0.2.csv';
      'SpeedFair-output_err0.5.csv';
      'SpeedFair-output_err_base.csv';
      };
  xVals = avgDuration;
elseif false
    avgDuration = [-0.2, 0.0, 0.2];
    speedfair_compl_files = {
      'SpeedFair-output_err-0.2.csv';
      'SpeedFair-output_err0.0.csv';
      'SpeedFair-output_err0.2.csv';
      'SpeedFair-output_err_base.csv';
      };
  xVals = avgDuration;
end


%%


figIdx = 0;

% fig_path = 'figs\';
%%


plots  = [true, false];

if plots(1) 
    INTERACTIVE_QUEUE = 'bursty';
   
    figure;
   scrsz = get(groot,'ScreenSize');   
   Y_MAX = -9999;
  for i=1:length(workloads)
    
   output_folder = [result_folder worloadFolders{i} '/output/'];
   
   [ speedfair_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, INTERACTIVE_QUEUE);     
   len = length(speedfair_avg_compl_time);
   baseline = speedfair_avg_compl_time(len/2);
   optimal = speedfair_avg_compl_time(len);
   performance_factors = speedfair_avg_compl_time(1:len-1)/baseline;
   plot(xVals*100, performance_factors, workloadLineStyles{i}, 'LineWidth',LineWidth);
   hold on;
   if max(performance_factors) > Y_MAX
     Y_MAX = max(performance_factors);
   end
  end
  xLabel=strEstimationErr;
  yLabel=strPerformaceFactor;
  legendStr=workloads;

  legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
  set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
  xlabel(xLabel,'FontSize',fontAxis);
  ylabel(yLabel,'FontSize',fontAxis);
  ylim([0 ceil(Y_MAX)]);
  set(gca,'FontSize',fontAxis);
   
   if is_printed
       figIdx=figIdx +1;
      fileNames{figIdx} = 'sen_analysis_est_err';
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
    pdfFile = [ LOCAL_FIG fileName '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end