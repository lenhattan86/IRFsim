clear; close all; clc;
addpath('matlab_func');
addpath('../ccra/results/');
common_settings;
is_printed = true;

MAX_DUR = 350;

inputFiles = {'pdf/queries_bb_FB_distr.csv', 'pdf/queries_tpcds_FB_distr_new.csv', 'pdf/queries_tpch_FB_distr.csv'}; 
workloads  = {'BB', 'TPC-DS', 'TPC-H'};


fig_path = ['/home/tanle/projects/EuroSys17/fig/'];
plots = [ false true]

figSize = figSizeFourFifthCol;

%% PDF
if(plots(1))
  % inputFile = 'pdf/queries_bb_FB_distr.csv'; workload='BB';
  % inputFile = 'pdf/queries_tpch_FB_distr.csv'; workload='TPC-H';
  inputFile = 'pdf/queries_tpcds_FB_distr_new.csv'; workload='TPC-DS';

  
  [durations,num_tasks] = importAllTaskInfo(inputFile);


  allDurations = zeros(sum(num_tasks),1);

  idx = 0;
  for i=1:length(num_tasks)
      for j=1:num_tasks(i)
          idx=idx+1;
          allDurations(idx) = durations(i);
      end
  end

  allDurations = allDurations(allDurations<MAX_DUR);
  hist(allDurations, 100);

  xLabel='task duration (secs)';
  yLabel='Number of tasks';


  set (gcf, 'Units', 'Inches', 'Position', figSize, 'PaperUnits', 'inches', 'PaperPosition', figSize);
  xlabel(xLabel,'FontSize',fontAxis);
  ylabel(yLabel,'FontSize',fontAxis);
  xlim([0 MAX_DUR]);
  set(gca,'FontSize',fontAxis);

  if is_printed
     figIdx=figIdx +1;
     fileNames{figIdx} = 'hist';
     epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
     print ('-depsc', epsFile);
  end
end
%% cdf

if(plots(2))  
  figure

  for wIdx=1:3
      inputFile = inputFiles{wIdx}; workload=workloads{wIdx};

      [durations,num_tasks] = importAllTaskInfo(inputFile);
      allDurations = zeros(sum(num_tasks),1);
      

      idx = 0;
      for i=1:length(num_tasks)
        for j=1:num_tasks(i)
            idx=idx+1;
            allDurations(idx) = durations(i);
        end
      end
      allDurations = allDurations(allDurations<MAX_DUR);
      
      mean(allDurations)
%       std(allDurations)


      [f,x]=ecdf(allDurations);
      plot(x,f, workloadLineStyles{wIdx},'LineWidth',LineWidth);
      hold on;
  end
  legendStr = workloads;
  legend(legendStr,'Location','southeast','FontSize',fontLegend,'Orientation','vertical');

  xLabel='task duration (secs)';
  yLabel='cdf';

  set (gcf, 'Units', 'Inches', 'Position', figSize, 'PaperUnits', 'inches', 'PaperPosition', figSize);
  xlabel(xLabel,'FontSize',fontAxis);
  ylabel(yLabel,'FontSize',fontAxis);
  xlim([0 MAX_DUR]);
  set(gca,'FontSize',fontAxis);

  if is_printed
     figIdx=figIdx +1;
     fileNames{figIdx} = 'cdf';
     epsFile = [ LOCAL_FIG fileNames{figIdx} '.eps'];
     print ('-depsc', epsFile);
  end
end

%%
return;
%% convert to pdf

for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    pdfFile = [ fig_path fileName '.pdf']   
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end
