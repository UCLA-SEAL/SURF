package edu.mwdb.project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class PrintAuthorVector extends PrintKeywordVector
{

	/**
	 * @param args
	 */

	public static void main(String[] args)
	{
		Utility utilityObj = new Utility();
		try 
		{
			Connection con = utilityObj.getDBConnection();
			Map<String,Float> rowData = new HashMap<String, Float>();

			Statement stmt = con.createStatement();
			//String personId = "1632672";
			String personId = args[0];

			// Creation of a Index Directory.
			StandardAnalyzer docAnalyzer = new StandardAnalyzer(Version.LUCENE_36);
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_36,docAnalyzer);
			Directory indexDirectory = new RAMDirectory();
			IndexWriter indexWr = new IndexWriter(indexDirectory, indexConfig);

			String doc="";

			String query_authorid = 
					"select p.abstract,p.year/year(current_date) as weight from papers p join " +  
							"(select distinct w.paperid from " + 
							"authors a join writtenby w where a.personid = w.personid and a.personid = " + personId  + 
							" order by paperid) T1 on p.paperid = T1.paperid where p.abstract != \"\"";

			ResultSet rs = stmt.executeQuery(query_authorid);
			while (rs.next())
			{
				rowData.put(rs.getString("abstract"), rs.getFloat("weight"));
				// Adding a field 'doc' from the abstract to create an indexed document.
				doc = rs.getString("abstract");
				Document document = new Document();
				document.add(new Field("doc", doc, Field.Store.YES,Field.Index.ANALYZED));
				indexWr.addDocument(document);
				indexWr.commit();
			}

			CharArraySet stopWordsCharArrSet;
			TokenStream docStream;
			TokenStream keywords;
			Map<String,Float> termFreq = new HashMap<String, Float>();
			KeywordConfig config;
			List<KeywordConfig> configList = new ArrayList<KeywordConfig>();
			float weightedNoOfWords = 0;

			for (Map.Entry<String, Float> abs : rowData.entrySet()) 
			{
				String[] rowDataArr = abs.getKey().split("[ ]+");
				weightedNoOfWords += abs.getValue()*rowDataArr.length;

				stopWordsCharArrSet = new CharArraySet(Version.LUCENE_36, utilityObj.createStopWordsSet(), true);
				docStream = new StandardTokenizer(Version.LUCENE_36, new StringReader(abs.getKey()));
				keywords = new StopFilter(Version.LUCENE_36, docStream ,stopWordsCharArrSet);

				termFreq = utilityObj.createTF(keywords, abs.getKey(), abs.getValue());

				for(Map.Entry<String, Float> keys : termFreq.entrySet())
				{
					config = new KeywordConfig();
					config.setKeyword(keys.getKey());
					config.setWeightedFreq(keys.getValue());
					configList.add(config);
				}
			}
			Map<String,Float> termFinalFreq = new HashMap<String, Float>();

			for (KeywordConfig itr: configList){
				Float val = termFinalFreq.get(itr.getKeyword());
				termFinalFreq.put(itr.getKeyword(), (val == null) ? itr.getWeightedFreq() : (val + itr.getWeightedFreq()));
			}
			
			for(Map.Entry<String, Float> k: termFinalFreq.entrySet()){
				termFinalFreq.put(k.getKey(), k.getValue()/weightedNoOfWords);
			}
			
			//Calling the method createTFIDF to create the TF and the TF-IDF vector output
			utilityObj.createTFIDF(rowData.size(),indexDirectory, termFinalFreq,args[1]);

			con.close();

		}

		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
}
