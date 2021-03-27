package me.ag2s.epublib.domain;
@SuppressWarnings("unused")
public enum ManifestItemRefProperties implements ManifestProperties {
	PAGE_SPREAD_LEFT("page-spread-left"),
	PAGE_SPREAD_RIGHT("page-spread-right");
	
	private final String name;
	
	ManifestItemRefProperties(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
