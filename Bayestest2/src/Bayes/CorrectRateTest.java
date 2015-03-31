package Bayes;

//import Bayes.Classifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CorrectRateTest 
{
	protected TrainnedModel model;
	private transient ChineseSpliter textSpliter;
	
	public final double getCorrectRate(String classifiedDir, String encoding, String model) 
	{
        int total = 0;
        int correct = 0;
        
        loadModel(model);
        
        File dir = new File(classifiedDir);
        
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("训练语料库搜索失败！ [" + classifiedDir
                    + "]");
        }

        String[] classifications = dir.list();
        for (String c : classifications)
     {
            String[] filesPath = IntermediateData.getFilesPath(classifiedDir, c);
            for (String file : filesPath) 
            {
                total++;
                String text = null;
                try {
                    text = IntermediateData.getText(file, encoding);
                	} catch (IOException e)
                		{
                			// TODO Auto-generated catch block
                			e.printStackTrace();
                		}
                	String classification = classify(text);
                	if(classification.equals(c)) { // 计算出的类别，和原始的类别是否相同
                    correct++;
                	}
            	}
        	}
        
        return ((double)correct/(double)total)*100;
    }

	public void loadModel(String modelFile) 
	{
        try {
            	ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
            	model = (TrainnedModel) in.readObject();
        	} 
        catch (Exception e) 
        	{
            	e.printStackTrace();
        	}
    }
	
	
	@SuppressWarnings("unchecked")
	public final String classify(final String text) 
	{
        String[] terms = null;
        // 中文分词处理
        terms = textSpliter.split(text, " ").split(" ");
        terms = ChineseSpliter.dropStopWords(terms); // 去掉停用词
        double probility = 0.0;
        List<ClassifyResult> resultList = new ArrayList<ClassifyResult>(); // 分类结果
        for (int i = 0; i < model.classifications.length; i++) 
        {
            // 计算给定的文本属性向量terms在给定的分类Ci中的分类条件概率
            probility = calcProd(terms, i);
            // 保存分类结果
            ClassifyResult result = new ClassifyResult();
            result.classification = model.classifications[i]; // 分类
            result.probility = probility; // 关键字在分类的条件概率
            //System.out.println("In process....");
            //System.out.println(model.classifications[i] + "：" + probility);
            resultList.add(result);
        }
        ClassifyResult maxElem = (ClassifyResult) java.util.Collections.max
        		(resultList, new Comparator() 
        			{
                    	public int compare(final Object o1, final Object o2)
                    	{
                    		final ClassifyResult m1 = (ClassifyResult) o1;
                    		final ClassifyResult m2 = (ClassifyResult) o2;
                    		final double ret = m1.probility - m2.probility;
                    		if (ret < 0) 
                    		{
                    			return -1;
                    		} 
                    		else 
                    		{
                    			return 1;
                    		}
                    	}
        			}
        		);

        return maxElem.classification;  
	}
	
	
	
    protected double calcProd(final String[] x, final int cj) 
    {
        double ret = 0.0;
        // 类条件概率连乘
        for (int i = 0; i < x.length; i++) {
            // 因为结果过小，因此在连乘之前放大10倍
            ret += Math.log(model.getPxc(x[i], cj));
        }
        // 再乘以先验概率
        ret += Math.log(model.getPc(cj));
        return ret;
    }
    
    public static void test(CorrectRateTest CorrectRateTest, String[] args)
    {
    double ret = CorrectRateTest.getCorrectRate(args[0], args[1], args[2]);
    System.out.println("正确率为：" + ret+"%");
    }
    
    public static void main(String[] args)
    {
    	CorrectRateTest crt = new CorrectRateTest();
    	CorrectRateTest.test(crt,args);
    }
}
