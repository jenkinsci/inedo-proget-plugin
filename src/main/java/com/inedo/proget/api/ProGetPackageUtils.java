package com.inedo.proget.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.inedo.proget.domain.PackageMetadata;
import com.inedo.proget.domain.ProGetPackage;

public class ProGetPackageUtils
{
	private List<String> fileList;
	private File sourceFolder;
	

	public File create(File sourceFolder, PackageMetadata metadata) {
		File zipFile = new File(sourceFolder, metadata.name.replace(" ",  "") + ".unpack");
		generateFileList(sourceFolder);
		zipIt(zipFile);

		return zipFile;
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
			//remember close it
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