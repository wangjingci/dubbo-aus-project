package org.dubbo.spring.boot.tigerz.gm.entity;
//import org.springframework.data.mongodb.core.mapping.Document;

//@MongoCollectionBind(collectionName = "simple_selling_house", databaseName = "crawlerdb")
public class Ip2locationDb11
{
    /********** attribute ***********/
    private java.lang.String _id;
     
    private java.lang.Integer ipFrom;
     
    private java.lang.Integer ipTo;
     
    private java.lang.String countryCode;
     
    private java.lang.String countryName;
     
    private java.lang.String regionName;
     
    private java.lang.String cityName;
     
    private java.lang.Double latitude;
     
    private java.lang.Double longitude;
     
    private java.lang.String zipCode;
     
    private java.lang.String timeZone;
     
    /********** constructors ***********/
    public Ip2locationDb11() {
     
    }
 
    public Ip2locationDb11(java.lang.String _id, java.lang.Integer ipFrom, java.lang.Integer ipTo, java.lang.String countryCode, java.lang.String countryName, java.lang.String regionName, java.lang.String cityName, java.lang.Double latitude, java.lang.Double longitude, java.lang.String zipCode, java.lang.String timeZone) {
        this._id = _id;
        this.ipFrom = ipFrom;
        this.ipTo = ipTo;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.regionName = regionName;
        this.cityName = cityName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zipCode = zipCode;
        this.timeZone = timeZone;
    }
 
    /********** get/set ***********/
    public java.lang.String get_id() {
        return _id;
    }
 
    public void set_id(java.lang.String _id) {
        this._id = _id;
    }
     
    public java.lang.Integer getIpFrom() {
        return ipFrom;
    }
 
    public void setIpFrom(java.lang.Integer ipFrom) {
        this.ipFrom = ipFrom;
    }
     
    public java.lang.Integer getIpTo() {
        return ipTo;
    }
 
    public void setIpTo(java.lang.Integer ipTo) {
        this.ipTo = ipTo;
    }
     
    public java.lang.String getCountryCode() {
        return countryCode;
    }
 
    public void setCountryCode(java.lang.String countryCode) {
        this.countryCode = countryCode;
    }
     
    public java.lang.String getCountryName() {
        return countryName;
    }
 
    public void setCountryName(java.lang.String countryName) {
        this.countryName = countryName;
    }
     
    public java.lang.String getRegionName() {
        return regionName;
    }
 
    public void setRegionName(java.lang.String regionName) {
        this.regionName = regionName;
    }
     
    public java.lang.String getCityName() {
        return cityName;
    }
 
    public void setCityName(java.lang.String cityName) {
        this.cityName = cityName;
    }
     
    public java.lang.Double getLatitude() {
        return latitude;
    }
 
    public void setLatitude(java.lang.Double latitude) {
        this.latitude = latitude;
    }
     
    public java.lang.Double getLongitude() {
        return longitude;
    }
 
    public void setLongitude(java.lang.Double longitude) {
        this.longitude = longitude;
    }
     
    public java.lang.String getZipCode() {
        return zipCode;
    }
 
    public void setZipCode(java.lang.String zipCode) {
        this.zipCode = zipCode;
    }
     
    public java.lang.String getTimeZone() {
        return timeZone;
    }
 
    public void setTimeZone(java.lang.String timeZone) {
        this.timeZone = timeZone;
    }
     
}
