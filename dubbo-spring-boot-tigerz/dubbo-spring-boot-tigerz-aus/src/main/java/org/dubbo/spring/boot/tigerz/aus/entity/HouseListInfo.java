package org.dubbo.spring.boot.tigerz.aus.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.tigerz.easymongo.util.Assert;


public class HouseListInfo {
	private String fatherName;
	private String searchName;
	
	private Double areaMidPrice; 
	private int curPage;
	private long maxPage;
	private long propNum;
	
	private ArrayList<Double> basePoint;  //中心点坐标
	private int mapLevel;          	       //1 地图上显示area， 2 地图上显示city 3 地图上显示suburb 4 地图上显示房子
	@SuppressWarnings("rawtypes")
    private LinkedList mapInfo = new LinkedList<>();   //地图显示信息, 类型因数据不同而不同,没有set方法，所以保证安全
	private LinkedList<HouseSimpleInfo> houseInfo = new LinkedList<>();
	@SuppressWarnings("unused")
    private List<School> schoolList;
	
	/**
	 * 从搜索结果里构建MapInfoList
	 * 分两种情况：
	 * 1. 如果地图显示的是房源信息，则装入图片，位置，价格等信息
	 * 2. 如果地图是区域信息，则装入位置，房源数量等信息
	 * @param hits 
	 * @param areaLevel  只支持0 - 5
	 * @return
	 */
	@SuppressWarnings("unchecked")
    public int setMapListFromSearchHits(SearchHits hits, int areaLevel) {
	    Assert.notNull(hits, "搜索结果不能为Null");
	    Assert.isRangeIn(areaLevel, 0, 5, "areaLevel必须在0-5的范围");
	    
	    if (areaLevel >= 0 && areaLevel <= 1) {
	        
	        this.mapInfo = new LinkedList<MapInfoForArea>();
	        
	        for (SearchHit hit : hits.getHits()){
	            Map<String, Object> result = hit.getSource();
	            MapInfoForArea simpleMapInfo = new MapInfoForArea();
	            // 就是name，兼容过去的字段
	            simpleMapInfo.setKeyword((String)result.get("name"));  
	            simpleMapInfo.setDisplayInfo((String)result.get("name"));
	            simpleMapInfo.setLevel((int)result.get("level")); 
	            if(result.get("house_count_by_day") != null){
	                int houseCount = (int)result.get("house_count_by_day");
	                if(houseCount == 0){
	                    //如果该区域没有可售房源，则不给前端显示
	                    continue;
	                }
	                simpleMapInfo.setPropNum((int)result.get("house_count_by_day"));
	            }
	            if(result.get("mid_price") != null){
	                simpleMapInfo.setMdiPrice((double)result.get("valuer_median"));
	            }
	                
	            //需要看看返回的是否是ArrayList，还是Double[]
	            ArrayList<Double> point = (ArrayList<Double>)result.get("base_point");
	            if(point == null){
	                continue;
	            }
	            simpleMapInfo.setBasePoint(point);
	            if (areaLevel == 1) {
	                simpleMapInfo.setBasePointDisplay((ArrayList<Double>)result.get("base_point_display"));
	            }
	            this.mapInfo.add(simpleMapInfo);
	        }
	    } else  {
	        
	        this.mapInfo = new LinkedList<MapInfoForHouse>();
	        
	        for (SearchHit hit : hits.getHits()){
                Map<String, Object> result = hit.getSource();
                MapInfoForHouse simpleMapInfo = new MapInfoForHouse();
                simpleMapInfo.setKeyword((String)result.get("address"));
                simpleMapInfo.setAddress((String)result.get("address"));
                simpleMapInfo.setLevel(5); 
                simpleMapInfo.setHousePrice((String)result.get("price"));
                simpleMapInfo.set_id(hit.getId());
                simpleMapInfo.setTitle((String)result.get("headline"));
                simpleMapInfo.setSuburb((String)result.get("suburb"));
                // TODO 这里只是临时使用domain的主图
                simpleMapInfo.setHouseMainImagePath((String)result.get("house_pic_main"));
                
                // 返回什么数据还不清楚
                if(result.get("polygon") != null){
                    Map<String, Object> map = (Map<String,Object>)result.get("coordinate_array");
                    if (map != null) {
                        @SuppressWarnings("rawtypes")
                        ArrayList doc = (ArrayList)map.get("coordinates");
                        simpleMapInfo.setCoordinateArray(doc);
                    }
                }
                
                //TODO 房源数量，以后可以进一步考虑公寓情况，就不是固定一个了
                simpleMapInfo.setPropNum(1);
                ArrayList<Double> point = (ArrayList<Double>)result.get("base_point");
                if(point == null){
                    continue;
                }
                simpleMapInfo.setBasePoint(point);
                
                simpleMapInfo.setBathroom((Integer)result.get("baths"));
                simpleMapInfo.setLandArea((Integer)result.get("land_area"));
                simpleMapInfo.setBedroom((Integer)result.get("beds"));
                // 不要下面两个字段了
                // mapInfo.setListingNo((String)result.get("listing_no"));
                // mapInfo.setListingView((String)result.get("listing_view"));
                simpleMapInfo.setStreetAddress((String)result.get("street"));
                String dateStr = (String)result.get("created_on");
                if(dateStr != null){
                    long dateLong = Long.valueOf(dateStr);
                    simpleMapInfo.setListedDate(dateLong);
                }
                
                this.mapInfo.add(simpleMapInfo);
            }
	    }
	    
	    
	    return this.mapInfo.size();
	}
	
