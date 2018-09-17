package es.bsc.inb.limtox.services;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.objectbank.ObjectBank;
@Service
class ClassifierServiceImpl implements ClassifierService {

	static final Logger classifierLog = Logger.getLogger("classifierLog");
	
	public void classify(String propertiesParametersPath) {
		try {
			classifierLog.info("Classify articles with properties :  " +  propertiesParametersPath);
			Properties propertiesParameters = this.loadPropertiesParameters(propertiesParametersPath);
			classifierLog.info("Classify articles with the model  :  " +  propertiesParameters.getProperty("classificatorModel"));
			classifierLog.info("Input directory with the articles to classify : " + propertiesParameters.getProperty("inputDirectory"));
			classifierLog.info("Outup directory with the relevant articles : " + propertiesParameters.getProperty("outputDirectory"));
			classifierLog.info("Relevant articles label: " + propertiesParameters.getProperty("relevantLabel"));
			
			String classificatorModel = propertiesParameters.getProperty("classificatorModel");
			String inputDirectoryPath = propertiesParameters.getProperty("inputDirectory");
			String outputDirectoryPath = propertiesParameters.getProperty("outputDirectory");
			String relevantLabel = propertiesParameters.getProperty("relevantLabel");
			
			//Levantar ColumnDataClassifier
			ByteArrayInputStream bais = new ByteArrayInputStream(FileUtils.readFileToByteArray(new File(classificatorModel)));
		    ObjectInputStream ois = new ObjectInputStream(bais);
		    ColumnDataClassifier cdc = ColumnDataClassifier.getClassifier(ois);
		    ois.close();
			
			/*ByteArrayInputStream bais = new ByteArrayInputStream(FileUtils.readFileToByteArray(new File(classificatorModel)));
		    ObjectInputStream ois = new ObjectInputStream(bais);
		    Classifier<String,String> lc = ErasureUtils.<Classifier<String,String>>uncheckedCast(ois.readObject());
		    ois.close();*/
			
		    /*ByteArrayInputStream bais2 = new ByteArrayInputStream(FileUtils.readFileToByteArray(new File(classificatorModel+".ser2")));
		    ObjectInputStream ois2 = new ObjectInputStream(bais2);
		    ColumnDataClassifier cdc2 = ColumnDataClassifier.getClassifier(ois2);
		    ois2.close();*/
		    
		    //ColumnDataClassifier cdc = new ColumnDataClassifier(propertiesClassificatorModelParameters);
		    File outputDirectory = new File(outputDirectoryPath);
		    if(!outputDirectory.exists())
		    	outputDirectory.mkdirs();
			
		    List<String> filesProcessed = readFilesProcessed(outputDirectoryPath); 
		    
		    BufferedWriter filesPrecessedWriter = new BufferedWriter(new FileWriter(outputDirectoryPath + File.separator + "list_files_processed.dat", true));
		    if (Files.isDirectory(Paths.get(inputDirectoryPath))) {
				File inputDirectory = new File(inputDirectoryPath);
				File[] files =  inputDirectory.listFiles();
				for (File file_to_classify : files) {
					if(file_to_classify.getName().endsWith(".txt") && filesProcessed!=null && !filesProcessed.contains(file_to_classify.getName())){
						Boolean result = this.classify(file_to_classify, cdc, outputDirectory, relevantLabel);
						if(result) {
							filesPrecessedWriter.write(file_to_classify.getName()+"\n");
							filesPrecessedWriter.flush();
						}
					}
				}
			}
		    filesPrecessedWriter.close();
		}  catch (Exception e) {
			classifierLog.error("Generic error in the classification step",e);
		}
	}

	private List<String> readFilesProcessed(String outputDirectoryPath) {
		List<String> files_processed = new ArrayList<String>();
		try {
			if(Files.isRegularFile(Paths.get(outputDirectoryPath + File.separator + "list_files_processed.dat"))) {
				FileReader fr = new FileReader(outputDirectoryPath + File.separator + "list_files_processed.dat");
			    BufferedReader br = new BufferedReader(fr);
			    
			    String sCurrentLine;
			    while ((sCurrentLine = br.readLine()) != null) {
			    	files_processed.add(sCurrentLine);
				}
			    br.close();
			    fr.close();
			    return files_processed;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return files_processed;
	}
	
	/**
	 * Classify 
	 * @param file_to_classify
	 */
	 private Boolean classify(File file_to_classify,  ColumnDataClassifier cdc , File outputDirectory, String relevantLabel) {
		 File fout = new File(outputDirectory.getAbsoluteFile() + File.separator + file_to_classify.getName());
		 FileOutputStream fos;
		 try {
			 fos = new FileOutputStream(fout);
			 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			 classifierLog.info(" File to classify " + file_to_classify.getAbsolutePath());
			 for (String line : ObjectBank.getLineIterator(file_to_classify.getAbsolutePath(), "utf-8")) {
				 String[] data = line.split("\t");
				 String line_to_classify = "\t" + data[0] + "\t" + data[2];
				 Datum<String,String> d = cdc.makeDatumFromLine(line_to_classify);
				 if(cdc.classOf(d)!=null && cdc.classOf(d).equals(relevantLabel)) {
					 //for now are the same, because in position:
					 // data[0] is the identificator
					 // data[1] is the Title (blocks) or the section name (sentences) 
					 // data[2] is the text to that was classified
					 bw.write(relevantLabel + "\t" + cdc.scoresOf(d).getCount(cdc.classOf(d)) + "\t" + data[0] + "\t" + data[1] + "\t" + data[2]);
					 /*if(sectionIndex!=-1) {
						 bw.write(relevantLabel + "\t" + cdc.scoresOf(d).getCount(cdc.classOf(d)) + "\t" + data[0] + "\t" + data[1] + "\t" + data[2]);
					 }else {
						 bw.write(relevantLabel + "\t" + cdc.scoresOf(d).getCount(cdc.classOf(d)) + "\t" + data[0] + "\t" + data[1] + "\t" + data[2]);
					 }*/
					 bw.newLine();
				}else {
					 System.out.print("");
				 }
			 }
			 bw.close(); 
			 return true;
		} catch (FileNotFoundException e) {
			classifierLog.error(" File not Found " + file_to_classify.getAbsolutePath(), e);
			return false;
		} catch (IOException e) {
			classifierLog.error(" IOException " + file_to_classify.getAbsolutePath(), e);
			return false;
		}
	 }


	/**
	  * Load Properties
	  * @param properitesParametersPath
	  */
	 public Properties loadPropertiesParameters(String properitesParametersPath) {
		 Properties prop = new Properties();
		 InputStream input = null;
		 try {
			 input = new FileInputStream(properitesParametersPath);
			 // load a properties file
			 prop.load(input);
			 return prop;
		 } catch (IOException ex) {
			 ex.printStackTrace();
		 } finally {
			 if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			 }
		}
		return null;
	 }
	 
}
