package org.dubbo.spring.boot.tigerz.aus.entity;

/**
 * This code is generated by Machine
 * @author wangjignci@126.com
 *
 */
public class NewsList
{
    /********** attribute ***********/
    private java.lang.String _id;
     
    private java.lang.Integer realestateNewsId;
     
    private java.lang.String headline;
     
    private java.lang.String title;
     
    private java.lang.String image;
     
    private java.lang.String url;
     
    private java.lang.String desc;
     
    private java.lang.String source;
     
    private java.lang.String dateStr;
     
    private java.util.Date date;
     
    private java.util.ArrayList description;
     
    private java.util.ArrayList descSource;
     
    private java.lang.String authorName;
     
    private java.lang.Object authorImage;
     
    private java.lang.String authorUrl;
     
    private java.lang.String state;
     
    private java.lang.String stateAll;
     
    private java.lang.String suburb;
    
    private Integer userView;
    
    private Integer realView;
    
    private Integer share;
    
    private Integer realShare;
    
     
    /********** constructors ***********/
    public NewsList() {
     
    }
 
    public NewsList(java.lang.String _id, java.lang.Integer realestateNewsId, java.lang.String headline, java.lang.String title, java.lang.String image, java.lang.String url, java.lang.String desc, java.lang.String source, java.lang.String dateStr, java.util.Date date, java.util.ArrayList description, java.util.ArrayList descSource, java.lang.String authorName, java.lang.Object authorImage, java.lang.String authorUrl, java.lang.String state, java.lang.String stateAll, java.lang.String suburb) {
        this._id = _id;
        this.realestateNewsId = realestateNewsId;
        this.headline = headline;
        this.title = title;
        this.image = image;
        this.url = url;
        this.desc = desc;
        this.source = source;
        this.dateStr = dateStr;
        this.date = date;
        this.description = description;
        this.descSource = descSource;
        this.authorName = authorName;
        this.authorImage = authorImage;
        this.authorUrl = authorUrl;
        this.state = state;
        this.stateAll = stateAll;
        this.suburb = suburb;
    }
 
    /********** get/set ***********/
    public java.lang.String get_id() {
        return _id;
    }
 
    public void set_id(java.lang.String _id) {
        this._id = _id;
    }
     
    
    
    public Integer getShare() {
        return share;
    }

    public void setShare(Integer share) {
        this.share = share;
    }

    public Integer getRealShare() {
        return realShare;
    }

    public void setRealShare(Integer realShare) {
        this.realShare = realShare;
    }

    public Integer getUserView() {
        return userView;
    }

    public void setUserView(Integer userView) {
        this.userView = userView;
    }

    public Integer getRealView() {
        return realView;
    }

    public void setRealView(Integer realView) {
        this.realView = realView;
    }

    public java.lang.Integer getRealestateNewsId() {
        return realestateNewsId;
    }
 
    public void setRealestateNewsId(java.lang.Integer realestateNewsId) {
        this.realestateNewsId = realestateNewsId;
    }
     
    public java.lang.String getHeadline() {
        return headline;
    }
 
    public void setHeadline(java.lang.String headline) {
        this.headline = headline;
    }
     
    public java.lang.String getTitle() {
        return title;
    }
 
    public void setTitle(java.lang.String title) {
        this.title = title;
    }
     
    public java.lang.String getImage() {
        return image;
    }
 
    public void setImage(java.lang.String image) {
        this.image = image;
    }
     
    public java.lang.String getUrl() {
        return url;
    }
 
    public void setUrl(java.lang.String url) {
        this.url = url;
    }
     
    public java.lang.String getDesc() {
        return desc;
    }
 
    public void setDesc(java.lang.String desc) {
        this.desc = desc;
    }
     
    public java.lang.String getSource() {
        return source;
    }
 
    public void setSource(java.lang.String source) {
        this.source = source;
    }
     
    public java.lang.String getDateStr() {
        return dateStr;
    }
 
    public void setDateStr(java.lang.String dateStr) {
        this.dateStr = dateStr;
    }
     
    public java.util.Date getDate() {
        return date;
    }
 
    public void setDate(java.util.Date date) {
        this.date = date;
    }
     
    public java.util.ArrayList getDescription() {
        return description;
    }
 
    public void setDescription(java.util.ArrayList description) {
        this.description = description;
    }
     
    public java.util.ArrayList getDescSource() {
        return descSource;
    }
 
    public void setDescSource(java.util.ArrayList descSource) {
        this.descSource = descSource;
    }
     
    public java.lang.String getAuthorName() {
        return authorName;
    }
 
    public void setAuthorName(java.lang.String authorName) {
        this.authorName = authorName;
    }
     
    public java.lang.Object getAuthorImage() {
        return authorImage;
    }
 
    public void setAuthorImage(java.lang.Object authorImage) {
        this.authorImage = authorImage;
    }
     
    public java.lang.String getAuthorUrl() {
        return authorUrl;
    }
 
    public void setAuthorUrl(java.lang.String authorUrl) {
        this.authorUrl = authorUrl;
    }
     
    public java.lang.String getState() {
        return state;
    }
 
    public void setState(java.lang.String state) {
        this.state = state;
    }
     
    public java.lang.String getStateAll() {
        return stateAll;
    }
 
    public void setStateAll(java.lang.String stateAll) {
        this.stateAll = stateAll;
    }
     
    public java.lang.String getSuburb() {
        return suburb;
    }
 
    public void setSuburb(java.lang.String suburb) {
        this.suburb = suburb;
    }

    @Override
    public String toString() {
        return "NewsList [_id=" + _id + ", realestateNewsId=" + realestateNewsId + ", headline=" + headline + ", title="
                + title + ", image=" + image + ", url=" + url + ", desc=" + desc + ", source=" + source + ", dateStr="
                + dateStr + ", date=" + date + ", description=" + description + ", descSource=" + descSource
                + ", authorName=" + authorName + ", authorImage=" + authorImage + ", authorUrl=" + authorUrl
                + ", state=" + state + ", stateAll=" + stateAll + ", suburb=" + suburb + ", userView=" + userView
                + ", realView=" + realView + "]";
    }
    
    
     
}