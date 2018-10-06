NumStacksPerGroup = 3; 
NumGroupsPerAxis = 6; 
NumStackElements = 4;

% labels to use on tick marks for groups 
groupLabels = { 'Test'; 2; 4; 6; 8; -1; }; 
stackData = rand(NumGroupsPerAxis,NumStacksPerGroup,NumStackElements);

plotBarStackGroups(stackData, groupLabels); 
set(gca,'FontSize',18) 
set(gcf,'Position',[100 100 720 650]) 
grid on 
set(gca,'Layer','top') % put grid lines on top of stacks