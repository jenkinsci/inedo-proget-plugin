package com.inedo.proget.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.inedo.proget.domain.PackageMetadata;

class ProGetPackageUtils
{
	private File sourceFolder;
	private File zipFile;
	private ZipOutputStream zos = null;
	
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
		
		sb.append("{").append(newLine);
		sb.append("\t\"group\": \"").append(metadata.group).append("\"").append(newLine);
		sb.append("\t\"name\": \"").append(metadata.name).append("\"").append(newLine);
		sb.append("\t\"version\": \"").append(metadata.version).append("\"").append(newLine);
		if (isProvided(metadata.title)) {
			sb.append("\t\"title\": \"").append(metadata.title).append("\"").append(newLine);
		}
		if (isProvided(metadata.icon)) {
			sb.append("\t\"icon\": \"").append(metadata.icon).append("\"").append(newLine);
		}
		if (isProvided(metadata.description)) {
			sb.append("\t\"description\": \"").append(metadata.description).append("\"").append(newLine);
		}
		if (isProvided(metadata.dependencies)) {
			sb.append("\t\"dependencies\": \"").append(metadata.dependencies).append("\"").append(newLine);
		}
		sb.append("}");
		
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
}