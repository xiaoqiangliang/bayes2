package Bayes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import Bayes.ChineseSpliter;

public class IntermediateData implements Serializable
{
	/* 类别名. */
    public String[] classifications;
    
    /* 单词X在类别C下出现的总数. */
	public HashMap[] filesOfXC;
	/** 给定分类下的文件数目. */
    public int[] filesOfC;
    /* 根目录下的文件总数. */
    public int files;
    /* 单词X在类别C下出现的总数 */
	public HashMap[] tokensOfXC;
    /*类别C下所有单词的总数. */
    public int[] tokensOfC;
    /* 整个语料库中单词的总数. */
    public int tokens;
    /* 整个训练语料所出现的单词. */
    public HashSet<String> vocabulary;
    /* 文本分类语料的根目录. */
    private transient String dir;
    /* 语料库中的文本文件的字符编码. */
    private transient String encoding;
    /* 中文分词. */
    private transient ChineseSpliter textSpliter;
    
    public IntermediateData() 
    {
    	vocabulary = new HashSet<String>();
    	textSpliter = new ChineseSpliter();
    }
    
    /*
     * 根据训练文本类别返回这个类别下的所有训练文本路径(full path).
     * 
     * @param dirStr 已分类的文本根目录，末尾不带斜杠
     * @param classification
     *            给定的分类
     * @return 给定分类下所有文件的路径（full path）
     */
    public static String[] getFilesPath(final String dirStr, final String classification) 
    {
        File classDir = new File(dirStr + File.separator + classification);
        String[] ret = classDir.list();
        for (int i = 0; i < ret.length; i++) 
        {
            ret[i] = dirStr + File.separator + classification + File.separator + ret[i];
        }
        return ret;
    }
    
    
    /*
     * 返回给定路径的文本文件内容.
     * 
     * filePath给定的文本文件路径
     * encoding 文本文件的编码
     * return 文本内容
     * throws java.io.IOException文件找不到或IO出错
     */
    public static String getText(final String filePath, final String encoding) throws IOException 
    {

        InputStreamReader isReader = new InputStreamReader(new FileInputStream(filePath), encoding);
        BufferedReader reader = new BufferedReader(isReader);
        String aLine;
        StringBuilder sb = new StringBuilder();

        while ((aLine = reader.readLine()) != null) 
        {
            sb.append(aLine + " ");
        }
        isReader.close();
        reader.close();
        return sb.toString();
    }
    
    
    /*获得单词表. 
     *IOException 读取语料库出错
     */
    private void extractVocabulary() throws IOException 
    {
    	for (String c : classifications) 
    	{
            String[] filesPath = getFilesPath(dir, c);

            for (String file : filesPath) 
            {
                String text = getText(file, encoding);

                String[] terms = null;
                // 中文分词处理(分词后结果可能还包含有停用词）
                terms = textSpliter.split(text, " ").split(" ");
                terms = ChineseSpliter.dropStopWords(terms); // 去掉停用词，以免影响分类

                for (String term : terms) 
                { //去除重复单词
                    vocabulary.add(term);
                }
            }
    	}
    }
    
