package com.realmax.easydldemo.bean;

import java.util.List;

public class ConfigBean {

    /**
     * product : EasyDL
     * ak : null
     * nType : 103
     * apiUrl : null
     * thresholdRec : 0.4
     * sk : null
     * model_version : 32354
     * model_type : 2
     * soc : ["arm","dsp","npu"]
     * model_name : Target2V2
     */

    private String product;
    private Object ak;
    private int nType;
    private Object apiUrl;
    private double thresholdRec;
    private Object sk;
    private int model_version;
    private int model_type;
    private String model_name;
    private List<String> soc;

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Object getAk() {
        return ak;
    }

    public void setAk(Object ak) {
        this.ak = ak;
    }

    public int getNType() {
        return nType;
    }

    public void setNType(int nType) {
        this.nType = nType;
    }

    public Object getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(Object apiUrl) {
        this.apiUrl = apiUrl;
    }

    public double getThresholdRec() {
        return thresholdRec;
    }

    public void setThresholdRec(double thresholdRec) {
        this.thresholdRec = thresholdRec;
    }

    public Object getSk() {
        return sk;
    }

    public void setSk(Object sk) {
        this.sk = sk;
    }

    public int getModel_version() {
        return model_version;
    }

    public void setModel_version(int model_version) {
        this.model_version = model_version;
    }

    public int getModel_type() {
        return model_type;
    }

    public void setModel_type(int model_type) {
        this.model_type = model_type;
    }

    public String getModel_name() {
        return model_name;
    }

    public void setModel_name(String model_name) {
        this.model_name = model_name;
    }

    public List<String> getSoc() {
        return soc;
    }

    public void setSoc(List<String> soc) {
        this.soc = soc;
    }

    public int getnType() {
        return nType;
    }

    public void setnType(int nType) {
        this.nType = nType;
    }

    @Override
    public String toString() {
        return "ConfigBean{" +
                "product='" + product + '\'' +
                ", ak=" + ak +
                ", nType=" + nType +
                ", apiUrl=" + apiUrl +
                ", thresholdRec=" + thresholdRec +
                ", sk=" + sk +
                ", model_version=" + model_version +
                ", model_type=" + model_type +
                ", model_name='" + model_name + '\'' +
                ", soc=" + soc +
                '}';
    }
}
