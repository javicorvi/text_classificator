package es.bsc.inb.limtox.services;


import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.objectbank.ObjectBank;
import edu.stanford.nlp.util.ErasureUtils;
@Service
class ClassifierServiceImpl implements ClassifierService {

	static final Logger classifierLog = Logger.getLogger("classifierLog");
	
	public void classify(String properitesParametersPath) {
		try {
			classifierLog.info("Classify articles with properties :  " +  properitesParametersPath);
			Properties propertiesParameters = this.loadPropertiesParameters(properitesParametersPath);
			classifierLog.info("Classify articles with the model  :  " +  propertiesParameters.getProperty("classificatorModel"));
			classifierLog.info("Input directory with the articles to classify : " + propertiesParameters.getProperty("inputDirectory"));
			classifierLog.info("Outup directory with the relevant articles : " + propertiesParameters.getProperty("outputDirectory"));
			classifierLog.info("Relevant articles label: " + propertiesParameters.getProperty("relevantLabel"));
			String classificatorModel = propertiesParameters.getProperty("classificatorModel");
			String inputDirectoryPath = propertiesParameters.getProperty("inputDirectory");
			String outputDirectoryPath = propertiesParameters.getProperty("outputDirectory");
			String relevantLabel = propertiesParameters.getProperty("relevantLabel");
			
			if (!Files.isDirectory(Paths.get(outputDirectoryPath))) {
				
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(FileUtils.readFileToByteArray(new File(classificatorModel)));
		    ObjectInputStream ois = new ObjectInputStream(bais);
		    LinearClassifier<String,String> cdc = ErasureUtils.<LinearClassifier<String,String>>uncheckedCast(ois.readObject());
		    ois.close();
			
		    /*ByteArrayInputStream bais2 = new ByteArrayInputStream(FileUtils.readFileToByteArray(new File(classificatorModel+".ser2")));
		    ObjectInputStream ois2 = new ObjectInputStream(bais2);
		    ColumnDataClassifier cdc2 = ColumnDataClassifier.getClassifier(ois2);
		    ois2.close();*/
		    
		    File outputDirectory = new File(outputDirectoryPath);
		    if(!outputDirectory.exists())
		    	outputDirectory.mkdirs();
			if (Files.isDirectory(Paths.get(inputDirectoryPath))) {
				File inputDirectory = new File(inputDirectoryPath);
				File[] files =  inputDirectory.listFiles();
				for (File file_to_classify : files) {
					this.classify(file_to_classify, cdc, outputDirectory, relevantLabel);
				}
			}
		}  catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Classify 
	 * @param file_to_classify
	 */
	 private void classify(File file_to_classify, LinearClassifier<String,String> lc , File outputDirectory, String relevantLabel) {
		 File fout = new File(outputDirectory.getAbsoluteFile() + File.separator + file_to_classify.getName());
		 FileOutputStream fos;
		 try {
			 fos = new FileOutputStream(fout);
			 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			 ColumnDataClassifier cdc = new ColumnDataClassifier(file_to_classify.getAbsolutePath());
			 for (String line : ObjectBank.getLineIterator(file_to_classify.getAbsolutePath(), "utf-8")) {
				 Datum<String,String> d = cdc.makeDatumFromLine(line);
				 //System.out.printf("%s  ==>  %s (%.4f)%n", line, lc.classOf(d), lc.scoresOf(d).getCount(lc.classOf(d)));
				 //if(lc.classOf(d)!=null && lc.classOf(d).equals(relevantLabel)) {
					 bw.write(lc.scoresOf(d).getCount(lc.classOf(d)) + "\t" + line);
					 bw.newLine();
				 //}
			 }
			 bw.close(); 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
