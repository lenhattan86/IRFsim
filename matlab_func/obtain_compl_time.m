function [ avg_compl_time complTimes] = obtain_compl_time( folder, files, QUEUE_NAME)

%     global batchJobRange

   num_files = length(files);
   avg_compl_time = zeros(1,num_files);
   complTimes=0;
   for i=1:num_files
      filePath = [folder files{i}];
      avg_compl_time(i) = 0;
      if exist(filePath, 'file')
         [JobId,startTime,endTime,duration,queueName] = import_compl_time(filePath);
         complTimes = [complTimes duration'];
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
         avg_compl_time(i) = mean(duration(idxs));         
      end
   end    
end