    /*
     * 返回训练文本集中在给定分类下的训练文本数目.
     * 
     * @param c
     *            给定的分类
     * @return 训练文本集中在给定分类下的训练文本数目
     */
    private int calcFileCountOfClassification(final int c) 
    {
        File classDir = new File(dir + File.separator
                + classifications[c]);
        return classDir.list().length;
    }
    
    
    /*
     * 计算 fileCountOfXC, fileCountOfC, fileCount, tokensOfXC, tokensOfC, tokens.
     */
    private void calculate() throws IOException {
        for (int i = 0;i < classifications.length; i++) {
        	HashMap<String, Integer> tmpT = (HashMap<String, Integer>)tokensOfXC[i];
        	HashMap<String, Integer> tmpF = (HashMap<String, Integer>)filesOfXC[i];

            String[] filesPath = getFilesPath(dir, classifications[i]);
            
            filesOfC[i] = filesPath.length;
            files += filesOfC[i];

            HashSet<String> words = new HashSet<String>();
            for (String file : filesPath) {
            	words.clear();
                String text = getText(file, encoding);
                String[] terms = null;
                
                // 中文分词处理(分词后结果可能还包含有停用词）
                terms = textSpliter.split(text, " ").split(" ");
                terms = ChineseSpliter.dropStopWords(terms); // 去掉停用词，以免影响分类

                for (String term : terms) { // 计算本类别下每个单词的出现次数
                	Integer value = tmpT.get(term);
                    if(value == null) {
                    	tmpT.put(term, new Integer(1));
                    } else {
                    	tmpT.put(term, value + 1);
                    }
                }
                
                // 开始计算 filesOfXC[i]
                for (String term : terms) { //去除重复单词
                    words.add(term);
                }
				for (Iterator<String> iter = words.iterator(); iter.hasNext();) {
					String key = iter.next();
					Integer value = tmpF.get(key);
					if (value == null) {
						tmpF.put(key, new Integer(1));
					} else {
						value++;
						tmpF.put(key, value);
					}
				}
            }

            // 该类别下所有单词的出现总数 nC
            for (Iterator<Entry<String, Integer>> iter = tmpT.entrySet()
                    .iterator(); iter.hasNext();) {
                Entry<String, Integer> entry = iter.next();

                tokensOfC[i] += entry.getValue().intValue();
            }
            
            tokens += tokensOfC[i]; // 所有单词出现总数
        }
    }
    
    
    
    
    public final void generate(final String trainTextDir,final String txtEncoding, final String modelFile) 
    {
        // 初始化
        dir = trainTextDir;
        if (txtEncoding == null) 
        {
            encoding = "GBK"; // 默认文本文件的编码为GBK;
        } 
        else 
        {
            encoding = txtEncoding;
        }

        // 枚举目录，获得各个类名
        File tmpDir = new File(dir);
        if (!tmpDir.isDirectory()) 
        {
            throw new IllegalArgumentException("训练语料库搜索失败！ [" + dir + "]");
        }
        classifications = tmpDir.list();
        
        filesOfC = new int[classifications.length];
        filesOfXC = new HashMap[classifications.length];
        tokensOfC = new int[classifications.length];
        tokensOfXC = new HashMap[classifications.length];
        for(int i = 0; i < classifications.length; i++) {
        	tokensOfXC[i] = new HashMap<String, Integer>();
        	filesOfXC[i] = new HashMap<String, Integer>();
        }
        
        // 计算各个类别的文件总数，保存
        for (int i = 0; i < classifications.length; i++) 
        {
            int n = calcFileCountOfClassification(i);
            filesOfC[i] = n;
            files += n; // 计算文件总数，保存
        }
        
        // 获得单词表
        try 
        {
			extractVocabulary();
		} 
        catch (IOException e1) 
        {
			e1.printStackTrace();
		}

		// 计算各类别下单词总数，单词总数
        try 
        {
			calculate();
		} 
        catch (IOException e1) 
        {
			e1.printStackTrace();
		}
		
		// 将预处理后的数据写入到磁盘
		try 
		{
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(modelFile));
            out.writeObject(this);
            out.close();
        } 
		catch (IOException e) 
		{
            e.printStackTrace();
        }
    }
    
    public static void main(String args[]) 
    {
       	long startTime = System.currentTimeMillis();
       	if(args.length < 3) 
       	{
       		System.err.println("参数格式： <语料库目录地址> <语料库文本编码(如:gbk)> <中间文件（.db）>");
       		return;
       	}
       	IntermediateData tdm = new IntermediateData();
       	tdm.generate(args[0], args[1], args[2]);
       	long endTime = System.currentTimeMillis();
       	System.out.println("中间数据生成！");
       	System.out.println("运行时间： "+(endTime-startTime)+"ms");

    }

    

}
