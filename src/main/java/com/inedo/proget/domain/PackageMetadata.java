package com.inedo.proget.domain;

public class PackageMetadata {
	/** A string of no more than fifty characters: 
	 * 		numbers (0-9)
	 * 		upper- and lower-case letters (a-Z)
	 * 		dashes (-)
	 * 		periods (.)
	 * 		forward-slashes (/)
	 * 		underscores (_)
	 * 		may not start or end with a forward-slash 
	 * Example: initrode/vendors/abl
	 * Required. 
	 */
	public String group;
	
	/** 
	 * A string of no more than fifty characters: 
	 * 		numbers (0-9)
	 * 		upper- and lower-case letters (a-Z)
	 * 		dashes (-)
	 * 		periods (.)
	 * 		and underscores (_)
	 * Example: ABLast
	 * Required.
	 */
	public String name;
	
	/** 
	 * A string consisting of three integers separated by periods (.); leading zeros are not permitted 
	 * Example: 0.0.1
	 * Required
	 */
	public String version;
	
	/** 
	 * A string of no more than fifty characters 
	 * Optional
	 */
	public String title;
	
	/** 
	 * A string of an absolute url pointing to an image to be displayed in the ProGet UI (at both 64px and 128px); 
	 * if  package:// is used as the protocol, ProGet will search within the package and serve that image instead
	 * Example: package://ablast.svg
	 * Optional
	 */
	public String icon;
	
	/** 
	 * A string containing any number of charters; these will be formatted as Markdown in the ProGet UI 
	 * Example: "This contains [ast distro](http://initrode-net.local/ast) files specific to ABL",
	 * Optional
	 */
	public String description;
	
	/**
	 * 	An array of strings, each consisting of a package identification string; this string is formatted as follows:
	 *	«group»:«package-name»
	 *  «group»:«package-name»:«version»
	 * When the version is not specified, the latest is used.
	 * 
	 * Example: [ "initrode/vendors-common:ast-common:2.0.0" ]
	 * Optional
	 */
	public String dependencies;
}
