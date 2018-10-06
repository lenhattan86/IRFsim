addpath('matlab_func');
common_settings;
is_printed = 1;
%%
barWidth = 0.5;
queue_num = 20;
cluster_size=10;
figureSize = figSizeThreeFourth;
plots  = [true false];

% files = {'DRF', 'ES',  'AlloX'};
% speedups = [0.1, 0.5, 1.0]
errs = 0:0.1:0.6;
files = {'DRFFIFO','DRF', 'ES',  'AlloX'};
methods = {'DRFF', strDRF, strES,  strAlloX};
DRFFIFOId = 1; ESId = 2; AlloXId = 4;
methodColors = {colorES; colorDRF; colorProposed};

%% load data
resVals = ones(length(files),length(errs)); 
for i=1:length(files)
    for j=1:length(errs)
        extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) '_e' sprintf('%1.1f',errs(j))];
        outputFile = [ 'output/' files{i} '-output' extraStr  '.csv'];
        [JobIds, startTimes, endTimes, durations, queueNames] = import_compl_time_real_job(outputFile);
        if(~isnan(durations))
            resVals(i,j) = mean(durations);
        end
    end
end

%%
if plots(1)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');       
    
%    plot(errs, resVals(DRFFIFOId,1)./resVals(AlloXId,:) , 'LineWidth', lineWidth);
%     hold on;
    yValues = (resVals(ESId,:)-resVals(AlloXId,:))./resVals(ESId,:) * 100;
    plot(errs*100, yValues , 'LineWidth', lineWidth);
    
   xLabel=strErrorStd;
   yLabel='improvement vs. DRFFIFO (%)';
   legendStr=methods;    
   ylim([0 max(yValues)]);
%     ylim([0 0.4]);
   set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
   ylabel(yLabel,'FontSize', fontAxis);    
   xlabel(xLabel,'FontSize', fontAxis);   
   fileNames{figIdx} = 'analysis_err';
end

%%
return;
%%
extra='';
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);    
    pdfFile = [ fig_path fileName '.pdf']  
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end