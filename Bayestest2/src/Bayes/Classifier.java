package Bayes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import Bayes.IntermediateData;
//Bayes.Classifier;

public class Classifier 
{
	protected TrainnedModel model;
	protected transient IntermediateData db;
	private transient ChineseSpliter textSpliter;
	
	public Classifier() 
	{
    	textSpliter = new ChineseSpliter();
    }
	
	/*加载数据模型文件*/
	public final void loadModel(final String modelFile) 
	{
		long startTime1 = System.currentTimeMillis();
        try {
            	ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
            	model = (TrainnedModel) in.readObject();
        	} 
        catch (Exception e) 
        	{
            	e.printStackTrace();
        	}
        long endTime1 = System.currentTimeMillis(); // 获取结束时间
        System.out.println("加载模型文件时间： " + (endTime1 - startTime1) + "ms");
    }
	@SuppressWarnings("unchecked")	
	/*对给定的文本分类*/
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
            System.out.println("In process....");
            System.out.println(model.classifications[i] + "：" + probility);
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
	
	public final void train(String intermediateData, String modelFile) {
    	// 加载中间数据文件
    	loadData(intermediateData);
    	
    	model = new TrainnedModel(db.classifications.length);
    	
    	model.classifications = db.classifications;
    	model.vocabulary = db.vocabulary;
    	// 开始训练
    	calculatePc();
    	calculatePxc();
    	db = null;
    	
    	try {
    		// 用序列化，将训练得到的结果存放到模型文件中
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(modelFile));
            out.writeObject(model);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	private final void loadData(String intermediateData) {
		try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(
            		intermediateData));
            db = (IntermediateData) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	/* 计算先验概率P(c) */
	protected void calculatePc() 
	{
		for (int i = 0; i < db.classifications.length; i++) 
		{
            model.setPc(i, (double)db.tokensOfC[i] / (double)db.tokens);
        }
    }
	
	/* 计算类条件概率P(x|c) */
    protected void calculatePxc() 
    {
    	for (int i = 0; i < db.classifications.length; i++) 
    	{
			HashMap<String, Integer> source =  db.tokensOfXC[i];
			//HashMap<String, Double> target = model.pXC[i];
			
			for(Iterator<String> iter = db.vocabulary.iterator(); iter.hasNext();) 
			{
				String t = iter.next();
				
				Integer value = source.get(t);
				if(value == null) 
				{ // 本类别下不包含单词t
					value = 0;
				}
				model.setPxc(t, i, (double)(value + 1)/(double)(db.tokensOfC[i] + db.vocabulary.size()));
			}
		}
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
	
    private static void usage() {
    	// 根据中间数据文件，训练产生模型文件
        System.err.println("usage:\t -t <中间文件> <模型文件>");
        // 对已经分类好的文本库，用某个模型文件来分类，测试正确率
        System.err.println("usage:\t -r <语料库目录> <语料库文本编码> <模型文件>");
        // 用某一个训练好的模型文件，来分类某个文本文件
        System.err.println("usage:\t <模型文件> <文本文件> <文本文件编码>");
    }
    
    
    public static void test(Classifier classifier, String[] args) {
    	long startTime = System.currentTimeMillis(); // 获取开始时间
        if (args.length < 3) {
            usage();
            return;
        }

        if (args[0].equals("-t")) { // 训练
            classifier.train(args[1], args[2]);
            System.out.println("训练完毕");
        } else if(args[0].equals("-r")) { // 获取正确率
            double ret = classifier.getCorrectRate(args[1], args[2], args[3]);
            System.out.println("正确率为：" + ret+"%");
        } else { // 分类
            classifier.loadModel(args[0]);

            String text = null;
            try {
                text = IntermediateData.getText(args[1], args[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            
            String result = classifier.classify(text); // 进行分类

            System.out.println("此属于[" + result + "]");
        }

        long endTime = System.currentTimeMillis(); // 获取结束时间
        System.out.println("程序运行时间： " + (endTime - startTime) + "ms");
    }

    
    
    public static void main(String[] args)
     
    {
    	Classifier test11 = new Classifier();
    	Classifier.test(test11,args);
    }
	
    /*
    public static void main(String[] args)
    {
    	Classifier test12 = new Classifier();
    	Classifier.test2(test12,args);
    	
    }
    */

}
