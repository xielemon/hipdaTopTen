import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import javax.swing.text.InternationalFormatter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.xml.internal.fastinfoset.algorithm.IntegerEncodingAlgorithm;
import com.sun.xml.internal.org.jvnet.fastinfoset.VocabularyApplicationData;

import jdk.internal.org.objectweb.asm.tree.IntInsnNode;
import sun.awt.FwDispatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;

public class hipda {

	private String url;

	URL hipdaURL;
	String today;

	hipda(String today) {

		try {
			hipdaURL = new URL("http://www.hi-pda.com/forum/index.php");
			this.today = today;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Use this method to login and get cookies
	 * 
	 * @return cookies
	 * @throws Exception
	 */
	public List login() throws Exception {
		// make form to post
		Map<String, String> map = new HashMap<String, String>();
		map.put("formhash", "1d41a5ce");
		map.put("referer", "http,//www.hi-pda.com/forum/index.php");
		map.put("loginfield", "username");
		// USE YOUR OWN USER NAME
		map.put("username", "");
		// USE YOUR OWN USER PASSWORD, U CAN GET IT THROUGH CAPTURE POST DATA
		// WHEN U LOGIN ON CHROME
		map.put("password", "");
		map.put("questionid", "0");
		map.put("answer", "");
		map.put("loginsubmit", "true");
		map.put("cookietime", "2592000");

		// encode map to string
		StringBuffer params = new StringBuffer();
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry element = (Map.Entry) it.next();
			params.append(element.getKey());
			params.append("=");
			params.append(element.getValue());
			params.append("&");
		}
		if (params.length() > 0) {
			params.deleteCharAt(params.length() - 1);
		}
		URL hipdaURL = new URL("http://www.hi-pda.com/forum/logging.php?action=login&loginsubmit=yes&inajax=1");
		HttpURLConnection hipdaURLConnection = (HttpURLConnection) hipdaURL.openConnection();
		// post
		hipdaURLConnection.setDoOutput(true);
		hipdaURLConnection.setDoInput(true);
		PrintWriter printWriter = new PrintWriter(hipdaURLConnection.getOutputStream());
		printWriter.write(params.toString());
		printWriter.flush();

		List<String> cookielist = hipdaURLConnection.getHeaderFields().get("Set-Cookie");

		return cookielist;
	}

	/**
	 * Use this method to get the content in Discovery
	 * 
	 * @param cookielist
	 * @param pagenum:which
	 *            page u want to
	 * @return
	 * @throws Exception
	 */
	public String requestDiscoveryContent(List<String> cookielist, int pagenum) throws Exception {

		URL url = new URL("http://www.hi-pda.com/forum/forumdisplay.php?fid=2&page=" + pagenum);
		HttpURLConnection huc = (HttpURLConnection) url.openConnection();

		// add cookie to request
		for (String cookie : cookielist) {
			huc.setRequestProperty("Cookie", cookie.split(";", 2)[0]);
		}

		huc.connect();
		String line = null;
		StringBuilder content = new StringBuilder();
		InputStream in = new GZIPInputStream(huc.getInputStream());
		BufferedReader bufw = new BufferedReader(new InputStreamReader(in, "gbk"));
		StringBuilder writeContent = new StringBuilder();
		int count = 0;
		while ((line = bufw.readLine()) != null) {
			String temp = null;
			Pattern titlePattern = Pattern.compile(" <span id=\"thread_(.*)span>");
			Matcher title = titlePattern.matcher(line.toString().subSequence(0, line.length()));

			Pattern numsPattern = Pattern.compile("<td class=\"nums\"><str(.*)td>");
			Matcher nums = numsPattern.matcher(line.toString().subSequence(0, line.length()));

			Pattern datePattern = Pattern.compile("<em>20(.*)em>");
			Matcher date = datePattern.matcher(line.toString().subSequence(0, line.length()));

			if (title.matches() == true) {
				//Delate the invalid XML character in title
				writeContent = writeContent.append(title.group().replaceAll("[\\x00-\\x08\\x0b-\\x0c\\x0e-\\x1f]", ""));

			}
			if (nums.matches() == true) {
				temp = nums.group();

				temp = temp.replaceAll("strong", "reply");
				temp = temp.replaceAll("em", "hit");
				temp = temp.replaceAll("<td class=\"nums\">", "");
				temp = temp.replaceAll("</td>", "");
				int i = temp.indexOf('/');
				int j = temp.lastIndexOf('/');

				writeContent = writeContent.insert(writeContent.length() - 7, temp);
				writeContent.append("\r\n");
			}
			if (date.matches() == true) {
				temp = date.group();
				temp = temp.replaceAll("em", "date");
				writeContent = writeContent.insert(writeContent.length() - 7, temp);
			}

			content.append(line + "\r\n");
		}

		return writeContent.toString();

	}

	/**
	 * Use this method to save data to xml file
	 * 
	 * @param FilePath
	 * @param pages
	 * @param hipda
	 * @throws Exception
	 */

	public void writeDiscoveryToXml(String FilePath, int pages, hipda hipda) throws Exception {
		Writer fw = null;

		try {
			FileOutputStream fos = new FileOutputStream(FilePath);
			fw = new OutputStreamWriter(fos, "UTF-8");
			fw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\r\n");
			fw.write("<discovery>" + "\r\n");
			for (int i = 1; i <= pages; i++) {
				System.out.println("Writting page " + i);
				fw.write(hipda.requestDiscoveryContent(hipda.login(), i));
			}
			fw.write("</discovery>" + "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			fw.close();
		}
	}

	/**
	 * Use this method to post the topTen to specific Thread
	 * 
	 * @param cookielist
	 * @param content
	 * @throws Exception
	 */

	public void postData(List<String> cookielist, String content) throws Exception {

		String formhash = null;
//POSTURL 是目标发帖地址，改变其中的tid参数即可
		URL posturl = new URL(
				"http://www.hi-pda.com/forum/post.php?action=reply&fid=57&tid=1892471&extra=&replysubmit=yes&infloat=yes&handlekey=fastpost&inajax=1");
		HttpURLConnection huc = (HttpURLConnection) posturl.openConnection();

		URL getFormHash = new URL("http://www.hi-pda.com/forum/viewthread.php?tid=1892286&extra=page%3D1");
		HttpURLConnection huc1 = (HttpURLConnection) getFormHash.openConnection();

		// add cookie to request
		for (String cookie : cookielist) {
			huc.setRequestProperty("Cookie", cookie.split(";", 2)[0]);
			huc1.setRequestProperty("Cookie", cookie.split(";", 2)[0]);
		}

		huc1.connect();
		String line = null;
		InputStream in = new GZIPInputStream(huc1.getInputStream());
		BufferedReader bufw = new BufferedReader(new InputStreamReader(in, "gbk"));
		while ((line = bufw.readLine()) != null) {
			// System.out.println(line);

			Pattern formHashPattern = Pattern.compile("^<a href=\"lo(.*)");
			Matcher formHash = formHashPattern.matcher(line);
			if (formHash.matches() == true) {
				line = formHash.group(0);
				System.out.println("..........Getting formhash..........");
				line = line.replaceAll("<a href=\"logging\\.php\\?action=logout\\&amp;formhash=", "");
				line = line.replaceAll("\">退出</a>", "");
				formhash = line;
			}
		}

		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("formhash", formhash);
		map.put("subject", "");
		map.put("usesig", "0");
		map.put("message", content);

		// encode map to string
		StringBuffer params = new StringBuffer();
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry element = (Map.Entry) it.next();
			params.append(element.getKey());
			params.append("=");
			params.append(java.net.URLEncoder.encode(element.getValue().toString(), "GBK"));
			params.append("&");
		}
		if (params.length() > 0) {
			params.deleteCharAt(params.length() - 1);
		}

		huc.setDoOutput(true);
		huc.setDoInput(true);
		huc.connect();
		PrintWriter printWriter = new PrintWriter(huc.getOutputStream());
		printWriter.write(params.toString());
		printWriter.flush();

		in = new GZIPInputStream(huc.getInputStream());
		bufw = new BufferedReader(new InputStreamReader(in, "gbk"));

		System.out.println(".........Success.........");
	}

	/**
	 * Used to sort map from high to low
	 * 
	 * @param oldMap
	 * @return
	 */

	public static Map sortMap(Map oldMap) {
		ArrayList<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(oldMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {

			@Override
			public int compare(Entry<java.lang.String, Integer> arg0, Entry<java.lang.String, Integer> arg1) {
				return arg1.getValue() - arg0.getValue();
			}
		});
		Map newMap = new LinkedHashMap();
		for (int i = 0; i < list.size(); i++) {
			newMap.put(list.get(i).getKey(), list.get(i).getValue());
		}
		return newMap;
	}

	/**
	 * Handle the xml file, and get the top ten file
	 * 
	 * @param FilePath
	 * @return
	 * @throws Exception
	 */

	public String getTopTen(String FilePath) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// 文档解析器
		DocumentBuilder db = dbf.newDocumentBuilder();

		Document document = db.parse(FilePath);
		Element e = document.getDocumentElement();

		NodeList nl = e.getElementsByTagName("span");
		Map<String, Integer> topTen = new LinkedHashMap<String, Integer>();

		for (int i = 0; i < nl.getLength(); i++) {
			NodeList nl2 = nl.item(i).getChildNodes();
			if (nl2.item(1).getTextContent().equals(this.today)) {
				topTen.put(nl.item(i).getAttributes().item(0).getTextContent(),
						new Integer(nl2.item(2).getTextContent()));
			}

		}
		System.out.println(".........Sorting.........");
		topTen = sortMap(topTen);

		// Get sorted thread(from high to low)
		Collection<String> topTenThread = null;
		topTenThread = topTen.keySet();
		String[] topTenThreadArr = topTenThread.toArray(new String[0]);

		StringBuilder postContent = new StringBuilder();
		postContent.append("[size=5][color=#0000ff]" + this.today + "Top ten[/color][/size]" + "\r\n" + "\r\n");
		for (int j = 0; j < 10; j++) {
			for (int i = 0; i < nl.getLength(); i++) {
				NodeList nl2 = nl.item(i).getChildNodes();
				if (topTenThreadArr[j].equals(nl.item(i).getAttributes().item(0).getTextContent())) {
					String temp = nl.item(i).getAttributes().item(0).getTextContent();// TITLE
					temp = temp.replaceAll("thread_", "");
					postContent
							.append("[url=http://www.hi-pda.com/forum/viewthread.php?tid=" + temp + "&extra=page%3D1]");// url
					postContent.append(nl2.item(0).getTextContent() + "[/url]" + "\r\n");// title
					postContent.append("回复数： " + nl2.item(2).getTextContent() + "\r\n" + "\r\n");
				}
			}
		}

		return postContent.toString();
	}

}

class hipdaDemo {
	public static void main(String[] args) {

		hipda test = new hipda("2016-6-24");
		try {

			test.writeDiscoveryToXml("test.xml", 10, test);

			test.postData(test.login(), test.getTopTen("test.xml"));
			// test.login();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
