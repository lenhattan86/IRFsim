 
% job file description:
% row1: cpu time for all jobs;
% row2: gpu time for all jobs;
% cpu_machine
function [assignment,cost] = testcorrectness(jobfile,cpu_machine,gpu_machine)
    large_matrix = [];
    [~,jobs] = size(jobfile);
    for i = 1:gpu_machine
        large_matrix = cat(1, large_matrix, jobfile(2,:));
    end
    for i = 1:cpu_machine
        large_matrix = cat(1, large_matrix, jobfile(1,:));
    end

    cost_matrix = [];
    for i = 1:jobs
        cost_matrix = cat(1,cost_matrix,i*large_matrix);
    end
    [assignment,cost] = munkres(cost_matrix);
% ass = reshape(assignment,12,[])'
end