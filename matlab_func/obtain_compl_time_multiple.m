function [ avg_compl_times]  = obtain_compl_time_multiple( folder, files, QUEUE_NAMES)

%     global batchJobRange

   num_files = length(files);
   numQueues = length(QUEUE_NAMES);
   avg_compl_times = zeros(numQueues,num_files);
   for i=1:num_files
      filePath = [folder files{i}];
      
      if exist(filePath, 'file')
         [JobId,startTime,endTime,duration,queueName] = import_compl_time(filePath);
   %       queueName = queueName(1:length(QUEUE_NAME));
         for q=1:numQueues
           avg_compl_times(q,i) = 0;
           QUEUE_NAME = QUEUE_NAMES{q};
           idxs = false(1,length(duration));
           for j=1:length(queueName)
              strTemp =  queueName{j};
              if length(QUEUE_NAME) <= length(strTemp)
                 strTemp = strTemp(1:length(QUEUE_NAME)); 
                 if strcmp(QUEUE_NAME, strTemp)
                    idxs(j) = true;
                 end
              end
           end
           avg_compl_times(q,i) = mean(duration(idxs));
         end                  
      end
   end
end

