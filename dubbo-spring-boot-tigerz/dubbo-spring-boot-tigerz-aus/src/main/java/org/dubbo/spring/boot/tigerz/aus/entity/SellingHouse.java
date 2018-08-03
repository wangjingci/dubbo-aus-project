package org.dubbo.spring.boot.tigerz.aus.entity;

import com.tigerz.easymongo.annotation.MongoFieldBind;

/**
 * This code is generated by Machine
 * @author wangjignci@126.com
 *
 */
public class SellingHouse
{
    /********** attribute ***********/
    private java.lang.String _id;
     
    private java.lang.String address;
     
    private java.lang.String unitNumber;
     
    private java.lang.String streetNumber;
     
    private java.lang.String street;
     
    private java.lang.String suburb;
     
    private java.lang.String postcode;
     
    private java.lang.String stateAbbreviation;
     
    private java.util.Date createdOn;
     
    private java.util.Date modifiedOn;
     
    private java.lang.String projectName;
     
    private java.lang.String projectImage;
     
    private java.lang.Integer domainId;
     
    private java.lang.String linkUrl;
     
    private java.lang.String projectUrl;
     
    private java.lang.String propertyType;
     
    private java.lang.String listingType;
     
    private org.bson.Document agency;
     
    private java.lang.String brandingAppearance;
     
    private java.util.ArrayList description;
     
    private java.lang.String headline;
     
    private java.lang.Double latitude;
     
    private java.lang.Double longitude;
    
    private java.util.ArrayList basePoint;
     
    private java.lang.Integer beds;
     
    private java.lang.Integer baths;
     
    private java.lang.Integer parking;
     
    private java.lang.String title;
     
    private java.lang.String price;
     
    private java.lang.String promoType;
     
    private java.lang.String tag;
     
    private java.lang.String mode;
     
    private java.lang.String domainStatus;
     
    private java.lang.String method;
     
    private java.lang.String buildingsize;
     
    private java.lang.String primaryPropertyType;
     
    private java.util.ArrayList images;
     
    private java.lang.String propertyId;
     
    private java.util.ArrayList agents;
     
    private java.lang.String propertyProfileUrlSlug;
     
    private java.util.ArrayList schools;
    
    private org.bson.Document neighbourhoodInsights;
     
    private org.bson.Document suburbInsights;
     
    private org.bson.Document suburbMedianPrice;
     
    private org.bson.Document domainSays;
     
    private org.bson.Document inspection;
    
    private org.bson.Document tagList;
     
    private java.util.Date createTime;
     
    private java.util.Date updateTime;
    
    // 补充新的属性
    
    private String housePicMain;
    
    private java.util.ArrayList polygon;
    
    private java.util.ArrayList sales;
    
    private Integer housePrice;
    
    private org.bson.Document valuation;
    
    private Integer landArea;
    
    private java.util.Date actionTime;
    
    private Integer priceType;
    
    private java.util.ArrayList video;
    
    private java.util.ArrayList virtualtour;
    
    private java.util.ArrayList floorplan;
    
    private java.util.ArrayList features;
    
    private String bgColour;
    
    private String status;
    
    private java.util.ArrayList priceList;
     
    private double priceAvg;
    
    private String region;
    
    private String area;
    

    /********** constructors ***********/
    public SellingHouse() {
     
    }
 
 
    /********** get/set ***********/
    public java.lang.String get_id() {
        return _id;
    }
 
    public void set_id(java.lang.String _id) {
        this._id = _id;
    }
    
    

    public String getRegion() {
        return region;
    }


    public void setRegion(String region) {
        this.region = region;
    }


    public String getArea() {
        return area;
    }


    public void setArea(String area) {
        this.area = area;
    }


    public double getPriceAvg() {
        return priceAvg;
    }


    public void setPriceAvg(double priceAvg) {
        this.priceAvg = priceAvg;
    }


    public java.util.ArrayList getPriceList() {
        return priceList;
    }

