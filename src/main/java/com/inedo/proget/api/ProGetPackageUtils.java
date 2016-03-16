package com.inedo.proget.api;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.types.FileSet;

import com.inedo.proget.domain.PackageMetadata;

import hudson.Util;

public class ProGetPackageUtils
{
	private File sourceFolder;
	private File zipFile;
	private ZipOutputStream zos = null;
		
	public File createPackage(File workFolder, List<File> files, PackageMetadata metadata) throws IOException {		
		FileOutputStream fos = null;
		
		this.sourceFolder = workFolder;
		this.zipFile = new File(sourceFolder, metadata.name.replace(" ",  "") + ".unpack");
		
		try {
			fos = new FileOutputStream(zipFile);
			zos = new ZipOutputStream(fos);

			appendMetadata(metadata);
//			appendFiles(files, "unpack/");
		} finally {
			if (zos != null) zos.closeEntry();
			if (zos != null) zos.close(); 
			if (fos != null) fos.close();
		}
		
		return zipFile;
	}

	public File createPackage(File sourceFolder, PackageMetadata metadata) throws IOException {		
		FileOutputStream fos = null;
		
		this.sourceFolder = sourceFolder;
		this.zipFile = new File(sourceFolder, metadata.name.replace(" ",  "") + ".unpack");
		
		try {
			fos = new FileOutputStream(zipFile);
			zos = new ZipOutputStream(fos);

			appendMetadata(metadata);
			appendFiles(sourceFolder, "unpack/");
		} finally {
			if (zos != null) zos.closeEntry();
			if (zos != null) zos.close(); 
			if (fos != null) fos.close();
		}
		
		return zipFile;
	}

	private void appendMetadata(PackageMetadata metadata) throws IOException {
		String newLine = System.getProperty("line.separator");;
		StringBuilder sb = new StringBuilder();
		
		sb.append("{");
		sb.append(newLine).append("\t\"group\": \"").append(metadata.group).append("\",");
		sb.append(newLine).append("\t\"name\": \"").append(metadata.name).append("\",");
		sb.append(newLine).append("\t\"version\": \"").append(metadata.version).append("\"");
		
		if (isProvided(metadata.title)) {
			sb.append(",").append(newLine).append("\t\"title\": \"").append(metadata.title).append("\"");
		}
		if (isProvided(metadata.icon)) {
			sb.append(",").append(newLine).append("\t\"icon\": \"").append(metadata.icon).append("\"");
		}
		if (isProvided(metadata.description)) {
			sb.append(",").append(newLine).append("\t\"description\": \"").append(metadata.description).append("\"");
		}
		if (isProvided(metadata.dependencies)) {
			sb.append(",").append(newLine).append("\t\"dependencies\": \"").append(metadata.dependencies).append("\"");
		}
		sb.append(newLine).append("}");
		
		ZipEntry ze = new ZipEntry("upack.json");
		byte[] buffer = new byte[1024];
		int len;
		
		zos.putNextEntry(ze);
		
		try (InputStream in = new ByteArrayInputStream(sb.toString().getBytes());) {
			while ((len = in.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}
		}
	}

	private boolean isProvided(String value) {
		return value !=null && !value.isEmpty();
	}
	
	private void appendFiles(Map<String, String> files, String destinationFolder) {
		for (Map.Entry<String, String> filePath : files.entrySet())	{
			//TODO finish off
//			filePath.getValue();
//			
//			ZipItem entry = generateZipEntry(node, destinationFolder);			
//			ZipEntry ze = new ZipEntry(entry.destinationFile);
//			byte[] buffer = new byte[1024];
//			int len;
//			
//			zos.putNextEntry(ze);
//
//			try (FileInputStream in = new FileInputStream(new File(entry.sourceFile))) {
//				while ((len = in.read(buffer)) > 0) {
//					zos.write(buffer, 0, len);
//				}
//			}
		}
	}
	
	/**
	 * Traverse a directory and get all files,
	 * and add the file into fileList  
	 * @param node file or directory
	 * @throws IOException 
	 */
	private void appendFiles(File node, String destinationFolder) throws IOException {

		//add file only
		if(node.isFile()) {
			if (node.equals(zipFile)) {
				return;
			}
			
			ZipItem entry = generateZipEntry(node, destinationFolder);			
			ZipEntry ze = new ZipEntry(entry.destinationFile);
			byte[] buffer = new byte[1024];
			int len;
			
			zos.putNextEntry(ze);

			try (FileInputStream in = new FileInputStream(new File(entry.sourceFile))) {
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
			}
		}

		if(node.isDirectory()){
			String[] subNote = node.list();
			for(String filename : subNote){
				appendFiles(new File(node, filename), destinationFolder);
			}
		}

	}
	
	/**
	 * Format the file path for zip
	 * @param file file path
	 * @return Formatted file path
	 */
	private ZipItem generateZipEntry(File file, String destinationFolder) {
		String srcFile = file.getAbsolutePath();
		String srcFolder = sourceFolder.getAbsolutePath();
		
		String destFile = srcFile.substring(srcFolder.length() + 1, srcFile.length());
				
		return new ZipItem(srcFile, destinationFolder + destFile);
	}
	
	private class ZipItem {
		final String sourceFile;
		final String destinationFile;
		
		ZipItem(String sourceFile, String destinationFile) {
			this.sourceFile = sourceFile;
			this.destinationFile = destinationFile;
		}
	}

	/**
	 * Unzips the content of the unpack folder in the package to the same folder as the package is located in
	 * 
	 * Does not delete the original package file.
	 * 
	 * @param pkg
	 * @throws IOException 
	 * @throws ZipException 
	 */
	public static void unpackContent(File pkg) throws IOException {
		final String UNPACK = "unpack/";
		
		try(ZipFile archive = new ZipFile(pkg)) {
			File extractTo = pkg.getParentFile();
		
	        Enumeration<? extends ZipEntry> e = archive.entries();
	        
	        while (e.hasMoreElements()) {
	            ZipEntry entry = e.nextElement();
	            
	            String entryName = entry.getName();
	            
	            if (entryName.startsWith(UNPACK) && !entryName.equals(UNPACK)) {
	            	entryName = entryName.substring(UNPACK.length());
	            	
		            File file = new File(extractTo, entryName);
		            if (entry.isDirectory()) {
		            	if (!file.exists()) {
		            		file.mkdirs();
		            	}
		            } else {
		                if (!file.getParentFile().exists()) {
		                    file.getParentFile().mkdirs();
		                }
		
		                InputStream in = archive.getInputStream(entry);
		                
		                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
		
			                byte[] buffer = new byte[8192];
			                int read;
			
			                while (-1 != (read = in.read(buffer))) {
			                    out.write(buffer, 0, read);
			                }
			                in.close();
		                }
		            }
	            }
            }
		}
    }

	public static List<File> getFileList(File baseFolder, String includes, String excludes, boolean caseSensitive) {
		List<File> files = new ArrayList<File>();

		FileSet fileSet = Util.createFileSet(baseFolder, includes, excludes);
		fileSet.setDefaultexcludes(false);
		fileSet.setCaseSensitive(caseSensitive);

		for (String f : fileSet.getDirectoryScanner().getIncludedFiles()) {
			//f = f.replace(File.separatorChar, '/');
			files.add(new File(f));
		}

		return files;
	}
}