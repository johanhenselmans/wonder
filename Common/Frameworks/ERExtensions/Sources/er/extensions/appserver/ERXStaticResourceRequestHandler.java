package er.extensions.appserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WORequestHandler;
import com.webobjects.appserver.WOResourceManager;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSBundle;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSNotificationCenter;

import er.extensions.foundation.ERXDictionaryUtilities;
import er.extensions.foundation.ERXProperties;

/**
 * Simple static resource request handler. Allows for better debugging 
 * and you can set the document root via the system property <code>WODocumentRoot</code>.
 * @author ak
 */
public class ERXStaticResourceRequestHandler extends WORequestHandler {
	
	private static Logger log = Logger.getLogger(ERXStaticResourceRequestHandler.class);
	
	private static WOApplication application = WOApplication.application();

	private String _documentRoot;

	public ERXStaticResourceRequestHandler() {
		_documentRoot = null;
	}

	protected WOResponse _generateResponseForInputStream(InputStream is, int length, String type) {
		WOResponse response = application.createResponseInContext(null);
		if (is != null) {
			if (length != 0) {
				response.setContentStream(is, 50*1024, length);
			}
		} else {
			response.setStatus(404);
		}
		if (type != null) {
			response.setHeader(type, "content-type");
		}
		if(length != 0) {
			response.setHeader("" + length, "content-length");
		}
		return response;
	}

	private String documentRoot() {
		if (_documentRoot == null) {
			_documentRoot = ERXProperties.stringForKey("WODocumentRoot");
			if(_documentRoot == null) {
				NSBundle bundle = NSBundle.bundleForName("JavaWebObjects");
				NSDictionary dict = ERXDictionaryUtilities.dictionaryFromPropertyList("WebServerConfig", bundle);
				_documentRoot = (String) dict.objectForKey("DocumentRoot");
			}
		}
		return _documentRoot;
	}

	public WOResponse handleRequest(WORequest request) {
		WOResponse response = null;
		FileInputStream is = null;
		int length = 0;
		String contentType = null;
		String uri = request.uri();
		if (uri.charAt(0) == '/') {
			WOResourceManager rm = application.resourceManager();
			String documentRoot = documentRoot();
			File file = null;
			StringBuffer sb = new StringBuffer(documentRoot.length() + uri.length());
			String wodataKey = request.stringFormValueForKey("wodata");
			if(uri.startsWith("/cgi-bin") && wodataKey != null) {
				uri = wodataKey;
				if(uri.startsWith("file:")) {
					// remove file:/
					uri = uri.substring(5);
				} else {
					
				}
			} else {
				int index = uri.indexOf("/wodata=");

				if(index >= 0) {
					uri = uri.substring(index+"/wodata=".length());
				} else {
					sb.append(documentRoot);
				}
			}
			sb.append(uri);
			String path = sb.toString();
			try {
				path = path.replace('+', ' ');
				path = path.replaceAll("\\?.*", "");
				file = new File(path);
				length = (int) file.length();
				is = new FileInputStream(file);
				
				contentType = rm.contentTypeForResourceNamed(path);
				log.debug("Reading file '" + file + "' for uri: " + uri);
			} catch (IOException ex) {
				if (!uri.equalsIgnoreCase("/favicon.ico")) {
					log.info("Unable to get contents of file '" + file + "' for uri: " + uri);
				}
			}
		} else {
			log.error("Can't fetch relative path: " + uri);
		}
		response = _generateResponseForInputStream(is, length, contentType);
		NSNotificationCenter.defaultCenter().postNotification(WORequestHandler.DidHandleRequestNotification, response);
		response._finalizeInContext(null);
		return response;
	}
}