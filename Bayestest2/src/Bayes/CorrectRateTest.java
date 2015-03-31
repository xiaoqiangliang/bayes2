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
            throw new IllegalArgumentException("ѵ�����Ͽ�����ʧ�ܣ� [" + classifiedDir
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
                	if(classification.equals(c)) { // ���������𣬺�ԭʼ������Ƿ���ͬ
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
        // ���ķִʴ���
        terms = textSpliter.split(text, " ").split(" ");
        terms = ChineseSpliter.dropStopWords(terms); // ȥ��ͣ�ô�
        double probility = 0.0;
        List<ClassifyResult> resultList = new ArrayList<ClassifyResult>(); // ������
        for (int i = 0; i < model.classifications.length; i++) 
        {
            // ����������ı���������terms�ڸ����ķ���Ci�еķ�����������
            probility = calcProd(terms, i);
            // ���������
            ClassifyResult result = new ClassifyResult();
            result.classification = model.classifications[i]; // ����
            result.probility = probility; // �ؼ����ڷ������������
            //System.out.println("In process....");
            //System.out.println(model.classifications[i] + "��" + probility);
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
        // ��������������
        for (int i = 0; i < x.length; i++) {
            // ��Ϊ�����С�����������֮ǰ�Ŵ�10��
            ret += Math.log(model.getPxc(x[i], cj));
        }
        // �ٳ����������
        ret += Math.log(model.getPc(cj));
        return ret;
    }
    
    public static void test(CorrectRateTest CorrectRateTest, String[] args)
    {
    double ret = CorrectRateTest.getCorrectRate(args[0], args[1], args[2]);
    System.out.println("��ȷ��Ϊ��" + ret+"%");
    }
    
    public static void main(String[] args)
    {
    	CorrectRateTest crt = new CorrectRateTest();
    	CorrectRateTest.test(crt,args);
    }
}
