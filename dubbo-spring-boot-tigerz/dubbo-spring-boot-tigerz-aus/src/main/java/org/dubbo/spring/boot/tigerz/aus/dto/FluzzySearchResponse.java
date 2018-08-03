package org.dubbo.spring.boot.tigerz.aus.dto;

public class FluzzySearchResponse implements Comparable<FluzzySearchResponse> {
	private String _id;
	private String name;
	private String displayName;
	private Integer level;
	private String fatherName;
	private double score;
	private String searchContent;
	
	
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getLevel() {
		return level;
	}
	public void setLevel(Integer level) {
		this.level = level;
	}
	public String getFatherName() {
		return fatherName;
	}
	public void setFatherName(String fatherName) {
		this.fatherName = fatherName;
	}

    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setSearchContent(String content) {
        if (content.length() >= 2 && content.length() <= 3) {
            content = content.substring(0, 1).toUpperCase() + content.substring(1).toLowerCase();
        } if (content.length() == 1) {
            content = content.toUpperCase();
        } if (content.length() >= 3) {
            content = content.substring(0,3);
            content = content.substring(0, 1).toUpperCase() + content.substring(1).toLowerCase();
        }
        this.searchContent = content;
    }

    @Override
    public int compareTo(FluzzySearchResponse o) {
        if (o == null) {
            throw new IllegalArgumentException("参数异常");
        }
        
        if (o.score == this.score && o.level == this.level && this.searchContent.length() > 0) {
            int index = this.displayName.indexOf(this.searchContent);
            int oindex = o.displayName.indexOf(o.searchContent);
            if (index < 0) index += 1000;
            if (oindex < 0) oindex += 1000;
            return index - oindex;
        }
        
        return Double.compare(o.score, this.score);
    }
	
	
}
