package er.coolcomponents;

import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

import er.extensions.appserver.ERXApplication;
import er.extensions.appserver.ERXResponseRewriter;
import er.extensions.appserver.ERXSession;
import er.extensions.components.ERXStatelessComponent;
import er.extensions.formatters.ERXTimestampFormatter;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.localization.ERXLocalizer;

/**
 * Wrapper around http://home.jongsma.org/software/js/datepicker
 * 
 * Because many options take a date-time with the format of YYYY-mm-DD HH:MM there is a utility method:
 * ERMDatePicker.optionsStringForTimestamp(NSTimestamp ts) that will return a correctly formatted
 * string for a given NSTimestamp.
 * DateTimePicker should use locale date and time notation as in http://en.wikipedia.org/wiki/Date_and_time_notation_by_country
 * and the http://en.wikipedia.org/wiki/Common_Locale_Data_Repository but we have not gotten around how to do this. Yet.
 * 
 * @binding dateIn an NSTimestamp supplying the value for the field (required)
 * @binding cssFile name of the css file (defaults to anytime.css)
 * @binding cssFramework name of the framework containing the css file (defaults to ERModernDirectToWeb)
 * @binding dateformat string containing the date format for the field
 * @binding injectStylesheet choose whether to dynamically inject the anytime at component load. 
 * 			if used in a ajax loaded component, it may be safer to load this manually.
 * 
 * See datepicker documentation for following optional values:
 * 
 * @binding options string - an object with name/value pairs of additional options
 * @binding datePicker boolean - a boolean value determining whether to display the date picker. Defaults to true.
 * @binding timePicker boolean - a boolean value determining whether to display the time picker. Defaults to true in java, false in javascript.
 * @binding timePickerAdjacent boolean - a boolean value determining whether to display the time picker next to the date picker (true) or under it (false, default).
 * @binding use24hrs boolean - a boolean value determining whether to display the time in AM/PM or 24 hour notation. Defaults to true in java, false in javascript.
 * @binding locale string - a locale string that determines which language and date format to use. Defaults to "en_US".
 * @binding onSelect string - A function to call when the user selects a date. The date object is passed as a parameter.
 * @binding onHover string - A function to call when the active date changes (when using keyboard navigation). The date object is passed as a parameter.
 * Other options currently undocumented
 * 
 * @author johanhenselmans, based on work of davidleber
 *
 */
public class CCDateTimePicker extends ERXStatelessComponent {
	
	static final Logger log = Logger.getLogger(CCDateTimePicker.class);

	public static final String FRAMEWORK_NAME = "ERCoolComponents";
	public static final String CSS_FILENAME = "DateTime/datepicker.css";
	
	private String _elementID;
	private String _openScript;
	private String _createScript;
	
    public CCDateTimePicker(WOContext context) {
        super(context);
    }
    
    @Override
    public void reset() {
    	super.reset();
    	_elementID = null;
    	_openScript = null;
    	_createScript = null;
    }

	public NSTimestamp value()
	{
		return (NSTimestamp)objectValueForBinding("value");
	}
	
	public void setValue(NSTimestamp newDateIn)
	{
		setValueForBinding(newDateIn, "value");
	}

