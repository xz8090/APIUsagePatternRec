package com.recommend.buct.entity;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "API")
@XmlType(propOrder = {
   "id",
   "name",
   "qualifiedName",
   "attributes",
   "vec"
})
public class API implements Serializable {
	private static final long serialVersionUID = 3L;
	private long id;
	private String name;
	private String qualifiedName;
	private String attributes;
	private float[] vec;

	public API() {
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
	public String getAttributes() {
		return attributes;
	}
	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}
	public float[] getVec() {
		return vec;
	}
	public void setVec(float[] vec) {
		this.vec = vec;
	}

	public API(long id, String name, String qualifiedName) {
		super();
		this.id = id;
		this.name = name;
		this.qualifiedName = qualifiedName;
	}

}
