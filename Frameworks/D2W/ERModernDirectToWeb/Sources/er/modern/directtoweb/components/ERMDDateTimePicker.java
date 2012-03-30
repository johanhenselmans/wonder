package er.modern.directtoweb.components;

import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WUtils;

import er.directtoweb.components.ERDCustomEditComponent;
import er.extensions.formatters.ERXTimestampFormatter;
import er.extensions.foundation.ERXValueUtilities;

/**
 * D2WEditComponent based on CCDateTimePicker.
 * 
 * @d2wKey dateTimeFormatter - string - date/time format string
 * @d2wKey formatter - string - date format string
 * @d2wKey dateTimePickerDragDisabled - boolean - disable dragging on date picker
 * @d2wKey dateTimePickerCssFile - string - file for alternate css file (default is datepicker.css)
 * @d2wKey dateTimePickerCssFramework - string - framework for alternate css file (default is ERModernDirectToWeb)
 * @d2wKey dateTimePickericon - string - which icon to display in input box
 * @d2wKey dateTimePickerDatePicker - boolean - show date default true
 * @d2wKey dateTimePickerTimePicker - boolean - show time default true
 * @d2wKey dateTimePickerLocale - string - which locale to use, eg en_US, pt_BR
 * @d2wKey dateTimePickerUse24hrs - boolean - use 24 hours notation
 * 
 * @author johanhenselmans
 *
 */
public class ERMDDateTimePicker extends ERDCustomEditComponent {
	
    private String _formatter;
    private String _dateReadableDescription;
    
	public ERMDDateTimePicker(WOContext context) {
        super(context);
    }
	
    /**
     * Format string for the date text fields
     * 
     * @return
     */
	public String formatter() {
		if(_formatter == null) {
			_formatter = (String)valueForBinding("formatter");
		}
		if(_formatter == null || _formatter.length() == 0) {
			// _formatter = ERXTimestampFormatter.DEFAULT_PATTERN;
			_formatter = "%Y-%m-%d";;
		}
		return _formatter;
	}

	public void setFormatter(String formatter) {
		_formatter = formatter;
	}
	


	/**
     * Convenience accessor for the readable date format description
     * 
     * @return
     */
	public String dateReadableDescription() {
		if (_dateReadableDescription == null) {
			_dateReadableDescription = D2WUtils.readableDateFormatDescription(formatter());
		}
		return _dateReadableDescription;
	}
	
	// DatePicker options

	/**
	 * {@link CCDateTimePicker} option: is drag disabled
	 * 
	 * @return
	 */
	public Boolean dragDisabled() {
		return ERXValueUtilities.BooleanValueWithDefault(valueForBinding("dateTimePickerDragDisabled"), null);
	}

	/**
	 * {@link CCDateTimePicker} option: name of the custom css file
	 * 
	 * @return
	 */
	public String cssFile() {
		return (String)valueForBinding("dateTimePickerCssFile");
	}

	/**
	 * {@link CCDateTimePicker} option: name of the custom css file framework
	 * 
	 * @return
	 */
	public String cssFramework() {
		return (String)valueForBinding("dateTimePickerCssFramework");
	}

	/**
	 * {@link CCDateTimePicker} option: show dates
	 * 
	 * @return
	 */
	public Boolean datePicker() {
		return ERXValueUtilities.BooleanValueWithDefault(valueForBinding("dateTimePickerDatePicker"), null);
	}
	
	/**
	 * {@link CCDateTimePicker} option: show time
	 * 
	 * @return
	 */
	public Boolean timePicker() {
		return ERXValueUtilities.BooleanValueWithDefault(valueForBinding("dateTimePickerTimePicker"), null);
	}
	
	

	/**
	 * {@link CCDateTimePicker} option: which locale to use, eg en_US, pt_BR, nl_NL
	 * 
	 * @return
	 */
	public String locale() {
		return (String)valueForBinding("dateTimePickerLocale");
	}
	
	/**
	 * {@link CCDateTimePicker} option: use 24 hours 
	 * 
	 * @return
	 */
	public Boolean use24hrs() {
		return ERXValueUtilities.BooleanValueWithDefault(valueForBinding("dateTimePickerUse24hrs"), null);
	}
	
}