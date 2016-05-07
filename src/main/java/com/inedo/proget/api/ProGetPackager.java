package com.inedo.proget.api;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.types.FileSet;

import com.inedo.proget.domain.PackageMetadata;
import com.inedo.proget.jenkins.UploadPackageBuilder;

import hudson.Util;

public class ProGetPackager
{
	private static final String PACKAGE = "package" + File.separatorChar;
	
	private File sourceFolder;
	private File zipFile;
	private ZipOutputStream zos = null;
		
	public File createPackage(File baseFolder, List<ZipItem> files, PackageMetadata metadata) throws IOException {		
		FileOutputStream fos = null;
		
		this.sourceFolder = baseFolder;
		this.zipFile = new File(sourceFolder, metadata.packageName.replace(" ",  "") + ".unpack");
		
		try {
			fos = new FileOutputStream(zipFile);
			zos = new ZipOutputStream(fos);

			appendMetadata(metadata);
			appendFiles(files, PACKAGE);
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
		sb.append(newLine).append("\t\"name\": \"").append(metadata.packageName).append("\",");
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
	
		if (metadata.dependencies.size() > 0) {
			sb.append(",").append(newLine).append("\t\"dependencies\": [");
			
			for (int i = 0; i < metadata.dependencies.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				
				sb.append(" \"").append(metadata.dependencies.get(i)).append("\"");
			}
			
			sb.append(" ]");
		}
		
		for (Entry<String, String> entry : metadata.extendedAttributes.entrySet()) {
			sb.append(",").append(newLine).append("\t\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
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
	
	private void appendFiles(List<ZipItem> files, String destinationFolder) throws IOException {
		for (ZipItem entry : files)	{
			File source = new File(sourceFolder, entry.getSourceFile());
			String destination = destinationFolder + entry.getDestinationFile();
			
			appendFile(source, destination);
		}
	}

	private void appendFile(File file, String destination) throws IOException, FileNotFoundException {
		ZipEntry ze = new ZipEntry(destination);
		byte[] buffer = new byte[1024];
		int len;
		
		zos.putNextEntry(ze);

		try (FileInputStream in = new FileInputStream(file)) {
			while ((len = in.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}
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
		try(ZipFile archive = new ZipFile(pkg)) {
			File extractTo = pkg.getParentFile();
		
	        Enumeration<? extends ZipEntry> e = archive.entries();
	        
	        while (e.hasMoreElements()) {
	            ZipEntry entry = e.nextElement();
	            
	            String entryName = new File(entry.getName()).getPath();
	            if (entryName.equals(PACKAGE)) {
	            	continue;
	            }
	            
	            if (entryName.startsWith(PACKAGE) && !entryName.equals(PACKAGE)) {
	            	entryName = entryName.substring(PACKAGE.length());
	            }
	            
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

	public List<ZipItem> getFileList(File baseFolder, UploadPackageBuilder settings) {
		List<ZipItem> files = new ArrayList<ZipItem>();

		FileSet fileSet = Util.createFileSet(baseFolder, removeTrimFolderMarker(settings.getArtifacts()), settings.getExcludes());
		fileSet.setDefaultexcludes(settings.isDefaultExcludes());
		fileSet.setCaseSensitive(settings.isCaseSensitive());

		for (String f : fileSet.getDirectoryScanner().getIncludedFiles()) {
			//TODO required? f.replace(File.separatorChar, '/')
			files.add(new ZipItem(f));
		}

		String[] includes = settings.getArtifacts().split(",");
		for (String prefix : includes) {
			prefix = prefix.trim();
			
			if (!prefix.startsWith("[")) {
				continue;
			}
			
			int index = prefix.indexOf("]");
			if (index < 0) {
				continue;
			}
			
			prefix = prefix.substring(1, index);
			prefix = prefix.replace("/", File.separator);
			
			if (!prefix.endsWith(File.separator)) {
				prefix += File.separator;
			}
				
			for (ZipItem file : files) {
				if (file.getSourceFile().startsWith(prefix)) {
					file.setDesinationFile(file.getSourceFile().substring(prefix.length()));
				}
			}
		}
		
		return files;
	}

	private String removeTrimFolderMarker(String pattern) {
		return pattern.replace("[", "").replace("]", "");
	}
	
	public class ZipItem {
		private final String sourceFile;
		private String destinationFile;
		
		ZipItem(String sourceFile) {
			this.sourceFile = sourceFile;
			this.destinationFile = sourceFile;
		}
		
		ZipItem(String sourceFile, String destinationFile) {
			this.sourceFile = sourceFile;
			this.destinationFile = destinationFile;
		}

		public String getSourceFile() {
			return sourceFile;
		}
		
		public String getDestinationFile() {
			return destinationFile;
		}
		
		public void setDesinationFile(String value) {
			this.destinationFile = value;
		}
	}
}