    /**
     * Adds datepicker.js to the header or includes it in an Ajax friendly manner.
     *
     * @see er.extensions.components.ERXNonSynchronizingComponent#appendToResponse(com.webobjects.appserver.WOResponse, com.webobjects.appserver.WOContext)
     * @see ERXResponseRewriter#addScriptResourceInHead(WOResponse, WOContext, String, String)
     */
    public void appendToResponse(WOResponse response, WOContext context)
    {
// leftover from CCDatePicker, haven't a clue what this is for..    	
//    	if (booleanValueForBinding("injectStylesheet")) {
//    		String framework = stringValueForBinding("cssFramework", FRAMEWORK_NAME);
//    		String cssFilename = stringValueForBinding("cssFile", CSS_FILENAME);
//   		ERXResponseRewriter.addStylesheetResourceInHead(response, context, framework, cssFilename);
//    	}
    	ERXResponseRewriter.addStylesheetResourceInHead(response, context, "ERCoolComponents","DateTime/datepicker.css");
    	ERXResponseRewriter.addScriptResourceInHead(response, context, "ERCoolComponents", "DateTime/prototype-base-extensions.js");
    	ERXResponseRewriter.addScriptResourceInHead(response, context, "ERCoolComponents", "DateTime/prototype-date-extensions.js");
    	ERXResponseRewriter.addScriptResourceInHead(response, context, "ERCoolComponents", "DateTime/datepicker.js");
        // we do not have a localized date time picker. Yet.
    	NSArray<String> bl = context.request().browserLanguages();
    	NSDictionary<?, ?> requestheaders = context.request().headers();
		NSLog.out.appendln("browserlanguages" + bl.toString()+" headers" + requestheaders.toString());
        String langScript = "-locale-"+ ERXLocalizer.currentLocalizer().languageCode() + "_"+ ERXLocalizer.currentLocalizer().locale() + ".js";

//        String langScript = "-locale-"+ ERXLocalizer.currentLocalizer().languageCode() + ".js";
		NSLog.out.appendln("language extension met locale: " + langScript+" use localized formatters: "+ERXLocalizer.useLocalizedFormatters());
//        ERXResponseRewriter.addScriptResourceInHead(response, context, FRAMEWORK_NAME, "DateTime/datepicker" + langScript);
        super.appendToResponse(response, context);
    }
    
	
	@Override
	public String name() {
		return elementID();
	}
	
	public void setDateformat(String value) {
		setValueForBinding(value, "dateformat");
	}
	
	public String dateformat() {
		String format = (String) stringValueForBinding("dateformat");
		if (format == null) {
//			format = ERXTimestampFormatter.DEFAULT_PATTERN;
//			format = "%Y-%m-%d";
//			format = "%Y-%m-%d %H:%M";
			format = null;
		}
		return format;
	}