	public int setHouseListFromSearchHits(SearchHits hits) {
	    return setHouseListFromSearchHits(hits,0,20);
	}
	
	/**
	 * 把搜索返回来的数据进行分页
	 * @param hits
	 * @param page 注意，这个page并不是用户输入的page，而是搜素获取一个1000个大小的数据集，在数据集内进行分页
	 * @param countPerPage
	 * @return
	 */
	@SuppressWarnings("unchecked")
    public int setHouseListFromSearchHits(SearchHits hits, int page, long countPerPage) {
	    
	    this.houseInfo = new LinkedList<HouseSimpleInfo>();
	    int index = 0;
	    long from = page * countPerPage;
	    long to = (page + 1) * countPerPage;
	    for (SearchHit hit : hits.getHits()){
            HouseSimpleInfo houseSimpleInfo = new HouseSimpleInfo();
            houseSimpleInfo.set_id(hit.getId());
            houseSimpleInfo.setScore(hit.getScore());
            Map<String, Object> result = hit.getSource();
            houseSimpleInfo.setBathroom((Integer)result.get("baths"));
            houseSimpleInfo.setBedroom((Integer)result.get("beds"));
            houseSimpleInfo.setParking((Integer)result.get("parking"));
            // TODO 这个图片是domain的图，暂时先用.我们的首页是house_main_image_path
            houseSimpleInfo.setHouseMainImagePath((String)result.get("house_pic_main"));
            houseSimpleInfo.setImages((ArrayList<String>)result.get("images"));
            houseSimpleInfo.setHousePrice((String)result.get("price"));
            houseSimpleInfo.setPrice((Integer)result.get("house_price"));
            String dateStr = (String)result.get("created_on");
            if(dateStr != null){
                long dateLong = Long.valueOf(dateStr);
                houseSimpleInfo.setListedDate(dateLong);
            }
            ArrayList<Double> point = (ArrayList<Double>)result.get("base_point");
            if(point == null){
                continue;
            }
            houseSimpleInfo.setBasePoint(point);
            houseSimpleInfo.setStatus((String)result.get("status"));
            houseSimpleInfo.setTitle((String)result.get("headline"));
            houseSimpleInfo.setSuburb((String)result.get("suburb"));
            houseSimpleInfo.setAddress((String)result.get("address"));
            houseSimpleInfo.setTagList((HashMap<String,Object>)result.get("tag_list"));
            if (result.get("price_avg") != null) {
                houseSimpleInfo.setPriceAvg((Double)result.get("price_avg"));
            }
            
            houseSimpleInfo.setPropertyType((String)result.get("property_type"));
            
            if (index >= from && index < to) {
                this.houseInfo.add(houseSimpleInfo);
            }
            if (index >= to) {
                break;
            }
  
            index ++;
        }
	    
	    return this.houseInfo.size();
	}
	
