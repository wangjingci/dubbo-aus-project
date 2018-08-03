package org.dubbo.spring.boot.tigerz.aus.entity;

public class BrowseHouseHistory {
    
    private Integer bed;
    private Integer parking;
    private String houseId;
    private String street;
    private String ip;
    private String suburb;
    private Integer housePrice;
    private Integer bath;
    private String uid;
    private Integer utype;
    private Integer operation;
    
    private String address;
    private String mainPic;
    private String price;
    
    private Long updateTime;
    
    public BrowseHouseHistory() {
        
    }
    
    public Integer getBed() {
        return bed;
    }
    public void setBed(Integer bed) {
        this.bed = bed;
    }
    public Integer getParking() {
        return parking;
    }
    public void setParking(Integer parking) {
        this.parking = parking;
    }
    public String getHouseId() {
        return houseId;
    }
    public void setHouseId(String houseId) {
        this.houseId = houseId;
    }
    public String getStreet() {
        return street;
    }
    public void setStreet(String street) {
        this.street = street;
    }
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public String getSuburb() {
        return suburb;
    }
    public void setSuburb(String suburb) {
        this.suburb = suburb;
    }
    public Integer getHousePrice() {
        return housePrice;
    }
    public void setHousePrice(Integer housePrice) {
        this.housePrice = housePrice;
    }
    public Integer getBath() {
        return bath;
    }
    public void setBath(Integer bath) {
        this.bath = bath;
    }
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
    public Integer getUtype() {
        return utype;
    }
    public void setUtype(Integer utype) {
        this.utype = utype;
    }
    public Integer getOperation() {
        return operation;
    }
    public void setOperation(Integer operation) {
        this.operation = operation;
    }
    public Long getUpdateTime() {
        return updateTime;
    }
    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
    
    
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMainPic() {
        return mainPic;
    }

    public void setMainPic(String mainPic) {
        this.mainPic = mainPic;
    }

    
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "BrowseHouseHistory [bed=" + bed + ", parking=" + parking + ", houseId=" + houseId + ", street=" + street
                + ", ip=" + ip + ", suburb=" + suburb + ", housePrice=" + housePrice + ", bath=" + bath + ", uid=" + uid
                + ", utype=" + utype + ", operation=" + operation + ", updateTime=" + updateTime + "]";
    }
    
    

}
