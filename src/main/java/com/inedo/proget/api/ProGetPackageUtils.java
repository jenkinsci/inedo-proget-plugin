package com.inedo.proget.api;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.inedo.proget.domain.PackageMetadata;

public class ProGetPackageUtils
{
	private List<ZipItem> fileList;
	private File sourceFolder;
	

	public File createPackage(File sourceFolder, PackageMetadata metadata) throws IOException {		
		this.fileList = new ArrayList<ZipItem>();
		this.sourceFolder = sourceFolder;
		
		generateFileList(sourceFolder, "unpack/");
		
		File metaFile = createMetadataFile(metadata);
		File zipFile = new File(sourceFolder, metadata.name.replace(" ",  "") + ".unpack");
		fileList.add(generateZipEntry(metaFile, ""));
		
		zipIt(zipFile, "unpack/");
		metaFile.delete();

		return zipFile;
	}

	private File createMetadataFile(PackageMetadata metadata) throws IOException {
		File file = new File(sourceFolder, "upack.json");
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
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(sb.toString());
		}
		
		return file;
	}

	private boolean isProvided(String value) {
		return value !=null && !value.isEmpty();
	}

	private void zipIt(File zipFile, String zipFolder) {

		byte[] buffer = new byte[1024];

		try{

			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);

			System.out.println("Output to Zip : " + zipFile);

			for(ZipItem entry : this.fileList) {

				System.out.println("File Added : " + entry.sourceFile);
				ZipEntry ze = new ZipEntry(entry.destinationFile);
				zos.putNextEntry(ze);

				FileInputStream in = new FileInputStream(new File(entry.sourceFile));

				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}

				in.close();
			}

			zos.closeEntry();
			zos.close();

			System.out.println("Done");
		}catch(IOException ex){
			ex.printStackTrace();   
		}
	}

	/**
	 * Traverse a directory and get all files,
	 * and add the file into fileList  
	 * @param node file or directory
	 */
	private void generateFileList(File node, String destinationFolder){

		//add file only
		if(node.isFile()){
			fileList.add(generateZipEntry(node, destinationFolder));
		}

		if(node.isDirectory()){
			String[] subNote = node.list();
			for(String filename : subNote){
				generateFileList(new File(node, filename), destinationFolder);
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