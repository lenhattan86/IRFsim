function [ complTimes] = queue_compl_time( folder, fileName, QUEUE_NAME)

%     global batchJobRange

  filePath = [folder fileName];  
  if exist(filePath, 'file')
     [JobId,startTime,endTime,duration,queueName] = import_compl_time(filePath);     
    %       queueName = queueName(1:length(QUEUE_NAME));
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
     complTimes = duration(idxs);  
  else
      error('File does not exists');
  end

