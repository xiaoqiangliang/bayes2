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
	
	/*��������ģ���ļ�*/
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
        long endTime1 = System.currentTimeMillis(); // ��ȡ����ʱ��
        System.out.println("����ģ���ļ�ʱ�䣺 " + (endTime1 - startTime1) + "ms");
    }
	@SuppressWarnings("unchecked")	
	/*�Ը������ı�����*/
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
            System.out.println("In process....");
            System.out.println(model.classifications[i] + "��" + probility);
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
    	// �����м������ļ�
    	loadData(intermediateData);
    	
    	model = new TrainnedModel(db.classifications.length);
    	
    	model.classifications = db.classifications;
    	model.vocabulary = db.vocabulary;
    	// ��ʼѵ��
    	calculatePc();
    	calculatePxc();
    	db = null;
    	
    	try {
    		// �����л�����ѵ���õ��Ľ����ŵ�ģ���ļ���
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
	/* �����������P(c) */
	protected void calculatePc() 
	{
		for (int i = 0; i < db.classifications.length; i++) 
		{
            model.setPc(i, (double)db.tokensOfC[i] / (double)db.tokens);
        }
    }
	
	/* ��������������P(x|c) */
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
				{ // ������²���������t
					value = 0;
				}
				model.setPxc(t, i, (double)(value + 1)/(double)(db.tokensOfC[i] + db.vocabulary.size()));
			}
		}
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
	
    private static void usage() {
    	// �����м������ļ���ѵ������ģ���ļ�
        System.err.println("usage:\t -t <�м��ļ�> <ģ���ļ�>");
        // ���Ѿ�����õ��ı��⣬��ĳ��ģ���ļ������࣬������ȷ��
        System.err.println("usage:\t -r <���Ͽ�Ŀ¼> <���Ͽ��ı�����> <ģ���ļ�>");
        // ��ĳһ��ѵ���õ�ģ���ļ���������ĳ���ı��ļ�
        System.err.println("usage:\t <ģ���ļ�> <�ı��ļ�> <�ı��ļ�����>");
    }
    
    
    public static void test(Classifier classifier, String[] args) {
    	long startTime = System.currentTimeMillis(); // ��ȡ��ʼʱ��
        if (args.length < 3) {
            usage();
            return;
        }

        if (args[0].equals("-t")) { // ѵ��
            classifier.train(args[1], args[2]);
            System.out.println("ѵ�����");
        } else if(args[0].equals("-r")) { // ��ȡ��ȷ��
            double ret = classifier.getCorrectRate(args[1], args[2], args[3]);
            System.out.println("��ȷ��Ϊ��" + ret+"%");
        } else { // ����
            classifier.loadModel(args[0]);

            String text = null;
            try {
                text = IntermediateData.getText(args[1], args[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            
            String result = classifier.classify(text); // ���з���

            System.out.println("������[" + result + "]");
        }

        long endTime = System.currentTimeMillis(); // ��ȡ����ʱ��
        System.out.println("��������ʱ�䣺 " + (endTime - startTime) + "ms");
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