	public static class HouseSimpleInfo {
		private String _id;
		private Integer bathroom;
		private Integer bedroom;
		private String houseMainImagePath;
		private ArrayList<String> images;
		private Long listedDate;
		private String status;
		private String title;
		private String housePrice;
		private Integer price;
		private String suburb;
		private String address;
		private ArrayList<Double> basePoint;
		private Integer parking;
        private HashMap<String,Object> tagList;
        private String propertyType;
        private double priceAvg;
        private float score;
        private Boolean isFollowed;
		
		public HouseSimpleInfo(){}
		
		@SuppressWarnings("unchecked")
        public HouseSimpleInfo(SellingHouse sellingHouse){
		    this._id = sellingHouse.get_id();
		    this.bathroom = sellingHouse.getBaths();
		    this.bedroom = sellingHouse.getBeds();
		    this.houseMainImagePath = sellingHouse.getHousePicMain();
		    this.images = sellingHouse.getImages();
		    this.listedDate = sellingHouse.getCreatedOn().getTime();
		    this.status = sellingHouse.getStatus();
		    this.title = sellingHouse.getTitle();
		    this.housePrice = sellingHouse.getPrice();
		    this.price = sellingHouse.getHousePrice();
		    this.suburb = sellingHouse.getSuburb();
		    this.address = sellingHouse.getAddress();
		    this.basePoint = sellingHouse.getBasePoint();
		    this.parking = sellingHouse.getParking();
		    this.propertyType = sellingHouse.getPropertyType();
		    this.priceAvg = sellingHouse.getPriceAvg();
		    
		    if (sellingHouse.getTagList() != null) {
		        Document tagDoc = sellingHouse.getTagList();
		        HashMap<String,Object> map = new HashMap<>();
		        for (String key : tagDoc.keySet()) {
		            map.put(key, tagDoc.get(key)) ;
		        }
		        this.tagList = map;
		    }
		    
		    
		}
		
		public float getScore() {
            return score;
        }



        public void setScore(float score) {
            this.score = score;
        }



        public Integer getPrice() {
            return price;
        }

        public void setPrice(Integer price) {
            this.price = price;
        }


        public ArrayList<Double> getBasePoint() {
            return basePoint;
        }


        public void setBasePoint(ArrayList<Double> basePoint) {
            this.basePoint = basePoint;
        }


        public ArrayList<String> getImages() {
            return images;
        }
        public void setImages(ArrayList<String> images) {
            this.images = images;
        }
        public String get_id() {
			return _id;
		}
		public void set_id(String _id) {
			this._id = _id;
		}
		public Long getListedDate() {
			return listedDate;
		}
		public void setListedDate(Long listedDate) {
			this.listedDate = listedDate;
		}
		
		public String getSuburb() {
			return suburb;
		}
		public void setSuburb(String suburb) {
			this.suburb = suburb;
		}
		public Integer getBathroom() {
			return bathroom;
		}
		public void setBathroom(Integer bathroom) {
			this.bathroom = bathroom;
		}
		public Integer getBedroom() {
			return bedroom;
		}
		public void setBedroom(Integer bedroom) {
			this.bedroom = bedroom;
		}
		public String getHouseMainImagePath() {
			return houseMainImagePath;
		}
		public void setHouseMainImagePath(String houseMainImagePath) {
			this.houseMainImagePath = houseMainImagePath;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public String getHousePrice() {
			return housePrice;
		}
		public void setHousePrice(String housePrice) {
			this.housePrice = housePrice;
		}
        public String getAddress() {
            return address;
        }
        public void setAddress(String address) {
            this.address = address;
        }


        public Integer getParking() {
            return parking;
        }
        public void setParking(Integer parking) {
            this.parking = parking;
        }


        public HashMap<String,Object> getTagList() {
            return tagList;
        }


        public void setTagList(HashMap<String,Object> tagList) {
            this.tagList = tagList;
        }

        public String getPropertyType() {
            return propertyType;
        }

        public void setPropertyType(String propertyType) {
            this.propertyType = propertyType;
        }

        public double getPriceAvg() {
            return priceAvg;
        }

        public void setPriceAvg(double priceAvg) {
            this.priceAvg = priceAvg;
        }

        public Boolean getIsFollowed() {
            return isFollowed;
        }

        public void setIsFollowed(Boolean isFollowed) {
            this.isFollowed = isFollowed;
        }

        @Override
        public String toString() {
            return "HouseSimpleInfo [_id=" + _id + ", bathroom=" + bathroom + ", bedroom=" + bedroom
                    + ", houseMainImagePath=" + houseMainImagePath + ", images=" + images + ", listedDate=" + listedDate
                    + ", status=" + status + ", title=" + title + ", housePrice=" + housePrice + ", price=" + price
                    + ", suburb=" + suburb + ", address=" + address + ", basePoint=" + basePoint + ", parking="
                    + parking + ", tagList=" + tagList + ", propertyType=" + propertyType + ", priceAvg=" + priceAvg
                    + ", score=" + score + ", isFollowed=" + isFollowed + "]";
        }
        
		
	}
	
