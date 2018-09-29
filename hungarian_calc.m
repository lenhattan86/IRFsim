% large matrix generation:
cpu_processing = zer 

rawHungaian = rawHungaian;
[m,n]= size(rawHungaian);
cost = rawHungaian;
for i = 2:n
    cost = cat(1,cost,i*rawHungaian);
end
[assignment,costsol] = munkres(cost);
ass = reshape(assignment,12,[])'