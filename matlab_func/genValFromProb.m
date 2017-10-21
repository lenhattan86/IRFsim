function [values XF DF] = genValFromProb(xf, probabilities, numOfSamples, valMean, minVal, maxVal, valStd)
    pdf = probabilities;
    % normalize the function to have an area of 1.0 under it
    pdf = pdf / sum(pdf);        
    % the integral of PDF is the cumulative distribution function
    cdf = cumsum(pdf);
    % these two variables holds and describes the CDF    %xq:x %cdf: P(x)
    % remove non-unique elements
    [cdf, mask] = unique(cdf);
    xf = xf(mask);
    uniValues = rand(1, numOfSamples);
    % inverse interpolation to achieve P(x) -> x projection of the random values
    normValues = interp1(cdf, xf, uniValues, 'linear','extrap');
    normStd = std(normValues);    
    valScale = valStd/normStd;
    
%     valueRange = (normValues-mean(normValues))*valScale  + valMean;    
    valueRange = xf*valScale  + valMean;    
    
    truncatedList = valueRange>=minVal & valueRange<=maxVal;
    truncatedXf = xf(truncatedList);
    truncatedCdf = cdf(truncatedList);    
    truncatedPdf = pdf(truncatedList);
    truncatedUniValues = (max(truncatedCdf)-min(truncatedCdf))* rand(1, numOfSamples) + min(truncatedCdf);
    truncatedValues = interp1(truncatedCdf, truncatedXf, truncatedUniValues, 'linear','extrap');
    
    XF = truncatedXf; DF = truncatedPdf;
    
    values = truncatedValues*valScale + valMean;    
    std(values)
end