	/**
	 * 
	 * @return
	 */
	public String dateFormatString() {
		
		if(dateformat() != null){
		String result = dateformat();
		
		/*
		 *  I have included all the strftime abbreviations, also the onces that are not changed or are not available,
		 *  just so one can see what is replaced see http://pubs.opengroup.org/onlinepubs/007908799/xsh/strftime.html
		 *  for a definition of strftime formats, and http://home.jongsma.org/software/js/datepicker. Or in the javascript 
		 *  code, of course
		 */
		result = ERXStringUtilities.replaceStringByStringInString("%a", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%A", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%b", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%B", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%c", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%C", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%d", "dd", result);
		result = ERXStringUtilities.replaceStringByStringInString("%D", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%e", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%h", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%H", "HH", result);
		result = ERXStringUtilities.replaceStringByStringInString("%I", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%j", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%m", "MM", result);
		result = ERXStringUtilities.replaceStringByStringInString("%M", "mm", result);
		result = ERXStringUtilities.replaceStringByStringInString("%n", "\n", result);
		result = ERXStringUtilities.replaceStringByStringInString("%p", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%r", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%R", "HH:mm", result);
		result = ERXStringUtilities.replaceStringByStringInString("%S", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%t", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%T", "HH:mm", result);
		result = ERXStringUtilities.replaceStringByStringInString("%u", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%U", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%V", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%w", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%W", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%x", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%X", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%y", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%Y", "yyyy", result);
		result = ERXStringUtilities.replaceStringByStringInString("%Z", "", result);
		result = ERXStringUtilities.replaceStringByStringInString("%%", "%%", result);
	
		
		if (result.indexOf("-") == 0) {
			// strip off leading "-"
			result = result.substring(1);
		}
		
		NSLog.out.appendln("dateformat: " + result);
		return result;
		} else {
			return null;
		}
		
	}
	
	public String dateTimePickerCreateScript() {
		if (_createScript == null) {
//			_createScript = "AnyTime.noPicker(\""+elementID()+"\"); AnyTime.picker("+dateTimePickerOptions() + ");";
//			_createScript = "AnyTime.noPicker(\""+elementID()+"\"); $(#\""+elementID()+"\"),remove; AnyTime.picker("+dateTimePickerOptions() + ");";
			_createScript = "new Control.DatePicker("+dateTimePickerOptions() + ");";
		}
		NSLog.out.appendln("createscript: "+_createScript);
		return _createScript;
	}
	
	private String dateTimePickerOptions() {
		String opts = "'" + elementID() + "',";

		/*
		 *  Start with finding out the locale, so that we can define the date format based on that.
		 *  Locale is based on en_US, nl_NL notation, so based on the countrycode we have to define the date format. 
		 *  Exception is if the user has set a formatter for the date himself. Than we should make sure that the locale
		 *  is not contradictory to the formatter. If that is the case, use a locale for javascript that does not 
		 *  contradict the formatter. Example: formatter is mm-dd-YYYY HH:MM, while locale is en_US. an english formatter
		 *  that conforms to this standard is dd-mm-YYYY HH:MM  
		 *  
		 *  Another topic is the dateformat: there is the possibility to use that datetime picker only as a datepicker, based 
		 *  on the option timePicker = false. In that case, the formatter passed on to datetimepicker.js and to the WOTextField
		 *  should only contain date info. 
		 */
		
		String locale = stringValueForBinding("locale");
		if (ne(locale)){ 
			String tmpdateFormat = (String) stringValueForBinding("dateformat");;
			String countrycode = countryCode(locale);
			if(countrycode.equalsIgnoreCase("BR")||
					countrycode.equalsIgnoreCase("AU")||
					countrycode.equalsIgnoreCase("ES")||
					countrycode.equalsIgnoreCase("EU")||
					countrycode.equalsIgnoreCase("GB")||
					countrycode.equalsIgnoreCase("NL")){
				setDateformat("%d-%m-%Y %H:%M");
				//setDateformat("%d-%m-%Y");
			} else if(countrycode.equalsIgnoreCase("FR")){
				setDateformat("%d/%m/%Y %H:%M");
				setDateformat("%d/%m/%Y");
			} else if(countrycode.equalsIgnoreCase("DE")||
					countrycode.equalsIgnoreCase("ISO8601")||
					countrycode.equalsIgnoreCase("LT")||
					countrycode.equalsIgnoreCase("PL")){
				setDateformat("%Y-%m-%d %H:%M");
				//setDateformat("%Y-%m-%d");
			} else if(countrycode.equalsIgnoreCase("US")){
				setDateformat("%m-%d-%Y %H:%M");
				//setDateformat("%m-%d-%Y");
			} else {
				// the rest will get iso8601
				setDateformat("%Y-%m-%d %H:%M");
				//setDateformat("%Y-%m-%d");
			}
			//check if the dateformat we received from the WOTextField equals the format one would expect
			if (tmpdateFormat !=null){
				if (tmpdateFormat.equalsIgnoreCase(dateformat())){
					NSLog.out.appendln("datetimeformat passed on is the same as the locale: "+locale+" country: "+countrycode+" format "+tmpdateFormat+" vs "+dateformat());
					// were OK
				} else {
					// we are not OK: the dateformat that we received is not equal to the locale we have received. 
					// We determine the locale that we pass through to the javascript based on the datetimeformat we received
					NSLog.out.appendln("datetimeformat passed on is different from chosen locale: "+locale+" country: "+countrycode+" format "+tmpdateFormat+" vs "+dateformat());
					// setDateformat(tmpdateFormat);
				}
			}
		}
		
		boolean timePicker = booleanValueForBinding("timePicker", true);
		boolean datePicker = booleanValueForBinding("datePicker", true);
		
		// check if timepicker == true, else only return dateformat as a string
		if (timePicker == true){
			opts += " { datetimeFormat:'" + dateFormatString() + "'";
		} else {
			opts +=" { dateFormat:'" + dmyFormat(dateFormatString()) + "'";
			setDateformat(dmyFormat(dateFormatString()));
		}
		
		if (datePicker==true)
			opts += ",datePicker:true";
		else 
			opts += ",datePicker:false";

		if (timePicker==true)
			opts += ",timePicker:true";
		else 
			opts += ",timePicker:false";

		boolean timePickerAdjacent = booleanValueForBinding("timePickerAdjacent", true);
		if (timePickerAdjacent==true)
			opts += ",timePickerAdjacent:true";

		boolean use24hrs = booleanValueForBinding("use24hrs", true);
		if (use24hrs==true)
			opts += ",use24hrs:true";
		else 
			opts += ",use24hrs:false";

		if (ne(locale)) 
			opts += ",locale:'" + locale+"'";
		
		String icon = stringValueForBinding("icon");
		if (ne(icon)) 
			opts += ",icon:'" + icon+"'";
		
		String onSelect = stringValueForBinding("onSelect");
		if (ne(onSelect))
			opts += ",onSelect:'" + onSelect +"'";

		String onHover = stringValueForBinding("onHover");
		if (ne(onHover))
			opts += ",onHover:'" + onHover +"'";
		
		opts += "}";
		return opts;
	}
	
	public String dateTimePickerOpenScript() {
		if (_openScript == null) {
//			_openScript = "";	
			_openScript = "new Control.DatePicker(\""+dateTimePickerOptions()+"\");";	
		}
		return _openScript;
	}
	
	public String elementID() {
		if (_elementID == null) {
			_elementID = ERXStringUtilities.safeIdentifierName(context().elementID(), "datetimebox");
		}
		return _elementID;
	}

	// This is left over from CCDatePicker, in case there will be an option to set a high or low date/time so that somebody else can make use of it. JH	
	private String parseDateRangeString(String dateRange) {
		String result = "";
		if (dateRange.indexOf(":") > 0) {
			NSArray<String> components = NSArray.componentsSeparatedByString(dateRange, ":");
			String firstDate = "'" + components.objectAtIndex(0) + "'";
			String lastDate = components.objectAtIndex(1);
			if (!lastDate.equals("1")) 
				lastDate  = "'" + lastDate + "'";
			result = "{" + firstDate + ":" + lastDate + "}";
		} else {
			result = "{'" + dateRange + "':1}" ;
		}
		return result;
	}
	
	private boolean ne(String v) {
		return v != null && v.length() > 0;
	}

	public static String optionsStringForTimestamp(NSTimestamp ts) {
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return formatter.format(ts);
	}

    // code from Practical WebObjects, Localization, page 226 to get country and language codes. 
	
	
	
    public static String languageCode(String aName) {
            // the string is in the form xx-YY
            String aCode = "";
            if (aName.length() > 2) {
                    aCode = aName.substring(0,2).toLowerCase();
            } else if (aName.length() == 2) {
                    aCode = aName.toLowerCase();
            } else {
                    aCode = "zz"; // Unable to determine the language code
            }
            return aCode;
    }

    public static String countryCode(String aName) {
        // the string is in the form xx_YY or xx-yy
        String aCode = "";
        if (aName.indexOf("_") > 0) {
        	aCode = aName.substring(aName.indexOf("_")+1).toUpperCase();
        } else if (aName.indexOf("-") > 0) {
        	aCode = aName.substring(aName.indexOf("-")+1).toUpperCase();
        } else {
            aCode = "ZZ"; // Unable to determine the country code
        }
        // The standard says that there can be another part after the country so just trim it.
        if (aCode.indexOf("_") > 0) {
        	aCode = aCode.substring(0,aName.indexOf("_")).toUpperCase();
        } else if (aCode.indexOf("-") > 0) {
        	aCode = aCode.substring(0,aName.indexOf("-")).toUpperCase();
        }
        return aCode;
    }
    

    public static String dmyFormat(String aName) {
        // the string is in the form "YY-MM-DD xx-YY", we look for the space
        String aCode = "";
        // The standard says that there can be another part after the country so just trim it.
        if (aCode.indexOf(" ") > 0) {
        	aCode = aCode.substring(0,aName.indexOf(""));
        }
        return aCode;
    }
    

    
}