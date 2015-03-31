package Bayes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

public class TrainnedModel implements Serializable 
{
	 
		/* �����. */
		public String[] classifications;
		/* ���Ͽ��������ֹ��ĵ���. */
		HashSet<String> vocabulary;	
		/* �����������. */
		private double[] pC;
	    /* ���Ե����������ʣ�String �ĸ�ʽΪ ������#�����. */
		private HashMap[] pXC;
	    
	    
	    public TrainnedModel(int n) 
	    {
	    	pC = new double[n];
	    	pXC = new HashMap[n];
	    	for(int i = 0; i < n; i++)
	    	{
	        	pXC[i] = new HashMap<String, Double>();
	        }
	    }
	    
	    /*
	     * ������� 
	     * c �����ķ���
	     * return ���������µ��������
	     */
	    public final double getPc(final int c) {
	        return pC[c];
	    }
	    
	    /*
	     * �����������.
	     */
	    public final void setPc(final int c, double p) {
	        pC[c] = p;
	    }
	    
	    
	    /*
	     * �������������
	     * x�������ı�����
	     * c�����ķ���
	     * return �����Ե�����������
	     */
	    public final double getPxc(final String x, final int c) {
	    	Double ret = 1.0;
	    	HashMap<String, Double> p = pXC[c];
	        Double f = p.get(x);
	        
	        if (f != null) {
	        	ret = f.doubleValue();
	        }

	        return ret;
	    }
	    
	    
	    /*
	     * ��������������.
	     *x �������ı�����
	     *c �����ķ���
	     *return �����Ե�����������
	     */
	    public final void setPxc(final String x, final int c, double p) {
	    	pXC[c].put(x, p);
	    }
	

	
}
