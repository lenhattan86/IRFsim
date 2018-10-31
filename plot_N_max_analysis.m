addpath('matlab_func');
common_settings;
%%
barWidth = 0.5;
queue_num = 10;
cluster_size=20;
GPU_CAP = 10;
CPU_CAP = GPU_CAP*32;
MEM_CAP = (GPU_CAP*2) * 64;
figureSize = figSizeThreeFourth;
plots  = [false true];
files = {'DRFFIFO','DRF', 'ES', 'DRFExt', 'AlloX', 'SRPT', 'AlloXopt'};
methods = {strDRFFIFO, strDRFSJF, strES, strDRFExt, strAlloX, strSRPT, strAlloXopt};
DRFFId = 1; DRFId = 2; ESId = 3; DRFExtId = 4;  AlloXId = 5; SRPTId = 6; AlloXoptId =7;
colors = {colorDRFFIFO, colorDRFSJF,  colorES, colorDRFExt, colorAlloX, colorSRPT};
plotMethods = [AlloXId, SRPTId];


%% load data & compute what we need
N_maxs = [10 20 30 50];
ESAvg = 105*ones(size(N_maxs));
AlloXopt = 75.93*ones(size(N_maxs));
allox = [94.4868 80.6163 76.8223 75.93];
%%
if plots(1)    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');   
    hold on;
    plot(N_maxs, ESAvg, lineES, 'Color', colorES, 'LineWidth', lineWidth);       
    plot(N_maxs, allox, lineAlloX, 'Color', colorAlloX, 'LineWidth', lineWidth);       
    plot(N_maxs, AlloXopt, lineAlloXopt, 'Color', colorAlloXopt, 'LineWidth', lineWidth);       
    hold off;    
    xLabel='Max job subset size (N_{max})';
    yLabel=strAvgCmplt;
    legendStr={strES, strAlloX, strAlloXopt};    
%     colors = {colorES, colorAlloX, colorSRPT};
    xlim([10 50]);    
    ylim([0 max(ESAvg)*1.1 ]);    
%     legend({'AlloX', 'ES+RP'},'Location', 'best','FontSize',fontAxis);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    %     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    xlabel(xLabel,'FontSize',fontAxis);   
    fileNames{figIdx} = 'analysis_N_max';
    legend(legendStr, 'Location','best','FontSize', fontLegend);    
end

%%
%%
if plots(2)   
    
    %% load data
    Nmaxs = [20, 30, 50, 100, 150, 200];
%     Nmaxs = [20, 30, 50, 100];
    caps = [20, 30, 40];
    avgComplt = zeros(length(plotMethods), length(caps), length(Nmaxs))-1;
    
    
    
    for i = 1:length(plotMethods)
        iMethod = plotMethods(i);
        for j=1:length(caps)
            cap = caps(j);
            for k=1:length(Nmaxs)        
                nMax = Nmaxs(k);
                extraStr=['_' num2str(10) '_' num2str(cap)  '_c'  num2str(cap) '_n' num2str(nMax) ];
                outputFile = [ 'output/' files{iMethod} '-output' extraStr  '.csv'];
                [JobIds, startTimes, endTimes, durations, queueNames, startRunningTimes, runningTimes] ...
                = import_compl_time(outputFile);  
                
                fullJobsIndices= find(JobIds>=0);
                
                if length(durations) >= 10000
                    avgComplt(i,j,k) = mean(durations(fullJobsIndices));
                else 
                    avgComplt(i,j,k) = nan;
                end
            end
        end
    end
    
    %%
    simTimeAlloX = [];
       
    %% plot AlloX only.
    yVals = squeeze(avgComplt(1,:,:));   
    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');   
    hold on;
    for iLine = 1:length(yVals(:,1))
        plot(Nmaxs, yVals(iLine,:), randomLines{mod(iLine-1, 4)+1},  'LineWidth', lineWidth);       
    end
    hold off;    
    xLabel='Max job subset size';
    yLabel=strAvgCmplt;
    legendStr={'Capacity=20', 'Capacity=30', 'Capacity=40'};    
%     colors = {colorES, colorAlloX, colorSRPT};
    xlim([Nmaxs(1) Nmaxs(end)]);    
    ylim([0 max(max(yVals))*1.1 ]);    
%     legend({'AlloX', 'ES+RP'},'Location', 'best','FontSize',fontAxis);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    %     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    xlabel(xLabel,'FontSize',fontAxis);   
    fileNames{figIdx} = 'analysis_N_max_ext';
    legend(legendStr, 'Location','best','FontSize', fontLegend);    
end


%%
if~is_printed
    return;
end
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