addpath('matlab_func');
common_settings;

workload='BB';
% workload='TPCDS';
% workload='TPCH';

%%


%%
result_folder= '';
% result_folder= ['result/20170105/' workload '/'];
result_folder= ['result/20170113/' workload '/'];


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
elseif true
  avgDuration = [2.0, 4.0, 6.0, 8.0, 10.0, 15.0, 20.0, 30.0, 40.0, 60.0, 80.0, 100.0];
    speedfair_compl_files = {
                      'SpeedFair-output_t0.125x.csv';
                      'SpeedFair-output_t0.25x.csv';
                      'SpeedFair-output_t0.5x.csv';
                      'SpeedFair-output_t1.0x.csv';
                      'SpeedFair-output_t2.0x.csv';
                      'SpeedFair-output_t4.0x.csv';
                      'SpeedFair-output_t8.0x.csv'};
end


%%
output_folder = [result_folder  'output/'];

figIdx = 0;

% fig_path = 'figs\';
%%


plots  = [true, false];

if plots(1) 
    INTERACTIVE_QUEUE = 'bursty';
   
   [ speedfair_avg_compl_time ] = obtain_compl_time( output_folder, speedfair_compl_files, INTERACTIVE_QUEUE);
   
   figure;
   scrsz = get(groot,'ScreenSize');   
   plot(scaleUps, speedfair_avg_compl_time, 'LineWidth',LineWidth);
   %title('Average completion time of interactive jobs','fontsize',fontLegend);
   xLabel='task duration scale factor';
   ylim([0 100]);
  yLabel='time (seconds)';
  legendStr={'BB', 'TPC-DS', 'TPC-H'};

  legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
  set (gcf, 'Units', 'Inches', 'Position', figSizeOneCol, 'PaperUnits', 'inches', 'PaperPosition', figSizeOneCol);
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
return;
%%

for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ LOCAL_FIG fileName '_' workload '.pdf'];    
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end