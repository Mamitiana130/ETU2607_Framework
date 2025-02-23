package models;

import java.util.HashMap;

public class ModelView{
    String url;
    HashMap<String , Object> data;

    public ModelView(){
        this.data = new HashMap<>();
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }
    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }
    public HashMap<String, Object> getData() {
        return data;
    }

    public void add(String nameUrl , Object value){
        this.data.put(nameUrl, value);
    }
}