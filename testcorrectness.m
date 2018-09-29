 jobfile = jobfile';
large_matrix = [];
for i = 1:4
    large_matrix = cat(1, large_matrix, jobfile(2,:));
end
for i = 1:8
    large_matrix = cat(1, large_matrix, jobfile(1,:));
end

cost_matrix = [];
for i = 1:40
    cost_matrix = cat(1,cost_matrix,i*large_matrix);
end
[assignment,cost] = munkres(cost_matrix);
ass = reshape(assignment,12,[])'