	public static class MapInfoForHouse {
		private String _id;
		private String houseMainImagePath;
		private String title;
		private String keyword;
		private Integer level;
		private Integer propNum;
		private Double  mdiPrice;
		private String housePrice;
		private Integer bathroom;
		private Integer bedroom;
		private String listingNo;
		private Long listedDate;
		private String listingView;
		private String streetAddress;
		private String suburb;
		@SuppressWarnings("rawtypes")
		private ArrayList coordinateArray; // 内部设置，没有安全问题
		private ArrayList<Double> basePoint;
		private ArrayList<Double> basePointDisplay;
		private Integer landArea;
		private String address;
		
		private MapInfoForHouse() {}
		
        public String get_id() {
            return _id;
        }
        public void set_id(String _id) {
            this._id = _id;
        }
        public String getAddress() {
            return address;
        }
        public void setAddress(String address) {
            this.address = address;
        }
        public String getHouseMainImagePath() {
            return houseMainImagePath;
        }
        public void setHouseMainImagePath(String houseMainImagePath) {
            this.houseMainImagePath = houseMainImagePath;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public String getKeyword() {
            return keyword;
        }
        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
        public Integer getLevel() {
            return level;
        }
        public void setLevel(Integer level) {
            this.level = level;
        }
        public Integer getPropNum() {
            return propNum;
        }
        public void setPropNum(Integer propNum) {
            this.propNum = propNum;
        }
        public Double getMdiPrice() {
            return mdiPrice;
        }
        public void setMdiPrice(Double mdiPrice) {
            this.mdiPrice = mdiPrice;
        }
        public String getHousePrice() {
            return housePrice;
        }
        public void setHousePrice(String housePrice) {
            this.housePrice = housePrice;
        }
        public Integer getBathroom() {
            return bathroom;
        }
        public void setBathroom(Integer bathroom) {
            this.bathroom = bathroom;
        }
        public Integer getBedroom() {
            return bedroom;
        }
        public void setBedroom(Integer bedroom) {
            this.bedroom = bedroom;
        }
        public String getListingNo() {
            return listingNo;
        }
        public void setListingNo(String listingNo) {
            this.listingNo = listingNo;
        }
        public Long getListedDate() {
            return listedDate;
        }
        public void setListedDate(Long listedDate) {
            this.listedDate = listedDate;
        }
        public String getListingView() {
            return listingView;
        }
        public void setListingView(String listingView) {
            this.listingView = listingView;
        }
        public String getStreetAddress() {
            return streetAddress;
        }
        public void setStreetAddress(String streetAddress) {
            this.streetAddress = streetAddress;
        }
        public String getSuburb() {
            return suburb;
        }
        public void setSuburb(String suburb) {
            this.suburb = suburb;
        }
        @SuppressWarnings("rawtypes")
        public ArrayList getCoordinateArray() {
            return coordinateArray;
        }
        @SuppressWarnings("rawtypes")
        public void setCoordinateArray(ArrayList coordinateArray) {
            this.coordinateArray = coordinateArray;
        }
        public ArrayList<Double> getBasePoint() {
            return basePoint;
        }
        public void setBasePoint(ArrayList<Double> basePoint) {
            this.basePoint = basePoint;
        }
        public ArrayList<Double> getBasePointDisplay() {
            return basePointDisplay;
        }
        public void setBasePointDisplay(ArrayList<Double> basePointDisplay) {
            this.basePointDisplay = basePointDisplay;
        }
        public Integer getLandArea() {
            return landArea;
        }
        public void setLandArea(Integer landArea) {
            this.landArea = landArea;
        }

		
	}
	
