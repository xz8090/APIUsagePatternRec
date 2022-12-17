package com.recommend.buct.utils;

import java.util.*;
import java.util.Map.Entry;

import com.recommend.buct.entity.Context;
import com.recommend.buct.entity.Method;

public class ComputeUtil {

	/**
	 * 计算TF-IDF特征
	 * @param tf	表示tf特征
	 * @param total	表示idf特征中语料库数量
	 * @param freq	表示idf特征中的频率
	 * @return
	 */
	public static double computeTFIDF(double tf, int total, int freq) {
		if(freq==0) return 0;
		return (tf * Math.log(total*1.0 / freq));
	}

	/**
	 * 计算两个项目tf-idf特征集合的相似性
	 * @param features1
	 * @param features2
	 * @return
	 */
	public static double computeProjectSimilarity(Map<Long,Double> features1,Map<Long,Double> features2) {
		double result = 0;
		Set<Long> sets = new HashSet<>();	//保存两个项目的交集，即共同使用的API编号
		double sum1 = 0, sum2 = 0, sum3 = 0;	//保存两个特征向量的平方和
		for(Long iid:features1.keySet()) {
			if(features2.keySet().contains(iid)) sets.add(iid);
			sum1 += Math.pow(features1.get(iid), 2);
		}
		for(Long iid:features2.keySet()) {
			sum2 += Math.pow(features2.get(iid), 2);
		}
		for(Long iid:sets) {
			sum3 += features1.get(iid)*features2.get(iid);
		}
		if(sum1!=0&&sum2!=0) result = sum3/((Math.sqrt(sum1))*(Math.sqrt(sum2)));
		return result;
	}

	/**
	 * 计算两个方法的相似性
	 * @param vec1	表示方法1的特征向量
	 * @param vec2	表示方法2的特征向量
	 * @return
	 */
	public static double computeMethodSimilarity(float[] vec1, float[] vec2) {
		//使用jaccard相似性
		double result = 0;
		int sum1 = 0, sum2 = 0, sum3 = 0;
		for(int i=0;i<vec1.length;i++) {
			if(vec1[i]!=0) sum1++;
			if(vec2[i]!=0) sum2++;
			if(vec1[i]!=0&&vec2[i]!=0) sum3++;
		}
		result = sum3*1.0/(sum1+sum2-sum3);
		return result;
	}

	/**
	 * 计算两个方法的相似性
	 * @param m1	表示方法1
	 * @param m2	表示方法2
	 * @return	相似性值
	 */
	public static double computeMethodSimilarity(Method m1, Method m2) {
		//使用jaccard相似性
		double result = 0;
		int sum = 0;//交集数量
		for(Long iid:m1.getAPICount().keySet()) {
			if(m2.getAPICount().keySet().contains(iid)) sum++;
		}
		result = sum*1.0/(m1.getAPICount().size()+m2.getAPICount().size()-sum);
		return result;
	}

	public static double computeMethodSimilarity(Context c, Method m2) {
		Set<Long> m1 = new HashSet<Long>();
		long activityMid = c.getActiveMethod();
		//获取活动方法中API
		for(Long iid:c.getMethods().get(activityMid).getAPIs().keySet()) {
			m1.add(iid);
		}
		m1.add(c.getPredictAPI());//选择该预测的API节点编号
		//使用jaccard相似性
		double result = 0;
		int sum = 0;//交集数量
		for(Long iid:m1) {
			if(m2.getAPICount().keySet().contains(iid)) sum++;
		}
		result = sum*1.0/(m1.size()+m2.getAPICount().size()-sum);
		return result;
	}

	/**
	 * 根据Map的value降序排序
	 * @param oriMap	表示原始的Map对象，key表示编号，value表示相似性结果
	 * @return
	 */
	public static Map<Long, Double> sortMapByValue(Map<Long, Double> oriMap) {
		if (oriMap == null || oriMap.isEmpty()) {
			return null;
		}
		Map<Long, Double> sortedMap = new LinkedHashMap<Long, Double>();
		List<Map.Entry<Long, Double>> entryList = new ArrayList<Map.Entry<Long, Double>>(oriMap.entrySet());
		Collections.sort(entryList, new MapValueComparator());
		//重新复制到新Map对象中
		Iterator<Map.Entry<Long, Double>> iter = entryList.iterator();
		Map.Entry<Long, Double> tmpEntry = null;
		while (iter.hasNext()) {
			tmpEntry = iter.next();
			sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
		}
		return sortedMap;
	}

	public static Map<String, Double> sortStringMapByValue(Map<String, Double> oriMap) {
		if (oriMap == null || oriMap.isEmpty()) {
			return null;
		}
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		List<Map.Entry<String, Double>> entryList = new ArrayList<Map.Entry<String, Double>>(oriMap.entrySet());
		Collections.sort(entryList, new MapValueComparator2());
		//重新复制到新Map对象中
		Iterator<Map.Entry<String, Double>> iter = entryList.iterator();
		Map.Entry<String, Double> tmpEntry = null;
		while (iter.hasNext()) {
			tmpEntry = iter.next();
			sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
		}
		return sortedMap;
	}

	/**
	 * 归一化API关联性并排序
	 * @param APICorrelationFrequent
	 * @return
	 */
	public static Map<Long,Double> normalizationAPICorFrequent(Map<Long,Integer> APICorrelationFrequent){
		Map<Long,Double> normalizationAPICorFrequent = new HashMap<>();
		int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
		for(Long iid:APICorrelationFrequent.keySet()){
			int n = APICorrelationFrequent.get(iid);
			if(n>max) max = n;
			if(n<min) min = n;
		}
		for(Long iid:APICorrelationFrequent.keySet()){
			double d = (APICorrelationFrequent.get(iid)-min)*1.0/(max-min);
			normalizationAPICorFrequent.put(iid,d);
		}
		return normalizationAPICorFrequent;
	}
}

/**
 * 自定义Map比较器，根据map的value值排序
 *
 */
class MapValueComparator implements Comparator<Map.Entry<Long, Double>> {
	@Override
	public int compare(Entry<Long, Double> me1, Entry<Long, Double> me2) {
		return me2.getValue().compareTo(me1.getValue());
	}
}

class MapValueComparator2 implements Comparator<Map.Entry<String, Double>> {
	@Override
	public int compare(Entry<String, Double> me1, Entry<String, Double> me2) {
		return me2.getValue().compareTo(me1.getValue());
	}
}
