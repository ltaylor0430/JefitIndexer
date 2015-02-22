package com.lct.JefitIndexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 

public class JefitMainIndexer 
{
	private static final Logger logger = LoggerFactory.getLogger(JefitMainIndexer.class);
	public static String Base_url = "http://www.jefit.com";
	private final String JEFIT_URL="http://www.jefit.com/exercises/bodypart.php?id=11&exercises=All&All=0&Bands=0&Bench=0&Dumbbell=0&EZBar=0&Kettlebell=0&MachineStrength=0&MachineCardio=0&Barbell=0&BodyOnly=0&ExerciseBall=0&FoamRoll=0&PullBar=0&WeightPlate=0&Other=0&Strength=0&Stretching=0&Powerlifting=0&OlympicWeightLifting=0&Beginner=0&Intermediate=0&Expert=0&page=%d";
    public static void main( String[] args )
    {
        //http://www.jefit.com/exercises/bodypart.php?id=11&exercises=All
 
        
        		//No results found break
        		/*<table width="100%" cellspacing="0" id="hor-minimalist_3">
		<tbody><tr>
			<td>
				
No result found!						<table width="100%" cellspacing="0"></table>
			</td>
		</tr>
	</tbody></table>*/		
       
       JefitMainIndexer a = new JefitMainIndexer();
       a.start();
    }
    
    public void start() {
    	PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    	CloseableHttpClient httpClient = HttpClients.custom()
    	        .setConnectionManager(cm)
    	        .build();

    	// URIs to perform GETs on
    	int pages = 135;
      	ExecutorService ex = Executors.newFixedThreadPool(5);
    	 
    	// create a thread for each URI
      	List<GetThread> myThreadList = new ArrayList<GetThread>();
    	GetThread[] threads = new GetThread[pages];
    	for (int i = 0; i < threads.length; i++) {
    		String uriToGet = String.format(JEFIT_URL,i+1);
    	    if (logger.isDebugEnabled()) {
    	    	logger.debug("Fetching URL:" + uriToGet);
    	    }
    		HttpGet httpget = new HttpGet(uriToGet);
    	    
    	    threads[i] = new GetThread(httpClient, httpget);
    	    myThreadList.add(threads[i]);
    	}
    	
    BufferedWriter bos = null;
    try {
    	if (logger.isDebugEnabled()) {
    		logger.debug("Starting Threads " + System.currentTimeMillis());
    	}
		List<Future<String>> futures = ex.invokeAll(myThreadList);
		
        ex.shutdown();
        ex.awaitTermination(30, TimeUnit.MINUTES);
        Iterator<Future<String>> results = futures.listIterator();
        //TODO: Get path to save csv file from parameter
        File outputFile = new File("/Users/Diggy/jeffitData.csv");
        if(outputFile.exists()) {
        	outputFile.delete();
        }
    	outputFile.createNewFile();
    	bos = new BufferedWriter(new FileWriter(outputFile));

        while (results.hasNext()) {
        	Future<String> s = results.next();
        	String data = s.get();
        	bos.write(data);
        	System.out.println("[" + data +"]");
        }
    } catch (InterruptedException e) {
		logger.error("",e);
	} catch (ExecutionException e) {
		logger.error("",e);
	} catch (Exception e) {
		logger.error("",e);
	}
    finally {
    	try {
			bos.close();
		} catch (IOException e) {
			logger.error("Closing writer exception",e);
		}
    }
    //exit
    System.exit(0);
    
    }
    
    //Callable Thread -get the work done!
    static class GetThread implements Callable<String> {
    	private static final Logger logger = LoggerFactory.getLogger(GetThread.class);
    	
    	private final CloseableHttpClient httpClient;
        private final HttpContext context;
        private final HttpGet httpget;

        public GetThread(CloseableHttpClient httpClient, HttpGet httpget) {
            this.httpClient = httpClient;
            this.context = HttpClientContext.create();
            this.httpget = httpget;
        }

		public String call() throws Exception {
			 CloseableHttpResponse response = null;
			 StringBuilder s = new StringBuilder();
			  try {
		              response = httpClient.execute(
		                    httpget, context);
		                 	Document doc = handleResponse(response);
		                 	Elements top = doc.select("#hor-minimalist_3 > tbody >tr >td >table >tbody > tr > td:lt(3)");
		                 	for (int i =0; i < top.size();i++)
		                 	{
		                 		
		                 		
		                 	 
		                 		Element trRoot = top.get(i);
		                 		Elements title = trRoot.select("a[title]:first-child");
		                 		if (title.size() == 0) {
		                 			
		                 			continue;
		                 		}
		                 		
		                 		String name = title.get(0).text();
		                 		if (!name.isEmpty())
		                 		{
		                 			if (i!=0) {
			                 			s.append("\n");
			                 		}
		                 		} else {
		                 			continue;
		                 		}
		                 		s.append(String.format("\"%s\"", name));
		                 		
		                 		
		                 		Elements pElements= trRoot.select("p");
		                 		if (pElements.size()>0) {
		                 			s.append(",");
		                 		}
		                 	 
		                 		for (int h=0; h <pElements.size();h++) {
		                 			if (h !=0) {
		                 				s.append(",");
		                 			}
		                 			String dirtyText = pElements.get(h).text();
		                 			String cleanup = dirtyText;
		                 			if (dirtyText.indexOf(":") != -1) {
		                 			 String[] splitTxt =dirtyText.split(":");
		                 			 cleanup = splitTxt[1];
		                 			}
		                 			s.append(String.format("\"%s\"",cleanup));
		                 		}
		                   
		                 		
		                 		}
		                 		
		                 	 
		                 	
		                 return s.toString();
		               
		            
		            
		        } catch (ClientProtocolException ex) {
		        	 if (logger.isDebugEnabled()) {
		        		 logger.debug("ClientProtocol Exception",ex);
		        	  }
		        	return "";
		            // Handle protocol errors
		        } catch (IOException ex) {
		        	 if (logger.isDebugEnabled()) {     
		        		 logger.debug("IO Exception",ex);
		        	  }
		          return "";
		        }
			  finally {
				  if (response != null)
					  response.close();
	        }
		}
		public static Document handleResponse(final HttpResponse response) throws IOException {
	      Document d= Jsoup.parse(response.getEntity().getContent(),"UTF-8",JefitMainIndexer.Base_url);
	      return d;
	    }
         
    }
    
}