    public void setPriceList(java.util.ArrayList priceList) {
        this.priceList = priceList;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.lang.String getAddress() {
        return address;
    }
 
    public void setAddress(java.lang.String address) {
        this.address = address;
    }
     
    public java.lang.String getUnitNumber() {
        return unitNumber;
    }
 
    public void setUnitNumber(java.lang.String unitNumber) {
        this.unitNumber = unitNumber;
    }
     
    public java.lang.String getStreetNumber() {
        return streetNumber;
    }
 
    public void setStreetNumber(java.lang.String streetNumber) {
        this.streetNumber = streetNumber;
    }
     
    public java.lang.String getStreet() {
        return street;
    }
 
    public void setStreet(java.lang.String street) {
        this.street = street;
    }
     
    public java.lang.String getSuburb() {
        return suburb;
    }
 
    public void setSuburb(java.lang.String suburb) {
        this.suburb = suburb;
    }
     
    public java.lang.String getPostcode() {
        return postcode;
    }
 
    public void setPostcode(java.lang.String postcode) {
        this.postcode = postcode;
    }
     
    public java.lang.String getStateAbbreviation() {
        return stateAbbreviation;
    }
 
    public void setStateAbbreviation(java.lang.String stateAbbreviation) {
        this.stateAbbreviation = stateAbbreviation;
    }
     
    public java.util.Date getCreatedOn() {
        return createdOn;
    }
 
    public void setCreatedOn(java.util.Date createdOn) {
        this.createdOn = createdOn;
    }
     
    public java.util.Date getModifiedOn() {
        return modifiedOn;
    }
 
    public void setModifiedOn(java.util.Date modifiedOn) {
        this.modifiedOn = modifiedOn;
    }
     
    public java.lang.String getProjectName() {
        return projectName;
    }
 
    public void setProjectName(java.lang.String projectName) {
        this.projectName = projectName;
    }
     
    public java.lang.String getProjectImage() {
        return projectImage;
    }
 
    public void setProjectImage(java.lang.String projectImage) {
        this.projectImage = projectImage;
    }
     
    public java.lang.Integer getDomainId() {
        return domainId;
    }
 
    public void setDomainId(java.lang.Integer domainId) {
        this.domainId = domainId;
    }
     
    public java.lang.String getLinkUrl() {
        return linkUrl;
    }
 
    public void setLinkUrl(java.lang.String linkUrl) {
        this.linkUrl = linkUrl;
    }
     
    public java.lang.String getProjectUrl() {
        return projectUrl;
    }
 
    public void setProjectUrl(java.lang.String projectUrl) {
        this.projectUrl = projectUrl;
    }
     
    public java.lang.String getPropertyType() {
        return propertyType;
    }
 
    public void setPropertyType(java.lang.String propertyType) {
        this.propertyType = propertyType;
    }
     
    public java.lang.String getListingType() {
        return listingType;
    }
 
    public void setListingType(java.lang.String listingType) {
        this.listingType = listingType;
    }
     
    public org.bson.Document getAgency() {
        return agency;
    }
 
    public void setAgency(org.bson.Document agency) {
        this.agency = agency;
    }
     
    public java.lang.String getBrandingAppearance() {
        return brandingAppearance;
    }
 
    public void setBrandingAppearance(java.lang.String brandingAppearance) {
        this.brandingAppearance = brandingAppearance;
    }
     
    public java.util.ArrayList getDescription() {
        return description;
    }
 
    public void setDescription(java.util.ArrayList description) {
        this.description = description;
    }
     
    public java.lang.String getHeadline() {
        return headline;
    }
 
    public void setHeadline(java.lang.String headline) {
        this.headline = headline;
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
     
    public java.util.ArrayList getBasePoint() {
        return basePoint;
    }
 
    public void setBasePoint(java.util.ArrayList basePoint) {
        this.basePoint = basePoint;
    }
     
    public java.lang.Integer getBeds() {
        return beds;
    }
 
    public void setBeds(java.lang.Integer beds) {
        this.beds = beds;
    }
     
    public java.lang.Integer getBaths() {
        return baths;
    }
 
    public void setBaths(java.lang.Integer baths) {
        this.baths = baths;
    }
     
    public java.lang.Integer getParking() {
        return parking;
    }
 
    public void setParking(java.lang.Integer parking) {
        this.parking = parking;
    }
     
    public java.lang.String getTitle() {
        return title;
    }
 
    public void setTitle(java.lang.String title) {
        this.title = title;
    }
     
    public java.lang.String getPrice() {
        return price;
    }
 
    public void setPrice(java.lang.String price) {
        this.price = price;
    }
     
    public java.lang.String getPromoType() {
        return promoType;
    }
 
    public void setPromoType(java.lang.String promoType) {
        this.promoType = promoType;
    }
     
    public java.lang.String getTag() {
        return tag;
    }
 
    public void setTag(java.lang.String tag) {
        this.tag = tag;
    }
     
    public java.lang.String getMode() {
        return mode;
    }
 
    public void setMode(java.lang.String mode) {
        this.mode = mode;
    }
     
    public java.lang.String getDomainStatus() {
        return domainStatus;
    }
 
    public void setDomainStatus(java.lang.String domainStatus) {
        this.domainStatus = domainStatus;
    }
     
    public java.lang.String getMethod() {
        return method;
    }
 
    public void setMethod(java.lang.String method) {
        this.method = method;
    }
     
    public java.lang.String getBuildingsize() {
        return buildingsize;
    }
 
    public void setBuildingsize(java.lang.String buildingsize) {
        this.buildingsize = buildingsize;
    }
     
    public java.lang.String getPrimaryPropertyType() {
        return primaryPropertyType;
    }
 
    public void setPrimaryPropertyType(java.lang.String primaryPropertyType) {
        this.primaryPropertyType = primaryPropertyType;
    }
     
    public java.util.ArrayList getImages() {
        return images;
    }
 
    public void setImages(java.util.ArrayList images) {
        this.images = images;
    }
     
    public java.lang.String getPropertyId() {
        return propertyId;
    }
 
    public void setPropertyId(java.lang.String propertyId) {
        this.propertyId = propertyId;
    }
     
    public java.util.ArrayList getAgents() {
        return agents;
    }
 
    public void setAgents(java.util.ArrayList agents) {
        this.agents = agents;
    }
     
    public java.lang.String getPropertyProfileUrlSlug() {
        return propertyProfileUrlSlug;
    }
 
    public void setPropertyProfileUrlSlug(java.lang.String propertyProfileUrlSlug) {
        this.propertyProfileUrlSlug = propertyProfileUrlSlug;
    }
     
    public java.util.ArrayList getSchools() {
        return schools;
    }
 
    public void setSchools(java.util.ArrayList schools) {
        this.schools = schools;
    }
     
    public org.bson.Document getNeighbourhoodInsights() {
        return neighbourhoodInsights;
    }
 
    public void setNeighbourhoodInsights(org.bson.Document neighbourhoodInsights) {
        this.neighbourhoodInsights = neighbourhoodInsights;
    }
     
    public org.bson.Document getSuburbInsights() {
        return suburbInsights;
    }
 
    public void setSuburbInsights(org.bson.Document suburbInsights) {
        this.suburbInsights = suburbInsights;
    }
     
    public org.bson.Document getSuburbMedianPrice() {
        return suburbMedianPrice;
    }
 
    public void setSuburbMedianPrice(org.bson.Document suburbMedianPrice) {
        this.suburbMedianPrice = suburbMedianPrice;
    }
     
    public org.bson.Document getDomainSays() {
        return domainSays;
    }
 
    public void setDomainSays(org.bson.Document domainSays) {
        this.domainSays = domainSays;
    }
     
    public org.bson.Document getInspection() {
        return inspection;
    }
 
    public void setInspection(org.bson.Document inspection) {
        this.inspection = inspection;
    }
     
    public java.util.Date getCreateTime() {
        return createTime;
    }
 
    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }
     
    public java.util.Date getUpdateTime() {
        return updateTime;
    }
 
    public void setUpdateTime(java.util.Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getHousePicMain() {
        return housePicMain;
    }

    public void setHousePicMain(String housePicMain) {
        this.housePicMain = housePicMain;
    }

    public java.util.ArrayList getPolygon() {
        return polygon;
    }

    public void setPolygon(java.util.ArrayList polygon) {
        this.polygon = polygon;
    }

    public java.util.ArrayList getSales() {
        return sales;
    }

    public void setSales(java.util.ArrayList sales) {
        this.sales = sales;
    }

    public Integer getHousePrice() {
        return housePrice;
    }

    public void setHousePrice(Integer housePrice) {
        this.housePrice = housePrice;
    }

    public org.bson.Document getValuation() {
        return valuation;
    }

    public void setValuation(org.bson.Document valuation) {
        this.valuation = valuation;
    }

    public Integer getLandArea() {
        return landArea;
    }

    public void setLandArea(Integer landArea) {
        this.landArea = landArea;
    }

    public java.util.Date getActionTime() {
        return actionTime;
    }

    public void setActionTime(java.util.Date actionTime) {
        this.actionTime = actionTime;
    }

    public Integer getPriceType() {
        return priceType;
    }

    public void setPriceType(Integer priceType) {
        this.priceType = priceType;
    }

    public java.util.ArrayList getVideo() {
        return video;
    }

    public void setVideo(java.util.ArrayList video) {
        this.video = video;
    }

    public java.util.ArrayList getVirtualtour() {
        return virtualtour;
    }

    public void setVirtualtour(java.util.ArrayList virtualtour) {
        this.virtualtour = virtualtour;
    }

    public java.util.ArrayList getFloorplan() {
        return floorplan;
    }

    public void setFloorplan(java.util.ArrayList floorplan) {
        this.floorplan = floorplan;
    }
    
    
    public java.util.ArrayList getFeatures() {
        return features;
    }

    public void setFeatures(java.util.ArrayList features) {
        this.features = features;
    }

    public String getBgColour() {
        return bgColour;
    }

    public void setBgColour(String bgColour) {
        this.bgColour = bgColour;
    }

    

    public org.bson.Document getTagList() {
        return tagList;
    }


    public void setTagList(org.bson.Document tagList) {
        this.tagList = tagList;
    }


    @Override
    public String toString() {
        return "SellingHouse [_id=" + _id + ", address=" + address + ", unitNumber=" + unitNumber + ", streetNumber="
                + streetNumber + ", street=" + street + ", suburb=" + suburb + ", postcode=" + postcode
                + ", stateAbbreviation=" + stateAbbreviation + ", createdOn=" + createdOn + ", modifiedOn=" + modifiedOn
                + ", projectName=" + projectName + ", projectImage=" + projectImage + ", domainId=" + domainId
                + ", linkUrl=" + linkUrl + ", projectUrl=" + projectUrl + ", propertyType=" + propertyType
                + ", listingType=" + listingType + ", agency=" + agency + ", brandingAppearance=" + brandingAppearance
                + ", description=" + description + ", headline=" + headline + ", latitude=" + latitude + ", longitude="
                + longitude + ", basePoint=" + basePoint + ", beds=" + beds + ", baths=" + baths + ", parking="
                + parking + ", title=" + title + ", price=" + price + ", promoType=" + promoType + ", tag=" + tag
                + ", mode=" + mode + ", domainStatus=" + domainStatus + ", method=" + method + ", buildingsize="
                + buildingsize + ", primaryPropertyType=" + primaryPropertyType + ", images=" + images + ", propertyId="
                + propertyId + ", agents=" + agents + ", propertyProfileUrlSlug=" + propertyProfileUrlSlug
                + ", schools=" + schools + ", neighbourhoodInsights=" + neighbourhoodInsights + ", suburbInsights="
                + suburbInsights + ", suburbMedianPrice=" + suburbMedianPrice + ", domainSays=" + domainSays
                + ", inspection=" + inspection + ", createTime=" + createTime + ", updateTime=" + updateTime
                + ", housePicMain=" + housePicMain + ", polygon=" + polygon + ", sales=" + sales + ", housePrice="
                + housePrice + ", valuation=" + valuation + ", landArea=" + landArea + ", actionTime=" + actionTime
                + ", priceType=" + priceType + ", video=" + video + ", virtualtour=" + virtualtour + ", floorplan="
                + floorplan + ", features=" + features + ", bgColour=" + bgColour + ", status=" + status
                + ", priceList=" + priceList + "]";
    }
    
    
     
    
}