    public static class MapInfoForArea {
        private String displayInfo ;
        private String keyword;
        private Integer level;
        private Integer propNum;
        private Double  mdiPrice;
        @SuppressWarnings("rawtypes")
        private ArrayList coordinateArray; // 内部设置，没有安全问题
        private ArrayList<Double> basePoint;
        private ArrayList<Double> basePointDisplay;
        
        private MapInfoForArea(){}
        public Integer getLevel() {
            return level;
        }
        private void setLevel(Integer level) {
            this.level = level;
        }
        public Integer getPropNum() {
            return propNum;
        }
        private void setPropNum(Integer propNum) {
            this.propNum = propNum;
        }
        public Double getMdiPrice() {
            return mdiPrice;
        }
        private void setMdiPrice(Double mdiPrice) {
            this.mdiPrice = mdiPrice;
        }
        @SuppressWarnings("rawtypes")
        public ArrayList getCoordinateArray() {
            return coordinateArray;
        }
        @SuppressWarnings("unused")
        private void setCoordinateArray(@SuppressWarnings("rawtypes") ArrayList coordinateArray) {
            this.coordinateArray = coordinateArray;
        }
        public ArrayList<Double> getBasePoint() {
            return basePoint;
        }
        private void setBasePoint(ArrayList<Double> basePoint) {
            this.basePoint = basePoint;
        }
        public ArrayList<Double> getBasePointDisplay() {
            return basePointDisplay;
        }
        private void setBasePointDisplay(ArrayList<Double> basePointDisplay) {
            this.basePointDisplay = basePointDisplay;
        }
        public String getDisplayInfo() {
            return displayInfo;
        }
        private void setDisplayInfo(String displayInfo) {
            this.displayInfo = displayInfo;
        }
        public String getKeyword() {
            return keyword;
        }
        private void setKeyword(String keyword) {
            this.keyword = keyword;
        }
        
        
    }
    

	
	public String getSearchName() {
		return searchName;
	}

	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}

	public Double getAreaMidPrice() {
		return areaMidPrice;
	}

	public void setAreaMidPrice(Double areaMidPrice) {
		this.areaMidPrice = areaMidPrice;
	}

	public String getFatherName() {
		return fatherName;
	}

	public void setFatherName(String fatherName) {
		this.fatherName = fatherName;
	}

	public ArrayList<Double> getBasePoint() {
		return basePoint;
	}

	public void setBasePoint(ArrayList<Double> basePoint) {
		this.basePoint = basePoint;
	}

	public int getCurPage() {
		return curPage;
	}

	public void setCurPage(int curPage) {
		this.curPage = curPage;
	}

	public long getMaxPage() {
		return maxPage;
	}

	public void setMaxPage(long maxPage) {
		this.maxPage = maxPage;
	}

	public long getPropNum() {
		return propNum;
	}

	public void setPropNum(long propNum) {
		this.propNum = propNum;
	}

	public	LinkedList<HouseSimpleInfo> getHouseInfo() {
		return houseInfo;
	}
	
	public void setHouseInfo(LinkedList<HouseSimpleInfo> houseInfo) {
	    this.houseInfo = houseInfo;
	}

	public int getMapLevel() {
		return mapLevel;
	}

	public void setMapLevel(int mapLevel) {
		this.mapLevel = mapLevel;
	}

    @SuppressWarnings("rawtypes")
    public LinkedList getMapInfo() {
        return mapInfo;
    }
    
    public void setMapInfo(LinkedList mapInfo) {
        this.mapInfo = mapInfo;
    }

    public List<School> getSchoolList() {
        return schoolList;
    }

    public void setSchoolList(List<School> schoolList) {
        this.schoolList = schoolList;
    }

    
	
}
