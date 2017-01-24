addpath('matlab_func');
common_settings;

workloads = {'BB', 'TPC-DS','TPC-H'};
worloadFolders = {'BB', 'TPCDS', 'TPCH'};
% workloads = {'BB'};
% worloadFolders = {''};

%%


%%
% result_folder= '.';
result_folder= ['result/20170124_err/' ];

if true
  errRate = 0.0:0.1:0.5;
    speedfair_compl_files = {     
      'SpeedFair-output_err0.0.csv';
      'SpeedFair-output_err0.1.csv';
      'SpeedFair-output_err0.2.csv';
      'SpeedFair-output_err0.3.csv';
      'SpeedFair-output_err0.4.csv';
      'SpeedFair-output_err0.5.csv';      
      };
  xVals = errRate;
end
Y_MAX = 1.25;

%%


figIdx = 0;

% fig_path = 'figs\';
%%


plots  = [true, false];

if plots(1) 
    INTERACTIVE_QUEUE = 'bursty';
   
    figure;
   scrsz = get(groot,'ScreenSize');   
   
  for i=1:length(workloads)
    
   output_folder = [result_folder worloadFolders{i} '/output/'];
   
   [ speedfair_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, INTERACTIVE_QUEUE);        
   baseline = speedfair_avg_compl_time(1);
    yVals = speedfair_avg_compl_time/baseline;
 
   plot(xVals*100,yVals , workloadLineStyles{i}, 'LineWidth',LineWidth);
   hold on;
   if max(yVals) > Y_MAX
     Y_MAX = max(yVals);
   end
  end
  xLabel=strEstimationErr;
  yLabel=strPerformaceFactor;
  legendStr=workloads;

  legend(legendStr,'Location','south','FontSize',fontLegend,'Orientation','horizontal');
  set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
  xlabel(xLabel,'FontSize',fontAxis);
  ylabel(yLabel,'FontSize',fontAxis);
  ylim([0 Y_MAX]);
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