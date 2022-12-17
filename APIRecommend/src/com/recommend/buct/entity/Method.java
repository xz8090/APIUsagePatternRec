package com.recommend.buct.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Method")
@XmlType(propOrder = {
		"id",
		"name",
		"qualifiedName",
		"codeSnippetPath",
		"APIs",
		"vec",
		"APICount"
})
public class Method implements Serializable {
	private static final long serialVersionUID = 2L;
	private long id;
	private String name;
	private String qualifiedName;
	private String codeSnippetPath;
	private Map<Long,API> APIs;
	private float[] vec;
	private Map<Long,Integer> APICount;//API数量,key为API编号，value为数量

	public Method() {
		super();
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
	public String getQualifiedName() {
		return qualifiedName;
	}
	public void setQualifiedName(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}
	public String getCodeSnippets() {
		return codeSnippetPath;
	}
	public void setCodeSnippets(String codeSnippetPath) {
		this.codeSnippetPath = codeSnippetPath;
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
	public float[] getVec() {
		return vec;
	}
	public void setVec(float[] vec) {
		this.vec = vec;
	}
	public Map<Long, Integer> getAPICount() {
		return APICount;
	}
	public void setAPICount(Map<Long, Integer> aPICount) {
		APICount = aPICount;
	}

	public Method(long id, String name, String qualifiedName, String codeSnippetPath) {
		super();
		this.id = id;
		this.name = name;
		this.qualifiedName = qualifiedName;
		this.codeSnippetPath = codeSnippetPath;
	}

}
