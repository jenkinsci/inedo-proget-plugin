package com.inedo.proget.domain;

public class Feed
{
	public static final String SINGLE = 
			"{\"Feed_Id\":1,\"Feed_Name\":\"Default\",\"Feed_Description\":null,\"Active_Indicator\":true,\"Cache_Connectors_Indicator\":true,\"DropPath_Text\":null,\"FeedPathOverride_Text\":null,\"FeedType_Name\":\"NuGet\",\"PackageStoreConfiguration_Xml\":null}";
			
	public static final String MULTIPLE = "["
			+ "{\"Feed_Id\":1,\"Feed_Name\":\"Default\",\"Feed_Description\":null,\"Active_Indicator\":true,\"Cache_Connectors_Indicator\":true,\"DropPath_Text\":null,\"FeedPathOverride_Text\":null,\"FeedType_Name\":\"NuGet\",\"PackageStoreConfiguration_Xml\":null},"
			+ "{\"Feed_Id\":2,\"Feed_Name\":\"Example\",\"Feed_Description\":null,\"Active_Indicator\":true,\"Cache_Connectors_Indicator\":true,\"DropPath_Text\":null,\"FeedPathOverride_Text\":null,\"FeedType_Name\":\"ProGet\",\"PackageStoreConfiguration_Xml\":null}"
			+ "]";

	public String Feed_Id;
	public String Feed_Name;
	public String Active_Indicator;
	public String FeedType_Name;
}

