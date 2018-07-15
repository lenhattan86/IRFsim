addpath('matlab_func');
common_settings;
% is_printed = 1;
%%
queue_num = 3;
figureSize = figSizeOneCol/3*2;

plots  = [true, false];

speedUps = [1 20 100];
costs = [1 20 100];


% CPU E5 2690 v3 3.6 GHz price New Egg 2018
% price_cpu_E5_2690 = 363.5;
% price_cpu_E5_2698 = 3100; % https://pcpartpicker.com/product/sBDzK8/intel-cpu-cm8064401609800
price_cpu_E5_2698 = 363.5*4; 

% speed up 
%https://www.nvidia.com/en-us/data-center/tesla-k80/
k80_nvidia_apps = {'STAC-A2','RTM','SPEC.', 'Caffe', 'MiniFE', 'LSMS', 'Clover.', 'CHRO.', 'Qua.', 'QMC.', 'HOOMD.', 'NAMD', 'LAM.', 'GRO.', 'AMBER'};
k80_speed_up = [2.2, 10.1, 5.7, 5.8, 5.65, 3.9, 6.1, 10.3, 1.2, 12.2, 6.2, 5, 3.2, 2.3, 6.7];
% https://www.bhphotovideo.com/c/product/1117682-REG/nvidia_900_22080_0000_000_tesla_k80_gpu_accelerator.html?ap=y&gclid=CjwKCAjwsdfZBRAkEiwAh2z65gMi0GVKCOJ3LEtwfeqqBgFjm7bvfKQqOxV-40A_B2g4I5n-CkMz6RoCGeUQAvD_BwE&smp=y
price_k80 = 3990; % www.serversupply.com

% P100
%https://images.nvidia.com/content/tesla/pdf/nvidia-tesla-p100-PCIe-datasheet.pdf
%https://images.nvidia.com/content/pdf/tesla/whitepaper/pascal-architecture-whitepaper.pdf
p100_nvidia_apps = {'NAMD', 'VASP', 'MILC', 'HOOMD-Blue', 'AMBER', 'Caffe'};
p1002x_speedup = [6, 7, 14, 13, 22, 15];
price_p100 = 7300; % https://www.microway.com/hpc-tech-tips/nvidia-tesla-p100-price-analysis/

GPU_NAME = 'k80';
%% Physical prices
figureSize = figSizeOneCol.*[0 0 2 2/3];
if plots(1)    
  if strcmp(GPU_NAME, 'k80')
    cost_performance_factor = k80_speed_up / (price_k80/price_cpu_E5_2698);
    apps = k80_nvidia_apps;
  elseif strcmp(GPU_NAME, 'p100')
      cost_performance_factor = p1002x_speedup / (price_k80/price_cpu_E5_2698);
      apps = p100_nvidia_apps;
  end
   
   figIdx=figIdx +1;         figures{figIdx} =figure;
   scrsz = get(groot,'ScreenSize');  
   
   bar(cost_performance_factor, 0.5);
   xLabel='apps';
   yLabel='perf.-cost ratios';
   ylabel(yLabel);
%    legendStr=methods;
  xlim([0.5 length(cost_performance_factor)+0.5]);
%     ylim([0 max(max(avgCmplt))*1.1]);
    xLabels=apps;
%     legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
    fileNames{figIdx} = 'cost_perf_physical';
end
%% VM prices
figureSize = figSizeOneCol/3*2;
% USDph
% https://cloud.google.com/gpu/
price_gpu_google  = [0.45 1.46 2.48]; % K80, P100, V100 for a single GPU

% https://aws.amazon.com/ec2/pricing/on-demand/
price_gpu_ec2_p2  = [0.90 7.2   14.4]; % p2.xlarge (1GPU), p2.8xlarge, p2.16xlarge: K80
price_gpu_ec2_p3  = [3.06 12.24 24.48 ]; % p3.2xlarge, p3.8xlarge, p3.16xlarge: V100
price_gpu_ec2_g3  = [1.14 2.28  4.56]; % g3.4xlarge, M60
price_gpu_ec2     = [1.14/4 0.9 3.06/2]; % M60, K80, V100;

% a single CPU % ondemand pricing 
price_cpu_google = 0.0475;
price_cpu_ec2    = 0.0058;
price_cpu16_ec3 = 0.68; % https://aws.amazon.com/ec2/pricing/on-demand/

%https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-optimize-cpu.html
% 1 VCPU = 1 hyperthread of a CPU.

if plots(2) 
   figIdx=figIdx +1;         figures{figIdx} =figure;
   scrsz = get(groot,'ScreenSize');  
   yyaxis left
   plot(1:3, speedUps);
   ylabel(yLabel,'FontSize',fontAxis);
   yyaxis right
   plot(1:3, costs);   
   ylabel(yLabel,'FontSize',fontAxis);
%     legendStr=methods;
    xlim([0.5 3.5]);
%     ylim([0 max(max(avgCmplt))*1.1]);
    xLabels={strUser1, strUser2, strUser3};
%     legend(legendStr,'Location','northoutside','FontSize',fontLegend,'Orientation','horizontal');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    xlabel(xLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel',xLabels,'FontSize',fontAxis);
    fileNames{figIdx} = 'avgCmplt';
end

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