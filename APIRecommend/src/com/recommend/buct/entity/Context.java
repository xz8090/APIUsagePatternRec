package com.recommend.buct.entity;

import java.util.HashMap;
import java.util.Map;

public class Context {
	private long id;
	private String name;
	private String url;
	private Map<Long,Method> methods;
	private Map<Long,API> APIs;
	private long activeMethod;//光标所处位置对应的方法
	private Long predictAPI;//光标处预测的API编号
	private Map<Long,Double> features;//key表示API编号，value表示该API的TFIDF特征
	private Map<Long,Integer> APICount;//API数量,key为API编号，value为数量
	private long time;

	public Context() {
		super();
		this.time = System.currentTimeMillis();
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Map<Long, Method> getMethods() {
		return methods;
	}
	public Method getMethodByName(String name) {
		Method target = new Method();
		for(Long key:methods.keySet()) {
			Method m = methods.get(key);
			if(m.getQualifiedName().equals(name)) {
				target = m;
				break;
			}
		}
		return target;
	}
	public void addMethod(Method newMethod) {
		if(methods==null) methods = new HashMap<>();
		methods.put(newMethod.getId(), newMethod);
	}
	public Map<Long, API> getAPIs() {
		return APIs;
	}
	public void addAPI(API newAPI) {
		if(APIs==null) {
			APIs = new HashMap<>();
			APICount = new HashMap<>();
		}
		if (APIs.containsKey(newAPI.getId())) {
			APICount.put(newAPI.getId(), APICount.get(newAPI.getId())+1);
		}else {
			APICount.put(newAPI.getId(),1);
			APIs.put(newAPI.getId(), newAPI);
		}
	}
	public long getActiveMethod() {
		return activeMethod;
	}
	public void setActiveMethod(long activeMethod) {
		this.activeMethod = activeMethod;
	}
	public Map<Long,Double> getFeatures() {
		return features;
	}
	public void setFeature(Long iid, double tfidf) {
		if(features==null) features = new HashMap<>();
		features.put(iid, tfidf);
	}
	public Map<Long, Integer> getAPICount() {
		return APICount;
	}
	public void setAPICount(Map<Long, Integer> aPICount) {
		APICount = aPICount;
	}
	public long getTime() {
		return time;
	}
	public Long getPredictAPI() {
		return predictAPI;
	}

	public void setPredictAPI(Long predictAPIId) {
		this.predictAPI = predictAPIId;
	}

	public Context(long id, String name, String url) {
		super();
		this.id = id;
		this.name = name;
		this.url = url;
		this.time = System.currentTimeMillis();
	}

}
