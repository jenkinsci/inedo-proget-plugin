package com.inedo.proget.api;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.inedo.proget.domain.PackageMetadata;

public class ProGetPackageUtils
{
	private List<String> fileList;
	private File sourceFolder;
	

	public File create(File sourceFolder, PackageMetadata metadata) throws IOException {		
		File zipFile = new File(sourceFolder, metadata.name.replace(" ",  "") + ".unpack");
		
		generateFileList(sourceFolder);
		File metaFile = createMetadataFile(metadata);
		zipIt(zipFile);
		metaFile.delete();

		return zipFile;
	}

	private File createMetadataFile(PackageMetadata metadata) throws IOException {
		File file = new File(sourceFolder, "upack.json");
		String newLine = System.getProperty("line.separator");;
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("{").append(newLine);
		sb.append("\"group\": \"").append(metadata.group).append("\"").append(newLine);
		sb.append("\"name\": \"").append(metadata.name).append("\"").append(newLine);
		sb.append("\"version\": \"").append(metadata.version).append("\"").append(newLine);
		sb.append("\"version\": \"").append(metadata.version).append("\"").append(newLine);
		if (isProvided(metadata.title)) {
			sb.append("\"title\": \"").append(metadata.title).append("\"").append(newLine);
		}
		if (isProvided(metadata.icon)) {
			sb.append("\"icon\": \"").append(metadata.icon).append("\"").append(newLine);
		}
		if (isProvided(metadata.description)) {
			sb.append("\"description\": \"").append(metadata.description).append("\"").append(newLine);
		}
		if (isProvided(metadata.dependencies)) {
			sb.append("\"dependencies\": \"").append(metadata.dependencies).append("\"").append(newLine);
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

	private void zipIt(File zipFile) {

		byte[] buffer = new byte[1024];

		try{

			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);

			System.out.println("Output to Zip : " + zipFile);

			for(String file : this.fileList) {

				System.out.println("File Added : " + file);
				ZipEntry ze = new ZipEntry(file);
				zos.putNextEntry(ze);

				FileInputStream in = new FileInputStream(new File(sourceFolder, file));

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
	private void generateFileList(File node){

		//add file only
		if(node.isFile()){
			fileList.add(generateZipEntry(node));
		}

		if(node.isDirectory()){
			String[] subNote = node.list();
			for(String filename : subNote){
				generateFileList(new File(node, filename));
			}
		}

	}

	/**
	 * Format the file path for zip
	 * @param file file path
	 * @return Formatted file path
	 */
	private String generateZipEntry(File file) {
		String srcFile = file.getAbsolutePath();
		String srcFolder = sourceFolder.getAbsolutePath();
		
		return srcFile.substring(srcFolder.length() + 1, srcFile.length());
	}
}