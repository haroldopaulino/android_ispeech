package com.speech;

public class SpinnerItemData {

    String text;
    Integer imageId;
    public SpinnerItemData(String text, Integer imageId){
        this.text=text;
        this.imageId=imageId;
    }

    public String getText(){
        return text;
    }

    public Integer getImageId(){
        return imageId;
    }
}
